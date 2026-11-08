package com.partapp.partservice.persistence.repository;

import java.time.*;
import java.util.*;
import com.baomidou.mybatisplus.core.conditions.query.QueryWrapper;
import com.partapp.partservice.persistence.mapper.ChatMessageMapper;
import com.partapp.partservice.persistence.entity.ChatMessageEntity;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.springframework.stereotype.Component;

@Component
public class MessageRepository {
    public record Message(UUID id, UUID roomId, UUID senderId, String clientMessageId, String messageType,
            String content, Map<String, Object> contentMeta, Instant createdAt, boolean recalled) {
    }

    private final ChatMessageMapper mapper;
    private final ObjectMapper json = new ObjectMapper().findAndRegisterModules();

    public MessageRepository(ChatMessageMapper m) {
        mapper = m;
    }

    public Message getMessages(UUID id) {
        return from(mapper.selectById(id));
    }

    public void putMessages(UUID id, Message v) {
        try {
            var e = new ChatMessageEntity();
            e.id = v.id();
            e.roomId = v.roomId();
            e.senderId = v.senderId();
            e.clientMessageId = v.clientMessageId();
            e.messageType = v.messageType();
            e.content = v.content();
            e.contentMeta = json.writeValueAsString(v.contentMeta());
            e.createdAt = v.createdAt();
            e.recalledAt = v.recalled() ? Instant.now() : null;
            if (mapper.selectById(id) == null)
                mapper.insert(e);
            else
                mapper.updateById(e);
        } catch (Exception x) {
            throw new IllegalStateException(x);
        }
    }

    public List<Message> allMessages() {
        return mapper.selectList(null).stream().map(this::from).toList();
    }

    public UUID getMessageKeys(String key) {
        var e = mapper.selectOne(new QueryWrapper<ChatMessageEntity>()
                .eq("sender_id", UUID.fromString(key.split(":", 2)[0])).eq("client_message_id", key.split(":", 2)[1]));
        return e == null ? null : e.id;
    }

    public void putMessageKeys(String key, UUID id) {
    }

    private Message from(ChatMessageEntity e) {
        if (e == null)
            return null;
        try {
            return new Message(e.id, e.roomId, e.senderId, e.clientMessageId, e.messageType, e.content,
                    json.readValue(e.contentMeta, Map.class), e.createdAt, e.recalledAt != null);
        } catch (Exception x) {
            throw new IllegalStateException(x);
        }
    }
}
