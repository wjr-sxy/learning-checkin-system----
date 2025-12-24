package com.example.learningcheckin.controller;

import com.baomidou.mybatisplus.core.conditions.query.QueryWrapper;
import com.example.learningcheckin.common.Result;
import com.example.learningcheckin.entity.Task;
import com.example.learningcheckin.entity.TaskSubmission;
import com.example.learningcheckin.entity.User;
import com.example.learningcheckin.mapper.UserMapper;
import com.example.learningcheckin.service.ITaskService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.*;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.List;
import java.util.Map;

@RestController
@RequestMapping("/api/tasks")
public class TaskController {

    private static final Logger logger = LoggerFactory.getLogger(TaskController.class);

    @Autowired
    private ITaskService taskService;

    @Autowired
    private UserMapper userMapper;

    private User getCurrentUser(Authentication authentication) {
        String username = authentication.getName();
        return userMapper.selectOne(new QueryWrapper<User>().eq("username", username));
    }

    // Teacher: Create Task
    @PostMapping("/create")
    public Result<Task> createTask(@RequestBody Task task, Authentication authentication) {
        logger.info("Received create task request: {}", task);
        User user = getCurrentUser(authentication);
        if (user == null) return Result.error(401, "User not found");
        // Verify role? Assuming frontend handles role check or we check here.
        // "Teacher" role check could be added.
        
        task.setTeacherId(user.getId());
        try {
            Task createdTask = taskService.createTask(task);
            logger.info("Task created successfully with ID: {}", createdTask.getId());
            return Result.success(createdTask);
        } catch (Exception e) {
            logger.error("Error creating task: ", e);
            throw e;
        }
    }

    // Teacher: Get Created Tasks
    @GetMapping("/teacher")
    public Result<List<Map<String, Object>>> getTeacherTasks(Authentication authentication) {
        User user = getCurrentUser(authentication);
        if (user == null) return Result.error(401, "User not found");
        return Result.success(taskService.getTeacherTasks(user.getId()));
    }

    // Student: Get Tasks
    @GetMapping("/student")
    public Result<List<Map<String, Object>>> getStudentTasks(Authentication authentication) {
        User user = getCurrentUser(authentication);
        if (user == null) return Result.error(401, "User not found");
        return Result.success(taskService.getStudentTasks(user.getId()));
    }

    // Student: Submit Task
    @PostMapping("/{id}/submit")
    public Result<String> submitTask(@PathVariable Long id, 
                                     @RequestBody Map<String, String> payload, 
                                     Authentication authentication) {
        User user = getCurrentUser(authentication);
        if (user == null) return Result.error(401, "User not found");
        
        String content = payload.get("content");
        String fileUrls = payload.get("fileUrls");
        
        try {
            taskService.submitTask(id, user.getId(), content, fileUrls);
            return Result.success("Submitted successfully");
        } catch (Exception e) {
            return Result.error(400, e.getMessage());
        }
    }

    @GetMapping("/{id}/leaderboard")
    public Result<List<Map<String, Object>>> getTaskLeaderboard(@PathVariable Long id) {
        return Result.success(taskService.getTaskLeaderboard(id));
    }

    @GetMapping("/{id}/stats")
    public Result<Map<String, Object>> getTaskStats(@PathVariable Long id) {
        return Result.success(taskService.getTaskStats(id));
    }

    // Teacher: Get Submissions for a Task
    @GetMapping("/{id}/submissions")
    public Result<List<TaskSubmission>> getSubmissions(@PathVariable Long id) {
        return Result.success(taskService.getTaskSubmissions(id));
    }

    // Teacher: Grade Submission
    @PostMapping("/submissions/{id}/grade")
    public Result<String> gradeSubmission(@PathVariable Long id, @RequestBody Map<String, Object> payload) {
        try {
            Integer score = (Integer) payload.get("score");
            // Handle float/double to integer conversion if necessary, or just cast
            // Safely handle if score comes as string or double from JSON
            if (payload.get("score") instanceof Double) {
                 score = ((Double) payload.get("score")).intValue();
            }
            
            Integer rating = 0;
            if (payload.get("rating") != null) {
                 if (payload.get("rating") instanceof Double) {
                     rating = ((Double) payload.get("rating")).intValue();
                 } else {
                     rating = (Integer) payload.get("rating");
                 }
            }
            
            String comment = (String) payload.get("comment");
            taskService.gradeSubmission(id, score, rating, comment);
            return Result.success("Graded successfully");
        } catch (Exception e) {
            return Result.error(400, e.getMessage());
        }
    }

    @PostMapping("/submissions/{id}/return")
    public Result<String> returnSubmission(@PathVariable Long id, @RequestBody Map<String, Object> payload) {
        try {
            String comment = (String) payload.get("comment");
            taskService.returnSubmission(id, comment);
            return Result.success("Returned successfully");
        } catch (Exception e) {
            return Result.error(400, e.getMessage());
        }
    }
}
