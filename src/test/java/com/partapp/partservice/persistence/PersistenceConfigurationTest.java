package com.partapp.partservice.persistence;

import static org.junit.jupiter.api.Assertions.*;

import java.util.UUID;
import org.apache.ibatis.session.SqlSessionFactory;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;

import com.partapp.partservice.persistence.mapper.ChatMessageMapper;
import com.partapp.partservice.persistence.typehandler.UuidTypeHandler;
import com.partapp.partservice.persistence.typehandler.JsonbStringTypeHandler;

@SpringBootTest(webEnvironment = SpringBootTest.WebEnvironment.NONE)
class PersistenceConfigurationTest {
  @Autowired
  SqlSessionFactory sessions;

  @Test
  void contextInitializesMappersWithUuidAndExplicitJsonbHandlers() {
    var configuration = sessions.getConfiguration();
    assertTrue(configuration.hasMapper(ChatMessageMapper.class));
    assertInstanceOf(UuidTypeHandler.class,
        configuration.getTypeHandlerRegistry().getTypeHandler(UUID.class));
    assertFalse(configuration.getTypeHandlerRegistry().getTypeHandler(String.class)
        instanceof JsonbStringTypeHandler);
    var resultMap = configuration.getResultMap(
        "com.partapp.partservice.persistence.mapper.ChatMessageMapper.mybatis-plus_ChatMessageEntity");
    var id = resultMap.getResultMappings().stream().filter(x -> x.getProperty().equals("id")).findFirst().orElseThrow();
    assertInstanceOf(UuidTypeHandler.class, id.getTypeHandler());
    var meta = resultMap.getResultMappings().stream().filter(x -> x.getProperty().equals("contentMeta"))
        .findFirst().orElseThrow();
    assertInstanceOf(JsonbStringTypeHandler.class, meta.getTypeHandler());
  }
}
