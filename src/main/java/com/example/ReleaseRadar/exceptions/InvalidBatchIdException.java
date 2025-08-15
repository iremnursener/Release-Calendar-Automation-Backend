package com.example.ReleaseRadar.exceptions;

public class InvalidBatchIdException extends RuntimeException {
    public InvalidBatchIdException(String message) {
        super(message);
    }
}
