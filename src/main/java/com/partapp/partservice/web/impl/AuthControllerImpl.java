package com.partapp.partservice.web.impl;

import java.util.*;
import java.time.Instant;
import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Service;
import org.springframework.web.context.request.RequestContextHolder;
import org.springframework.web.context.request.ServletRequestAttributes;

import com.partapp.partservice.api.Requests;
import com.partapp.partservice.persistence.repository.UserRepository;
import com.partapp.partservice.persistence.repository.UserRepository.TokenPair;
import com.partapp.partservice.persistence.repository.UserRepository.User;
import com.partapp.partservice.web.ApiException;
import com.partapp.partservice.web.generated.api.AuthApiDelegate;
import com.partapp.partservice.web.generated.model.*;

@Service
class AuthControllerImpl implements AuthApiDelegate {
  private final UserRepository userRepo;

  AuthControllerImpl(UserRepository userRepo) {
    this.userRepo = userRepo;
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

  public SendCodeResult sendCode(Requests.SendCode r) {
    if (!Set.of("email", "phone").contains(r.identityType()))
      throw ApiException.bad("identity_type 不支持");
    String key = userRepo.identityKey(r.identityType(), r.identityValue());
    userRepo.putCode(key + ":" + r.purpose(), "000000");
    return new SendCodeResult(300, 60);
  }

  public TokenPair register(Requests.Register r) {
    userRepo.verifyCode(r.identityType(), r.identityValue(), r.code(), "register");
    String key = userRepo.identityKey(r.identityType(), r.identityValue());
    if (userRepo.hasIdentity(key))
      throw ApiException.conflict("身份已注册");
    User u = new User(UUID.randomUUID(), r.nickname(), r.communityId(), null, null, null, "active", Instant.now());
    userRepo.insert(u.id(), u.nickname(), u.communityId(), r.identityType(),
        r.identityValue().trim().toLowerCase());
    userRepo.putUser(u.id(), u);
    userRepo.putIdentity(key, u.id());
    return userRepo.issue(u);
  }

  public TokenPair login(Requests.Login r) {
    userRepo.verifyCode(r.identityType(), r.identityValue(), r.code(), "login");
    String identity = userRepo.identityKey(r.identityType(), r.identityValue());
    UUID id = userRepo.getIdentity(identity);
    if (id == null)
      id = userRepo.findIdByIdentity(r.identityType(), r.identityValue().trim().toLowerCase()).orElse(null);
    if (id == null)
      throw ApiException.unauthorized("身份未注册");
    User u = userRepo.active(id);
    userRepo.putIdentity(identity, id);
    return userRepo.issue(u);
  }

  public TokenPair refresh(String token) {
    UUID id = userRepo.getRefresh(token);
    if (id == null)
      throw ApiException.unauthorized("refresh token 无效");
    return userRepo.issue(userRepo.active(id));
  }

  public void logout(String token) {
    userRepo.removeRefresh(token);
  }

  @Override
  public ResponseEntity<ApiResponseSendCode> sendCode(SendCodeRequest x) {
    return ResponseEntity.ok(new ApiResponseSendCode(0, "ok", sendCode(new Requests.SendCode(x.getIdentityType(), x.getIdentityValue(), x.getPurpose())), requestId()));
  }

  @Override
  public ResponseEntity<ApiResponseTokenPair> register(RegisterRequest x) {
    return ResponseEntity.ok(new ApiResponseTokenPair(0, "ok", ApiModelMapper.tokenPair(register(new Requests.Register(x.getIdentityType(), x.getIdentityValue(), x.getCode(), x.getNickname(),
        x.getCommunityId()))), requestId()));
  }

  @Override
  public ResponseEntity<ApiResponseTokenPair> login(LoginRequest x) {
    return ResponseEntity.ok(new ApiResponseTokenPair(0, "ok", ApiModelMapper.tokenPair(login(new Requests.Login(x.getIdentityType(), x.getIdentityValue(), x.getCode(), x.getDeviceId()))), requestId()));
  }

  @Override
  public ResponseEntity<ApiResponseTokenPair> refresh(RefreshRequest x) {
    return ResponseEntity.ok(new ApiResponseTokenPair(0, "ok", ApiModelMapper.tokenPair(refresh(x.getRefreshToken())), requestId()));
  }

  @Override
  public ResponseEntity<ApiResponseEmpty> logout(RefreshRequest x) {
    logout(x.getRefreshToken());
    return ResponseEntity.ok(new ApiResponseEmpty(0, "ok", Map.of(), requestId()));
  }
}
