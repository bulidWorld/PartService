package com.partapp.partservice.web;

import java.io.IOException;
import java.time.Instant;
import java.time.OffsetDateTime;
import java.time.format.DateTimeFormatter;
import java.time.format.DateTimeFormatterBuilder;
import java.util.function.Function;

import com.fasterxml.jackson.core.JsonGenerator;
import com.fasterxml.jackson.databind.JsonSerializer;
import com.fasterxml.jackson.databind.SerializerProvider;
import org.springframework.boot.autoconfigure.jackson.Jackson2ObjectMapperBuilderCustomizer;
import org.openapitools.jackson.nullable.JsonNullableModule;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

@Configuration
public class JacksonConfig {
  private static final DateTimeFormatter SECONDS =
      new DateTimeFormatterBuilder().appendInstant(0).toFormatter();

  @Bean
  public Jackson2ObjectMapperBuilderCustomizer secondPrecisionTimestamps() {
    return builder -> {
      builder.serializerByType(Instant.class, timestampSerializer(Function.identity()));
      builder.serializerByType(OffsetDateTime.class, timestampSerializer(OffsetDateTime::toInstant));
    };
  }

  private static <T> JsonSerializer<T> timestampSerializer(Function<T, Instant> toInstant) {
    return new JsonSerializer<>() {
      @Override
      public void serialize(T value, JsonGenerator generator, SerializerProvider provider)
          throws IOException {
        generator.writeString(SECONDS.format(toInstant.apply(value)));
      }
    };
  }

  @Bean
  public JsonNullableModule jsonNullableModule() {
    return new JsonNullableModule();
  }
}
