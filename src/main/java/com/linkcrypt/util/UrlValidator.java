package com.linkcrypt.util;

import com.linkcrypt.core.exception.ValidationException;
import java.net.URI;
import java.net.URISyntaxException;

public final class UrlValidator {
  private UrlValidator() {}

  public static void validate(String url) {
    if (url == null || url.isBlank()) {
      throw new ValidationException("URL must not be empty");
    }

    try {
      URI uri = new URI(url);
      String scheme = uri.getScheme();
      if (scheme == null
          || !(scheme.equalsIgnoreCase("http") || scheme.equalsIgnoreCase("https"))) {
        throw new ValidationException("Only http/https URLs are allowed");
      }
      if (uri.getHost() == null || uri.getHost().isBlank()) {
        throw new ValidationException("URL must contain a host");
      }
    } catch (URISyntaxException e) {
      throw new ValidationException("Malformed URL: " + e.getMessage());
    }
  }
}
