package com.partapp.partservice.security;

import java.security.MessageDigest;
import java.security.SecureRandom;
import java.util.Base64;
import javax.crypto.SecretKeyFactory;
import javax.crypto.spec.PBEKeySpec;

/** PBKDF2 password hashing with a per-password random salt. */
public final class PasswordHasher {
  private static final String ALGORITHM = "PBKDF2WithHmacSHA256";
  private static final int ITERATIONS = 310_000;
  private static final int SALT_BYTES = 16;
  private static final int HASH_BITS = 256;
  private static final SecureRandom RANDOM = new SecureRandom();

  private PasswordHasher() {}

  public static String hash(String password) {
    if (password == null || password.isBlank() || password.length() < 8 || password.length() > 72)
      throw new IllegalArgumentException("密码长度必须为 8 至 72 个字符");
    byte[] salt = new byte[SALT_BYTES];
    RANDOM.nextBytes(salt);
    byte[] derived = derive(password, salt, ITERATIONS);
    return "pbkdf2-sha256$" + ITERATIONS + "$" + Base64.getUrlEncoder().withoutPadding().encodeToString(salt)
        + "$" + Base64.getUrlEncoder().withoutPadding().encodeToString(derived);
  }

  public static boolean matches(String password, String encoded) {
    if (password == null || encoded == null)
      return false;
    try {
      String[] parts = encoded.split("\\$", -1);
      if (parts.length != 4 || !parts[0].equals("pbkdf2-sha256"))
        return false;
      int iterations = Integer.parseInt(parts[1]);
      if (iterations < 100_000 || iterations > 2_000_000)
        return false;
      byte[] salt = Base64.getUrlDecoder().decode(parts[2]);
      byte[] expected = Base64.getUrlDecoder().decode(parts[3]);
      if (salt.length != SALT_BYTES || expected.length != HASH_BITS / 8)
        return false;
      return MessageDigest.isEqual(expected, derive(password, salt, iterations));
    } catch (RuntimeException e) {
      return false;
    }
  }

  private static byte[] derive(String password, byte[] salt, int iterations) {
    PBEKeySpec spec = new PBEKeySpec(password.toCharArray(), salt, iterations, HASH_BITS);
    try {
      return SecretKeyFactory.getInstance(ALGORITHM).generateSecret(spec).getEncoded();
    } catch (Exception e) {
      throw new IllegalStateException("无法计算密码哈希", e);
    } finally {
      spec.clearPassword();
    }
  }
}
