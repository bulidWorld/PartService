package com.partapp.partservice.persistence.repository;

import java.util.Optional;

import com.baomidou.mybatisplus.core.conditions.query.QueryWrapper;
import com.partapp.partservice.persistence.entity.GlobalDictionaryEntity;
import com.partapp.partservice.persistence.mapper.GlobalDictionaryMapper;
import org.springframework.stereotype.Component;

@Component
public class GlobalDictionaryRepository {
  public static final String REGISTER_VERIFY_CODE = "register_verify_code";

  private final GlobalDictionaryMapper mapper;

  public GlobalDictionaryRepository(GlobalDictionaryMapper mapper) {
    this.mapper = mapper;
  }

  public Optional<String> getValue(String key) {
    return Optional.ofNullable(mapper.selectOne(
        new QueryWrapper<GlobalDictionaryEntity>().eq("\"key\"", key)))
        .map(entry -> entry.value);
  }

  public boolean isRegisterCodeVerificationEnabled() {
    // Only an explicit false disables verification; missing or invalid values keep it enabled.
    return getValue(REGISTER_VERIFY_CODE).map(value -> !"false".equalsIgnoreCase(value.trim())).orElse(true);
  }
}
