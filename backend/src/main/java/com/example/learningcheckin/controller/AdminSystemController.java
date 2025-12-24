package com.example.learningcheckin.controller;

import cn.hutool.core.text.csv.CsvUtil;
import cn.hutool.core.text.csv.CsvWriter;
import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.example.learningcheckin.annotation.Log;
import com.example.learningcheckin.common.Result;
import com.example.learningcheckin.entity.Announcement;
import com.example.learningcheckin.entity.Blacklist;
import com.example.learningcheckin.entity.LoginLog;
import com.example.learningcheckin.entity.OperationLog;
import com.example.learningcheckin.mapper.AnnouncementMapper;
import com.example.learningcheckin.mapper.BlacklistMapper;
import com.example.learningcheckin.mapper.LoginLogMapper;
import com.example.learningcheckin.mapper.OperationLogMapper;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.*;

import javax.servlet.http.HttpServletResponse;
import java.io.IOException;
import java.time.LocalDateTime;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

@RestController
@RequestMapping("/api/admin/system")
public class AdminSystemController {

    @Autowired
    private AnnouncementMapper announcementMapper;

    @Autowired
    private BlacklistMapper blacklistMapper;

    @Autowired
    private OperationLogMapper operationLogMapper;

    @Autowired
    private LoginLogMapper loginLogMapper;

    // Announcements
    @GetMapping("/announcements")
    public Result<Page<Announcement>> listAnnouncements(@RequestParam(defaultValue = "1") Integer page,
                                                        @RequestParam(defaultValue = "10") Integer size) {
        return Result.success(announcementMapper.selectPage(new Page<>(page, size), 
                new LambdaQueryWrapper<Announcement>().orderByDesc(Announcement::getCreateTime)));
    }

    @PostMapping("/announcements")
    @Log(module = "系统工具", action = "保存公告")
    public Result<String> saveAnnouncement(@RequestBody Announcement announcement) {
        if (announcement.getId() == null) {
            announcement.setCreateTime(LocalDateTime.now());
            announcementMapper.insert(announcement);
        } else {
            announcementMapper.updateById(announcement);
        }
        return Result.success("Saved successfully");
    }

    @DeleteMapping("/announcements/{id}")
    @Log(module = "系统工具", action = "删除公告")
    public Result<String> deleteAnnouncement(@PathVariable Long id) {
        announcementMapper.deleteById(id);
        return Result.success("Deleted successfully");
    }

    // Blacklist
    @GetMapping("/blacklist")
    public Result<Page<Blacklist>> listBlacklist(@RequestParam(defaultValue = "1") Integer page,
                                                 @RequestParam(defaultValue = "10") Integer size,
                                                 @RequestParam(required = false) String type) {
        LambdaQueryWrapper<Blacklist> wrapper = new LambdaQueryWrapper<>();
        if (type != null && !type.isEmpty()) {
            wrapper.eq(Blacklist::getType, type);
        }
        wrapper.orderByDesc(Blacklist::getCreateTime);
        return Result.success(blacklistMapper.selectPage(new Page<>(page, size), wrapper));
    }

    @PostMapping("/blacklist")
    @Log(module = "系统工具", action = "添加黑名单")
    public Result<String> addBlacklist(@RequestBody Blacklist blacklist) {
        blacklist.setCreateTime(LocalDateTime.now());
        blacklistMapper.insert(blacklist);
        return Result.success("Added to blacklist");
    }

    @DeleteMapping("/blacklist/{id}")
    @Log(module = "系统工具", action = "删除黑名单")
    public Result<String> deleteBlacklist(@PathVariable Long id) {
        blacklistMapper.deleteById(id);
        return Result.success("Removed from blacklist");
    }

    // Logs
    @GetMapping("/logs/operation")
    public Result<Page<OperationLog>> listOperationLogs(@RequestParam(defaultValue = "1") Integer page,
                                                        @RequestParam(defaultValue = "10") Integer size) {
        return Result.success(operationLogMapper.selectPage(new Page<>(page, size), 
                new LambdaQueryWrapper<OperationLog>().orderByDesc(OperationLog::getCreateTime)));
    }

    @GetMapping("/logs/operation/export")
    public void exportOperationLogs(HttpServletResponse response) throws IOException {
        response.setContentType("text/csv;charset=UTF-8");
        response.setHeader("Content-Disposition", "attachment; filename=operation_logs.csv");
        
        List<OperationLog> list = operationLogMapper.selectList(new LambdaQueryWrapper<OperationLog>().orderByDesc(OperationLog::getCreateTime));
        
        CsvWriter writer = CsvUtil.getWriter(response.getWriter());
        writer.write(new String[]{"ID", "User ID", "Module", "Action", "Description", "Method", "IP", "Status", "Execution Time (ms)", "Create Time"});
        for (OperationLog log : list) {
            writer.write(new String[]{
                String.valueOf(log.getId()),
                String.valueOf(log.getUserId()),
                log.getModule(),
                log.getAction(),
                log.getDescription(),
                log.getMethod(),
                log.getIp(),
                log.getStatus() == 0 ? "Success" : "Fail",
                String.valueOf(log.getExecutionTime()),
                String.valueOf(log.getCreateTime())
            });
        }
        writer.flush();
        writer.close();
    }

    @GetMapping("/logs/login")
    public Result<Page<LoginLog>> listLoginLogs(@RequestParam(defaultValue = "1") Integer page,
                                                @RequestParam(defaultValue = "10") Integer size) {
        return Result.success(loginLogMapper.selectPage(new Page<>(page, size), 
                new LambdaQueryWrapper<LoginLog>().orderByDesc(LoginLog::getCreateTime)));
    }

    @GetMapping("/logs/login/export")
    public void exportLoginLogs(HttpServletResponse response) throws IOException {
        response.setContentType("text/csv;charset=UTF-8");
        response.setHeader("Content-Disposition", "attachment; filename=login_logs.csv");
        
        List<LoginLog> list = loginLogMapper.selectList(new LambdaQueryWrapper<LoginLog>().orderByDesc(LoginLog::getCreateTime));
        
        CsvWriter writer = CsvUtil.getWriter(response.getWriter());
        writer.write(new String[]{"ID", "User ID", "Username", "IP", "Status", "Message", "Create Time"});
        for (LoginLog log : list) {
            writer.write(new String[]{
                String.valueOf(log.getId()),
                String.valueOf(log.getUserId()),
                log.getUsername(),
                log.getIp(),
                log.getStatus() == 0 ? "Success" : "Fail",
                log.getMessage(),
                String.valueOf(log.getCreateTime())
            });
        }
        writer.flush();
        writer.close();
    }

    @GetMapping("/health")
    public Result<Map<String, Object>> getSystemHealth() {
        LocalDateTime oneHourAgo = LocalDateTime.now().minusHours(1);
        
        // 1. Login Failures
        Long loginFailures = loginLogMapper.selectCount(new LambdaQueryWrapper<LoginLog>()
                .eq(LoginLog::getStatus, 1)
                .ge(LoginLog::getCreateTime, oneHourAgo));

        // 2. Interface Errors
        Long interfaceErrors = operationLogMapper.selectCount(new LambdaQueryWrapper<OperationLog>()
                .eq(OperationLog::getStatus, 1) // 1: Fail
                .ge(OperationLog::getCreateTime, oneHourAgo));

        // 3. Slow Queries (Execution time > 1000ms)
        Long slowQueries = operationLogMapper.selectCount(new LambdaQueryWrapper<OperationLog>()
                .gt(OperationLog::getExecutionTime, 1000)
                .ge(OperationLog::getCreateTime, oneHourAgo));

        Map<String, Object> health = new HashMap<>();
        health.put("loginFailures", loginFailures);
        health.put("interfaceErrors", interfaceErrors);
        health.put("slowQueries", slowQueries);
        health.put("status", (loginFailures > 5 || interfaceErrors > 10 || slowQueries > 5) ? "Warning" : "Normal");

        return Result.success(health);
    }
}
