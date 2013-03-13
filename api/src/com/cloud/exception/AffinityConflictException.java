package com.cloud.exception;

import com.cloud.exception.CloudException;
import com.cloud.utils.SerialVersionUID;

public class AffinityConflictException extends CloudException {

    private static final long serialVersionUID = SerialVersionUID.AffinityConflictException;

    public AffinityConflictException(String message) {
        super(message);
    }

    public AffinityConflictException(String message, Throwable th) {
        super(message, th);
    }

}
