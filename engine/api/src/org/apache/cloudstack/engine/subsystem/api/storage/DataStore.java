package org.apache.cloudstack.engine.subsystem.api.storage;

public interface DataStore {
    DataStoreDriver getDriver();
    DataStoreRole getRole();
    long getId();
    String getUri();
    Scope getScope();
}
