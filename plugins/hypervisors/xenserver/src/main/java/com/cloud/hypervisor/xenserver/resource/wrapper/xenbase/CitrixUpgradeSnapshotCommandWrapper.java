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

import org.apache.log4j.Logger;

import com.cloud.agent.api.Answer;
import com.cloud.agent.api.UpgradeSnapshotCommand;
import com.cloud.hypervisor.xenserver.resource.CitrixResourceBase;
import com.cloud.resource.CommandWrapper;
import com.cloud.resource.ResourceWrapper;
import com.xensource.xenapi.Connection;

@ResourceWrapper(handles =  UpgradeSnapshotCommand.class)
public final class CitrixUpgradeSnapshotCommandWrapper extends CommandWrapper<UpgradeSnapshotCommand, Answer, CitrixResourceBase> {

    private static final Logger s_logger = Logger.getLogger(CitrixUpgradeSnapshotCommandWrapper.class);

    @Override
    public Answer execute(final UpgradeSnapshotCommand command, final CitrixResourceBase citrixResourceBase) {
        final String secondaryStorageUrl = command.getSecondaryStorageUrl();
        final String backedUpSnapshotUuid = command.getSnapshotUuid();
        final Long volumeId = command.getVolumeId();
        final Long accountId = command.getAccountId();
        final Long templateId = command.getTemplateId();
        final Long tmpltAcountId = command.getTmpltAccountId();
        final String version = command.getVersion();

        if (!version.equals("2.1")) {
            return new Answer(command, true, "success");
        }
        try {
            final Connection conn = citrixResourceBase.getConnection();
            final URI uri = new URI(secondaryStorageUrl);
            final String secondaryStorageMountPath = uri.getHost() + ":" + uri.getPath();
            final String snapshotPath = secondaryStorageMountPath + "/snapshots/" + accountId + "/" + volumeId + "/" + backedUpSnapshotUuid + ".vhd";
            final String templatePath = secondaryStorageMountPath + "/template/tmpl/" + tmpltAcountId + "/" + templateId;
            citrixResourceBase.upgradeSnapshot(conn, templatePath, snapshotPath);
            return new Answer(command, true, "success");
        } catch (final Exception e) {
            final String details = "upgrading snapshot " + backedUpSnapshotUuid + " failed due to " + e.toString();
            s_logger.error(details, e);

        }
        return new Answer(command, false, "failure");
    }
}
