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

import org.apache.cloudstack.framework.backuprecovery.api.AddBackupRecoveryProviderCmd;
import org.apache.cloudstack.framework.backuprecovery.api.DeleteBackupRecoveryProviderCmd;
import org.apache.cloudstack.framework.backuprecovery.api.ListBackupRecoveryProvidersCmd;
import org.apache.cloudstack.framework.backuprecovery.api.response.BackupPolicyResponse;
import org.apache.cloudstack.framework.backuprecovery.api.response.BackupRecoveryProviderResponse;
import org.apache.cloudstack.framework.backuprecovery.impl.BackupPolicyVO;
import org.apache.cloudstack.framework.backuprecovery.impl.BackupRecoveryProviderVO;

import java.lang.reflect.InvocationTargetException;
import java.util.List;

public interface BackupRecoveryService {

    /**
     * Add a new Backup and Recovery provider
     */
    BackupRecoveryProviderVO addBackupRecoveryProvider(AddBackupRecoveryProviderCmd cmd) throws InvocationTargetException, NoSuchMethodException, InstantiationException, IllegalAccessException;

    /**
     * Delete existing Backup and Recovery provider
     */
    boolean deleteBackupRecoveryProvider(DeleteBackupRecoveryProviderCmd cmd);

    /**
     * List existing Backup and Recovery providers
     * @param cmd
     * @return
     */
    List<BackupRecoveryProviderVO> listBackupRecoveryProviders(ListBackupRecoveryProvidersCmd cmd);

    /**
     * Generate a response from the Backup and Recovery Provider VO
     */
    BackupRecoveryProviderResponse createBackupRecoveryProviderResponse(BackupRecoveryProviderVO backupRecoveryProviderVO);

    /**
     * Generate a response from the Backup Policy VO
     */
    BackupPolicyResponse createBackupPolicyResponse(BackupPolicyVO policyVO);
}
