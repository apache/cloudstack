package org.apache.cloudstack.storage.datastore.protocol;

public enum DataStoreProtocol {
    NFS("nfs"),
    ISCSI("iscsi");
    
    private String name;
    DataStoreProtocol(String name) {
        this.name = name;
    }
    
    @Override
    public String toString() {
        return this.name;
    }
}
