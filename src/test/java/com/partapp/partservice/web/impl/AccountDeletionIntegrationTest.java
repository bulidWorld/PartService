package com.partapp.partservice.web.impl;

import static org.junit.jupiter.api.Assertions.*;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

import java.time.Instant;
import java.util.LinkedHashMap;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.UUID;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.partapp.partservice.persistence.repository.AccountDeletionService;
import com.partapp.partservice.persistence.repository.UserRepository;
import com.partapp.partservice.persistence.repository.UserRepository.TokenPair;
import com.partapp.partservice.persistence.repository.UserRepository.User;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.condition.EnabledIfSystemProperty;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.test.web.servlet.MockMvc;

/** Opt in with -DPART_ACCOUNT_TEST_URL pointing to an isolated database initialized from the XML schema. */
@EnabledIfSystemProperty(named = "PART_ACCOUNT_TEST_URL", matches = ".+")
@SpringBootTest(properties = {
    "spring.datasource.url=${PART_ACCOUNT_TEST_URL}",
    "spring.datasource.username=${PART_ACCOUNT_TEST_USER:account_test}",
    "spring.datasource.password=${PART_ACCOUNT_TEST_PASSWORD:}",
    "logging.file.name=target/account-deletion-test.log"
})
@AutoConfigureMockMvc
class AccountDeletionIntegrationTest {
  @Autowired MockMvc http;
  @Autowired JdbcTemplate db;
  @Autowired UserRepository users;
  @Autowired AccountDeletionService deletion;
  @Autowired ObjectMapper json;

  private final Map<UUID, TokenPair> created = new LinkedHashMap<>();
  private final List<UUID> createdRooms = new ArrayList<>();
  private UUID community;

  @AfterEach
  void cleanup() {
    for (var entry : created.entrySet()) {
      if (users.findById(entry.getKey()).isPresent())
        deletion.deleteCurrentUser("Bearer " + entry.getValue().accessToken());
    }
    if (community != null) {
      // Production retains history; remove only this test's rooms during teardown.
      for (UUID id : createdRooms)
        db.update("DELETE FROM chat_room WHERE id = ?", id);
      db.update("DELETE FROM community WHERE id = ?", community);
    }
  }

  private TokenPair registerUser(String label) {
    if (community == null) {
      community = UUID.randomUUID();
      db.update("INSERT INTO community (id, name) VALUES (?, ?)", community, "delete-test-" + community);
    }
    UUID id = UUID.randomUUID();
    String email = id + "@example.com";
    users.insert(id, label, community, "email", email, null);
    User user = new User(id, label, community, null, null, null, "active", Instant.now());
    users.putUser(id, user);
    users.putIdentity(users.identityKey("email", email), id);
    TokenPair pair = users.issue(user);
    created.put(id, pair);
    return pair;
  }

  private UUID topic(UUID publisher) {
    UUID id = UUID.randomUUID();
    db.update("INSERT INTO topic (id, publisher_id, community_id, title, detail) VALUES (?, ?, ?, 'test', 'test')",
        id, publisher, community);
    return id;
  }

  private UUID room(UUID topic, UUID... members) {
    UUID id = UUID.randomUUID();
    db.update("INSERT INTO chat_room (id, topic_id) VALUES (?, ?)", id, topic);
    createdRooms.add(id);
    for (UUID member : members)
      db.update("INSERT INTO chat_member (room_id, user_id) VALUES (?, ?)", id, member);
    return id;
  }

  private UUID message(UUID room, UUID sender) {
    UUID id = UUID.randomUUID();
    db.update("INSERT INTO chat_message (id, room_id, sender_id, client_message_id, message_type, content) "
        + "VALUES (?, ?, ?, ?, 'text', 'test')", id, room, sender, id.toString());
    return id;
  }

  private UUID rating(UUID topic, UUID from, UUID to) {
    UUID id = UUID.randomUUID();
    db.update("INSERT INTO rating (id, topic_id, from_user_id, to_user_id, score) VALUES (?, ?, ?, ?, 'good')",
        id, topic, from, to);
    db.update("INSERT INTO rating_category_item (rating_id, category_id) "
        + "SELECT ?, id FROM rating_category WHERE code = 'reliable'", id);
    return id;
  }

  private int count(String table, String column, Object id) {
    return db.queryForObject("SELECT count(*) FROM \"" + table + "\" WHERE " + column + " = ?", Integer.class, id);
  }

  private void assertRevoked(TokenPair pair) throws Exception {
    http.perform(get("/api/v1/me").header("Authorization", "Bearer " + pair.accessToken()))
        .andExpect(status().isUnauthorized()).andExpect(jsonPath("$.code").value(40101));
    http.perform(post("/api/v1/auth/refresh").contentType("application/json")
        .content(json.writeValueAsString(Map.of("refreshToken", pair.refreshToken()))))
        .andExpect(status().isUnauthorized()).andExpect(jsonPath("$.code").value(40101));
  }

  @Test
  void deletesGraphAndAllSessionsWithoutDeletingOtherAccountsAndAllowsIdentityReuse() throws Exception {
    TokenPair a = registerUser("A"), b = registerUser("B"), c = registerUser("C");
    UUID aid = a.user().id(), bid = b.user().id(), cid = c.user().id();
    db.update("INSERT INTO user_identity (user_id, identity_type, identity_value) VALUES (?, 'phone', '13900000002')", aid);
    users.putIdentity("phone:13900000002", aid);
    users.putCode("phone:13900000002:reset", "246810");
    TokenPair secondSession = users.issue(a.user());
    UUID ownTopic = topic(aid), sharedTopic = topic(bid), unrelatedTopic = topic(cid);
    UUID ownRoom = room(ownTopic, aid, bid), sharedRoom = room(sharedTopic, aid, bid);
    UUID unrelatedRoom = room(unrelatedTopic, cid);
    UUID ownRoomMessage = message(ownRoom, bid);
    UUID ownMessage = message(sharedRoom, aid), peerMessage = message(sharedRoom, bid);
    message(unrelatedRoom, cid);
    db.update("UPDATE chat_member SET last_read_message_id = ? WHERE room_id = ?", ownMessage, sharedRoom);
    db.update("INSERT INTO chat_message_read (room_id, user_id, last_read_message_id) VALUES (?, ?, ?)",
        sharedRoom, bid, ownMessage);
    db.update("INSERT INTO chat_message_read (room_id, user_id) VALUES (?, ?)", sharedRoom, aid);
    for (UUID t : new UUID[] {ownTopic, sharedTopic}) {
      db.update("INSERT INTO topic_recipient (topic_id, user_id) VALUES (?, ?)", t, aid);
      db.update("INSERT INTO topic_participant (topic_id, user_id, source) VALUES (?, ?, 'acceptance')", t, aid);
    }
    rating(sharedTopic, aid, bid);
    rating(sharedTopic, bid, aid);
    rating(ownTopic, bid, cid);
    UUID retainedRating = rating(unrelatedTopic, bid, cid);
    db.update("INSERT INTO refresh_token (user_id, token_hash, expires_at) VALUES (?, ?, now() + interval '1 day')",
        aid, UUID.randomUUID().toString());
    db.update("INSERT INTO verification_code (identity_type, identity_value, purpose, code_hash, expires_at) "
        + "VALUES ('email', ?, 'register', 'test', now() + interval '1 day')", aid + "@example.com");
    db.update("INSERT INTO user_rating_stat (user_id, algorithm_version) VALUES (?, 'test')", aid);
    db.update("INSERT INTO user_risk_tag (user_id, tag_code, source) VALUES (?, 'test', 'system')", aid);
    users.putCode("email:" + aid + "@example.com:login", "246810");
    db.execute("CREATE TABLE IF NOT EXISTS audit_log_test_default PARTITION OF audit_log DEFAULT");
    db.update("INSERT INTO audit_log (request_id, actor_user_id, action, resource_type) VALUES ('test', ?, 'test', 'user')", aid);

    http.perform(delete("/api/v1/me").header("Authorization", "Bearer " + a.accessToken()))
        .andExpect(status().isOk()).andExpect(jsonPath("$.code").value(0))
        .andExpect(jsonPath("$.data").isEmpty()).andExpect(jsonPath("$.requestId").isNotEmpty());

    for (String table : new String[] {"user_identity", "refresh_token", "topic_recipient", "topic_participant",
        "chat_member", "chat_message_read", "user_rating_stat", "user_risk_tag"})
      assertEquals(0, count(table, "user_id", aid), table);
    assertEquals(0, count("user", "id", aid));
    assertEquals(0, count("topic", "id", ownTopic));
    assertEquals(1, count("chat_room", "id", ownRoom));
    assertEquals(1, count("chat_message", "id", ownRoomMessage));
    assertEquals(1, count("chat_message", "sender_id", aid));
    assertEquals(0, count("audit_log", "actor_user_id", aid));
    assertEquals(0, count("verification_code", "identity_value", aid + "@example.com"));
    assertNull(users.getIdentity("phone:13900000002"));
    assertNull(users.getCode("phone:13900000002:reset"));
    assertEquals(0, db.queryForObject("SELECT count(*) FROM service_state WHERE state_key = ? "
        + "OR state_key LIKE ? OR state_value = to_jsonb(CAST(? AS text))", Integer.class,
        aid.toString(), "email:" + aid + "@example.com%", aid.toString()));
    assertEquals(1, count("user", "id", bid));
    assertEquals(1, count("user", "id", cid));
    assertEquals(1, count("topic", "id", sharedTopic));
    assertEquals(1, count("chat_room", "id", unrelatedRoom));
    assertEquals(1, count("chat_message", "id", peerMessage));
    assertEquals(1, count("rating", "id", retainedRating));
    assertEquals(1, count("rating_category_item", "rating_id", retainedRating));
    assertEquals(ownMessage, db.queryForObject("SELECT last_read_message_id FROM chat_member WHERE room_id = ? AND user_id = ?",
        UUID.class, sharedRoom, bid));
    assertEquals(ownMessage, db.queryForObject("SELECT last_read_message_id FROM chat_message_read WHERE room_id = ? AND user_id = ?",
        UUID.class, sharedRoom, bid));
    assertRevoked(a);
    for (UUID retainedRoom : new UUID[] {ownRoom, sharedRoom})
      http.perform(get("/api/v1/rooms/" + retainedRoom + "/messages")
          .header("Authorization", "Bearer " + b.accessToken()))
          .andExpect(status().isOk())
          .andExpect(jsonPath("$.data.items[?(@.id == '" +
              (retainedRoom.equals(ownRoom) ? ownRoomMessage : ownMessage) + "')]").isNotEmpty());
    assertRevoked(secondSession);
    http.perform(get("/api/v1/me").header("Authorization", "Bearer " + b.accessToken()))
        .andExpect(status().isOk()).andExpect(jsonPath("$.data.id").value(bid.toString()));

    // Repeatedly reuse the exact identity, verifying both the unique constraint and JSON state are cleared.
    for (String identityType : new String[] {"email", "email", "phone"}) {
      String value = identityType.equals("email") ? aid + "@example.com" : "13900000002";
      users.putCode(identityType + ":" + value + ":register", "246810");
      String response = http.perform(post("/api/v1/auth/register").contentType("application/json")
          .content(json.writeValueAsString(Map.of("identityType", identityType, "identityValue", value,
              "code", "246810", "nickname", "reused", "communityId", community.toString()))))
          .andExpect(status().isOk()).andReturn().getResponse().getContentAsString();
      var data = json.readTree(response).get("data");
      UUID reusedId = UUID.fromString(data.get("user").get("id").asText());
      created.put(reusedId, new TokenPair(data.get("accessToken").asText(), 7200,
          data.get("refreshToken").asText(), null));
      assertNotEquals(aid, reusedId);
      http.perform(delete("/api/v1/me").header("Authorization", "Bearer " + data.get("accessToken").asText()))
          .andExpect(status().isOk());
    }
  }

  @Test
  void rejectsMissingAndForgedCredentialsWithoutDeletingAnyUser() throws Exception {
    TokenPair a = registerUser("A");
    http.perform(delete("/api/v1/me")).andExpect(status().isUnauthorized());
    http.perform(delete("/api/v1/me").header("Authorization", "Bearer forged"))
        .andExpect(status().isUnauthorized());
    assertEquals(1, count("user", "id", a.user().id()));
  }

  @Test
  void databaseFailureRollsBackAccountIdentitiesSessionsAndBusinessData() throws Exception {
    TokenPair a = registerUser("A"), b = registerUser("B");
    UUID ownTopic = topic(a.user().id());
    UUID ownRoom = room(ownTopic, a.user().id(), b.user().id());
    UUID ownRating = rating(ownTopic, a.user().id(), b.user().id());
    db.execute("CREATE FUNCTION reject_account_test_delete() RETURNS trigger LANGUAGE plpgsql AS $$ "
        + "BEGIN RAISE EXCEPTION 'injected deletion failure'; END $$");
    db.execute("CREATE TRIGGER reject_account_test_delete BEFORE DELETE ON \"user\" "
        + "FOR EACH ROW EXECUTE FUNCTION reject_account_test_delete()");
    try {
      assertThrows(RuntimeException.class, () -> deletion.deleteCurrentUser("Bearer " + a.accessToken()));
      assertEquals(1, count("user", "id", a.user().id()));
      assertEquals(1, count("user_identity", "user_id", a.user().id()));
      assertEquals(1, count("topic", "id", ownTopic));
      assertEquals(1, count("chat_room", "id", ownRoom));
      assertEquals(1, count("rating", "id", ownRating));
      assertEquals(a.user().id(), users.getAccess(a.accessToken()));
      assertEquals(a.user().id(), users.getRefresh(a.refreshToken()));
      assertTrue(users.hasIdentity("email:" + a.user().id() + "@example.com"));
    } finally {
      db.execute("DROP TRIGGER reject_account_test_delete ON \"user\"");
      db.execute("DROP FUNCTION reject_account_test_delete()");
    }
  }

  @Autowired TopicsControllerImpl topicsController;
  @Autowired com.partapp.partservice.persistence.repository.TopicParticipationService participation;

  @Test
  void publishesInboxAndCreatesOneRoomOnConcurrentAcceptance() throws Exception {
    var a=registerUser("publisher"); var b=registerUser("B"); var c=registerUser("C");
    var t=topicsController.createTopic(a.accessToken(), new com.partapp.partservice.api.Requests.Topic(
        "walk", "details", null, null, java.util.Set.of(), null));
    assertEquals(0,count("chat_room","topic_id",t.id()));
    assertEquals(2,count("topic_recipient","topic_id",t.id()));
    http.perform(get("/api/v1/topics/receipts").header("Authorization","Bearer "+b.accessToken()))
        .andExpect(status().isOk()).andExpect(jsonPath("$.data.items[0].topicId").value(t.id().toString()));
    http.perform(post("/api/v1/topics/"+t.id()+"/read").header("Authorization","Bearer "+b.accessToken()))
        .andExpect(status().isOk()).andExpect(jsonPath("$.data.readAt").isNotEmpty());
    assertEquals(1,participation.inbox(b.accessToken(),"pending",null,1).items().size());
    assertNotNull(participation.read(b.accessToken(),t.id()).getReadAt().orElse(null));
    try (var pool=java.util.concurrent.Executors.newFixedThreadPool(2)) {
      var start=new java.util.concurrent.CountDownLatch(1);
      var first=pool.submit(() -> { start.await(); return participation.accept(b.accessToken(),t.id()); });
      var second=pool.submit(() -> { start.await(); return participation.accept(c.accessToken(),t.id()); });
      start.countDown();
      UUID room=first.get(15,java.util.concurrent.TimeUnit.SECONDS).getRoomId().orElse(null);
      createdRooms.add(room);
      assertEquals(room,second.get(15,java.util.concurrent.TimeUnit.SECONDS).getRoomId().orElse(null));
      assertEquals(room,participation.accept(b.accessToken(),t.id()).getRoomId().orElse(null));
      http.perform(post("/api/v1/topics/"+t.id()+"/accept").header("Authorization","Bearer "+b.accessToken()))
          .andExpect(status().isOk()).andExpect(jsonPath("$.data.roomId").value(room.toString()));
      assertEquals(3,count("chat_member","room_id",room));
      assertEquals(3,count("topic_participant","topic_id",t.id()));
      assertEquals(1,count("chat_room","topic_id",t.id()));
      assertEquals("owner",db.queryForObject("SELECT role FROM chat_member WHERE room_id=? AND user_id=?",String.class,room,a.user().id()));
      topicsController.closeTopic(a.accessToken(),t.id());
      assertEquals(room,participation.accept(b.accessToken(),t.id()).getRoomId().orElse(null));
    }
    http.perform(post("/api/v1/topics/"+t.id()+"/invite").header("Authorization","Bearer "+a.accessToken())
        .contentType("application/json").content("{}" )).andExpect(status().isNotFound());
  }

  @Test
  void rejectsNewAcceptanceAfterCloseOrExpiryAndRechecksCommunity() {
    var a=registerUser("publisher"); var b=registerUser("B");
    var t=topicsController.createTopic(a.accessToken(),new com.partapp.partservice.api.Requests.Topic(
        "walk","details",null,null,java.util.Set.of(),null));
    assertThrows(com.partapp.partservice.web.ApiException.class,() -> participation.accept(a.accessToken(),t.id()));
    db.update("UPDATE topic SET expires_at=now()-interval '1 minute' WHERE id=?",t.id());
    assertThrows(com.partapp.partservice.web.ApiException.class,() -> participation.accept(b.accessToken(),t.id()));
    assertEquals(0,count("chat_room","topic_id",t.id()));
    assertEquals(0,count("topic_participant","topic_id",t.id()));
    db.update("UPDATE topic SET expires_at=NULL, status='closed' WHERE id=?",t.id());
    assertThrows(com.partapp.partservice.web.ApiException.class,() -> participation.accept(b.accessToken(),t.id()));
    assertEquals(0,count("chat_room","topic_id",t.id()));
  }

  @Test
  void rollsBackRoomAndParticipationWhenMembershipInsertFails() {
    var a=registerUser("publisher"); var b=registerUser("B");
    var t=topicsController.createTopic(a.accessToken(),new com.partapp.partservice.api.Requests.Topic(
        "walk","details",null,null,java.util.Set.of(),null));
    db.execute("ALTER TABLE chat_member ADD CONSTRAINT test_reject_member CHECK (user_id <> '"+b.user().id()+"'::uuid) NOT VALID");
    try {
      assertThrows(org.springframework.dao.DataIntegrityViolationException.class,() -> participation.accept(b.accessToken(),t.id()));
      assertEquals(0,count("chat_room","topic_id",t.id()));
      assertEquals(0,count("topic_participant","topic_id",t.id()));
      assertEquals("pending",participation.inbox(b.accessToken(),null,null,20).items().getFirst().getStatus().getValue());
    } finally { db.execute("ALTER TABLE chat_member DROP CONSTRAINT test_reject_member"); }
  }

  @Test
  void distributionAndAcceptanceEnforceCommunityAndCurrentProfile() {
    var a=registerUser("publisher"); var b=registerUser("B"); var unmatched=registerUser("unmatched");
    db.update("UPDATE \"user\" SET gender='male' WHERE id=?",b.user().id());
    var t=topicsController.createTopic(a.accessToken(),new com.partapp.partservice.api.Requests.Topic(
        "walk","details",null,null,java.util.Set.of("male"),null));
    assertEquals(1,count("topic_recipient","topic_id",t.id()));
    assertThrows(com.partapp.partservice.web.ApiException.class,() -> participation.accept(unmatched.accessToken(),t.id()));
    db.update("UPDATE \"user\" SET gender='female' WHERE id=?",b.user().id());
    assertThrows(com.partapp.partservice.web.ApiException.class,() -> participation.accept(b.accessToken(),t.id()));
    UUID other=UUID.randomUUID();
    db.update("INSERT INTO community (id,name) VALUES (?, 'other')",other);
    try {
      db.update("UPDATE \"user\" SET gender='male',community_id=? WHERE id=?",other,b.user().id());
      assertTrue(participation.inbox(b.accessToken(),null,null,20).items().isEmpty());
      assertThrows(com.partapp.partservice.web.ApiException.class,() -> participation.accept(b.accessToken(),t.id()));
      assertEquals(0,count("chat_room","topic_id",t.id()));
    } finally {
      db.update("UPDATE \"user\" SET community_id=? WHERE id=?",community,b.user().id());
      db.update("DELETE FROM community WHERE id=?",other);
    }
  }
}
