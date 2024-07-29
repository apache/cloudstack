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
import org.apache.commons.codec.binary.Base64;
import org.apache.commons.io.IOUtils;

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

    private UserVm deployFileShareVM(Long zoneId, Account owner, List<Long> networkIds, String name, Long serviceOfferingId, Long diskOfferingId, FileShare.FileSystemType fileSystem, Long size) {
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
        List<String> keypairs = new ArrayList<String>();

        UserVm vm;
        try {
            String fsVmConfig = getStorageFsVmConfig(fileSystem.toString().toLowerCase());
            String base64UserData = Base64.encodeBase64String(fsVmConfig.getBytes(com.cloud.utils.StringUtils.getPreferredCharset()));
            vm = userVmService.createAdvancedVirtualMachine(zone, serviceOffering, template, networkIds, owner, hostName, hostName,
                    diskOfferingId, diskSize, null, Hypervisor.HypervisorType.None, BaseCmd.HTTPMethod.POST, base64UserData,
                    null, null, keypairs, null, addrs, null, null, null,
                    customParameterMap, null, null, null, null,
                    true, UserVmManager.STORAGEFSVM, null);
        } catch (Exception ex) {
            throw new CloudRuntimeException("Unable to deploy fsvm due to exception " + ex.getMessage());
        }
        return vm;
    }

    @Override
    public void checkPrerequisites(Long zoneId, Long serviceOfferingId) {
        if (serviceOfferingId != null) {
            ServiceOfferingVO serviceOffering = serviceOfferingDao.findById(serviceOfferingId);
            if (serviceOffering.getCpu() < STORAGEFSVM_MIN_CPU_COUNT.valueIn(zoneId)) {
                throw new InvalidParameterValueException("Service offering's number of cpu should be greater than or equal to " + STORAGEFSVM_MIN_CPU_COUNT.key());
            }
            if (serviceOffering.getRamSize() < STORAGEFSVM_MIN_RAM_SIZE.valueIn(zoneId)) {
                throw new InvalidParameterValueException("Service offering's ram size should be greater than or equal to " + STORAGEFSVM_MIN_RAM_SIZE.key());
            }
            if (serviceOffering.isOfferHA() == false) {
                throw new InvalidParameterValueException("Service offering's should be HA enabled");
            }
        }
        if (zoneId != null) {
            DataCenter zone = entityMgr.findById(DataCenter.class, zoneId);
            Hypervisor.HypervisorType availableHypervisor = resourceMgr.getAvailableHypervisor(zoneId);
            VMTemplateVO template = templateDao.findSystemVMReadyTemplate(zoneId, availableHypervisor);
            if (template == null) {
                throw new CloudRuntimeException(String.format("Unable to find the system templates or it was not downloaded in %s.", zone.toString()));
            }
        }
    }

    @Override
    public Pair<Long, Long> deployFileShare(FileShare fileShare, Long networkId, Long diskOfferingId, Long size) {
        Account owner = accountMgr.getActiveAccountById(fileShare.getAccountId());
        UserVm vm = deployFileShareVM(fileShare.getDataCenterId(), owner, List.of(networkId), fileShare.getName(), fileShare.getServiceOfferingId(), diskOfferingId, fileShare.getFsType(), size);

        List<VolumeVO> volumes = volumeDao.findByInstanceAndType(vm.getId(), Volume.Type.DATADISK);
        return new Pair<>(volumes.get(0).getId(), vm.getId());
    }

    @Override
    public boolean startFileShare(FileShare fileShare) {
        try {
            UserVmVO vm = userVmDao.findById(fileShare.getVmId());
            userVmManager.startVirtualMachine(vm);
        } catch (OperationTimedoutException | ResourceUnavailableException | InsufficientCapacityException ex) {
            throw new CloudRuntimeException("Failed to start VM due to exception " + ex.getMessage());
        }
        return true;
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
    public Long reDeployFileShare(FileShare fileShare) {
        Long vmId = fileShare.getVmId();
        final List<NicVO> nics = nicDao.listByVmId(vmId);
        List<Long> networkIds = new ArrayList<>();
        for (NicVO nic : nics) {
           networkIds.add(nic.getNetworkId());
        }
        Account owner = accountMgr.getActiveAccountById(fileShare.getAccountId());
        UserVm vm = deployFileShareVM(fileShare.getDataCenterId(), owner, networkIds, fileShare.getName(), fileShare.getServiceOfferingId(), null, fileShare.getFsType(), null);
        if (vmId != null) {
            try {
                expungeVm(vmId);
            } catch (Exception ex) {
                expungeVm(vm.getId());
                throw ex;
            }
        }
        VolumeVO volume = volumeDao.findById(fileShare.getVolumeId());
        volumeApiService.attachVolumeToVM(vm.getId(), volume.getId(), null, true);
        return vm.getId();
    }

    @Override
    public boolean changeFileShareDiskOffering(FileShare fileShare, Long diskOfferingId, Long newSize, Long newMinIops, Long newMaxIops) {
        DiskOfferingVO diskOfferingVO = diskOfferingDao.findById(diskOfferingId);
        try {
            volumeApiService.changeDiskOfferingForVolumeInternal(fileShare.getVolumeId(), diskOfferingId, newSize, newMinIops, newMaxIops, true, false);
        } catch (ResourceAllocationException ex) {
            throw new CloudRuntimeException("Failed to start VM due to exception " + ex.getMessage());
        }
        return true;
    }
}
