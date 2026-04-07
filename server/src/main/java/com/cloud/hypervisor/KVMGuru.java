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
package com.cloud.hypervisor;

import com.cloud.agent.api.Command;
import com.cloud.agent.api.to.DataObjectType;
import com.cloud.agent.api.to.NicTO;
import com.cloud.agent.api.to.VirtualMachineTO;
import com.cloud.capacity.CapacityManager;
import com.cloud.event.EventTypes;
import com.cloud.event.UsageEventUtils;
import com.cloud.host.HostVO;
import com.cloud.hypervisor.Hypervisor.HypervisorType;
import com.cloud.hypervisor.dao.HypervisorCapabilitiesDao;
import com.cloud.hypervisor.kvm.dpdk.DpdkHelper;
import com.cloud.service.ServiceOfferingVO;
import com.cloud.storage.DataStoreRole;
import com.cloud.storage.GuestOSHypervisorVO;
import com.cloud.storage.GuestOSVO;
import com.cloud.storage.Volume;
import com.cloud.storage.VolumeVO;
import com.cloud.storage.dao.GuestOSDao;
import com.cloud.storage.dao.GuestOSHypervisorDao;
import com.cloud.storage.dao.VolumeDao;
import com.cloud.utils.Pair;
import com.cloud.utils.exception.CloudRuntimeException;
import com.cloud.vm.UserVmManager;
import com.cloud.vm.VMInstanceVO;
import com.cloud.vm.VirtualMachine;
import com.cloud.vm.VirtualMachineProfile;
import com.cloud.vm.dao.VMInstanceDao;
import org.apache.cloudstack.backup.Backup;
import org.apache.cloudstack.framework.config.ConfigKey;
import org.apache.cloudstack.storage.command.CopyCommand;
import org.apache.cloudstack.storage.command.StorageSubSystemCommand;
import org.apache.cloudstack.utils.reflectiontostringbuilderutils.ReflectionToStringBuilderUtils;
import org.apache.commons.collections.MapUtils;
import javax.inject.Inject;
import java.math.BigDecimal;
import java.math.RoundingMode;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import org.apache.cloudstack.utils.bytescale.ByteScaleUtils;

public class KVMGuru extends HypervisorGuruBase implements HypervisorGuru {
    @Inject
    GuestOSDao _guestOsDao;
    @Inject
    GuestOSHypervisorDao _guestOsHypervisorDao;
    @Inject
    DpdkHelper dpdkHelper;
    @Inject
    VMInstanceDao _instanceDao;
    @Inject
    VolumeDao _volumeDao;
    @Inject
    HypervisorCapabilitiesDao _hypervisorCapabilitiesDao;


    @Override
    public HypervisorType getHypervisorType() {
        return HypervisorType.KVM;
    }

    protected KVMGuru() {
        super();
    }

    /**
     * Get next free DeviceId for a KVM Guest
     */

    protected Long getNextAvailableDeviceId(List<VolumeVO> vmVolumes) {

        int maxDataVolumesSupported;
        int maxDeviceId;
        List<String> devIds = new ArrayList<>();

        try {
            maxDataVolumesSupported = _hypervisorCapabilitiesDao.getMaxDataVolumesLimit(HypervisorType.KVM,"default");
            int maxDevices = maxDataVolumesSupported + 2; // add 2 to consider devices root volume and cdrom
            maxDeviceId = maxDevices - 1;
        } catch (Exception e) {
            throw new RuntimeException("Cannot find maximum number of disk devices that can be attached to the KVM Hypervisor");
        }
        for (int i = 1; i <= maxDeviceId; i++) {
            devIds.add(String.valueOf(i));
        }
        devIds.remove("3");
        for (VolumeVO vmVolume : vmVolumes) {
            devIds.remove(vmVolume.getDeviceId().toString().trim());
        }
        if (devIds.isEmpty()) {
            throw new RuntimeException("All device Ids are in use.");
        }
        return Long.parseLong(devIds.iterator().next());
    }


    /**
     * Retrieve host max CPU speed
     */
    protected double getHostCPUSpeed(HostVO host) {
        return host.getSpeed();
    }

    protected double getVmSpeed(VirtualMachineTO to) {
        return to.getMaxSpeed() != null ? to.getMaxSpeed() : to.getSpeed();
    }

    /**
     * Set VM CPU quota percentage with respect to host CPU on 'to' if CPU limit option is set
     * @param to vm to
     * @param vmProfile vm profile
     */
    protected void setVmQuotaPercentage(VirtualMachineTO to, VirtualMachineProfile vmProfile) {
        if (!to.isLimitCpuUse()) {
            return;
        }

        VirtualMachine vm = vmProfile.getVirtualMachine();
        HostVO host = hostDao.findById(vm.getHostId());
        if (host == null) {
            logger.warn("Host is not available. Skipping setting CPU quota percentage for VM: [{}].", vm);
            return;
        }

        logger.debug("Limiting CPU usage for VM: [{}] on host: [{}].", vm, host);
        double maxSpeed = getVmSpeed(to);
        double hostMaxSpeed = getHostCPUSpeed(host);
        Double cpuQuotaPercentage = getCpuQuotaPercentage(maxSpeed, hostMaxSpeed);
        if (cpuQuotaPercentage != null) {
            to.setCpuQuotaPercentage(cpuQuotaPercentage);
        }
    }

    /**
     * Calculates the VM quota percentage based on the VM and host CPU speeds.
     * @param vmSpeeed Speed of the VM.
     * @param hostSpeed Speed of the host.
     * @return The VM quota percentage.
     */
    public Double getCpuQuotaPercentage(double vmSpeeed, double hostSpeed) {
        logger.debug("Calculating CPU quota percentage for VM with speed [{}] on host with speed [{}].", vmSpeeed, hostSpeed);
        try {
            BigDecimal percent = new BigDecimal(vmSpeeed / hostSpeed);
            percent = percent.setScale(2, RoundingMode.HALF_DOWN);
            if (percent.compareTo(new BigDecimal(1)) > 0) {
                logger.debug("VM CPU speed exceeded host CPU speed and, therefore, limiting VM CPU quota to the host maximum.");
                percent = new BigDecimal(1);
            }
            double quotaPercentage = percent.doubleValue();
            logger.info("Calculated CPU quota percentage for VM with speed [{}] on host with speed [{}] is [{}].", vmSpeeed, hostSpeed, quotaPercentage);
            return quotaPercentage;
        } catch (NumberFormatException e) {
            logger.info("Could not calculate CPU quota percentage for VM with speed [{}] on host with speed [{}]. Therefore, CPU limitation will not be set for the domain.", vmSpeeed, hostSpeed);
            return null;
        }
    }

    @Override
    public VirtualMachineTO implement(VirtualMachineProfile vm) {
        VirtualMachineTO to = toVirtualMachineTO(vm);
        setVmQuotaPercentage(to, vm);

        enableDpdkIfNeeded(vm, to);

        VirtualMachine virtualMachine = vm.getVirtualMachine();
        Long hostId = virtualMachine.getHostId();
        HostVO host = hostId == null ? null : hostDao.findById(hostId);

        // Determine the VM's OS description
        configureVmOsDescription(virtualMachine, to, host);

        configureVmMemoryAndCpuCores(to, host, virtualMachine, vm);

        to.setMetadata(makeVirtualMachineMetadata(vm));

        return to;
    }

    protected void configureVmOsDescription(VirtualMachine virtualMachine, VirtualMachineTO virtualMachineTo, HostVO hostVo) {
        GuestOSVO guestOS = _guestOsDao.findByIdIncludingRemoved(virtualMachine.getGuestOSId());
        String guestOsDisplayName = guestOS.getDisplayName();
        virtualMachineTo.setOs(guestOsDisplayName);
        GuestOSHypervisorVO guestOsMapping = null;

        if (hostVo != null) {
            guestOsMapping = _guestOsHypervisorDao.findByOsIdAndHypervisor(guestOS.getId(), getHypervisorType().toString(), hostVo.getHypervisorVersion());
        }

        if (guestOsMapping == null || hostVo == null) {
            virtualMachineTo.setPlatformEmulator(guestOsDisplayName == null ? "Other" : guestOsDisplayName);
        } else {
            virtualMachineTo.setPlatformEmulator(guestOsMapping.getGuestOsName());
        }
    }

    protected void enableDpdkIfNeeded(VirtualMachineProfile virtualMachineProfile, VirtualMachineTO virtualMachineTo) {
        if (dpdkHelper.isDpdkvHostUserModeSettingOnServiceOffering(virtualMachineProfile)) {
            dpdkHelper.setDpdkVhostUserMode(virtualMachineTo, virtualMachineProfile);
        }

        if (virtualMachineTo.getType() == VirtualMachine.Type.User && MapUtils.isNotEmpty(virtualMachineTo.getExtraConfig()) &&
                virtualMachineTo.getExtraConfig().containsKey(DpdkHelper.DPDK_NUMA) && virtualMachineTo.getExtraConfig().containsKey(DpdkHelper.DPDK_HUGE_PAGES)) {
            for (final NicTO nic : virtualMachineTo.getNics()) {
                nic.setDpdkEnabled(true);
            }
        }
    }

    protected void configureVmMemoryAndCpuCores(VirtualMachineTO virtualMachineTo, HostVO hostVo, VirtualMachine virtualMachine, VirtualMachineProfile virtualMachineProfile) {
        String vmDescription = virtualMachineTo.toString();

        Pair<Long, Integer> max = getHostMaxMemoryAndCpuCores(hostVo, virtualMachine, vmDescription);

        Long maxHostMemory = max.first();
        Integer maxHostCpuCores = max.second();

        long minMemory = virtualMachineTo.getMinRam();
        Long maxMemory = virtualMachineTo.getMaxRam();
        long requestedMemory = maxMemory;

        int minCpuCores = virtualMachineTo.getCpus();
        int maxCpuCores = minCpuCores;

        if (isVmDynamicScalable(virtualMachineTo, virtualMachine)) {
            ServiceOfferingVO serviceOfferingVO = serviceOfferingDao.findById(virtualMachineProfile.getId(), virtualMachineProfile.getServiceOfferingId());
            serviceOfferingDao.loadDetails(serviceOfferingVO);

            Long clusterId = hostVo != null ? hostVo.getClusterId() : null;
            maxMemory = getVmMaxMemory(serviceOfferingVO, vmDescription, maxHostMemory, clusterId);
            maxCpuCores = getVmMaxCpuCores(serviceOfferingVO, vmDescription, maxHostCpuCores, clusterId);
        }

        virtualMachineTo.setRam(minMemory, maxMemory, requestedMemory);
        virtualMachineTo.setCpus(minCpuCores);
        virtualMachineTo.setVcpuMaxLimit(maxCpuCores);
    }

    protected boolean isVmDynamicScalable(VirtualMachineTO virtualMachineTo, VirtualMachine virtualMachine) {
        return virtualMachineTo.isEnableDynamicallyScaleVm() && UserVmManager.EnableDynamicallyScaleVm.valueIn(virtualMachine.getDataCenterId());
    }

    protected Pair<Long, Integer> getHostMaxMemoryAndCpuCores(HostVO host, VirtualMachine virtualMachine, String vmDescription){
        Long maxHostMemory = Long.MAX_VALUE;
        Integer maxHostCpuCore = Integer.MAX_VALUE;

        if (host != null) {
            return new Pair<>(host.getTotalMemory(), host.getCpus());
        }

        Long lastHostId = virtualMachine.getLastHostId();

        HostVO lastHost = lastHostId == null ? null : hostDao.findById(lastHostId);
        logger.info("{} is not running; therefore, we use the last host [{}] with id {} that the " +
                        "VM was running on to derive the unconstrained service offering max CPU " +
                "and memory.", vmDescription, lastHost, lastHostId);
        if (lastHost != null) {
            maxHostMemory = lastHost.getTotalMemory();
            maxHostCpuCore = lastHost.getCpus();
            logger.debug(String.format("Retrieved memory and cpu max values {\"memory\": %s, \"cpu\": %s} from %s last %s.", maxHostMemory, maxHostCpuCore, vmDescription, lastHost));
        } else {
            logger.warn(String.format("%s host [%s] and last host [%s] are null. Using 'Long.MAX_VALUE' [%s] and 'Integer.MAX_VALUE' [%s] as max memory and cpu cores.", vmDescription, virtualMachine.getHostId(), lastHostId, maxHostMemory, maxHostCpuCore));
        }

        return new Pair<>(maxHostMemory, maxHostCpuCore);
    }

    protected Long getVmMaxMemory(ServiceOfferingVO serviceOfferingVO, String vmDescription, Long maxHostMemory, Long clusterId) {
        Long maxMemory;
        ConfigKey<Integer> maxMemoryConfig = CapacityManager.KvmMemoryDynamicScalingCapacity;
        Integer maxMemoryConfigValue = maxMemoryConfig.valueIn(clusterId);
        logger.info("[{}] is a dynamically scalable service offering. Using config [{}] value [{}] in cluster [ID: {}] as max [{}] memory.",
                serviceOfferingVO.toString(), maxMemoryConfig.key(), maxMemoryConfigValue, clusterId, vmDescription);
        if (maxMemoryConfigValue > 0) {
            maxMemory = ByteScaleUtils.mebibytesToBytes(maxMemoryConfigValue);
        } else {
            logger.info("Config [{}] in cluster [ID: {}] has value less or equal '0'. Using [{}] host or last host max memory [{}] as VM max memory in the hypervisor.",
                    maxMemoryConfig.key(), clusterId, vmDescription, maxHostMemory);
            maxMemory = maxHostMemory;
        }
        return maxMemory;
    }

    protected Integer getVmMaxCpuCores(ServiceOfferingVO serviceOfferingVO, String vmDescription, Integer maxHostCpuCores, Long clusterId) {
        Integer maxCpuCores;
        ConfigKey<Integer> maxCpuCoresConfig = CapacityManager.KvmCpuDynamicScalingCapacity;
        Integer maxCpuCoresConfigValue = maxCpuCoresConfig.valueIn(clusterId);
        logger.info("[{}] is a dynamically scalable service offering. Using config [{}] value [{}] in cluster [ID: {}] as max [{}] CPU cores.",
                serviceOfferingVO.toString(), maxCpuCoresConfig.key(), maxCpuCoresConfigValue, clusterId, vmDescription);
        if (maxCpuCoresConfigValue > 0) {
            maxCpuCores = maxCpuCoresConfigValue;
        } else {
            logger.info("Config [{}] in cluster [ID: {}] has value less or equal '0'. Using [{}] host or last host max CPU cores [{}] as VM CPU cores in the hypervisor.",
                    maxCpuCoresConfig.key(), clusterId, vmDescription, maxHostCpuCores);
            maxCpuCores = maxHostCpuCores;
        }
        return maxCpuCores;
    }

    @Override
    public Pair<Boolean, Long> getCommandHostDelegation(long hostId, Command cmd) {
        if (cmd instanceof StorageSubSystemCommand) {
            StorageSubSystemCommand c = (StorageSubSystemCommand)cmd;
            c.setExecuteInSequence(false);
        }
        if (cmd instanceof CopyCommand) {
            CopyCommand c = (CopyCommand) cmd;
            boolean inSeq = true;
            if (c.getSrcTO().getObjectType() == DataObjectType.SNAPSHOT ||
                    c.getDestTO().getObjectType() == DataObjectType.SNAPSHOT) {
                inSeq = false;
            } else if (c.getDestTO().getDataStore().getRole() == DataStoreRole.Image ||
                    c.getDestTO().getDataStore().getRole() == DataStoreRole.ImageCache) {
                inSeq = false;
            }
            c.setExecuteInSequence(inSeq);
            if (c.getSrcTO().getHypervisorType() == HypervisorType.KVM) {
                return new Pair<>(true, hostId);
            }
        }
        return new Pair<>(false, hostId);
    }

    @Override
    public boolean trackVmHostChange() {
        return false;
    }

    @Override
    public Map<String, String> getClusterSettings(long vmId) {
        return null;
    }

    @Override
    public VirtualMachine importVirtualMachineFromBackup(long zoneId, long domainId, long accountId, long userId, String vmInternalName, Backup backup)  {
        logger.debug(String.format("Trying to import VM [vmInternalName: %s] from Backup [%s].", vmInternalName,
                ReflectionToStringBuilderUtils.reflectOnlySelectedFields(backup, "id", "uuid", "vmId", "externalId", "backupType")));

        VMInstanceVO vm = _instanceDao.findVMByInstanceNameIncludingRemoved(vmInternalName);
        if (vm == null) {
            throw new CloudRuntimeException("Cannot find VM: " + vmInternalName);
        }
        try {
            if (vm.getRemoved() == null) {
                vm.setState(VirtualMachine.State.Stopped);
                vm.setPowerState(VirtualMachine.PowerState.PowerOff);
                _instanceDao.update(vm.getId(), vm);
            }
           for (Backup.VolumeInfo VMVolToRestore : vm.getBackupVolumeList()) {
               VolumeVO volume = _volumeDao.findByUuidIncludingRemoved(VMVolToRestore.getUuid());
               volume.setState(Volume.State.Ready);
               _volumeDao.update(volume.getId(), volume);
               if (VMVolToRestore.getType() == Volume.Type.ROOT) {
                   _volumeDao.update(volume.getId(), volume);
                   _volumeDao.attachVolume(volume.getId(), vm.getId(), 0L);
               } else if (VMVolToRestore.getType() == Volume.Type.DATADISK) {
                   List<VolumeVO> vmVolumes = _volumeDao.findByInstance(vm.getId());
                   _volumeDao.update(volume.getId(), volume);
                   _volumeDao.attachVolume(volume.getId(), vm.getId(), getNextAvailableDeviceId(vmVolumes));
               }
               UsageEventUtils.publishUsageEvent(EventTypes.EVENT_VOLUME_ATTACH, volume.getAccountId(), volume.getDataCenterId(), volume.getId(), volume.getName(),
                       volume.getDiskOfferingId(), volume.getTemplateId(), volume.getSize(), Volume.class.getName(), volume.getUuid(), vm.getId(), volume.isDisplay());
           }
        } catch (Exception e) {
            throw new RuntimeException("Could not restore VM " + vm.getName() + " due to : " + e.getMessage());
        }
    return vm;
    }

    @Override public boolean attachRestoredVolumeToVirtualMachine(long zoneId, String location, Backup.VolumeInfo volumeInfo, VirtualMachine vm, long poolId, Backup backup) {

        VMInstanceVO targetVM = _instanceDao.findVMByInstanceNameIncludingRemoved(vm.getName());
        List<VolumeVO> vmVolumes = _volumeDao.findByInstance(targetVM.getId());
        VolumeVO restoredVolume = _volumeDao.findByUuid(location);
        if (restoredVolume != null) {
            try {
                _volumeDao.attachVolume(restoredVolume.getId(), vm.getId(), getNextAvailableDeviceId(vmVolumes));
                restoredVolume.setState(Volume.State.Ready);
                _volumeDao.update(restoredVolume.getId(), restoredVolume);
                UsageEventUtils.publishUsageEvent(EventTypes.EVENT_VOLUME_ATTACH, restoredVolume.getAccountId(), restoredVolume.getDataCenterId(), restoredVolume.getId(), restoredVolume.getName(),
                        restoredVolume.getDiskOfferingId(), restoredVolume.getTemplateId(), restoredVolume.getSize(), Volume.class.getName(), restoredVolume.getUuid(), vm.getId(), restoredVolume.isDisplay());
                return true;
            } catch (Exception e) {
                restoredVolume.setDisplay(false);
                restoredVolume.setDisplayVolume(false);
                restoredVolume.setState(Volume.State.Destroy);
                _volumeDao.update(restoredVolume.getId(), restoredVolume);
                throw new RuntimeException("Unable to attach volume " + restoredVolume.getName() + " to VM" + vm.getName() + " due to : " + e.getMessage());
            }
        }
    return false;
    }
}
