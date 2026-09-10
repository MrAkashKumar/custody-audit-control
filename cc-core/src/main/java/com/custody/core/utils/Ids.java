package com.custody.core.utils;

import java.security.SecureRandom;
import java.util.HexFormat;

public final class Ids {
  private static final SecureRandom RANDOM = new SecureRandom();

  private Ids() {}

  public static String next() {
    byte[] bytes = new byte[16];
    RANDOM.nextBytes(bytes);
    return HexFormat.of().formatHex(bytes);
  }
}
