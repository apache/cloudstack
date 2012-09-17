package com.cloud.simulator;

import com.cloud.utils.SerialVersionUID;
import com.cloud.utils.exception.RuntimeCloudException;

/**
 * wrap exceptions that you know there's no point in dealing with.
 */
public class SimulatorRuntimeException extends RuntimeCloudException {

    private static final long serialVersionUID = SerialVersionUID.CloudRuntimeException;
    
    public SimulatorRuntimeException(String message) {
        super(message);
    }
    
    public SimulatorRuntimeException(String message, Throwable th) {
        super(message, th);
    }
    
    protected SimulatorRuntimeException() {
        super();
    }
}