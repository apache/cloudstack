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

import java.util.List;
import java.util.Map;

import org.apache.log4j.Logger;
import org.libvirt.Connect;
import org.libvirt.Domain;
import org.libvirt.LibvirtException;

import com.cloud.agent.api.Answer;
import com.cloud.agent.api.UnPlugNicAnswer;
import com.cloud.agent.api.UnPlugNicCommand;
import com.cloud.agent.api.to.NicTO;
import com.cloud.hypervisor.kvm.resource.LibvirtComputingResource;
import com.cloud.hypervisor.kvm.resource.LibvirtVMDef.InterfaceDef;
import com.cloud.hypervisor.kvm.resource.VifDriver;
import com.cloud.resource.CommandWrapper;
import com.cloud.resource.ResourceWrapper;

@ResourceWrapper(handles =  UnPlugNicCommand.class)
public final class LibvirtUnPlugNicCommandWrapper extends CommandWrapper<UnPlugNicCommand, Answer, LibvirtComputingResource> {

    private static final Logger s_logger = Logger.getLogger(LibvirtUnPlugNicCommandWrapper.class);

    @Override
    public Answer execute(final UnPlugNicCommand command, final LibvirtComputingResource libvirtComputingResource) {
        final NicTO nic = command.getNic();
        final String vmName = command.getVmName();
        final Map<String, Boolean> vlanToPersistenceMap = command.getVlanToPersistenceMap();
        Domain vm = null;
        try {
            final LibvirtUtilitiesHelper libvirtUtilitiesHelper = libvirtComputingResource.getLibvirtUtilitiesHelper();

            final Connect conn = libvirtUtilitiesHelper.getConnectionByVmName(vmName);
            vm = libvirtComputingResource.getDomain(conn, vmName);
            final List<InterfaceDef> pluggedNics = libvirtComputingResource.getInterfaces(conn, vmName);

            for (final InterfaceDef pluggedNic : pluggedNics) {
                if (pluggedNic.getMacAddress().equalsIgnoreCase(nic.getMac())) {
                    if (nic.isSecurityGroupEnabled()) {
                        libvirtComputingResource.destroyNetworkRulesForNic(conn, vmName, nic);
                    }
                    vm.detachDevice(pluggedNic.toString());
                    String vlanId = libvirtComputingResource.getVlanIdFromBridgeName(pluggedNic.getBrName());
                    // We don't know which "traffic type" is associated with
                    // each interface at this point, so inform all vif drivers
                    for (final VifDriver vifDriver : libvirtComputingResource.getAllVifDrivers()) {
                        vifDriver.unplug(pluggedNic, libvirtComputingResource.shouldDeleteBridge(vlanToPersistenceMap, vlanId));
                    }
                    return new UnPlugNicAnswer(command, true, "success");
                }
            }
            return new UnPlugNicAnswer(command, true, "success");
        } catch (final LibvirtException e) {
            final String msg = " Unplug Nic failed due to " + e.toString();
            s_logger.warn(msg, e);
            return new UnPlugNicAnswer(command, false, msg);
        } finally {
            if (vm != null) {
                try {
                    vm.free();
                } catch (final LibvirtException l) {
                    s_logger.trace("Ignoring libvirt error.", l);
                }
            }
        }
    }
}
