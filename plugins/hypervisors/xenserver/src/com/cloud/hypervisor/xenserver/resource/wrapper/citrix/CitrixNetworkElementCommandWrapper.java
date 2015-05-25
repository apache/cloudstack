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

package com.cloud.hypervisor.xenserver.resource.wrapper.citrix;

import com.cloud.agent.api.Answer;
import com.cloud.agent.api.routing.NetworkElementCommand;
import com.cloud.agent.resource.virtualnetwork.VirtualRoutingResource;
import com.cloud.hypervisor.xenserver.resource.CitrixResourceBase;
import com.cloud.resource.CommandWrapper;
import com.cloud.resource.ResourceWrapper;

@ResourceWrapper(handles =  NetworkElementCommand.class)
public final class CitrixNetworkElementCommandWrapper extends CommandWrapper<NetworkElementCommand, Answer, CitrixResourceBase> {

    @Override
    public Answer execute(final NetworkElementCommand command, final CitrixResourceBase citrixResourceBase) {
        final VirtualRoutingResource routingResource = citrixResourceBase.getVirtualRoutingResource();
        return routingResource.executeRequest(command);
    }
}