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

import org.libvirt.Connect;
import org.libvirt.LibvirtException;

import com.cloud.agent.api.Answer;
import com.cloud.agent.api.SecurityGroupRuleAnswer;
import com.cloud.agent.api.SecurityGroupRulesCmd;
import com.cloud.agent.api.to.VirtualMachineTO;
import com.cloud.hypervisor.kvm.resource.LibvirtComputingResource;
import com.cloud.hypervisor.kvm.resource.LibvirtVMDef.InterfaceDef;
import com.cloud.resource.CommandWrapper;
import com.cloud.resource.ResourceWrapper;

@ResourceWrapper(handles =  SecurityGroupRulesCmd.class)
public final class LibvirtSecurityGroupRulesCommandWrapper extends CommandWrapper<SecurityGroupRulesCmd, Answer, LibvirtComputingResource> {


    @Override
    public Answer execute(final SecurityGroupRulesCmd command, final LibvirtComputingResource libvirtComputingResource) {
        String vif = null;
        String brname = null;
        try {
            final LibvirtUtilitiesHelper libvirtUtilitiesHelper = libvirtComputingResource.getLibvirtUtilitiesHelper();

            final Connect conn = libvirtUtilitiesHelper.getConnectionByVmName(command.getVmName());
            final List<InterfaceDef> nics = libvirtComputingResource.getInterfaces(conn, command.getVmName());

            vif = nics.get(0).getDevName();
            brname = nics.get(0).getBrName();

            final VirtualMachineTO vm = command.getVmTO();
            if (!libvirtComputingResource.applyDefaultNetworkRules(conn, vm, true)) {
                logger.warn("Failed to program default network rules for vm " + command.getVmName());
                return new SecurityGroupRuleAnswer(command, false, "programming default network rules failed");
            }
        } catch (final LibvirtException e) {
            return new SecurityGroupRuleAnswer(command, false, e.toString());
        }

        final boolean result = libvirtComputingResource.addNetworkRules(command.getVmName(), Long.toString(command.getVmId()), command.getGuestIp(), command.getGuestIp6(), command.getSignature(),
                Long.toString(command.getSeqNum()), command.getGuestMac(), command.stringifyRules(), vif, brname, command.getSecIpsString());

        if (!result) {
            logger.warn("Failed to program network rules for vm " + command.getVmName());
            return new SecurityGroupRuleAnswer(command, false, "programming network rules failed");
        } else {
            logger.debug("Programmed network rules for vm " + command.getVmName() + " guestIp=" + command.getGuestIp() + ",ingress numrules="
                    + command.getIngressRuleSet().size() + ",egress numrules=" + command.getEgressRuleSet().size());
            return new SecurityGroupRuleAnswer(command);
        }
    }
}
