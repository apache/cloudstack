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
import com.cloud.configuration.ConfigurationManagerImpl;
import com.cloud.host.HostVO;
import com.cloud.host.dao.HostDao;
import com.cloud.hypervisor.Hypervisor.HypervisorType;
import com.cloud.hypervisor.kvm.dpdk.DpdkHelper;
import com.cloud.service.ServiceOfferingVO;
import com.cloud.service.dao.ServiceOfferingDao;
import com.cloud.storage.DataStoreRole;
import com.cloud.storage.GuestOSHypervisorVO;
import com.cloud.storage.GuestOSVO;
import com.cloud.storage.dao.GuestOSDao;
import com.cloud.storage.dao.GuestOSHypervisorDao;
import com.cloud.utils.Pair;
import com.cloud.utils.exception.CloudRuntimeException;
import com.cloud.vm.VirtualMachine;
import com.cloud.vm.VirtualMachineProfile;
import org.apache.cloudstack.storage.command.CopyCommand;
import org.apache.cloudstack.storage.command.StorageSubSystemCommand;
import org.apache.commons.collections.MapUtils;
import org.apache.log4j.Logger;

import javax.inject.Inject;
import java.math.BigDecimal;
import java.math.RoundingMode;
import java.util.Map;
import org.apache.cloudstack.api.ApiConstants;
import org.apache.cloudstack.utils.bytescale.ByteScaleUtils;
import org.apache.commons.lang3.math.NumberUtils;

public class KVMGuru extends HypervisorGuruBase implements HypervisorGuru {
    @Inject
    GuestOSDao _guestOsDao;
    @Inject
    GuestOSHypervisorDao _guestOsHypervisorDao;
    @Inject
    HostDao _hostDao;
    @Inject
    DpdkHelper dpdkHelper;

    @Inject
    ServiceOfferingDao serviceOfferingDao;

    public static final Logger s_logger = Logger.getLogger(KVMGuru.class);

    @Override
    public HypervisorType getHypervisorType() {
        return HypervisorType.KVM;
    }

    protected KVMGuru() {
        super();
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
        if (to.getLimitCpuUse()) {
            VirtualMachine vm = vmProfile.getVirtualMachine();
            HostVO host = _hostDao.findById(vm.getHostId());
            if (host == null) {
                throw new CloudRuntimeException("Host with id: " + vm.getHostId() + " not found");
            }
            s_logger.debug("Limiting CPU usage for VM: " + vm.getUuid() + " on host: " + host.getUuid());
            double hostMaxSpeed = getHostCPUSpeed(host);
            double maxSpeed = getVmSpeed(to);
            try {
                BigDecimal percent = new BigDecimal(maxSpeed / hostMaxSpeed);
                percent = percent.setScale(2, RoundingMode.HALF_DOWN);
                if (percent.compareTo(new BigDecimal(1)) == 1) {
                    s_logger.debug("VM " + vm.getUuid() + " CPU MHz exceeded host " + host.getUuid() + " CPU MHz, limiting VM CPU to the host maximum");
                    percent = new BigDecimal(1);
                }
                to.setCpuQuotaPercentage(percent.doubleValue());
                s_logger.debug("Host: " + host.getUuid() + " max CPU speed = " + hostMaxSpeed + "MHz, VM: " + vm.getUuid() +
                        "max CPU speed = " + maxSpeed + "MHz. Setting CPU quota percentage as: " + percent.doubleValue());
            } catch (NumberFormatException e) {
                s_logger.error("Error calculating VM: " + vm.getUuid() + " quota percentage, it wll not be set. Error: " + e.getMessage(), e);
            }
        }
    }

    @Override

    public VirtualMachineTO implement(VirtualMachineProfile vm) {
        VirtualMachineTO to = toVirtualMachineTO(vm);
        setVmQuotaPercentage(to, vm);

        enableDpdkIfNeeded(vm, to);

        VirtualMachine virtualMachine = vm.getVirtualMachine();
        Long hostId = virtualMachine.getHostId();
        HostVO host = hostId == null ? null : _hostDao.findById(hostId);

        // Determine the VM's OS description
        configureVmOsDescription(virtualMachine, to, host);

        configureVmMemoryAndCpuCores(to, host, virtualMachine, vm);
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
        Integer maxHostCpuCore = max.second();

        Long minMemory = virtualMachineTo.getMinRam();
        Long maxMemory = minMemory;
        Integer minCpuCores = virtualMachineTo.getCpus();
        Integer maxCpuCores = minCpuCores;

        ServiceOfferingVO serviceOfferingVO = serviceOfferingDao.findById(virtualMachineProfile.getId(), virtualMachineProfile.getServiceOfferingId());
        if (serviceOfferingVO.isDynamic()) {
            serviceOfferingDao.loadDetails(serviceOfferingVO);

            maxMemory = getVmMaxMemory(serviceOfferingVO, vmDescription, maxHostMemory);
            maxCpuCores = getVmMaxCpuCores(serviceOfferingVO, vmDescription, maxHostCpuCore);
        }

        virtualMachineTo.setRam(minMemory, maxMemory);
        virtualMachineTo.setCpus(minCpuCores);
        virtualMachineTo.setVcpuMaxLimit(maxCpuCores);
    }

    protected Pair<Long, Integer> getHostMaxMemoryAndCpuCores(HostVO host, VirtualMachine virtualMachine, String vmDescription){
        Long maxHostMemory = Long.MAX_VALUE;
        Integer maxHostCpuCore = Integer.MAX_VALUE;

        if (host != null) {
            return new Pair<>(host.getTotalMemory(), host.getCpus());
        }

        Long lastHostId = virtualMachine.getLastHostId();
        s_logger.info(String.format("%s is not running; therefore, we use the last host [%s] that the VM was running on to derive the unconstrained service offering max CPU and memory.", vmDescription, lastHostId));

        HostVO lastHost = lastHostId == null ? null : _hostDao.findById(lastHostId);
        if (lastHost != null) {
            maxHostMemory = lastHost.getTotalMemory();
            maxHostCpuCore = lastHost.getCpus();
            s_logger.debug(String.format("Retrieved memory and cpu max values {\"memory\": %s, \"cpu\": %s} from %s last %s.", maxHostMemory, maxHostCpuCore, vmDescription, lastHost.toString()));
        } else {
            s_logger.warn(String.format("%s host [%s] and last host [%s] are null. Using 'Long.MAX_VALUE' [%s] and 'Integer.MAX_VALUE' [%s] as max memory and cpu cores.", vmDescription, virtualMachine.getHostId(), lastHostId, maxHostMemory, maxHostCpuCore));
        }

        return new Pair<>(maxHostMemory, maxHostCpuCore);
    }

    protected Long getVmMaxMemory(ServiceOfferingVO serviceOfferingVO, String vmDescription, Long maxHostMemory) {
        String serviceOfferingDescription = serviceOfferingVO.toString();

        Long maxMemory;
        Integer customOfferingMaxMemory = NumberUtils.createInteger(serviceOfferingVO.getDetail(ApiConstants.MAX_MEMORY));
        Integer maxMemoryConfig = ConfigurationManagerImpl.VM_SERVICE_OFFERING_MAX_RAM_SIZE.value();
        if (customOfferingMaxMemory != null) {
            s_logger.debug(String.format("Using 'Custom unconstrained' %s max memory value [%sMb] as %s memory.", serviceOfferingDescription, customOfferingMaxMemory, vmDescription));
            maxMemory = ByteScaleUtils.mibToBytes(customOfferingMaxMemory);
        } else {
            String maxMemoryConfigKey = ConfigurationManagerImpl.VM_SERVICE_OFFERING_MAX_RAM_SIZE.key();

            s_logger.info(String.format("%s is a 'Custom unconstrained' service offering. Using config [%s] value [%s] as max %s memory.",
              serviceOfferingDescription, maxMemoryConfigKey, maxMemoryConfig, vmDescription));

            if (maxMemoryConfig > 0) {
                maxMemory = ByteScaleUtils.mibToBytes(maxMemoryConfig);
            } else {
                s_logger.info(String.format("Config [%s] has value less or equal '0'. Using %s host or last host max memory [%s] as VM max memory in the hypervisor.", maxMemoryConfigKey, vmDescription, maxHostMemory));
                maxMemory = maxHostMemory;
            }
        }
        return maxMemory;
    }

    protected Integer getVmMaxCpuCores(ServiceOfferingVO serviceOfferingVO, String vmDescription, Integer maxHostCpuCore) {
        String serviceOfferingDescription = serviceOfferingVO.toString();

        Integer maxCpuCores;
        Integer customOfferingMaxCpuCores = NumberUtils.createInteger(serviceOfferingVO.getDetail(ApiConstants.MAX_CPU_NUMBER));
        Integer maxCpuCoresConfig = ConfigurationManagerImpl.VM_SERVICE_OFFERING_MAX_CPU_CORES.value();

        if (customOfferingMaxCpuCores != null) {
            s_logger.debug(String.format("Using 'Custom unconstrained' %s max cpu cores [%s] as %s cpu cores.", serviceOfferingDescription, customOfferingMaxCpuCores, vmDescription));
            maxCpuCores = customOfferingMaxCpuCores;
        } else {
            String maxCpuCoreConfigKey = ConfigurationManagerImpl.VM_SERVICE_OFFERING_MAX_CPU_CORES.key();

            s_logger.info(String.format("%s is a 'Custom unconstrained' service offering. Using config [%s] value [%s] as max %s cpu cores.",
              serviceOfferingDescription, maxCpuCoreConfigKey, maxCpuCoresConfig, vmDescription));

            if (maxCpuCoresConfig > 0) {
                maxCpuCores = maxCpuCoresConfig;
            } else {
                s_logger.info(String.format("Config [%s] has value less or equal '0'. Using %s host or last host max cpu cores [%s] as VM cpu cores in the hypervisor.", maxCpuCoreConfigKey, vmDescription, maxHostCpuCore));
                maxCpuCores = maxHostCpuCore;
            }
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

}
