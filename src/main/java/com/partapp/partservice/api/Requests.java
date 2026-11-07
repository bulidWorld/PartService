package com.partapp.partservice.api;

import java.time.Instant;
import java.util.Set;
import java.util.UUID;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;

public final class Requests {
  private Requests() {
  }

  public record SendCode(@NotBlank String identityType, @NotBlank String identityValue, @NotBlank String purpose) {
  }

  public record Register(@NotBlank String identityType, @NotBlank String identityValue, String code,
      @Size(min = 8, max = 72) String password, @NotBlank @Size(max = 32) String nickname,
      @NotNull UUID communityId) {
  }

  public record Login(@NotBlank String identityType, @NotBlank String identityValue, String code, String password,
      String deviceId) {
  }

  public record ChangePassword(@NotBlank String currentPassword, @NotBlank @Size(min = 8, max = 72) String newPassword) {
  }

  public record Refresh(@NotBlank String refreshToken) {
  }

  public record Profile(String nickname, String gender, java.time.LocalDate birthDate, String avatarUrl) {
  }

  public record Community(@NotNull UUID communityId) {
  }

  public record Topic(@NotBlank @Size(max = 80) String title, @NotBlank String detail, Integer minAge, Integer maxAge,
      Set<String> genders, Instant expiresAt) {
  }


  public record Message(@NotBlank @Size(max = 64) String clientMessageId, @NotBlank String messageType, String content,
      java.util.Map<String, Object> contentMeta) {
  }

  public record Read(@NotNull UUID lastReadMessageId) {
  }

  public record Rating(@NotNull UUID toUserId, @NotBlank String score, Set<String> categoryCodes,
      @Size(max = 500) String comment) {
  }
}
