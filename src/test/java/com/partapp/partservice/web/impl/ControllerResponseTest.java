package com.partapp.partservice.web.impl;

import static org.junit.jupiter.api.Assertions.*;

import java.time.Instant;
import java.util.List;
import java.util.Set;
import java.util.UUID;

import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.Test;
import org.springframework.mock.web.MockHttpServletRequest;
import org.springframework.web.context.request.RequestContextHolder;
import org.springframework.web.context.request.ServletRequestAttributes;

import com.partapp.partservice.api.PageResponse;
import com.partapp.partservice.persistence.repository.TopicRepository;
import com.partapp.partservice.persistence.repository.UserRepository;
import com.partapp.partservice.web.generated.api.AuthApiDelegate;
import com.partapp.partservice.web.generated.model.ApiResponseSendCode;
import com.partapp.partservice.web.generated.model.SendCodeRequest;

class ControllerResponseTest {
  @AfterEach
  void clearRequest() {
    RequestContextHolder.resetRequestAttributes();
  }

  @Test
  void sendCodeReturnsTypedPayloadAndRequestIdThroughDelegate() {
    String[] storedCode = new String[2];
    UserRepository repository = new UserRepository(null, null, null) {
      @Override
      public void putCode(String key, String value) {
        storedCode[0] = key;
        storedCode[1] = value;
      }
    };
    MockHttpServletRequest request = new MockHttpServletRequest();
    request.setAttribute("requestId", "request-123");
    RequestContextHolder.setRequestAttributes(new ServletRequestAttributes(request));
    AuthApiDelegate delegate = new AuthControllerImpl(repository, null);

    ApiResponseSendCode response = delegate.sendCode(
        new SendCodeRequest("email", "user@example.com", "login")).getBody();

    assertNotNull(response);
    assertEquals(0, response.getCode());
    assertEquals("request-123", response.getRequestId());
    assertEquals(300, response.getData().getExpiresIn());
    assertEquals(60, response.getData().getNextSendAfter());
    assertEquals("email:user@example.com:login", storedCode[0]);
    assertEquals("000000", storedCode[1]);
  }

  @Test
  void topicPagePreservesItemsCursorAndNullableFields() {
    Instant created = Instant.parse("2026-10-15T00:00:00Z");
    TopicRepository.Topic topic = new TopicRepository.Topic(UUID.randomUUID(), UUID.randomUUID(),
        UUID.randomUUID(), "title", "detail", null, 40, Set.of("female"), null, "open", created);

    var page = ApiModelMapper.topics(new PageResponse<>(List.of(topic), "next", true));

    assertEquals(1, page.getItems().size());
    assertEquals("next", page.getNextCursor().get());
    assertTrue(page.getHasMore());
    var item = page.getItems().getFirst();
    assertEquals(topic.id(), item.getId());
    assertNull(item.getMinAge().get());
    assertEquals(40, item.getMaxAge().get());
    assertNull(item.getExpiresAt().get());
    assertEquals(created, item.getCreatedAt().toInstant());
    assertEquals(List.of("female"), item.getGenders());
  }
}
