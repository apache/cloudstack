//Licensed to the Apache Software Foundation (ASF) under one
//or more contributor license agreements.  See the NOTICE file
//distributed with this work for additional information
//regarding copyright ownership.  The ASF licenses this file
//to you under the Apache License, Version 2.0 (the
//"License"); you may not use this file except in compliance
//the License.  You may obtain a copy of the License at
//
//http://www.apache.org/licenses/LICENSE-2.0
//
//Unless required by applicable law or agreed to in writing,
//software distributed under the License is distributed on an
//"AS IS" BASIS, WITHOUT WARRANTIES OR CONDITIONS OF ANY
//KIND, either express or implied.  See the License for the
//specific language governing permissions and limitations
//under the License.

package com.cloud.hypervisor.kvm.resource.wrapper;

import org.apache.cloudstack.backup.CreateImageTransferAnswer;
import org.apache.cloudstack.backup.CreateImageTransferCommand;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

import com.cloud.agent.api.Answer;
import com.cloud.hypervisor.kvm.resource.LibvirtComputingResource;
import com.cloud.resource.CommandWrapper;
import com.cloud.resource.ResourceWrapper;

@ResourceWrapper(handles = CreateImageTransferCommand.class)
public class LibvirtCreateImageTransferCommandWrapper extends CommandWrapper<CreateImageTransferCommand, Answer, LibvirtComputingResource> {
    protected Logger logger = LogManager.getLogger(getClass());

    private CreateImageTransferAnswer handleUpload(CreateImageTransferCommand cmd) {
        return new CreateImageTransferAnswer(cmd, false, "Image Upload is not handled by KVM agent");
    }

    private CreateImageTransferAnswer handleDownload(CreateImageTransferCommand cmd) {
        String exportName = cmd.getExportName();
        int nbdPort = cmd.getNbdPort();
        try {
            String hostIpAddress = cmd.getHostIpAddress();
            String transferUrl = String.format("nbd://%s:%d/%s", hostIpAddress, nbdPort, exportName);

            return new CreateImageTransferAnswer(cmd, true, "Image transfer created for download",
                    cmd.getTransferId(), transferUrl);

        } catch (Exception e) {
            return new CreateImageTransferAnswer(cmd, false, "Error creating image transfer: " + e.getMessage());
        }
    }

    @Override
    public Answer execute(CreateImageTransferCommand cmd, LibvirtComputingResource resource) {
        if (cmd.getDirection().equals("download")) {
            return handleDownload(cmd);
        } else {
            return handleUpload(cmd);
        }
    }
}
