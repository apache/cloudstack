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

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import javax.inject.Inject;

import org.apache.cloudstack.framework.extensions.manager.ExtensionsManager;
import org.apache.commons.collections.MapUtils;

import com.cloud.agent.api.Command;
import com.cloud.agent.api.StopCommand;
import com.cloud.agent.api.to.VirtualMachineTO;
import com.cloud.host.Host;
import com.cloud.hypervisor.Hypervisor;
import com.cloud.hypervisor.HypervisorGuru;
import com.cloud.hypervisor.HypervisorGuruBase;
import com.cloud.vm.VirtualMachine;
import com.cloud.vm.VirtualMachineManager;
import com.cloud.vm.VirtualMachineProfile;
import com.cloud.vm.VirtualMachineProfileImpl;
import com.cloud.vm.dao.UserVmDao;

public class ExternalHypervisorGuru extends HypervisorGuruBase implements HypervisorGuru {

    @Inject
    private VirtualMachineManager virtualMachineManager;
    @Inject
    private UserVmDao userVmDao;
    @Inject
    ExtensionsManager extensionsManager;

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
        Map<String, String> serviceOfferingDetails = _serviceOfferingDetailsDao.listDetailsKeyPairs(vmProfile.getServiceOfferingId());
        if (MapUtils.isNotEmpty(serviceOfferingDetails)) {
            newDetails.putAll(serviceOfferingDetails);
        }
        newDetails.putAll(toDetails);
        if (MapUtils.isNotEmpty(newDetails)) {
            to.setDetails(newDetails);
        }

        return to;
    }

    protected void updateStopCommandForExternalHypervisorType(final Hypervisor.HypervisorType hypervisorType,
                  final Long hostId, final Map<String, String> vmExternalDetails, final StopCommand stopCommand) {
        if (!Hypervisor.HypervisorType.External.equals(hypervisorType) || hostId == null) {
            return;
        }
        Host host = hostDao.findById(hostId);
        if (host == null) {
            return;
        }
        stopCommand.setExternalDetails(extensionsManager.getExternalAccessDetails(host, vmExternalDetails));
        stopCommand.setExpungeVM(true);
    }

    public List<Command> finalizeExpunge(VirtualMachine vm) {
        List<Command> commands = new ArrayList<>();
        final StopCommand stop = new StopCommand(vm, virtualMachineManager.getExecuteInSequence(vm.getHypervisorType()), false, false);
        VirtualMachineProfile profile = new VirtualMachineProfileImpl(vm);
        VirtualMachineTO virtualMachineTO = toVirtualMachineTO(profile);
        stop.setVirtualMachine(virtualMachineTO);
        final Long hostId = vm.getHostId() != null ? vm.getHostId() : vm.getLastHostId();
        updateStopCommandForExternalHypervisorType(vm.getHypervisorType(), hostId,
                virtualMachineTO.getExternalDetails(), stop);
        commands.add(stop);
        return commands;
    }
}
