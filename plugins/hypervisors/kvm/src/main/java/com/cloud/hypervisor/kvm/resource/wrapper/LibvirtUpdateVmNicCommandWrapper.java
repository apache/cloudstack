//
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
//

package com.cloud.hypervisor.kvm.resource.wrapper;

import com.cloud.agent.api.Answer;
import com.cloud.agent.api.UpdateVmNicAnswer;
import com.cloud.agent.api.UpdateVmNicCommand;
import com.cloud.hypervisor.kvm.resource.LibvirtComputingResource;
import com.cloud.hypervisor.kvm.resource.LibvirtVMDef.InterfaceDef;
import com.cloud.resource.CommandWrapper;
import com.cloud.resource.ResourceWrapper;
import com.cloud.vm.Nic;
import org.libvirt.Connect;
import org.libvirt.Domain;
import org.libvirt.LibvirtException;

@ResourceWrapper(handles =  UpdateVmNicCommand.class)
public final class LibvirtUpdateVmNicCommandWrapper extends CommandWrapper<UpdateVmNicCommand, Answer, LibvirtComputingResource> {

    @Override
    public Answer execute(UpdateVmNicCommand command, LibvirtComputingResource libvirtComputingResource) {
        String nicMacAddress = command.getNicMacAddress();
        String vmName = command.getVmName();
        Nic.LinkState linkState = command.getLinkState();

        Domain vm = null;
        try {
            final LibvirtUtilitiesHelper libvirtUtilitiesHelper = libvirtComputingResource.getLibvirtUtilitiesHelper();
            final Connect conn = libvirtUtilitiesHelper.getConnectionByVmName(vmName);
            vm = libvirtComputingResource.getDomain(conn, vmName);

            final InterfaceDef nic = libvirtComputingResource.getInterface(conn, vmName, nicMacAddress);
            nic.setLinkStateUp(linkState == Nic.LinkState.Enabled);
            vm.updateDeviceFlags(nic.toString(), Domain.DeviceModifyFlags.LIVE);

            return new UpdateVmNicAnswer(command, true, "success");
        } catch (final LibvirtException e) {
            final String msg = String.format(" Update NIC failed due to %s.", e);
            logger.warn(msg, e);
            return new UpdateVmNicAnswer(command, false, msg);
        } finally {
            if (vm != null) {
                try {
                    vm.free();
                } catch (final LibvirtException l) {
                    logger.trace("Ignoring libvirt error.", l);
                }
            }
        }
    }
}
