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
import com.cloud.agent.api.NetworkUsageAnswer;
import com.cloud.agent.api.NetworkUsageCommand;
import com.cloud.hypervisor.kvm.resource.LibvirtComputingResource;
import com.cloud.resource.CommandWrapper;
import com.cloud.resource.ResourceWrapper;

@ResourceWrapper(handles =  NetworkUsageCommand.class)
public final class LibvirtNetworkUsageCommandWrapper extends CommandWrapper<NetworkUsageCommand, Answer, LibvirtComputingResource> {

    @Override
    public Answer execute(final NetworkUsageCommand command, final LibvirtComputingResource libvirtComputingResource) {
        if (command.isForVpc()) {
            if (command.getOption() != null && command.getOption().equals("create")) {
                final String result = libvirtComputingResource.configureVPCNetworkUsage(command.getPrivateIP(), command.getGatewayIP(), "create", command.getVpcCIDR());
                final NetworkUsageAnswer answer = new NetworkUsageAnswer(command, result, 0L, 0L);
                return answer;
            } else if (command.getOption() != null && (command.getOption().equals("get") || command.getOption().equals("vpn"))) {
                final long[] stats = libvirtComputingResource.getVPCNetworkStats(command.getPrivateIP(), command.getGatewayIP(), command.getOption());
                final NetworkUsageAnswer answer = new NetworkUsageAnswer(command, "", stats[0], stats[1]);
                return answer;
            } else {
                final String result = libvirtComputingResource.configureVPCNetworkUsage(command.getPrivateIP(), command.getGatewayIP(), command.getOption(), command.getVpcCIDR());
                final NetworkUsageAnswer answer = new NetworkUsageAnswer(command, result, 0L, 0L);
                return answer;
            }
        } else {
            if (command.getOption() != null && command.getOption().equals("create")) {
                final String result = libvirtComputingResource.networkUsage(command.getPrivateIP(), "create", null);
                final NetworkUsageAnswer answer = new NetworkUsageAnswer(command, result, 0L, 0L);
                return answer;
            }
            final long [] stats = libvirtComputingResource.getNetworkStats(command.getPrivateIP());
            final NetworkUsageAnswer answer = new NetworkUsageAnswer(command, "", stats[0], stats[1]);
            return answer;
        }
    }
}
