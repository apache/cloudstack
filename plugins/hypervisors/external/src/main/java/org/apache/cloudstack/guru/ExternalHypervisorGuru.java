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
package org.apache.cloudstack.guru;

import com.cloud.agent.api.Command;
import com.cloud.agent.api.StopCommand;
import com.cloud.agent.api.to.VirtualMachineTO;
import com.cloud.host.HostVO;
import com.cloud.host.dao.HostDao;
import com.cloud.host.dao.HostDetailsDao;
import com.cloud.hypervisor.Hypervisor;
import com.cloud.hypervisor.HypervisorGuru;
import com.cloud.hypervisor.HypervisorGuruBase;
import com.cloud.vm.UserVmVO;
import com.cloud.vm.VirtualMachine;
import com.cloud.vm.VirtualMachineManager;
import com.cloud.vm.VirtualMachineProfile;
import com.cloud.vm.VirtualMachineProfileImpl;
import com.cloud.vm.VmDetailConstants;
import com.cloud.vm.dao.UserVmDao;
import org.apache.commons.collections.MapUtils;

import javax.inject.Inject;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

public class ExternalHypervisorGuru extends HypervisorGuruBase implements HypervisorGuru {

    @Inject
    private VirtualMachineManager virtualMachineManager;
    @Inject
    private UserVmDao _userVmDao;
    @Inject
    HostDao _hostDao;
    @Inject
    HostDetailsDao _hostDetailsDao;

    protected ExternalHypervisorGuru() {
        super();
    }

    @Override
    public Hypervisor.HypervisorType getHypervisorType() {
        return Hypervisor.HypervisorType.External;
    }

    @Override
    public VirtualMachineTO implement(VirtualMachineProfile vm) {
        VirtualMachineTO to = toVirtualMachineTO(vm);
        return to;
    }

    @Override
    public boolean trackVmHostChange() {
        return false;
    }

    @Override
    protected VirtualMachineTO toVirtualMachineTO(VirtualMachineProfile vmProfile) {
        VirtualMachineTO to = super.toVirtualMachineTO(vmProfile);

        Map<String, String> newDetails = new HashMap<>();
        Map<String, String> toDetails = to.getDetails();
        newDetails.putAll(toDetails);
        Map<String, String> serviceOfferingDetails = _serviceOfferingDetailsDao.listDetailsKeyPairs(vmProfile.getServiceOfferingId());
        if (MapUtils.isNotEmpty(serviceOfferingDetails)) {
            newDetails.putAll(serviceOfferingDetails);
        }
        if (MapUtils.isNotEmpty(newDetails)) {
            to.setDetails(newDetails);
        }

        return to;
    }

    public List<Command> finalizeExpunge(VirtualMachine vm) {

        List<Command> commands = new ArrayList<>();

        final StopCommand stop = new StopCommand(vm, virtualMachineManager.getExecuteInSequence(vm.getHypervisorType()), false, false);
        VirtualMachineProfile profile = new VirtualMachineProfileImpl(vm);
        stop.setVirtualMachine(toVirtualMachineTO(profile));

        if (Hypervisor.HypervisorType.External.equals(vm.getHypervisorType())) {
            final Long hostId = vm.getHostId() != null ? vm.getHostId() : vm.getLastHostId();
            if (hostId != null) {
                HostVO host = _hostDao.findById(hostId);
                HashMap<String, String> accessDetails = new HashMap<>();
                _hostDao.loadDetails(host);
                Map<String, String> hostDetails = host.getDetails();
                UserVmVO userVm = _userVmDao.findById(vm.getId());
                _userVmDao.loadDetails(userVm);
                Map<String, String> userVmDetails = userVm.getDetails();
                loadExternalHostAccessDetails(hostDetails, accessDetails, host.getClusterId());
                loadExternalInstanceDetails(userVmDetails, accessDetails);

                stop.setDetails(accessDetails);
                stop.setExpungeVM(true);
            } else {
                return null;
            }
        }

        commands.add(stop);

        return commands;
    }

    public static void loadExternalHostAccessDetails(Map<String, String> hostDetails, Map<String, String> accessDetails, Long clusterId) {
        Map<String, String> externalHostDetails = new HashMap<>();
        for (Map.Entry<String, String> entry : hostDetails.entrySet()) {
            if (entry.getKey().startsWith(VmDetailConstants.EXTERNAL_DETAIL_PREFIX)) {
                externalHostDetails.put(entry.getKey(), entry.getValue());
            }
        }
        accessDetails.putAll(externalHostDetails);
    }

    public static void loadExternalInstanceDetails(Map<String, String> userVmDetails, Map<String, String> accessDetails) {
        Map<String, String> externalInstanceDetails = new HashMap<>();
        for (Map.Entry<String, String> entry : userVmDetails.entrySet()) {
            if (entry.getKey().startsWith(VmDetailConstants.EXTERNAL_DETAIL_PREFIX)) {
                externalInstanceDetails.put(entry.getKey(), entry.getValue());
            }
        }
        accessDetails.putAll(externalInstanceDetails);
    }
}
