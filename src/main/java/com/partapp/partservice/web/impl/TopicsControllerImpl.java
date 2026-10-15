package com.partapp.partservice.web.impl;

import java.util.*;
import java.time.Instant;
import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Service;
import org.springframework.web.context.request.RequestContextHolder;
import org.springframework.web.context.request.ServletRequestAttributes;

import com.partapp.partservice.api.Requests;
import com.partapp.partservice.persistence.repository.InvitationRepository;
import com.partapp.partservice.persistence.repository.RoomRepository;
import com.partapp.partservice.persistence.repository.TopicRepository;
import com.partapp.partservice.persistence.repository.UserRepository;
import com.partapp.partservice.persistence.repository.InvitationRepository.Invitation;
import com.partapp.partservice.persistence.repository.RoomRepository.Room;
import com.partapp.partservice.persistence.repository.TopicRepository.Topic;
import com.partapp.partservice.persistence.repository.UserRepository.User;
import com.partapp.partservice.api.PageResponse;
import com.partapp.partservice.web.ApiException;
import com.partapp.partservice.web.generated.api.TopicsApiDelegate;
import com.partapp.partservice.web.generated.model.*;

@Service
class TopicsControllerImpl implements TopicsApiDelegate {
  private final UserRepository userRepo;
  private final TopicRepository topicRepo;
  private final RoomRepository roomRepo;
  private final InvitationRepository invitationRepo;

  TopicsControllerImpl(UserRepository userRepo, TopicRepository topicRepo, RoomRepository roomRepo,
      InvitationRepository invitationRepo) {
    this.userRepo = userRepo;
    this.topicRepo = topicRepo;
    this.roomRepo = roomRepo;
    this.invitationRepo = invitationRepo;
  }

  protected String authorization() {
    ServletRequestAttributes a = (ServletRequestAttributes) RequestContextHolder.getRequestAttributes();
    return a == null ? null : a.getRequest().getHeader("Authorization");
  }

  protected String requestId() {
    ServletRequestAttributes attributes = (ServletRequestAttributes) RequestContextHolder.getRequestAttributes();
    Object id = attributes == null ? null : attributes.getRequest().getAttribute("requestId");
    return id == null ? "" : id.toString();
  }

  public Topic createTopic(String bearer, Requests.Topic r) {
    User u = userRepo.current(bearer);
    topicRepo.validateConstraints(r);
    Topic t = new Topic(UUID.randomUUID(), u.id(), u.communityId(), r.title(), r.detail(), r.minAge(), r.maxAge(),
        r.genders() == null ? Set.of() : Set.copyOf(r.genders()), r.expiresAt(), "open", Instant.now());
    topicRepo.putTopic(t.id(), t);
    Room room = new Room(UUID.randomUUID(), t.id(), u.id(), new HashSet<>(Set.of(u.id())), Instant.now());
    roomRepo.putRoom(room.id(), room);
    return t;
  }

  public Topic closeTopic(String bearer, UUID id) {
    User u = userRepo.current(bearer);
    Topic t = topic(bearer, id);
    if (!t.publisherId().equals(u.id()))
      throw ApiException.forbidden("只有发布者可以关闭主题");
    if (!t.status().equals("open"))
      return t;
    Topic n = new Topic(t.id(), t.publisherId(), t.communityId(), t.title(), t.detail(), t.minAge(), t.maxAge(),
        t.genders(), t.expiresAt(), "closed", t.createdAt());
    topicRepo.putTopic(id, n);
    return n;
  }

  public Topic updateTopic(String bearer, UUID id, Requests.Topic r) {
    User u = userRepo.current(bearer);
    Topic old = topic(bearer, id);
    if (!old.publisherId().equals(u.id()))
      throw ApiException.forbidden("只有发布者可以修改主题");
    topicRepo.validateConstraints(r);
    Topic n = new Topic(old.id(), old.publisherId(), old.communityId(), r.title(), r.detail(), r.minAge(), r.maxAge(),
        r.genders() == null ? Set.of() : Set.copyOf(r.genders()), r.expiresAt(), old.status(), old.createdAt());
    topicRepo.putTopic(id, n);
    return n;
  }

  public PageResponse<Topic> listTopics(String bearer, String status) {
    User u = userRepo.current(bearer);
    List<Topic> list = topicRepo.allTopics().stream().filter(t -> t.communityId().equals(u.communityId()))
        .filter(t -> status == null || t.status().equals(status)).filter(t -> topicRepo.matches(t, u))
        .sorted(Comparator.comparing(Topic::createdAt).reversed()).toList();
    return new PageResponse<>(list, null, false);
  }

  public Topic topic(String bearer, UUID id) {
    User u = userRepo.current(bearer);
    Topic t = topicRepo.getTopic(id);
    if (t == null || !t.communityId().equals(u.communityId()))
      throw ApiException.notFound("主题不存在");
    return t;
  }

  public Invitation invite(String bearer, UUID topicId, UUID invitee) {
    User u = userRepo.current(bearer);
    Topic t = topic(bearer, topicId);
    if (!t.publisherId().equals(u.id()))
      throw ApiException.forbidden("只有发布者可以邀请");
    User target = userRepo.getUser(invitee);
    if (invitee.equals(u.id()) || target == null || !target.communityId().equals(u.communityId())
        || !topicRepo.matches(t, target))
      throw ApiException.bad("邀请对象不满足主题条件");
    Invitation old = invitationRepo.allInvitations().stream()
        .filter(i -> i.topicId().equals(topicId) && i.inviteeId().equals(invitee)).findFirst().orElse(null);
    if (old != null)
      return old;
    Invitation i = new Invitation(UUID.randomUUID(), topicId, invitee, u.id(), "pending", null);
    invitationRepo.putInvitation(i.id(), i);
    return i;
  }

  private Requests.Topic request(TopicRequest x) {
    return new Requests.Topic(x.getTitle(), x.getDetail(), x.getMinAge(), x.getMaxAge(),
        x.getGenders() == null ? null : Set.copyOf(x.getGenders()),
        x.getExpiresAt() == null ? null : x.getExpiresAt().toInstant());
  }

  @Override
  public ResponseEntity<ApiResponseTopic> createTopic(TopicRequest x) {
    return ResponseEntity.ok(new ApiResponseTopic(0, "ok", ApiModelMapper.topic(createTopic(authorization(), request(x))), requestId()));
  }

  @Override
  public ResponseEntity<ApiResponseTopics> listTopics(String status, String cursor, Integer limit) {
    return ResponseEntity.ok(new ApiResponseTopics(0, "ok", ApiModelMapper.topics(listTopics(authorization(), status)), requestId()));
  }

  @Override
  public ResponseEntity<ApiResponseTopic> getTopic(UUID id) {
    return ResponseEntity.ok(new ApiResponseTopic(0, "ok", ApiModelMapper.topic(topic(authorization(), id)), requestId()));
  }

  @Override
  public ResponseEntity<ApiResponseTopic> closeTopic(UUID id) {
    return ResponseEntity.ok(new ApiResponseTopic(0, "ok", ApiModelMapper.topic(closeTopic(authorization(), id)), requestId()));
  }

  @Override
  public ResponseEntity<ApiResponseInvitation> inviteUser(UUID id, InviteRequest x) {
    return ResponseEntity.ok(new ApiResponseInvitation(0, "ok", ApiModelMapper.invitation(invite(authorization(), id, x.getUserId())), requestId()));
  }

  @Override
  public ResponseEntity<ApiResponseTopic> updateTopic(UUID id, TopicRequest x) {
    return ResponseEntity.ok(new ApiResponseTopic(0, "ok", ApiModelMapper.topic(updateTopic(authorization(), id, request(x))), requestId()));
  }
}
