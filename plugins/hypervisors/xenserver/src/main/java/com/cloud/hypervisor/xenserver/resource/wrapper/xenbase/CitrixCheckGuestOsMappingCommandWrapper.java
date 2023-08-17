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

package com.cloud.hypervisor.xenserver.resource.wrapper.xenbase;

import java.util.Set;

import org.apache.commons.collections.CollectionUtils;
import org.apache.log4j.Logger;

import com.cloud.agent.api.Answer;
import com.cloud.agent.api.CheckGuestOsMappingAnswer;
import com.cloud.agent.api.CheckGuestOsMappingCommand;
import com.cloud.hypervisor.xenserver.resource.CitrixResourceBase;
import com.cloud.resource.CommandWrapper;
import com.cloud.resource.ResourceWrapper;
import com.xensource.xenapi.Connection;
import com.xensource.xenapi.VM;

@ResourceWrapper(handles =  CheckGuestOsMappingCommand.class)
public final class CitrixCheckGuestOsMappingCommandWrapper extends CommandWrapper<CheckGuestOsMappingCommand, Answer, CitrixResourceBase> {

    private static final Logger s_logger = Logger.getLogger(CitrixCheckGuestOsMappingCommandWrapper.class);

    @Override
    public Answer execute(final CheckGuestOsMappingCommand command, final CitrixResourceBase citrixResourceBase) {
        final Connection conn = citrixResourceBase.getConnection();
        String guestOsName = command.getGuestOsName();
        String guestOsMappingName = command.getGuestOsHypervisorMappingName();
        try {
            s_logger.info("Checking guest os mapping name: " + guestOsMappingName + " for the guest os: " + guestOsName + " in the hypervisor");
            final Set<VM> vms = VM.getAll(conn);
            if (CollectionUtils.isEmpty(vms)) {
                return new CheckGuestOsMappingAnswer(command, "Unable to match guest os mapping name: " + guestOsMappingName + " in the hypervisor");
            }
            for (VM vm : vms) {
                if (vm != null && vm.getIsATemplate(conn) && guestOsMappingName.equalsIgnoreCase(vm.getNameLabel(conn))) {
                    if (guestOsName.equalsIgnoreCase(vm.getNameLabel(conn))) {
                        s_logger.debug("Hypervisor guest os name label matches with os name: " + guestOsName);
                    }
                    s_logger.info("Hypervisor guest os name label matches with os mapping: " + guestOsMappingName + " from user");
                    return new CheckGuestOsMappingAnswer(command);
                }
            }
            return new CheckGuestOsMappingAnswer(command, "Guest os mapping name: " + guestOsMappingName + " not found in the hypervisor");
        } catch (final Exception e) {
            s_logger.error("Failed to find the hypervisor guest os mapping name: " + guestOsMappingName, e);
            return new CheckGuestOsMappingAnswer(command, e.getLocalizedMessage());
        }
    }
}
