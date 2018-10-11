package com.cloud.storage.secondary;

import com.cloud.utils.exception.CloudRuntimeException;
import org.apache.cloudstack.engine.subsystem.api.storage.DataStore;

public interface CapacityChecker {
    double CAPACITY_THRESHOLD = 0.90;

    boolean hasEnoughCapacity(DataStore imageStore);

    default Long parse(String value) {
        try {
            return Long.parseLong(value);
        } catch (NumberFormatException e) {
            throw new CloudRuntimeException(String.format("unable to parse %s: %s.%n", value, e.getMessage()));
        }
    }
}
