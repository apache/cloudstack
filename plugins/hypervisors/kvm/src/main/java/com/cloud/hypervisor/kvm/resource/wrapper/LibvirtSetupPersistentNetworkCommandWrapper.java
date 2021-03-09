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

package com.cloud.hypervisor.kvm.resource.wrapper;

import org.apache.log4j.Logger;
import org.libvirt.LibvirtException;

import com.cloud.agent.api.Answer;
import com.cloud.agent.api.SetupPersistentNetworkAnswer;
import com.cloud.agent.api.SetupPersistentNetworkCommand;
import com.cloud.agent.api.to.NicTO;
import com.cloud.exception.InternalErrorException;
import com.cloud.hypervisor.kvm.resource.LibvirtComputingResource;
import com.cloud.hypervisor.kvm.resource.VifDriver;
import com.cloud.resource.CommandWrapper;
import com.cloud.resource.ResourceWrapper;


@ResourceWrapper(handles = SetupPersistentNetworkCommand.class)
public class LibvirtSetupPersistentNetworkCommandWrapper extends CommandWrapper<SetupPersistentNetworkCommand, Answer, LibvirtComputingResource> {
    private static final Logger s_logger = Logger.getLogger(LibvirtSetupPersistentNetworkCommandWrapper.class);

    @Override
    public Answer execute(SetupPersistentNetworkCommand command, LibvirtComputingResource serverResource) {
        NicTO nic = command.getNic();
        VifDriver driver = serverResource.getVifDriver(nic.getType());
        try {
            driver.plug(nic, null, "", null);
        } catch (InternalErrorException | LibvirtException e) {
            return new SetupPersistentNetworkAnswer(command, false, e.getLocalizedMessage());
        }

        return new SetupPersistentNetworkAnswer(command, true, "Successfully setup persistent network");
    }
}
