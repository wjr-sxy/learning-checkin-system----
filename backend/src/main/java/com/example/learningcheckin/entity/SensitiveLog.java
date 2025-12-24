package com.example.learningcheckin.entity;

import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import lombok.Data;
import java.time.LocalDateTime;

@Data
@TableName("sys_sensitive_log")
public class SensitiveLog {
    @TableId(type = IdType.AUTO)
    private Long id;
    private Long userId;
    private String contentSnippet;
    private String detectedWords;
    private String sourceType; // TASK_SUBMISSION, COMMENT
    private Long sourceId;
    private LocalDateTime createTime;
}
