package com.linkcrypt.core.service;

import java.security.SecureRandom;
import java.util.Random;

public class ShortCodeGenerator {
  private static final char[] ALPHABET =
      "0123456789ABCDEFGHIJKLMNOPQRSTUVWXYZabcdefghijklmnopqrstuvwxyz".toCharArray();
  private final Random random;
  private final int length;

  public ShortCodeGenerator(int length) {
    this(length, new SecureRandom());
  }

  public ShortCodeGenerator(int length, Random random) {
    if (length < 4) {
      throw new IllegalArgumentException("code length must be >= 4");
    }

    this.length = length;
    this.random = random;
  }

  public String generate() {
    char[] buf = new char[length];
    for (int i = 0; i < length; i++) {
      buf[i] = ALPHABET[random.nextInt(ALPHABET.length)];
    }

    return new String(buf);
  }
}
