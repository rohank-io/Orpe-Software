package com.orpe.consultants.exception;

public class DataExtractionException extends RuntimeException {
    public DataExtractionException(String message) {
        super(message);
    }
    
    public DataExtractionException(String message, Throwable cause) {
        super(message, cause);
    }
}
