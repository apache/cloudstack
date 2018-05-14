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

import java.util.List;

import org.apache.cloudstack.framework.backup.BackupPolicy;

import com.cloud.utils.component.Adapter;

/**
 * Backup and Recovery Provider
 */
public interface BackupProviderDriver extends Adapter {

    /**
     * Register Backup and Recovery Provider
     */
    boolean registerProvider(long zoneId, String name, String url, String username, String password);

    /**
     * True if policy exists on the provider, false if not
     */
    boolean policyExists(String policyId, String policyName);

    /**
     * List existing Backup Policies on the provider
     */
    List<BackupPolicy> listBackupPolicies(long providerId);

    /**
     * Assign a VM to an existing backup policy
     */
    boolean assignVMToBackupPolicy(String policyId, String vmId);

    /**
     * Unregister Backup and Recovery Provider
     */
    boolean unregisterProvider();

    void restoreVMFromBackup();
    void restoreAndAttachVolumeToVM();
}
