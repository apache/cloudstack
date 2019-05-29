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
import com.cloud.offering.ServiceOffering;
import com.cloud.service.ServiceOfferingDetailsVO;
import com.cloud.service.dao.ServiceOfferingDetailsDao;
import com.cloud.utils.exception.CloudRuntimeException;
import com.cloud.vm.VirtualMachineProfile;
import org.apache.log4j.Logger;

import javax.inject.Inject;

public class DPDKHelperImpl implements DPDKHelper {

    @Inject
    ServiceOfferingDetailsDao serviceOfferingDetailsDao;

    public static final Logger s_logger = Logger.getLogger(DPDKHelperImpl.class);

    private ServiceOffering getServiceOfferingFromVMProfile(VirtualMachineProfile virtualMachineProfile) {
        ServiceOffering offering = virtualMachineProfile.getServiceOffering();
        if (offering == null) {
            throw new CloudRuntimeException("VM does not have an associated service offering");
        }
        return offering;
    }

    @Override
    public boolean isDPDKvHostUserModeSettingOnServiceOffering(VirtualMachineProfile vm) {
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
                s_logger.error(String.format("DPDK vHost User mode found as a detail for service offering: %s " +
                                "but value: %s is not supported. Supported values: %s, %s",
                        offering.getId(), mode,
                        VHostUserMode.CLIENT.toString(), VHostUserMode.SERVER.toString()));
            }
        }
    }
}
