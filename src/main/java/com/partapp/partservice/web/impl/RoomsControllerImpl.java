package com.partapp.partservice.web.impl;

import java.util.*;
import java.time.Instant;
import java.time.temporal.ChronoUnit;
import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Service;
import org.springframework.web.context.request.RequestContextHolder;
import org.springframework.web.context.request.ServletRequestAttributes;

import com.partapp.partservice.api.Requests;
import com.partapp.partservice.persistence.repository.MessageRepository;
import com.partapp.partservice.persistence.repository.RoomRepository;
import com.partapp.partservice.persistence.repository.UserRepository;
import com.partapp.partservice.persistence.repository.MessageRepository.Message;
import com.partapp.partservice.persistence.repository.RoomRepository.Room;
import com.partapp.partservice.api.PageResponse;
import com.partapp.partservice.web.ApiException;
import com.partapp.partservice.web.generated.api.RoomsApiDelegate;
import com.partapp.partservice.web.generated.model.*;

@Service
class RoomsControllerImpl implements RoomsApiDelegate {
  private final UserRepository userRepo;
  private final RoomRepository roomRepo;
  private final MessageRepository messageRepo;
  RoomsControllerImpl(UserRepository userRepo, RoomRepository roomRepo, MessageRepository messageRepo) {
    this.userRepo = userRepo;
    this.roomRepo = roomRepo;
    this.messageRepo = messageRepo;
  }
  protected String authorization() {
    ServletRequestAttributes a=(ServletRequestAttributes)RequestContextHolder.getRequestAttributes(); return a==null?null:a.getRequest().getHeader("Authorization");
  }
  protected String requestId() {
    ServletRequestAttributes attributes = (ServletRequestAttributes) RequestContextHolder.getRequestAttributes();
    Object id = attributes == null ? null : attributes.getRequest().getAttribute("requestId");
    return id == null ? "" : id.toString();
  }

  public PageResponse<Room> rooms(String bearer) {
    UUID id = userRepo.authenticate(bearer);
    return new PageResponse<>(roomRepo.allRooms().stream().filter(r -> r.members().contains(id)).toList(), null, false);
  }

  public Room room(String bearer, UUID id) {
    UUID uid = userRepo.authenticate(bearer);
    Room r = roomRepo.getRoom(id);
    if (r == null || !r.members().contains(uid))
      throw ApiException.forbidden("不是聊天室成员");
    return r;
  }

  public Message message(String bearer, UUID roomId, Requests.Message r) {
    UUID uid = userRepo.authenticate(bearer);
    Room room = room(bearer, roomId);
    String key = uid + ":" + r.clientMessageId();
    UUID old = messageRepo.getMessageKeys(key);
    if (old != null)
      return messageRepo.getMessages(old);
    if (!Set.of("text", "image", "system").contains(r.messageType()))
      throw ApiException.bad("message_type 不支持");
    if (r.content() == null || r.content().isBlank())
      throw ApiException.bad("消息内容不能为空");
    Message m = new Message(UUID.randomUUID(), room.id(), uid, r.clientMessageId(), r.messageType(), r.content(),
        r.contentMeta() == null ? Map.of() : r.contentMeta(), Instant.now().truncatedTo(ChronoUnit.SECONDS), false);
    messageRepo.putMessages(m.id(), m);
    messageRepo.putMessageKeys(key, m.id());
    return m;
  }

  public PageResponse<Message> messages(String bearer, UUID roomId) {
    room(bearer, roomId);
    return new PageResponse<>(messageRepo.allMessages().stream().filter(m -> m.roomId().equals(roomId))
        .sorted(Comparator.comparing(Message::createdAt)).toList(), null, false);
  }

  public Message recall(String bearer, UUID id) {
    UUID uid = userRepo.authenticate(bearer);
    Message m = messageRepo.getMessages(id);
    if (m == null || !m.senderId().equals(uid))
      throw ApiException.forbidden("只能撤回自己的消息");
    Message n = new Message(m.id(), m.roomId(), m.senderId(), m.clientMessageId(), m.messageType(), m.content(),
        m.contentMeta(), m.createdAt(), true);
    messageRepo.putMessages(id, n);
    return n;
  }
  @Override
  public ResponseEntity<ApiResponseRooms> listRooms() {
    return ResponseEntity.ok(new ApiResponseRooms(0, "ok", ApiModelMapper.rooms(rooms(authorization())), requestId()));
  }
  @Override
  public ResponseEntity<ApiResponseRoom> getRoom(UUID id) {
    return ResponseEntity.ok(new ApiResponseRoom(0, "ok", ApiModelMapper.room(room(authorization(),id)), requestId()));
  }
  @Override
  public ResponseEntity<ApiResponseMessages> listMessages(UUID id,UUID before,Integer limit) {
    return ResponseEntity.ok(new ApiResponseMessages(0, "ok", ApiModelMapper.messages(messages(authorization(),id)), requestId()));
  }
  @Override
  public ResponseEntity<ApiResponseMessage> sendMessage(UUID id,MessageRequest x) {
    return ResponseEntity.ok(new ApiResponseMessage(0, "ok", ApiModelMapper.message(message(authorization(),id,new Requests.Message(x.getClientMessageId(),x.getMessageType(),x.getContent(),x.getContentMeta()))), requestId()));
  }
  @Override
  public ResponseEntity<ApiResponseMessage> recallMessage(UUID roomId,UUID messageId) {
    return ResponseEntity.ok(new ApiResponseMessage(0, "ok", ApiModelMapper.message(recall(authorization(),messageId)), requestId()));
  }
  @Override
  public ResponseEntity<ApiResponseRead> markRoomRead(UUID roomId,ReadRequest x) {
    return ResponseEntity.ok(new ApiResponseRead(0, "ok", new ReadPosition(roomId, x.getLastReadMessageId()), requestId()));
  }
}
