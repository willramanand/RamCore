package dev.willram.ramcore.data;

/**
 * Runtime wrapper for repository load/save failures.
 */
public final class DataRepositoryException extends RuntimeException {
    public DataRepositoryException(String message, Throwable cause) {
        super(message, cause);
    }
}
