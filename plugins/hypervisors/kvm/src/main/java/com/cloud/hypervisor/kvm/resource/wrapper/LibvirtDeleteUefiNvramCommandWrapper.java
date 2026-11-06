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

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.UUID;

import org.apache.commons.lang3.StringUtils;
import org.libvirt.Connect;
import org.libvirt.Domain;
import org.libvirt.Error;
import org.libvirt.LibvirtException;

import com.cloud.agent.api.Answer;
import com.cloud.agent.api.DeleteUefiNvramCommand;
import com.cloud.hypervisor.kvm.resource.LibvirtComputingResource;
import com.cloud.resource.CommandWrapper;
import com.cloud.resource.ResourceWrapper;

@ResourceWrapper(handles = DeleteUefiNvramCommand.class)
public class LibvirtDeleteUefiNvramCommandWrapper extends CommandWrapper<DeleteUefiNvramCommand, Answer, LibvirtComputingResource> {

    @Override
    public Answer execute(DeleteUefiNvramCommand command, LibvirtComputingResource resource) {
        String vmUuid = command.getVmUuid();
        if (!isValidUuid(vmUuid)) {
            return new Answer(command, false, String.format("Invalid VM UUID [%s]; not deleting any UEFI NVRAM file", vmUuid));
        }

        String nvramPath = resource.getUefiNvramPath(vmUuid);
        if (StringUtils.isBlank(nvramPath)) {
            return new Answer(command, true, "No UEFI NVRAM path is configured on this host; nothing to delete");
        }

        try {
            if (isDomainDefined(resource, vmUuid)) {
                String msg = String.format("VM [%s] with UUID [%s] is still defined on this host; keeping its UEFI NVRAM file [%s]",
                        command.getVmName(), vmUuid, nvramPath);
                logger.warn(msg);
                return new Answer(command, false, msg);
            }
        } catch (LibvirtException e) {
            String msg = String.format("Unable to check whether VM [%s] is still defined on this host; keeping its UEFI NVRAM file [%s]: %s",
                    command.getVmName(), nvramPath, e.getMessage());
            logger.warn(msg);
            return new Answer(command, false, msg);
        }

        try {
            if (Files.deleteIfExists(Path.of(nvramPath))) {
                logger.info("Deleted UEFI NVRAM file [{}] of expunged VM [{}]", nvramPath, command.getVmName());
                return new Answer(command, true, String.format("Deleted UEFI NVRAM file [%s]", nvramPath));
            }
            return new Answer(command, true, String.format("UEFI NVRAM file [%s] does not exist on this host", nvramPath));
        } catch (IOException e) {
            String msg = String.format("Unable to delete UEFI NVRAM file [%s] of VM [%s]: %s", nvramPath, command.getVmName(), e.getMessage());
            logger.warn(msg);
            return new Answer(command, false, msg);
        }
    }

    protected boolean isValidUuid(String vmUuid) {
        if (StringUtils.isBlank(vmUuid)) {
            return false;
        }
        try {
            return UUID.fromString(vmUuid).toString().equalsIgnoreCase(vmUuid);
        } catch (IllegalArgumentException e) {
            return false;
        }
    }

    protected boolean isDomainDefined(LibvirtComputingResource resource, String vmUuid) throws LibvirtException {
        Connect conn = resource.getLibvirtUtilitiesHelper().getConnection();
        Domain domain = null;
        try {
            domain = conn.domainLookupByUUIDString(vmUuid);
            return domain != null;
        } catch (LibvirtException e) {
            if (e.getError() != null && e.getError().getCode() == Error.ErrorNumber.VIR_ERR_NO_DOMAIN) {
                return false;
            }
            throw e;
        } finally {
            if (domain != null) {
                try {
                    domain.free();
                } catch (LibvirtException e) {
                    logger.trace("Ignoring libvirt error while freeing domain", e);
                }
            }
        }
    }
}
