/*
 * Licensed to the Apache Software Foundation (ASF) under one
 * or more contributor license agreements.  See the NOTICE file
 * distributed with this work for additional information
 * regarding copyright ownership.  The ASF licenses this file
 * to you under the Apache License, Version 2.0 (the
 * "License"); you may not use this file except in compliance
 * with the License.  You may obtain a copy of the License at
 *
 *   http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing,
 * software distributed under the License is distributed on an
 * "AS IS" BASIS, WITHOUT WARRANTIES OR CONDITIONS OF ANY
 * KIND, either express or implied.  See the License for the
 * specific language governing permissions and limitations
 * under the License.
 */
package com.cloud.hypervisor.xenserver.resource;

import javax.ejb.Local;

import org.apache.cloudstack.hypervisor.xenserver.XenServerResourceNewBase;
import org.apache.log4j.Logger;
import org.apache.xmlrpc.XmlRpcException;

import com.cloud.resource.ServerResource;
import com.cloud.storage.resource.StorageSubsystemCommandHandler;
import com.cloud.storage.resource.StorageSubsystemCommandHandlerBase;
import com.cloud.utils.exception.CloudRuntimeException;
import com.cloud.utils.ssh.SSHCmdHelper;
import com.xensource.xenapi.Connection;
import com.xensource.xenapi.Host;
import com.xensource.xenapi.Types;
import com.xensource.xenapi.VM;

@Local(value=ServerResource.class)
public class Xenserver625Resource extends XenServerResourceNewBase {

    private static final Logger s_logger = Logger.getLogger(Xenserver625Resource.class);

    @Override
    protected String getPatchFilePath() {
        return "scripts/vm/hypervisor/xenserver/xenserver62/patch";
    }

    @Override
    protected StorageSubsystemCommandHandler buildStorageHandler() {
        XenServerStorageProcessor processor = new Xenserver625StorageProcessor(this);
        return new StorageSubsystemCommandHandlerBase(processor);
    }

    @Override
    public void umountSnapshotDir(final Connection conn, final Long dcId) {}

    @Override
    public boolean setupServer(final Connection conn,final Host host) {
        final com.trilead.ssh2.Connection sshConnection = new com.trilead.ssh2.Connection(_host.getIp(), 22);
        try {
            sshConnection.connect(null, 60000, 60000);
            if (!sshConnection.authenticateWithPassword(_username, _password.peek())) {
                throw new CloudRuntimeException("Unable to authenticate");
            }

            final String cmd = "rm -f /opt/xensource/sm/hostvmstats.py " +
                    "/opt/xensource/bin/copy_vhd_to_secondarystorage.sh " +
                    "/opt/xensource/bin/copy_vhd_from_secondarystorage.sh " +
                    "/opt/xensource/bin/create_privatetemplate_from_snapshot.sh " +
                    "/opt/xensource/bin/vhd-util " +
                    "/opt/cloud/bin/copy_vhd_to_secondarystorage.sh " +
                    "/opt/cloud/bin/copy_vhd_from_secondarystorage.sh " +
                    "/opt/cloud/bin/create_privatetemplate_from_snapshot.sh " +
                    "/opt/cloud/bin/vhd-util";

            SSHCmdHelper.sshExecuteCmd(sshConnection, cmd);
        } catch (final Exception e) {
            s_logger.debug("Catch exception " + e.toString(), e);
        } finally {
            sshConnection.close();
        }
        return super.setupServer(conn, host);
    }

    @Override
    public String revertToSnapshot(final Connection conn, final VM vmSnapshot,
            final String vmName, final String oldVmUuid, final Boolean snapshotMemory, final String hostUUID)
                    throws Types.XenAPIException, XmlRpcException {

        final String results = callHostPluginAsync(conn, "vmopsSnapshot",
                "revert_memory_snapshot", 10 * 60 * 1000, "snapshotUUID",
                vmSnapshot.getUuid(conn), "vmName", vmName, "oldVmUuid",
                oldVmUuid, "snapshotMemory", snapshotMemory.toString(), "hostUUID", hostUUID);
        String errMsg = null;
        if (results == null || results.isEmpty()) {
            errMsg = "revert_memory_snapshot return null";
        } else {
            if (results.equals("0")) {
                return results;
            } else {
                errMsg = "revert_memory_snapshot exception";
            }
        }
        s_logger.warn(errMsg);
        throw new CloudRuntimeException(errMsg);
    }

}
