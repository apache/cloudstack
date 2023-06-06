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

import org.apache.log4j.Logger;

import com.cloud.agent.api.Answer;
import com.cloud.agent.api.PingTestCommand;
import com.cloud.hypervisor.kvm.resource.LibvirtComputingResource;
import com.cloud.resource.CommandWrapper;
import com.cloud.resource.ResourceWrapper;
import com.cloud.utils.script.Script;

@ResourceWrapper(handles =  PingTestCommand.class)
public final class LibvirtPingTestCommandWrapper extends CommandWrapper<PingTestCommand, Answer, LibvirtComputingResource> {

    private static final Logger s_logger = Logger.getLogger(LibvirtPingTestCommandWrapper.class);

    @Override
    public Answer execute(final PingTestCommand command, final LibvirtComputingResource libvirtComputingResource) {
        String result = null;
        final String computingHostIp = command.getComputingHostIp(); // TODO, split the command into 2 types

        if (computingHostIp != null) {
            result = doPingTest(libvirtComputingResource, computingHostIp);
        } else if (command.getRouterIp() != null && command.getPrivateIp() != null) {
            result = doPingTest(libvirtComputingResource, command.getRouterIp(), command.getPrivateIp());
        } else {
            return new Answer(command, false, "routerip and private ip is null");
        }

        if (result != null) {
            return new Answer(command, false, result);
        }
        return new Answer(command);
    }

    protected String doPingTest(final LibvirtComputingResource libvirtComputingResource, final String computingHostIp) {
        final Script command = new Script(libvirtComputingResource.getPingTestPath(), 10000, s_logger);
        command.add("-h", computingHostIp);
        return command.execute();
    }

    protected String doPingTest(final LibvirtComputingResource libvirtComputingResource, final String domRIp, final String vmIp) {
        final Script command = new Script(libvirtComputingResource.getPingTestPath(), 10000, s_logger);
        command.add("-i", domRIp);
        command.add("-p", vmIp);
        return command.execute();
    }
}
