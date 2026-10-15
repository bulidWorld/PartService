package com.partapp.partservice.persistence.entity;

import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import com.baomidou.mybatisplus.annotation.TableField;
import com.partapp.partservice.persistence.typehandler.JsonbStringTypeHandler;

@TableName(value = "service_state", autoResultMap = true)
public class ServiceStateEntity {
  @TableId
  private String namespace;
  @TableField("state_key")
  private String stateKey;
  @TableField(value = "state_value", typeHandler = JsonbStringTypeHandler.class)
  private String stateValue;

  public String getNamespace() {
    return namespace;
  }

  public void setNamespace(String v) {
    namespace = v;
  }

  public String getStateKey() {
    return stateKey;
  }

  public void setStateKey(String v) {
    stateKey = v;
  }

  public String getStateValue() {
    return stateValue;
  }

  public void setStateValue(String v) {
    stateValue = v;
  }
}
