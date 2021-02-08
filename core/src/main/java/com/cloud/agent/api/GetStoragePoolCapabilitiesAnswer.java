package com.cloud.agent.api;

public class GetStoragePoolCapabilitiesAnswer extends Answer {
    public GetStoragePoolCapabilitiesAnswer(GetStoragePoolCapabilitiesCommand cmd) {
        super(cmd);

        poolInfo = new StoragePoolInfo();
    }

    public StoragePoolInfo getPoolInfo() {
        return poolInfo;
    }

    public void setPoolInfo(StoragePoolInfo poolInfo) {
        this.poolInfo = poolInfo;
    }

    private StoragePoolInfo poolInfo;
}
