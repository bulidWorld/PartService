package com.partapp.partservice.web.impl;

import static org.junit.jupiter.api.Assertions.assertEquals;

import java.time.Instant;
import java.time.LocalDate;
import java.time.OffsetDateTime;
import java.util.Map;
import java.util.UUID;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.partapp.partservice.persistence.repository.MessageRepository;
import com.partapp.partservice.web.JacksonConfig;
import com.partapp.partservice.web.generated.model.ApiResponseMessage;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.json.JsonTest;
import org.springframework.context.annotation.Import;

@JsonTest
@Import(JacksonConfig.class)
class TimestampSerializationTest {
  @Autowired ObjectMapper json;

  @Test
  void messageResponseOmitsFractionalSeconds() throws Exception {
    var message = new MessageRepository.Message(UUID.randomUUID(), UUID.randomUUID(), UUID.randomUUID(),
        "case-msg-M", "text", "大家好，准时集合。", Map.of(),
        Instant.parse("2026-10-23T03:15:48.085446Z"), false);
    var response = new ApiResponseMessage(0, "ok", ApiModelMapper.message(message), "request-test");
    var data = json.readTree(json.writeValueAsString(response)).get("data");
    assertEquals("2026-10-23T03:15:48Z", data.get("createdAt").textValue());
  }

  @Test
  void timestampsUseUtcSecondsWithoutRoundingAndDatesStayDates() throws Exception {
    var values = Map.of(
        "instant", Instant.parse("2026-10-23T03:15:48.999999999Z"),
        "offset", OffsetDateTime.parse("2026-10-23T11:15:48.999999999+08:00"),
        "date", LocalDate.parse("2000-01-02"));
    var data = json.readTree(json.writeValueAsString(values));
    assertEquals("2026-10-23T03:15:48Z", data.get("instant").textValue());
    assertEquals("2026-10-23T03:15:48Z", data.get("offset").textValue());
    assertEquals("2000-01-02", data.get("date").textValue());
    assertEquals(Instant.parse("2026-10-23T03:15:48.085446Z"),
        json.readValue("\"2026-10-23T03:15:48.085446Z\"", Instant.class));
  }
}
