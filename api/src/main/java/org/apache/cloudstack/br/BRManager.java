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

package org.apache.cloudstack.br;

import com.cloud.utils.component.Manager;
import com.cloud.utils.component.PluggableService;
import org.apache.cloudstack.api.response.BRPolicyResponse;
import org.apache.cloudstack.api.response.BRProviderResponse;
import org.apache.cloudstack.framework.br.BRPolicy;
import org.apache.cloudstack.framework.br.BRProvider;
import org.apache.cloudstack.framework.br.BRService;
import org.apache.cloudstack.framework.config.Configurable;

/**
 * Backup and Recover Manager Interface
 */
public interface BRManager extends BRService, Configurable, PluggableService, Manager {

    /**
     * Get the Backup and Recovery Provider for the policy id, null if not registered
     */
    BRProviderDriver getBRProviderFromPolicy(String policyId);

    /**
     * Get the Backup and Recovery Provider for the provider id, null if not registered
     */
    BRProviderDriver getBRProviderFromProvider(String providerId);

    /**
     * Generate a response from the Backup and Recovery Provider VO
     */
    BRProviderResponse createBRProviderResponse(BRProvider backupRecoveryProviderVO);

    /**
     * Generate a response from the Backup Policy VO
     */
    BRPolicyResponse createBackupPolicyResponse(BRPolicy policyVO);

    /**
     * Return Backup and Recovery id from provider uuid
     */
    long getProviderId(String providerId);
}
