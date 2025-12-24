package com.example.learningcheckin.controller;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.example.learningcheckin.common.Result;
import com.example.learningcheckin.entity.PointsRecord;
import com.example.learningcheckin.entity.PointsRule;
import com.example.learningcheckin.entity.User;
import com.example.learningcheckin.mapper.PointsRecordMapper;
import com.example.learningcheckin.mapper.PointsRuleMapper;
import com.example.learningcheckin.mapper.UserMapper;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.*;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

@RestController
@RequestMapping("/api/admin/points")
public class AdminPointsController {

    @Autowired
    private PointsRuleMapper pointsRuleMapper;

    @Autowired
    private PointsRecordMapper pointsRecordMapper;

    @Autowired
    private com.example.learningcheckin.service.IConfigService configService;

    @Autowired
    private UserMapper userMapper;

    @GetMapping("/stats")
    public Result<Map<String, Object>> getPointsStats() {
        Map<String, Object> stats = new HashMap<>();

        // 1. Calculate Today's Issuance
        List<PointsRecord> todayRecords = pointsRecordMapper.selectList(new LambdaQueryWrapper<PointsRecord>()
                .eq(PointsRecord::getType, 1)
                .ge(PointsRecord::getCreateTime, LocalDate.now().atStartOfDay()));
        int todayIssuance = todayRecords.stream().mapToInt(PointsRecord::getAmount).sum();

        // 2. Calculate Current Total Circulation (Sum of all user points)
        List<User> users = userMapper.selectList(null);
        long totalCirculation = users.stream()
                .filter(u -> u.getPoints() != null)
                .mapToLong(User::getPoints)
                .sum();

        // 3. Calculate Inflation Rate (Week over Week)
        LocalDateTime oneWeekAgo = LocalDateTime.now().minusDays(7);
        List<PointsRecord> weekRecords = pointsRecordMapper.selectList(new LambdaQueryWrapper<PointsRecord>()
                .ge(PointsRecord::getCreateTime, oneWeekAgo));

        long weekIssuance = weekRecords.stream()
                .filter(r -> r.getType() == 1)
                .mapToLong(PointsRecord::getAmount)
                .sum();

        long weekConsumption = weekRecords.stream()
                .filter(r -> r.getType() == 2)
                .mapToLong(PointsRecord::getAmount)
                .sum();

        long netChange = weekIssuance - weekConsumption;
        long startOfWeekCirculation = totalCirculation - netChange;

        double inflationRate = 0.0;
        if (startOfWeekCirculation > 0) {
            inflationRate = (double) netChange / startOfWeekCirculation * 100;
        }

        boolean inflationWarning = inflationRate > 15.0;
        String inflationAdvice = inflationWarning ? "警告：通胀率过高！建议降低基础积分获取或提高商品价格。" : "正常";

        stats.put("todayIssuance", todayIssuance);
        stats.put("totalCirculation", totalCirculation);
        stats.put("inflationRate", String.format("%.2f", inflationRate));
        stats.put("inflationWarning", inflationWarning);
        stats.put("inflationAdvice", inflationAdvice);

        return Result.success(stats);
    }

    @GetMapping("/rules")
    public Result<List<PointsRule>> getRules() {
        return Result.success(pointsRuleMapper.selectList(null));
    }

    @PostMapping("/rules")
    public Result<String> updateRule(@RequestBody PointsRule rule) {
        if (rule.getId() != null) {
            rule.setUpdateTime(LocalDateTime.now());
            pointsRuleMapper.updateById(rule);
        } else {
            pointsRuleMapper.insert(rule);
        }
        return Result.success("Rule saved");
    }

    @GetMapping("/multiplier")
    public Result<Double> getMultiplier() {
        return Result.success(configService.getPointsMultiplier());
    }

    @PostMapping("/multiplier")
    @com.example.learningcheckin.annotation.Log(module = "积分央行", action = "调整全局倍率")
    public Result<String> setMultiplier(@RequestBody Map<String, Double> body) {
        Double multiplier = body.get("multiplier");
        if (multiplier == null) {
            return Result.error(400, "Multiplier is required");
        }
        try {
            configService.setPointsMultiplier(multiplier);
            return Result.success("Multiplier updated");
        } catch (RuntimeException e) {
            return Result.error(400, e.getMessage());
        }
    }
}
