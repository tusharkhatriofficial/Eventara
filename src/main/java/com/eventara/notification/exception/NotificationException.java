package com.eventara.notification.exception;

import com.eventara.common.exception.EventaraException;

public class NotificationException extends EventaraException {

    public NotificationException(String message) {
        super(message);
    }

    public NotificationException(String message, Throwable cause) {
        super(message, cause);
    }
}
