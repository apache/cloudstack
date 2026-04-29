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
import com.cloud.agent.api.RecreateCheckpointsCommand;
import com.cloud.hypervisor.kvm.resource.LibvirtComputingResource;
import com.cloud.resource.CommandWrapper;
import com.cloud.resource.ResourceWrapper;
import org.apache.cloudstack.storage.to.VolumeObjectTO;
import org.libvirt.LibvirtException;

import java.util.List;

@ResourceWrapper(handles =  RecreateCheckpointsCommand.class)
public class LibvirtRecreateCheckpointsCommandWrapper extends CommandWrapper<RecreateCheckpointsCommand, Answer, LibvirtComputingResource> {
    @Override
    public Answer execute(RecreateCheckpointsCommand command, LibvirtComputingResource serverResource) {
        String vmName = command.getVmName();
        List<VolumeObjectTO> volumes = command.getDisks();

        boolean result;
        try {
            result = serverResource.recreateCheckpointsOnVm(volumes, vmName, serverResource.getLibvirtUtilitiesHelper().getConnectionByVmName(vmName));
        } catch (LibvirtException e) {
            logger.error(String.format("Failed to recreate checkpoints on VM [%s] due to %s", vmName, e.getMessage()), e);
            return new Answer(command, e);
        }
        return new Answer(command, result, null);
    }
}
