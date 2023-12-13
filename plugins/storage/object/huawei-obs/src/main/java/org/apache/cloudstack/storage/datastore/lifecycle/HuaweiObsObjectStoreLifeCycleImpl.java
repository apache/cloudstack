package org.apache.cloudstack.storage.datastore.lifecycle;

import com.cloud.agent.api.StoragePoolInfo;
import com.cloud.hypervisor.Hypervisor.HypervisorType;
import com.cloud.utils.exception.CloudRuntimeException;
import com.obs.services.ObsClient;
import com.obs.services.model.ListBucketsRequest;
import org.apache.cloudstack.engine.subsystem.api.storage.ClusterScope;
import org.apache.cloudstack.engine.subsystem.api.storage.DataStore;
import org.apache.cloudstack.engine.subsystem.api.storage.HostScope;
import org.apache.cloudstack.engine.subsystem.api.storage.ZoneScope;
import org.apache.cloudstack.storage.datastore.db.ObjectStoreVO;
import org.apache.cloudstack.storage.object.datastore.ObjectStoreHelper;
import org.apache.cloudstack.storage.object.datastore.ObjectStoreProviderManager;
import org.apache.cloudstack.storage.object.store.lifecycle.ObjectStoreLifeCycle;
import org.apache.log4j.Logger;

import javax.inject.Inject;
import java.util.HashMap;
import java.util.Map;

public class HuaweiObsObjectStoreLifeCycleImpl implements ObjectStoreLifeCycle {

    private static final Logger LOG = Logger.getLogger(HuaweiObsObjectStoreLifeCycleImpl.class);

    @Inject
    ObjectStoreHelper objectStoreHelper;
    @Inject
    ObjectStoreProviderManager objectStoreMgr;

    public HuaweiObsObjectStoreLifeCycleImpl() {
    }

    @SuppressWarnings("unchecked")
    @Override
    public DataStore initialize(Map<String, Object> dsInfos) {

        String url = (String) dsInfos.get("url");
        String name = (String) dsInfos.get("name");
        String providerName = (String) dsInfos.get("providerName");
        Map<String, String> details = (Map<String, String>) dsInfos.get("details");
        if (details == null) {
            throw new CloudRuntimeException("Huawei OBS credentials are missing");
        }
        String accessKey = details.get("accesskey");
        String secretKey = details.get("secretkey");

        Map<String, Object> objectStoreParameters = new HashMap();
        objectStoreParameters.put("name", name);
        objectStoreParameters.put("url", url);

        objectStoreParameters.put("providerName", providerName);
        objectStoreParameters.put("accesskey", accessKey);
        objectStoreParameters.put("secretkey", secretKey);

        try {
            //check credentials
            ObsClient obsClient = new ObsClient(accessKey, secretKey, url);
            // Test connection by listing buckets
            ListBucketsRequest request = new ListBucketsRequest();
            request.setQueryLocation(true);
            obsClient.listBucketsV2(request);
            LOG.debug("Successfully connected to Huawei OBS EndPoint: " + url);
        } catch (Exception ex) {
            LOG.debug("Error while initializing Huawei OBS Object Store: " + ex.getMessage());
            throw new RuntimeException("Error while initializing Huawei OBS Object Store. Invalid credentials or endpoint URL");
        }

        ObjectStoreVO objectStore = objectStoreHelper.createObjectStore(objectStoreParameters, details);
        return objectStoreMgr.getObjectStore(objectStore.getId());
    }

    @Override
    public boolean attachCluster(DataStore store, ClusterScope scope) {
        return false;
    }

    @Override
    public boolean attachHost(DataStore store, HostScope scope, StoragePoolInfo existingInfo) {
        return false;
    }

    @Override
    public boolean attachZone(DataStore dataStore, ZoneScope scope, HypervisorType hypervisorType) {
        return false;
    }

    @Override
    public boolean maintain(DataStore store) {
        return false;
    }

    @Override
    public boolean cancelMaintain(DataStore store) {
        return false;
    }

    @Override
    public boolean deleteDataStore(DataStore store) {
        return false;
    }

    /* (non-Javadoc)
     * @see org.apache.cloudstack.engine.subsystem.api.storage.DataStoreLifeCycle#migrateToObjectStore(org.apache.cloudstack.engine.subsystem.api.storage.DataStore)
     */
    @Override
    public boolean migrateToObjectStore(DataStore store) {
        return false;
    }

}
