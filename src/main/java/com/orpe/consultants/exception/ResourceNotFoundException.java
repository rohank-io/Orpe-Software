package com.orpe.consultants.exception;

public class ResourceNotFoundException extends RuntimeException {

    private String resourceName;
    private String fieldName;
    private Object fieldValue;

    // Default constructor
    public ResourceNotFoundException() {
        super();
    }

    // Constructor with custom message only
    public ResourceNotFoundException(String message) {
        super(message);
    }

    // Constructor with message and cause
    public ResourceNotFoundException(String message, Throwable cause) {
        super(message, cause);
    }

    // Constructor with cause only
    public ResourceNotFoundException(Throwable cause) {
        super(cause);
    }

    // Constructor for long/numeric field values (your original style)
    public ResourceNotFoundException(String resourceName, String fieldName, long fieldValue) {
        super(String.format("%s not found with %s: %d", resourceName, fieldName, fieldValue));
        this.resourceName = resourceName;
        this.fieldName = fieldName;
        this.fieldValue = fieldValue;
    }

    // Constructor for string field values (your original style)
    public ResourceNotFoundException(String resourceName, String fieldName, String stringValue) {
        super(String.format("%s not found with %s: %s", resourceName, fieldName, stringValue));
        this.resourceName = resourceName;
        this.fieldName = fieldName;
        this.fieldValue = stringValue;
    }

    // Constructor for any object field values
    public ResourceNotFoundException(String resourceName, String fieldName, Object fieldValue) {
        super(String.format("%s not found with %s: %s", resourceName, fieldName, fieldValue));
        this.resourceName = resourceName;
        this.fieldName = fieldName;
        this.fieldValue = fieldValue;
    }

    // Getters and Setters
    public String getResourceName() {
        return resourceName;
    }

    public void setResourceName(String resourceName) {
        this.resourceName = resourceName;
    }

    public String getFieldName() {
        return fieldName;
    }

    public void setFieldName(String fieldName) {
        this.fieldName = fieldName;
    }

    public long getFieldValue() {
        if (fieldValue instanceof Number) {
            return ((Number) fieldValue).longValue();
        }
        return 0;
    }

    public void setFieldValue(long fieldValue) {
        this.fieldValue = fieldValue;
    }

    public String getStringValue() {
        return fieldValue != null ? fieldValue.toString() : null;
    }

    public void setStringValue(String stringValue) {
        this.fieldValue = stringValue;
    }

    public Object getFieldValueAsObject() {
        return fieldValue;
    }

    public void setFieldValueAsObject(Object fieldValue) {
        this.fieldValue = fieldValue;
    }
}
