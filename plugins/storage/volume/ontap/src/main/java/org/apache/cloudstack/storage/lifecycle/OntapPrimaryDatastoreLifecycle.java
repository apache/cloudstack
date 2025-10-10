package org.apache.cloudstack.storage.lifecycle;


import com.cloud.agent.api.StoragePoolInfo;
import com.cloud.dc.ClusterVO;
import com.cloud.dc.dao.ClusterDao;
import com.cloud.host.HostVO;
import com.cloud.hypervisor.Hypervisor;
import com.cloud.resource.ResourceManager;
import com.cloud.storage.Storage;
import com.cloud.storage.StorageManager;
import com.cloud.storage.StoragePool;
import com.google.common.base.Preconditions;
import org.apache.cloudstack.engine.subsystem.api.storage.ClusterScope;
import org.apache.cloudstack.engine.subsystem.api.storage.DataStore;
import org.apache.cloudstack.engine.subsystem.api.storage.HostScope;
import org.apache.cloudstack.engine.subsystem.api.storage.PrimaryDataStoreInfo;
import org.apache.cloudstack.engine.subsystem.api.storage.PrimaryDataStoreLifeCycle;
import org.apache.cloudstack.engine.subsystem.api.storage.PrimaryDataStoreParameters;
import org.apache.cloudstack.engine.subsystem.api.storage.ZoneScope;
import org.apache.cloudstack.storage.datastore.lifecycle.BasePrimaryDataStoreLifeCycleImpl;
import org.apache.cloudstack.storage.ontap.StorageProviderManager;
import org.apache.cloudstack.storage.volume.datastore.PrimaryDataStoreHelper;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

import javax.inject.Inject;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.UUID;

public class OntapPrimaryDatastoreLifecycle extends BasePrimaryDataStoreLifeCycleImpl implements PrimaryDataStoreLifeCycle {
    @Inject private ClusterDao _clusterDao;
    @Inject private StorageManager _storageMgr;
    @Inject private ResourceManager _resourceMgr;
    @Inject private PrimaryDataStoreHelper _dataStoreHelper;
    private static final Logger s_logger = (Logger)LogManager.getLogger(OntapPrimaryDatastoreLifecycle.class);

    /**
     * Creates primary storage on NetApp storage
     * @param dsInfos
     * @return
     */
    @Override
    public DataStore initialize(Map<String, Object> dsInfos) {
        String url = (String) dsInfos.get("url");
        Long zoneId = (Long) dsInfos.get("zoneId");
        Long podId = (Long)dsInfos.get("podId");
        Long clusterId = (Long)dsInfos.get("clusterId");
        String storagePoolName = (String)dsInfos.get("name");
        String providerName = (String)dsInfos.get("providerName");
        String tags = (String)dsInfos.get("tags");
        Boolean isTagARule = (Boolean) dsInfos.get("isTagARule");
        String protocol = (String) dsInfos.get("protocol"); // TODO: Figure out the proper key for protocol
        // Additional details requested for ONTAP primary storage pool creation
        @SuppressWarnings("unchecked")
        Map<String, String> details = (Map<String, String>)dsInfos.get("details");
        // Validations
        if (podId != null && clusterId == null) {
            s_logger.error("Cluster Id is null, cannot create primary storage");
            return null;
        } else if (podId == null && clusterId != null) {
            s_logger.error("Pod Id is null, cannot create primary storage");
            return null;
        }

        if (podId == null && clusterId == null) {
            if (zoneId != null) {
                s_logger.info("Both Pod Id and Cluster Id are null, Primary storage pool will be associated with a Zone");
            } else {
                s_logger.error("Pod Id, Cluster Id and Zone Id are all null, cannot create primary storage");
                return null;
            }
        }

        PrimaryDataStoreParameters parameters = new PrimaryDataStoreParameters();
        if (clusterId != null) {
            ClusterVO clusterVO = _clusterDao.findById(clusterId);
            Preconditions.checkNotNull(clusterVO, "Unable to locate the specified cluster");
            parameters.setHypervisorType(clusterVO.getHypervisorType());
        }
        else {
            parameters.setHypervisorType(Hypervisor.HypervisorType.Any);
        }

        // Validate the ONTAP details
        StorageProviderManager storageProviderManager = new StorageProviderManager(details, protocol);
        boolean isValid = storageProviderManager.connect(details);
        //TODO: Use the return value to decide if we should proceed with pool creation

        if (isValid) {
            String volumeName = storagePoolName + "_vol"; //TODO: Figure out a better naming convention
            storageProviderManager.createVolume(volumeName, Long.parseLong(details.get("size"))); // TODO: size should be in bytes, so see if conversion is needed
            // TODO: The volume name should be stored against the StoragePool name/id in the DB
        } else {
            s_logger.error("ONTAP details validation failed, cannot create primary storage");
            return null; // TODO: Figure out a better exception handling mechanism
        }

        parameters.setTags(tags);
        parameters.setIsTagARule(isTagARule);
        parameters.setDetails(details);
        parameters.setType(Storage.StoragePoolType.Iscsi);
        parameters.setUuid(UUID.randomUUID().toString());
        parameters.setZoneId(zoneId);
        parameters.setPodId(podId);
        parameters.setClusterId(clusterId);
        parameters.setName(storagePoolName);
        parameters.setProviderName(providerName);
        parameters.setManaged(true);

        return _dataStoreHelper.createPrimaryDataStore(parameters);
    }

    @Override
    public boolean attachCluster(DataStore dataStore, ClusterScope scope) {
        PrimaryDataStoreInfo primarystore = (PrimaryDataStoreInfo)dataStore;
        List<HostVO> hostsToConnect = _resourceMgr.getEligibleUpAndEnabledHostsInClusterForStorageConnection(primarystore);

        logger.debug(String.format("Attaching the pool to each of the hosts %s in the cluster: %s", hostsToConnect, primarystore.getClusterId()));
        for (HostVO host : hostsToConnect) {
            // TODO: Fetch the host IQN and add to the initiator group on ONTAP cluster
            try {
                _storageMgr.connectHostToSharedPool(host, dataStore.getId());
            } catch (Exception e) {
                logger.warn("Unable to establish a connection between " + host + " and " + dataStore, e);
            }
        }
        _dataStoreHelper.attachCluster(dataStore);
        return true;
    }

    @Override
    public boolean attachHost(DataStore store, HostScope scope, StoragePoolInfo existingInfo) {
        return false;
    }

    @Override
    public boolean attachZone(DataStore dataStore, ZoneScope scope, Hypervisor.HypervisorType hypervisorType) {
        List<HostVO> hostsToConnect = new ArrayList<>();
        Hypervisor.HypervisorType[] hypervisorTypes = {Hypervisor.HypervisorType.XenServer, Hypervisor.HypervisorType.VMware, Hypervisor.HypervisorType.KVM};

        for (Hypervisor.HypervisorType type : hypervisorTypes) {
            hostsToConnect.addAll(_resourceMgr.getEligibleUpAndEnabledHostsInZoneForStorageConnection(dataStore, scope.getScopeId(), type));
        }

        logger.debug(String.format("In createPool. Attaching the pool to each of the hosts in %s.", hostsToConnect));
        for (HostVO host : hostsToConnect) {
            // TODO: Fetch the host IQN and add to the initiator group on ONTAP cluster
            try {
                _storageMgr.connectHostToSharedPool(host, dataStore.getId());
            } catch (Exception e) {
                logger.warn("Unable to establish a connection between " + host + " and " + dataStore, e);
            }
        }
        _dataStoreHelper.attachZone(dataStore);
        return true;
    }

    @Override
    public boolean maintain(DataStore store) {
        return true;
    }

    @Override
    public boolean cancelMaintain(DataStore store) {
        return true;
    }

    @Override
    public boolean deleteDataStore(DataStore store) {
        return true;
    }

    @Override
    public boolean migrateToObjectStore(DataStore store) {
        return true;
    }

    @Override
    public void updateStoragePool(StoragePool storagePool, Map<String, String> details) {

    }

    @Override
    public void enableStoragePool(DataStore store) {

    }

    @Override
    public void disableStoragePool(DataStore store) {

    }

    @Override
    public void changeStoragePoolScopeToZone(DataStore store, ClusterScope clusterScope, Hypervisor.HypervisorType hypervisorType) {

    }

    @Override
    public void changeStoragePoolScopeToCluster(DataStore store, ClusterScope clusterScope, Hypervisor.HypervisorType hypervisorType) {

    }
}