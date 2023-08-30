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
import com.cloud.agent.api.GetRemoteVmsAnswer;
import com.cloud.agent.api.GetRemoteVmsCommand;
import com.cloud.hypervisor.kvm.resource.LibvirtComputingResource;
import com.cloud.hypervisor.kvm.resource.LibvirtConnection;
import com.cloud.resource.CommandWrapper;
import com.cloud.resource.ResourceWrapper;
import org.apache.log4j.Logger;
import org.libvirt.Connect;
import org.libvirt.LibvirtException;

import java.util.List;

@ResourceWrapper(handles = GetRemoteVmsCommand.class)
public final class LibvirtGetRemoteVmsCommandWrapper extends CommandWrapper<GetRemoteVmsCommand, Answer, LibvirtComputingResource> {

    private static final Logger s_logger = Logger.getLogger(LibvirtGetRemoteVmsCommandWrapper.class);

    @Override
    public Answer execute(final GetRemoteVmsCommand command, final LibvirtComputingResource libvirtComputingResource) {
        String result = null;
        StringBuilder sb = new StringBuilder("qemu+ssh://");
        sb.append(command.getUsername());
        sb.append("@");
        sb.append(command.getRemoteIp());
        sb.append("/system");
        String hypervisorURI = sb.toString();
        try {
            Connect conn = LibvirtConnection.getConnection(hypervisorURI);
            List<String> vmNames = libvirtComputingResource.getStoppedVms(conn);
            return  new GetRemoteVmsAnswer(command, "", vmNames);
        } catch (final LibvirtException e) {
            s_logger.error("Error while listing stopped Vms on remote host: "+ e.getMessage());
            return new Answer(command, false, result);
        }
    }

}
