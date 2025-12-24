package com.example.learningcheckin.entity;

import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import lombok.Data;
import java.time.LocalDateTime;

@Data
@TableName("sys_message")
public class Message {
    @TableId(type = IdType.AUTO)
    private Long id;
    private Long senderId; // 0 for System
    private Long receiverId;
    private String title;
    private String content;
    private Integer type; // 0: System, 1: Private, 2: Remind
    private Integer isRead; // 0: No, 1: Yes
    private LocalDateTime createTime;
}
