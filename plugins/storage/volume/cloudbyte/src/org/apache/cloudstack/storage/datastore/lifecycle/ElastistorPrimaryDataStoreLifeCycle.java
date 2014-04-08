/*
 * Licensed to the Apache Software Foundation (ASF) under one
 * or more contributor license agreements.  See the NOTICE file
 * distributed with this work for additional information
 * regarding copyright ownership.  The ASF licenses this file
 * to you under the Apache License, Version 2.0 (the
 * "License"); you may not use this file except in compliance
 * with the License.  You may obtain a copy of the License at
 *
 *   http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing,
 * software distributed under the License is distributed on an
 * "AS IS" BASIS, WITHOUT WARRANTIES OR CONDITIONS OF ANY
 * KIND, either express or implied.  See the License for the
 * specific language governing permissions and limitations
 * under the License.
 */
package org.apache.cloudstack.storage.datastore.lifecycle;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.StringTokenizer;

import javax.inject.Inject;

import org.apache.cloudstack.engine.subsystem.api.storage.ClusterScope;
import org.apache.cloudstack.engine.subsystem.api.storage.DataStore;
import org.apache.cloudstack.engine.subsystem.api.storage.HostScope;
import org.apache.cloudstack.engine.subsystem.api.storage.PrimaryDataStoreInfo;
import org.apache.cloudstack.engine.subsystem.api.storage.PrimaryDataStoreLifeCycle;
import org.apache.cloudstack.engine.subsystem.api.storage.PrimaryDataStoreParameters;
import org.apache.cloudstack.engine.subsystem.api.storage.ZoneScope;
import org.apache.cloudstack.storage.datastore.db.PrimaryDataStoreDao;
import org.apache.cloudstack.storage.datastore.db.StoragePoolDetailsDao;
import org.apache.cloudstack.storage.datastore.db.StoragePoolVO;
import org.apache.cloudstack.storage.datastore.util.ElastistorUtil;
import org.apache.cloudstack.storage.datastore.util.ElastistorUtil.CreateTsmCmdResponse;
import org.apache.cloudstack.storage.datastore.util.ElastistorUtil.CreateVolumeCmdResponse;
import org.apache.cloudstack.storage.volume.datastore.PrimaryDataStoreHelper;
import org.apache.log4j.Logger;

import com.cloud.agent.AgentManager;
import com.cloud.agent.api.Answer;
import com.cloud.agent.api.CreateStoragePoolCommand;
import com.cloud.agent.api.DeleteStoragePoolCommand;
import com.cloud.agent.api.StoragePoolInfo;
import com.cloud.dc.dao.DataCenterDao;
import com.cloud.host.Host;
import com.cloud.host.HostVO;
import com.cloud.host.dao.HostDao;
import com.cloud.hypervisor.Hypervisor.HypervisorType;
import com.cloud.resource.ResourceManager;
import com.cloud.storage.Storage.StoragePoolType;
import com.cloud.storage.StorageManager;
import com.cloud.storage.StoragePool;
import com.cloud.storage.StoragePoolAutomation;
import com.cloud.storage.StoragePoolHostVO;
import com.cloud.storage.dao.StoragePoolHostDao;
import com.cloud.utils.exception.CloudRuntimeException;

public class ElastistorPrimaryDataStoreLifeCycle implements PrimaryDataStoreLifeCycle {
    private static final Logger s_logger = Logger.getLogger(ElastistorPrimaryDataStoreLifeCycle.class);

    @Inject
    HostDao _hostDao;
    @Inject
    StoragePoolHostDao _storagePoolHostDao;
    @Inject
    protected ResourceManager _resourceMgr;
    @Inject
    PrimaryDataStoreDao primaryDataStoreDao;
    @Inject
    AgentManager agentMgr;
    @Inject
    StorageManager storageMgr;
    @Inject
    PrimaryDataStoreHelper dataStoreHelper;
    @Inject
    PrimaryDataStoreDao _storagePoolDao;
    @Inject
    PrimaryDataStoreHelper _dataStoreHelper;
    @Inject
    StoragePoolAutomation _storagePoolAutomation;
    @Inject
    StoragePoolDetailsDao _storagePoolDetailsDao;
    @Inject
    DataCenterDao _zoneDao;

    @Override
    public DataStore initialize(Map<String, Object> dsInfos) {

        String url = (String) dsInfos.get("url");
        Long zoneId = (Long) dsInfos.get("zoneId");
        Long podId = (Long) dsInfos.get("podId");
        Long clusterId = (Long) dsInfos.get("clusterId");
        String storagePoolName = (String) dsInfos.get("name");
        String providerName = (String) dsInfos.get("providerName");
        Long capacityBytes = (Long) dsInfos.get("capacityBytes");
        Long capacityIops = (Long) dsInfos.get("capacityIops");
        String tags = (String) dsInfos.get("tags");
        Map<String, String> details = (Map<String, String>) dsInfos.get("details");
        String storageIp = getStorageIp(url);
        int storagePort = getDefaultStoragePort(url);
        StoragePoolType storagetype = getStorageType(url);
        String accesspath = getAccessPath(url);
        String protocoltype = getProtocolType(url);
        String[]  mp = accesspath.split("/");
        String mountpoint = mp[1];
        String uuid = null ;

        /**
         * if the elastistor params which are required for plugin configuration
         *  are not injected through spring-storage-volume-cloudbyte-context.xml, it can be set from details map.
         */
        if(details.get("esaccountid") != null)
            ElastistorUtil.setElastistorAccountId(details.get("esaccountid"));
        if(details.get("esapikey") != null)
            ElastistorUtil.setElastistorApiKey(details.get("esapikey"));
        if(details.get("esdefaultgateway") != null)
            ElastistorUtil.setElastistorGateway(details.get("esdefaultgateway"));
        if(details.get("estntinterface") != null)
            ElastistorUtil.setElastistorInterface(details.get("estntinterface"));
        if(details.get("esmanagementip") != null)
            ElastistorUtil.setElastistorManagementIp(details.get("esmanagementip"));
        if(details.get("espoolid") != null)
            ElastistorUtil.setElastistorPoolId(details.get("espoolid"));
        if(details.get("essubnet") != null)
            ElastistorUtil.setElastistorSubnet(details.get("essubnet"));

       if (capacityBytes == null || capacityBytes <= 0) {
            throw new IllegalArgumentException("'capacityBytes' must be present and greater than 0.");
        }

        if (capacityIops == null || capacityIops <= 0) {
            throw new IllegalArgumentException("'capacityIops' must be present and greater than 0.");
        }

        // elastistor does not allow same name and ip pools.
        List<StoragePoolVO> storagePoolVO = _storagePoolDao.listAll();
        for(StoragePoolVO poolVO : storagePoolVO){
        if (storagePoolName.equals(poolVO.getName())) {
            throw new IllegalArgumentException("storage pool with that name already exists in elastistor,please specify a unique name .");
        }
        if (storageIp.equals(poolVO.getHostAddress())) {
            throw new IllegalArgumentException("storage pool with that ip already exists in elastistor,please specify a unique ip .");
        }
        }

        PrimaryDataStoreParameters parameters = new PrimaryDataStoreParameters();

        // creates the volume in elastistor
        parameters = createElastistorVolume(parameters, storagePoolName, storageIp, capacityBytes, capacityIops, protocoltype, mountpoint);

        parameters.setHost(storageIp);
        parameters.setPort(storagePort);
        if(protocoltype.contentEquals("nfs")){
        parameters.setPath(accesspath);
        }
        parameters.setType(storagetype);
        parameters.setZoneId(zoneId);
        parameters.setPodId(podId);
        parameters.setName(storagePoolName);
        parameters.setProviderName(providerName);
        parameters.setManaged(false);
        parameters.setCapacityBytes(capacityBytes);
        parameters.setUsedBytes(0);
        parameters.setCapacityIops(capacityIops);
        parameters.setHypervisorType(HypervisorType.Any);
        parameters.setTags(tags);
        parameters.setDetails(details);
        parameters.setClusterId(clusterId);

        return _dataStoreHelper.createPrimaryDataStore(parameters);
    }

    private PrimaryDataStoreParameters createElastistorVolume(PrimaryDataStoreParameters parameters, String storagePoolName, String storageIp, Long capacityBytes, Long capacityIops, String protocoltype, String mountpoint){

        s_logger.info("creation of elastistor volume started");
        try {

            CreateTsmCmdResponse tsmCmdResponse = ElastistorUtil.createElastistorTsm(storagePoolName, storageIp, capacityBytes, capacityIops);

            String uuid = tsmCmdResponse.getTsm().getUuid();
            parameters.setUuid(uuid);

            CreateVolumeCmdResponse volumeCmdResponse = ElastistorUtil.createElastistorVolume(storagePoolName, tsmCmdResponse, capacityBytes, capacityIops, protocoltype ,mountpoint);

            if(protocoltype.contentEquals("iscsi")){
              String accesspath = "/"+volumeCmdResponse.getFileSystem().getIqn()+"/0";
              parameters.setPath(accesspath);
            }
        s_logger.info("creation of elastistor volume complete");

            return parameters;
        } catch (Throwable e) {
            throw new CloudRuntimeException("Failed to create volume in elastistor" + e.toString());
        }

    }

    private String getAccessPath(String url) {
        StringTokenizer st = new StringTokenizer(url ,"/");
        int count = 0;
        while (st.hasMoreElements()) {
      if(count == 2)
      {  String s = "/" ;
            return s.concat(st.nextElement().toString());
      }
      st.nextElement();
      count++;
      }
        return null;
    }


    private StoragePoolType getStorageType(String url) {

        StringTokenizer st = new StringTokenizer(url ,":");

    while (st.hasMoreElements())
    {
            String accessprotocol = st.nextElement().toString();

             if(accessprotocol.contentEquals("nfs"))
             {
                 return StoragePoolType.NetworkFilesystem;
             }
             else if(accessprotocol.contentEquals("iscsi"))
             {
                 return StoragePoolType.IscsiLUN;
             }

             else

                 break;

      }
    return null;
    }

    private String getProtocolType(String url) {
        StringTokenizer st = new StringTokenizer(url ,":");

        while (st.hasMoreElements())
        {
                String accessprotocol = st.nextElement().toString();

                 if(accessprotocol.contentEquals("nfs")){
                     return "nfs";
                 }else if(accessprotocol.contentEquals("iscsi")){
                     return "iscsi";
                 } else
                     break;
          }
        return null;
    }

    // this method parses the url and gets the default storage port based on access protocol
    private int getDefaultStoragePort(String url) {

        StringTokenizer st = new StringTokenizer(url ,":");

        while (st.hasMoreElements())
        {

                String accessprotocol = st.nextElement().toString();

                 if(accessprotocol.contentEquals("nfs")){
                     return 2049;
                 }
                 else if(accessprotocol.contentEquals("iscsi")){
                     return 3260;
                 }
                 else
                     break;

          }
        return -1;

    }

  // parses the url and returns the storage volume ip
    private String getStorageIp(String url) {

        StringTokenizer st = new StringTokenizer(url ,"/");
        int count = 0;

        while (st.hasMoreElements()) {
         if(count == 1)
              return st.nextElement().toString();

         st.nextElement();
            count++;
      }
        return null;
    }

     @Override
        public boolean attachCluster(DataStore store, ClusterScope scope) {

         dataStoreHelper.attachCluster(store);

           PrimaryDataStoreInfo primarystore = (PrimaryDataStoreInfo) store;
            // Check if there is host up in this cluster
            List<HostVO> allHosts = _resourceMgr.listAllUpAndEnabledHosts(Host.Type.Routing, primarystore.getClusterId(),
                    primarystore.getPodId(), primarystore.getDataCenterId());
            if (allHosts.isEmpty()) {
                primaryDataStoreDao.expunge(primarystore.getId());
                throw new CloudRuntimeException("No host up to associate a storage pool with in cluster "
                        + primarystore.getClusterId());
            }


            boolean success = false;
            for (HostVO h : allHosts) {
                success = createStoragePool(h.getId(), primarystore);
                if (success) {
                    break;
                }
            }

            s_logger.debug("In createPool Adding the pool to each of the hosts");
            List<HostVO> poolHosts = new ArrayList<HostVO>();
            for (HostVO h : allHosts) {
                try {
                    storageMgr.connectHostToSharedPool(h.getId(), primarystore.getId());
                   poolHosts.add(h);
                } catch (Exception e) {
                    s_logger.warn("Unable to establish a connection between " + h + " and " + primarystore, e);
                }

           if (poolHosts.isEmpty()) {
                s_logger.warn("No host can access storage pool " + primarystore + " on cluster "
                        + primarystore.getClusterId());
                primaryDataStoreDao.expunge(primarystore.getId());
                throw new CloudRuntimeException("Failed to access storage pool");
            }
         }

            return true;
        }

    private boolean createStoragePool(long hostId, StoragePool pool) {
          s_logger.debug("creating pool " + pool.getName() + " on  host " + hostId);
          if (pool.getPoolType() != StoragePoolType.NetworkFilesystem && pool.getPoolType() != StoragePoolType.Filesystem
                  && pool.getPoolType() != StoragePoolType.IscsiLUN && pool.getPoolType() != StoragePoolType.Iscsi
                  && pool.getPoolType() != StoragePoolType.VMFS && pool.getPoolType() != StoragePoolType.SharedMountPoint
                  && pool.getPoolType() != StoragePoolType.PreSetup && pool.getPoolType() != StoragePoolType.OCFS2
                  && pool.getPoolType() != StoragePoolType.RBD && pool.getPoolType() != StoragePoolType.CLVM) {
              s_logger.warn(" Doesn't support storage pool type " + pool.getPoolType());
              return false;
          }
          CreateStoragePoolCommand cmd = new CreateStoragePoolCommand(true, pool);
          final Answer answer = agentMgr.easySend(hostId, cmd);
          if (answer != null && answer.getResult()) {
              return true;
          } else {
              primaryDataStoreDao.expunge(pool.getId());
              String msg = "";
              if (answer != null) {
                  msg = "Can not create storage pool through host " + hostId + " due to " + answer.getDetails();
                  s_logger.warn(msg);
              } else {
                  msg = "Can not create storage pool through host " + hostId + " due to CreateStoragePoolCommand returns null";
                  s_logger.warn(msg);
              }
              throw new CloudRuntimeException(msg);
          }
    }


    @Override
    public boolean attachHost(DataStore store, HostScope scope, StoragePoolInfo existingInfo) {
        _dataStoreHelper.attachHost(store, scope, existingInfo);
        return true;
    }

    @Override
    public boolean attachZone(DataStore dataStore, ZoneScope scope, HypervisorType hypervisorType) {
    List<HostVO> hosts = _resourceMgr.listAllUpAndEnabledHostsInOneZoneByHypervisor(hypervisorType, scope.getScopeId());
    s_logger.debug("In createPool. Attaching the pool to each of the hosts.");
    List<HostVO> poolHosts = new ArrayList<HostVO>();
    for (HostVO host : hosts) {
        try {
            storageMgr.connectHostToSharedPool(host.getId(), dataStore.getId());
            poolHosts.add(host);
        } catch (Exception e) {
            s_logger.warn("Unable to establish a connection between " + host + " and " + dataStore, e);
        }
    }
    if (poolHosts.isEmpty()) {
        s_logger.warn("No host can access storage pool " + dataStore + " in this zone.");
        primaryDataStoreDao.expunge(dataStore.getId());
        throw new CloudRuntimeException("Failed to create storage pool as it is not accessible to hosts.");
    }
    dataStoreHelper.attachZone(dataStore, hypervisorType);
    return true;
    }

    @Override
    public boolean maintain(DataStore store) {
        _storagePoolAutomation.maintain(store);
        _dataStoreHelper.maintain(store);
        return true;
    }

    @Override
    public boolean cancelMaintain(DataStore store) {
        _dataStoreHelper.cancelMaintain(store);
        _storagePoolAutomation.cancelMaintain(store);
        return true;
    }

    @SuppressWarnings("finally")
    @Override
    public boolean deleteDataStore(DataStore store) {
        List<StoragePoolHostVO> hostPoolRecords = _storagePoolHostDao.listByPoolId(store.getId());
        StoragePool pool = (StoragePool) store;

        // find the hypervisor where the storage is attached to.
        HypervisorType hType = null;
        if(hostPoolRecords.size() > 0 ){
            hType = getHypervisorType(hostPoolRecords.get(0).getHostId());
        }

        // Remove the SR associated with the Xenserver
        for (StoragePoolHostVO host : hostPoolRecords) {
            DeleteStoragePoolCommand deleteCmd = new DeleteStoragePoolCommand(pool);
            final Answer answer = agentMgr.easySend(host.getHostId(), deleteCmd);

            if (answer != null && answer.getResult()) {
                // if host is KVM hypervisor then send deleteStoragepoolcmd to all the kvm hosts.
                if (HypervisorType.KVM != hType) {
                    break;
                }
            } else {
                if (answer != null) {
                    s_logger.error("Failed to delete storage pool: " + answer.getResult());
                }
            }
        }

        //delete the Elastistor volume at backend
            deleteElastistorVolume(pool);

        return _dataStoreHelper.deletePrimaryDataStore(store);
    }

    private void deleteElastistorVolume(StoragePool pool){

            String poolip = pool.getHostAddress();
            String esip = null;
            String apikey = null;

            // check if apikey and managentip is empty, if so getting it from stragepooldetails
            if(ElastistorUtil.s_esIPVAL == "" && ElastistorUtil.s_esAPIKEYVAL == ""){
            Map<String, String> detailsMap = _storagePoolDetailsDao.listDetailsKeyPairs(pool.getId());
            ElastistorUtil.setElastistorManagementIp(detailsMap.get("esmanagementip"));
            esip=ElastistorUtil.s_esIPVAL;
            ElastistorUtil.setElastistorApiKey(detailsMap.get("esapikey"));
            apikey = ElastistorUtil.s_esAPIKEYVAL;
            }else{
                esip = ElastistorUtil.s_esIPVAL;
                apikey = ElastistorUtil.s_esAPIKEYVAL;
            }

            boolean status;
            try {
                status = ElastistorUtil.deleteElastistorVolume(poolip,esip,apikey);
            } catch (Throwable e) {
                throw new CloudRuntimeException("Failed to delete primary storage on elastistor" + e);
            }

            if(status == true){
                s_logger.info("deletion of elastistor primary storage complete");
            }else{
                s_logger.error("deletion of elastistor volume failed");
            }

    }

    private HypervisorType getHypervisorType(long hostId) {
        HostVO host = _hostDao.findById(hostId);
        if (host != null)
            return host.getHypervisorType();
        return HypervisorType.None;
    }

    @Override
    public boolean migrateToObjectStore(DataStore store) {
        return false;
    }


}
