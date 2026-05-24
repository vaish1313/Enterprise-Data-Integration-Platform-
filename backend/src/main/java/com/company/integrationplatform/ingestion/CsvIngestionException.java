package com.company.integrationplatform.ingestion;

/**
 * Thrown when a CSV file fails structural validation before or during parsing.
 *
 * <p>Covers:
 * <ul>
 *   <li>Wrong file type (not text/csv or .csv extension)</li>
 *   <li>File exceeds maximum allowed size</li>
 *   <li>Empty file or missing header row</li>
 *   <li>Malformed CSV structure (unclosed quotes, inconsistent column counts)</li>
 *   <li>Required header columns missing</li>
 * </ul>
 *
 * Maps to HTTP 422 Unprocessable Entity via {@link com.company.integrationplatform.exception.GlobalExceptionHandler}.
 */
public class CsvIngestionException extends RuntimeException {

    public CsvIngestionException(String message) {
        super(message);
    }

    public CsvIngestionException(String message, Throwable cause) {
        super(message, cause);
    }
}
