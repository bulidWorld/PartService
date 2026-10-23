package com.partapp.partservice.web.impl;

import static org.junit.jupiter.api.Assertions.*;

import java.time.Instant;
import java.util.Set;
import java.util.UUID;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.partapp.partservice.persistence.repository.TopicRepository;
import com.partapp.partservice.web.JacksonConfig;
import com.partapp.partservice.web.generated.model.ApiResponseTopic;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.json.JsonTest;
import org.springframework.context.annotation.Import;

@JsonTest
@Import(JacksonConfig.class)
class TopicSerializationTest {
  @Autowired ObjectMapper json;

  @Test
  void unlimitedConstraintsSerializeAsNull() throws Exception {
    var data = serialize(null, null, null);

    assertTrue(data.has("minAge"));
    assertTrue(data.get("minAge").isNull());
    assertTrue(data.has("maxAge"));
    assertTrue(data.get("maxAge").isNull());
    assertTrue(data.has("expiresAt"));
    assertTrue(data.get("expiresAt").isNull());
  }

  @Test
  void constraintsSerializeAsNumbersAndDateString() throws Exception {
    Instant expires = Instant.parse("2026-10-25T00:00:00Z");
    var data = serialize(18, 40, expires);

    assertTrue(data.get("minAge").isIntegralNumber());
    assertEquals(18, data.get("minAge").intValue());
    assertTrue(data.get("maxAge").isIntegralNumber());
    assertEquals(40, data.get("maxAge").intValue());
    assertEquals(expires, Instant.parse(data.get("expiresAt").textValue()));
  }

  private com.fasterxml.jackson.databind.JsonNode serialize(Integer minAge, Integer maxAge, Instant expires)
      throws Exception {
    var topic = new TopicRepository.Topic(UUID.randomUUID(), UUID.randomUUID(), UUID.randomUUID(),
        "title", "detail", minAge, maxAge, Set.of(), expires, "open", Instant.now());
    var response = new ApiResponseTopic(0, "ok", ApiModelMapper.topic(topic), "request-test");
    return json.readTree(json.writeValueAsString(response)).get("data");
  }
}
