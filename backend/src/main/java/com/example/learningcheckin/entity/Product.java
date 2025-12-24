package com.example.learningcheckin.entity;

import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import lombok.Data;
import java.time.LocalDateTime;

@Data
@TableName("sys_product")
public class Product {
    @TableId(type = IdType.AUTO)
    private Long id;
    private String name;
    private String description;
    private Integer price;
    private String imageUrl;
    private Integer stock;
    private String type;
    private LocalDateTime createTime;
}
