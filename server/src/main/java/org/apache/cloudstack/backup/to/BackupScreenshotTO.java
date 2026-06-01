package org.apache.cloudstack.backup.to;

import com.cloud.agent.api.to.DataObjectType;
import com.cloud.agent.api.to.DataStoreTO;
import com.cloud.agent.api.to.DataTO;
import com.cloud.hypervisor.Hypervisor;

public class BackupScreenshotTO implements DataTO {
    DataStoreTO dataStoreTO;
    Hypervisor.HypervisorType hypervisor;
    String path;

    public BackupScreenshotTO(DataStoreTO dataStoreTO, Hypervisor.HypervisorType hypervisor, String path) {
        this.dataStoreTO = dataStoreTO;
        this.hypervisor = hypervisor;
        this.path = path;
    }

    @Override
    public DataObjectType getObjectType() {
        return DataObjectType.ARCHIVE;
    }

    @Override
    public DataStoreTO getDataStore() {
        return dataStoreTO;
    }

    @Override
    public Hypervisor.HypervisorType getHypervisorType() {
        return hypervisor;
    }

    @Override
    public String getPath() {
        return path;
    }

    @Override
    public long getId() {
        return 0;
    }
}
