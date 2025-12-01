package com.eventara.alert.exception;

import com.eventara.common.exception.EventaraException;

public class AlertProcessingException extends EventaraException {

    public AlertProcessingException(String message) {
        super(message);
    }

    public AlertProcessingException(String message, Throwable cause) {
        super(message, cause);
    }
}
