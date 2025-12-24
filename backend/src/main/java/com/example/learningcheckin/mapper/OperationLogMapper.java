package com.example.learningcheckin.mapper;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.example.learningcheckin.entity.OperationLog;
import org.apache.ibatis.annotations.Mapper;

@Mapper
public interface OperationLogMapper extends BaseMapper<OperationLog> {
}
