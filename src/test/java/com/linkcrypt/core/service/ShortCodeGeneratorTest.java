package com.linkcrypt.core.service;

import static org.junit.jupiter.api.Assertions.*;

import java.util.Random;
import org.junit.jupiter.api.Test;

class ShortCodeGeneratorTest {

  @Test
  void generatesCodeOfExpectedLength() {
    ShortCodeGenerator generator = new ShortCodeGenerator(7);
    assertEquals(7, generator.generate().length());
  }

  @Test
  void generatesAlphanumeric() {
    ShortCodeGenerator generator = new ShortCodeGenerator(10, new Random(42));
    assertTrue(generator.generate().matches("[0-9A-Za-z]+"));
  }

  @Test
  void rejectsTooSmallLength() {
    assertThrows(IllegalArgumentException.class, () -> new ShortCodeGenerator(3));
  }

  @Test
  void distinctCodesAreUsuallyGenerated() {
    ShortCodeGenerator generator = new ShortCodeGenerator(7);
    assertEquals(
        100,
        java.util.stream.IntStream.range(0, 100)
            .mapToObj(i -> generator.generate())
            .distinct()
            .count());
  }
}
