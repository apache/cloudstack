package org.apache.cloudstack.engine.subsystem.api.storage;

import java.net.URI;

import com.cloud.org.Grouping;

public interface StorageSubSystem {
    String getType();
    Class<? extends Grouping> getScope();

    URI grantAccess(String vol, String reservationId);
    URI RemoveAccess(String vol, String reservationId);
}
