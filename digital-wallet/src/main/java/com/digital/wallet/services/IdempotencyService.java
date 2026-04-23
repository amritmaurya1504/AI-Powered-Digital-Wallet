package com.digital.wallet.services;

public interface IdempotencyService {

    boolean tryCreate(String key);

    String handleDuplicate(String key);

    void markSuccess(String key, String txnId);

    void markFailed(String key, String error);
}
