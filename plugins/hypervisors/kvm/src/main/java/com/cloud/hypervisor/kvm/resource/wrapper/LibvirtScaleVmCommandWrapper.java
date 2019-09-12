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

import com.cloud.agent.api.ScaleVmAnswer;
import com.cloud.agent.api.ScaleVmCommand;
import com.cloud.hypervisor.kvm.resource.LibvirtComputingResource;
import com.cloud.resource.CommandWrapper;
import com.cloud.resource.ResourceWrapper;
import org.apache.log4j.Logger;
import org.libvirt.Connect;
import org.libvirt.Domain;
import org.libvirt.LibvirtException;

@ResourceWrapper(handles =  ScaleVmCommand.class)
public class LibvirtScaleVmCommandWrapper extends CommandWrapper<ScaleVmCommand, ScaleVmAnswer, LibvirtComputingResource> {

    private static final Logger s_logger = Logger.getLogger(LibvirtScaleVmCommandWrapper.class);

    @Override
    public ScaleVmAnswer execute(ScaleVmCommand command, LibvirtComputingResource serverResource) {
        String vmName = command.getVmName();
        s_logger.debug("Trying to scale VM " + vmName + " CPUs to " + command.getCpus());
        try {
            final LibvirtUtilitiesHelper libvirtUtilitiesHelper = serverResource.getLibvirtUtilitiesHelper();
            Connect conn = libvirtUtilitiesHelper.getConnection();
            Domain dm = serverResource.getDomain(conn, vmName);
            dm.setVcpus(command.getCpus());
            return new ScaleVmAnswer(command, true, "OK");
        } catch (LibvirtException e) {
            s_logger.error(e.getMessage());
            return new ScaleVmAnswer(command, false, e.getMessage());
        }
    }

}
