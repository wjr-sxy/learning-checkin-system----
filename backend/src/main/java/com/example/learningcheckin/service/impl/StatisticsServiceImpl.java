package com.example.learningcheckin.service.impl;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.example.learningcheckin.dto.DailyStatsDTO;
import com.example.learningcheckin.dto.StudentAbnormalDTO;
import com.example.learningcheckin.dto.TrendPointDTO;
import com.example.learningcheckin.entity.Checkin;
import com.example.learningcheckin.entity.CourseStudent;
import com.example.learningcheckin.entity.StudyPlan;
import com.example.learningcheckin.entity.User;
import com.example.learningcheckin.mapper.CheckinMapper;
import com.example.learningcheckin.mapper.CourseStudentMapper;
import com.example.learningcheckin.mapper.StudyPlanMapper;
import com.example.learningcheckin.mapper.UserMapper;
import com.example.learningcheckin.service.IStatisticsService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.time.LocalDate;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

@Service
public class StatisticsServiceImpl implements IStatisticsService {

    @Autowired
    private CourseStudentMapper courseStudentMapper;

    @Autowired
    private CheckinMapper checkinMapper;

    @Autowired
    private StudyPlanMapper studyPlanMapper;

    @Autowired
    private UserMapper userMapper;

    @Override
    public DailyStatsDTO getDailyStats(Long courseId, LocalDate date) {
        if (date == null) {
            date = LocalDate.now();
        }

        DailyStatsDTO stats = new DailyStatsDTO();
        stats.setDate(date);

        // 1. Get Active Students
        List<CourseStudent> students = courseStudentMapper.selectList(new LambdaQueryWrapper<CourseStudent>()
                .eq(CourseStudent::getCourseId, courseId)
                .eq(CourseStudent::getStatus, 0)); // 0: Active

        int totalStudents = students.size();
        stats.setTotalStudents(totalStudents);

        if (totalStudents == 0) {
            stats.setCheckedInCount(0);
            stats.setCheckinRate(BigDecimal.ZERO);
            stats.setAbnormalStudents(new ArrayList<>());
            return stats;
        }

        List<Long> studentIds = students.stream().map(CourseStudent::getStudentId).collect(Collectors.toList());

        // 2. Count Check-ins for these students on date
        // Using CheckinMapper. We need to query count where date = date and userId in studentIds
        Long checkinCount = checkinMapper.selectCount(new LambdaQueryWrapper<Checkin>()
                .eq(Checkin::getCheckinDate, date)
                .in(Checkin::getUserId, studentIds));

        stats.setCheckedInCount(checkinCount.intValue());

        BigDecimal rate = BigDecimal.valueOf(checkinCount)
                .divide(BigDecimal.valueOf(totalStudents), 4, RoundingMode.HALF_UP) // 4 decimal places
                .multiply(BigDecimal.valueOf(100))
                .setScale(1, RoundingMode.HALF_UP); // 1 decimal place (e.g. 85.5)
        stats.setCheckinRate(rate);

        // 3. Find Abnormal Students (Completion Rate < 60%)
        // We calculate average completion rate for each student across all their plans in this course
        List<StudentAbnormalDTO> abnormalList = new ArrayList<>();

        // Get all plans for this course (student copies)
        List<StudyPlan> coursePlans = studyPlanMapper.selectList(new LambdaQueryWrapper<StudyPlan>()
                .eq(StudyPlan::getCourseId, courseId)
                .in(StudyPlan::getUserId, studentIds));

        // Group by Student
        Map<Long, List<StudyPlan>> studentPlans = coursePlans.stream()
                .collect(Collectors.groupingBy(StudyPlan::getUserId));

        for (Long studentId : studentIds) {
            List<StudyPlan> myPlans = studentPlans.getOrDefault(studentId, new ArrayList<>());
            
            int totalTasks = 0;
            int completedTasks = 0;
            
            for (StudyPlan p : myPlans) {
                if (p.getTotalTasks() != null) totalTasks += p.getTotalTasks();
                if (p.getCompletedTasks() != null) completedTasks += p.getCompletedTasks();
            }

            BigDecimal completionRate = BigDecimal.ZERO;
            if (totalTasks > 0) {
                completionRate = BigDecimal.valueOf(completedTasks)
                        .divide(BigDecimal.valueOf(totalTasks), 4, RoundingMode.HALF_UP)
                        .multiply(BigDecimal.valueOf(100));
            } else {
                // If no tasks, assume 100% or 0%? 
                // If plans exist but 0 tasks, maybe 100%. If no plans, maybe N/A (ignore).
                // Let's assume if no tasks assigned, they are not abnormal.
                if (myPlans.isEmpty()) completionRate = BigDecimal.valueOf(100);
            }

            if (completionRate.compareTo(BigDecimal.valueOf(60)) < 0) {
                StudentAbnormalDTO abnormal = new StudentAbnormalDTO();
                abnormal.setStudentId(studentId);
                abnormal.setTotalTasks(totalTasks);
                abnormal.setCompletedTasks(completedTasks);
                abnormal.setCompletionRate(completionRate.setScale(1, RoundingMode.HALF_UP));
                
                User user = userMapper.selectById(studentId);
                if (user != null) {
                    abnormal.setStudentName(user.getUsername()); // or real name
                    abnormal.setStudentNumber(user.getUsername()); // placeholder
                } else {
                    abnormal.setStudentName("Unknown");
                }
                
                abnormalList.add(abnormal);
            }
        }

        stats.setAbnormalStudents(abnormalList);

        return stats;
    }

    @Override
    public List<TrendPointDTO> getCompletionTrend(Long courseId, Integer days) {
        if (days == null || days <= 0) days = 30;
        LocalDate endDate = LocalDate.now();
        LocalDate startDate = endDate.minusDays(days - 1);

        List<TrendPointDTO> trend = new ArrayList<>();

        // Get Active Students
        List<CourseStudent> students = courseStudentMapper.selectList(new LambdaQueryWrapper<CourseStudent>()
                .eq(CourseStudent::getCourseId, courseId)
                .eq(CourseStudent::getStatus, 0));
        
        int totalStudents = students.size();
        if (totalStudents == 0) {
             return trend; // Empty
        }
        List<Long> studentIds = students.stream().map(CourseStudent::getStudentId).collect(Collectors.toList());

        // For each day, calculate Check-in Rate (as a proxy for "Activity/Completion Trend" for now)
        // Optimization: Group by date in SQL
        // SELECT checkin_date, COUNT(*) FROM sys_checkin WHERE ... GROUP BY checkin_date
        
        List<Checkin> checkins = checkinMapper.selectList(new LambdaQueryWrapper<Checkin>()
                .ge(Checkin::getCheckinDate, startDate)
                .le(Checkin::getCheckinDate, endDate)
                .in(Checkin::getUserId, studentIds));
        
        Map<LocalDate, Long> countByDate = checkins.stream()
                .collect(Collectors.groupingBy(Checkin::getCheckinDate, Collectors.counting()));

        for (int i = 0; i < days; i++) {
            LocalDate d = startDate.plusDays(i);
            TrendPointDTO point = new TrendPointDTO();
            point.setDate(d.toString());
            
            Long count = countByDate.getOrDefault(d, 0L);
            BigDecimal rate = BigDecimal.valueOf(count)
                    .divide(BigDecimal.valueOf(totalStudents), 4, RoundingMode.HALF_UP)
                    .multiply(BigDecimal.valueOf(100))
                    .setScale(1, RoundingMode.HALF_UP);
            
            point.setRate(rate);
            trend.add(point);
        }

        return trend;
    }
}
