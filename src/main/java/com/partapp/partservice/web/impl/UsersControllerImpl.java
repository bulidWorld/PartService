package com.partapp.partservice.web.impl;

import java.util.*;
import java.time.Instant;
import java.time.Duration;
import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Service;
import org.springframework.web.context.request.RequestContextHolder;
import org.springframework.web.context.request.ServletRequestAttributes;

import com.partapp.partservice.api.Requests;
import com.baomidou.mybatisplus.core.conditions.query.QueryWrapper;
import com.partapp.partservice.persistence.entity.CommunityEntity;
import com.partapp.partservice.persistence.mapper.CommunityMapper;
import com.partapp.partservice.persistence.repository.UserRepository;
import com.partapp.partservice.persistence.repository.AccountDeletionService;
import com.partapp.partservice.persistence.repository.UserRepository.User;
import com.partapp.partservice.web.ApiException;
import com.partapp.partservice.web.generated.api.UsersApiDelegate;
import com.partapp.partservice.web.generated.model.*;

@Service
class UsersControllerImpl implements UsersApiDelegate {
  private final UserRepository userRepo;
  private final AccountDeletionService accountDeletion;
  private final CommunityMapper communityMapper;
  UsersControllerImpl(UserRepository userRepo, AccountDeletionService accountDeletion, CommunityMapper communityMapper) {
    this.userRepo = userRepo;
    this.accountDeletion = accountDeletion;
    this.communityMapper = communityMapper;
  }
  protected String authorization() {
    ServletRequestAttributes a=(ServletRequestAttributes)RequestContextHolder.getRequestAttributes(); return a==null?null:a.getRequest().getHeader("Authorization");
  }
  protected String requestId() {
    ServletRequestAttributes attributes = (ServletRequestAttributes) RequestContextHolder.getRequestAttributes();
    Object id = attributes == null ? null : attributes.getRequest().getAttribute("requestId");
    return id == null ? "" : id.toString();
  }

  public User updateUser(String bearer, Requests.Profile r) {
    UUID id = userRepo.authenticate(bearer);
    User old = userRepo.active(id);
    User n = new User(id, r.nickname() != null ? r.nickname() : old.nickname(), old.communityId(),
        r.gender() != null ? r.gender() : old.gender(), r.birthDate() != null ? r.birthDate() : old.birthDate(),
        r.avatarUrl() != null ? r.avatarUrl() : old.avatarUrl(), old.status(), old.communityChangedAt());
    userRepo.putUser(id, n);
    userRepo.update(n);
    return n;
  }

  public User changeCommunity(String bearer, UUID community) {
    UUID id = userRepo.authenticate(bearer);
    User old = userRepo.active(id);
    if (old.communityId().equals(community))
      return old;
    if (old.communityChangedAt().plus(Duration.ofDays(7)).isAfter(Instant.now()))
      throw ApiException.conflict("7天内不可切换社区");
    User n = new User(id, old.nickname(), community, old.gender(), old.birthDate(), old.avatarUrl(), old.status(),
        Instant.now());
    userRepo.putUser(id, n);
    userRepo.update(n);
    return n;
  }
  @Override
  public ResponseEntity<ApiResponseEmpty> deleteCurrentUser() {
    accountDeletion.deleteCurrentUser(authorization());
    return ResponseEntity.ok(new ApiResponseEmpty(0, "ok", Map.of(), requestId()));
  }
  @Override
  public ResponseEntity<ApiResponseUser> getCurrentUser() {
    return ResponseEntity.ok(new ApiResponseUser(0, "ok", ApiModelMapper.user(userRepo.current(authorization())), requestId()));
  }
  @Override
  public ResponseEntity<ApiResponseUser> updateCurrentUser(ProfileRequest x) {
    return ResponseEntity.ok(new ApiResponseUser(0, "ok", ApiModelMapper.user(updateUser(authorization(), new Requests.Profile(x.getNickname(), x.getGender(), x.getBirthDate(), x.getAvatarUrl()==null?null:x.getAvatarUrl().toString()))), requestId()));
  }
  @Override
  public ResponseEntity<ApiResponseUser> changeCommunity(CommunityRequest x) {
    return ResponseEntity.ok(new ApiResponseUser(0, "ok", ApiModelMapper.user(changeCommunity(authorization(), x.getCommunityId())), requestId()));
  }
  @Override
  public ResponseEntity<ApiResponseCommunities> listCommunities() {
    List<Community> communities = communityMapper.selectList(new QueryWrapper<CommunityEntity>()
        .eq("status", "active").orderByAsc("name", "id")).stream()
        .map(entity -> new Community().id(entity.id).name(entity.name))
        .toList();
    return ResponseEntity.ok(new ApiResponseCommunities(0, "ok", communities, requestId()));
  }
}
