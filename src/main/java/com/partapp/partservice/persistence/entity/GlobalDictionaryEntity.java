package com.partapp.partservice.persistence.entity;

import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableField;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;

@TableName("global_dictionary")
public class GlobalDictionaryEntity {
  @TableId(type = IdType.AUTO)
  public Integer id;
  @TableField("\"key\"")
  public String key;
  @TableField("\"value\"")
  public String value;
  @TableField("\"desc\"")
  public String desc;
}
