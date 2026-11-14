package com.winedine.llm;

/** The language model could not be reached or returned something unusable. */
public class LLMException extends RuntimeException {
    public LLMException(String message) { super(message); }

    public LLMException(String message, Throwable cause) { super(message, cause); }
}
