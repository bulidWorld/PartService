package com.partapp.partservice.web.impl;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

import java.util.List;
import java.util.UUID;

import com.baomidou.mybatisplus.core.conditions.Wrapper;
import com.partapp.partservice.persistence.entity.CommunityEntity;
import com.partapp.partservice.persistence.mapper.CommunityMapper;
import org.junit.jupiter.api.Test;

class CommunityListTest {
  @Test
  void returnsCommunitiesFromDatabaseAndFiltersActiveStatus() {
    CommunityMapper mapper = mock(CommunityMapper.class);
    CommunityEntity entity = new CommunityEntity();
    entity.id = UUID.randomUUID();
    entity.name = "测试社区";
    entity.status = "active";
    when(mapper.selectList(any(Wrapper.class))).thenAnswer(invocation -> {
      var query = (com.baomidou.mybatisplus.core.conditions.query.QueryWrapper<?>) invocation.getArgument(0);
      assertTrue(query.getSqlSegment().contains("status ="));
      assertTrue(query.getParamNameValuePairs().containsValue("active"));
      return List.of(entity);
    });

    var response = new UsersControllerImpl(null, null, mapper).listCommunities().getBody();

    assertNotNull(response);
    assertEquals(0, response.getCode());
    assertEquals(1, response.getData().size());
    assertEquals(entity.id, response.getData().getFirst().getId());
    assertEquals(entity.name, response.getData().getFirst().getName());
    verify(mapper).selectList(any(Wrapper.class));
  }

  @Test
  void returnsEmptyListWhenNoActiveCommunitiesExist() {
    CommunityMapper mapper = mock(CommunityMapper.class);
    when(mapper.selectList(any(Wrapper.class))).thenReturn(List.of());

    var response = new UsersControllerImpl(null, null, mapper).listCommunities().getBody();

    assertNotNull(response);
    assertEquals(List.of(), response.getData());
  }
}
