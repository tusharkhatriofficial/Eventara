package com.eventara.notification.exception;

import com.eventara.common.exception.EventaraException;

public class NotificationDeliveryException extends EventaraException {

    public NotificationDeliveryException(String message) {
        super(message);
    }

    public NotificationDeliveryException(String message, Throwable cause) {
        super(message, cause);
    }
}
