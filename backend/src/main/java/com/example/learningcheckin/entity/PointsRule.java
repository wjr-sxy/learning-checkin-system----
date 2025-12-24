package com.example.learningcheckin.entity;

import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import lombok.Data;
import java.time.LocalDateTime;

@Data
@TableName("sys_points_rule")
public class PointsRule {
    @TableId(type = IdType.AUTO)
    private Long id;
    private String ruleKey;
    private String ruleValue;
    private String description;
    private LocalDateTime updateTime;
}
