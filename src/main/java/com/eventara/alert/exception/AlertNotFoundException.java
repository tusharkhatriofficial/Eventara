package com.eventara.alert.exception;

import com.eventara.common.exception.EventaraException;

public class AlertNotFoundException extends EventaraException {

    public AlertNotFoundException(String message) {
        super(message);
    }

    public AlertNotFoundException(String message, Throwable cause) {
        super(message, cause);
    }
}
