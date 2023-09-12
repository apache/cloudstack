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

import org.apache.commons.lang3.StringUtils;
import org.apache.log4j.Logger;

import com.cloud.agent.api.Answer;
import com.cloud.agent.api.OvsFetchInterfaceAnswer;
import com.cloud.agent.api.OvsFetchInterfaceCommand;
import com.cloud.hypervisor.kvm.resource.LibvirtComputingResource;
import com.cloud.resource.CommandWrapper;
import com.cloud.resource.ResourceWrapper;
import com.cloud.utils.script.Script;

@ResourceWrapper(handles =  OvsFetchInterfaceCommand.class)
public final class LibvirtOvsFetchInterfaceCommandWrapper extends CommandWrapper<OvsFetchInterfaceCommand, Answer, LibvirtComputingResource> {

    private static final Logger s_logger = Logger.getLogger(LibvirtOvsFetchInterfaceCommandWrapper.class);

    @Override
    public Answer execute(final OvsFetchInterfaceCommand command, final LibvirtComputingResource libvirtComputingResource) {
        final String label = command.getLabel();

        s_logger.debug("Will look for network with name-label:" + label);
        try {
            String ipadd = Script.runSimpleBashScript("ifconfig " + label + " | grep 'inet addr:' | cut -d: -f2 | awk '{ print $1}'");
            if (StringUtils.isEmpty(ipadd)) {
                ipadd = Script.runSimpleBashScript("ifconfig " + label + " | grep ' inet ' | awk '{ print $2}'");
            }
            String mask = Script.runSimpleBashScript("ifconfig " + label + " | grep 'inet addr:' | cut -d: -f4");
            if (StringUtils.isEmpty(mask)) {
                mask = Script.runSimpleBashScript("ifconfig " + label + " | grep ' inet ' | awk '{ print $4}'");
            }
            String mac = Script.runSimpleBashScript("ifconfig " + label + " | grep HWaddr | awk -F \" \" '{print $5}'");
            if (StringUtils.isEmpty(mac)) {
                mac = Script.runSimpleBashScript("ifconfig " + label + " | grep ' ether ' | awk '{ print $2}'");
            }
            return new OvsFetchInterfaceAnswer(command, true, "Interface " + label
                    + " retrieved successfully", ipadd, mask, mac);

        } catch (final Exception e) {
            s_logger.warn("Caught execption when fetching interface", e);
            return new OvsFetchInterfaceAnswer(command, false, "EXCEPTION:"
                    + e.getMessage());
        }
    }
}
