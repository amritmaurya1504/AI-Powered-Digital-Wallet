package com.digital.wallet.services.impl;

import com.digital.wallet.entities.Idempotency;
import com.digital.wallet.enums.IdempotencyStatus;
import com.digital.wallet.exceptions.ResourceNotFoundException;
import com.digital.wallet.repositories.IdempotencyRepo;
import com.digital.wallet.services.IdempotencyService;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Propagation;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;


@Service
public class IdempotencyServiceImpl implements IdempotencyService {

    private final IdempotencyRepo idempotencyRepo;

    public IdempotencyServiceImpl(IdempotencyRepo idempotencyRepo){
        this.idempotencyRepo = idempotencyRepo;
    }

    @Override
    @Transactional(propagation = Propagation.REQUIRES_NEW)
    public void markSuccess(String key, String txnId) {

        Idempotency record = idempotencyRepo.findByIdempotencyKey(key).orElseThrow(
                () -> new ResourceNotFoundException("Idempotency Key not found")
        );

        record.setStatus(IdempotencyStatus.SUCCESS);
        record.setTxnId(txnId);
        record.setResponse(txnId);
        record.setUpdatedAt(LocalDateTime.now());
        idempotencyRepo.save(record);
    }

    @Override
    @Transactional(propagation = Propagation.REQUIRES_NEW)
    public boolean tryCreate(String key) {
        try {
            Idempotency record = new Idempotency();
            record.setIdempotencyKey(key);
            record.setStatus(IdempotencyStatus.PROCESSING);
            record.setCreatedAt(LocalDateTime.now());
            record.setUpdatedAt(LocalDateTime.now());

            idempotencyRepo.save(record);
            return true; // winner
        } catch (DataIntegrityViolationException e) {
            return false; // duplicate
        }
    }

    @Override
    public String handleDuplicate(String key) {
        return "";
    }

    @Override
    @Transactional(propagation = Propagation.REQUIRES_NEW)
    public void markFailed(String key, String error) {
        Idempotency record = idempotencyRepo.findByIdempotencyKey(key).orElseThrow(
                () -> new ResourceNotFoundException("Idempotency Key not found")
        );
        record.setStatus(IdempotencyStatus.FAILED);
        record.setError(error);
        record.setUpdatedAt(LocalDateTime.now());
        idempotencyRepo.save(record);
    }
}
