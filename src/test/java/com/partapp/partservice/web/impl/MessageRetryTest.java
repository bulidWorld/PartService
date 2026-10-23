package com.partapp.partservice.web.impl;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

import java.time.temporal.ChronoUnit;
import java.util.Map;
import java.util.Set;
import java.util.UUID;
import java.util.concurrent.atomic.AtomicReference;

import com.partapp.partservice.api.Requests;
import com.partapp.partservice.persistence.entity.ChatMessageEntity;
import com.partapp.partservice.persistence.mapper.ChatMessageMapper;
import com.partapp.partservice.persistence.repository.MessageRepository;
import com.partapp.partservice.persistence.repository.RoomRepository;
import com.partapp.partservice.persistence.repository.UserRepository;
import org.junit.jupiter.api.Test;

class MessageRetryTest {
  @Test
  void firstSendUsesWholeSecondsWithoutReadingBackAndMatchesRetryAndHistory() {
    UUID sender = UUID.randomUUID();
    UUID roomId = UUID.randomUUID();
    var users = mock(UserRepository.class);
    var rooms = mock(RoomRepository.class);
    var mapper = mock(ChatMessageMapper.class);
    var stored = new AtomicReference<ChatMessageEntity>();
    when(users.authenticate("token")).thenReturn(sender);
    when(rooms.getRoom(roomId)).thenReturn(
        new RoomRepository.Room(roomId, UUID.randomUUID(), sender, Set.of(sender), null));
    when(mapper.selectById(any(UUID.class))).thenAnswer(call -> stored.get());
    when(mapper.selectOne(any())).thenAnswer(call -> stored.get());
    when(mapper.insert(any(ChatMessageEntity.class))).thenAnswer(call -> {
      ChatMessageEntity entity = call.getArgument(0);
      assertEquals(0, entity.createdAt.getNano());
      // Model PostgreSQL timestamptz(0), which rounds rather than truncates.
      entity.createdAt = entity.createdAt.plusMillis(500).truncatedTo(ChronoUnit.SECONDS);
      stored.set(entity);
      return 1;
    });
    when(mapper.selectList(any())).thenAnswer(call -> java.util.List.of(stored.get()));
    var controller = new RoomsControllerImpl(users, rooms, new MessageRepository(mapper));
    var request = new Requests.Message("case-msg-M", "text", "大家好，准时集合。", Map.of());

    var first = controller.message("token", roomId, request);
    // The repository checks existence before insert; sending adds no readback.
    verify(mapper, times(1)).selectById(first.id());
    assertEquals(0, first.createdAt().getNano());
    assertEquals(stored.get().createdAt, first.createdAt());
    var retry = controller.message("token", roomId, request);
    var history = controller.messages("token", roomId).items();

    assertEquals(stored.get().createdAt, first.createdAt());
    assertEquals(first, retry);
    assertEquals(java.util.List.of(first), history);
    verify(mapper, times(1)).insert(any(ChatMessageEntity.class));
  }
}
