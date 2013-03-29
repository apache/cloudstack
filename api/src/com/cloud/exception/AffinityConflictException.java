package com.cloud.exception;

import com.cloud.utils.SerialVersionUID;
import com.cloud.utils.exception.CloudRuntimeException;

public class AffinityConflictException extends CloudRuntimeException {

    private static final long serialVersionUID = SerialVersionUID.AffinityConflictException;

    public AffinityConflictException(String message) {
        super(message);
    }

    public AffinityConflictException(String message, Throwable th) {
        super(message, th);
    }

}
