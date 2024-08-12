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

import com.cloud.dc.dao.DataCenterDao;
import com.cloud.exception.InsufficientCapacityException;
import com.cloud.exception.InvalidParameterValueException;
import com.cloud.exception.OperationTimedoutException;
import com.cloud.exception.ResourceAllocationException;
import com.cloud.offering.ServiceOffering;
import com.cloud.storage.LaunchPermissionVO;
import com.cloud.storage.Volume;
import com.cloud.storage.VolumeApiService;
import com.cloud.storage.VolumeVO;
import com.cloud.storage.dao.LaunchPermissionDao;
import com.cloud.storage.dao.VolumeDao;
import com.cloud.uservm.UserVm;
import com.cloud.utils.FileUtil;
import com.cloud.utils.Pair;

import java.io.IOException;
import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.Iterator;
import java.util.List;
import java.util.Map;

import javax.inject.Inject;

import com.cloud.exception.ResourceUnavailableException;
import com.cloud.utils.net.NetUtils;
import com.cloud.vm.NicVO;
import com.cloud.vm.UserVmManager;
import com.cloud.vm.UserVmService;
import com.cloud.vm.UserVmVO;
import com.cloud.vm.dao.NicDao;
import com.cloud.vm.dao.UserVmDao;

import org.apache.cloudstack.api.ApiCommandResourceType;
import org.apache.cloudstack.api.BaseCmd;
import org.apache.cloudstack.context.CallContext;
import org.apache.cloudstack.storage.fileshare.FileShare;
import org.apache.cloudstack.storage.fileshare.FileShareLifeCycle;
import org.apache.commons.codec.binary.Base64;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

import com.cloud.dc.DataCenter;
import com.cloud.hypervisor.Hypervisor;
import com.cloud.network.Network;
import com.cloud.resource.ResourceManager;
import com.cloud.service.dao.ServiceOfferingDao;
import com.cloud.storage.VMTemplateVO;
import com.cloud.storage.dao.VMTemplateDao;
import com.cloud.user.Account;
import com.cloud.user.AccountManager;
import com.cloud.utils.exception.CloudRuntimeException;
import com.cloud.vm.VirtualMachineManager;

public class StorageFsVmFileShareLifeCycle implements FileShareLifeCycle {
    protected Logger logger = LogManager.getLogger(getClass());

    @Inject
    private AccountManager accountMgr;

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
    private DataCenterDao dataCenterDao;

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
    protected LaunchPermissionDao launchPermissionDao;

    private String readResourceFile(String resource) {
        try {
            return FileUtil.readResourceFile(resource);
        } catch (IOException e) {
            throw new CloudRuntimeException("Unable to read the user data resource file due to exception " + e.getMessage());
        }
    }

    private String getStorageFsVmConfig(final String fileSystem, final String hypervisorType) {
        String fsVmConfig = readResourceFile("/conf/fsvm-init.yml");
        final String filesystem = "{{ fsvm.filesystem }}";
        final String hypervisor = "{{ fsvm.hypervisor }}";
        fsVmConfig = fsVmConfig.replace(filesystem, fileSystem);
        fsVmConfig = fsVmConfig.replace(hypervisor, hypervisorType);
        return fsVmConfig;
    }

    private String getStorageFsVmName(String fileShareName) {
        String prefix = String.format("%s-%s", FileShareVmNamePrefix, fileShareName);
        String suffix = Long.toHexString(System.currentTimeMillis());

        if (!NetUtils.verifyDomainNameLabel(prefix, true)) {
            prefix = prefix.replaceAll("[^a-zA-Z0-9-]", "");
        }
        int nameLength = prefix.length() + suffix.length() + FileShareVmNamePrefix.length();
        if (nameLength > 63) {
            int prefixLength = prefix.length() - (nameLength - 63);
            prefix = prefix.substring(0, prefixLength);
        }
        return (String.format("%s-%s", prefix, suffix));
    }

    private UserVm deployFileShareVM(Long zoneId, Account owner, List<Long> networkIds, String name, Long serviceOfferingId, Long diskOfferingId, FileShare.FileSystemType fileSystem, Long size, Long minIops, Long maxIops) throws OperationTimedoutException, ResourceUnavailableException, InsufficientCapacityException, ResourceAllocationException {
        ServiceOffering serviceOffering = serviceOfferingDao.findById(serviceOfferingId);
        DataCenter zone = dataCenterDao.findById(zoneId);

        List<Hypervisor.HypervisorType> hypervisors = resourceMgr.getSupportedHypervisorTypes(zoneId, false, null);
        if (hypervisors.size() > 0) {
            Collections.shuffle(hypervisors);
        } else {
            throw new CloudRuntimeException(String.format("No supported hypervisor found for zone %s.", zone.toString()));
        }

        String hostName = getStorageFsVmName(name);
        Network.IpAddresses addrs = new Network.IpAddresses(null, null);
        Map<String, String> customParameterMap = new HashMap<String, String>();
        if (minIops != null) {
            customParameterMap.put("minIopsDo", minIops.toString());
            customParameterMap.put("maxIopsDo", maxIops.toString());
        }
        List<String> keypairs = new ArrayList<String>();

        for (final Iterator<Hypervisor.HypervisorType> iter = hypervisors.iterator(); iter.hasNext();) {
            final Hypervisor.HypervisorType hypervisor = iter.next();
            VMTemplateVO template = templateDao.findSystemVMReadyTemplate(zoneId, hypervisor);
            if (template == null && !iter.hasNext()) {
                throw new CloudRuntimeException(String.format("Unable to find the systemvm template for %s or it was not downloaded in %s.", hypervisor.toString(), zone.toString()));
            }

            LaunchPermissionVO existingPermission = launchPermissionDao.findByTemplateAndAccount(template.getId(), owner.getId());
            if (existingPermission == null) {
                LaunchPermissionVO launchPermission = new LaunchPermissionVO(template.getId(), owner.getId());
                launchPermissionDao.persist(launchPermission);
            }

            UserVm vm = null;
            String fsVmConfig = getStorageFsVmConfig(fileSystem.toString().toLowerCase(), hypervisor.toString().toLowerCase());
            String base64UserData = Base64.encodeBase64String(fsVmConfig.getBytes(com.cloud.utils.StringUtils.getPreferredCharset()));
            CallContext vmContext = CallContext.register(CallContext.current(), ApiCommandResourceType.VirtualMachine);
            try {
                vm = userVmService.createAdvancedVirtualMachine(zone, serviceOffering, template, networkIds, owner, hostName, hostName,
                        diskOfferingId, size, null, Hypervisor.HypervisorType.None, BaseCmd.HTTPMethod.POST, base64UserData,
                        null, null, keypairs, null, addrs, null, null, null,
                        customParameterMap, null, null, null, null,
                        true, UserVmManager.STORAGEFSVM, null);
                vmContext.setEventResourceId(vm.getId());
                userVmService.startVirtualMachine(vm);
            } catch (InsufficientCapacityException ex) {
                if (vm != null) {
                    expungeVm(vm.getId());
                }
                if (iter.hasNext()) {
                    continue;
                } else {
                    throw ex;
                }
            } finally {
                CallContext.unregister();
            }
            return vm;
        }
        return null;
    }

    @Override
    public void checkPrerequisites(DataCenter zone, Long serviceOfferingId) {
        ServiceOffering serviceOffering = serviceOfferingDao.findById(serviceOfferingId);
        if (serviceOffering.getCpu() < STORAGEFSVM_MIN_CPU_COUNT.valueIn(zone.getId())) {
            throw new InvalidParameterValueException("Service offering's number of cpu should be greater than or equal to " + STORAGEFSVM_MIN_CPU_COUNT.key());
        }
        if (serviceOffering.getRamSize() < STORAGEFSVM_MIN_RAM_SIZE.valueIn(zone.getId())) {
            throw new InvalidParameterValueException("Service offering's ram size should be greater than or equal to " + STORAGEFSVM_MIN_RAM_SIZE.key());
        }
        if (!serviceOffering.isOfferHA()) {
            throw new InvalidParameterValueException("Service offering's should be HA enabled");
        }
    }

    @Override
    public Pair<Long, Long> deployFileShare(FileShare fileShare, Long networkId, Long diskOfferingId, Long size, Long minIops, Long maxIops) throws ResourceUnavailableException, InsufficientCapacityException, ResourceAllocationException, OperationTimedoutException {
        Account owner = accountMgr.getActiveAccountById(fileShare.getAccountId());
        UserVm vm = deployFileShareVM(fileShare.getDataCenterId(), owner, List.of(networkId), fileShare.getName(), fileShare.getServiceOfferingId(), diskOfferingId, fileShare.getFsType(), size, minIops, maxIops);

        List<VolumeVO> volumes = volumeDao.findByInstanceAndType(vm.getId(), Volume.Type.DATADISK);
        return new Pair<>(volumes.get(0).getId(), vm.getId());
    }

    @Override
    public void startFileShare(FileShare fileShare) throws OperationTimedoutException, ResourceUnavailableException, InsufficientCapacityException {
        UserVmVO vm = userVmDao.findById(fileShare.getVmId());
        userVmService.startVirtualMachine(vm);
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
    public Pair<Boolean, Long> reDeployFileShare(FileShare fileShare) throws ResourceUnavailableException, InsufficientCapacityException, ResourceAllocationException, OperationTimedoutException {
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
            newVm = deployFileShareVM(fileShare.getDataCenterId(), owner, networkIds, fileShare.getName(), fileShare.getServiceOfferingId(), null, fileShare.getFsType(), null, null, null);
        } catch (Exception ex) {
            logger.error(String.format("Redeploy fileshare [%s]: VM deploy failed with error %s", fileShare.toString(), ex.getMessage()));
            throw ex;
        }

        Volume volume = null;
        if (!fileShare.getState().equals(FileShare.State.Detached)) {
            volume = volumeApiService.detachVolumeViaDestroyVM(oldVmId, volumeId);
            if (volume == null) {
                volume = volumeDao.findById(volumeId);
                expungeVm(newVm.getId());
                String message = String.format("Redeploy fileshare [%s]: volume %s couldn't be detached from the old VM", fileShare.toString(), volume.toString());
                logger.error(message);
                throw new CloudRuntimeException(message);
            }
        } else {
            volume = volumeDao.findById(volumeId);
        }

        volume = volumeApiService.attachVolumeToVM(newVm.getId(), volume.getId(), null, true);
        if (volume == null) {
            volume = volumeDao.findById(volumeId);
            logger.error(String.format("Redeploy fileshare [%s]: volume %s couldn't be attached to the VM %s", fileShare.toString(), volume.toString(), newVm.toString()));
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
