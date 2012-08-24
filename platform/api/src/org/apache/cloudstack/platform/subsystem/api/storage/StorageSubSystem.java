package org.apache.cloudstack.platform.subsystem.api.storage;

import java.net.URI;

import com.cloud.org.Grouping;

public interface StorageSubSystem {
    String getType();
    Class<? extends Grouping> getScope();

    create();

    URI grantAccess(String vol, String reservationId);
    URI RemoveAccess(String vol, String reservationId);
}
