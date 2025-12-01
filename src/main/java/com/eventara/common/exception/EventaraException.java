package com.eventara.common.exception;

public class EventaraException extends RuntimeException {

    public EventaraException(String message) {
        super(message);
    }

    public EventaraException(String message, Throwable cause) {
        super(message, cause);
    }
}
