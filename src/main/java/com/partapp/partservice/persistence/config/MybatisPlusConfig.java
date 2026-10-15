package com.partapp.partservice.persistence.config;

import org.mybatis.spring.annotation.MapperScan;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.Bean;
import java.util.UUID;
import com.baomidou.mybatisplus.autoconfigure.ConfigurationCustomizer;
import com.partapp.partservice.persistence.typehandler.UuidTypeHandler;

@Configuration
@MapperScan("com.partapp.partservice.persistence.mapper")
public class MybatisPlusConfig {
  @Bean
  public ConfigurationCustomizer uuidTypeHandlerCustomizer() {
    return configuration -> configuration.getTypeHandlerRegistry().register(UUID.class, new UuidTypeHandler());
  }
}
