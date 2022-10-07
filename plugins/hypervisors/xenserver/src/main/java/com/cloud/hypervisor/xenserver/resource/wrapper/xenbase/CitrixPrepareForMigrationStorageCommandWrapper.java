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

package com.cloud.hypervisor.xenserver.resource.wrapper.xenbase;

import com.cloud.agent.api.Answer;
import com.cloud.agent.api.PrepareForMigrationStorageAnswer;
import com.cloud.agent.api.PrepareForMigrationStorageCommand;
import com.cloud.agent.api.to.DiskTO;
import com.cloud.agent.api.to.NicTO;
import com.cloud.agent.api.to.VirtualMachineTO;
import com.cloud.agent.api.to.VolumeTO;
import com.cloud.hypervisor.xenserver.resource.CitrixResourceBase;
import com.cloud.resource.CommandWrapper;
import com.cloud.resource.ResourceWrapper;
import com.cloud.storage.Storage;
import com.cloud.storage.Volume;
import com.cloud.template.VirtualMachineTemplate;
import com.cloud.vm.VirtualMachine;
import com.xensource.xenapi.Connection;
import com.xensource.xenapi.SR;
import com.xensource.xenapi.Types;
import com.xensource.xenapi.VDI;
import com.xensource.xenapi.VM;
import org.apache.cloudstack.storage.to.VolumeObjectTO;
import org.apache.log4j.Logger;
import org.apache.xmlrpc.XmlRpcException;

import java.util.HashMap;
import java.util.Map;

@ResourceWrapper(handles = PrepareForMigrationStorageCommand.class)
public final class CitrixPrepareForMigrationStorageCommandWrapper extends CommandWrapper<PrepareForMigrationStorageCommand, Answer, CitrixResourceBase> {

    private static final Logger s_logger = Logger.getLogger(CitrixPrepareForMigrationStorageCommandWrapper.class);

    @Override
    public Answer execute(final PrepareForMigrationStorageCommand command, final CitrixResourceBase citrixResourceBase) {
        final Connection conn = citrixResourceBase.getConnection();
        String vmNameNVdiRootId;
        try {
            vmNameNVdiRootId = citrixResourceBase.callHostPlugin(conn,
                    "migrate-unattached-disk", "create_n_attach",
                    "local_vdi_uuid", command.getVolumeTO().getPath()).trim();
        } catch (final Exception e) {
            s_logger.warn("Catch Exception " + e.getClass().getName() + " prepare for storage migration failed due to " + e.toString(), e);
            return new PrepareForMigrationStorageAnswer(command, e);
        }

        String[] splitInfo = vmNameNVdiRootId.split(" ");
        String vmName = splitInfo[0];
        String rootVdiId = splitInfo[1];

        VM.Record vmRecord;
        VDI.Record vdiRecord;
        String poolUuid;
        VolumeObjectTO volumeToMove = command.getVolumeTO();
        try {
            VM vm = citrixResourceBase.getVM(conn, vmName);
            vmRecord = vm.getRecord(conn);
            VDI vdi = citrixResourceBase.getVDIbyUuid(conn, rootVdiId);
            vdiRecord = vdi.getRecord(conn);
            SR sr = vdi.getSR(conn);
            poolUuid = sr.getNameLabel(conn);
        } catch (Types.XenAPIException e) {
            s_logger.warn("Catch Exception " + e.getClass().getName() + " prepare for migration storage failed due to " + e.toString(), e);
            return new PrepareForMigrationStorageAnswer(command, e);
        } catch (XmlRpcException e) {
            s_logger.warn("Catch Exception " + e.getClass().getName() + " prepare for migration storage failed due to " + e.toString(), e);
            return new PrepareForMigrationStorageAnswer(command, e);
        }


        VirtualMachineTO vmSpec = new VirtualMachineTO(1L,
                vmName,
                VirtualMachine.Type.Instance,
                vmRecord.VCPUsMax.intValue(),
                2000,
                vmRecord.memoryStaticMin,
                vmRecord.memoryStaticMax,
                VirtualMachineTemplate.BootloaderType.External,
                "busybox",
                false,
                false,
                "none");
        vmSpec.setDisks(new DiskTO[]{
                new DiskTO(volumeToMove, 2L, volumeToMove.getPath(), volumeToMove.getVolumeType())
        });
        Map<String, String> details = new HashMap();
        details.put("forcemigrate", "true");
        vmSpec.setDetails(details);
        vmSpec.setNics(new NicTO[]{});


        VolumeTO[] volumes = new VolumeTO[]{
                new VolumeTO(1,
                        Volume.Type.ROOT,
                        Storage.StoragePoolType.PreSetup,
                        poolUuid, "root", "/",
                        rootVdiId, vdiRecord.virtualSize,
                        ""),
                new VolumeTO(
                        volumeToMove.getId(),
                        volumeToMove.getVolumeType(),
                        Storage.StoragePoolType.VMFS,
                        volumeToMove.getDataStore().getUuid(), volumeToMove.getName(), "/",
                        volumeToMove.getPath(), volumeToMove.getSize(),
                        volumeToMove.getChainInfo()),
        };
        return new PrepareForMigrationStorageAnswer(command, vmSpec, volumes);
    }
}
