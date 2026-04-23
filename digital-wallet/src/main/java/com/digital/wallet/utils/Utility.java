package com.digital.wallet.utils;

public class Utility {
    public static boolean isDuplicateKeyException(Exception e) {
        return e instanceof org.springframework.dao.DataIntegrityViolationException
                || (e.getCause() != null && e.getCause().getMessage().contains("Duplicate entry"));
    }
}
