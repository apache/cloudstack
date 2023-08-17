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

import java.util.ArrayList;
import java.util.List;
import java.util.Set;

import org.apache.commons.collections.CollectionUtils;
import org.apache.commons.lang3.StringUtils;
import org.apache.log4j.Logger;

import com.cloud.agent.api.Answer;
import com.cloud.agent.api.GetHypervisorGuestOsNamesAnswer;
import com.cloud.agent.api.GetHypervisorGuestOsNamesCommand;
import com.cloud.hypervisor.xenserver.resource.CitrixResourceBase;
import com.cloud.resource.CommandWrapper;
import com.cloud.resource.ResourceWrapper;
import com.cloud.utils.Pair;
import com.xensource.xenapi.Connection;
import com.xensource.xenapi.VM;

@ResourceWrapper(handles =  GetHypervisorGuestOsNamesCommand.class)
public final class CitrixGetHypervisorGuestOsNamesCommandWrapper extends CommandWrapper<GetHypervisorGuestOsNamesCommand, Answer, CitrixResourceBase> {

    private static final Logger s_logger = Logger.getLogger(CitrixGetHypervisorGuestOsNamesCommandWrapper.class);

    @Override
    public Answer execute(final GetHypervisorGuestOsNamesCommand command, final CitrixResourceBase citrixResourceBase) {
        final Connection conn = citrixResourceBase.getConnection();
        String keyword = command.getKeyword();
        try {
            s_logger.info("Getting guest os names in the hypervisor");
            final Set<VM> vms = VM.getAll(conn);
            if (CollectionUtils.isEmpty(vms)) {
                return new GetHypervisorGuestOsNamesAnswer(command, "Guest os names not found in the hypervisor");
            }
            List<Pair<String, String>> hypervisorGuestOsNames = new ArrayList<>();
            for (VM vm : vms) {
                if (vm != null && vm.getIsATemplate(conn)) {
                    String guestOSNameLabel = vm.getNameLabel(conn);
                    if (StringUtils.isNotBlank(keyword)) {
                        if (guestOSNameLabel.toLowerCase().contains(keyword.toLowerCase())) {
                            Pair<String, String> hypervisorGuestOs = new Pair<>(guestOSNameLabel, guestOSNameLabel);
                            hypervisorGuestOsNames.add(hypervisorGuestOs);
                        }
                    } else {
                        Pair<String, String> hypervisorGuestOs = new Pair<>(guestOSNameLabel, guestOSNameLabel);
                        hypervisorGuestOsNames.add(hypervisorGuestOs);
                    }
                }
            }
            return new GetHypervisorGuestOsNamesAnswer(command, hypervisorGuestOsNames);
        } catch (final Exception e) {
            s_logger.error("Failed to fetch hypervisor guest os names due to: " + e.getLocalizedMessage(), e);
            return new GetHypervisorGuestOsNamesAnswer(command, e.getLocalizedMessage());
        }
    }
}
