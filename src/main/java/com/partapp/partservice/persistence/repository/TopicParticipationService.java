package com.partapp.partservice.persistence.repository;

import java.time.*;
import java.util.*;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import com.partapp.partservice.web.ApiException;
import com.partapp.partservice.web.generated.model.TopicReceipt;
import com.partapp.partservice.api.PageResponse;

/** Durable topic inbox. Database topic locks serialize acceptance and closure across instances. */
@Service
public class TopicParticipationService {
  private final JdbcTemplate db;
  private final UserRepository users;
  private final TopicRepository topics;
  public TopicParticipationService(JdbcTemplate db, UserRepository users, TopicRepository topics) {
    this.db = db; this.users = users; this.topics = topics;
  }

  public void lock(UUID topicId) {
    if (db.queryForList("SELECT id FROM topic WHERE id = ? FOR UPDATE", topicId).isEmpty())
      throw ApiException.notFound("主题不存在");
  }

  /** Persisting the inbox in the publish transaction makes delivery queryable without an external push provider. */
  public void distribute(TopicRepository.Topic topic) {
    db.query("SELECT id FROM \"user\" WHERE community_id = ? AND status = 'active' AND id <> ?",
        (rs, row) -> rs.getObject(1, UUID.class), topic.communityId(), topic.publisherId()).forEach(id -> {
      var user = users.findById(id).orElseThrow();
      if (topics.matches(topic, user))
        db.update("INSERT INTO topic_recipient (topic_id,user_id) VALUES (?,?) ON CONFLICT (topic_id,user_id) DO NOTHING", topic.id(), id);
    });
  }

  private TopicReceipt receipt(UUID topicId, UUID userId) {
    var rows = db.query("""
        SELECT r.topic_id, r.created_at, r.read_at,
          CASE WHEN r.accepted_at IS NOT NULL THEN 'accepted'
               WHEN t.status <> 'open' OR t.expires_at <= now() THEN 'expired' ELSE 'pending' END AS status,
          CASE WHEN r.accepted_at IS NOT NULL THEN c.id END AS room_id
        FROM topic_recipient r JOIN topic t ON t.id=r.topic_id
        LEFT JOIN chat_room c ON c.topic_id=t.id WHERE r.topic_id=? AND r.user_id=?
        """, (rs, row) -> new TopicReceipt(rs.getObject("topic_id", UUID.class),
            TopicReceipt.StatusEnum.fromValue(rs.getString("status")), rs.getObject("created_at", OffsetDateTime.class))
            .roomId(rs.getObject("room_id", UUID.class)).readAt(rs.getObject("read_at", OffsetDateTime.class)), topicId, userId);
    if (rows.isEmpty()) throw ApiException.notFound("主题收件记录不存在");
    return rows.getFirst();
  }

  @Transactional
  public TopicReceipt accept(String bearer, UUID topicId) {
    var user = users.current(bearer);
    lock(topicId);
    var result = receipt(topicId, user.id());
    if (result.getStatus() == TopicReceipt.StatusEnum.ACCEPTED) return result;
    var topic = topics.getTopic(topicId);
    if (!topic.communityId().equals(user.communityId()) || !topics.matches(topic, user))
      throw ApiException.forbidden("不满足主题参与条件");
    if (topic.publisherId().equals(user.id())) throw ApiException.bad("不能接受自己的主题");
    if (result.getStatus() != TopicReceipt.StatusEnum.PENDING) throw ApiException.conflict("主题已关闭或过期");
    users.findById(topic.publisherId()).filter(u -> u.status().equals("active"))
        .orElseThrow(() -> ApiException.conflict("主题发布者不可用"));
    db.update("INSERT INTO chat_room (id,topic_id) VALUES (?,?) ON CONFLICT (topic_id) DO NOTHING", UUID.randomUUID(), topicId);
    UUID room = db.queryForObject("SELECT id FROM chat_room WHERE topic_id=?", UUID.class, topicId);
    for (UUID id : List.of(topic.publisherId(), user.id())) {
      boolean owner = id.equals(topic.publisherId());
      db.update("INSERT INTO topic_participant (topic_id,user_id,source) VALUES (?,?,?) ON CONFLICT DO NOTHING", topicId,id,owner ? "publisher" : "acceptance");
      db.update("INSERT INTO chat_member (room_id,user_id,role) VALUES (?,?,?) ON CONFLICT DO NOTHING", room,id,owner ? "owner" : "member");
    }
    db.update("UPDATE topic_recipient SET accepted_at=now() WHERE topic_id=? AND user_id=?",topicId,user.id());
    return receipt(topicId,user.id());
  }

  @Transactional
  public TopicReceipt read(String bearer, UUID topicId) {
    var user = users.current(bearer);
    var topic = topics.getTopic(topicId);
    if (topic == null || !topic.communityId().equals(user.communityId())) throw ApiException.notFound("主题不存在");
    db.update("UPDATE topic_recipient SET read_at=COALESCE(read_at,now()) WHERE topic_id=? AND user_id=?",topicId,user.id());
    return receipt(topicId,user.id());
  }

  @Transactional
  public PageResponse<TopicReceipt> inbox(String bearer, String status, String cursor, Integer limit) {
    var user = users.current(bearer);
    int size = limit == null ? 20 : limit;
    if (size < 1 || size > 100 || (status != null && !Set.of("pending","accepted","expired").contains(status)))
      throw ApiException.bad("分页或状态参数无效");
    UUID after = null;
    try { if (cursor != null) after = UUID.fromString(cursor); }
    catch (IllegalArgumentException e) { throw ApiException.bad("游标无效"); }
    var ids = db.query("""
        SELECT r.topic_id FROM topic_recipient r JOIN topic t ON t.id=r.topic_id
        WHERE r.user_id=? AND t.community_id=? AND (CAST(? AS uuid) IS NULL OR r.topic_id > CAST(? AS uuid))
        AND (CAST(? AS text) IS NULL OR
          CASE WHEN r.accepted_at IS NOT NULL THEN 'accepted'
          WHEN t.status <> 'open' OR t.expires_at <= now() THEN 'expired' ELSE 'pending' END = ?)
        ORDER BY r.topic_id LIMIT ?
        """, (rs,row) -> rs.getObject(1,UUID.class),user.id(),user.communityId(),after,after,status,status,size+1);
    boolean more=ids.size()>size;
    var page=ids.stream().limit(size).map(id -> receipt(id,user.id())).toList();
    return new PageResponse<>(page,more ? page.getLast().getTopicId().toString() : null,more);
  }
}
