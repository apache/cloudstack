/**
 *  Copyright (C) 2010 Cloud.com, Inc.  All rights reserved.
 * 
 * This software is licensed under the GNU General Public License v3 or later.
 * 
 * It is free software: you can redistribute it and/or modify
 * it under the terms of the GNU General Public License as published by
 * the Free Software Foundation, either version 3 of the License, or any later version.
 * This program is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 * GNU General Public License for more details.
 * 
 * You should have received a copy of the GNU General Public License
 * along with this program.  If not, see <http://www.gnu.org/licenses/>.
 * 
 */
package com.cloud.storage;

import java.net.URI;
import java.net.UnknownHostException;
import java.util.ArrayList;
import java.util.Collections;
import java.util.Date;
import java.util.Enumeration;
import java.util.Formatter;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.UUID;
import java.util.concurrent.Executors;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.TimeUnit;

import javax.ejb.Local;
import javax.naming.ConfigurationException;

import org.apache.log4j.Logger;

import com.cloud.agent.AgentManager;
import com.cloud.agent.api.Answer;
import com.cloud.agent.api.BackupSnapshotCommand;
import com.cloud.agent.api.Command;
import com.cloud.agent.api.CreateVolumeFromSnapshotAnswer;
import com.cloud.agent.api.CreateVolumeFromSnapshotCommand;
import com.cloud.agent.api.DeleteStoragePoolCommand;
import com.cloud.agent.api.ManageSnapshotCommand;
import com.cloud.agent.api.ModifyStoragePoolAnswer;
import com.cloud.agent.api.ModifyStoragePoolCommand;
import com.cloud.agent.api.storage.CopyVolumeAnswer;
import com.cloud.agent.api.storage.CopyVolumeCommand;
import com.cloud.agent.api.storage.CreateAnswer;
import com.cloud.agent.api.storage.CreateCommand;
import com.cloud.agent.api.storage.DeleteTemplateCommand;
import com.cloud.agent.api.storage.DestroyCommand;
import com.cloud.agent.api.to.StorageFilerTO;
import com.cloud.agent.api.to.VolumeTO;
import com.cloud.alert.AlertManager;
import com.cloud.api.BaseCmd;
import com.cloud.async.AsyncInstanceCreateStatus;
import com.cloud.async.AsyncJobExecutor;
import com.cloud.async.AsyncJobManager;
import com.cloud.async.AsyncJobVO;
import com.cloud.async.BaseAsyncJobExecutor;
import com.cloud.capacity.CapacityVO;
import com.cloud.capacity.dao.CapacityDao;
import com.cloud.configuration.Config;
import com.cloud.configuration.ConfigurationManager;
import com.cloud.configuration.ResourceCount.ResourceType;
import com.cloud.configuration.dao.ConfigurationDao;
import com.cloud.consoleproxy.ConsoleProxyManager;
import com.cloud.dc.DataCenterVO;
import com.cloud.dc.HostPodVO;
import com.cloud.dc.dao.DataCenterDao;
import com.cloud.dc.dao.HostPodDao;
import com.cloud.deploy.DeployDestination;
import com.cloud.event.EventState;
import com.cloud.event.EventTypes;
import com.cloud.event.EventVO;
import com.cloud.event.dao.EventDao;
import com.cloud.exception.AgentUnavailableException;
import com.cloud.exception.ConcurrentOperationException;
import com.cloud.exception.DiscoveryException;
import com.cloud.exception.InsufficientCapacityException;
import com.cloud.exception.InsufficientStorageCapacityException;
import com.cloud.exception.InternalErrorException;
import com.cloud.exception.InvalidParameterValueException;
import com.cloud.exception.OperationTimedoutException;
import com.cloud.exception.ResourceAllocationException;
import com.cloud.exception.ResourceInUseException;
import com.cloud.exception.StorageUnavailableException;
import com.cloud.host.Host;
import com.cloud.host.Host.Type;
import com.cloud.host.HostVO;
import com.cloud.host.Status;
import com.cloud.host.dao.DetailsDao;
import com.cloud.host.dao.HostDao;
import com.cloud.hypervisor.Hypervisor;
import com.cloud.network.NetworkManager;
import com.cloud.offering.ServiceOffering;
import com.cloud.service.ServiceOfferingVO;
import com.cloud.service.dao.ServiceOfferingDao;
import com.cloud.storage.Storage.ImageFormat;
import com.cloud.storage.Storage.StoragePoolType;
import com.cloud.storage.Volume.Event;
import com.cloud.storage.Volume.MirrorState;
import com.cloud.storage.Volume.SourceType;
import com.cloud.storage.Volume.VolumeType;
import com.cloud.storage.allocator.StoragePoolAllocator;
import com.cloud.storage.dao.DiskOfferingDao;
import com.cloud.storage.dao.SnapshotDao;
import com.cloud.storage.dao.StoragePoolDao;
import com.cloud.storage.dao.StoragePoolHostDao;
import com.cloud.storage.dao.VMTemplateDao;
import com.cloud.storage.dao.VMTemplateHostDao;
import com.cloud.storage.dao.VMTemplatePoolDao;
import com.cloud.storage.dao.VolumeDao;
import com.cloud.storage.listener.StoragePoolMonitor;
import com.cloud.storage.secondary.SecondaryStorageVmManager;
import com.cloud.storage.snapshot.SnapshotManager;
import com.cloud.storage.snapshot.SnapshotScheduler;
import com.cloud.template.TemplateManager;
import com.cloud.user.Account;
import com.cloud.user.AccountManager;
import com.cloud.user.AccountVO;
import com.cloud.user.User;
import com.cloud.user.dao.AccountDao;
import com.cloud.user.dao.UserDao;
import com.cloud.uservm.UserVm;
import com.cloud.utils.NumbersUtil;
import com.cloud.utils.Pair;
import com.cloud.utils.component.Adapters;
import com.cloud.utils.component.ComponentLocator;
import com.cloud.utils.component.Inject;
import com.cloud.utils.concurrency.NamedThreadFactory;
import com.cloud.utils.db.DB;
import com.cloud.utils.db.GlobalLock;
import com.cloud.utils.db.JoinBuilder;
import com.cloud.utils.db.SearchBuilder;
import com.cloud.utils.db.SearchCriteria;
import com.cloud.utils.db.Transaction;
import com.cloud.utils.exception.CloudRuntimeException;
import com.cloud.utils.exception.ExecutionException;
import com.cloud.vm.DiskProfile;
import com.cloud.vm.State;
import com.cloud.vm.UserVmManager;
import com.cloud.vm.UserVmVO;
import com.cloud.vm.VMInstanceVO;
import com.cloud.vm.VirtualMachine;
import com.cloud.vm.VirtualMachineProfile;
import com.cloud.vm.dao.ConsoleProxyDao;
import com.cloud.vm.dao.UserVmDao;
import com.cloud.vm.dao.VMInstanceDao;

@Local(value = { StorageManager.class })
public class StorageManagerImpl implements StorageManager {
    private static final Logger s_logger = Logger.getLogger(StorageManagerImpl.class);

    protected String _name;
    @Inject protected UserVmManager _userVmMgr;
    @Inject protected AgentManager _agentMgr;
    @Inject protected TemplateManager _tmpltMgr;
    @Inject protected AsyncJobManager _asyncMgr;
    @Inject protected SnapshotManager _snapshotMgr;
    @Inject protected SnapshotScheduler _snapshotScheduler;
    @Inject protected AccountManager _accountMgr;
    @Inject protected ConfigurationManager _configMgr;
    @Inject protected ConsoleProxyManager _consoleProxyMgr;
    @Inject protected SecondaryStorageVmManager _secStorageMgr;
    @Inject protected NetworkManager _networkMgr;
    @Inject protected VolumeDao _volsDao;
    @Inject protected HostDao _hostDao;
    @Inject protected ConsoleProxyDao _consoleProxyDao;
    @Inject protected DetailsDao _detailsDao;
    @Inject protected SnapshotDao _snapshotDao;
    @Inject protected StoragePoolHostDao _storagePoolHostDao;
    @Inject protected AlertManager _alertMgr;
    @Inject protected VMTemplateHostDao _vmTemplateHostDao = null;
    @Inject protected VMTemplatePoolDao _vmTemplatePoolDao = null;
    @Inject protected VMTemplateDao _vmTemplateDao = null;
    @Inject protected StoragePoolHostDao _poolHostDao = null;
    @Inject protected UserVmDao _userVmDao;
    @Inject protected VMInstanceDao _vmInstanceDao;
    @Inject protected StoragePoolDao _storagePoolDao = null;
    @Inject protected CapacityDao _capacityDao;
    @Inject protected DiskOfferingDao _diskOfferingDao;
    @Inject protected AccountDao _accountDao;
    @Inject protected EventDao _eventDao = null;
    @Inject protected DataCenterDao _dcDao = null;
    @Inject protected HostPodDao _podDao = null;
    @Inject protected VMTemplateDao _templateDao;
    @Inject protected VMTemplateHostDao _templateHostDao;
    @Inject protected ServiceOfferingDao _offeringDao;
    @Inject protected UserDao _userDao;
    
    @Inject(adapter=StoragePoolAllocator.class)
    protected Adapters<StoragePoolAllocator> _storagePoolAllocators;
    @Inject(adapter=StoragePoolDiscoverer.class)
    protected Adapters<StoragePoolDiscoverer> _discoverers;
    
    protected SearchBuilder<VMTemplateHostVO> HostTemplateStatesSearch;
    protected SearchBuilder<StoragePoolVO> PoolsUsedByVmSearch;

    ScheduledExecutorService _executor = null;
    boolean _storageCleanupEnabled;
    int _storageCleanupInterval;
    int _storagePoolAcquisitionWaitSeconds = 1800; // 30 minutes
    protected int _retry = 2;
    protected int _pingInterval = 60; // seconds
    protected int _hostRetry;
    protected int _overProvisioningFactor = 1;

    private int _totalRetries;
    private int _pauseInterval;
    private final boolean _shouldBeSnapshotCapable = true;
    private Hypervisor.Type _hypervisorType;
    
    @Override
    public boolean share(VMInstanceVO vm, List<VolumeVO> vols, HostVO host, boolean cancelPreviousShare) {
    	
    	//this check is done for maintenance mode for primary storage
    	//if any one of the volume is unusable, we return false
    	//if we return false, the allocator will try to switch to another PS if available
    	for(VolumeVO vol: vols)
    	{
    		if(vol.getRemoved()!=null)
    		{
    			s_logger.warn("Volume id:"+vol.getId()+" is removed, cannot share on this instance");
        		//not ok to share
        		return false;
    		}
    	}
    	
    	//ok to share
        return true;
    }
    
    @DB
    public List<VolumeVO> allocate(DiskProfile rootDisk, List<DiskProfile> dataDisks, VMInstanceVO vm, DataCenterVO dc, AccountVO account) {
        ArrayList<VolumeVO> vols = new ArrayList<VolumeVO>(dataDisks.size() + 1);
        VolumeVO dataVol = null;
        VolumeVO rootVol = null;
        long deviceId = 0;
        Transaction txn = Transaction.currentTxn();
        txn.start();
        rootVol = new VolumeVO(VolumeType.ROOT, rootDisk.getName(), dc.getId(), account.getDomainId(), account.getId(), rootDisk.getDiskOfferingId(), rootDisk.getSize());
        if (rootDisk.getTemplateId() != null) {
            rootVol.setTemplateId(rootDisk.getTemplateId());
        }
        rootVol.setInstanceId(vm.getId());
        rootVol.setDeviceId(deviceId++);
        rootVol = _volsDao.persist(rootVol);
        vols.add(rootVol);
        for (DiskProfile dataDisk : dataDisks) {
            dataVol = new VolumeVO(VolumeType.DATADISK, dataDisk.getName(), dc.getId(), account.getDomainId(), account.getId(), dataDisk.getDiskOfferingId(), dataDisk.getSize());
            dataVol.setDeviceId(deviceId++);
            dataVol.setInstanceId(vm.getId());
            dataVol = _volsDao.persist(dataVol);
            vols.add(dataVol);
        }
        txn.commit();
        
        return vols;
    }
    
    @Override
    public VolumeVO allocateIsoInstalledVm(VMInstanceVO vm, VMTemplateVO template, DiskOfferingVO rootOffering, Long size, DataCenterVO dc, AccountVO account) {
        assert (template.getFormat() == ImageFormat.ISO) : "The template has to be ISO";
        
        long rootId = _volsDao.getNextInSequence(Long.class, "volume_seq");
        DiskProfile rootDisk = new DiskProfile(rootId, VolumeType.ROOT, "ROOT-" + vm.getId() + "-" + rootId, rootOffering.getId(), size != null ? size : rootOffering.getDiskSizeInBytes(), rootOffering.getTagsArray(), rootOffering.getUseLocalStorage(), rootOffering.isRecreatable(), null);
        List<VolumeVO> vols = allocate(rootDisk, null, vm, dc, account);
        return vols.get(0);
    }
    
    VolumeVO allocateDuplicateVolume(VolumeVO oldVol) {
        VolumeVO newVol = new VolumeVO(oldVol.getVolumeType(), oldVol.getName(), oldVol.getDataCenterId(), oldVol.getDomainId(), oldVol.getAccountId(), oldVol.getDiskOfferingId(), oldVol.getSize());
        newVol.setTemplateId(oldVol.getTemplateId());
        newVol.setDeviceId(oldVol.getDeviceId());
        newVol.setInstanceId(oldVol.getInstanceId());
        return _volsDao.persist(newVol);
    }
    
    
    @Override
    public List<VolumeVO> prepare(VMInstanceVO vm, HostVO host) {
        List<VolumeVO> vols = _volsDao.findCreatedByInstance(vm.getId());
        List<VolumeVO> recreateVols = new ArrayList<VolumeVO>(vols.size());
        for (VolumeVO vol : vols) {
            if (!vol.isRecreatable()) {
                return vols;
            }
            
            //if we have a system vm
            //get the storage pool
            //if pool is in maintenance
            //add to recreate vols, and continue
            if(vm.getType().equals(VirtualMachine.Type.ConsoleProxy) || vm.getType().equals(VirtualMachine.Type.DomainRouter) || vm.getType().equals(VirtualMachine.Type.SecondaryStorageVm))
            {
            	StoragePoolVO sp = _storagePoolDao.findById(vol.getPoolId());
            	
            	if(sp!=null && sp.getStatus().equals(Status.PrepareForMaintenance))
            	{
            		recreateVols.add(vol);
            		continue;
            	}
            }
            
            StoragePoolHostVO ph = _storagePoolHostDao.findByPoolHost(vol.getPoolId(), host.getId());
            if (ph == null) {
                if (s_logger.isDebugEnabled()) {
                    s_logger.debug("Must recreate " + vol + " since " + vol.getPoolId() + " has is not hooked up with host " + host.getId());
                }
                recreateVols.add(vol);
            }
        }
        
        if (recreateVols.size() == 0) {
            s_logger.debug("No need to recreate the volumes");
            return vols;
        }
        
        List<VolumeVO> createds = new ArrayList<VolumeVO>(recreateVols.size());
        for (VolumeVO vol : recreateVols) {
            VolumeVO create = new VolumeVO(vol.getVolumeType(), vol.getInstanceId(), vol.getTemplateId(), vol.getName(), vol.getDataCenterId(), host.getPodId(), vol.getAccountId(), vol.getDomainId(), vol.isRecreatable());
            create.setDiskOfferingId(vol.getDiskOfferingId());
            create.setDeviceId(vol.getDeviceId());
            create = _volsDao.persist(create);
            VMTemplateVO template = _templateDao.findById(create.getTemplateId());
            DataCenterVO dc = _dcDao.findById(create.getDataCenterId());
            HostPodVO pod = _podDao.findById(host.getPodId());
            DiskOfferingVO diskOffering = null;
            diskOffering = _diskOfferingDao.findById(vol.getDiskOfferingId());
            ServiceOfferingVO offering;
            if (vm instanceof UserVmVO) {
                offering = _offeringDao.findById(((UserVmVO)vm).getServiceOfferingId());
            } else {
                offering = _offeringDao.findById(vol.getDiskOfferingId());
            }
            VolumeVO created = createVolume(create, vm, template, dc, pod, host.getClusterId(), offering, diskOffering, new ArrayList<StoragePoolVO>(),0);
            if (created == null) {
                break;
            }
            createds.add(created);
        }

        for (VolumeVO vol : recreateVols) {
            _volsDao.remove(vol.getId());
        }
        
        return createds;
    }

    @Override
    public List<Pair<VolumeVO, StoragePoolVO>> isStoredOn(VMInstanceVO vm) {
        List<Pair<VolumeVO, StoragePoolVO>> lst = new ArrayList<Pair<VolumeVO, StoragePoolVO>>();

        List<VolumeVO> vols = _volsDao.findByInstance(vm.getId());
        for (VolumeVO vol : vols) {
            StoragePoolVO pool = _storagePoolDao.findById(vol.getPoolId());
            lst.add(new Pair<VolumeVO, StoragePoolVO>(vol, pool));
        }

        return lst;
    }
    
    @Override
    public boolean isLocalStorageActiveOnHost(HostVO host) {
    	List<StoragePoolHostVO> storagePoolHostRefs = _storagePoolHostDao.listByHostId(host.getId());
    	for (StoragePoolHostVO storagePoolHostRef : storagePoolHostRefs) {
    		StoragePoolVO storagePool = _storagePoolDao.findById(storagePoolHostRef.getPoolId());
    		if (storagePool.getPoolType() == StoragePoolType.LVM) {
    			SearchBuilder<VolumeVO> volumeSB = _volsDao.createSearchBuilder();
    			volumeSB.and("poolId", volumeSB.entity().getPoolId(), SearchCriteria.Op.EQ);
    			volumeSB.and("removed", volumeSB.entity().getRemoved(), SearchCriteria.Op.NULL);
    			
    			SearchBuilder<VMInstanceVO> activeVmSB = _vmInstanceDao.createSearchBuilder();
	        	activeVmSB.and("state", activeVmSB.entity().getState(), SearchCriteria.Op.IN);
	        	volumeSB.join("activeVmSB", activeVmSB, volumeSB.entity().getInstanceId(), activeVmSB.entity().getId(), JoinBuilder.JoinType.INNER);
    			
	        	SearchCriteria<VolumeVO> volumeSC = volumeSB.create();
	        	volumeSC.setParameters("poolId", storagePool.getId());
	        	volumeSC.setJoinParameters("activeVmSB", "state", new Object[] {State.Creating, State.Starting, State.Running, State.Stopping, State.Migrating});
    			
    			List<VolumeVO> volumes = _volsDao.search(volumeSC, null);
    			if (volumes.size() > 0) {
    				return true;
    			}
    		}
    	}
    	
    	return false;
    }

    @Override
    public List<VolumeVO> unshare(VMInstanceVO vm, HostVO host) {
        final List<VolumeVO> vols = _volsDao.findCreatedByInstance(vm.getId());
        if (vols.size() == 0) {
            return vols;
        }

        return unshare(vm, vols, host) ? vols : null;
    }

    protected StoragePoolVO findStoragePool(DiskProfile dskCh, final DataCenterVO dc, HostPodVO pod, Long clusterId, final ServiceOffering offering, final VMInstanceVO vm, final VMTemplateVO template, final Set<StoragePool> avoid) {
        Enumeration<StoragePoolAllocator> en = _storagePoolAllocators.enumeration();
        while (en.hasMoreElements()) {
            final StoragePoolAllocator allocator = en.nextElement();
            final StoragePool pool = allocator.allocateToPool(dskCh, dc, pod, clusterId, vm, template, avoid);
            if (pool != null) {
                return (StoragePoolVO) pool;
            }
        }
        return null;
    }

    @Override
    public Long findHostIdForStoragePool(StoragePool pool) {
        List<StoragePoolHostVO> poolHosts = _poolHostDao.listByHostStatus(pool.getId(), Status.Up);

        if (poolHosts.size() == 0) {
            return null;
        } else {
            return poolHosts.get(0).getHostId();
        }
    }

    @Override
    public Answer[] sendToPool(StoragePool pool, Command[] cmds, boolean stopOnError) {
        List<StoragePoolHostVO> poolHosts = _poolHostDao.listByHostStatus(pool.getId(), Status.Up);
        Collections.shuffle(poolHosts);
        
        for (StoragePoolHostVO poolHost: poolHosts) {
            try {
                Answer[] answerRet = _agentMgr.send(poolHost.getHostId(), cmds, stopOnError);
                return answerRet;
                
            } catch (AgentUnavailableException e) {
                s_logger.debug("Moving on because unable to send to " + poolHost.getHostId() + " due to " + e.getMessage());
            } catch (OperationTimedoutException e) {
                s_logger.debug("Moving on because unable to send to " + poolHost.getHostId() + " due to " + e.getMessage());
            }
        }
        if( !poolHosts.isEmpty() ) {
            s_logger.warn("Unable to send commands to the pool because we ran out of hosts to send to");
        }
        return null;
    }
    
    @Override
    public Answer sendToPool(StoragePool pool, Command cmd) {
        Command[] cmds = new Command[]{cmd};
        Answer[] answers = sendToPool(pool, cmds, true);
        if (answers == null) {
            return null;
        }
        return answers[0];
    }
    
    protected DiskProfile createDiskCharacteristics(VolumeVO volume, VMTemplateVO template, DataCenterVO dc, DiskOfferingVO diskOffering) {
        if (volume.getVolumeType() == VolumeType.ROOT && Storage.ImageFormat.ISO != template.getFormat()) {
            SearchCriteria<VMTemplateHostVO> sc = HostTemplateStatesSearch.create();
            sc.setParameters("id", template.getId());
            sc.setParameters("state", com.cloud.storage.VMTemplateStorageResourceAssoc.Status.DOWNLOADED);
            sc.setJoinParameters("host", "dcId", dc.getId());
        
            List<VMTemplateHostVO> sss = _vmTemplateHostDao.search(sc, null);
            if (sss.size() == 0) {
                throw new CloudRuntimeException("Template " + template.getName() + " has not been completely downloaded to zone " + dc.getId());
            }
            VMTemplateHostVO ss = sss.get(0);
        
            return new DiskProfile(volume.getId(), volume.getVolumeType(), volume.getName(), diskOffering.getId(), ss.getSize(), diskOffering.getTagsArray(), diskOffering.getUseLocalStorage(), diskOffering.isRecreatable(), Storage.ImageFormat.ISO != template.getFormat() ? template.getId() : null);
        } else {
            return new DiskProfile(volume.getId(), volume.getVolumeType(), volume.getName(), diskOffering.getId(), diskOffering.getDiskSizeInBytes(), diskOffering.getTagsArray(), diskOffering.getUseLocalStorage(), diskOffering.isRecreatable(), null);
        }
    }
    
    @Override
    public boolean canVmRestartOnAnotherServer(long vmId) {
        List<VolumeVO> vols = _volsDao.findCreatedByInstance(vmId);
        for (VolumeVO vol : vols) {
            if (!vol.isRecreatable() && !vol.getPoolType().isShared()) {
                return false;
            }
        }
        return true;
    }
    
    @DB
    protected Pair<VolumeVO, String> createVolumeFromSnapshot(long userId, long accountId, String userSpecifiedName, DataCenterVO dc, SnapshotVO snapshot, long virtualsize) {
        
        VolumeVO createdVolume = null;
        Long volumeId = null;
        
        String volumeFolder = null;
        
        // Create the Volume object and save it so that we can return it to the user
        Account account = _accountDao.findById(accountId);
        VolumeVO volume = new VolumeVO(userSpecifiedName, -1, -1, -1, -1, new Long(-1), null, null, 0, Volume.VolumeType.DATADISK);
        volume.setPoolId(null);
        volume.setDataCenterId(dc.getId());
        volume.setPodId(null);
        volume.setAccountId(accountId);
        volume.setDomainId(account.getDomainId());
        volume.setMirrorState(MirrorState.NOT_MIRRORED);
        volume.setSize(virtualsize);
        volume.setStorageResourceType(Storage.StorageResourceType.STORAGE_POOL);
        volume.setInstanceId(null);
        volume.setUpdated(new Date());
        volume.setStatus(AsyncInstanceCreateStatus.Creating);
        volume.setSourceType(SourceType.Snapshot);
        volume.setSourceId(snapshot.getId());
        volume = _volsDao.persist(volume);
        volumeId = volume.getId();
        
        AsyncJobExecutor asyncExecutor = BaseAsyncJobExecutor.getCurrentExecutor();
        if(asyncExecutor != null) {
            AsyncJobVO job = asyncExecutor.getJob();

            if(s_logger.isInfoEnabled())
                s_logger.info("CreateVolume created a new instance " + volumeId + ", update async job-" + job.getId() + " progress status");
            
            _asyncMgr.updateAsyncJobAttachment(job.getId(), "volume", volumeId);
            _asyncMgr.updateAsyncJobStatus(job.getId(), BaseCmd.PROGRESS_INSTANCE_CREATED, volumeId);
        }
        
        final HashSet<StoragePool> poolsToAvoid = new HashSet<StoragePool>();
        StoragePoolVO pool = null;
        boolean success = false;
        Set<Long> podsToAvoid = new HashSet<Long>();
        Pair<HostPodVO, Long> pod = null;
        String volumeUUID = null;
        String details = null;
        
        DiskProfile dskCh = new DiskProfile(volume.getId(), volume.getVolumeType(), volume.getName(), 0, virtualsize, null, false, false, null);
        
        int retry = 0;
        // Determine what pod to store the volume in
        while ((pod = _agentMgr.findPod(null, null, dc, account.getId(), podsToAvoid)) != null) {
            podsToAvoid.add(pod.first().getId());
            // Determine what storage pool to store the volume in
            while ((pool = findStoragePool(dskCh, dc, pod.first(), null, null, null, null, poolsToAvoid)) != null) {
                poolsToAvoid.add(pool);
                volumeFolder = pool.getPath();
                if (s_logger.isDebugEnabled()) {
                    s_logger.debug("Attempting to create volume from snapshotId: " + snapshot.getId() + " on storage pool " + pool.getName());
                }
                
                // Get the newly created VDI from the snapshot.
                // This will return a null volumePath if it could not be created
                Pair<String, String> volumeDetails = createVDIFromSnapshot(userId, snapshot, pool);
                volumeUUID = volumeDetails.first();
                details = volumeDetails.second();
                
                if (volumeUUID != null) {
                    if (s_logger.isDebugEnabled()) {
                        s_logger.debug("Volume with UUID " + volumeUUID + " was created on storage pool " + pool.getName());
                    }
                    success = true;
                    break; // break out of the "find storage pool" loop
                } else {
                	retry++;
                	if( retry >= 3 ) {
                		_volsDao.expunge(volumeId);
                		String msg = "Unable to create volume from snapshot " + snapshot.getId() + " after retrying 3 times, due to " + details;
                		s_logger.debug(msg);
                        throw new CloudRuntimeException(msg);
                		
                	}
                }

                s_logger.warn("Unable to create volume on pool " + pool.getName() + ", reason: " + details);
                
            }
            
            if (success) {
                break; // break out of the "find pod" loop
            } 
        }
        
        if( !success ) {       	
    		_volsDao.expunge(volumeId);
    		String msg = "Unable to create volume from snapshot " + snapshot.getId() + " due to " + details;
    		s_logger.debug(msg);
            throw new CloudRuntimeException(msg);
    		
        }
        // Update the volume in the database
        Transaction txn = Transaction.currentTxn();
        txn.start();
        createdVolume = _volsDao.findById(volumeId);
        
        if (success) {
            // Increment the number of volumes
            _accountMgr.incrementResourceCount(accountId, ResourceType.volume);
            
            createdVolume.setStatus(AsyncInstanceCreateStatus.Created);
            createdVolume.setPodId(pod.first().getId());
            createdVolume.setPoolId(pool.getId());
            createdVolume.setPoolType(pool.getPoolType());
            createdVolume.setFolder(volumeFolder);
            createdVolume.setPath(volumeUUID);
            createdVolume.setDomainId(account.getDomainId());
        } else {
            createdVolume.setStatus(AsyncInstanceCreateStatus.Corrupted);
            createdVolume.setDestroyed(true);
        }
        
        _volsDao.update(volumeId, createdVolume);
        txn.commit();
        return new Pair<VolumeVO, String>(createdVolume, details);
    }

    @Override
    @DB
    public VolumeVO createVolumeFromSnapshot(long userId, long accountId, long snapshotId, String volumeName, long startEventId) {
        
        EventVO event = new EventVO();
        event.setUserId(userId);
        event.setAccountId(accountId);
        event.setType(EventTypes.EVENT_VOLUME_CREATE);
        event.setState(EventState.Started);
        event.setStartId(startEventId);
        event.setDescription("Creating volume from snapshot with id: "+snapshotId);
        _eventDao.persist(event);
        
        // By default, assume failure.
        VolumeVO createdVolume = null;
        String details = null;
        Long volumeId = null;
        SnapshotVO snapshot = _snapshotDao.findById(snapshotId); // Precondition: snapshot is not null and not removed.
        Long origVolumeId = snapshot.getVolumeId();
        VolumeVO originalVolume = _volsDao.findById(origVolumeId); // NOTE: Original volume could be destroyed and removed.

        // everything went well till now
        DataCenterVO dc = _dcDao.findById(originalVolume.getDataCenterId());

        Pair<VolumeVO, String> volumeDetails = createVolumeFromSnapshot(userId, accountId, volumeName, dc,
                snapshot, originalVolume.getSize());
        createdVolume = volumeDetails.first();
        if (createdVolume != null) {
            volumeId = createdVolume.getId();
        }
        details = volumeDetails.second();

        
        Transaction txn = Transaction.currentTxn();
        txn.start();
        // Create an event
        long templateId = -1;
        long diskOfferingId = -1;
        if(originalVolume.getTemplateId() != null){
            templateId = originalVolume.getTemplateId();
        }
        diskOfferingId = originalVolume.getDiskOfferingId();
        long sizeMB = createdVolume.getSize()/(1024*1024);

        String poolName = _storagePoolDao.findById(createdVolume.getPoolId()).getName();
        String eventParams = "id=" + volumeId +"\ndoId="+diskOfferingId+"\ntId="+templateId+"\ndcId="+originalVolume.getDataCenterId()+"\nsize="+sizeMB;
        event = new EventVO();
        event.setAccountId(accountId);
        event.setUserId(userId);
        event.setType(EventTypes.EVENT_VOLUME_CREATE);
        event.setParameters(eventParams);
        event.setStartId(startEventId);
        event.setState(EventState.Completed);
        if (createdVolume.getPath() != null) {
            event.setDescription("Created volume: "+ createdVolume.getName() + " with size: " + sizeMB + " MB in pool: " + poolName + " from snapshot id: " + snapshotId);
            event.setLevel(EventVO.LEVEL_INFO);
        }
        else {
            details = "CreateVolume From Snapshot for snapshotId: " + snapshotId + " failed at the backend, reason " + details;
            event.setDescription(details);
            event.setLevel(EventVO.LEVEL_ERROR);
        }
        _eventDao.persist(event);
        txn.commit();
        return createdVolume;
    }
    
    protected Pair<String, String> createVDIFromSnapshot(long userId, SnapshotVO snapshot, StoragePoolVO pool) {
        String vdiUUID = null;
        
        Long volumeId = snapshot.getVolumeId();
        VolumeVO volume = _volsDao.findById(volumeId);
        String primaryStoragePoolNameLabel = pool.getUuid(); // pool's uuid is actually  the namelabel.
        String secondaryStoragePoolUrl = getSecondaryStorageURL(volume.getDataCenterId());
        Long dcId = volume.getDataCenterId();
        long accountId = volume.getAccountId();
        
        String backedUpSnapshotUuid = snapshot.getBackupSnapshotId();
        
        CreateVolumeFromSnapshotCommand createVolumeFromSnapshotCommand =
            new CreateVolumeFromSnapshotCommand(primaryStoragePoolNameLabel,
                                                secondaryStoragePoolUrl,
                                                dcId,
                                                accountId,
                                                volumeId,
                                                backedUpSnapshotUuid,
                                                snapshot.getName());
        
        String basicErrMsg = "Failed to create volume from " + snapshot.getName() + " for volume: " + volume.getId();
        CreateVolumeFromSnapshotAnswer answer = (CreateVolumeFromSnapshotAnswer) sendToHostsOnStoragePool(pool.getId(),
                                                                                                                      createVolumeFromSnapshotCommand,
                                                                                                                      basicErrMsg,
                                                                                                                      _totalRetries,
                                                                                                                      _pauseInterval,
                                                                                                                      _shouldBeSnapshotCapable, null);
        if (answer != null && answer.getResult()) {
            vdiUUID = answer.getVdi();
        }
        else if (answer != null) {
            s_logger.error(basicErrMsg);
        }
        
        return new Pair<String, String>(vdiUUID, basicErrMsg);
    }
    
    @DB 
    protected VolumeVO createVolume(VolumeVO volume, VMInstanceVO vm, VMTemplateVO template, DataCenterVO dc, HostPodVO pod, Long clusterId,
                                    ServiceOfferingVO offering, DiskOfferingVO diskOffering, List<StoragePoolVO> avoids, long size) {
        StoragePoolVO pool = null;
        final HashSet<StoragePool> avoidPools = new HashSet<StoragePool>(avoids);
       
        DiskProfile dskCh = null;
        if (volume.getVolumeType() == VolumeType.ROOT && Storage.ImageFormat.ISO != template.getFormat()) {
            dskCh = createDiskCharacteristics(volume, template, dc, offering);
        } else {
            dskCh = createDiskCharacteristics(volume, template, dc, diskOffering);
        }
        
        Transaction txn = Transaction.currentTxn();
        
        VolumeType volType = volume.getVolumeType();
        
        VolumeTO created = null;
        int retry = _retry;
        while (--retry >= 0) {
            created = null;
            
            txn.start();
            
            long podId = pod.getId();
            pod = _podDao.lock(podId, true);
            if (pod == null) {
                txn.rollback();
                volume.setStatus(AsyncInstanceCreateStatus.Failed);
                volume.setDestroyed(true);
                _volsDao.persist(volume);
                throw new CloudRuntimeException("Unable to acquire lock on the pod " + podId);
            }
            
            pool = findStoragePool(dskCh, dc, pod, clusterId, offering, vm, template, avoidPools);
            if (pool == null) {
                txn.rollback();
                break;
            }
            
            avoidPools.add(pool);
            if (s_logger.isDebugEnabled()) {
                s_logger.debug("Trying to create " + volume + " on " + pool);
            }

            volume.setPoolId(pool.getId());
            _volsDao.persist(volume);
            
            txn.commit();
            
            CreateCommand cmd = null;
            VMTemplateStoragePoolVO tmpltStoredOn = null;
            if (volume.getVolumeType() == VolumeType.ROOT && Storage.ImageFormat.ISO != template.getFormat()) {
                tmpltStoredOn = _tmpltMgr.prepareTemplateForCreate(template, pool);
                if (tmpltStoredOn == null) {
                    continue;
                }
                cmd = new CreateCommand(dskCh, tmpltStoredOn.getLocalDownloadPath(), new StorageFilerTO(pool));
            } else {
                cmd = new CreateCommand(dskCh, new StorageFilerTO(pool), size);
            }
            
            Answer answer = sendToPool(pool, cmd);
            if (answer != null && answer.getResult()) {
                created = ((CreateAnswer)answer).getVolume();
                break;
            }
            
            volume.setPoolId(null);
            _volsDao.persist(volume);
            
            s_logger.debug("Retrying the create because it failed on pool " + pool);
        }

        if (created == null) {
            if (s_logger.isDebugEnabled()) {
                s_logger.debug("Unable to create a volume for " + volume);
            }
            volume.setStatus(AsyncInstanceCreateStatus.Failed);
            volume.setDestroyed(true);
            _volsDao.persist(volume);

            return null;
        }

        volume.setStatus(AsyncInstanceCreateStatus.Created);
        volume.setFolder(pool.getPath());
        volume.setPath(created.getPath());
        volume.setSize(created.getSize());
        volume.setPoolType(pool.getPoolType());
        volume.setPodId(pod.getId());
        _volsDao.persist(volume);
        return volume;
    }
    
    @Override
    public List<VolumeVO> create(Account account, VMInstanceVO vm, VMTemplateVO template, DataCenterVO dc, HostPodVO pod, ServiceOfferingVO offering, DiskOfferingVO diskOffering, long size) throws StorageUnavailableException, ExecutionException {
        List<StoragePoolVO> avoids = new ArrayList<StoragePoolVO>();
        return create(account, vm, template, dc, pod, offering, diskOffering, avoids, size);
    }
    
    @DB
    protected List<VolumeVO> create(Account account, VMInstanceVO vm, VMTemplateVO template, DataCenterVO dc, HostPodVO pod,
            ServiceOfferingVO offering, DiskOfferingVO diskOffering, List<StoragePoolVO> avoids, long size) {
        ArrayList<VolumeVO> vols = new ArrayList<VolumeVO>(2);
        VolumeVO dataVol = null;
        VolumeVO rootVol = null;
        Transaction txn = Transaction.currentTxn();
        txn.start();
        if (Storage.ImageFormat.ISO == template.getFormat()) {
            rootVol = new VolumeVO(VolumeType.ROOT, vm.getId(), vm.getInstanceName() + "-ROOT", dc.getId(), pod.getId(), account.getId(), account.getDomainId(),(size>0)? size : diskOffering.getDiskSizeInBytes());

        	createStartedEvent(account, rootVol);
        	
            rootVol.setDiskOfferingId(diskOffering.getId());
            rootVol.setSourceType(SourceType.Template);
            rootVol.setSourceId(template.getId());
            rootVol.setDeviceId(0l);
            rootVol = _volsDao.persist(rootVol);
        } else {
            rootVol = new VolumeVO(VolumeType.ROOT, vm.getId(), template.getId(), vm.getInstanceName() + "-ROOT", dc.getId(), pod.getId(), account.getId(), account.getDomainId(), offering.isRecreatable());
         
        	createStartedEvent(account, rootVol);
            
            rootVol.setDiskOfferingId(offering.getId());
            rootVol.setTemplateId(template.getId());
            rootVol.setSourceId(template.getId());
            rootVol.setSourceType(SourceType.Template);
            rootVol.setDeviceId(0l);
            rootVol = _volsDao.persist(rootVol);
            
            if (diskOffering != null && diskOffering.getDiskSizeInBytes() > 0) {
                dataVol = new VolumeVO(VolumeType.DATADISK, vm.getId(), vm.getInstanceName() + "-DATA", dc.getId(), pod.getId(), account.getId(), account.getDomainId(), (size>0)? size : diskOffering.getDiskSizeInBytes());
                
                createStartedEvent(account, dataVol);
                
                dataVol.setDiskOfferingId(diskOffering.getId());
                dataVol.setSourceType(SourceType.DiskOffering);
                dataVol.setSourceId(diskOffering.getId());
                dataVol.setDeviceId(1l);
                dataVol = _volsDao.persist(dataVol);
            }
        }
        txn.commit();

        VolumeVO dataCreated = null;
        VolumeVO rootCreated = null;
        try {
            rootCreated = createVolume(rootVol, vm, template, dc, pod, null, offering, diskOffering, avoids,size);
            if (rootCreated == null) {
                throw new CloudRuntimeException("Unable to create " + rootVol);
            }
            
            vols.add(rootCreated);
            
            if (dataVol != null) {
                StoragePoolVO pool = _storagePoolDao.findById(rootCreated.getPoolId());
                dataCreated = createVolume(dataVol, vm, null, dc, pod, pool.getClusterId(), offering, diskOffering, avoids,size);
                if (dataCreated == null) {
                    throw new CloudRuntimeException("Unable to create " + dataVol);
                }
                vols.add(dataCreated);
            }
            
            return vols;
        } catch (Exception e) {
            if (s_logger.isDebugEnabled()) {
                s_logger.debug(e.getMessage());
            }
            if (rootCreated != null) {
                destroyVolume(rootCreated);
            }
            throw new CloudRuntimeException("Unable to create volumes for " + vm, e);
        }
    }

	private void createStartedEvent(Account account, VolumeVO rootVol) {
		EventVO event1 = new EventVO();
		event1.setAccountId(account.getId());
		event1.setUserId(1L);
		event1.setType(EventTypes.EVENT_VOLUME_CREATE);
		event1.setState(EventState.Started);
		event1.setDescription("Create volume: " + rootVol.getName()+ "started");
		_eventDao.persist(event1);
	}
    

    @Override
    public long createUserVM(Account account, VMInstanceVO vm, VMTemplateVO template, DataCenterVO dc, HostPodVO pod, ServiceOfferingVO offering, DiskOfferingVO diskOffering,
            List<StoragePoolVO> avoids, long size) {
        List<VolumeVO> volumes = create(account, vm, template, dc, pod, offering, diskOffering, avoids, size);
        if( volumes == null || volumes.size() == 0) {
            throw new CloudRuntimeException("Unable to create volume for " + vm.getName());
        }
        
        for (VolumeVO v : volumes) {
        	
        	//when the user vm is created, the volume is attached upon creation
        	//set the attached datetime
        	try{
        		v.setAttached(new Date());
        		_volsDao.update(v.getId(), v);
        	}catch(Exception e)
        	{
        		s_logger.warn("Error updating the attached value for volume "+v.getId()+":"+e);
        	}
        	
            long templateId = -1;
            long doId = v.getDiskOfferingId();
        	if(v.getVolumeType() == VolumeType.ROOT && Storage.ImageFormat.ISO != template.getFormat()){
        	        templateId = template.getId();
        	        doId = -1;
        	}
        	
        	long volumeId = v.getId();
        	// Create an event
            long sizeMB = v.getSize() / (1024 * 1024);
        	String eventParams = "id=" + volumeId + "\ndoId=" + doId + "\ntId=" + templateId + "\ndcId=" + dc.getId() + "\nsize=" + sizeMB;
        	EventVO event = new EventVO();
        	event.setAccountId(account.getId());
        	event.setUserId(1L);
        	event.setType(EventTypes.EVENT_VOLUME_CREATE);
        	event.setParameters(eventParams);
        	event.setDescription("Created volume: " + v.getName() + " with size: " + sizeMB + " MB");
        	_eventDao.persist(event);
        }
        
        return volumes.get(0).getPoolId();
    }

    public Long chooseHostForStoragePool(StoragePoolVO poolVO, List<Long> avoidHosts, boolean sendToVmResidesOn, Long vmId) {
    	if (sendToVmResidesOn) {
    		if (vmId != null) {
    			VMInstanceVO vmInstance = _vmInstanceDao.findById(vmId);
    			if (vmInstance != null) {
    				Long hostId = vmInstance.getHostId();
    				if (hostId != null && !avoidHosts.contains(vmInstance.getHostId()))
    					return hostId;
    			}
    		}
    		/*Can't find the vm where host resides on(vm is destroyed? or volume is detached from vm), randomly choose a host to send the cmd */
    	}
        List<StoragePoolHostVO> poolHosts = _poolHostDao.listByHostStatus(poolVO.getId(), Status.Up);
        Collections.shuffle(poolHosts);
        if (poolHosts != null && poolHosts.size() > 0) {
            for (StoragePoolHostVO sphvo : poolHosts) {
                if (!avoidHosts.contains(sphvo.getHostId())) {
                    return sphvo.getHostId();
                }
            }
        }
        return null;
    }

    @Override
    public String chooseStorageIp(VMInstanceVO vm, Host host, Host storage) {
        Enumeration<StoragePoolAllocator> en = _storagePoolAllocators.enumeration();
        while (en.hasMoreElements()) {
            StoragePoolAllocator allocator = en.nextElement();
            String ip = allocator.chooseStorageIp(vm, host, storage);
            if (ip != null) {
                return ip;
            }
        }

        assert false : "Hmm....fell thru the loop";
        return null;
    }

    @Override
    public boolean unshare(VMInstanceVO vm, List<VolumeVO> vols, HostVO host) {
        if (s_logger.isDebugEnabled()) {
            s_logger.debug("Asking for volumes of " + vm.toString() + " to be unshared to " + (host != null ? host.toString() : "all"));
        }

        return true;
    }

    @Override
    public void destroy(VMInstanceVO vm, List<VolumeVO> vols) {
        if (s_logger.isDebugEnabled() && vm != null) {
            s_logger.debug("Destroying volumes of " + vm.toString());
        }

        for (VolumeVO vol : vols) {
        	_volsDao.detachVolume(vol.getId());
            _volsDao.destroyVolume(vol.getId());

            // First delete the entries in the snapshot_policy and
            // snapshot_schedule table for the volume.
            // They should not get executed after the volume is destroyed.
            _snapshotMgr.deletePoliciesForVolume(vol.getId());

            String volumePath = vol.getPath();
            Long poolId = vol.getPoolId();
            if (poolId != null && volumePath != null && !volumePath.trim().isEmpty()) {
                Answer answer = null;
                StoragePoolVO pool = _storagePoolDao.findById(poolId);
                final DestroyCommand cmd = new DestroyCommand(pool, vol);
                boolean removed = false;
                List<StoragePoolHostVO> poolhosts = _storagePoolHostDao.listByPoolId(poolId);
                for (StoragePoolHostVO poolhost : poolhosts) {
                    answer = _agentMgr.easySend(poolhost.getHostId(), cmd);
                    if (answer != null && answer.getResult()) {
                        removed = true;
                        break;
                    }
                }

                if (removed) {
                    _volsDao.remove(vol.getId());
                } else {
                    _alertMgr.sendAlert(AlertManager.ALERT_TYPE_STORAGE_MISC, vol.getDataCenterId(), vol.getPodId(),
                            "Storage cleanup required for storage pool: " + pool.getName(), "Volume folder: " + vol.getFolder() + ", Volume Path: " + vol.getPath() + ", Volume id: " +vol.getId()+ ", Volume Name: " +vol.getName()+ ", Storage PoolId: " +vol.getPoolId());
					s_logger.warn("destroy volume " + vol.getFolder() + " : " + vol.getPath() + " failed for Volume id : " +vol.getId()+ " Volume Name: " +vol.getName()+ " Storage PoolId : " +vol.getPoolId());
                }
            } else {
                _volsDao.remove(vol.getId());
            }
        }

    }

    @Override
    public boolean configure(String name, Map<String, Object> params) throws ConfigurationException {
        _name = name;

        ComponentLocator locator = ComponentLocator.getCurrentLocator();

        ConfigurationDao configDao = locator.getDao(ConfigurationDao.class);
        if (configDao == null) {
            s_logger.error("Unable to get the configuration dao.");
            return false;
        }

        Map<String, String> configs = configDao.getConfiguration("management-server", params);
        

        String overProvisioningFactorStr = configs.get("storage.overprovisioning.factor");
        if (overProvisioningFactorStr != null) {
            _overProvisioningFactor = Integer.parseInt(overProvisioningFactorStr);
        }

        _retry = NumbersUtil.parseInt(configs.get(Config.StartRetry.key()), 2);
        _pingInterval = NumbersUtil.parseInt(configs.get("ping.interval"), 60);
        _hostRetry = NumbersUtil.parseInt(configs.get("host.retry"), 2);
        _storagePoolAcquisitionWaitSeconds = NumbersUtil.parseInt(configs.get("pool.acquisition.wait.seconds"), 1800);
        s_logger.info("pool.acquisition.wait.seconds is configured as " + _storagePoolAcquisitionWaitSeconds + " seconds");

        _totalRetries = NumbersUtil.parseInt(configDao.getValue("total.retries"), 4);
        _pauseInterval = 2*NumbersUtil.parseInt(configDao.getValue("ping.interval"), 60);
        
        String hypervisoType = configDao.getValue("hypervisor.type");
        if (hypervisoType.equalsIgnoreCase("KVM")) {
        	_hypervisorType = Hypervisor.Type.KVM;
        } else if(hypervisoType.equalsIgnoreCase("vmware")) {
        	_hypervisorType = Hypervisor.Type.VmWare;
        }
        _agentMgr.registerForHostEvents(new StoragePoolMonitor(this, _hostDao, _storagePoolDao), true, false, true);

        String storageCleanupEnabled = configs.get("storage.cleanup.enabled");
        _storageCleanupEnabled = (storageCleanupEnabled == null) ? true : Boolean.parseBoolean(storageCleanupEnabled);

        String time = configs.get("storage.cleanup.interval");
        _storageCleanupInterval = NumbersUtil.parseInt(time, 86400);

        String workers = configs.get("expunge.workers");
        int wrks = NumbersUtil.parseInt(workers, 10);
        _executor = Executors.newScheduledThreadPool(wrks, new NamedThreadFactory("StorageManager-Scavenger"));

        boolean localStorage = Boolean.parseBoolean(configs.get(Config.UseLocalStorage.key()));
        if (localStorage) {
            _agentMgr.registerForHostEvents(ComponentLocator.inject(LocalStoragePoolListener.class), true, false, false);
        }
        
        PoolsUsedByVmSearch = _storagePoolDao.createSearchBuilder();
        SearchBuilder<VolumeVO> volSearch = _volsDao.createSearchBuilder();
        PoolsUsedByVmSearch.join("volumes", volSearch, volSearch.entity().getPoolId(), PoolsUsedByVmSearch.entity().getId(), JoinBuilder.JoinType.INNER);
        volSearch.and("vm", volSearch.entity().getInstanceId(), SearchCriteria.Op.EQ);
        volSearch.done();
        PoolsUsedByVmSearch.done();
        
        HostTemplateStatesSearch = _vmTemplateHostDao.createSearchBuilder();
        HostTemplateStatesSearch.and("id", HostTemplateStatesSearch.entity().getTemplateId(), SearchCriteria.Op.EQ);
        HostTemplateStatesSearch.and("state", HostTemplateStatesSearch.entity().getDownloadState(), SearchCriteria.Op.EQ);
        
        SearchBuilder<HostVO> HostSearch = _hostDao.createSearchBuilder();
        HostSearch.and("dcId", HostSearch.entity().getDataCenterId(), SearchCriteria.Op.EQ);
        
        HostTemplateStatesSearch.join("host", HostSearch, HostSearch.entity().getId(), HostTemplateStatesSearch.entity().getHostId(), JoinBuilder.JoinType.INNER);
        HostSearch.done();
        HostTemplateStatesSearch.done();
        
        return true;
    }

    public String getVolumeFolder(String parentDir, long accountId, String diskFolderName) {
        StringBuilder diskFolderBuilder = new StringBuilder();
        Formatter diskFolderFormatter = new Formatter(diskFolderBuilder);
        diskFolderFormatter.format("%s/u%06d/%s", parentDir, accountId, diskFolderName);
        return diskFolderBuilder.toString();
    }

    public String getRandomVolumeName() {
        return UUID.randomUUID().toString();
    }

    @Override
    public boolean volumeOnSharedStoragePool(VolumeVO volume) {
        Long poolId = volume.getPoolId();
        if (poolId == null) {
            return false;
        } else {
            StoragePoolVO pool = _storagePoolDao.findById(poolId);

            if (pool == null) {
                return false;
            } else {
                return pool.isShared();
            }
        }
    }

    @Override
    public boolean volumeInactive(VolumeVO volume) {
        Long vmId = volume.getInstanceId();
        if (vmId != null) {
            UserVm vm = _userVmDao.findById(vmId);

            if (vm == null) {
                return false;
            }

            if (!vm.getState().equals(State.Stopped)) {
                return false;
            }
        }

        return true;
    }
    
    @Override
    public String getVmNameOnVolume(VolumeVO volume) {
    	 Long vmId = volume.getInstanceId();
         if (vmId != null) {
             VMInstanceVO vm = _vmInstanceDao.findById(vmId);

             if (vm == null) {
                 return null;
             }
             return vm.getInstanceName();
         }
         return null;
    }

    public Pair<String, String> getAbsoluteIsoPath(long templateId, long dataCenterId) {
        String isoPath = null;

        List<HostVO> storageHosts = _hostDao.listBy(Host.Type.SecondaryStorage, dataCenterId);
        if (storageHosts != null) {
            for (HostVO storageHost : storageHosts) {
                VMTemplateHostVO templateHostVO = _vmTemplateHostDao.findByHostTemplate(storageHost.getId(), templateId);
                if (templateHostVO != null) {
                    isoPath = storageHost.getStorageUrl() + "/" + templateHostVO.getInstallPath();
                    return new Pair<String, String>(isoPath, storageHost.getStorageUrl());
                }
            }
        }

        return null;
    }

    @Override
    public String getSecondaryStorageURL(long zoneId) {
        // Determine the secondary storage URL
        HostVO secondaryStorageHost = _hostDao.findSecondaryStorageHost(zoneId);

        if (secondaryStorageHost == null) {
            return null;
        }

        return secondaryStorageHost.getStorageUrl();
    }

    @Override
    public HostVO getSecondaryStorageHost(long zoneId) {
        return _hostDao.findSecondaryStorageHost(zoneId);
    }
    
    @Override
    public String getStoragePoolTags(long poolId) {
    	return _configMgr.listToCsvTags(_storagePoolDao.searchForStoragePoolDetails(poolId, "true"));
    }

    @Override
    public String getName() {
        return _name;
    }

    @Override
    public boolean start() {
        if (_storageCleanupEnabled) {
            _executor.scheduleWithFixedDelay(new StorageGarbageCollector(), _storageCleanupInterval, _storageCleanupInterval, TimeUnit.SECONDS);
        } else {
            s_logger.debug("Storage cleanup is not enabled, so the storage cleanup thread is not being scheduled.");
        }

        return true;
    }

    @Override
    public boolean stop() {
        if (_storageCleanupEnabled) {
            _executor.shutdown();
        }

        return true;
    }

    protected StorageManagerImpl() {
    }
    
    @Override
    public StoragePoolVO createPool(long zoneId, Long podId, Long clusterId, String poolName, URI uri, String tags, Map<String, String> details) throws ResourceInUseException, IllegalArgumentException, UnknownHostException, ResourceAllocationException {
        if (tags != null) {
            if (details == null) {
                details = new HashMap<String, String>();
            }
            String[] tokens = tags.split(",");
            
            for (String tag : tokens) {
                tag = tag.trim();
                if (tag.length() == 0) {
                    continue;
                }
                details.put(tag, "true");
            }
        }
        
        Hypervisor.Type hypervisorType = null;
        List<HostVO> hosts = null;
        if (podId != null) {
            hosts = _hostDao.listByHostPod(podId);
        } else {
            hosts = _hostDao.listByDataCenter(zoneId);
        }
        
        for (HostVO h : hosts) {
            if (h.getType() == Type.Routing) {
                hypervisorType = h.getHypervisorType();
                break;
            }
        }
        if (hypervisorType == null) {
        	if (_hypervisorType == Hypervisor.Type.KVM) {
        		hypervisorType = Hypervisor.Type.KVM;
        	} else if(_hypervisorType == Hypervisor.Type.VmWare) {
        		hypervisorType = Hypervisor.Type.VmWare;
        	} else {
        		s_logger.debug("Couldn't find a host to serve in the server pool");
        		return null;
        	}
        }
        
        String scheme = uri.getScheme();
        String storageHost = uri.getHost();
        String hostPath = uri.getPath();
        int port = uri.getPort();
        StoragePoolVO pool = null;
        s_logger.debug("createPool Params @ scheme - " +scheme+ " storageHost - " +storageHost+ " hostPath - " +hostPath+ " port - " +port);
        if (scheme.equalsIgnoreCase("nfs")) {
            if (port == -1) {
                port = 2049;
            }
            pool = new StoragePoolVO(StoragePoolType.NetworkFilesystem, storageHost, port, hostPath);
            if (hypervisorType == Hypervisor.Type.XenServer && clusterId == null) {
                throw new IllegalArgumentException("NFS need to have clusters specified for XenServers");
            }
        } else if (scheme.equalsIgnoreCase("file")) {
            if (port == -1) {
                port = 0;
            }
            pool = new StoragePoolVO(StoragePoolType.Filesystem, "localhost", 0, hostPath);
        } else if (scheme.equalsIgnoreCase("iscsi")) {
            String[] tokens = hostPath.split("/");
            int lun = NumbersUtil.parseInt(tokens[tokens.length - 1], -1);
            if (port == -1) {
                port = 3260;
            }
            if (lun != -1) {
                if (hypervisorType == Hypervisor.Type.XenServer && clusterId == null) {
                    throw new IllegalArgumentException("IscsiLUN need to have clusters specified");
                }
                hostPath.replaceFirst("/", "");
                pool = new StoragePoolVO(StoragePoolType.IscsiLUN, storageHost, port, hostPath);
            } else {
                Enumeration<StoragePoolDiscoverer> en = _discoverers.enumeration();
                while (en.hasMoreElements()) {
                    Map<StoragePoolVO, Map<String, String>> pools;
                    try {
                        pools = en.nextElement().find(zoneId, podId, uri, details);
                    } catch (DiscoveryException e) {
                        throw new IllegalArgumentException("Not enough information for discovery " + uri, e);
                    }
                    if (pools != null) {
                        Map.Entry<StoragePoolVO, Map<String, String>> entry = pools.entrySet().iterator().next();
                        pool = entry.getKey();
                        details = entry.getValue();
                        break;
                    }
                }
            }
        } else if (scheme.equalsIgnoreCase("iso")) {
            if (port == -1) {
                port = 2049;
            }
            pool = new StoragePoolVO(StoragePoolType.ISO, storageHost, port, hostPath);
        } else {
            s_logger.warn("Unable to figure out the scheme for URI: " + uri);
            throw new IllegalArgumentException("Unable to figure out the scheme for URI: " + uri);
        }

        if (pool == null) {
            s_logger.warn("Unable to figure out the scheme for URI: " + uri);
            throw new IllegalArgumentException("Unable to figure out the scheme for URI: " + uri);
        }
        
        List<StoragePoolVO> pools = _storagePoolDao.listPoolByHostPath(storageHost, hostPath);
        if (!pools.isEmpty()) {
            Long oldPodId = pools.get(0).getPodId();
            throw new ResourceInUseException("Storage pool " + uri + " already in use by another pod (id=" + oldPodId + ")", "StoragePool", uri.toASCIIString());
        }

        // iterate through all the hosts and ask them to mount the filesystem.
        // FIXME Not a very scalable implementation. Need an async listener, or
        // perhaps do this on demand, or perhaps mount on a couple of hosts per
        // pod
        List<HostVO> allHosts = _hostDao.listBy(Host.Type.Routing, clusterId, podId, zoneId);
        if (allHosts.isEmpty() && _hypervisorType != Hypervisor.Type.KVM) {
            throw new ResourceAllocationException("No host exists to associate a storage pool with");
        }
        long poolId = _storagePoolDao.getNextInSequence(Long.class, "id");
        String uuid = UUID.nameUUIDFromBytes(new String(storageHost + hostPath).getBytes()).toString();
        
        List<StoragePoolVO> spHandles = _storagePoolDao.findIfDuplicatePoolsExistByUUID(uuid);
        if(spHandles!=null && spHandles.size()>0)
        {
        	s_logger.debug("Another active pool with the same uuid already exists");
        	throw new ResourceInUseException("Another active pool with the same uuid already exists");
        }
        
        s_logger.debug("In createPool Setting poolId - " +poolId+ " uuid - " +uuid+ " zoneId - " +zoneId+ " podId - " +podId+ " poolName - " +poolName);
        pool.setId(poolId);
        pool.setUuid(uuid);
        pool.setDataCenterId(zoneId);
        pool.setPodId(podId);
        pool.setName(poolName);
        pool.setClusterId(clusterId);
        pool.setStatus(Status.Up);
        pool = _storagePoolDao.persist(pool, details);
        if (_hypervisorType == Hypervisor.Type.KVM && allHosts.isEmpty()) {
        	return pool;
        }
        s_logger.debug("In createPool Adding the pool to each of the hosts");
        List<HostVO> poolHosts = new ArrayList<HostVO>();
        for (HostVO h : allHosts) {
            boolean success = addPoolToHost(h.getId(), pool);
            if (success) {
                poolHosts.add(h);
            }
        }

        if (poolHosts.isEmpty()) {
            _storagePoolDao.expunge(pool.getId());
            pool = null;
        } else {
            createCapacityEntry(pool);
        }
        return pool;
    }
    
    @Override
    public StoragePoolVO updateStoragePool(long poolId, String tags) throws IllegalArgumentException {
    	StoragePoolVO pool = _storagePoolDao.findById(poolId);
    	if (pool == null) {
    		throw new IllegalArgumentException("Unable to find storage pool with ID: " + poolId);
    	}
    	
    	if (tags != null) {
    		Map<String, String> details = _storagePoolDao.getDetails(poolId);
    		String[] tagsList = tags.split(",");
    		for (String tag : tagsList) {
    			tag = tag.trim();
    			if (tag.length() > 0 && !details.containsKey(tag)) {
    				details.put(tag, "true");
    			}
    		}
    		
    		_storagePoolDao.updateDetails(poolId, details);
    	}
    	
    	return pool;
    }

    @Override
    @DB
    public boolean deletePool(long id) {
        boolean deleteFlag = false;

        // get the pool to delete
        StoragePoolVO sPool = _storagePoolDao.findById(id);

        if (sPool == null)
            return false;

        // for the given pool id, find all records in the storage_pool_host_ref
        List<StoragePoolHostVO> hostPoolRecords = _storagePoolHostDao.listByPoolId(id);

        // if not records exist, delete the given pool (base case)
        if (hostPoolRecords.size() == 0) {
            _storagePoolDao.remove(id);
            return true;
        } else {
            // 1. Check if the pool has associated volumes in the volumes table
            // 2. If it does, then you cannot delete the pool
            Pair<Long, Long> volumeRecords = _volsDao.getCountAndTotalByPool(id);

            if (volumeRecords.first() > 0) {
                return false; // cannot delete as there are associated vols
            }
            // 3. Else part, remove the SR associated with the Xenserver
            else {
                // First get the host_id from storage_pool_host_ref for given
                // pool id
                StoragePoolVO lock = _storagePoolDao.acquire(sPool.getId());
                try {
                    if (lock == null) {
                        s_logger.debug("Failed to acquire lock when deleting StoragePool with ID: " + sPool.getId());
                        return false;
                    }

                    for (StoragePoolHostVO host : hostPoolRecords) {
                        DeleteStoragePoolCommand cmd = new DeleteStoragePoolCommand(sPool);
                        final Answer answer = _agentMgr.easySend(host.getHostId(), cmd);

                        if (answer != null) {
                            if (answer.getResult() == true) {
                                deleteFlag = true;
                                break;
                            }
                        }
                    }

                } finally {
                    if (lock != null) {
                        _storagePoolDao.release(lock.getId());
                    }
                }

                if (deleteFlag) {
                    // now delete the storage_pool_host_ref and storage_pool
                    // records
                    for (StoragePoolHostVO host : hostPoolRecords) {
                        _storagePoolHostDao.deleteStoragePoolHostDetails(host.getHostId(),host.getPoolId());
                    }

                    _storagePoolDao.remove(id);
                    return true;
                }
            }
        }
        return false;

    }
    
    @Override
    public boolean addPoolToHost(long hostId, StoragePoolVO pool) {
        s_logger.debug("Adding pool " + pool.getName() + " to  host " + hostId);
        if (pool.getPoolType() != StoragePoolType.NetworkFilesystem && pool.getPoolType() != StoragePoolType.Filesystem && pool.getPoolType() != StoragePoolType.IscsiLUN && pool.getPoolType() != StoragePoolType.Iscsi) {
            return true;
        }
        ModifyStoragePoolCommand cmd = new ModifyStoragePoolCommand(true, pool);
        final Answer answer = _agentMgr.easySend(hostId, cmd);

        if (answer != null) {
            if (answer.getResult() == false) {
                String msg = "Add host failed due to ModifyStoragePoolCommand failed" + answer.getDetails();
                _alertMgr.sendAlert(AlertManager.ALERT_TYPE_HOST, pool.getDataCenterId(), pool.getPodId(), msg, msg);
                s_logger.warn(msg);
                return false;
            }
            if (answer instanceof ModifyStoragePoolAnswer) {
                ModifyStoragePoolAnswer mspAnswer = (ModifyStoragePoolAnswer) answer;

                StoragePoolHostVO poolHost = _poolHostDao.findByPoolHost(pool.getId(), hostId);
                if (poolHost == null) {
                    poolHost = new StoragePoolHostVO(pool.getId(), hostId, mspAnswer.getPoolInfo().getLocalPath().replaceAll("//", "/"));
                    _poolHostDao.persist(poolHost);
                } else {
                    poolHost.setLocalPath(mspAnswer.getPoolInfo().getLocalPath().replaceAll("//", "/"));
                }
                pool.setAvailableBytes(mspAnswer.getPoolInfo().getAvailableBytes());
                pool.setCapacityBytes(mspAnswer.getPoolInfo().getCapacityBytes());
                _storagePoolDao.update(pool.getId(), pool);
                return true;
            }

        } else {
            return false;
        }
        return false;
    }

    @Override
    public VolumeVO moveVolume(VolumeVO volume, long destPoolDcId, Long destPoolPodId, Long destPoolClusterId) throws InternalErrorException {
    	// Find a destination storage pool with the specified criteria
    	DiskOfferingVO diskOffering = _diskOfferingDao.findById(volume.getDiskOfferingId());
    	DiskProfile dskCh = new DiskProfile(volume.getId(), volume.getVolumeType(), volume.getName(), diskOffering.getId(), diskOffering.getDiskSizeInBytes(), diskOffering.getTagsArray(), diskOffering.getUseLocalStorage(), diskOffering.isRecreatable(), null);
    	DataCenterVO destPoolDataCenter = _dcDao.findById(destPoolDcId);
    	HostPodVO destPoolPod = _podDao.findById(destPoolPodId);
        StoragePoolVO destPool = findStoragePool(dskCh, destPoolDataCenter, destPoolPod, destPoolClusterId, null, null, null, new HashSet<StoragePool>());
    	
        if (destPool == null) {
        	throw new InternalErrorException("Failed to find a storage pool with enough capacity to move the volume to.");
        }
        
        StoragePoolVO srcPool = _storagePoolDao.findById(volume.getPoolId());
        String secondaryStorageURL = getSecondaryStorageURL(volume.getDataCenterId());
        String secondaryStorageVolumePath = null;

        // Find hosts where the source and destination storage pools are visible
        Long sourceHostId = findHostIdForStoragePool(srcPool);
        Long destHostId = findHostIdForStoragePool(destPool);

        if (sourceHostId == null) {
            throw new InternalErrorException("Failed to find a host where the source storage pool is visible.");
        } else if (destHostId == null) {
            throw new InternalErrorException("Failed to find a host where the dest storage pool is visible.");
        }

        // Copy the volume from the source storage pool to secondary storage
        CopyVolumeCommand cvCmd = new CopyVolumeCommand(volume.getId(), volume.getPath(), srcPool, secondaryStorageURL, true);
        CopyVolumeAnswer cvAnswer = (CopyVolumeAnswer) _agentMgr.easySend(sourceHostId, cvCmd);

        if (cvAnswer == null || !cvAnswer.getResult()) {
            throw new InternalErrorException("Failed to copy the volume from the source primary storage pool to secondary storage.");
        }

        secondaryStorageVolumePath = cvAnswer.getVolumePath();

        // Copy the volume from secondary storage to the destination storage
        // pool
        cvCmd = new CopyVolumeCommand(volume.getId(), secondaryStorageVolumePath, destPool, secondaryStorageURL, false);
        cvAnswer = (CopyVolumeAnswer) _agentMgr.easySend(destHostId, cvCmd);

        if (cvAnswer == null || !cvAnswer.getResult()) {
            throw new InternalErrorException("Failed to copy the volume from secondary storage to the destination primary storage pool.");
        }

        String destPrimaryStorageVolumePath = cvAnswer.getVolumePath();
        String destPrimaryStorageVolumeFolder = cvAnswer.getVolumeFolder();

        // Delete the volume on the source storage pool
        final DestroyCommand cmd = new DestroyCommand(srcPool, volume);
        Answer destroyAnswer = _agentMgr.easySend(sourceHostId, cmd);
        
        if (destroyAnswer == null || !destroyAnswer.getResult()) {
            throw new InternalErrorException("Failed to delete the volume from the source primary storage pool.");
        }

        volume.setPath(destPrimaryStorageVolumePath);
        volume.setFolder(destPrimaryStorageVolumeFolder);
        volume.setPodId(destPool.getPodId());
        volume.setPoolId(destPool.getId());
        _volsDao.update(volume.getId(), volume);

        return _volsDao.findById(volume.getId());
    }

    @Override
    @DB
    public VolumeVO createVolume(long accountId, long userId, String userSpecifiedName, DataCenterVO dc, DiskOfferingVO diskOffering, long startEventId, long size) 
    {
    	String volumeName = "";
        VolumeVO createdVolume = null;
        
        try 
        {
	        // Determine the volume's name
	        volumeName = getRandomVolumeName();
	
	        // Create the Volume object and save it so that we can return it to the user
	        Account account = _accountDao.findById(accountId);
	        VolumeVO volume = new VolumeVO(userSpecifiedName, -1, -1, -1, -1, new Long(-1), null, null, 0, Volume.VolumeType.DATADISK);
	        volume.setPoolId(null);
	        volume.setDataCenterId(dc.getId());
	        volume.setPodId(null);
	        volume.setAccountId(accountId);
	        volume.setDomainId(account.getDomainId());
	        volume.setMirrorState(MirrorState.NOT_MIRRORED);
	        volume.setDiskOfferingId(diskOffering.getId());
	        volume.setStorageResourceType(Storage.StorageResourceType.STORAGE_POOL);
	        volume.setInstanceId(null);
	        volume.setUpdated(new Date());
	        volume.setStatus(AsyncInstanceCreateStatus.Creating);
	        volume.setDomainId(account.getDomainId());
	        volume.setSourceId(diskOffering.getId());
	        volume.setSourceType(SourceType.DiskOffering);
	        volume = _volsDao.persist(volume);
	
	        AsyncJobExecutor asyncExecutor = BaseAsyncJobExecutor.getCurrentExecutor();
	        if (asyncExecutor != null) {
	            AsyncJobVO job = asyncExecutor.getJob();
	
	            if (s_logger.isInfoEnabled())
	                s_logger.info("CreateVolume created a new instance " + volume.getId() + ", update async job-" + job.getId() + " progress status");
	
	            _asyncMgr.updateAsyncJobAttachment(job.getId(), "volume", volume.getId());
	            _asyncMgr.updateAsyncJobStatus(job.getId(), BaseCmd.PROGRESS_INSTANCE_CREATED, volume.getId());
	        }
	        
	        List<StoragePoolVO> poolsToAvoid = new ArrayList<StoragePoolVO>();
	        Set<Long> podsToAvoid = new HashSet<Long>();
	        Pair<HostPodVO, Long> pod = null;
	
	        while ((pod = _agentMgr.findPod(null, null, dc, account.getId(), podsToAvoid)) != null) {
	            if ((createdVolume = createVolume(volume, null, null, dc, pod.first(), null, null, diskOffering, poolsToAvoid, size)) != null) {
	            	break;
	            } else {
	                podsToAvoid.add(pod.first().getId());
	            }
	        }
	
	        // Create an event
	        EventVO event = new EventVO();
	        event.setAccountId(accountId);
	        event.setUserId(userId);
	        event.setType(EventTypes.EVENT_VOLUME_CREATE);
	        event.setStartId(startEventId);
	
	        Transaction txn = Transaction.currentTxn();

            txn.start();

            if (createdVolume != null) {
                // Increment the number of volumes
                _accountMgr.incrementResourceCount(accountId, ResourceType.volume);
                
                // Set event parameters
                long sizeMB = createdVolume.getSize() / (1024 * 1024);
                StoragePoolVO pool = _storagePoolDao.findById(createdVolume.getPoolId());
                String eventParams = "id=" + createdVolume.getId() + "\ndoId=" + diskOffering.getId() + "\ntId=" + -1 + "\ndcId=" + dc.getId() + "\nsize=" + sizeMB;
                event.setLevel(EventVO.LEVEL_INFO);
                event.setDescription("Created volume: " + createdVolume.getName() + " with size: " + sizeMB + " MB in pool: " + pool.getName());
                event.setParameters(eventParams);
                _eventDao.persist(event);
            } else {
            	// Mark the existing volume record as corrupted
                volume.setStatus(AsyncInstanceCreateStatus.Corrupted);
                volume.setDestroyed(true);
                _volsDao.update(volume.getId(), volume);
            }            

            txn.commit();
        } catch (Exception e) {
            s_logger.error("Unhandled exception while saving volume " + volumeName, e);
        }
        
        return createdVolume;
    }

    @Override
    @DB
    public void destroyVolume(VolumeVO volume) {
    	Transaction txn = Transaction.currentTxn();
    	txn.start();
    	
        Long volumeId = volume.getId();
        _volsDao.destroyVolume(volumeId);
        
        String eventParams = "id=" + volumeId;
        EventVO event = new EventVO();
        event.setAccountId(volume.getAccountId());
        event.setUserId(1L);
        event.setType(EventTypes.EVENT_VOLUME_DELETE);
        event.setParameters(eventParams);
        event.setDescription("Volume " +volume.getName()+ " deleted");
        event.setLevel(EventVO.LEVEL_INFO);
        _eventDao.persist(event);

        // Delete the recurring snapshot policies for this volume.
        _snapshotMgr.deletePoliciesForVolume(volumeId);

        // Decrement the resource count for volumes
        _accountMgr.decrementResourceCount(volume.getAccountId(), ResourceType.volume);
                
        txn.commit();
    }
    
    @Override
    public void createCapacityEntry(StoragePoolVO storagePool) {
    	createCapacityEntry(storagePool, 0);
    }

    @Override
    public void createCapacityEntry(StoragePoolVO storagePool, long allocated) {
        SearchCriteria capacitySC = _capacityDao.createSearchCriteria();
        capacitySC.addAnd("hostOrPoolId", SearchCriteria.Op.EQ, storagePool.getId());
        capacitySC.addAnd("dataCenterId", SearchCriteria.Op.EQ, storagePool.getDataCenterId());
        capacitySC.addAnd("capacityType", SearchCriteria.Op.EQ, CapacityVO.CAPACITY_TYPE_STORAGE);

        List<CapacityVO> capacities = _capacityDao.search(capacitySC, null);

        if (capacities.size() == 0) {
            CapacityVO capacity = new CapacityVO(storagePool.getId(), storagePool.getDataCenterId(), storagePool.getPodId(), storagePool.getAvailableBytes(), storagePool.getCapacityBytes(),
                    CapacityVO.CAPACITY_TYPE_STORAGE);
            _capacityDao.persist(capacity);
        } else {
            CapacityVO capacity = capacities.get(0);
            capacity.setTotalCapacity(storagePool.getCapacityBytes());
            long used = storagePool.getCapacityBytes() - storagePool.getAvailableBytes();
            if( used <= 0 ) {
            	used = 0;
            }
            capacity.setUsedCapacity(used);
            _capacityDao.update(capacity.getId(), capacity);
        }
        s_logger.debug("Successfully set Capacity - " +storagePool.getCapacityBytes()+ " for CAPACITY_TYPE_STORAGE, DataCenterId - " +storagePool.getDataCenterId()+ ", HostOrPoolId - " +storagePool.getId()+ ", PodId " +storagePool.getPodId());
        capacitySC = _capacityDao.createSearchCriteria();
        capacitySC.addAnd("hostOrPoolId", SearchCriteria.Op.EQ, storagePool.getId());
        capacitySC.addAnd("dataCenterId", SearchCriteria.Op.EQ, storagePool.getDataCenterId());
        capacitySC.addAnd("capacityType", SearchCriteria.Op.EQ, CapacityVO.CAPACITY_TYPE_STORAGE_ALLOCATED);

        capacities = _capacityDao.search(capacitySC, null);

        int provFactor = 1;
        if( storagePool.getPoolType() == StoragePoolType.NetworkFilesystem ) {
            provFactor = _overProvisioningFactor;
        }
        if (capacities.size() == 0) {
            CapacityVO capacity = new CapacityVO(storagePool.getId(), storagePool.getDataCenterId(), storagePool.getPodId(), allocated, storagePool.getCapacityBytes()
                    * provFactor, CapacityVO.CAPACITY_TYPE_STORAGE_ALLOCATED);
            _capacityDao.persist(capacity);
        } else {
            CapacityVO capacity = capacities.get(0);
            long currCapacity = provFactor * storagePool.getCapacityBytes();
        	boolean update = false;
            if (capacity.getTotalCapacity() != currCapacity) {
                capacity.setTotalCapacity(currCapacity);
                update = true;
            }
            if ( allocated != 0 ) {
                capacity.setUsedCapacity(allocated);
                update = true;
            }
            if ( update ) {
            	_capacityDao.update(capacity.getId(), capacity);
            }
        }
        s_logger.debug("Successfully set Capacity - " +storagePool.getCapacityBytes()* _overProvisioningFactor+ " for CAPACITY_TYPE_STORAGE_ALLOCATED, DataCenterId - " +storagePool.getDataCenterId()+ ", HostOrPoolId - " +storagePool.getId()+ ", PodId " +storagePool.getPodId());
    }

    @Override
    public Answer sendToHostsOnStoragePool(Long poolId, Command cmd, String basicErrMsg) {
        return sendToHostsOnStoragePool(poolId, cmd, basicErrMsg, 1, 0, false,  null);
    }

    @Override
    public Answer sendToHostsOnStoragePool(Long poolId, Command cmd, String basicErrMsg, int totalRetries, int pauseBeforeRetry, boolean shouldBeSnapshotCapable,
    									 Long vmId) {
        Answer answer = null;
        Long hostId = null;
        StoragePoolVO storagePool = _storagePoolDao.findById(poolId);
        List<Long> hostsToAvoid = new ArrayList<Long>();
        
        int tryCount = 0;
        boolean sendToVmHost = sendToVmResidesOn(cmd);
       
        if (chooseHostForStoragePool(storagePool, hostsToAvoid, sendToVmHost, vmId) == null) {
            // Don't just fail. The host could be reconnecting.
            // wait for some time for it to get connected
            // Wait for 3*ping.interval, since the code attempts a manual
            // reconnect after that timeout.
            try {
                Thread.sleep(3 * _pingInterval * 1000);
            } catch (InterruptedException e) {
                s_logger.error("Interrupted while waiting for any host on poolId: " + poolId + " to get connected. " + e.getMessage());
                // continue.
            }
        }
        while ((hostId = chooseHostForStoragePool(storagePool, hostsToAvoid, sendToVmHost, vmId)) != null && tryCount++ < totalRetries) {
            String errMsg = basicErrMsg + " on host: " + hostId + " try: " + tryCount + ", reason: ";
            try {
                HostVO hostVO = _hostDao.findById(hostId);
                if (shouldBeSnapshotCapable) {
                    if (hostVO == null ) {
                        hostsToAvoid.add(hostId);
                        continue;
                    }
                }
                s_logger.debug("Trying to execute Command: " + cmd + " on host: " + hostId + " try: " + tryCount);
                // set 120 min timeout for storage related command
                answer = _agentMgr.send(hostId, cmd, 120*60*1000);
                
                if (answer != null && answer.getResult()) {
                    return answer;
                } else {
                    s_logger.warn(errMsg + ((answer != null) ? answer.getDetails() : "null"));
                    Thread.sleep(pauseBeforeRetry * 1000);
                }
            } catch (AgentUnavailableException e1) {
                s_logger.warn(errMsg + e1.getMessage(), e1);
            } catch (OperationTimedoutException e1) {
                s_logger.warn(errMsg + e1.getMessage(), e1);
            } catch (InterruptedException e) {
                s_logger.warn(errMsg + e.getMessage(), e);
            }
        }

        s_logger.error(basicErrMsg + ", no hosts available to execute command: " + cmd);
        return answer;
    }

    protected class StorageGarbageCollector implements Runnable {

        public StorageGarbageCollector() {
        }

        @Override
        public void run() {
            try {
                s_logger.info("Storage Garbage Collection Thread is running.");

                GlobalLock scanLock = GlobalLock.getInternLock(this.getClass().getName());
                try {
                    if (scanLock.lock(3)) {
                        try {
                            cleanupStorage(true);
                        } finally {
                            scanLock.unlock();
                        }
                    }
                } finally {
                    scanLock.releaseRef();
                }

            } catch (Exception e) {
                s_logger.error("Caught the following Exception", e);
            }
        }
    }

    @Override
    public void cleanupStorage(boolean recurring) {

        // Cleanup primary storage pools
        List<StoragePoolVO> storagePools = _storagePoolDao.listAll();
        for (StoragePoolVO pool : storagePools) {
            try {
                if (recurring && pool.isLocal()) {
                    continue;
                }
    
                List<VMTemplateStoragePoolVO> unusedTemplatesInPool = _tmpltMgr.getUnusedTemplatesInPool(pool);
                s_logger.debug("Storage pool garbage collector found " + unusedTemplatesInPool.size() + " templates to clean up in storage pool: " + pool.getName());
                for (VMTemplateStoragePoolVO templatePoolVO : unusedTemplatesInPool) {
                    if (templatePoolVO.getDownloadState() != VMTemplateStorageResourceAssoc.Status.DOWNLOADED) {
                        s_logger.debug("Storage pool garbage collector is skipping templatePoolVO with ID: " + templatePoolVO.getId() + " because it is not completely downloaded.");
                        continue;
                    }
    
                    if (!templatePoolVO.getMarkedForGC()) {
                        templatePoolVO.setMarkedForGC(true);
                        _vmTemplatePoolDao.update(templatePoolVO.getId(), templatePoolVO);
                        s_logger.debug("Storage pool garbage collector has marked templatePoolVO with ID: " + templatePoolVO.getId() + " for garbage collection.");
                        continue;
                    }
    
                    _tmpltMgr.evictTemplateFromStoragePool(templatePoolVO);
                }
            } catch (Exception e) {
                s_logger.warn("Problem cleaning up primary storage pool " + pool, e);
            }
        }

        // Cleanup secondary storage hosts
        List<HostVO> secondaryStorageHosts = _hostDao.listSecondaryStorageHosts();
        for (HostVO secondaryStorageHost : secondaryStorageHosts) {
            try {
                long hostId = secondaryStorageHost.getId();
                List<VMTemplateHostVO> destroyedTemplateHostVOs = _vmTemplateHostDao.listDestroyed(hostId);
                s_logger.debug("Secondary storage garbage collector found " + destroyedTemplateHostVOs.size() + " templates to cleanup on secondary storage host: "
                        + secondaryStorageHost.getName());
                for (VMTemplateHostVO destroyedTemplateHostVO : destroyedTemplateHostVOs) {
                    if (!_tmpltMgr.templateIsDeleteable(destroyedTemplateHostVO)) {
                        s_logger.debug("Not deleting template at: " + destroyedTemplateHostVO.getInstallPath());
                        continue;
                    }
    
                    String installPath = destroyedTemplateHostVO.getInstallPath();
    
                    if (installPath != null) {
                        Answer answer = _agentMgr.easySend(hostId, new DeleteTemplateCommand(destroyedTemplateHostVO.getInstallPath()));
    
                        if (answer == null || !answer.getResult()) {
                            s_logger.debug("Failed to delete template at: " + destroyedTemplateHostVO.getInstallPath());
                        } else {
                            _vmTemplateHostDao.remove(destroyedTemplateHostVO.getId());
                            s_logger.debug("Deleted template at: " + destroyedTemplateHostVO.getInstallPath());
                        }
                    } else {
                        _vmTemplateHostDao.remove(destroyedTemplateHostVO.getId());
                    }
                }
            } catch (Exception e) {
                s_logger.warn("problem cleaning up secondary storage " + secondaryStorageHost, e);
            }
        }
        
        List<VolumeVO> vols = _volsDao.listRemovedButNotDestroyed();
        for (VolumeVO vol : vols) {
            try {
                Long poolId = vol.getPoolId();
                Answer answer = null;
                StoragePoolVO pool = _storagePoolDao.findById(poolId);
                final DestroyCommand cmd = new DestroyCommand(pool, vol);
                answer = sendToPool(pool, cmd);
                if (answer != null && answer.getResult()) {
                    s_logger.debug("Destroyed " + vol);
                    vol.setDestroyed(true);
                    _volsDao.update(vol.getId(), vol);
                }
            } catch (Exception e) {
                s_logger.warn("Unable to destroy " + vol.getId(), e);
            }
        }

    }
    
    @Override
    public List<StoragePoolVO> getStoragePoolsForVm(long vmId) {
        SearchCriteria<StoragePoolVO> sc = PoolsUsedByVmSearch.create();
        sc.setJoinParameters("volumes", "vm", vmId);
        
        return _storagePoolDao.search(sc, null);
    }
    
    @Override
    public String getPrimaryStorageNameLabel(VolumeVO volume) {
        Long poolId     = volume.getPoolId();
        
        // poolId is null only if volume is destroyed, which has been checked before.
        assert poolId != null;
        StoragePoolVO storagePoolVO = _storagePoolDao.findById(poolId);
        assert storagePoolVO != null;
        return storagePoolVO.getUuid();
    }
    
    @Override
    @DB
    public boolean preparePrimaryStorageForMaintenance(long primaryStorageId, long userId) 
    {
        long count = 1;
        boolean restart = true;
        try 
        {
        	//1. Get the primary storage record
        	StoragePoolVO primaryStorage = _storagePoolDao.findById(primaryStorageId);
        	
        	if(primaryStorage == null)
        	{
        		s_logger.warn("The primary storage does not exist");
        		return false;
        	}	
        	
        	//check to see if other ps exist
        	//if they do, then we can migrate over the system vms to them
        	//if they dont, then just stop all vms on this one
        	List<StoragePoolVO> upPools = _storagePoolDao.listPoolsByStatus(Status.Up);
        	
        	if(upPools==null || upPools.size()==0)
        		restart = false;
        		
        	//2. Get a list of all the volumes within this storage pool
        	List<VolumeVO> allVolumes = _volsDao.findByPoolId(primaryStorageId);
        	
        	//3. Each volume has an instance associated with it, stop the instance if running
        	for(VolumeVO volume : allVolumes)
        	{
        		VMInstanceVO vmInstance = _vmInstanceDao.findById(volume.getInstanceId());
        		
        		if(vmInstance == null)
        			continue;
        		
        		//shut down the running vms
        		if(vmInstance.getState().equals(State.Running) || vmInstance.getState().equals(State.Stopped) || vmInstance.getState().equals(State.Stopping) || vmInstance.getState().equals(State.Starting))
        		{
        			
        			//if the instance is of type consoleproxy, call the console proxy
        			if(vmInstance.getType().equals(VirtualMachine.Type.ConsoleProxy))
        			{        				
        				//make sure it is not restarted again, update config to set flag to false
        				_configMgr.updateConfiguration(userId, "consoleproxy.restart", "false");
        				
        				//create a dummy event
        				long eventId = saveScheduledEvent(User.UID_SYSTEM, Account.ACCOUNT_ID_SYSTEM, EventTypes.EVENT_PROXY_STOP, "stopping console proxy with Id: "+vmInstance.getId());
        		        
        				//call the consoleproxymanager
        				if(!_consoleProxyMgr.stopProxy(vmInstance.getId(), eventId))
            			{
            				s_logger.warn("There was an error stopping the console proxy id: "+vmInstance.getId()+" ,cannot enable storage maintenance");
                        	primaryStorage.setStatus(Status.ErrorInMaintenance);
                    		_storagePoolDao.persist(primaryStorage);
                    		return false;
            			}
        				else if(restart)
        				{
    						//create a dummy event
    						long eventId1 = saveScheduledEvent(User.UID_SYSTEM, Account.ACCOUNT_ID_SYSTEM, EventTypes.EVENT_PROXY_START, "starting console proxy with Id: "+vmInstance.getId());
    						
    						//Restore config val for consoleproxy.restart to true
    						_configMgr.updateConfiguration(userId, "consoleproxy.restart", "true");
    						
    						if(_consoleProxyMgr.startProxy(vmInstance.getId(), eventId1)==null)
    						{
    							s_logger.warn("There was an error starting the console proxy id: "+vmInstance.getId()+" on another storage pool, cannot enable primary storage maintenance");
    			            	primaryStorage.setStatus(Status.ErrorInMaintenance);
    			        		_storagePoolDao.persist(primaryStorage);
    							return false;				
    						}	  						
        				}
        			}
        			
        			//if the instance is of type uservm, call the user vm manager
        			if(vmInstance.getType().equals(VirtualMachine.Type.User))
        			{
        				
        				if(!_userVmMgr.stopVirtualMachine(userId, vmInstance.getId()))
        				{
        					s_logger.warn("There was an error stopping the user vm id: "+vmInstance.getId()+" ,cannot enable storage maintenance");
        	            	primaryStorage.setStatus(Status.ErrorInMaintenance);
        	        		_storagePoolDao.persist(primaryStorage);
        					return false;
        				}
        			}
        			
        			//if the instance is of type secondary storage vm, call the secondary storage vm manager
        			if(vmInstance.getType().equals(VirtualMachine.Type.SecondaryStorageVm))
        			{           				
        				//create a dummy event
        				long eventId1 = saveScheduledEvent(User.UID_SYSTEM, Account.ACCOUNT_ID_SYSTEM, EventTypes.EVENT_SSVM_STOP, "stopping ssvm with Id: "+vmInstance.getId());

        				if(!_secStorageMgr.stopSecStorageVm(vmInstance.getId(), eventId1))
        				{
        					s_logger.warn("There was an error stopping the ssvm id: "+vmInstance.getId()+" ,cannot enable storage maintenance");
        	            	primaryStorage.setStatus(Status.ErrorInMaintenance);
        	        		_storagePoolDao.persist(primaryStorage);
        					return false;
        				}
        				else if(restart)
        				{
    						//create a dummy event and restart the ssvm immediately
    						long eventId = saveScheduledEvent(User.UID_SYSTEM, Account.ACCOUNT_ID_SYSTEM, EventTypes.EVENT_SSVM_START, "starting ssvm with Id: "+vmInstance.getId());
    						if(_secStorageMgr.startSecStorageVm(vmInstance.getId(), eventId)==null)
    						{
    							s_logger.warn("There was an error starting the ssvm id: "+vmInstance.getId()+" on another storage pool, cannot enable primary storage maintenance");
    			            	primaryStorage.setStatus(Status.ErrorInMaintenance);
    			        		_storagePoolDao.persist(primaryStorage);
    							return false;
    						}
        				}
        			}

           			//if the instance is of type domain router vm, call the network manager
        			if(vmInstance.getType().equals(VirtualMachine.Type.DomainRouter))
        			{   
        				//create a dummy event
        				long eventId2 = saveScheduledEvent(User.UID_SYSTEM, Account.ACCOUNT_ID_SYSTEM, EventTypes.EVENT_ROUTER_STOP, "stopping domain router with Id: "+vmInstance.getId());

        				if(!_networkMgr.stopRouter(vmInstance.getId(), eventId2))
        				{
        					s_logger.warn("There was an error stopping the domain router id: "+vmInstance.getId()+" ,cannot enable primary storage maintenance");
        	            	primaryStorage.setStatus(Status.ErrorInMaintenance);
        	        		_storagePoolDao.persist(primaryStorage);
        					return false;
        				}
           				else if(restart)
        				{
    						//create a dummy event and restart the domr immediately
    						long eventId = saveScheduledEvent(User.UID_SYSTEM, Account.ACCOUNT_ID_SYSTEM, EventTypes.EVENT_PROXY_START, "starting domr with Id: "+vmInstance.getId());
    						if(_networkMgr.startRouter(vmInstance.getId(), eventId)==null)
    						{
    							s_logger.warn("There was an error starting the omr id: "+vmInstance.getId()+" on another storage pool, cannot enable primary storage maintenance");
    			            	primaryStorage.setStatus(Status.ErrorInMaintenance);
    			        		_storagePoolDao.persist(primaryStorage);
    							return false;
    						}
        				}
        			}
        		}	
        	}
        	
        	//5. Update the status
        	primaryStorage.setStatus(Status.Maintenance);
        	_storagePoolDao.persist(primaryStorage);
        	
        } catch (Exception e) {
            s_logger.error("Exception in enabling primary storage maintenance:"+e);
        }
        
        return true;
    }

    private Long saveScheduledEvent(Long userId, Long accountId, String type, String description) 
    {
        EventVO event = new EventVO();
        event.setUserId(userId);
        event.setAccountId(accountId);
        event.setType(type);
        event.setState(EventState.Scheduled);
        event.setDescription("Scheduled async job for "+description);
        event = _eventDao.persist(event);
        return event.getId();
    }

	@Override
	@DB
	public boolean cancelPrimaryStorageForMaintenance(long primaryStorageId,long userId) 
	{
    	//1. Get the primary storage record
    	StoragePoolVO primaryStorage = _storagePoolDao.findById(primaryStorageId);
    	
    	if(primaryStorage == null)
    	{
    		s_logger.warn("The primary storage does not exist");
    		return false;
    	}	
		
       	//2. Get a list of all the volumes within this storage pool
    	List<VolumeVO> allVolumes = _volsDao.findByPoolId(primaryStorageId);

    	//3. If the volume is not removed AND not destroyed, start the vm corresponding to it
    	for(VolumeVO volume: allVolumes)
    	{
    		if((!volume.destroyed) && (volume.removed==null))
    		{
    			VMInstanceVO vmInstance = _vmInstanceDao.findById(volume.getInstanceId());
    		
    			if(vmInstance.getState().equals(State.Stopping) || vmInstance.getState().equals(State.Stopped))
    			{
        			//if the instance is of type consoleproxy, call the console proxy
        			if(vmInstance.getType().equals(VirtualMachine.Type.ConsoleProxy))
        			{
        				
        				//create a dummy event
        				long eventId = saveScheduledEvent(User.UID_SYSTEM, Account.ACCOUNT_ID_SYSTEM, EventTypes.EVENT_PROXY_START, "starting console proxy with Id: "+vmInstance.getId());
        				
        				if(_consoleProxyMgr.startProxy(vmInstance.getId(), eventId)==null)
        				{
        					s_logger.warn("There was an error starting the console proxy id: "+vmInstance.getId()+" on storage pool, cannot complete primary storage maintenance");
        	            	primaryStorage.setStatus(Status.ErrorInMaintenance);
        	        		_storagePoolDao.persist(primaryStorage);
        					return false;
        				}
        			}
        			
        			//if the instance is of type ssvm, call the ssvm manager
        			if(vmInstance.getType().equals(VirtualMachine.Type.SecondaryStorageVm))
        			{
        				
        				//create a dummy event
        				long eventId = saveScheduledEvent(User.UID_SYSTEM, Account.ACCOUNT_ID_SYSTEM, EventTypes.EVENT_SSVM_START, "starting ssvm with Id: "+vmInstance.getId());
        				
        				if(_secStorageMgr.startSecStorageVm(vmInstance.getId(), eventId)==null)
        				{
        					s_logger.warn("There was an error starting the ssvm id: "+vmInstance.getId()+" on storage pool, cannot complete primary storage maintenance");
        	            	primaryStorage.setStatus(Status.ErrorInMaintenance);
        	        		_storagePoolDao.persist(primaryStorage);
        					return false;
        				}
        			}
        			
        			//if the instance is of type user vm, call the user vm manager
        			if(vmInstance.getType().equals(VirtualMachine.Type.User))
        			{
        				
        				//create a dummy event
        				long eventId = saveScheduledEvent(User.UID_SYSTEM, Account.ACCOUNT_ID_SYSTEM, EventTypes.EVENT_VM_START, "starting ssvm with Id: "+vmInstance.getId());
        				
        				try {
							if(_userVmMgr.start(vmInstance.getId(), eventId)==null)
							{
								s_logger.warn("There was an error starting the ssvm id: "+vmInstance.getId()+" on storage pool, cannot complete primary storage maintenance");
	        	            	primaryStorage.setStatus(Status.ErrorInMaintenance);
	        	        		_storagePoolDao.persist(primaryStorage);
	        					return false;
							}
						} catch (StorageUnavailableException e) {
							s_logger.warn("There was an error starting the ssvm id: "+vmInstance.getId()+" on storage pool, cannot complete primary storage maintenance");
							s_logger.warn(e);
        	            	primaryStorage.setStatus(Status.ErrorInMaintenance);
        	        		_storagePoolDao.persist(primaryStorage);
        					return false;
						} catch (InsufficientCapacityException e) {
							s_logger.warn("There was an error starting the ssvm id: "+vmInstance.getId()+" on storage pool, cannot complete primary storage maintenance");
							s_logger.warn(e);
        	            	primaryStorage.setStatus(Status.ErrorInMaintenance);
        	        		_storagePoolDao.persist(primaryStorage);
        					return false;				
						} catch (ConcurrentOperationException e) {
							s_logger.warn("There was an error starting the ssvm id: "+vmInstance.getId()+" on storage pool, cannot complete primary storage maintenance");
							s_logger.warn(e);
        	            	primaryStorage.setStatus(Status.ErrorInMaintenance);
        	        		_storagePoolDao.persist(primaryStorage);
        					return false;
						} catch (ExecutionException e) {
							s_logger.warn("There was an error starting the ssvm id: "+vmInstance.getId()+" on storage pool, cannot complete primary storage maintenance");
							s_logger.warn(e);
        	            	primaryStorage.setStatus(Status.ErrorInMaintenance);
        	        		_storagePoolDao.persist(primaryStorage);
        					return false;
						}
        			}
        			    				
    			}
    		}
    	}
    	
		//Restore config val for consoleproxy.restart to true
		try {
			_configMgr.updateConfiguration(userId, "consoleproxy.restart", "true");
		} catch (InvalidParameterValueException e) {
			s_logger.warn("Error changing consoleproxy.restart back to false at end of cancel maintenance:"+e);
        	primaryStorage.setStatus(Status.ErrorInMaintenance);
    		_storagePoolDao.persist(primaryStorage);
			return false;
		} catch (InternalErrorException e) {
			s_logger.warn("Error changing consoleproxy.restart back to false at end of cancel maintenance:"+e);
        	primaryStorage.setStatus(Status.ErrorInMaintenance);
    		_storagePoolDao.persist(primaryStorage);
			return false;
		}
		
		//Change the storage state back to up
		primaryStorage.setStatus(Status.Up);
		_storagePoolDao.persist(primaryStorage);
		
    	return true;
	}
	
	private boolean sendToVmResidesOn(Command cmd) {
    	if ((_hypervisorType == Hypervisor.Type.KVM) &&
    		((cmd instanceof ManageSnapshotCommand) ||
    		 (cmd instanceof BackupSnapshotCommand))) {
    		return true;
    	} else {
    		return false;
    	}
    }
	
	protected DiskProfile toDiskProfile(VolumeVO vol, DiskOfferingVO offering) {
	    return new DiskProfile(vol.getId(), vol.getVolumeType(), vol.getName(), offering.getId(), vol.getSize(), offering.getTagsArray(), offering.getUseLocalStorage(), offering.isRecreatable(), vol.getTemplateId());
	}
	
    @Override
    public <T extends VMInstanceVO> DiskProfile allocateRawVolume(VolumeType type, String name, DiskOfferingVO offering, Long size, T vm, AccountVO owner) {
        if (size == null) {
            size = offering.getDiskSizeInBytes();
        }
        VolumeVO vol = new VolumeVO(type, name, vm.getDataCenterId(), owner.getDomainId(), owner.getId(), offering.getId(), size);
        if (vm != null) {
            vol.setInstanceId(vm.getId());
        }
        
        vol = _volsDao.persist(vol);
        
        return toDiskProfile(vol, offering);
    }
    
    @Override 
    public <T extends VMInstanceVO> DiskProfile allocateTemplatedVolume(VolumeType type, String name, DiskOfferingVO offering, VMTemplateVO template, T vm, AccountVO owner) {
        assert (template.getFormat() != ImageFormat.ISO) : "ISO is not a template really....";
        
        SearchCriteria<VMTemplateHostVO> sc = HostTemplateStatesSearch.create();
        sc.setParameters("id", template.getId());
        sc.setParameters("state", com.cloud.storage.VMTemplateStorageResourceAssoc.Status.DOWNLOADED);
        sc.setJoinParameters("host", "dcId", vm.getDataCenterId());
        
        List<VMTemplateHostVO> sss = _vmTemplateHostDao.search(sc, null);
        if (sss.size() == 0) {
            throw new CloudRuntimeException("Template " + template.getName() + " has not been completely downloaded to zone " + vm.getDataCenterId());
        }
        VMTemplateHostVO ss = sss.get(0);
        
        VolumeVO vol = new VolumeVO(type, name, vm.getDataCenterId(), owner.getDomainId(), owner.getId(), offering.getId(), ss.getSize());
        if (vm != null) {
            vol.setInstanceId(vm.getId());
        }
        vol.setTemplateId(template.getId());
        
        vol = _volsDao.persist(vol);
        
        return toDiskProfile(vol, offering);
    }
    
    final protected DiskProfile createDiskCharacteristics(VolumeVO volume, DiskOfferingVO offering) {
        return new DiskProfile(volume.getId(), volume.getVolumeType(), volume.getName(), offering.getId(), volume.getSize(), offering.getTagsArray(), offering.getUseLocalStorage(), offering.isRecreatable(), volume.getTemplateId());
    }
    
    final protected DiskProfile createDiskCharacteristics(VolumeVO volume) {
        DiskOfferingVO offering = _diskOfferingDao.findById(volume.getDiskOfferingId());
        return createDiskCharacteristics(volume, offering);
    }
    
    protected StoragePool findStorage(DiskProfile dskCh, DeployDestination dest, VirtualMachineProfile vm, List<? extends Volume> alreadyAllocated, Set<? extends StoragePool> avoid) {
        for (StoragePoolAllocator allocator : _storagePoolAllocators) {
            StoragePool pool = allocator.allocateTo(dskCh, vm, dest, alreadyAllocated, avoid);
            if (pool != null) {
                return pool;
            }
        }
        return null;
    }
    
    @Override
    public VolumeTO[] prepare(VirtualMachineProfile vm, DeployDestination dest) throws StorageUnavailableException, InsufficientStorageCapacityException {
        List<VolumeVO> vols = _volsDao.findUsableVolumesForInstance(vm.getId());
        if (s_logger.isDebugEnabled()) {
            s_logger.debug("Preparing " + vols.size() + " volumes for " + vm);
        }
        
        List<VolumeVO> recreateVols = new ArrayList<VolumeVO>(vols.size());
        VolumeTO[] disks = new VolumeTO[vols.size()];
        
        int i = 0;
        for (VolumeVO vol : vols) {
            Volume.State state = vol.getState();
            if (state == Volume.State.Ready) {
                StoragePoolVO pool = _storagePoolDao.findById(vol.getPoolId());
                if (pool.getRemoved() != null || pool.isInMaintenance()) {
                    if (vol.isRecreatable()) {
                        if (s_logger.isDebugEnabled()) {
                            s_logger.debug("Volume " + vol + " has to be recreated due to storage pool " + pool + " is unavailable");
                        }
                        recreateVols.add(vol);
                    } else {
                        throw new StorageUnavailableException("Volume " + vol + " is not available on the storage pool.", pool);
                    }
                } else {
                    if (s_logger.isDebugEnabled()) {
                        s_logger.debug("Volume " + vol + " is ready.");
                    }
                    disks[i++] = new VolumeTO(vol, pool);
                }
            } else if (state == Volume.State.Allocated) {
                if (s_logger.isDebugEnabled()) {
                    s_logger.debug("Creating volume " + vol + " for the first time.");
                }
                recreateVols.add(vol);
            } else {
                throw new StorageUnavailableException("Volume " + vol + " can not be used", vol);
            }
        }
        
        for (VolumeVO vol : recreateVols) {
            VolumeVO newVol;
            if (vol.getState() == Volume.State.Allocated) {
                newVol = vol;
            } else {
                newVol = switchVolume(vol);
                if (s_logger.isDebugEnabled()) {
                    s_logger.debug("Created new volume " + newVol + " for old volume " + vol);
                }
            }

            try {
                _volsDao.update(newVol, Volume.Event.Create);
            } catch(ConcurrentOperationException e) {
                throw new StorageUnavailableException("Unable to create " + newVol, newVol);
            }
            Pair<VolumeTO, StoragePool> created = createVolume(newVol, _diskOfferingDao.findById(newVol.getDiskOfferingId()), vm, vols, dest);
            if (created == null) {
                newVol.setPoolId(null);
                try {
                    _volsDao.update(newVol, Event.OperationFailed);
                } catch (ConcurrentOperationException e) {
                    throw new CloudRuntimeException("Unable to update the failure on a volume: " + newVol, e);
                }
                throw new StorageUnavailableException("Unable to create " + newVol, newVol);
            }
            newVol.setStatus(AsyncInstanceCreateStatus.Created);
            newVol.setFolder(created.second().getPath());
            newVol.setPath(created.first().getPath());
            newVol.setSize(created.first().getSize());
            newVol.setPoolType(created.second().getPoolType());
            newVol.setPodId(created.second().getPodId());
            try {
                _volsDao.update(newVol, Event.OperationSucceeded);
            } catch (ConcurrentOperationException e) {
                throw new CloudRuntimeException("Unable to update an CREATE operation succeeded on volume " + newVol, e);
            }
            if (s_logger.isDebugEnabled()) {
                s_logger.debug("Volume " + newVol + " is created on " + created.second());
            }
            disks[i++] = created.first();
        }
        
        return disks;
    }
    
    @DB
    protected VolumeVO switchVolume(VolumeVO existingVolume) throws StorageUnavailableException {
        Transaction txn = Transaction.currentTxn();
        try {
            txn.start();
            _volsDao.update(existingVolume, Event.Destroy);
            VolumeVO newVolume = allocateDuplicateVolume(existingVolume);
            txn.commit();
            return newVolume;
        } catch (ConcurrentOperationException e) {
            throw new StorageUnavailableException("Unable to duplicate the volume " + existingVolume, existingVolume, e);
        }
    }
    
    public Pair<VolumeTO, StoragePool> createVolume(VolumeVO toBeCreated, DiskOfferingVO offering, VirtualMachineProfile vm, List<? extends Volume> alreadyCreated, DeployDestination dest) {
        if (s_logger.isDebugEnabled()) {
            s_logger.debug("Creating volume: " + toBeCreated);
        }
        DiskProfile diskProfile = new DiskProfile(toBeCreated, offering);

        Set<StoragePool> avoids = new HashSet<StoragePool>();
        StoragePool pool = null;
        while ((pool = findStorage(diskProfile, dest, vm, alreadyCreated, avoids)) != null) {
            if (s_logger.isDebugEnabled()) {
                s_logger.debug("Trying to create in " + pool);
            }
            avoids.add(pool);
            toBeCreated.setPoolId(pool.getId());
            try {
                _volsDao.update(toBeCreated, Volume.Event.OperationRetry);
            } catch (ConcurrentOperationException e) {
                throw new CloudRuntimeException("Unable to retry a create operation on volume " + toBeCreated);
            }
            CreateCommand cmd = new CreateCommand(diskProfile, new StorageFilerTO(pool), diskProfile.getSize());
            Answer answer = sendToPool(pool, cmd);
            if (answer.getResult()) {
                CreateAnswer createAnswer = (CreateAnswer)answer;
                return new Pair<VolumeTO, StoragePool>(createAnswer.getVolume(), pool);
            }
        }
        
        if (s_logger.isDebugEnabled()) {
            s_logger.debug("Unable to create volume " + toBeCreated);
        }
        return null;
    }
    
    @Override
    public <T extends VMInstanceVO> void create(T vm) {
        List<VolumeVO> vols = _volsDao.findByInstance(vm.getId());
        assert vols.size() >= 1 : "Come on, what's with the zero volumes for " + vm;
        for (VolumeVO vol : vols) {
            DiskProfile dskCh = createDiskCharacteristics(vol);
            int retry = _retry;
            while (--retry >= 0) {
                
            }
        }
        /*
StoragePoolVO pool = null;
final HashSet<StoragePool> avoidPools = new HashSet<StoragePool>(avoids);

VolumeType volType = volume.getVolumeType();

VolumeTO created = null;
int retry = _retry;
while (--retry >= 0) {
created = null;

txn.start();

long podId = pod.getId();
pod = _podDao.lock(podId, true);
if (pod == null) {
txn.rollback();
volume.setStatus(AsyncInstanceCreateStatus.Failed);
volume.setDestroyed(true);
_volsDao.persist(volume);
throw new CloudRuntimeException("Unable to acquire lock on the pod " + podId);
}

pool = findStoragePool(dskCh, dc, pod, clusterId, offering, vm, template, avoidPools);
if (pool == null) {
txn.rollback();
break;
}

avoidPools.add(pool);
if (s_logger.isDebugEnabled()) {
s_logger.debug("Trying to create " + volume + " on " + pool);
}

volume.setPoolId(pool.getId());
_volsDao.persist(volume);

txn.commit();

CreateCommand cmd = null;
VMTemplateStoragePoolVO tmpltStoredOn = null;
if (volume.getVolumeType() == VolumeType.ROOT && Storage.ImageFormat.ISO != template.getFormat()) {
tmpltStoredOn = _tmpltMgr.prepareTemplateForCreate(template, pool);
if (tmpltStoredOn == null) {
continue;
}
cmd = new CreateCommand(volume, vm, dskCh, tmpltStoredOn.getLocalDownloadPath(), pool);
} else {
cmd = new CreateCommand(volume, vm, dskCh, pool, size);
}

Answer answer = sendToPool(pool, cmd);
if (answer != null && answer.getResult()) {
created = ((CreateAnswer)answer).getVolume();
break;
}

volume.setPoolId(null);
_volsDao.persist(volume);

s_logger.debug("Retrying the create because it failed on pool " + pool);
}

if (created == null) {
if (s_logger.isDebugEnabled()) {
s_logger.debug("Unable to create a volume for " + volume);
}
volume.setStatus(AsyncInstanceCreateStatus.Failed);
volume.setDestroyed(true);
_volsDao.persist(volume);

return null;
}

volume.setStatus(AsyncInstanceCreateStatus.Created);
volume.setFolder(pool.getPath());
volume.setPath(created.getPath());
volume.setSize(created.getSize());
volume.setPoolType(pool.getPoolType());
volume.setPodId(pod.getId());
_volsDao.persist(volume);
return volume;
    */
        
    }
}
