package com.partapp.partservice.persistence.mapper;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.partapp.partservice.persistence.entity.TopicEntity;
import org.apache.ibatis.annotations.Mapper;

@Mapper
public interface TopicMapper extends BaseMapper<TopicEntity> {
}
