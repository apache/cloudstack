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
import com.cloud.agent.api.Command;
import com.cloud.agent.api.CreateVolumeFromSnapshotAnswer;
import com.cloud.agent.api.CreateVolumeFromSnapshotCommand;
import com.cloud.agent.api.DeleteStoragePoolCommand;
import com.cloud.agent.api.ModifyStoragePoolAnswer;
import com.cloud.agent.api.ModifyStoragePoolCommand;
import com.cloud.agent.api.storage.CopyVolumeAnswer;
import com.cloud.agent.api.storage.CopyVolumeCommand;
import com.cloud.agent.api.storage.CreateAnswer;
import com.cloud.agent.api.storage.CreateCommand;
import com.cloud.agent.api.storage.DeleteTemplateCommand;
import com.cloud.agent.api.storage.DestroyCommand;
import com.cloud.agent.api.to.DiskCharacteristicsTO;
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
import com.cloud.dc.DataCenterVO;
import com.cloud.dc.HostPodVO;
import com.cloud.dc.dao.DataCenterDao;
import com.cloud.dc.dao.HostPodDao;
import com.cloud.event.EventState;
import com.cloud.event.EventTypes;
import com.cloud.event.EventVO;
import com.cloud.event.dao.EventDao;
import com.cloud.exception.AgentUnavailableException;
import com.cloud.exception.DiscoveryException;
import com.cloud.exception.InternalErrorException;
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
import com.cloud.service.ServiceOffering;
import com.cloud.service.ServiceOfferingVO;
import com.cloud.service.dao.ServiceOfferingDao;
import com.cloud.storage.Storage.ImageFormat;
import com.cloud.storage.Storage.StoragePoolType;
import com.cloud.storage.Volume.MirrorState;
import com.cloud.storage.Volume.StorageResourceType;
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
import com.cloud.storage.snapshot.SnapshotManager;
import com.cloud.storage.snapshot.SnapshotScheduler;
import com.cloud.template.TemplateManager;
import com.cloud.user.Account;
import com.cloud.user.AccountManager;
import com.cloud.user.dao.AccountDao;
import com.cloud.utils.NumbersUtil;
import com.cloud.utils.Pair;
import com.cloud.utils.component.Adapters;
import com.cloud.utils.component.ComponentLocator;
import com.cloud.utils.component.Inject;
import com.cloud.utils.concurrency.NamedThreadFactory;
import com.cloud.utils.db.DB;
import com.cloud.utils.db.GlobalLock;
import com.cloud.utils.db.SearchBuilder;
import com.cloud.utils.db.SearchCriteria;
import com.cloud.utils.db.Transaction;
import com.cloud.utils.exception.CloudRuntimeException;
import com.cloud.utils.exception.ExecutionException;
import com.cloud.vm.State;
import com.cloud.vm.UserVm;
import com.cloud.vm.UserVmVO;
import com.cloud.vm.VMInstanceVO;
import com.cloud.vm.dao.UserVmDao;
import com.cloud.vm.dao.VMInstanceDao;

@Local(value = { StorageManager.class })
public class StorageManagerImpl implements StorageManager {
    private static final Logger s_logger = Logger.getLogger(StorageManagerImpl.class);

    protected String _name;
    @Inject protected AgentManager _agentMgr;
    @Inject protected TemplateManager _tmpltMgr;
    @Inject protected AsyncJobManager _asyncMgr;
    @Inject protected SnapshotManager _snapshotMgr;
    @Inject protected SnapshotScheduler _snapshotScheduler;
    @Inject protected AccountManager _accountMgr;
    @Inject protected ConfigurationManager _configMgr;
    @Inject protected VolumeDao _volsDao;
    @Inject protected HostDao _hostDao;
    @Inject protected DetailsDao _detailsDao;
    @Inject protected SnapshotDao _snapshotDao;
    protected Adapters<StoragePoolAllocator> _storagePoolAllocators;
    protected Adapters<StoragePoolDiscoverer> _discoverers;
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
    private String _hypervisorType;
    
    @Override
    public boolean share(VMInstanceVO vm, List<VolumeVO> vols, HostVO host, boolean cancelPreviousShare) {
        return true;
    }
    
    @Override
    public List<VolumeVO> prepare(VMInstanceVO vm, HostVO host) {
        List<VolumeVO> vols = _volsDao.findCreatedByInstance(vm.getId());
        List<VolumeVO> recreateVols = new ArrayList<VolumeVO>(vols.size());
        for (VolumeVO vol : vols) {
            if (!vol.isRecreatable()) {
                return vols;
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
            if (vol.getDiskOfferingId() != null) {
                diskOffering = _diskOfferingDao.findById(vol.getDiskOfferingId());
            }
            ServiceOfferingVO offering;
            if (vm instanceof UserVmVO) {
                offering = _offeringDao.findById(((UserVmVO)vm).getServiceOfferingId());
            } else {
                offering = _offeringDao.findById(vol.getDiskOfferingId());
            }
            VolumeVO created = createVolume(create, vm, template, dc, pod, host.getClusterId(), offering, diskOffering, new ArrayList<StoragePoolVO>());
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
	        	volumeSB.join("activeVmSB", activeVmSB, volumeSB.entity().getInstanceId(), activeVmSB.entity().getId());
    			
	        	SearchCriteria volumeSC = volumeSB.create();
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

    protected StoragePoolVO findStoragePool(DiskCharacteristicsTO dskCh, final DataCenterVO dc, HostPodVO pod, Long clusterId, final ServiceOffering offering, final VMInstanceVO vm, final VMTemplateVO template, final Set<StoragePool> avoid) {
        Enumeration<StoragePoolAllocator> en = _storagePoolAllocators.enumeration();
        while (en.hasMoreElements()) {
            final StoragePoolAllocator allocator = en.nextElement();
            final StoragePool pool = allocator.allocateToPool(dskCh, offering, dc, pod, clusterId, vm, template, avoid);
            if (pool != null) {
                return (StoragePoolVO) pool;
            }
        }
        return null;
    }

    protected Long findHostIdForStoragePool(StoragePoolVO pool) {
        List<StoragePoolHostVO> poolHosts = _poolHostDao.listByHostStatus(pool.getId(), Status.Up);

        if (poolHosts.size() == 0) {
            return null;
        } else {
            return poolHosts.get(0).getHostId();
        }
    }

    @Override
    public Answer[] sendToPool(StoragePoolVO pool, Command[] cmds, boolean stopOnError) {
        List<StoragePoolHostVO> poolHosts = _poolHostDao.listByHostStatus(pool.getId(), Status.Up);
        Collections.shuffle(poolHosts);
        
        for (StoragePoolHostVO poolHost: poolHosts) {
            try {
                return _agentMgr.send(poolHost.getHostId(), cmds, stopOnError);
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
    public Answer sendToPool(StoragePoolVO pool, Command cmd) {
        Command[] cmds = new Command[]{cmd};
        Answer[] answers = sendToPool(pool, cmds, true);
        if (answers == null) {
            return null;
        }
        return answers[0];
    }
    
    protected DiskCharacteristicsTO createDiskCharacteristics(VolumeVO volume, VMTemplateVO template, DataCenterVO dc, DiskOfferingVO diskOffering) {
        if (volume.getVolumeType() == VolumeType.ROOT && Storage.ImageFormat.ISO != template.getFormat()) {
            SearchCriteria sc = HostTemplateStatesSearch.create();
            sc.setParameters("id", template.getId());
            sc.setParameters("state", com.cloud.storage.VMTemplateStorageResourceAssoc.Status.DOWNLOADED);
            sc.setJoinParameters("host", "dcId", dc.getId());
        
            List<VMTemplateHostVO> sss = _vmTemplateHostDao.search(sc, null);
            if (sss.size() == 0) {
                throw new CloudRuntimeException("Template " + template.getName() + " has not been completely downloaded to zone " + dc.getId());
            }
            VMTemplateHostVO ss = sss.get(0);
        
            return new DiskCharacteristicsTO(volume.getVolumeType(), volume.getName(), diskOffering, ss.getSize());
        } else {
            return new DiskCharacteristicsTO(volume.getVolumeType(), volume.getName(), diskOffering);
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
    protected Pair<VolumeVO, String> createVolumeFromSnapshot(long userId, long accountId, String userSpecifiedName, DataCenterVO dc, DiskOfferingVO diskOffering, SnapshotVO snapshot, String templatePath, Long originalVolumeSize, VMTemplateVO template) {
        
        VolumeVO createdVolume = null;
        Long volumeId = null;
        
        String volumeFolder = null;
        
        // Create the Volume object and save it so that we can return it to the user
        Account account = _accountDao.findById(accountId);
        VolumeVO volume = new VolumeVO(null, userSpecifiedName, -1, -1, -1, -1, new Long(-1), null, null, 0, Volume.VolumeType.DATADISK);
        volume.setPoolId(null);
        volume.setDataCenterId(dc.getId());
        volume.setPodId(null);
        volume.setAccountId(accountId);
        volume.setDomainId(account.getDomainId().longValue());
        volume.setMirrorState(MirrorState.NOT_MIRRORED);
        if (diskOffering != null) {
            volume.setDiskOfferingId(diskOffering.getId());
        }
        volume.setSize(originalVolumeSize);
        volume.setStorageResourceType(StorageResourceType.STORAGE_POOL);
        volume.setInstanceId(null);
        volume.setUpdated(new Date());
        volume.setStatus(AsyncInstanceCreateStatus.Creating);
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
        

        StoragePoolVO pool = null;
        boolean success = false;
        Set<Long> podsToAvoid = new HashSet<Long>();
        Pair<HostPodVO, Long> pod = null;
        String volumeUUID = null;
        String details = null;
        
        DiskCharacteristicsTO dskCh = createDiskCharacteristics(volume, template, dc, diskOffering);
        
        int retry = 0;
        // Determine what pod to store the volume in
        while ((pod = _agentMgr.findPod(null, null, dc, account.getId(), podsToAvoid)) != null) {
            podsToAvoid.add(pod.first().getId());
            // Determine what storage pool to store the volume in
            HashSet<StoragePool> poolsToAvoid = new HashSet<StoragePool>();
            while ((pool = findStoragePool(dskCh, dc, pod.first(), null, null, null, null, poolsToAvoid)) != null) {
                poolsToAvoid.add(pool);
                volumeFolder = pool.getPath();
                if (s_logger.isDebugEnabled()) {
                    s_logger.debug("Attempting to create volume from snapshotId: " + snapshot.getId() + " on storage pool " + pool.getName());
                }
                
                // Get the newly created VDI from the snapshot.
                // This will return a null volumePath if it could not be created
                Pair<String, String> volumeDetails = createVDIFromSnapshot(userId, snapshot, pool, templatePath);
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
                		_volsDao.delete(volumeId);
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
    		_volsDao.delete(volumeId);
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
            createdVolume.setDomainId(account.getDomainId().longValue());
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
        String templatePath = null;
        VMTemplateVO template = null;
        if(originalVolume.getVolumeType().equals(Volume.VolumeType.ROOT)){

            ImageFormat format =  _snapshotMgr.getImageFormat(origVolumeId);
            if (format != null && format != ImageFormat.ISO) {           	
                Long templateId = originalVolume.getTemplateId();
                template = _templateDao.findById(templateId);
                if(template == null) {
                    details = "Unable find template id: " + templateId + " to create volume from root disk";
                    s_logger.error(details);
                }
                else if (template.getFormat() != ImageFormat.ISO) {
                    // For ISOs there is no base template VHD file. The root disk itself is the base template.
                    // Creating a volume from an ISO Root Disk is the same as creating a volume for a Data Disk.
                    
                    // Absolute crappy way of getting the template path on secondary storage.
                    // Why is the secondary storage a host? It's just an NFS mount point. Why do we need to look into the templateHostVO?
                    HostVO secondaryStorageHost = getSecondaryStorageHost(originalVolume.getDataCenterId());
                    VMTemplateHostVO templateHostVO = _templateHostDao.findByHostTemplate(secondaryStorageHost.getId(), templateId);
                    if (templateHostVO == null ||
                        templateHostVO.getDownloadState() != VMTemplateStorageResourceAssoc.Status.DOWNLOADED ||
                        (templatePath = templateHostVO.getInstallPath()) == null)
                    {
                        details = "Template id: " + templateId + " is not present on secondaryStorageHost Id: " + secondaryStorageHost.getId() + ". Can't create volume from ROOT DISK";
                        s_logger.warn(details);
                        throw new CloudRuntimeException(details);
                    }
                }
            }
		}
		// everything went well till now
		DataCenterVO dc = _dcDao.findById(originalVolume.getDataCenterId());
		DiskOfferingVO diskOffering = null;

		if (originalVolume.getVolumeType() == VolumeType.DATADISK
				|| originalVolume.getVolumeType() == VolumeType.ROOT) {
			Long diskOfferingId = originalVolume.getDiskOfferingId();
			if (diskOfferingId != null) {
				diskOffering = _diskOfferingDao.findById(diskOfferingId);
			}
		} else {
			// The code never reaches here.
			s_logger
					.error("Original volume must have been a ROOT DISK or a DATA DISK");
			return null;
		}
		Pair<VolumeVO, String> volumeDetails = createVolumeFromSnapshot(userId,
				accountId, volumeName, dc, diskOffering, snapshot,
				templatePath, originalVolume.getSize(), template);
		createdVolume = volumeDetails.first();
		if (createdVolume != null) {
			volumeId = createdVolume.getId();
		} else {
			details = "Creating volume failed due to " + volumeDetails.second();
			s_logger.warn(details);
			throw new CloudRuntimeException(details);
		}

        Transaction txn = Transaction.currentTxn();
        txn.start();
        // Create an event
        long templateId = -1;
        long diskOfferingId = -1;
        if(originalVolume.getTemplateId() != null){
            templateId = originalVolume.getTemplateId();
        }
        if(originalVolume.getDiskOfferingId() != null){
            diskOfferingId = originalVolume.getDiskOfferingId();
        }
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
    
    protected Pair<String, String> createVDIFromSnapshot(long userId, SnapshotVO snapshot, StoragePoolVO pool, String templatePath) {
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
                                                templatePath);
        
        String basicErrMsg = "Failed to create volume from " + snapshot.getName() + " for volume: " + volume.getId();
        CreateVolumeFromSnapshotAnswer answer = (CreateVolumeFromSnapshotAnswer) sendToHostsOnStoragePool(pool.getId(),
                                                                                                                      createVolumeFromSnapshotCommand,
                                                                                                                      basicErrMsg,
                                                                                                                      _totalRetries,
                                                                                                                      _pauseInterval,
                                                                                                                      _shouldBeSnapshotCapable);
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
                                    ServiceOfferingVO offering, DiskOfferingVO diskOffering, List<StoragePoolVO> avoids) {
        StoragePoolVO pool = null;
        final HashSet<StoragePool> avoidPools = new HashSet<StoragePool>(avoids);
       
        DiskCharacteristicsTO dskCh = null;
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
                cmd = new CreateCommand(volume, vm, dskCh, pool);
            }
            
            Answer answer = sendToPool(pool, cmd);
            if (answer != null && answer.getResult()) {
                created = ((CreateAnswer)answer).getVolume();
                break;
            }
            
            volume.setPoolId(pool.getId());
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
    public List<VolumeVO> create(Account account, VMInstanceVO vm, VMTemplateVO template, DataCenterVO dc, HostPodVO pod, ServiceOfferingVO offering, DiskOfferingVO diskOffering) throws StorageUnavailableException, ExecutionException {
        List<StoragePoolVO> avoids = new ArrayList<StoragePoolVO>();
        return create(account, vm, template, dc, pod, offering, diskOffering, avoids);
    }
    
    @DB
    protected List<VolumeVO> create(Account account, VMInstanceVO vm, VMTemplateVO template, DataCenterVO dc, HostPodVO pod,
            ServiceOfferingVO offering, DiskOfferingVO diskOffering, List<StoragePoolVO> avoids) {
        ArrayList<VolumeVO> vols = new ArrayList<VolumeVO>(2);
        VolumeVO dataVol = null;
        VolumeVO rootVol = null;
        Transaction txn = Transaction.currentTxn();
        txn.start();
        if (Storage.ImageFormat.ISO == template.getFormat()) {
            rootVol = new VolumeVO(VolumeType.ROOT, vm.getId(), vm.getInstanceName() + "-ROOT", dc.getId(), pod.getId(), account.getId(), account.getDomainId(), diskOffering.getDiskSizeInBytes());
            rootVol.setDiskOfferingId(diskOffering.getId());
            rootVol.setDeviceId(0l);
            rootVol = _volsDao.persist(rootVol);
        } else {
            rootVol = new VolumeVO(VolumeType.ROOT, vm.getId(), template.getId(), vm.getInstanceName() + "-ROOT", dc.getId(), pod.getId(), account.getId(), account.getDomainId(), offering.isRecreatable());
            rootVol.setDiskOfferingId(offering.getId());
            rootVol.setTemplateId(template.getId());
            rootVol.setDeviceId(0l);
            rootVol = _volsDao.persist(rootVol);
            
            if (diskOffering != null && diskOffering.getDiskSizeInBytes() > 0) {
                dataVol = new VolumeVO(VolumeType.DATADISK, vm.getId(), vm.getInstanceName() + "-DATA", dc.getId(), pod.getId(), account.getId(), account.getDomainId(), diskOffering.getDiskSizeInBytes());
                dataVol.setDiskOfferingId(diskOffering.getId());
                dataVol.setDeviceId(1l);
                dataVol = _volsDao.persist(dataVol);
            }
        }
        txn.commit();

        VolumeVO dataCreated = null;
        VolumeVO rootCreated = null;
        try {
            rootCreated = createVolume(rootVol, vm, template, dc, pod, null, offering, diskOffering, avoids);
            if (rootCreated == null) {
                throw new CloudRuntimeException("Unable to create " + rootVol);
            }
            
            vols.add(rootCreated);
            
            if (dataVol != null) {
                StoragePoolVO pool = _storagePoolDao.findById(rootCreated.getPoolId());
                dataCreated = createVolume(dataVol, vm, null, dc, pod, pool.getClusterId(), offering, diskOffering, avoids);
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
            return null;
        }
    }
    

    @Override
    public long createUserVM(Account account, VMInstanceVO vm, VMTemplateVO template, DataCenterVO dc, HostPodVO pod, ServiceOfferingVO offering, DiskOfferingVO diskOffering,
            List<StoragePoolVO> avoids) {
        List<VolumeVO> volumes = create(account, vm, template, dc, pod, offering, diskOffering, avoids);
        if( volumes == null || volumes.size() == 0) {
            throw new CloudRuntimeException("Unable to create volume for " + vm.getName());
        }
        
        for (VolumeVO v : volumes) {
        	        	      	
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

    public StoragePoolHostVO chooseHostForStoragePool(StoragePoolVO poolVO, List<Long> avoidHosts) {
        List<StoragePoolHostVO> poolHosts = _poolHostDao.listByHostStatus(poolVO.getId(), Status.Up);
        Collections.shuffle(poolHosts);
        if (poolHosts != null && poolHosts.size() > 0) {
            for (StoragePoolHostVO sphvo : poolHosts) {
                if (!avoidHosts.contains(sphvo.getHostId())) {
                    return sphvo;
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

        _storagePoolAllocators = locator.getAdapters(StoragePoolAllocator.class);
        if (!_storagePoolAllocators.isSet()) {
            throw new ConfigurationException("Unable to get any storage pool allocators.");
        }
        
        _discoverers = locator.getAdapters(StoragePoolDiscoverer.class);

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

        _totalRetries = NumbersUtil.parseInt(configDao.getValue("total.retries"), 2);
        _pauseInterval = 2*NumbersUtil.parseInt(configDao.getValue("ping.interval"), 60);
        
        _hypervisorType = configDao.getValue("hypervisor.type");
        
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
        PoolsUsedByVmSearch.join("volumes", volSearch, volSearch.entity().getPoolId(), PoolsUsedByVmSearch.entity().getId());
        volSearch.and("vm", volSearch.entity().getInstanceId(), SearchCriteria.Op.EQ);
        volSearch.done();
        PoolsUsedByVmSearch.done();
        
        HostTemplateStatesSearch = _vmTemplateHostDao.createSearchBuilder();
        HostTemplateStatesSearch.and("id", HostTemplateStatesSearch.entity().getTemplateId(), SearchCriteria.Op.EQ);
        HostTemplateStatesSearch.and("state", HostTemplateStatesSearch.entity().getDownloadState(), SearchCriteria.Op.EQ);
        
        SearchBuilder<HostVO> HostSearch = _hostDao.createSearchBuilder();
        HostSearch.and("dcId", HostSearch.entity().getDataCenterId(), SearchCriteria.Op.EQ);
        
        HostTemplateStatesSearch.join("host", HostSearch, HostSearch.entity().getId(), HostTemplateStatesSearch.entity().getHostId());
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
    public String getAbsoluteIsoPath(long templateId, long dataCenterId) {
        String isoPath = null;

        List<HostVO> storageHosts = _hostDao.listBy(Host.Type.SecondaryStorage, dataCenterId);
        if (storageHosts != null) {
            for (HostVO storageHost : storageHosts) {
                VMTemplateHostVO templateHostVO = _vmTemplateHostDao.findByHostTemplate(storageHost.getId(), templateId);
                if (templateHostVO != null) {
                    isoPath = storageHost.getStorageUrl() + "/" + templateHostVO.getInstallPath();
                    break;
                }
            }
        }

        return isoPath;
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
        	if (_hypervisorType.equalsIgnoreCase("KVM")) {
        		hypervisorType = Hypervisor.Type.KVM;
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
        if (allHosts.isEmpty() && !_hypervisorType.equalsIgnoreCase("KVM")) {
            throw new ResourceAllocationException("No host exists to associate a storage pool with");
        }
        long poolId = _storagePoolDao.getNextInSequence(Long.class, "id");
        String uuid = UUID.nameUUIDFromBytes(new String(storageHost + hostPath).getBytes()).toString();
        s_logger.debug("In createPool Setting poolId - " +poolId+ " uuid - " +uuid+ " zoneId - " +zoneId+ " podId - " +podId+ " poolName - " +poolName);
        pool.setId(poolId);
        pool.setUuid(uuid);
        pool.setDataCenterId(zoneId);
        pool.setPodId(podId);
        pool.setName(poolName);
        pool.setClusterId(clusterId);
        pool = _storagePoolDao.persist(pool, details);
        if (_hypervisorType.equalsIgnoreCase("KVM") && allHosts.isEmpty()) {
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
            _storagePoolDao.delete(pool.getId());
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
                    poolHost = new StoragePoolHostVO(pool.getId(), hostId, mspAnswer.getPoolInfo().getLocalPath());
                    _poolHostDao.persist(poolHost);
                } else {
                    poolHost.setLocalPath(mspAnswer.getPoolInfo().getLocalPath());
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
    	DiskCharacteristicsTO dskCh = new DiskCharacteristicsTO(volume.getVolumeType(), volume.getName(), diskOffering);
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
    public VolumeVO createVolume(long accountId, long userId, String userSpecifiedName, DataCenterVO dc, DiskOfferingVO diskOffering, long startEventId) {
        // Determine the volume's name
        String volumeName = getRandomVolumeName();

        // Create the Volume object and save it so that we can return it to the user
        Account account = _accountDao.findById(accountId);
        VolumeVO volume = new VolumeVO(null, userSpecifiedName, -1, -1, -1, -1, new Long(-1), null, null, 0, Volume.VolumeType.DATADISK);
        volume.setPoolId(null);
        volume.setDataCenterId(dc.getId());
        volume.setPodId(null);
        volume.setAccountId(accountId);
        volume.setDomainId(account.getDomainId().longValue());
        volume.setMirrorState(MirrorState.NOT_MIRRORED);
        volume.setDiskOfferingId(diskOffering.getId());
        volume.setStorageResourceType(StorageResourceType.STORAGE_POOL);
        volume.setInstanceId(null);
        volume.setUpdated(new Date());
        volume.setStatus(AsyncInstanceCreateStatus.Creating);
        volume.setDomainId(account.getDomainId().longValue());
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
        VolumeVO createdVolume = null;

        while ((pod = _agentMgr.findPod(null, null, dc, account.getId(), podsToAvoid)) != null) {
            podsToAvoid.add(pod.first().getId());
            if ((createdVolume = createVolume(volume, null, null, dc, pod.first(), null, null, diskOffering, poolsToAvoid)) != null) {
            	break;
            } 
        }

        // Create an event
        EventVO event = new EventVO();
        event.setAccountId(accountId);
        event.setUserId(userId);
        event.setType(EventTypes.EVENT_VOLUME_CREATE);
        event.setStartId(startEventId);

        Transaction txn = Transaction.currentTxn();
        try {
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
            } else {
            	// Mark the existing volume record as corrupted
                volume.setStatus(AsyncInstanceCreateStatus.Corrupted);
                volume.setDestroyed(true);
                _volsDao.update(volume.getId(), volume);
                
                // Set event parameters
                event.setLevel(EventVO.LEVEL_ERROR);
                event.setDescription("Failed to create volume with size: " + diskOffering.getDiskSizeInBytes() / (1024 * 1024) + " mb");
            }
            
            _eventDao.persist(event);
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
        return sendToHostsOnStoragePool(poolId, cmd, basicErrMsg, 1, 0, false);
    }

    @Override
    public Answer sendToHostsOnStoragePool(Long poolId, Command cmd, String basicErrMsg, int totalRetries, int pauseBeforeRetry, boolean shouldBeSnapshotCapable) {
        Answer answer = null;
        Long hostId = null;
        StoragePoolVO storagePool = _storagePoolDao.findById(poolId);
        List<Long> hostsToAvoid = new ArrayList<Long>();
        StoragePoolHostVO storagePoolHost;
        int tryCount = 0;
        if (chooseHostForStoragePool(storagePool, hostsToAvoid) == null) {
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
        while ((storagePoolHost = chooseHostForStoragePool(storagePool, hostsToAvoid)) != null && tryCount++ < totalRetries) {
            String errMsg = basicErrMsg + " on host: " + hostId + " try: " + tryCount + ", reason: ";
            hostsToAvoid.add(hostId);
            try {
                hostId = storagePoolHost.getHostId();
                HostVO hostVO = _hostDao.findById(hostId);
                if (shouldBeSnapshotCapable) {
                    if (hostVO == null || hostVO.getHypervisorType() != Hypervisor.Type.XenServer) {
                        // Only XenServer hosts are snapshot capable.
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
        List<StoragePoolVO> storagePools = _storagePoolDao.listAllActive();
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
        SearchCriteria sc = PoolsUsedByVmSearch.create();
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
}
