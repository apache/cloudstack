package com.cloud.agent.api;

import com.cloud.agent.api.to.StorageFilerTO;

public class GetStoragePoolCapabilitiesCommand extends Command {

    public StorageFilerTO getPool() {
        return pool;
    }

    public void setPool(StorageFilerTO pool) {
        this.pool = pool;
    }

    private StorageFilerTO pool;

    @Override
    public boolean executeInSequence() {
        return false;
    }
}
