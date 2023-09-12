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

import java.net.URI;
import java.util.HashMap;
import java.util.Set;

import org.apache.log4j.Logger;

import com.cloud.agent.api.Answer;
import com.cloud.agent.api.storage.PrimaryStorageDownloadAnswer;
import com.cloud.agent.api.storage.PrimaryStorageDownloadCommand;
import com.cloud.hypervisor.xenserver.resource.CitrixResourceBase;
import com.cloud.resource.CommandWrapper;
import com.cloud.resource.ResourceWrapper;
import com.xensource.xenapi.Connection;
import com.xensource.xenapi.SR;
import com.xensource.xenapi.VDI;

@ResourceWrapper(handles =  PrimaryStorageDownloadCommand.class)
public final class CitrixPrimaryStorageDownloadCommandWrapper extends CommandWrapper<PrimaryStorageDownloadCommand, Answer, CitrixResourceBase> {

    private static final Logger s_logger = Logger.getLogger(CitrixPrimaryStorageDownloadCommandWrapper.class);

    @Override
    public Answer execute(final PrimaryStorageDownloadCommand command, final CitrixResourceBase citrixResourceBase) {
        final String tmplturl = command.getUrl();
        final String poolName = command.getPoolUuid();
        final int wait = command.getWait();
        try {
            final URI uri = new URI(tmplturl);
            final String tmplpath = uri.getHost() + ":" + uri.getPath();
            final Connection conn = citrixResourceBase.getConnection();
            SR poolsr = null;
            final Set<SR> srs = SR.getByNameLabel(conn, poolName);
            if (srs.size() != 1) {
                final String msg = "There are " + srs.size() + " SRs with same name: " + poolName;
                s_logger.warn(msg);
                return new PrimaryStorageDownloadAnswer(msg);
            } else {
                poolsr = srs.iterator().next();
            }
            final String pUuid = poolsr.getUuid(conn);
            final boolean isISCSI = citrixResourceBase.IsISCSI(poolsr.getType(conn));
            final String uuid = citrixResourceBase.copyVhdFromSecondaryStorage(conn, tmplpath, pUuid, wait);
            final VDI tmpl = citrixResourceBase.getVDIbyUuid(conn, uuid);
            final VDI snapshotvdi = tmpl.snapshot(conn, new HashMap<String, String>());
            final String snapshotUuid = snapshotvdi.getUuid(conn);
            snapshotvdi.setNameLabel(conn, "Template " + command.getName());
            final String parentuuid = citrixResourceBase.getVhdParent(conn, pUuid, snapshotUuid, isISCSI);
            final VDI parent = citrixResourceBase.getVDIbyUuid(conn, parentuuid);
            final Long phySize = parent.getPhysicalUtilisation(conn);
            tmpl.destroy(conn);
            poolsr.scan(conn);
            try {
                Thread.sleep(5000);
            } catch (final Exception e) {
            }
            return new PrimaryStorageDownloadAnswer(snapshotvdi.getUuid(conn), phySize);
        } catch (final Exception e) {
            final String msg = "Catch Exception " + e.getClass().getName() + " on host:" + citrixResourceBase.getHost().getUuid() + " for template: " + tmplturl + " due to "
                    + e.toString();
            s_logger.warn(msg, e);
            return new PrimaryStorageDownloadAnswer(msg);
        }
    }
}
