package com.example.learningcheckin.entity;

import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import lombok.Data;

@Data
@TableName("sys_config")
public class Config {
    @TableId(type = IdType.AUTO)
    private Long id;
    private String keyName;
    private String keyValue;
    private String description;
}
