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

package org.apache.cloudstack.api;

import java.util.ArrayList;
import java.util.List;

import org.apache.cloudstack.api.response.BackupOfferingResponse;
import org.apache.cloudstack.api.response.ListResponse;
import org.apache.cloudstack.api.response.BackupResponse;
import org.apache.cloudstack.api.response.BackupRestorePointResponse;
import org.apache.cloudstack.backup.BackupOffering;
import org.apache.cloudstack.backup.Backup;
import org.apache.cloudstack.context.CallContext;

public abstract class BaseBackupListCmd extends BaseListCmd {

    protected void setupResponseBackupOfferingsList(final List<BackupOffering> offerings) {
        final ListResponse<BackupOfferingResponse> response = new ListResponse<>();
        final List<BackupOfferingResponse> responses = new ArrayList<>();
        for (final BackupOffering offering : offerings) {
            if (offering == null) {
                continue;
            }
            BackupOfferingResponse backupOfferingResponse = _responseGenerator.createBackupOfferingResponse(offering);
            responses.add(backupOfferingResponse);
        }
        response.setResponses(responses);
        response.setResponseName(getCommandName());
        setResponseObject(response);
    }

    protected void setupResponseBackupList(final List<Backup> backups, final List<Backup.RestorePoint> restorePoints) {
        final ListResponse<BackupResponse> response = new ListResponse<>();
        final List<BackupResponse> responses = new ArrayList<>();
        for (Backup backup : backups) {
            if (backup == null) {
                continue;
            }
            BackupResponse backupResponse = _responseGenerator.createBackupResponse(backup);
            if (restorePoints != null && !restorePoints.isEmpty()) {
                final List<BackupRestorePointResponse> restorePointResponses = new ArrayList<>();
                for (Backup.RestorePoint rp : restorePoints) {
                    if (rp == null) {
                        continue;
                    }
                    BackupRestorePointResponse rpResponse = new BackupRestorePointResponse();
                    rpResponse.setId(rp.getId());
                    rpResponse.setCreated(rp.getCreated());
                    rpResponse.setType(rp.getType());
                    restorePointResponses.add(rpResponse);
                }
                backupResponse.setRestorePoints(restorePointResponses);
            }
            responses.add(backupResponse);
        }
        response.setResponses(responses);
        response.setResponseName(getCommandName());
        setResponseObject(response);
    }

    @Override
    public long getEntityOwnerId() {
        return CallContext.current().getCallingAccount().getId();
    }
}
