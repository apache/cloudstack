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

import org.apache.cloudstack.api.response.VMBackupResponse;
import org.apache.cloudstack.backup.VMBackup;
import org.apache.cloudstack.backup.VMBackupVO;

import com.cloud.utils.db.GenericDao;

public interface VMBackupDao extends GenericDao<VMBackupVO, Long> {

    List<VMBackup> listByVmId(Long zoneId, Long vmId);
    List<VMBackup> syncVMBackups(Long zoneId, Long vmId, List<VMBackup> externalBackups);
    List<VMBackup> listByZoneAndState(Long zoneId, VMBackup.Status state);

    VMBackupResponse newBackupResponse(VMBackup backup);
    VMBackupVO getBackupVO(VMBackup backup);
}
