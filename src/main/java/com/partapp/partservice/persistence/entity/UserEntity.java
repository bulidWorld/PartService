package com.partapp.partservice.persistence.entity;

import java.time.*;
import java.util.UUID;
import com.baomidou.mybatisplus.annotation.*;

@TableName(value = "\"user\"", keepGlobalPrefix = true)
public class UserEntity {
    @TableId(type = IdType.INPUT)
    public UUID id;
    public String nickname;
    @TableField(value = "community_id", updateStrategy = FieldStrategy.ALWAYS)
    public UUID communityId;
    @TableField(updateStrategy = FieldStrategy.ALWAYS)
    public String gender;
    @TableField(value = "birth_date", updateStrategy = FieldStrategy.ALWAYS)
    public LocalDate birthDate;
    @TableField(value = "avatar_url", updateStrategy = FieldStrategy.ALWAYS)
    public String avatarUrl;
    @TableField(updateStrategy = FieldStrategy.ALWAYS)
    public String status;
    @TableField(value = "community_changed_at", updateStrategy = FieldStrategy.ALWAYS)
    public Instant communityChangedAt;
}
