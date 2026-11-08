package com.partapp.partservice.web.impl;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;

import java.util.UUID;

import jakarta.validation.Validation;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.NullAndEmptySource;
import org.junit.jupiter.params.provider.ValueSource;

import com.baomidou.mybatisplus.core.conditions.query.QueryWrapper;
import com.partapp.partservice.persistence.entity.GlobalDictionaryEntity;
import com.partapp.partservice.persistence.mapper.GlobalDictionaryMapper;
import com.partapp.partservice.persistence.repository.GlobalDictionaryRepository;
import com.partapp.partservice.persistence.repository.UserRepository;
import com.partapp.partservice.web.ApiException;
import com.partapp.partservice.web.generated.model.RegisterRequest;

class AuthRegistrationTest {
  private GlobalDictionaryMapper mapper;
  private UserRepository users;
  private AuthControllerImpl controller;

  @BeforeEach
  void setUp() {
    mapper = mock(GlobalDictionaryMapper.class);
    users = mock(UserRepository.class);
    when(users.identityKey("email", "user@example.com")).thenReturn("email:user@example.com");
    when(users.issue(any())).thenAnswer(call ->
        new UserRepository.TokenPair("access", 7200, "refresh", call.getArgument(0)));
    controller = new AuthControllerImpl(users, new GlobalDictionaryRepository(mapper));
  }

  private void configure(String value) {
    GlobalDictionaryEntity entry = new GlobalDictionaryEntity();
    entry.key = GlobalDictionaryRepository.REGISTER_VERIFY_CODE;
    entry.value = value;
    when(mapper.selectOne(any(QueryWrapper.class))).thenReturn(entry);
  }

  private RegisterRequest request(String code) {
    return new RegisterRequest().identityType("email").identityValue("user@example.com")
        .nickname("test").communityId(UUID.randomUUID()).code(code);
  }

  @ParameterizedTest
  @NullAndEmptySource
  @ValueSource(strings = {"wrong", " "})
  void disabledVerificationAllowsMissingOrInvalidCode(String code) {
    configure("false");
    RegisterRequest request = request(code);
    try (var factory = Validation.buildDefaultValidatorFactory()) {
      assertTrue(factory.getValidator().validate(request).isEmpty());
    }

    var response = controller.register(request).getBody();

    assertNotNull(response);
    assertEquals(0, response.getCode());
    verify(users, never()).verifyCode(anyString(), anyString(), nullable(String.class), anyString());
    verify(users).insert(any(UUID.class), eq("test"), eq(request.getCommunityId()),
        eq("email"), eq("user@example.com"), isNull());
  }

  @Test
  void enabledVerificationChecksCodeBeforeRegistering() {
    configure("true");

    assertEquals(0, controller.register(request("123456")).getBody().getCode());

    var order = inOrder(users);
    order.verify(users).verifyCode("email", "user@example.com", "123456", "register");
    order.verify(users).insert(any(UUID.class), eq("test"), any(UUID.class), eq("email"), eq("user@example.com"),
        isNull());
  }

  @ParameterizedTest
  @NullAndEmptySource
  @ValueSource(strings = {"wrong", " "})
  void enabledVerificationRejectsMissingOrInvalidCode(String code) {
    configure("true");
    doCallRealMethod().when(users).verifyCode(anyString(), anyString(), nullable(String.class), anyString());

    assertThrows(ApiException.class, () -> controller.register(request(code)));

    verify(users, never()).insert(any(), any(), any(), any(), any(), any());
  }

  @ParameterizedTest
  @NullAndEmptySource
  @ValueSource(strings = {"true", "invalid"})
  void missingOrInvalidConfigurationKeepsVerificationEnabled(String value) {
    if (value != null)
      configure(value);
    doThrow(ApiException.bad("验证码错误或已过期")).when(users)
        .verifyCode(anyString(), anyString(), nullable(String.class), anyString());

    assertThrows(ApiException.class, () -> controller.register(request(null)));

    verify(users, never()).insert(any(), any(), any(), any(), any(), any());
  }

  @Test
  void configurationChangesTakeEffectOnNextRegistration() {
    configure("false");
    controller.register(request(null));
    configure("true");
    doThrow(ApiException.bad("验证码错误或已过期")).when(users)
        .verifyCode(anyString(), anyString(), nullable(String.class), anyString());

    assertThrows(ApiException.class, () -> controller.register(request(null)));
  }
}
