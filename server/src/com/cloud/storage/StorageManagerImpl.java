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
import java.net.URISyntaxException;
import java.net.UnknownHostException;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.Date;
import java.util.Enumeration;
import java.util.Formatter;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Iterator;
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
import com.cloud.agent.api.to.DiskCharacteristicsTO;
import com.cloud.agent.api.to.VolumeTO;
import com.cloud.agent.manager.AgentManager;
import com.cloud.alert.AlertManager;
import com.cloud.api.BaseCmd;
import com.cloud.api.ServerApiException;
import com.cloud.api.commands.CancelPrimaryStorageMaintenanceCmd;
import com.cloud.api.commands.CreateStoragePoolCmd;
import com.cloud.api.commands.CreateVolumeCmd;
import com.cloud.api.commands.DeletePoolCmd;
import com.cloud.api.commands.DeleteVolumeCmd;
import com.cloud.api.commands.PreparePrimaryStorageForMaintenanceCmd;
import com.cloud.api.commands.UpdateStoragePoolCmd;
import com.cloud.async.AsyncInstanceCreateStatus;
import com.cloud.async.AsyncJobManager;
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
import com.cloud.domain.dao.DomainDao;
import com.cloud.event.EventState;
import com.cloud.event.EventTypes;
import com.cloud.event.EventVO;
import com.cloud.event.dao.EventDao;
import com.cloud.exception.AgentUnavailableException;
import com.cloud.exception.ConcurrentOperationException;
import com.cloud.exception.DiscoveryException;
import com.cloud.exception.InsufficientCapacityException;
import com.cloud.exception.InternalErrorException;
import com.cloud.exception.InvalidParameterValueException;
import com.cloud.exception.OperationTimedoutException;
import com.cloud.exception.PermissionDeniedException;
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
import com.cloud.storage.secondary.SecondaryStorageVmManager;
import com.cloud.storage.snapshot.SnapshotManager;
import com.cloud.storage.snapshot.SnapshotScheduler;
import com.cloud.template.TemplateManager;
import com.cloud.user.Account;
import com.cloud.user.AccountManager;
import com.cloud.user.AccountVO;
import com.cloud.user.User;
import com.cloud.user.UserContext;
import com.cloud.user.dao.AccountDao;
import com.cloud.uservm.UserVm;
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
import com.cloud.vm.UserVmManager;
import com.cloud.vm.UserVmVO;
import com.cloud.vm.VMInstanceVO;
import com.cloud.vm.VirtualMachine;
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
    @Inject protected DomainDao _domainDao;
    
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
    private int _maxVolumeSizeInGb;

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
	        	volumeSB.join("activeVmSB", activeVmSB, volumeSB.entity().getInstanceId(), activeVmSB.entity().getId());
    			
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
    
    private DiskCharacteristicsTO createDiskCharacteristics(VolumeVO volume, VMTemplateVO template, long dataCenterId, DiskOfferingVO diskOffering) {
        if (volume.getVolumeType() == VolumeType.ROOT && Storage.ImageFormat.ISO != template.getFormat()) {
            SearchCriteria<VMTemplateHostVO> sc = HostTemplateStatesSearch.create();
            sc.setParameters("id", template.getId());
            sc.setParameters("state", com.cloud.storage.VMTemplateStorageResourceAssoc.Status.DOWNLOADED);
            sc.setJoinParameters("host", "dcId", dataCenterId);
        
            List<VMTemplateHostVO> sss = _vmTemplateHostDao.search(sc, null);
            if (sss.size() == 0) {
                throw new CloudRuntimeException("Template " + template.getName() + " has not been completely downloaded to zone " + dataCenterId);
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
    private Pair<VolumeVO, String> createVolumeFromSnapshot(VolumeVO volume, SnapshotVO snapshot, String templatePath, Long originalVolumeSize, VMTemplateVO template) {
        VolumeVO createdVolume = null;
        Long volumeId = null;
        
        String volumeFolder = null;
        
        // Create the Volume object and save it so that we can return it to the user
        Account account = _accountDao.findById(volume.getAccountId());
        
        final HashSet<StoragePool> poolsToAvoid = new HashSet<StoragePool>();
        StoragePoolVO pool = null;
        boolean success = false;
        Set<Long> podsToAvoid = new HashSet<Long>();
        Pair<HostPodVO, Long> pod = null;
        String volumeUUID = null;
        String details = null;
        DiskOfferingVO diskOffering = _diskOfferingDao.findById(volume.getDiskOfferingId());
        DataCenterVO dc = _dcDao.findById(volume.getDataCenterId());
        DiskCharacteristicsTO dskCh = createDiskCharacteristics(volume, template, dc.getId(), diskOffering);

        // Determine what pod to store the volume in
        while ((pod = _agentMgr.findPod(null, null, dc, account.getId(), podsToAvoid)) != null) {
            // Determine what storage pool to store the volume in
            while ((pool = findStoragePool(dskCh, dc, pod.first(), null, null, null, null, poolsToAvoid)) != null) {
                volumeFolder = pool.getPath();
                if (s_logger.isDebugEnabled()) {
                    s_logger.debug("Attempting to create volume from snapshotId: " + snapshot.getId() + " on storage pool " + pool.getName());
                }

                // Get the newly created VDI from the snapshot.
                // This will return a null volumePath if it could not be created
                Pair<String, String> volumeDetails = createVDIFromSnapshot(UserContext.current().getUserId(), snapshot, pool, templatePath);
                volumeUUID = volumeDetails.first();
                details = volumeDetails.second();

                if (volumeUUID != null) {
                    if (s_logger.isDebugEnabled()) {
                        s_logger.debug("Volume with UUID " + volumeUUID + " was created on storage pool " + pool.getName());
                    }
                    success = true;
                    break; // break out of the "find storage pool" loop
                }

                s_logger.warn("Unable to create volume on pool " + pool.getName() + ", reason: " + details);
            }
            
            if (success) {
                break; // break out of the "find pod" loop
            } else {
                podsToAvoid.add(pod.first().getId());
            }
        }
        
        // Update the volume in the database
        Transaction txn = Transaction.currentTxn();
        txn.start();
        createdVolume = _volsDao.findById(volumeId);
        
        if (success) {
            // Increment the number of volumes
            _accountMgr.incrementResourceCount(account.getId(), ResourceType.volume);
            
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

    private VolumeVO createVolumeFromSnapshot(VolumeVO volume, long snapshotId/*, long startEventId*/) {
        // FIXME:  start event id needs to come from somewhere
        EventVO event = new EventVO();
        event.setUserId(UserContext.current().getUserId());
        event.setAccountId(volume.getAccountId());
        event.setType(EventTypes.EVENT_VOLUME_CREATE);
        event.setState(EventState.Started);
// FIXME:        event.setStartId(startEventId);
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
            if(originalVolume.getTemplateId() == null){
                details = "Null Template Id for Root Volume Id: " + origVolumeId + ". Cannot create volume from snapshot of root disk.";
                s_logger.error(details);
            }
            else {
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
                    }
                }
            }
        }
        if (details == null) {
            // everything went well till now
            DataCenterVO dc = _dcDao.findById(originalVolume.getDataCenterId());
            DiskOfferingVO diskOffering = null;

            if (originalVolume.getVolumeType() == VolumeType.DATADISK || originalVolume.getVolumeType() == VolumeType.ROOT) {
                Long diskOfferingId = originalVolume.getDiskOfferingId();
                if (diskOfferingId != null) {
                    diskOffering = _diskOfferingDao.findById(diskOfferingId);
                }
            }
//            else if (originalVolume.getVolumeType() == VolumeType.ROOT) {
//                // Create a temporary disk offering with the same size as the ROOT DISK
//                Long rootDiskSize = originalVolume.getSize();
//                Long rootDiskSizeInMB = rootDiskSize/(1024*1024);
//                Long sizeInGB = rootDiskSizeInMB/1024;
//                String name = "Root Disk Offering";
//                String displayText = "Temporary Disk Offering for Snapshot from Root Disk: " + originalVolume.getId() + "[" + sizeInGB + "GB Disk]";
//                diskOffering = new DiskOfferingVO(originalVolume.getDomainId(), name, displayText, rootDiskSizeInMB, false, null);
//            }
            else {
                // The code never reaches here.
                s_logger.error("Original volume must have been a ROOT DISK or a DATA DISK");
                return null;
            }
            Pair<VolumeVO, String> volumeDetails = createVolumeFromSnapshot(volume, snapshot, templatePath, originalVolume.getSize(), template);
            createdVolume = volumeDetails.first();
            if (createdVolume != null) {
                volumeId = createdVolume.getId();
            }
            details = volumeDetails.second();
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
        event.setAccountId(volume.getAccountId());
        event.setUserId(UserContext.current().getUserId());
        event.setType(EventTypes.EVENT_VOLUME_CREATE);
        event.setParameters(eventParams);
// FIXME:        event.setStartId(startEventId);
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
                                                snapshot.getName(),
                                                templatePath);
        
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

        DiskCharacteristicsTO dskCh = null;
        if (volume.getVolumeType() == VolumeType.ROOT && Storage.ImageFormat.ISO != template.getFormat()) {
            dskCh = createDiskCharacteristics(volume, template, dc.getId(), offering);
        } else {
            dskCh = createDiskCharacteristics(volume, template, dc.getId(), diskOffering);
        }

        Transaction txn = Transaction.currentTxn();

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
                dataVol = new VolumeVO(VolumeType.DATADISK, vm.getId(), vm.getInstanceName() + "-DATA", dc.getId(), pod.getId(), account.getId(), account.getDomainId(), (size>0)? size : diskOffering.getDiskSizeInBytes());
                dataVol.setDiskOfferingId(diskOffering.getId());
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
                dataCreated = createVolume(dataVol, vm, template, dc, pod, pool.getClusterId(), offering, diskOffering, avoids,size);
                if (dataCreated == null) {
                    throw new CloudRuntimeException("Unable to create " + dataVol);
                }
                vols.add(dataCreated);
            }
            
            return vols;
        } catch (Exception e) {
        	s_logger.error("Unexpected exception ", e);
        	
            if (rootCreated != null) {
                destroyVolume(rootCreated);
            }
            throw new CloudRuntimeException("Unable to create volumes for " + vm, e);
        }
    }
    

    @Override
    public long createUserVM(Account account, VMInstanceVO vm, VMTemplateVO template, DataCenterVO dc, HostPodVO pod, ServiceOfferingVO offering, DiskOfferingVO diskOffering,
            List<StoragePoolVO> avoids, long size) {
        List<VolumeVO> volumes = create(account, vm, template, dc, pod, offering, diskOffering, avoids, size);
        
        for (VolumeVO v : volumes) {
        	long volumeId = v.getId();
        	// Create an event
        	long sizeMB = v.getSize() / (1024 * 1024);
        	String diskOfferingIdentifier = (diskOffering != null) ? String.valueOf(diskOffering.getId()) : "-1";
        	String eventParams = "id=" + volumeId + "\ndoId=" + diskOfferingIdentifier + "\ntId=" + template.getId() + "\ndcId=" + dc.getId() + "\nsize=" + sizeMB;
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
    				return vmInstance.getHostId();
    			}
    		}
    		return null;
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

        String overProvisioningFactorStr = (String) params.get("storage.overprovisioning.factor");
        if (overProvisioningFactorStr != null) {
            _overProvisioningFactor = Integer.parseInt(overProvisioningFactorStr);
        }

        Map<String, String> configs = configDao.getConfiguration("management-server", params);

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

    public String getSecondaryStorageURL(long zoneId) {
        // Determine the secondary storage URL
        HostVO secondaryStorageHost = _hostDao.findSecondaryStorageHost(zoneId);

        if (secondaryStorageHost == null) {
            return null;
        }

        return secondaryStorageHost.getStorageUrl();
    }

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

    @Override @SuppressWarnings("rawtypes")
    public StoragePoolVO createPool(CreateStoragePoolCmd cmd) throws ResourceInUseException, IllegalArgumentException, UnknownHostException, ResourceAllocationException {
        Long clusterId = cmd.getClusterId();
        Long podId = cmd.getPodId();
        Map ds = cmd.getDetails();

        if (clusterId != null && podId == null) {
            throw new ServerApiException(BaseCmd.PARAM_ERROR, "Cluster id requires pod id");
        }

        Map<String, String> details = new HashMap<String, String>();
        if (ds != null) {
            Collection detailsCollection = ds.values();
            Iterator it = detailsCollection.iterator();
            while (it.hasNext()) {
                HashMap d = (HashMap)it.next();
                Iterator it2 = d.entrySet().iterator();
                while (it2.hasNext()) {
                    Map.Entry entry = (Map.Entry)it2.next();
                    details.put((String)entry.getKey(), (String)entry.getValue());
                }
            }
        }

        //verify input parameters
        Long zoneId = cmd.getZoneId();
        DataCenterVO zone = _dcDao.findById(cmd.getZoneId());
        if (zone == null) {
            throw new ServerApiException(BaseCmd.PARAM_ERROR, "unable to find zone by id " + zoneId);
        }
        
        URI uri = null;
        try {
            uri = new URI(cmd.getUrl());
            if (uri.getScheme() == null)
                throw new ServerApiException(BaseCmd.PARAM_ERROR, "scheme is null " + cmd.getUrl() + ", add nfs:// as a prefix");
            else if (uri.getScheme().equalsIgnoreCase("nfs")) {
                String uriHost = uri.getHost();
                String uriPath = uri.getPath();
                if (uriHost == null || uriPath == null || uriHost.trim().isEmpty() || uriPath.trim().isEmpty()) {
                    throw new ServerApiException(BaseCmd.PARAM_ERROR, "host or path is null, should be nfs://hostname/path");
                }
            }
        } catch (URISyntaxException e) {
            throw new ServerApiException(BaseCmd.PARAM_ERROR, cmd.getUrl() + " is not a valid uri");
        }

        String tags = cmd.getTags();
        if (tags != null) {
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
        	} else {
        	    if (s_logger.isDebugEnabled()) {
                    s_logger.debug("Couldn't find a host to serve in the server pool");
        	    }
        		return null;
        	}
        }
        
        String scheme = uri.getScheme();
        String storageHost = uri.getHost();
        String hostPath = uri.getPath();
        int port = uri.getPort();
        StoragePoolVO pool = null;
        if (s_logger.isDebugEnabled()) {
            s_logger.debug("createPool Params @ scheme - " +scheme+ " storageHost - " +storageHost+ " hostPath - " +hostPath+ " port - " +port);
        }
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
                        pools = en.nextElement().find(cmd.getZoneId(), podId, uri, details);
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
        String uuid = UUID.nameUUIDFromBytes(new String(storageHost + hostPath + System.currentTimeMillis()).getBytes()).toString();
        if (s_logger.isDebugEnabled()) {
            s_logger.debug("In createPool Setting poolId - " +poolId+ " uuid - " +uuid+ " zoneId - " +zoneId+ " podId - " +podId+ " poolName - " +cmd.getStoragePoolName());
        }
        pool.setId(poolId);
        pool.setUuid(uuid);
        pool.setDataCenterId(cmd.getZoneId());
        pool.setPodId(podId);
        pool.setName(cmd.getStoragePoolName());
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
            _storagePoolDao.delete(pool.getId());
            pool = null;
        } else {
            createCapacityEntry(pool);
        }
        return pool;
    }
    
    @Override
    public StoragePoolVO updateStoragePool(UpdateStoragePoolCmd cmd) throws IllegalArgumentException 
    {
    	//Input validation
    	Long id = cmd.getId();
    	String tags = cmd.getTags();
    	
    	StoragePoolVO pool = _storagePoolDao.findById(id);
    	if (pool == null) {
    		throw new IllegalArgumentException("Unable to find storage pool with ID: " + id);
    	}
    	
    	if (tags != null) {
    		Map<String, String> details = _storagePoolDao.getDetails(id);
    		String[] tagsList = tags.split(",");
    		for (String tag : tagsList) {
    			tag = tag.trim();
    			if (tag.length() > 0 && !details.containsKey(tag)) {
    				details.put(tag, "true");
    			}
    		}
    		
    		_storagePoolDao.updateDetails(id, details);
    	}
    	
    	return pool;
    }

    @DB
    public boolean deletePool(DeletePoolCmd command) throws InvalidParameterValueException{
    	Long id = command.getId();
        boolean deleteFlag = false;
        
        
    	//verify parameters
    	StoragePoolVO sPool = _storagePoolDao.findById(id);
    	if (sPool == null) {
    		throw new InvalidParameterValueException("Unable to find pool by id " + id);
    	}
    	
    	if (sPool.getPoolType().equals(StoragePoolType.LVM)) {
    		throw new InvalidParameterValueException("Unable to delete local storage id: " + id);
    	}


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
    public VolumeVO createVolumeDB(CreateVolumeCmd cmd) throws InvalidParameterValueException, PermissionDeniedException, ResourceAllocationException {
        // FIXME:  some of the scheduled event stuff might be missing here...
        Account account = (Account)UserContext.current().getAccountObject();
        String accountName = cmd.getAccountName();
        Long domainId = cmd.getDomainId();
        Account targetAccount = null;
        if ((account == null) || isAdmin(account.getType())) {
            // Admin API call
            if ((domainId != null) && (accountName != null)) {
                if ((account != null) && !_domainDao.isChildDomain(account.getDomainId(), domainId)) {
                    throw new PermissionDeniedException("Unable to create volume in domain " + domainId + ", permission denied.");
                }

                targetAccount = _accountDao.findActiveAccount(accountName, domainId);
            } else {
                targetAccount = account;
            }

            // If the account is null, this means that the accountName and domainId passed in were invalid
            if (targetAccount == null) {
                throw new InvalidParameterValueException("Unable to find account with name: " + accountName + " and domain ID: " + domainId);
            }
        } else {
            targetAccount = account;
        }

        // check if the volume can be created for the user
        // Check that the resource limit for volumes won't be exceeded
        if (_accountMgr.resourceLimitExceeded((AccountVO)targetAccount, ResourceType.volume)) {
            ResourceAllocationException rae = new ResourceAllocationException("Maximum number of volumes for account: " + targetAccount.getAccountName() + " has been exceeded.");
            rae.setResourceType("volume");
            throw rae;
        }

        // validate input parameters before creating the volume
        if (cmd.getSnapshotId() == null) {
            Long zoneId = cmd.getZoneId();
            if ((zoneId == null)) {
                throw new InvalidParameterValueException("Missing parameter, zoneid must be specified.");
            }

            Long diskOfferingId = cmd.getDiskOfferingId();
            Long size = cmd.getSize();
            if ((diskOfferingId == null) && (size == null)) {
                throw new InvalidParameterValueException("Missing parameter(s),either a positive volume size or a valid disk offering id must be specified.");
            } else if ((diskOfferingId == null) && (size != null)) {
                boolean ok = validateCustomVolumeSizeRange(size);

                if (!ok) {
                    throw new InvalidParameterValueException("Invalid size for custom volume creation: " + size);
                }

                //this is the case of creating var size vol with private disk offering
                List<DiskOfferingVO> privateTemplateList = _diskOfferingDao.findPrivateDiskOffering();
                diskOfferingId = privateTemplateList.get(0).getId(); //we use this id for creating volume
            } else {
                // Check that the the disk offering is specified
                DiskOfferingVO diskOffering = _diskOfferingDao.findById(diskOfferingId);
                if ((diskOffering == null) || !DiskOfferingVO.Type.Disk.equals(diskOffering.getType())) {
                    throw new InvalidParameterValueException("Please specify a valid disk offering.");
                }
            }
        } else {
            Long snapshotId = cmd.getSnapshotId();
            Snapshot snapshotCheck = _snapshotDao.findById(snapshotId);
            if (snapshotCheck == null) {
                throw new ServerApiException (BaseCmd.SNAPSHOT_INVALID_PARAM_ERROR, "unable to find a snapshot with id " + snapshotId);
            }
            
            if (account != null) {
                if (isAdmin(account.getType())) {
                    Account snapshotOwner = _accountDao.findById(snapshotCheck.getAccountId());
                    if (!_domainDao.isChildDomain(account.getDomainId(), snapshotOwner.getDomainId())) {
                        throw new ServerApiException(BaseCmd.ACCOUNT_ERROR, "Unable to create volume from snapshot with id " + snapshotId + ", permission denied.");
                    }
                } else if (account.getId().longValue() != snapshotCheck.getAccountId()) {
                    throw new ServerApiException(BaseCmd.SNAPSHOT_INVALID_PARAM_ERROR, "unable to find a snapshot with id " + snapshotId + " for this account");
                }
            }
        }

        Long zoneId = cmd.getZoneId();
        // Check that the zone is valid
        DataCenterVO zone = _dcDao.findById(zoneId);
        if (zone == null) {
            throw new InvalidParameterValueException("Please specify a valid zone.");
        }
        
        // Check that there is a shared primary storage pool in the specified zone
        List<StoragePoolVO> storagePools = _storagePoolDao.listByDataCenterId(zoneId);
        boolean sharedPoolExists = false;
        for (StoragePoolVO storagePool : storagePools) {
            if (storagePool.isShared()) {
                sharedPoolExists = true;
            }
        }
        
        // Check that there is at least one host in the specified zone
        List<HostVO> hosts = _hostDao.listByDataCenter(zoneId);
        if (hosts.isEmpty()) {
            throw new InvalidParameterValueException("Please add a host in the specified zone before creating a new volume.");
        }
        
        if (!sharedPoolExists) {
            throw new InvalidParameterValueException("Please specify a zone that has at least one shared primary storage pool.");
        }

        String userSpecifiedName = cmd.getVolumeName();
        if (userSpecifiedName == null) {
            userSpecifiedName = getRandomVolumeName();
        }

        VolumeVO volume = new VolumeVO(null, userSpecifiedName, -1, -1, -1, -1, new Long(-1), null, null, 0, Volume.VolumeType.DATADISK);
        volume.setPoolId(null);
        volume.setDataCenterId(zoneId);
        volume.setPodId(null);
        volume.setAccountId(targetAccount.getId());
        volume.setDomainId(account.getDomainId().longValue());
        volume.setMirrorState(MirrorState.NOT_MIRRORED);
        volume.setDiskOfferingId(cmd.getDiskOfferingId());
        volume.setStorageResourceType(StorageResourceType.STORAGE_POOL);
        volume.setInstanceId(null);
        volume.setUpdated(new Date());
        volume.setStatus(AsyncInstanceCreateStatus.Creating);
        volume.setDomainId(account.getDomainId().longValue());
        volume = _volsDao.persist(volume);

        return volume;
    }

    @Override @DB
    public VolumeVO createVolume(CreateVolumeCmd cmd) {
        VolumeVO volume = _volsDao.findById(cmd.getId());
        VolumeVO createdVolume = null;
        Long userId = UserContext.current().getUserId();

        if (cmd.getSnapshotId() != null) {
            return createVolumeFromSnapshot(volume, cmd.getSnapshotId());
        } else {
            DataCenterVO dc = _dcDao.findById(cmd.getZoneId());
            DiskOfferingVO diskOffering = _diskOfferingDao.findById(cmd.getDiskOfferingId());
            long size = diskOffering.getDiskSize();

            try {
                List<StoragePoolVO> poolsToAvoid = new ArrayList<StoragePoolVO>();
                Set<Long> podsToAvoid = new HashSet<Long>();
                Pair<HostPodVO, Long> pod = null;
        
                while ((pod = _agentMgr.findPod(null, null, dc, volume.getAccountId(), podsToAvoid)) != null) {
                    if ((createdVolume = createVolume(volume, null, null, dc, pod.first(), null, null, diskOffering, poolsToAvoid, size)) != null) {
                        break;
                    } else {
                        podsToAvoid.add(pod.first().getId());
                    }
                }

                // Create an event
                EventVO event = new EventVO();
                event.setAccountId(volume.getAccountId());
                event.setUserId(userId);
                event.setType(EventTypes.EVENT_VOLUME_CREATE);
                event.setStartId(cmd.getStartEventId());
        
                Transaction txn = Transaction.currentTxn();

                txn.start();

                if (createdVolume != null) {
                    // Increment the number of volumes
                    _accountMgr.incrementResourceCount(createdVolume.getAccountId(), ResourceType.volume);
                    
                    // Set event parameters
                    long sizeMB = createdVolume.getSize() / (1024 * 1024);
                    StoragePoolVO pool = _storagePoolDao.findById(createdVolume.getPoolId());
                    String eventParams = "id=" + createdVolume.getId() + "\ndoId=" + diskOffering.getId() + "\ntId=" + -1 + "\ndcId=" + dc.getId() + "\nsize=" + sizeMB;
                    event.setLevel(EventVO.LEVEL_INFO);
                    event.setDescription("Created volume: " + createdVolume.getName() + " with size: " + sizeMB + " MB in pool: " + pool.getName());
                    event.setParameters(eventParams);
                    event.setState(EventState.Completed);
                    _eventDao.persist(event);
                } else {
                    event.setDescription("Unable to create a volume for " + volume);
                    event.setLevel(EventVO.LEVEL_ERROR);
                    event.setState(EventState.Completed);
                    _eventDao.persist(event);
                }            

                txn.commit();
            } catch (Exception e) {
                s_logger.error("Unhandled exception while creating volume " + volume.getName(), e);
            }
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
        SearchCriteria<CapacityVO> capacitySC = _capacityDao.createSearchCriteria();
        capacitySC.addAnd("hostOrPoolId", SearchCriteria.Op.EQ, storagePool.getId());
        capacitySC.addAnd("dataCenterId", SearchCriteria.Op.EQ, storagePool.getDataCenterId());
        capacitySC.addAnd("capacityType", SearchCriteria.Op.EQ, CapacityVO.CAPACITY_TYPE_STORAGE);

        List<CapacityVO> capacities = _capacityDao.search(capacitySC, null);

        if (capacities.size() == 0) {
            CapacityVO capacity = new CapacityVO(storagePool.getId(), storagePool.getDataCenterId(), storagePool.getPodId(), 0L, storagePool.getCapacityBytes(),
                    CapacityVO.CAPACITY_TYPE_STORAGE);
            _capacityDao.persist(capacity);
        } else {
            CapacityVO capacity = capacities.get(0);
            if (capacity.getTotalCapacity() != storagePool.getCapacityBytes()) {
                capacity.setTotalCapacity(storagePool.getCapacityBytes());
                _capacityDao.update(capacity.getId(), capacity);
            }
        }
        s_logger.debug("Successfully set Capacity - " +storagePool.getCapacityBytes()+ " for CAPACITY_TYPE_STORAGE, DataCenterId - " +storagePool.getDataCenterId()+ ", HostOrPoolId - " +storagePool.getId()+ ", PodId " +storagePool.getPodId());
        capacitySC = _capacityDao.createSearchCriteria();
        capacitySC.addAnd("hostOrPoolId", SearchCriteria.Op.EQ, storagePool.getId());
        capacitySC.addAnd("dataCenterId", SearchCriteria.Op.EQ, storagePool.getDataCenterId());
        capacitySC.addAnd("capacityType", SearchCriteria.Op.EQ, CapacityVO.CAPACITY_TYPE_STORAGE_ALLOCATED);

        capacities = _capacityDao.search(capacitySC, null);

        if (capacities.size() == 0) {
            int provFactor = 1;
            if( storagePool.getPoolType() == StoragePoolType.NetworkFilesystem ) {
                provFactor = _overProvisioningFactor;
            }

            CapacityVO capacity = new CapacityVO(storagePool.getId(), storagePool.getDataCenterId(), storagePool.getPodId(), 0L, storagePool.getCapacityBytes()
                    * provFactor, CapacityVO.CAPACITY_TYPE_STORAGE_ALLOCATED);
            _capacityDao.persist(capacity);
        } else {
            CapacityVO capacity = capacities.get(0);
            long currCapacity = _overProvisioningFactor * storagePool.getCapacityBytes();
            if (capacity.getTotalCapacity() != currCapacity) {
                capacity.setTotalCapacity(currCapacity);
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
                answer = _agentMgr.send(hostId, cmd);
                
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
    
    public List<StoragePoolVO> getStoragePoolsForVm(long vmId) {
        SearchCriteria<StoragePoolVO> sc = PoolsUsedByVmSearch.create();
        sc.setJoinParameters("volumes", "vm", vmId);
        
        return _storagePoolDao.search(sc, null);
    }
    
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
    public boolean preparePrimaryStorageForMaintenance(PreparePrimaryStorageForMaintenanceCmd cmd) {
    	Long primaryStorageId = cmd.getId();
    	Long userId = UserContext.current().getUserId();
    	
        boolean destroyVolumes = false;
        long count = 1;
        long consoleProxyId = 0;
        long ssvmId = 0;
        try 
        {
        	//1. Get the primary storage record and perform validation check
        	StoragePoolVO primaryStorage = _storagePoolDao.findById(primaryStorageId);
        	
        	if(primaryStorage == null)
        	{
        		s_logger.warn("The primary storage does not exist");
        		return false;
        	}	
        	
        	if (!primaryStorage.getStatus().equals(Status.Up)) {
    			throw new InvalidParameterValueException("Primary storage with id " + primaryStorageId + " is not ready for migration, as the status is:" + primaryStorage.getStatus().toString());
        	}
        	
        	//check to see if other ps exist
        	//if they do, then we can migrate over the system vms to them, destroy volumes for sys vms
        	//if they dont, then do NOT destroy the volumes on this one
        	count = _storagePoolDao.countBy(primaryStorage.getId(), Status.Up);
        	if(count>1)
        	{
        		destroyVolumes = true;
        	}
        	
        	//2. Get a list of all the volumes within this storage pool
        	List<VolumeVO> allVolumes = _volsDao.findByPoolId(primaryStorageId);
        	List<VolumeVO> markedVolumes = new ArrayList<VolumeVO>();
        	
        	//3. Each volume has an instance associated with it, stop the instance if running
        	for(VolumeVO volume : allVolumes)
        	{
        		VMInstanceVO vmInstance = _vmInstanceDao.findById(volume.getInstanceId());
        		
        		//shut down the running vms
        		if(vmInstance.getState().equals(State.Running) || vmInstance.getState().equals(State.Stopped) || vmInstance.getState().equals(State.Stopping) || vmInstance.getState().equals(State.Starting))
        		{
        			
        			//if the instance is of type consoleproxy, call the console proxy
        			if(vmInstance.getType().equals(VirtualMachine.Type.ConsoleProxy))
        			{
        				//add this volume to be removed if flag=true
        				if(destroyVolumes)
        					markedVolumes.add(volume);
        				
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
        				else
        				{
        					if(destroyVolumes)
        					{
        						//proxy vm is stopped, and we have another ps available 
        						//get the id for restart
        						consoleProxyId = vmInstance.getId();        						
        					}
        				}
        			}
        			
        			//if the instance is of type uservm, call the user vm manager
        			if(vmInstance.getType().equals(VirtualMachine.Type.User))
        			{
        				//create a dummy event
        				long eventId = saveScheduledEvent(User.UID_SYSTEM, Account.ACCOUNT_ID_SYSTEM, EventTypes.EVENT_VM_STOP, "stopping user vm with Id: "+vmInstance.getId());
        				
        				if(!_userVmMgr.stopVirtualMachine(userId, vmInstance.getId(),eventId))
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
        				//add this volume to be removed if flag=true
        				if(destroyVolumes)
        					markedVolumes.add(volume);
        				
        				//create a dummy event
        				long eventId1 = saveScheduledEvent(User.UID_SYSTEM, Account.ACCOUNT_ID_SYSTEM, EventTypes.EVENT_SSVM_STOP, "stopping ssvm with Id: "+vmInstance.getId());

        				if(!_secStorageMgr.stopSecStorageVm(vmInstance.getId(), eventId1))
        				{
        					s_logger.warn("There was an error stopping the ssvm id: "+vmInstance.getId()+" ,cannot enable storage maintenance");
        	            	primaryStorage.setStatus(Status.ErrorInMaintenance);
        	        		_storagePoolDao.persist(primaryStorage);
        					return false;
        				}
        				else
        				{
        					if(destroyVolumes)
        					{
        						//ss vm is stopped, and we have another ps available 				
        						//get the id for restart
        						ssvmId = vmInstance.getId();
        					}
        				}

        			}

           			//if the instance is of type domain router vm, call the network manager
        			if(vmInstance.getType().equals(VirtualMachine.Type.DomainRouter))
        			{   
        				//add this volume to be removed if flag=true
        				if(destroyVolumes)
        					markedVolumes.add(volume);
        				
        				//create a dummy event
        				long eventId2 = saveScheduledEvent(User.UID_SYSTEM, Account.ACCOUNT_ID_SYSTEM, EventTypes.EVENT_ROUTER_STOP, "stopping domain router with Id: "+vmInstance.getId());

        				if(!_networkMgr.stopRouter(vmInstance.getId(), eventId2))
        				{
        					s_logger.warn("There was an error stopping the domain router id: "+vmInstance.getId()+" ,cannot enable primary storage maintenance");
        	            	primaryStorage.setStatus(Status.ErrorInMaintenance);
        	        		_storagePoolDao.persist(primaryStorage);
        					return false;
        				}
        			}

        		}	
        	}
        	
        	//4. Mark the volumes as removed
        	for(VolumeVO vol : markedVolumes)
        	{
        		_volsDao.remove(vol.getId());
        	}
        	
        	//5. Restart all the system vms conditionally
        	if(destroyVolumes) //this means we have another ps. Ok to restart
        	{
				//create a dummy event
				long eventId = saveScheduledEvent(User.UID_SYSTEM, Account.ACCOUNT_ID_SYSTEM, EventTypes.EVENT_SSVM_START, "starting ssvm with Id: "+ssvmId);
				if(_secStorageMgr.startSecStorageVm(ssvmId, eventId)==null)
				{
					s_logger.warn("There was an error starting the ssvm id: "+ssvmId+" on another storage pool, cannot enable primary storage maintenance");
	            	primaryStorage.setStatus(Status.ErrorInMaintenance);
	        		_storagePoolDao.persist(primaryStorage);
					return false;
				}
				
				//create a dummy event
				long eventId1 = saveScheduledEvent(User.UID_SYSTEM, Account.ACCOUNT_ID_SYSTEM, EventTypes.EVENT_PROXY_START, "starting console proxy with Id: "+consoleProxyId);
				
				//Restore config val for consoleproxy.restart to true
				_configMgr.updateConfiguration(userId, "consoleproxy.restart", "true");
				
				if(_consoleProxyMgr.startProxy(consoleProxyId, eventId1)==null)
				{
					s_logger.warn("There was an error starting the console proxy id: "+consoleProxyId+" on another storage pool, cannot enable primary storage maintenance");
	            	primaryStorage.setStatus(Status.ErrorInMaintenance);
	        		_storagePoolDao.persist(primaryStorage);
					return false;				}
        	}
        	
        	//6. Update the status
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
	public StoragePoolVO cancelPrimaryStorageForMaintenance(CancelPrimaryStorageMaintenanceCmd cmd) throws InvalidParameterValueException{
		Long primaryStorageId = cmd.getId();
		Long userId = UserContext.current().getUserId();
		
    	//1. Get the primary storage record and perform validation check
    	StoragePoolVO primaryStorage = _storagePoolDao.findById(primaryStorageId);
    	
    	if(primaryStorage == null)
    	{
    		s_logger.warn("The primary storage does not exist");
    		throw new InvalidParameterValueException("Primary storage doesn't exist");
    	}
    	
    	if (!primaryStorage.getStatus().equals(Status.Maintenance)) {
    		throw new InvalidParameterValueException("Primary storage with id " + primaryStorageId + " is not ready for migration, as the status is:" + primaryStorage.getStatus().toString());
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
        					return primaryStorage;
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
        					return primaryStorage;
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
	        					return primaryStorage;
							}
						} catch (StorageUnavailableException e) {
							s_logger.warn("There was an error starting the ssvm id: "+vmInstance.getId()+" on storage pool, cannot complete primary storage maintenance");
							s_logger.warn(e);
        	            	primaryStorage.setStatus(Status.ErrorInMaintenance);
        	        		_storagePoolDao.persist(primaryStorage);
        					return primaryStorage;
						} catch (InsufficientCapacityException e) {
							s_logger.warn("There was an error starting the ssvm id: "+vmInstance.getId()+" on storage pool, cannot complete primary storage maintenance");
							s_logger.warn(e);
        	            	primaryStorage.setStatus(Status.ErrorInMaintenance);
        	        		_storagePoolDao.persist(primaryStorage);
        					return primaryStorage;				
						} catch (ConcurrentOperationException e) {
							s_logger.warn("There was an error starting the ssvm id: "+vmInstance.getId()+" on storage pool, cannot complete primary storage maintenance");
							s_logger.warn(e);
        	            	primaryStorage.setStatus(Status.ErrorInMaintenance);
        	        		_storagePoolDao.persist(primaryStorage);
        					return primaryStorage;
						} catch (ExecutionException e) {
							s_logger.warn("There was an error starting the ssvm id: "+vmInstance.getId()+" on storage pool, cannot complete primary storage maintenance");
							s_logger.warn(e);
        	            	primaryStorage.setStatus(Status.ErrorInMaintenance);
        	        		_storagePoolDao.persist(primaryStorage);
        					return primaryStorage;
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
			return primaryStorage;
		} catch (InternalErrorException e) {
			s_logger.warn("Error changing consoleproxy.restart back to false at end of cancel maintenance:"+e);
        	primaryStorage.setStatus(Status.ErrorInMaintenance);
    		_storagePoolDao.persist(primaryStorage);
			return primaryStorage;
		}
		
		//Change the storage state back to up
		primaryStorage.setStatus(Status.Up);
		_storagePoolDao.persist(primaryStorage);
		
    	return primaryStorage;
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

	private boolean isAdmin(short accountType) {
	    return ((accountType == Account.ACCOUNT_TYPE_ADMIN) ||
	            (accountType == Account.ACCOUNT_TYPE_DOMAIN_ADMIN) ||
	            (accountType == Account.ACCOUNT_TYPE_READ_ONLY_ADMIN));
	}
	
	@Override
	public boolean deleteVolume(DeleteVolumeCmd cmd) throws InvalidParameterValueException {
    	Account account = (Account) UserContext.current().getAccountObject();
    	Long volumeId = cmd.getId();
    	
    	boolean isAdmin;
    	if (account == null) {
    		// Admin API call
    		isAdmin = true;
    	} else {
    		// User API call
    		isAdmin = isAdmin(account.getType());
    	}

    	// Check that the volume ID is valid
    	VolumeVO volume = _volsDao.findById(volumeId);
    	if (volume == null) {
    		throw new ServerApiException(BaseCmd.PARAM_ERROR, "Unable to find volume with ID: " + volumeId);
    	}
    	
    	// If the account is not an admin, check that the volume is owned by the account that was passed in
    	if (!isAdmin) {
    		if (account.getId() != volume.getAccountId()) {
                throw new ServerApiException(BaseCmd.PARAM_ERROR, "Unable to find volume with ID: " + volumeId + " for account: " + account.getAccountName());
    		}
    	} else if ((account != null) && !_domainDao.isChildDomain(account.getDomainId(), volume.getDomainId())) {
            throw new ServerApiException(BaseCmd.PARAM_ERROR, "Unable to delete volume with id " + volumeId + ", permission denied.");
    	}


        // Check that the volume is stored on shared storage
        if (!volumeOnSharedStoragePool(volume)) {
            throw new InvalidParameterValueException("Please specify a volume that has been created on a shared storage pool.");
        }

        // Check that the volume is not currently attached to any VM
        if (volume.getInstanceId() != null) {
            throw new InvalidParameterValueException("Please specify a volume that is not attached to any VM.");
        }
           
        // Check that the volume is not already destroyed
        if (volume.getDestroyed()) {
            throw new InvalidParameterValueException("Please specify a volume that is not already destroyed.");
        }
        
        try {
			// Destroy the volume
			destroyVolume(volume);
		} catch (Exception e) {
			s_logger.warn("Error destroying volume:"+e);
			throw new ServerApiException(BaseCmd.INTERNAL_ERROR, "Error destroying volume:"+e);
		}
        
        return true;
    	
	}

	private boolean validateCustomVolumeSizeRange(long size) throws InvalidParameterValueException {
	    if (size<0 || (size>0 && size < 1)) {
	        throw new InvalidParameterValueException("Please specify a size of at least 1 Gb.");
	    } else if (size > _maxVolumeSizeInGb) {
	        throw new InvalidParameterValueException("The maximum size allowed is " + _maxVolumeSizeInGb + " Gb.");
	    }

	    return true;
	}
}