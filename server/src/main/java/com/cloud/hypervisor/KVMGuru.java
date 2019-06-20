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
import com.cloud.agent.api.to.VirtualMachineTO;
import com.cloud.host.HostVO;
import com.cloud.host.dao.HostDao;
import com.cloud.hypervisor.Hypervisor.HypervisorType;
import com.cloud.hypervisor.kvm.dpdk.DPDKHelper;
import com.cloud.offering.ServiceOffering;
import com.cloud.service.ServiceOfferingDetailsVO;
import com.cloud.storage.DataStoreRole;
import com.cloud.storage.GuestOSHypervisorVO;
import com.cloud.storage.GuestOSVO;
import com.cloud.storage.dao.GuestOSDao;
import com.cloud.storage.dao.GuestOSHypervisorDao;
import com.cloud.utils.Pair;
import com.cloud.utils.exception.CloudRuntimeException;
import com.cloud.vm.VirtualMachine;
import com.cloud.vm.VirtualMachineProfile;
import org.apache.cloudstack.api.ApiConstants;
import org.apache.cloudstack.storage.command.CopyCommand;
import org.apache.cloudstack.storage.command.StorageSubSystemCommand;
import org.apache.commons.collections.CollectionUtils;
import org.apache.log4j.Logger;

import javax.inject.Inject;
import java.math.BigDecimal;
import java.math.RoundingMode;
import java.util.List;
import java.util.Map;

public class KVMGuru extends HypervisorGuruBase implements HypervisorGuru {
    @Inject
    GuestOSDao _guestOsDao;
    @Inject
    GuestOSHypervisorDao _guestOsHypervisorDao;
    @Inject
    HostDao _hostDao;
    @Inject
    DPDKHelper dpdkHelper;

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
        addServiceOfferingExtraConfiguration(to, vm);

        if (dpdkHelper.isDPDKvHostUserModeSettingOnServiceOffering(vm)) {
            dpdkHelper.setDpdkVhostUserMode(to, vm);
        }

        // Determine the VM's OS description
        GuestOSVO guestOS = _guestOsDao.findByIdIncludingRemoved(vm.getVirtualMachine().getGuestOSId());
        to.setOs(guestOS.getDisplayName());
        HostVO host = _hostDao.findById(vm.getVirtualMachine().getHostId());
        GuestOSHypervisorVO guestOsMapping = null;
        if (host != null) {
            guestOsMapping = _guestOsHypervisorDao.findByOsIdAndHypervisor(guestOS.getId(), getHypervisorType().toString(), host.getHypervisorVersion());
        }
        if (guestOsMapping == null || host == null) {
            to.setPlatformEmulator("Other");
        } else {
            to.setPlatformEmulator(guestOsMapping.getGuestOsName());
        }

        return to;
    }

    /**
     * Add extra configurations from service offering to the VM TO.
     * Extra configuration keys are expected in formats:
     * - "extraconfig-N"
     * - "extraconfig-CONFIG_NAME"
     */
    protected void addServiceOfferingExtraConfiguration(VirtualMachineTO to, VirtualMachineProfile vmProfile) {
        ServiceOffering offering = vmProfile.getServiceOffering();
        List<ServiceOfferingDetailsVO> details = _serviceOfferingDetailsDao.listDetails(offering.getId());
        if (CollectionUtils.isNotEmpty(details)) {
            for (ServiceOfferingDetailsVO detail : details) {
                if (detail.getName().startsWith(ApiConstants.EXTRA_CONFIG)) {
                    to.addExtraConfig(detail.getName(), detail.getValue());
                }
            }
        }
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
