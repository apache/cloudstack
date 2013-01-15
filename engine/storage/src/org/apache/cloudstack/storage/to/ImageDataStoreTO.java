package org.apache.cloudstack.storage.to;

import org.apache.cloudstack.storage.image.datastore.ImageDataStoreInfo;

public class ImageDataStoreTO {
    private final String type;
    private final String uri;
    public ImageDataStoreTO(ImageDataStoreInfo dataStore) {
        this.type = dataStore.getType();
        this.uri = dataStore.getUri();
    }
    
    public String getType() {
        return this.type;
    }
    
    public String getUri() {
        return this.uri;
    }
}
