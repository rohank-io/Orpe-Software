// File: src/main/java/com/orpe/consultants/utils/ParseError.java
package com.orpe.consultants.utils;

/**
 * Represents an error for a specific sheet/row/column observed during parsing.
 */
public final class ParseError {
    public final String sheetName;
    public final int rowIndex;
    public final Integer colIndex; // nullable
    public final String message;

    public ParseError(String sheetName, int rowIndex, Integer colIndex, String message) {
        this.sheetName = sheetName;
        this.rowIndex = rowIndex;
        this.colIndex = colIndex;
        this.message = message;
    }

    @Override
    public String toString() {
        return "ParseError{" +
                "sheetName='" + sheetName + '\'' +
                ", rowIndex=" + rowIndex +
                ", colIndex=" + colIndex +
                ", message='" + message + '\'' +
                '}';
    }
}
