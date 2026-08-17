// Licensed to the Apache Software Foundation (ASF) under one
// or more contributor license agreements.  See the NOTICE file
// distributed with this work for additional information
// regarding copyright ownership.  The ASF licenses this file
// to you under the Apache License, Version 2.0 (the
// "License"); you may not use this file except in compliance
// with the License.  You may obtain a copy of the License at
//
//   http://www.apache.org/licenses/LICENSE-2.0
//
// Unless required by applicable law or agreed to in writing,
// software distributed under the License is distributed on an
// "AS IS" BASIS, WITHOUT WARRANTIES OR CONDITIONS OF ANY
// KIND, either express or implied.  See the License for the
// specific language governing permissions and limitations
// under the License.

package org.apache.cloudstack.framework.kms;

import com.cloud.utils.exception.CloudRuntimeException;

/**
 * Exception class for KMS-related errors with structured error types
 * to enable proper retry logic and error handling.
 */
public class KMSException extends CloudRuntimeException {

    /**
     * Error types for KMS operations to enable intelligent retry logic
     */
    public enum ErrorType {
        CONNECTION_FAILED(true),
        /**
         * Authentication failed (e.g., incorrect PIN)
         */
        AUTHENTICATION_FAILED(false),
        /**
         * Provider not initialized or unavailable
         */
        PROVIDER_NOT_INITIALIZED(false),

        /**
         * KEK not found in backend
         */
        KEK_NOT_FOUND(false),

        /**
         * KEK with given label already exists
         */
        KEY_ALREADY_EXISTS(false),

        /**
         * Invalid parameters provided
         */
        INVALID_PARAMETER(false),

        /**
         * Wrap/unwrap operation failed
         */
        WRAP_UNWRAP_FAILED(true),

        /**
         * KEK operation (create/delete) failed
         */
        KEK_OPERATION_FAILED(true),

        /**
         * Health check failed
         */
        HEALTH_CHECK_FAILED(true),

        /**
         * Transient network or communication error
         */
        TRANSIENT_ERROR(true),

        /**
         * Unknown error
         */
        UNKNOWN(false);

        private final boolean retryable;

        ErrorType(boolean retryable) {
            this.retryable = retryable;
        }

        public boolean isRetryable() {
            return retryable;
        }
    }

    private final ErrorType errorType;

    public KMSException(String message) {
        super(message);
        this.errorType = ErrorType.UNKNOWN;
    }

    public KMSException(String message, Throwable cause) {
        super(message, cause);
        this.errorType = ErrorType.UNKNOWN;
    }

    public KMSException(ErrorType errorType, String message) {
        super(message);
        this.errorType = errorType;
    }

    public KMSException(ErrorType errorType, String message, Throwable cause) {
        super(message, cause);
        this.errorType = errorType;
    }

    public static KMSException providerNotInitialized(String details) {
        return new KMSException(ErrorType.PROVIDER_NOT_INITIALIZED,
                "KMS provider not initialized: " + details);
    }

    public static KMSException kekNotFound(String kekId) {
        return new KMSException(ErrorType.KEK_NOT_FOUND,
                "KEK not found: " + kekId);
    }

    public static KMSException keyAlreadyExists(String details) {
        return new KMSException(ErrorType.KEY_ALREADY_EXISTS,
                "Key already exists: " + details);
    }

    public static KMSException invalidParameter(String details) {
        return new KMSException(ErrorType.INVALID_PARAMETER,
                "Invalid parameter: " + details);
    }

    public static KMSException wrapUnwrapFailed(String details, Throwable cause) {
        return new KMSException(ErrorType.WRAP_UNWRAP_FAILED,
                "Wrap/unwrap operation failed: " + details, cause);
    }

    public static KMSException wrapUnwrapFailed(String details) {
        return new KMSException(ErrorType.WRAP_UNWRAP_FAILED,
                "Wrap/unwrap operation failed: " + details);
    }

    public static KMSException kekOperationFailed(String details, Throwable cause) {
        return new KMSException(ErrorType.KEK_OPERATION_FAILED,
                "KEK operation failed: " + details, cause);
    }

    public static KMSException kekOperationFailed(String details) {
        return new KMSException(ErrorType.KEK_OPERATION_FAILED,
                "KEK operation failed: " + details);
    }

    public static KMSException healthCheckFailed(String details, Throwable cause) {
        return new KMSException(ErrorType.HEALTH_CHECK_FAILED,
                "Health check failed: " + details, cause);
    }

    public static KMSException transientError(String details, Throwable cause) {
        return new KMSException(ErrorType.TRANSIENT_ERROR,
                "Transient error: " + details, cause);
    }

    public ErrorType getErrorType() {
        return errorType;
    }

    @Override
    public String toString() {
        return "KMSException{" +
               "errorType=" + errorType +
               ", retryable=" + isRetryable() +
               ", message='" + getMessage() + '\'' +
               '}';
    }

    public boolean isRetryable() {
        return errorType.isRetryable();
    }
}
