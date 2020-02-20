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

import java.util.Map;

import com.cloud.agent.api.Answer;
import com.cloud.agent.api.SetHostParamsCommand;
import com.cloud.hypervisor.kvm.resource.LibvirtComputingResource;
import com.cloud.resource.CommandWrapper;
import com.cloud.resource.ResourceWrapper;

@ResourceWrapper(handles =  SetHostParamsCommand.class)
public final class LibvirtSetHostParamsCommandWrapper extends CommandWrapper<SetHostParamsCommand, Answer, LibvirtComputingResource> {

    @Override
    public Answer execute(final SetHostParamsCommand command, final LibvirtComputingResource libvirtComputingResource) {

        final Map<String, String> params = command.getParams();
        boolean success = libvirtComputingResource.getVirtRouterResource().configureHostParams(params);
        success = success && libvirtComputingResource.configureHostParams(params);

        if (!success) {
            return new Answer(command, false, "Failed to set host parameters");
        } else {
            return new Answer(command, true, null);
        }
    }
}
