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
	private String espoolid;
	private String esmanagementip;
	private String esaccountid;
	private String esapikey;
	private String esdefaultgateway;
	private String essubnet;
	private String estntinterface;
	
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
    
    
    protected ElastiCenterClient restClient = null;

    
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
        
        //Add to the details elastistor configurable values
        details.put(ElastistorUtil.ES_MANAGEMENT_IP, esmanagementip);
        details.put(ElastistorUtil.ES_API_KEY, esapikey);
        details.put(ElastistorUtil.ES_ACCOUNT_ID, esaccountid);
        details.put(ElastistorUtil.ES_POOL_ID,espoolid);
        details.put(ElastistorUtil.ES_INTERFACE, estntinterface);
        details.put(ElastistorUtil.ES_SUBNET, essubnet);
        details.put(ElastistorUtil.ES_GATEWAY,esdefaultgateway);
        
        String quotasize; 
        
       if((1099511627776L)>capacityBytes &&(capacityBytes>(1073741824))){
        	
        	quotasize =(String.valueOf(capacityBytes/(1024*1024*1024))+"G");
        }
        else
        {
        	int temp1 = (int) (capacityBytes/(1024*1024*1024));
        	int temp2  = temp1/1024;
        	
            quotasize =(String.valueOf(temp2)+"T");
        	
        }
        
        //String subnet = "8";
        //String router = "10.10.1.1";
        //String tntinterface = "em0";
        String totalthroughput = String.valueOf(capacityIops*4);
        String noofcopies = "1";
        String blocksize = "4K";
        String totaliops = String.valueOf(capacityIops);
        String latency = "15";
        String graceallowed = "false";
        
      //AddQosGroup parameters 
        String memlimit = "0";
        String networkspeed = "0";
        String datasetid ;
        String tsmid;
        
        //createVolume parameters
        String qosgroupid ;
        String deduplication = "off";
        String compression = "off";
        String sync = "always";
        String casesensitivity = "sensitive";
        String readonly = "off";
        String unicode = "off";
        String authnetwork = "all";
        String mapuserstoroot = "yes";
  
        
        
      
        
        
       /* details = new HashMap<String, String>();
        details.put(ElastistorUtil.ES_STORAGE_TYPE, "NetworkFilesystem");
        details.put(ElastistorUtil.ES_STORAGE_TYPE, "NetworkFilesystem");
        details.put(ElastistorUtil.ES_STORAGE_TYPE, "NetworkFilesystem");
        */
        
        //String storageIp = details.get(ElastistorUtil.ES_STORAGE_IP);
        //int storagePort = Integer.parseInt(details.get(ElastistorUtil.ES_STORAGE_PORT));
        
       /* 
        *   new feature added by punith to get storage volumeip and storage port        
        */
        
        
        
        String storageIp = getStorageVip(url);      
    	int storagePort = getDefaultStoragePort(url);
        String storagetype = getstoragetype(url);
        String accesspath = getAccessPath(url);
        String protocoltype = getprotocoltype(url);
        
        // for elstistor purpose 
        String[]  mp = accesspath.split("/");
        String mountpoint = mp[1];

        DataCenterVO zone = _zoneDao.findById(zoneId);

        
        
       // String uuid = ElastistorUtil.ES_PROVIDER_NAME + "_" + zone.getUuid() + "_" + storageIp;
   
        String uuid = ElastistorUtil.ES_PROVIDER_NAME + "_" + zone.getUuid() + "_" + storageIp + "_" + accesspath;     
        
        
       if (capacityBytes == null || capacityBytes <= 0) {
            throw new IllegalArgumentException("'capacityBytes' must be present and greater than 0.");
        }

        if (capacityIops == null || capacityIops <= 0) {
            throw new IllegalArgumentException("'capacityIops' must be present and greater than 0.");
        }
    
        
        
        try {
			restClient = new ElastiCenterClient(esmanagementip, esapikey);
		} catch (InvalidCredentialsException e) {
			e.printStackTrace();
			throw new IllegalArgumentException("ELASTICENTER CONNECTION FAILED",e);
		} catch (InvalidParameterException e) {
			e.printStackTrace();
			throw new IllegalArgumentException("ELASTICENTER CONNECTION FAILED",e);
		} catch (SSLHandshakeException e) {
			e.printStackTrace();
			throw new IllegalArgumentException("ELASTICENTER CONNECTION FAILED",e);
		} catch (ServiceUnavailableException e) {
			e.printStackTrace();
			throw new IllegalArgumentException("ELASTICENTER CONNECTION FAILED",e);
		}
         
        

        
        
        
        try {
        	s_logger.info("creation of elastistor plugin started");
        	
        	CreateTsmCmd cmd = new CreateTsmCmd();
        	
        
			  if ( null != esaccountid ) cmd.putCommandParameter(ElastistorUtil.REST_PARAM_ACCOUNTID, esaccountid);
			  if ( null != totalthroughput ) cmd.putCommandParameter("totalthroughput", totalthroughput);
			  if ( null != espoolid ) cmd.putCommandParameter(ElastistorUtil.REST_PARAM_POOLID, espoolid);
			  if ( null != storagePoolName ) cmd.putCommandParameter("name", "TSM"+storagePoolName);
			  if ( null != quotasize ) cmd.putCommandParameter("quotasize", quotasize);
			  
			  if ( null != storageIp ) cmd.putCommandParameter("ipaddress", storageIp);
			  if ( null != essubnet ) cmd.putCommandParameter(ElastistorUtil.REST_PARAM_SUBNET, essubnet);
			  if ( null != esdefaultgateway ) cmd.putCommandParameter(ElastistorUtil.REST_PARAM_GATEWAY, esdefaultgateway);
			  if ( null != estntinterface ) cmd.putCommandParameter(ElastistorUtil.REST_PARAM_INTERFACE, estntinterface);			  
			  if ( null != noofcopies ) cmd.putCommandParameter("noofcopies", noofcopies);			 
			  if ( null != blocksize ) cmd.putCommandParameter("recordsize", blocksize);	  
			  if ( null != totaliops ) cmd.putCommandParameter("totaliops", totaliops);
			  if ( null != latency ) cmd.putCommandParameter("latency", latency);
			  if ( null != blocksize ) cmd.putCommandParameter("blocksize", blocksize);
			
			  if ( null != graceallowed ) cmd.putCommandParameter("graceallowed", graceallowed);
			
			  
         CreateTsmCmdResponse cmdResponse = (CreateTsmCmdResponse) restClient.executeCommand(cmd);
         
         if ( cmdResponse.getTsm().getUuid() == null  ){
        	 
        	 s_logger.error("*************TSM CREATION FAILED *********************");
        	 throw new CloudRuntimeException("TSM CREATION FAILED , contact elatistor admin");
         }
         /*
         else{
        	 AddQosGroupCmd cmd2 = new AddQosGroupCmd();
        	
        	 tsmid = cmdResponse.getTsm().getUuid();
        	 datasetid = cmdResponse.getTsm().getDatasetid();
        	 
        	 if ( null != storagePoolName ) cmd2.putCommandParameter("name", "QOS_"+storagePoolName);
        	 if ( null != totaliops ) cmd2.putCommandParameter("iops", totaliops);
			  if ( null != latency ) cmd2.putCommandParameter("latency", latency);
			  if ( null != blocksize ) cmd2.putCommandParameter("blocksize", blocksize);
			  if ( null != totalthroughput ) cmd2.putCommandParameter("throughput", totalthroughput);
			  if ( null != memlimit ) cmd2.putCommandParameter("memlimit", memlimit);
			  if ( null != networkspeed ) cmd2.putCommandParameter("networkspeed", networkspeed);
			  if ( null != tsmid ) cmd2.putCommandParameter("tsmid", tsmid);
			  if ( null != datasetid ) cmd2.putCommandParameter("datasetid", datasetid);
			  if ( null != graceallowed ) cmd2.putCommandParameter("graceallowed", graceallowed);
        	 
			  AddQosGroupCmdResponse cmdResponse2 = (AddQosGroupCmdResponse) restClient.executeCommand( cmd2 );
			  
			  if ( cmdResponse2.getQoSGroup().getUuid() == null  ) {
				  
				  s_logger.error("*************ADD QOS GROUP FAILED *********************");
		        	 throw new CloudRuntimeException("ADD QOS GROUP FAILED , contact elatistor admin");
				  
			  }
			  
			  else{
				  
				  CreateVolumeCmd cmd3 = new CreateVolumeCmd();
				  
				  qosgroupid = cmdResponse2.getQoSGroup().getUuid();
				  
				 
				  if ( null != esaccountid ) cmd3.putCommandParameter(ElastistorUtil.REST_PARAM_ACCOUNTID, esaccountid);
				  if ( null != qosgroupid ) cmd3.putCommandParameter("qosgroupid", qosgroupid);
				  if ( null != tsmid ) cmd3.putCommandParameter("tsmid", tsmid);
				  if ( null != espoolid ) cmd3.putCommandParameter(ElastistorUtil.REST_PARAM_POOLID, espoolid);
				  if ( null != storagePoolName ) cmd3.putCommandParameter("name", storagePoolName);
				  if ( null != quotasize ) cmd3.putCommandParameter("quotasize", quotasize);
				  if ( null != blocksize ) cmd3.putCommandParameter("blocksize", blocksize);
				  if ( null != deduplication ) cmd3.putCommandParameter("deduplication", deduplication);
				  if ( null != sync ) cmd3.putCommandParameter("sync", sync);
				  if ( null != compression ) cmd3.putCommandParameter("compression", compression);
				  if ( null != noofcopies ) cmd3.putCommandParameter("noofcopies", noofcopies);
				  cmd3.putCommandParameter("mountpoint", mountpoint);
				  if ( null != casesensitivity ) cmd3.putCommandParameter("casesensitivity", casesensitivity);
				  if ( null != readonly ) cmd3.putCommandParameter("readonly", readonly);
				  if ( null != datasetid ) cmd3.putCommandParameter("datasetid", datasetid);
				  if ( null != unicode ) cmd3.putCommandParameter("unicode", unicode);
				  cmd3.putCommandParameter("protocoltype", protocoltype);
				  if ( null != authnetwork ) cmd3.putCommandParameter("authnetwork", authnetwork);
				  if ( null != mapuserstoroot ) cmd3.putCommandParameter("mapuserstoroot", mapuserstoroot);
				  
				  
				  CreateVolumeCmdResponse cmdResponse3 = (CreateVolumeCmdResponse) restClient.executeCommand( cmd3 );
				  				  
				  
				  if ( cmdResponse3.getFileSystem().getUuid() == null  ){
					  s_logger.error("*************CREATING VOLUME FAILED *********************");
			        	 throw new CloudRuntimeException("CREATING VOLUME FAILED , contact elatistor admin");
					
				  }
				  else {
					  s_logger.info("creation of elastistor volume complete");
				  }
				  
				  
			      if(protocoltype == "iscsi")
					{
			    	  accesspath = "/"+cmdResponse3.getFileSystem().getIqn()+"/0";
			    	  
					}  	 
				 				  
			  }
         }
         */
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
		 
	/*        PrimaryDataStoreInfo primarystore = (PrimaryDataStoreInfo) store;
	        // Check if there is host up in this cluster
	        List<HostVO> allHosts = _resourceMgr.listAllUpAndEnabledHosts(Host.Type.Routing, primarystore.getClusterId(),
	                primarystore.getPodId(), primarystore.getDataCenterId());
	        if (allHosts.isEmpty()) {
	            primaryDataStoreDao.expunge(primarystore.getId());
	            throw new CloudRuntimeException("No host up to associate a storage pool with in cluster "
	                    + primarystore.getClusterId());
	        }
*/
/*
	        boolean success = false;
	        for (HostVO h : allHosts) {
	            success = createStoragePool(h.getId(), primarystore);
	            if (success) {
	                break;
	            }
	        }
*/
	/*        s_logger.debug("In createPool Adding the pool to each of the hosts");
	        List<HostVO> poolHosts = new ArrayList<HostVO>();
	        for (HostVO h : allHosts) {
	            try {
	                storageMgr.connectHostToSharedPool(h.getId(), primarystore.getId());
	               // poolHosts.add(h);
	            } catch (Exception e) {
	                s_logger.warn("Unable to establish a connection between " + h + " and " + primarystore, e);
	            }
	        }
*/
	    /*    if (poolHosts.isEmpty()) {
	            s_logger.warn("No host can access storage pool " + primarystore + " on cluster "
	                    + primarystore.getClusterId());
	            primaryDataStoreDao.expunge(primarystore.getId());
	            throw new CloudRuntimeException("Failed to access storage pool");
	        }*/

	      
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
        	String esip = esmanagementip;
        	String apikey = esapikey;
        	
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
    
    public String getEspoolid() {
		return espoolid;
	}


	public void setEspoolid(String espoolid) {
		this.espoolid = espoolid;
	}


	public String getEsmanagementip() {
		return esmanagementip;
	}


	public void setEsmanagementip(String esmanagementip) {
		this.esmanagementip = esmanagementip;
	}


	public String getEsaccountid() {
		return esaccountid;
	}


	public void setEsaccountid(String esaccountid) {
		this.esaccountid = esaccountid;
	}


	public String getEsapikey() {
		return esapikey;
	}


	public void setEsapikey(String esapikey) {
		this.esapikey = esapikey;
	}
	
	public String getesdefaultgateway() {
		return esdefaultgateway;
	}

	public void setesdefaultgateway(String esdefaultgateway) {
		this.esdefaultgateway = esdefaultgateway;
	}
	public String getEssubnet() {
		return essubnet;
	}


	public void setEssubnet(String essubnet) {
		this.essubnet = essubnet;
	}


	public String getEstntinterface() {
		return estntinterface;
	}


	public void setEstntinterface(String estntinterface) {
		this.estntinterface = estntinterface;
	}

    
}
