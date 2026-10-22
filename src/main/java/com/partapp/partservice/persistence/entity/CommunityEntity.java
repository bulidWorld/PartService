package com.partapp.partservice.persistence.entity;

import java.util.UUID;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;

@TableName("community")
public class CommunityEntity {
  @TableId
  public UUID id;
  public String name;
  public String status;
}
