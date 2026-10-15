package com.partapp.partservice.persistence.entity;
import java.time.Instant; import java.util.UUID; import com.baomidou.mybatisplus.annotation.*;
@TableName("chat_room") public class ChatRoomEntity { @TableId public UUID id; @TableField("topic_id") public UUID topicId; public String status; @TableField("created_at") public Instant createdAt; }
