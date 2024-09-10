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

package org.apache.cloudstack.backup.dao;

import java.util.List;

import org.apache.cloudstack.api.response.BackupResponse;
import org.apache.cloudstack.backup.Backup;
import org.apache.cloudstack.backup.BackupVO;

import com.cloud.utils.db.GenericDao;

public interface BackupDao extends GenericDao<BackupVO, Long> {

    Backup findByVmId(Long vmId);
    Backup findByVmIdIncludingRemoved(Long vmId);

    List<Backup> listByVmId(Long zoneId, Long vmId);
    List<Backup> listByAccountId(Long accountId);
    List<Backup> syncBackups(Long zoneId, Long vmId, List<Backup> externalBackups);
    BackupVO getBackupVO(Backup backup);
    List<Backup> listByOfferingId(Long backupOfferingId);
    BackupResponse newBackupResponse(Backup backup);
}
