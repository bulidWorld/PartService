package com.partapp.partservice.web.impl;

import java.util.*;
import java.time.Instant;
import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Service;
import org.springframework.web.context.request.RequestContextHolder;
import org.springframework.web.context.request.ServletRequestAttributes;

import com.partapp.partservice.api.Requests;
import com.partapp.partservice.persistence.repository.TopicParticipationService;
import com.partapp.partservice.persistence.repository.TopicRepository;
import com.partapp.partservice.persistence.repository.UserRepository;
import com.partapp.partservice.persistence.repository.TopicRepository.Topic;
import com.partapp.partservice.persistence.repository.UserRepository.User;
import com.partapp.partservice.api.PageResponse;
import com.partapp.partservice.web.ApiException;
import com.partapp.partservice.web.generated.api.TopicsApiDelegate;
import com.partapp.partservice.web.generated.model.*;

@Service
@org.springframework.transaction.annotation.Transactional
class TopicsControllerImpl implements TopicsApiDelegate {
  private final UserRepository userRepo;
  private final TopicRepository topicRepo;
  private final TopicParticipationService participation;

  TopicsControllerImpl(UserRepository userRepo, TopicRepository topicRepo,
      TopicParticipationService participation) {
    this.userRepo = userRepo;
    this.topicRepo = topicRepo;
    this.participation = participation;
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
    participation.distribute(t);
    return t;
  }

  public Topic closeTopic(String bearer, UUID id) {
    User u = userRepo.current(bearer);
    participation.lock(id);
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
    participation.lock(id);
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
  public ResponseEntity<ApiResponseTopic> updateTopic(UUID id, TopicRequest x) {
    return ResponseEntity.ok(new ApiResponseTopic(0, "ok", ApiModelMapper.topic(updateTopic(authorization(), id, request(x))), requestId()));
  }
  @Override
  public ResponseEntity<ApiResponseTopicReceipt> acceptTopic(UUID id) {
    return ResponseEntity.ok(new ApiResponseTopicReceipt(0, "ok", participation.accept(authorization(), id), requestId()));
  }

  @Override
  public ResponseEntity<ApiResponseTopicReceipt> readTopic(UUID id) {
    return ResponseEntity.ok(new ApiResponseTopicReceipt(0, "ok", participation.read(authorization(), id), requestId()));
  }

  @Override
  public ResponseEntity<ApiResponseTopicReceipts> listTopicReceipts(String status, String cursor, Integer limit) {
    var page = participation.inbox(authorization(), status, cursor, limit);
    var data = new PageOfTopicReceipts().items(page.items()).nextCursor(page.nextCursor()).hasMore(page.hasMore());
    return ResponseEntity.ok(new ApiResponseTopicReceipts(0, "ok", data, requestId()));
  }
}
