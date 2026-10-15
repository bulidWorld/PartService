package com.partapp.partservice.persistence.entity;
import java.util.UUID; import com.baomidou.mybatisplus.annotation.*;
@TableName("user_identity")
public class UserIdentityEntity { @TableId(type=IdType.INPUT) public UUID id; @TableField("user_id") public UUID userId; @TableField("identity_type") public String identityType; @TableField("identity_value") public String identityValue; @TableField("is_primary") public boolean primary; }
