package com.cloud.storage.mount;

public interface MountManager {
    String getMountPoint(String storageUrl, Integer nfsVersion);
}
