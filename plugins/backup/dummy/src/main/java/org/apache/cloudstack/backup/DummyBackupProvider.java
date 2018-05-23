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
package org.apache.cloudstack.backup;

import com.cloud.utils.component.AdapterBase;
import org.apache.log4j.Logger;

import java.util.Arrays;
import java.util.List;

public class DummyBackupProvider extends AdapterBase implements BackupProvider {

    private static final Logger s_logger = Logger.getLogger(DummyBackupProvider.class);

    @Override
    public String getName() {
        return "dummy";
    }

    @Override
    public String getDescription() {
        return "Dummy B&R Plugin";
    }

    @Override
    public boolean assignVMToBackupPolicy(String vmUuid, String policyUuid) {
        s_logger.debug("Assigning VM " + vmUuid + " to backup policy " + policyUuid);
        return true;
    }

    @Override
    public List<BackupPolicy> listBackupPolicies() {
        s_logger.debug("Listing backup policies on Dummy B&R Plugin");
        BackupPolicy policy1 = new BackupPolicyTO("aaaa-aaaa", "Golden Policy", "Gold description");
        BackupPolicy policy2 = new BackupPolicyTO("bbbb-bbbb", "Silver Policy", "Silver description");
        return Arrays.asList(policy1, policy2);
    }

    @Override
    public boolean isBackupPolicy(String uuid) {
        s_logger.debug("Checking if backup policy exists on the Dummy Backup Provider");
        return true;
    }

    @Override
    public boolean restoreVMFromBackup(String vmUuid, String backupUuid) {
        s_logger.debug("Restoring vm " + vmUuid + "from backup " + backupUuid + " on the Dummy Backup Provider");
        return true;
    }
}
