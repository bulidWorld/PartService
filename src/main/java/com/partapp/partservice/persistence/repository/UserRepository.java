package com.partapp.partservice.persistence.repository;

import java.util.Map;
import java.util.Objects;
import java.util.Optional;
import java.util.UUID;
import java.time.Instant;
import java.time.LocalDate;

import com.partapp.partservice.persistence.mapper.ServiceStateMapper;
import com.partapp.partservice.persistence.mapper.UserMapper;
import com.partapp.partservice.persistence.mapper.UserIdentityMapper;
import com.partapp.partservice.persistence.entity.UserEntity;
import com.partapp.partservice.persistence.entity.UserIdentityEntity;
import com.baomidou.mybatisplus.core.conditions.query.QueryWrapper;
import org.springframework.stereotype.Component;

import com.partapp.partservice.web.ApiException;

/** PostgreSQL-backed persistence for users, identities, codes and sessions. */
@Component
public class UserRepository {
  public record User(UUID id, String nickname, UUID communityId, String gender, LocalDate birthDate, String avatarUrl,
      String status, Instant communityChangedAt) {
  }

  public record TokenPair(String accessToken, long expiresIn, String refreshToken, User user) {
  }

  private final StateStore state;

  private final UserMapper userMapper;
  private final UserIdentityMapper identityMapper;

  public UserRepository(ServiceStateMapper mapper, UserMapper userMapper, UserIdentityMapper identityMapper) {
    this.userMapper = userMapper;
    this.identityMapper = identityMapper;
    this.state = new StateStore(mapper);
  }

  public UUID authenticate(String bearer) {
    if (bearer == null || bearer.isBlank())
      throw ApiException.unauthorized("未登录");
    String token = bearer.startsWith("Bearer ") ? bearer.substring(7) : bearer;
    UUID id = getAccess(token);
    if (id == null)
      throw ApiException.unauthorized("令牌无效");
    return id;
  }

  public User active(UUID id) {
    User u = getUser(id);
    if (u == null)
      throw ApiException.unauthorized("用户不存在");
    if (!u.status().equals("active"))
      throw ApiException.forbidden("账号不可用");
    return u;
  }

  public TokenPair issue(User u) {
    String a = UUID.randomUUID().toString(), r = UUID.randomUUID().toString();
    putAccess(a, u.id());
    putRefresh(r, u.id());
    return new TokenPair(a, 7200, r, u);
  }

  public void verifyCode(String type, String value, String code, String purpose) {
    if (code == null || code.isBlank())
      throw ApiException.bad("验证码错误或已过期");
    String key = identityKey(type, value) + ":" + purpose;
    if (!Objects.equals(getCode(key), code))
      throw ApiException.bad("验证码错误或已过期");
    state.remove("codes", key);
  }

  public String identityKey(String t, String v) {
    return t.toLowerCase() + ":" + v.trim().toLowerCase();
  }

  public User current(String bearer) {
    UUID id = authenticate(bearer);
    User u = findById(id).orElse(getUser(id));
    if (u == null)
      throw ApiException.unauthorized("用户不存在");
    putUser(id, u);
    return active(id);
  }

  public Optional<User> findById(UUID id) {
    UserEntity e = userMapper.selectById(id);
    return Optional.ofNullable(e).map(UserRepository::toUser);
  }

  public Optional<UUID> findIdByIdentity(String type, String value) {
    UserIdentityEntity e = identityMapper.selectOne(
        new QueryWrapper<UserIdentityEntity>().eq("identity_type", type).eq("identity_value", value).last("LIMIT 1"));
    return Optional.ofNullable(e).map(x -> x.userId);
  }

  public void insert(UUID id, String nickname, UUID communityId, String identityType, String identityValue,
      String passwordHash) {
    UserEntity u = new UserEntity();
    u.id = id;
    u.nickname = nickname;
    u.communityId = communityId;
    u.passwordHash = passwordHash;
    userMapper.insert(u);
    UserIdentityEntity i = new UserIdentityEntity();
    i.id = UUID.randomUUID();
    i.userId = id;
    i.identityType = identityType;
    i.identityValue = identityValue;
    i.primary = true;
    identityMapper.insert(i);
  }

  public Optional<String> findPasswordHash(UUID userId) {
    UserEntity user = userMapper.selectById(userId);
    return Optional.ofNullable(user).map(u -> u.passwordHash);
  }

  public void updatePasswordHash(UUID userId, String passwordHash) {
    UserEntity user = new UserEntity();
    user.id = userId;
    user.passwordHash = passwordHash;
    userMapper.updateById(user);
  }

  public void update(User u) {
    UserEntity e = new UserEntity();
    e.id = u.id();
    e.nickname = u.nickname();
    e.gender = u.gender();
    e.birthDate = u.birthDate();
    e.avatarUrl = u.avatarUrl();
    e.communityId = u.communityId();
    e.communityChangedAt = u.communityChangedAt();
    e.status = u.status();
    userMapper.updateById(e);
  }

  private static User toUser(UserEntity e) {
    return new User(e.id, e.nickname, e.communityId, e.gender, e.birthDate, e.avatarUrl, e.status,
        e.communityChangedAt);
  }

  public User getUser(UUID id) {
    return state.get("users", id, User.class);
  }

  public void putUser(UUID id, User v) {
    state.put("users", id, v);
  }

  public UUID getIdentity(String k) {
    return state.get("identities", k, UUID.class);
  }

  public void putIdentity(String k, UUID v) {
    state.put("identities", k, v);
  }

  public boolean hasIdentity(String k) {
    return getIdentity(k) != null;
  }

  public void putCode(String k, String v) {
    state.put("codes", k, v);
  }

  public String getCode(String k) {
    return state.get("codes", k, String.class);
  }

  public UUID getAccess(String k) {
    return state.get("access", k, UUID.class);
  }

  private void putAccess(String k, UUID v) {
    state.put("access", k, v);
  }

  public UUID getRefresh(String k) {
    return state.get("refresh", k, UUID.class);
  }

  public void putRefresh(String k, UUID v) {
    state.put("refresh", k, v);
  }

  public void removeRefresh(String k) {
    state.remove("refresh", k);
  }
}
