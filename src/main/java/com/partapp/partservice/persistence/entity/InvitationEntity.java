package com.partapp.partservice.persistence.entity;
import java.util.UUID; import com.baomidou.mybatisplus.annotation.*;
@TableName("topic_invitation") public class InvitationEntity { @TableId public UUID id; @TableField("topic_id") public UUID topicId; @TableField("invitee_id") public UUID inviteeId; @TableField("inviter_id") public UUID inviterId; public String status; @TableField("created_at") public java.time.Instant createdAt; }
