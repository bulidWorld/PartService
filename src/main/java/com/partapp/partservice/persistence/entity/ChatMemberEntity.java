package com.partapp.partservice.persistence.entity;
import java.util.UUID; import com.baomidou.mybatisplus.annotation.*;
@TableName("chat_member") public class ChatMemberEntity { @TableField("room_id") public UUID roomId; @TableField("user_id") public UUID userId; }
