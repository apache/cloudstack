// Licensed to the Apache Software Foundation (ASF) under one
// or more contributor license agreements.  See the NOTICE file
// distributed with this work for additional information
// regarding copyright ownership.  The ASF licenses this file
// to you under the Apache License, Version 2.0 (the
// "License"); you may not use this file except in compliance
// with the License.  You may obtain a copy of the License at
//
//   http://www.apache.org/licenses/LICENSE-2.0
//
// Unless required by applicable law or agreed to in writing,
// software distributed under the License is distributed on an
// "AS IS" BASIS, WITHOUT WARRANTIES OR CONDITIONS OF ANY
// KIND, either express or implied.  See the License for the
// specific language governing permissions and limitations
// under the License.

package org.apache.cloudstack.storage.fileshare.lifecycle;

import static org.apache.cloudstack.storage.fileshare.FileShare.FileShareVmNamePrefix;
import static org.apache.cloudstack.storage.fileshare.provider.StorageFsVmFileShareProvider.STORAGEFSVM_MIN_CPU_COUNT;
import static org.apache.cloudstack.storage.fileshare.provider.StorageFsVmFileShareProvider.STORAGEFSVM_MIN_RAM_SIZE;

import com.cloud.exception.InsufficientCapacityException;
import com.cloud.exception.InvalidParameterValueException;
import com.cloud.exception.OperationTimedoutException;
import com.cloud.exception.ResourceAllocationException;
import com.cloud.storage.LaunchPermissionVO;
import com.cloud.storage.Volume;
import com.cloud.storage.VolumeApiService;
import com.cloud.storage.VolumeVO;
import com.cloud.storage.dao.LaunchPermissionDao;
import com.cloud.storage.dao.VolumeDao;
import com.cloud.uservm.UserVm;
import com.cloud.utils.Pair;

import java.io.IOException;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;

import javax.inject.Inject;

import com.cloud.exception.ResourceUnavailableException;
import com.cloud.vm.NicVO;
import com.cloud.vm.UserVmManager;
import com.cloud.vm.UserVmService;
import com.cloud.vm.UserVmVO;
import com.cloud.vm.dao.NicDao;
import com.cloud.vm.dao.UserVmDao;

import org.apache.cloudstack.api.BaseCmd;
import org.apache.cloudstack.context.CallContext;
import org.apache.cloudstack.storage.fileshare.FileShare;
import org.apache.cloudstack.storage.fileshare.FileShareLifeCycle;
import org.apache.cloudstack.storage.fileshare.FileShareService;
import org.apache.commons.codec.binary.Base64;
import org.apache.commons.io.IOUtils;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

import com.cloud.dc.DataCenter;
import com.cloud.hypervisor.Hypervisor;
import com.cloud.network.Network;
import com.cloud.resource.ResourceManager;
import com.cloud.service.ServiceOfferingVO;
import com.cloud.service.dao.ServiceOfferingDao;
import com.cloud.storage.DiskOfferingVO;
import com.cloud.storage.VMTemplateVO;
import com.cloud.storage.dao.DiskOfferingDao;
import com.cloud.storage.dao.VMTemplateDao;
import com.cloud.user.Account;
import com.cloud.user.AccountManager;
import com.cloud.utils.db.EntityManager;
import com.cloud.utils.exception.CloudRuntimeException;
import com.cloud.vm.VirtualMachineManager;

public class StorageFsVmFileShareLifeCycle implements FileShareLifeCycle {
    protected Logger logger = LogManager.getLogger(getClass());

    @Inject
    private FileShareService fileShareService;

    @Inject
    private AccountManager accountMgr;

    @Inject
    private EntityManager entityMgr;

    @Inject
    protected ResourceManager resourceMgr;

    @Inject
    private VirtualMachineManager virtualMachineManager;

    @Inject
    private VolumeApiService volumeApiService;

    @Inject
    protected UserVmService userVmService;

    @Inject
    protected UserVmManager userVmManager;

    @Inject
    private VMTemplateDao templateDao;

    @Inject
    VolumeDao volumeDao;

    @Inject
    private UserVmDao userVmDao;

    @Inject
    NicDao nicDao;

    @Inject
    ServiceOfferingDao serviceOfferingDao;

    @Inject
    private DiskOfferingDao diskOfferingDao;

    @Inject
    protected LaunchPermissionDao launchPermissionDao;

    private String readResourceFile(String resource) {
        try {
            return IOUtils.toString(Objects.requireNonNull(Thread.currentThread().getContextClassLoader().getResourceAsStream(resource)), com.cloud.utils.StringUtils.getPreferredCharset());
        } catch (IOException e) {
            throw new CloudRuntimeException("Unable to read the user data resource file due to exception " + e.getMessage());
        }
    }

    private String getStorageFsVmConfig(final String fileSystem) {
        String fsVmConfig = readResourceFile("/conf/fsvm-init.yml");
        final String filesystem = "{{ fsvm.filesystem }}";
        fsVmConfig = fsVmConfig.replace(filesystem, fileSystem);
        return fsVmConfig;
    }

    private UserVm createFileShareVM(Long zoneId, Account owner, List<Long> networkIds, String name, Long serviceOfferingId, Long diskOfferingId, FileShare.FileSystemType fileSystem, Long size, Long minIops, Long maxIops) throws ResourceUnavailableException, InsufficientCapacityException, ResourceAllocationException {
        ServiceOfferingVO serviceOffering = serviceOfferingDao.findById(serviceOfferingId);

        Long diskSize = null;
        if (diskOfferingId != null) {
            DiskOfferingVO diskOffering = diskOfferingDao.findById(diskOfferingId);
            if (diskOffering.isCustomized()) {
                diskSize = size;
            }
        }

        DataCenter zone = entityMgr.findById(DataCenter.class, zoneId);
        Hypervisor.HypervisorType availableHypervisor = resourceMgr.getAvailableHypervisor(zoneId);
        VMTemplateVO template = templateDao.findSystemVMReadyTemplate(zoneId, availableHypervisor);

        LaunchPermissionVO existingPermission = launchPermissionDao.findByTemplateAndAccount(template.getId(), owner.getId());
        if (existingPermission == null) {
            LaunchPermissionVO launchPermission = new LaunchPermissionVO(template.getId(), owner.getId());
            launchPermissionDao.persist(launchPermission);
        }

        String suffix = Long.toHexString(System.currentTimeMillis());
        String hostName = String.format("%s-%s-%s", FileShareVmNamePrefix, name, suffix);

        Network.IpAddresses addrs = new Network.IpAddresses(null, null);
        Map<String, String> customParameterMap = new HashMap<String, String>();
        if (minIops != null) {
            customParameterMap.put("minIopsDo", minIops.toString());
            customParameterMap.put("maxIopsDo", maxIops.toString());
        }
        List<String> keypairs = new ArrayList<String>();

        UserVm vm;
        String fsVmConfig = getStorageFsVmConfig(fileSystem.toString().toLowerCase());
        String base64UserData = Base64.encodeBase64String(fsVmConfig.getBytes(com.cloud.utils.StringUtils.getPreferredCharset()));
        vm = userVmService.createAdvancedVirtualMachine(zone, serviceOffering, template, networkIds, owner, hostName, hostName,
                diskOfferingId, diskSize, null, Hypervisor.HypervisorType.None, BaseCmd.HTTPMethod.POST, base64UserData,
                null, null, keypairs, null, addrs, null, null, null,
                customParameterMap, null, null, null, null,
                true, UserVmManager.STORAGEFSVM, null);
        return vm;
    }

    @Override
    public void checkPrerequisites(DataCenter zone, Long serviceOfferingId) {
        ServiceOfferingVO serviceOffering = serviceOfferingDao.findById(serviceOfferingId);
        if (serviceOffering.getCpu() < STORAGEFSVM_MIN_CPU_COUNT.valueIn(zone.getId())) {
            throw new InvalidParameterValueException("Service offering's number of cpu should be greater than or equal to " + STORAGEFSVM_MIN_CPU_COUNT.key());
        }
        if (serviceOffering.getRamSize() < STORAGEFSVM_MIN_RAM_SIZE.valueIn(zone.getId())) {
            throw new InvalidParameterValueException("Service offering's ram size should be greater than or equal to " + STORAGEFSVM_MIN_RAM_SIZE.key());
        }
        if (serviceOffering.isOfferHA() == false) {
            throw new InvalidParameterValueException("Service offering's should be HA enabled");
        }

        Hypervisor.HypervisorType availableHypervisor = resourceMgr.getAvailableHypervisor(zone.getId());
        VMTemplateVO template = templateDao.findSystemVMReadyTemplate(zone.getId(), availableHypervisor);
        if (template == null) {
            throw new CloudRuntimeException(String.format("Unable to find the system templates or it was not downloaded in %s.", zone.toString()));
        }
    }

    @Override
    public Pair<Long, Long> commitFileShare(FileShare fileShare, Long networkId, Long diskOfferingId, Long size, Long minIops, Long maxIops) throws ResourceUnavailableException, InsufficientCapacityException, ResourceAllocationException {
        Account owner = accountMgr.getActiveAccountById(fileShare.getAccountId());
        UserVm vm = createFileShareVM(fileShare.getDataCenterId(), owner, List.of(networkId), fileShare.getName(), fileShare.getServiceOfferingId(), diskOfferingId, fileShare.getFsType(), size, minIops, maxIops);

        List<VolumeVO> volumes = volumeDao.findByInstanceAndType(vm.getId(), Volume.Type.DATADISK);
        return new Pair<>(volumes.get(0).getId(), vm.getId());
    }

    @Override
    public void startFileShare(FileShare fileShare) throws OperationTimedoutException, ResourceUnavailableException, InsufficientCapacityException {
        UserVmVO vm = userVmDao.findById(fileShare.getVmId());
        userVmManager.startVirtualMachine(vm);
    }

    @Override
    public boolean stopFileShare(FileShare fileShare, Boolean forced) {
        userVmManager.stopVirtualMachine(fileShare.getVmId(), false);
        return true;
    }

    private void expungeVm(Long vmId) {
        UserVmVO userVM = userVmDao.findById(vmId);
        if (userVM == null) {
            return;
        }
        try {
            UserVm vm = userVmService.destroyVm(userVM.getId(), true);
            if (!userVmManager.expunge(userVM)) {
                throw new CloudRuntimeException("Failed to expunge VM " + userVM.toString());
            }
        } catch (ResourceUnavailableException e) {
            throw new CloudRuntimeException("Failed to expunge VM " + userVM.toString());
        }
        userVmDao.remove(vmId);
    }

    @Override
    public boolean deleteFileShare(FileShare fileShare) {
        Long vmId = fileShare.getVmId();
        Long volumeId = fileShare.getVolumeId();
        if (vmId != null) {
            expungeVm(vmId);
        }

        if (volumeId == null) {
            return true;
        }
        VolumeVO volume = volumeDao.findById(volumeId);
        Boolean expunge = false;
        Boolean forceExpunge = false;
        if (volume.getState() == Volume.State.Allocated) {
            expunge = true;
            forceExpunge = true;
        }
        volumeApiService.destroyVolume(volume.getId(), CallContext.current().getCallingAccount(), expunge, forceExpunge);
        return true;
    }

    @Override
    public Pair<Boolean, Long> reDeployFileShare(FileShare fileShare) throws ResourceUnavailableException, InsufficientCapacityException, ResourceAllocationException {
        Long oldVmId = fileShare.getVmId();
        Long volumeId = fileShare.getVolumeId();
        Account owner = accountMgr.getActiveAccountById(fileShare.getAccountId());

        final List<NicVO> nics = nicDao.listByVmId(oldVmId);
        List<Long> networkIds = new ArrayList<>();
        for (NicVO nic : nics) {
           networkIds.add(nic.getNetworkId());
        }

        UserVm newVm;
        try {
            newVm = createFileShareVM(fileShare.getDataCenterId(), owner, networkIds, fileShare.getName(), fileShare.getServiceOfferingId(), null, fileShare.getFsType(), null, null, null);
        } catch (Exception ex) {
            logger.error(String.format("Redeploy fileshare [%]: VM deploy failed with error %", fileShare.toString(), ex.getMessage()));
            throw ex;
        }

        Volume volume = null;
        if (!fileShare.getState().equals(FileShare.State.Detached)) {
            volume = volumeApiService.detachVolumeViaDestroyVM(oldVmId, volumeId);
            if (volume == null) {
                UserVmVO oldVM = userVmDao.findById(oldVmId);
                volume = volumeDao.findById(volumeId);
                expungeVm(newVm.getId());
                String message = String.format("Redeploy fileshare [%]: volume % couldn't be detached from the old VM %", fileShare.toString(), volume.toString(), oldVmId.toString());
                logger.error(message);
                throw new CloudRuntimeException(message);
            }
        } else {
            volume = volumeDao.findById(volumeId);
        }

        volume = volumeApiService.attachVolumeToVM(newVm.getId(), volume.getId(), null, true);
        if (volume == null) {
            logger.error(String.format("Redeploy fileshare [%]: volume % couldn't be attached to the new VM %", fileShare.toString(), volume.toString(), oldVmId.toString()));
            expungeVm(newVm.getId());
            return new Pair<>(false, 0L);
        }

        try {
            expungeVm(oldVmId);
        } catch (Exception ex) {
            logger.error("Redeploy fileshare " + fileShare.toString() + ": expunging of old VM " + newVm.toString() + "failed with error " + ex.getMessage());
        }
        return new Pair<>(true, newVm.getId());
    }
}
