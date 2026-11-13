package com.linkcrypt.util;

import static org.junit.jupiter.api.Assertions.*;

import com.linkcrypt.core.exception.ValidationException;
import org.junit.jupiter.api.Test;

class UrlValidatorTest {

  @Test
  void acceptsHttp() {
    assertDoesNotThrow(() -> UrlValidator.validate("http://example.com"));
  }

  @Test
  void acceptsHttps() {
    assertDoesNotThrow(() -> UrlValidator.validate("https://www.baeldung.com/java-9-http-client"));
  }

  @Test
  void rejectsNull() {
    assertThrows(ValidationException.class, () -> UrlValidator.validate(null));
  }

  @Test
  void rejectsEmpty() {
    assertThrows(ValidationException.class, () -> UrlValidator.validate("   "));
  }

  @Test
  void rejectsBadProtocol() {
    assertThrows(ValidationException.class, () -> UrlValidator.validate("ftp://example.com"));
  }

  @Test
  void rejectsMalformed() {
    assertThrows(ValidationException.class, () -> UrlValidator.validate("http://"));
  }
}
