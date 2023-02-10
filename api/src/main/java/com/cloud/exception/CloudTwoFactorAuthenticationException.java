package com.cloud.exception;

import com.cloud.utils.SerialVersionUID;
import com.cloud.utils.exception.CloudRuntimeException;

public class CloudTwoFactorAuthenticationException extends CloudRuntimeException {
    private static final long serialVersionUID = SerialVersionUID.CloudTwoFactorAuthenticationException;

    public CloudTwoFactorAuthenticationException(String message) {
        super(message);
    }

    public CloudTwoFactorAuthenticationException(String message, Throwable th) {
        super(message, th);
    }
}
