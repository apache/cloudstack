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

package com.cloud.hypervisor.xenserver.resource.wrapper.xen610;

import java.util.List;
import java.util.Set;

import org.apache.cloudstack.storage.to.VolumeObjectTO;
import org.apache.log4j.Logger;

import com.cloud.agent.api.Answer;
import com.cloud.agent.api.MigrateWithStorageCompleteAnswer;
import com.cloud.agent.api.MigrateWithStorageCompleteCommand;
import com.cloud.agent.api.to.VirtualMachineTO;
import com.cloud.hypervisor.xenserver.resource.XenServer610Resource;
import com.cloud.hypervisor.xenserver.resource.XsHost;
import com.cloud.resource.CommandWrapper;
import com.cloud.resource.ResourceWrapper;
import com.cloud.utils.exception.CloudRuntimeException;
import com.xensource.xenapi.Connection;
import com.xensource.xenapi.Host;
import com.xensource.xenapi.VM;

@ResourceWrapper(handles =  MigrateWithStorageCompleteCommand.class)
public final class XenServer610MigrateWithStorageCompleteCommandWrapper extends CommandWrapper<MigrateWithStorageCompleteCommand, Answer, XenServer610Resource> {

    private static final Logger s_logger = Logger.getLogger(XenServer610MigrateWithStorageCompleteCommandWrapper.class);

    @Override
    public Answer execute(final MigrateWithStorageCompleteCommand command, final XenServer610Resource xenServer610Resource) {
        final Connection connection = xenServer610Resource.getConnection();
        final VirtualMachineTO vmSpec = command.getVirtualMachine();

        final String name = vmSpec.getName();
        try {
            final XsHost xsHost = xenServer610Resource.getHost();
            final String uuid = xsHost.getUuid();

            final Set<VM> vms = VM.getByNameLabel(connection, name);
            // Check if VMs can be found by label.
            if (vms == null) {
                throw new CloudRuntimeException("Couldn't find VMs by label " + name + " on the destination host.");
            }
            final VM migratedVm = vms.iterator().next();

            // Check the vm is present on the new host.
            if (migratedVm == null) {
                throw new CloudRuntimeException("Couldn't find the migrated vm " + name + " on the destination host.");
            }

            final Host host = Host.getByUuid(connection, uuid);
            migratedVm.setAffinity(connection, host);

            // Volume paths would have changed. Return that information.
            final List<VolumeObjectTO> volumeToSet = xenServer610Resource.getUpdatedVolumePathsOfMigratedVm(connection, migratedVm, vmSpec.getDisks());

            return new MigrateWithStorageCompleteAnswer(command, volumeToSet);
        } catch (final CloudRuntimeException e) {
            s_logger.error("Migration of vm " + name + " with storage failed due to " + e.toString(), e);
            return new MigrateWithStorageCompleteAnswer(command, e);
        } catch (final Exception e) {
            s_logger.error("Migration of vm " + name + " with storage failed due to " + e.toString(), e);
            return new MigrateWithStorageCompleteAnswer(command, e);
        }
    }
}
