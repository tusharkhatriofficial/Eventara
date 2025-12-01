package com.eventara.notification.exception;

import com.eventara.common.exception.EventaraException;

public class NotificationChannelNotFoundException extends EventaraException {

    public NotificationChannelNotFoundException(String message) {
        super(message);
    }

    public NotificationChannelNotFoundException(String message, Throwable cause) {
        super(message, cause);
    }
}
