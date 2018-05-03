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

package org.apache.cloudstack.framework.backuprecovery;

import com.cloud.exception.AgentUnavailableException;
import com.cloud.exception.OperationTimedoutException;
import com.cloud.utils.component.Adapter;
import org.apache.cloudstack.framework.backuprecovery.api.AddBackupRecoveryPolicyCmd;
import org.apache.cloudstack.framework.backuprecovery.api.AssignBackupPolicyCmd;
import org.apache.cloudstack.framework.backuprecovery.api.ListBackupRecoveryPoliciesCmd;
import org.apache.cloudstack.framework.backuprecovery.api.ListBackupRecoveryProviderPoliciesCmd;
import org.apache.cloudstack.framework.backuprecovery.api.response.BackupRecoveryProviderPolicyResponse;
import org.apache.cloudstack.framework.backuprecovery.impl.BackupPolicyVO;

import java.util.List;

/**
 * Backup and Recovery Provider
 */
public interface BackupRecoveryProvider extends Adapter {

    /**
     * List existing Backup Policies on the provider
     */
    List<BackupRecoveryProviderPolicyResponse> listBackupPolicies(ListBackupRecoveryProviderPoliciesCmd cmd) throws AgentUnavailableException, OperationTimedoutException;

    /**
     * List backup policies mapped to provider policies
     */
    List<BackupPolicyVO> listBackupPolicies(ListBackupRecoveryPoliciesCmd cmd);

    /**
     * Add a Backup Policy by mapping it to a policy on the provider
     */
    BackupPolicyVO addBackupPolicy(AddBackupRecoveryPolicyCmd cmd) throws AgentUnavailableException, OperationTimedoutException;

    /**
     * Assign a VM to an existing backup policy
     */
    boolean assignVMToBackupPolicy(AssignBackupPolicyCmd cmd) throws AgentUnavailableException, OperationTimedoutException;

    void restoreVMFromBackup();
    void restoreAndAttachVolumeToVM();

}
