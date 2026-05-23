package com.url_shortner.exception;

public class AliasAlreadyExistsException extends RuntimeException {
    public AliasAlreadyExistsException(String alias) {
        super("Custom alias already exists: " + alias);
    }
}
