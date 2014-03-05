package org.apache.cloudstack.storage.datastore.lifecycle;

import java.security.InvalidParameterException;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.StringTokenizer;

import javax.inject.Inject;
import javax.naming.ServiceUnavailableException;
import javax.net.ssl.SSLHandshakeException;

import org.apache.cloudstack.engine.subsystem.api.storage.ClusterScope;
import org.apache.cloudstack.engine.subsystem.api.storage.DataStore;
import org.apache.cloudstack.engine.subsystem.api.storage.HostScope;
import org.apache.cloudstack.engine.subsystem.api.storage.PrimaryDataStoreInfo;
import org.apache.cloudstack.engine.subsystem.api.storage.PrimaryDataStoreLifeCycle;
import org.apache.cloudstack.engine.subsystem.api.storage.PrimaryDataStoreParameters;
import org.apache.cloudstack.engine.subsystem.api.storage.ZoneScope;
import org.apache.cloudstack.storage.datastore.client.Config;
import org.apache.cloudstack.storage.datastore.client.ElastiCenterClient;
import org.apache.cloudstack.storage.datastore.command.AddQosGroupCmd;
import org.apache.cloudstack.storage.datastore.command.CreateTsmCmd;
import org.apache.cloudstack.storage.datastore.command.CreateVolumeCmd;
import org.apache.cloudstack.storage.datastore.db.PrimaryDataStoreDao;
import org.apache.cloudstack.storage.datastore.db.StoragePoolDetailsDao;
import org.apache.cloudstack.storage.datastore.response.AddQosGroupCmdResponse;
import org.apache.cloudstack.storage.datastore.response.CreateTsmCmdResponse;
import org.apache.cloudstack.storage.datastore.response.CreateVolumeCmdResponse;
import org.apache.cloudstack.storage.datastore.util.ElastistorUtil;
import org.apache.cloudstack.storage.volume.datastore.PrimaryDataStoreHelper;
import org.apache.http.auth.InvalidCredentialsException;
import org.apache.log4j.Logger;

import com.cloud.agent.AgentManager;
import com.cloud.agent.api.Answer;
import com.cloud.agent.api.CreateStoragePoolCommand;
import com.cloud.agent.api.DeleteStoragePoolCommand;
import com.cloud.agent.api.StoragePoolInfo;
import com.cloud.dc.DataCenterVO;
import com.cloud.dc.dao.DataCenterDao;
import com.cloud.host.Host;
import com.cloud.host.HostVO;
import com.cloud.host.dao.HostDao;
import com.cloud.hypervisor.Hypervisor.HypervisorType;
import com.cloud.resource.ResourceManager;
import com.cloud.storage.StorageManager;
import com.cloud.storage.StoragePool;
import com.cloud.storage.StoragePoolAutomation;
import com.cloud.storage.StoragePoolHostVO;
import com.cloud.storage.Storage.StoragePoolType;
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
        
        s_logger.info("elastistor plugin configured");
        
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
        
        String storageIp = getStorageVip(url);      
        int storagePort = getDefaultStoragePort(url);
        String storagetype = getstoragetype(url);
        String accesspath = getAccessPath(url);
        String protocoltype = getprotocoltype(url);
        
        // for elstistor purpose 
        String[]  mp = accesspath.split("/");
        String mountpoint = mp[1];


        String uuid = null ;
        
        
       if (capacityBytes == null || capacityBytes <= 0) {
            throw new IllegalArgumentException("'capacityBytes' must be present and greater than 0.");
        }

        if (capacityIops == null || capacityIops <= 0) {
            throw new IllegalArgumentException("'capacityIops' must be present and greater than 0.");
        }
    


        
        try {
            
            
            CreateTsmCmdResponse tsmCmdResponse = ElastistorUtil.createElastistorTsm(storagePoolName, storageIp, capacityBytes, capacityIops);
            
            uuid = tsmCmdResponse.getTsm().getUuid();
                        
            CreateVolumeCmdResponse volumeCmdResponse = ElastistorUtil.createElastistorVolume(storagePoolName, tsmCmdResponse, capacityBytes, capacityIops, protocoltype ,mountpoint);
            
            if(protocoltype == "iscsi")
            {
              accesspath = "/"+volumeCmdResponse.getFileSystem().getIqn()+"/0";
              
            } 
            
        } catch (Throwable e) {

            e.printStackTrace();
            throw new CloudRuntimeException("Failed to add data store", e);
        }        
        
      
        PrimaryDataStoreParameters parameters = new PrimaryDataStoreParameters();

        parameters.setHost(storageIp);
        parameters.setPort(storagePort);
        parameters.setPath(accesspath);
        parameters.setType(StoragePoolType.valueOf(storagetype));
        parameters.setUuid(uuid);
        parameters.setZoneId(zoneId);
        parameters.setPodId(podId);
        parameters.setName(storagePoolName);
        parameters.setProviderName(providerName);
        if(protocoltype == "nfs")
        {   
        parameters.setManaged(false);
        }
        else
        {
        parameters.setManaged(true);
        }
        parameters.setCapacityBytes(capacityBytes);
        parameters.setUsedBytes(0);
        parameters.setCapacityIops(capacityIops);
        parameters.setHypervisorType(HypervisorType.Any);
        parameters.setTags(tags);
        parameters.setDetails(details);
        parameters.setClusterId(clusterId);
        // this adds a row in the cloud.storage_pool table for this cloudbyte
        // cluster
        return _dataStoreHelper.createPrimaryDataStore(parameters);
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


    private String getstoragetype(String url) {
        
        StringTokenizer st = new StringTokenizer(url ,":");
     
    while (st.hasMoreElements()) 
    {
        
          
            String accessprotocol = st.nextElement().toString();
             
             if(accessprotocol.contentEquals("nfs"))
             {
                 return "NetworkFilesystem";
             }   
             else if(accessprotocol.contentEquals("iscsi"))
             {
                 return "IscsiLUN";
             }   
          
             else 
                 
                 break;
        
      }
    return null;
    }
    
    private String getprotocoltype(String url) {
        StringTokenizer st = new StringTokenizer(url ,":");
        
         
        while (st.hasMoreElements()) 
        {
            
              
                String accessprotocol = st.nextElement().toString();
                 
                 if(accessprotocol.contentEquals("nfs"))
                 {
                     return "nfs";
                 }   
                 else if(accessprotocol.contentEquals("iscsi"))
                 {
                     return "iscsi";
                 }   
              
                 else 
                     
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
                 
                 if(accessprotocol.contentEquals("nfs"))
                 {
                     return 2049;
                 }   
                 else if(accessprotocol.contentEquals("iscsi"))
                 {
                     return 3260;
                 }   
              
                 else 
                     
                     break;
            
          }
        return -1;
        
        
    }

    
  // parses the url and returns the storage volume ip
    private String getStorageVip(String url) {
        
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

    
/*
    @Override
    public boolean attachCluster(DataStore store, ClusterScope scope) {
        _dataStoreHelper.attachCluster(store);
        return true;
    }
*/
    
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
                  msg = "Can not create storage pool through host " + hostId
                          + " due to CreateStoragePoolCommand returns null";
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
        dataStoreHelper.attachZone(dataStore);
        
        List<HostVO> xenServerHosts = _resourceMgr.listAllUpAndEnabledHostsInOneZoneByHypervisor(HypervisorType.XenServer, scope.getScopeId());
        List<HostVO> vmWareServerHosts = _resourceMgr.listAllUpAndEnabledHostsInOneZoneByHypervisor(HypervisorType.VMware, scope.getScopeId());
        List<HostVO> kvmHosts = _resourceMgr.listAllUpAndEnabledHostsInOneZoneByHypervisor(HypervisorType.KVM, scope.getScopeId());
        List<HostVO> hosts = new ArrayList<HostVO>();

        hosts.addAll(xenServerHosts);
        hosts.addAll(vmWareServerHosts);
        hosts.addAll(kvmHosts);

        for (HostVO host : hosts) {
            try {
                storageMgr.connectHostToSharedPool(host.getId(), dataStore.getId());
            } catch (Exception e) {
                s_logger.warn("Unable to establish a connection between " + host + " and " + dataStore, e);
            }
    }

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
        
        boolean deleteFlag = false;
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
                deleteFlag = true;
                // if host is KVM hypervisor then send deleteStoragepoolcmd to all the kvm hosts.
                if (HypervisorType.KVM != hType) {
                    break;
                }
            } else {
                if (answer != null) {
                    s_logger.debug("Failed to delete storage pool: " + answer.getResult());
                }
            }
        }
        /*
        if (!deleteFlag) {
            throw new CloudRuntimeException("Failed to delete storage pool on host");
             }*/
        
 
        try{
          
            String poolip = pool.getHostAddress();
            String esip = ElastistorUtil.ES_IP_VAL;
            String apikey = ElastistorUtil.ES_API_KEY_VAL;
            
            boolean status = ElastistorUtil.deleteElastistorVolume(poolip,esip,apikey);
            
            if(status == true)
            {
                s_logger.info("deletion of elastistor storage pool complete");
            }
            else
            {
                s_logger.warn("deletion of elastistor volume failed");
            }
                
        }
         catch (Throwable e) {
            e.printStackTrace();
            throw new CloudRuntimeException("Failed to delete storage pool on ELASTISTOR, now clearing the DB");
        }
        
        return _dataStoreHelper.deletePrimaryDataStore(store);
        
    }
        

    private HypervisorType getHypervisorType(long hostId) {
        HostVO host = _hostDao.findById(hostId);
        if (host != null)
            return host.getHypervisorType();
        return HypervisorType.None;
    }


    @Override
    public boolean migrateToObjectStore(DataStore store) {
        // TODO Auto-generated method stub
        return false;
    }
    
    
    
}
