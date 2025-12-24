package com.example.learningcheckin.controller;

import com.example.learningcheckin.common.Result;
import com.example.learningcheckin.entity.LoginLog;
import com.example.learningcheckin.entity.OperationLog;
import com.example.learningcheckin.entity.User;
import com.example.learningcheckin.mapper.LoginLogMapper;
import com.example.learningcheckin.mapper.OperationLogMapper;
import com.example.learningcheckin.mapper.UserMapper;
import com.baomidou.mybatisplus.core.conditions.query.QueryWrapper;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.Map;
import java.util.regex.Pattern;

@RestController
@RequestMapping("/api/user")
@CrossOrigin
public class UserController {

    @Autowired
    private UserMapper userMapper;

    @Autowired
    private LoginLogMapper loginLogMapper;

    @Autowired
    private OperationLogMapper operationLogMapper;

    @Autowired
    private PasswordEncoder passwordEncoder;

    @PutMapping("/profile")
    public Result<User> updateProfile(@RequestBody Map<String, Object> body) {
        Long userId = Long.valueOf(body.get("id").toString());
        User user = userMapper.selectById(userId);
        if (user == null) {
            return Result.error(404, "User not found");
        }

        if (body.containsKey("email")) {
            user.setEmail((String) body.get("email"));
        }
        if (body.containsKey("avatar")) {
            user.setAvatar((String) body.get("avatar"));
        }
        if (body.containsKey("fullName")) {
            String fullName = (String) body.get("fullName");
            if (fullName.length() > 20) {
                return Result.error(400, "姓名长度限制20字符");
            }
            user.setFullName(fullName);
        }
        if (body.containsKey("college")) {
            user.setCollege((String) body.get("college"));
        }
        if (body.containsKey("phone")) {
            String phone = (String) body.get("phone");
            if (!Pattern.matches("^1[3-9]\\d{9}$", phone)) {
                return Result.error(400, "手机号格式不正确");
            }
            user.setPhone(phone);
        }
        
        userMapper.updateById(user);
        user.setPassword(null);
        return Result.success(user);
    }

    @PutMapping("/password")
    public Result<String> updatePassword(@RequestBody Map<String, String> body) {
        Long userId = Long.valueOf(body.get("id"));
        String oldPassword = body.get("oldPassword");
        String newPassword = body.get("newPassword");

        User user = userMapper.selectById(userId);
        if (user == null) {
            return Result.error(404, "User not found");
        }

        if (!passwordEncoder.matches(oldPassword, user.getPassword())) {
            return Result.error(400, "Old password incorrect");
        }

        user.setPassword(passwordEncoder.encode(newPassword));
        userMapper.updateById(user);

        return Result.success("Password updated successfully");
    }

    @GetMapping("/login-logs")
    public Result<List<LoginLog>> getLoginLogs(@RequestParam Long userId) {
        QueryWrapper<LoginLog> queryWrapper = new QueryWrapper<>();
        queryWrapper.eq("user_id", userId).orderByDesc("create_time");
        return Result.success(loginLogMapper.selectList(queryWrapper));
    }

    @GetMapping("/operation-logs")
    public Result<List<OperationLog>> getOperationLogs(
            @RequestParam Long userId,
            @RequestParam(required = false) String startDate,
            @RequestParam(required = false) String endDate,
            @RequestParam(required = false) String module) {
        QueryWrapper<OperationLog> queryWrapper = new QueryWrapper<>();
        queryWrapper.eq("user_id", userId);
        
        if (startDate != null && !startDate.isEmpty()) {
            queryWrapper.ge("create_time", startDate + " 00:00:00");
        }
        if (endDate != null && !endDate.isEmpty()) {
            queryWrapper.le("create_time", endDate + " 23:59:59");
        }
        if (module != null && !module.isEmpty()) {
            queryWrapper.eq("module", module);
        }
        
        queryWrapper.orderByDesc("create_time");
        return Result.success(operationLogMapper.selectList(queryWrapper));
    }

    @PostMapping("/equip")
    public Result<User> equipDecoration(@RequestBody Map<String, Object> body) {
        Long userId = Long.valueOf(body.get("userId").toString());
        String type = (String) body.get("type"); // avatarFrame, skin, badge
        String value = (String) body.get("value"); // name or id or url

        User user = userMapper.selectById(userId);
        if (user == null) {
            return Result.error(404, "User not found");
        }

        switch (type) {
            case "avatarFrame":
                user.setCurrentAvatarFrame(value);
                break;
            case "skin":
                user.setCurrentSkin(value);
                break;
            case "badge":
                user.setCurrentBadge(value);
                break;
            default:
                return Result.error(400, "Invalid decoration type");
        }

        userMapper.updateById(user);
        user.setPassword(null);
        return Result.success(user);
    }
}
