package com.linkcrypt.core.exception;

public class LimitExceededException extends RuntimeException {
    public LimitExceededException(String code) {
        super("Click limit reached for link: " + code);
    }
}
