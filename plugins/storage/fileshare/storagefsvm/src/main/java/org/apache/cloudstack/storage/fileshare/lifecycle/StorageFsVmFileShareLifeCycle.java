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

import com.cloud.exception.InsufficientCapacityException;
import com.cloud.exception.OperationTimedoutException;
import com.cloud.storage.Volume;
import com.cloud.storage.VolumeApiService;
import com.cloud.storage.VolumeVO;
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
import com.cloud.vm.UserVmManager;
import com.cloud.vm.UserVmService;
import com.cloud.vm.UserVmVO;
import com.cloud.vm.dao.UserVmDao;

import org.apache.cloudstack.api.BaseCmd;
import org.apache.cloudstack.context.CallContext;
import org.apache.cloudstack.engine.cloud.entity.api.db.dao.VMNetworkMapDao;
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
    ServiceOfferingDao serviceOfferingDao;

    @Inject
    private DiskOfferingDao diskOfferingDao;

    @Inject
    protected VMNetworkMapDao vmNetworkMapDao;

    private String readResourceFile(String resource) {
        try {
            return IOUtils.toString(Objects.requireNonNull(Thread.currentThread().getContextClassLoader().getResourceAsStream(resource)), com.cloud.utils.StringUtils.getPreferredCharset());
        } catch (IOException e) {
            throw new CloudRuntimeException("Unable to read the user data resource file due to exception " + e.getMessage());
        }
    }

    private UserVm deployFileShareVM(Long zoneId, Account owner, List<Long> networkIds, String name, Long serviceOfferingId, Long diskOfferingId, Long size, Volume volume) {
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
        if (template == null) {
            throw new CloudRuntimeException(String.format("Unable to find the system templates or it was not downloaded in %s.", zone.toString()));
        }

        String suffix = Long.toHexString(System.currentTimeMillis());
        String hostName = String.format("%s-%s-%s", FileShareVmNamePrefix, name, suffix);

        Network.IpAddresses addrs = new Network.IpAddresses(null, null);
        Map<String, String> customParameterMap = new HashMap<String, String>();
        List<String> keypairs = new ArrayList<String>();

        UserVm vm;
        try {
            String fsVmConfig = readResourceFile("/conf/fsvm-init.yml");
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
    public Pair<Long, Long> deployFileShare(FileShare fileShare, Long networkId, Long diskOfferingId, Long size) {
        Account owner = accountMgr.getActiveAccountById(fileShare.getAccountId());
        UserVm vm = deployFileShareVM(fileShare.getDataCenterId(), owner, List.of(networkId), fileShare.getName(), fileShare.getServiceOfferingId(), diskOfferingId, size, null);

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
    public boolean stopFileShare(FileShare fileShare) {
        userVmManager.stopVirtualMachine(fileShare.getVmId(), false);
        return true;
    }

    private void expungeVmWithoutDeletingDataVolume(Long vmId) {
        UserVmVO userVM = userVmDao.findById(vmId);
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
            expungeVmWithoutDeletingDataVolume(vmId);
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
        Volume vol = volumeApiService.destroyVolume(volume.getId(), CallContext.current().getCallingAccount(), expunge, forceExpunge);
        if (vol == null) {
            throw new CloudRuntimeException("Failed to destroy Data volume " + volume.toString());
        }
        return true;
    }

    @Override
    public Long restartFileShare(FileShare fileShare, boolean cleanup) {
        Long vmId = fileShare.getVmId();
        List<Long> networkIds = vmNetworkMapDao.getNetworks(vmId);
        if (vmId != null) {
            expungeVmWithoutDeletingDataVolume(vmId);
        }
        VolumeVO volume = volumeDao.findById(fileShare.getVolumeId());
        Account owner = accountMgr.getActiveAccountById(fileShare.getAccountId());
        UserVm vm = deployFileShareVM(fileShare.getDataCenterId(), owner, networkIds, fileShare.getName(), fileShare.getServiceOfferingId(), null, null, null);
        volumeApiService.attachVolumeToVM(vm.getId(), volume.getId(), null, true);
        try {
            userVmManager.startVirtualMachine(vm);
        } catch (OperationTimedoutException | ResourceUnavailableException | InsufficientCapacityException ex) {
            throw new CloudRuntimeException("Failed to start VM due to exception " + ex.getMessage());
        }
        return vm.getId();
    }

    @Override
    public boolean resizeFileShare(FileShare fileShare, Long newSize) {
        return false;
    }
}
