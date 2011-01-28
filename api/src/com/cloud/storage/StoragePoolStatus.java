package com.cloud.storage;

public enum StoragePoolStatus {
    Up,
    PrepareForMaintenance,
    ErrorInMaintenance,
    CancelMaintenance,
    Maintenance,
    Removed;
}
