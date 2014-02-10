package org.apache.cloudstack.storage.datastore.model;

import com.google.gson.annotations.SerializedName;

public class StorageVolumes {

    @SerializedName("count")
    private int count;

    @SerializedName("filesystem")
    private StorageVolume[] volumes;

    public int getCount() {
        return count;
    }

    public StorageVolume getStorageVolume(int i) {
        return volumes[i];
    }
}
