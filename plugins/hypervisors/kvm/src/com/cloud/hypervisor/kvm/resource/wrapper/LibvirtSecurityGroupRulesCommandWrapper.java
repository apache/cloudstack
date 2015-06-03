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

import org.apache.log4j.Logger;
import org.libvirt.Connect;
import org.libvirt.LibvirtException;

import com.cloud.agent.api.Answer;
import com.cloud.agent.api.SecurityGroupRuleAnswer;
import com.cloud.agent.api.SecurityGroupRulesCmd;
import com.cloud.hypervisor.kvm.resource.LibvirtComputingResource;
import com.cloud.hypervisor.kvm.resource.LibvirtVMDef.InterfaceDef;
import com.cloud.resource.CommandWrapper;
import com.cloud.resource.ResourceWrapper;

@ResourceWrapper(handles =  SecurityGroupRulesCmd.class)
public final class LibvirtSecurityGroupRulesCommandWrapper extends CommandWrapper<SecurityGroupRulesCmd, Answer, LibvirtComputingResource> {

    private static final Logger s_logger = Logger.getLogger(LibvirtSecurityGroupRulesCommandWrapper.class);

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
        } catch (final LibvirtException e) {
            return new SecurityGroupRuleAnswer(command, false, e.toString());
        }

        final boolean result = libvirtComputingResource.addNetworkRules(command.getVmName(), Long.toString(command.getVmId()), command.getGuestIp(), command.getSignature(),
                Long.toString(command.getSeqNum()), command.getGuestMac(), command.stringifyRules(), vif, brname, command.getSecIpsString());

        if (!result) {
            s_logger.warn("Failed to program network rules for vm " + command.getVmName());
            return new SecurityGroupRuleAnswer(command, false, "programming network rules failed");
        } else {
            s_logger.debug("Programmed network rules for vm " + command.getVmName() + " guestIp=" + command.getGuestIp() + ",ingress numrules="
                    + command.getIngressRuleSet().length + ",egress numrules=" + command.getEgressRuleSet().length);
            return new SecurityGroupRuleAnswer(command);
        }
    }
}