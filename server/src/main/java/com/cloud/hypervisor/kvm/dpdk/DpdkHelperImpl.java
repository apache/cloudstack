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
package com.cloud.hypervisor.kvm.dpdk;

import com.cloud.agent.api.to.VirtualMachineTO;
import com.cloud.host.HostVO;
import com.cloud.host.dao.HostDao;
import com.cloud.offering.ServiceOffering;
import com.cloud.service.ServiceOfferingDetailsVO;
import com.cloud.service.dao.ServiceOfferingDetailsDao;
import com.cloud.utils.exception.CloudRuntimeException;
import com.cloud.vm.UserVmDetailVO;
import com.cloud.vm.VMInstanceVO;
import com.cloud.vm.VirtualMachine;
import com.cloud.vm.VirtualMachineProfile;
import com.cloud.vm.dao.UserVmDetailsDao;
import com.cloud.vm.dao.VMInstanceDao;
import org.apache.commons.collections.CollectionUtils;
import org.apache.commons.lang3.StringUtils;
import org.apache.logging.log4j.Logger;
import org.apache.logging.log4j.LogManager;

import javax.inject.Inject;
import java.util.List;

public class DpdkHelperImpl implements DpdkHelper {

    @Inject
    private ServiceOfferingDetailsDao serviceOfferingDetailsDao;
    @Inject
    private HostDao hostDao;
    @Inject
    private VMInstanceDao vmInstanceDao;
    @Inject
    private UserVmDetailsDao userVmDetailsDao;

    protected Logger logger = LogManager.getLogger(getClass());

    private ServiceOffering getServiceOfferingFromVMProfile(VirtualMachineProfile virtualMachineProfile) {
        ServiceOffering offering = virtualMachineProfile.getServiceOffering();
        if (offering == null) {
            throw new CloudRuntimeException("VM does not have an associated service offering");
        }
        return offering;
    }

    @Override
    public boolean isDpdkvHostUserModeSettingOnServiceOffering(VirtualMachineProfile vm) {
        ServiceOffering offering = getServiceOfferingFromVMProfile(vm);
        ServiceOfferingDetailsVO detail = serviceOfferingDetailsDao.findDetail(offering.getId(), DPDK_VHOST_USER_MODE);
        return detail != null;
    }

    @Override
    public void setDpdkVhostUserMode(VirtualMachineTO to, VirtualMachineProfile vm) {
        ServiceOffering offering = getServiceOfferingFromVMProfile(vm);
        ServiceOfferingDetailsVO detail = serviceOfferingDetailsDao.findDetail(offering.getId(), DPDK_VHOST_USER_MODE);
        if (detail != null) {
            String mode = detail.getValue();
            try {
                VHostUserMode dpdKvHostUserMode = VHostUserMode.fromValue(mode);
                to.addExtraConfig(DPDK_VHOST_USER_MODE, dpdKvHostUserMode.toString());
            } catch (IllegalArgumentException e) {
                logger.error(String.format("DPDK vHost User mode found as a detail for service offering: %s " +
                                "but value: %s is not supported. Supported values: %s, %s",
                        offering.getId(), mode,
                        VHostUserMode.CLIENT.toString(), VHostUserMode.SERVER.toString()));
            }
        }
    }

    @Override
    public boolean isVMDpdkEnabled(long vmId) {
        VMInstanceVO vm = vmInstanceDao.findById(vmId);
        if (vm == null) {
            throw new CloudRuntimeException("Could not find VM with id " + vmId);
        }

        if (vm.getType() != VirtualMachine.Type.User) {
            return false;
        }

        List<UserVmDetailVO> details = userVmDetailsDao.listDetails(vm.getId());
        List<ServiceOfferingDetailsVO> offeringDetails = serviceOfferingDetailsDao.listDetails(vm.getServiceOfferingId());

        if (!hasRequiredDPDKConfigurations(details, offeringDetails)) {
            return false;
        }

        return isHostDpdkEnabled(vm.getHostId());
    }

    /**
     * True if VM is DPDK enabled. NUMA and HUGEPAGES configurations must be present on VM or service offering details
     */
    private boolean hasRequiredDPDKConfigurations(List<UserVmDetailVO> details, List<ServiceOfferingDetailsVO> offeringDetails) {
        if (CollectionUtils.isEmpty(details)) {
            return hasValidDPDKConfigurationsOnServiceOffering(false, false, offeringDetails);
        } else {
            boolean isNumaSet = false;
            boolean isHugePagesSet = false;
            for (UserVmDetailVO detail : details) {
                if (detail.getName().equals(DPDK_NUMA)) {
                    isNumaSet = true;
                } else if (detail.getName().equals(DPDK_HUGE_PAGES)) {
                    isHugePagesSet = true;
                }
            }
            boolean valid = isNumaSet && isHugePagesSet;
            if (!valid) {
                return hasValidDPDKConfigurationsOnServiceOffering(isNumaSet, isHugePagesSet, offeringDetails);
            }
            return true;
        }
    }

    /**
     * True if DPDK required configurations are set
     */
    private boolean hasValidDPDKConfigurationsOnServiceOffering(boolean isNumaSet, boolean isHugePagesSet, List<ServiceOfferingDetailsVO> offeringDetails) {
        if (!CollectionUtils.isEmpty(offeringDetails)) {
            for (ServiceOfferingDetailsVO detail : offeringDetails) {
                if (detail.getName().equals(DPDK_NUMA)) {
                    isNumaSet = true;
                } else if (detail.getName().equals(DPDK_HUGE_PAGES)) {
                    isHugePagesSet = true;
                }
            }
        }
        return isNumaSet && isHugePagesSet;
    }

    @Override
    public boolean isHostDpdkEnabled(long hostId) {
        HostVO host = hostDao.findById(hostId);
        if (host == null) {
            throw new CloudRuntimeException("Could not find host with id " + hostId);
        }
        return StringUtils.isNotBlank(host.getCapabilities()) && host.getCapabilities().contains("dpdk");
    }
}
