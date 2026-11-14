package com.winedine.data;

/** Thrown when the restaurant dataset cannot be read or contains invalid data. */
public class DataLoadException extends RuntimeException {
    public DataLoadException(String message) { super(message); }

    public DataLoadException(String message, Throwable cause) { super(message, cause); }
}
