package com.partapp.partservice.persistence.repository;

import java.time.*;
import java.util.*;

import com.partapp.partservice.persistence.mapper.TopicMapper;
import com.partapp.partservice.persistence.entity.TopicEntity;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.springframework.stereotype.Component;

import com.partapp.partservice.api.Requests;
import com.partapp.partservice.web.ApiException;

/** PostgreSQL-backed persistence for topics. */
@Component
public class TopicRepository {
  public record Topic(UUID id, UUID publisherId, UUID communityId, String title, String detail, Integer minAge,
      Integer maxAge, Set<String> genders, Instant expiresAt, String status, Instant createdAt) {
  }

  private final TopicMapper mapper;
  private final ObjectMapper json = new ObjectMapper().findAndRegisterModules();

  public TopicRepository(TopicMapper mapper) {
    this.mapper = mapper;
  }

  public void validateConstraints(Requests.Topic r) {
    if (r.minAge() != null && (r.minAge() < 0 || r.minAge() > 150))
      throw ApiException.bad("最小年龄无效");
    if (r.maxAge() != null && (r.maxAge() < 0 || r.maxAge() > 150))
      throw ApiException.bad("最大年龄无效");
    if (r.minAge() != null && r.maxAge() != null && r.minAge() > r.maxAge())
      throw ApiException.bad("年龄范围无效");
    if (r.genders() != null && !Set.of("male", "female", "other").containsAll(r.genders()))
      throw ApiException.bad("性别枚举无效");
  }

  public boolean matches(Topic t, UserRepository.User u) {
    if (t.minAge() == null && t.maxAge() == null && t.genders().isEmpty())
      return true;
    if (u.birthDate() == null)
      return false;
    int age = Period.between(u.birthDate(), LocalDate.now()).getYears();
    return (t.minAge() == null || age >= t.minAge()) && (t.maxAge() == null || age <= t.maxAge())
        && (t.genders().isEmpty() || t.genders().contains(u.gender()));
  }

  public Topic getTopic(UUID id) {
    return from(mapper.selectById(id));
  }

  public void putTopic(UUID id, Topic t) {
    try {
      TopicEntity e = new TopicEntity();
      e.id = t.id();
      e.publisherId = t.publisherId();
      e.communityId = t.communityId();
      e.title = t.title();
      e.detail = t.detail();
      e.status = t.status();
      e.expiresAt = t.expiresAt();
      e.createdAt = t.createdAt();
      e.constraintJson = json.writeValueAsString(Map.of("minAge", t.minAge() == null ? 0 : t.minAge(), "maxAge",
          t.maxAge() == null ? 0 : t.maxAge(), "genders", t.genders()));
      if (mapper.selectById(id) == null)
        mapper.insert(e);
      else
        mapper.updateById(e);
    } catch (Exception ex) {
      throw new IllegalStateException(ex);
    }
  }

  public List<Topic> allTopics() {
    return mapper.selectList(null).stream().map(this::from).toList();
  }

  private Topic from(TopicEntity e) {
    if (e == null)
      return null;
    try {
      var n = json.readTree(e.constraintJson);
      Integer min = n.path("minAge").asInt() == 0 ? null : n.path("minAge").asInt();
      Integer max = n.path("maxAge").asInt() == 0 ? null : n.path("maxAge").asInt();
      Set<String> g = new HashSet<>();
      n.path("genders").forEach(x -> g.add(x.asText()));
      return new Topic(e.id, e.publisherId, e.communityId, e.title, e.detail, min, max, g, e.expiresAt, e.status,
          e.createdAt);
    } catch (Exception ex) {
      throw new IllegalStateException(ex);
    }
  }
}
