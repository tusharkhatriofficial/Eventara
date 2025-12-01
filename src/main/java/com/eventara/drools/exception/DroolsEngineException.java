package com.eventara.drools.exception;

import com.eventara.common.exception.EventaraException;

public class DroolsEngineException extends EventaraException {

    public DroolsEngineException(String message) {
        super(message);
    }

    public DroolsEngineException(String message, Throwable cause) {
        super(message, cause);
    }
}
