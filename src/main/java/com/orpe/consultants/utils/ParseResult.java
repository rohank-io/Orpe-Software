// File: src/main/java/com/orpe/consultants/utils/ParseResult.java
package com.orpe.consultants.utils;

import java.util.Collections;
import java.util.List;

/** Immutable parse result containing parsed rows and errors. */
public final class ParseResult<T> {
    private final List<T> rows;
    private final List<ParseError> errors;

    public ParseResult(List<T> rows, List<ParseError> errors) {
        this.rows = rows == null ? Collections.emptyList() : rows;
        this.errors = errors == null ? Collections.emptyList() : errors;
    }

    public List<T> getRows() { return rows; }
    public List<ParseError> getErrors() { return errors; }
    public boolean hasErrors() { return !errors.isEmpty(); }
}
