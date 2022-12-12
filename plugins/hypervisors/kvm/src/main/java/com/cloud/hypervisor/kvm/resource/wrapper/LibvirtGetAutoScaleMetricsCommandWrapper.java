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
import com.cloud.agent.api.routing.GetAutoScaleMetricsAnswer;
import com.cloud.agent.api.routing.GetAutoScaleMetricsCommand;
import com.cloud.hypervisor.kvm.resource.LibvirtComputingResource;
import com.cloud.network.router.VirtualRouterAutoScale;
import com.cloud.network.router.VirtualRouterAutoScale.AutoScaleMetrics;
import com.cloud.network.router.VirtualRouterAutoScale.AutoScaleMetricsValue;
import com.cloud.network.router.VirtualRouterAutoScale.AutoScaleValueType;
import com.cloud.resource.CommandWrapper;
import com.cloud.resource.ResourceWrapper;

import java.util.ArrayList;
import java.util.List;

@ResourceWrapper(handles =  GetAutoScaleMetricsCommand.class)
public class LibvirtGetAutoScaleMetricsCommandWrapper extends CommandWrapper<GetAutoScaleMetricsCommand, Answer, LibvirtComputingResource> {

    static final String NETWORK_USAGE_GET_OPTION = "get";

    @Override
    public Answer execute(final GetAutoScaleMetricsCommand command, final LibvirtComputingResource libvirtComputingResource) {

        Long bytesSent;
        Long bytesReceived;
        if (command.isForVpc()) {
            final long[] stats = libvirtComputingResource.getVPCNetworkStats(command.getPrivateIP(), command.getPublicIP(), NETWORK_USAGE_GET_OPTION);
            bytesSent = stats[0];
            bytesReceived = stats[1];
        } else {
            final long [] stats = libvirtComputingResource.getNetworkStats(command.getPrivateIP(), command.getPublicIP());
            bytesSent = stats[0];
            bytesReceived = stats[1];
        }
        final long [] lbStats = libvirtComputingResource.getNetworkLbStats(command.getPrivateIP(), command.getPublicIP(), command.getPort());
        final long lbConnections = lbStats[0];

        List<AutoScaleMetricsValue> values = new ArrayList<>();

        for (AutoScaleMetrics metrics : command.getMetrics()) {
            switch (metrics.getCounter()) {
                case NETWORK_RECEIVED_AVERAGE_MBPS:
                    values.add(new AutoScaleMetricsValue(metrics, AutoScaleValueType.AGGREGATED_VM_GROUP, Double.valueOf(bytesReceived) / VirtualRouterAutoScale.MBITS_TO_BYTES));
                    break;
                case NETWORK_TRANSMIT_AVERAGE_MBPS:
                    values.add(new AutoScaleMetricsValue(metrics, AutoScaleValueType.AGGREGATED_VM_GROUP, Double.valueOf(bytesSent) / VirtualRouterAutoScale.MBITS_TO_BYTES));
                    break;
                case LB_AVERAGE_CONNECTIONS:
                    values.add(new AutoScaleMetricsValue(metrics, AutoScaleValueType.INSTANT_VM, Double.valueOf(lbConnections)));
                    break;
            }
        }

        return new GetAutoScaleMetricsAnswer(command, true, values);
    }
}
