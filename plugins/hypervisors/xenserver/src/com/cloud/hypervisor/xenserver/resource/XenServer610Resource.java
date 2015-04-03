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
package com.cloud.hypervisor.xenserver.resource;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;

import javax.ejb.Local;

import org.apache.cloudstack.storage.to.VolumeObjectTO;
import org.apache.log4j.Logger;
import org.apache.xmlrpc.XmlRpcException;

import com.cloud.agent.api.Answer;
import com.cloud.agent.api.Command;
import com.cloud.agent.api.MigrateWithStorageCompleteAnswer;
import com.cloud.agent.api.MigrateWithStorageCompleteCommand;
import com.cloud.agent.api.storage.MigrateVolumeAnswer;
import com.cloud.agent.api.storage.MigrateVolumeCommand;
import com.cloud.agent.api.to.DiskTO;
import com.cloud.agent.api.to.StorageFilerTO;
import com.cloud.agent.api.to.VirtualMachineTO;
import com.cloud.resource.ServerResource;
import com.cloud.storage.Volume;
import com.cloud.utils.exception.CloudRuntimeException;
import com.xensource.xenapi.Connection;
import com.xensource.xenapi.Host;
import com.xensource.xenapi.SR;
import com.xensource.xenapi.Task;
import com.xensource.xenapi.Types;
import com.xensource.xenapi.Types.XenAPIException;
import com.xensource.xenapi.VBD;
import com.xensource.xenapi.VDI;
import com.xensource.xenapi.VIF;
import com.xensource.xenapi.VM;

@Local(value = ServerResource.class)
public class XenServer610Resource extends XenServer600Resource {

    private static final Logger s_logger = Logger.getLogger(XenServer610Resource.class);

    @Override
    public Answer executeRequest(final Command cmd) {
        if (cmd instanceof MigrateWithStorageCompleteCommand) {
            return execute((MigrateWithStorageCompleteCommand)cmd);
        } else if (cmd instanceof MigrateVolumeCommand) {
            return execute((MigrateVolumeCommand)cmd);
        } else {
            return super.executeRequest(cmd);
        }
    }

    public List<VolumeObjectTO> getUpdatedVolumePathsOfMigratedVm(final Connection connection, final VM migratedVm, final DiskTO[] volumes) throws CloudRuntimeException {
        final List<VolumeObjectTO> volumeToList = new ArrayList<VolumeObjectTO>();

        try {
            // Volume paths would have changed. Return that information.
            final Set<VBD> vbds = migratedVm.getVBDs(connection);
            final Map<String, VDI> deviceIdToVdiMap = new HashMap<String, VDI>();
            // get vdi:vbdr to a map
            for (final VBD vbd : vbds) {
                final VBD.Record vbdr = vbd.getRecord(connection);
                if (vbdr.type == Types.VbdType.DISK) {
                    final VDI vdi = vbdr.VDI;
                    deviceIdToVdiMap.put(vbdr.userdevice, vdi);
                }
            }

            for (final DiskTO volumeTo : volumes) {
                if (volumeTo.getType() != Volume.Type.ISO) {
                    final VolumeObjectTO vol = (VolumeObjectTO)volumeTo.getData();
                    final Long deviceId = volumeTo.getDiskSeq();
                    final VDI vdi = deviceIdToVdiMap.get(deviceId.toString());
                    final VolumeObjectTO newVol = new VolumeObjectTO();
                    newVol.setPath(vdi.getUuid(connection));
                    newVol.setId(vol.getId());
                    volumeToList.add(newVol);
                }
            }
        } catch (final Exception e) {
            s_logger.error("Unable to get the updated VDI paths of the migrated vm " + e.toString(), e);
            throw new CloudRuntimeException("Unable to get the updated VDI paths of the migrated vm " + e.toString(), e);
        }

        return volumeToList;
    }

    protected MigrateWithStorageCompleteAnswer execute(final MigrateWithStorageCompleteCommand cmd) {
        final Connection connection = getConnection();
        final VirtualMachineTO vmSpec = cmd.getVirtualMachine();

        try {
            final Host host = Host.getByUuid(connection, _host.getUuid());
            final Set<VM> vms = VM.getByNameLabel(connection, vmSpec.getName());
            final VM migratedVm = vms.iterator().next();

            // Check the vm is present on the new host.
            if (migratedVm == null) {
                throw new CloudRuntimeException("Couldn't find the migrated vm " + vmSpec.getName() + " on the destination host.");
            }

            // Volume paths would have changed. Return that information.
            final List<VolumeObjectTO> volumeToSet = getUpdatedVolumePathsOfMigratedVm(connection, migratedVm, vmSpec.getDisks());
            migratedVm.setAffinity(connection, host);

            return new MigrateWithStorageCompleteAnswer(cmd, volumeToSet);
        } catch (final CloudRuntimeException e) {
            s_logger.error("Migration of vm " + vmSpec.getName() + " with storage failed due to " + e.toString(), e);
            return new MigrateWithStorageCompleteAnswer(cmd, e);
        } catch (final Exception e) {
            s_logger.error("Migration of vm " + vmSpec.getName() + " with storage failed due to " + e.toString(), e);
            return new MigrateWithStorageCompleteAnswer(cmd, e);
        }
    }

    protected MigrateVolumeAnswer execute(final MigrateVolumeCommand cmd) {
        final Connection connection = getConnection();
        final String volumeUUID = cmd.getVolumePath();
        final StorageFilerTO poolTO = cmd.getPool();

        try {
            final SR destinationPool = getStorageRepository(connection, poolTO.getUuid());
            final VDI srcVolume = getVDIbyUuid(connection, volumeUUID);
            final Map<String, String> other = new HashMap<String, String>();
            other.put("live", "true");

            // Live migrate the vdi across pool.
            final Task task = srcVolume.poolMigrateAsync(connection, destinationPool, other);
            final long timeout = _migratewait * 1000L;
            waitForTask(connection, task, 1000, timeout);
            checkForSuccess(connection, task);
            final VDI dvdi = Types.toVDI(task, connection);

            return new MigrateVolumeAnswer(cmd, true, null, dvdi.getUuid(connection));
        } catch (final Exception e) {
            final String msg = "Catch Exception " + e.getClass().getName() + " due to " + e.toString();
            s_logger.error(msg, e);
            return new MigrateVolumeAnswer(cmd, false, msg, null);
        }
    }

    @Override
    protected void plugDom0Vif(final Connection conn, final VIF dom0Vif) throws XmlRpcException, XenAPIException {
        // do nothing. In xenserver 6.1 and beyond this step isn't needed.
    }
}
