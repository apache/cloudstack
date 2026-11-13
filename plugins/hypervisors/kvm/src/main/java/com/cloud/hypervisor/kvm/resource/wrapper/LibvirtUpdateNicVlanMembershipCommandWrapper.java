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
import com.cloud.agent.api.UpdateNicVlanMembershipAnswer;
import com.cloud.agent.api.UpdateNicVlanMembershipCommand;
import com.cloud.agent.api.to.NicTO;
import com.cloud.exception.InternalErrorException;
import com.cloud.hypervisor.kvm.resource.LibvirtComputingResource;
import com.cloud.hypervisor.kvm.resource.LibvirtVMDef.InterfaceDef;
import com.cloud.hypervisor.kvm.resource.VifDriver;
import com.cloud.resource.CommandWrapper;
import com.cloud.resource.ResourceWrapper;
import com.cloud.utils.exception.CloudRuntimeException;
import org.libvirt.Connect;
import org.libvirt.Domain;
import org.libvirt.LibvirtException;

@ResourceWrapper(handles = UpdateNicVlanMembershipCommand.class)
public final class LibvirtUpdateNicVlanMembershipCommandWrapper
        extends CommandWrapper<UpdateNicVlanMembershipCommand, Answer, LibvirtComputingResource> {

    @Override
    public Answer execute(final UpdateNicVlanMembershipCommand command, final LibvirtComputingResource libvirtComputingResource) {
        final NicTO nic = command.getNic();
        final String vmName = command.getVmName();
        Domain vm = null;
        try {
            final LibvirtUtilitiesHelper libvirtUtilitiesHelper = libvirtComputingResource.getLibvirtUtilitiesHelper();
            final Connect conn = libvirtUtilitiesHelper.getConnectionByVmName(vmName);
            vm = libvirtComputingResource.getDomain(conn, vmName);

            final InterfaceDef iface = libvirtComputingResource.getInterface(conn, vmName, nic.getMac());

            final VifDriver vifDriver = libvirtComputingResource.getVifDriver(nic.getType(), nic.getName());
            vifDriver.updateVlanTrunkMembership(vm, iface, nic);

            return new UpdateNicVlanMembershipAnswer(command, true, "success");
        } catch (final LibvirtException | InternalErrorException | CloudRuntimeException e) {
            final String msg = "Update NIC VLAN membership failed due to " + e;
            logger.warn(msg, e);
            return new UpdateNicVlanMembershipAnswer(command, false, msg);
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
