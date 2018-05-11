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

import com.cloud.utils.component.AdapterBase;
import org.apache.cloudstack.framework.br.BRPolicy;
import org.apache.log4j.Logger;

import java.util.Arrays;
import java.util.List;
import java.util.UUID;

public class DummyBRProvider extends AdapterBase implements BRProviderDriver {

    private static final Logger s_logger = Logger.getLogger(DummyBRProvider.class);

    @Override
    public boolean registerProvider(long zoneId, String name, String url, String username, String password) {
        s_logger.debug("Registering Dummy Backup and Recovery provider");
        return true;
    }

    @Override
    public List<BRPolicy> listBackupPolicies(long providerId) {
        s_logger.debug("Listing Dummy Backup policies");
        BRPolicy policy1 = new BRPolicyTO(1l, UUID.randomUUID().toString(), "Golden Policy", "aaaa-aaaa", providerId);
        BRPolicy policy2 = new BRPolicyTO(1l, UUID.randomUUID().toString(), "Silver Policy", "bbbb-bbbb", providerId);
        return Arrays.asList(policy1, policy2);
    }

    @Override
    public boolean policyExists(String policyId, String policyName) {
        s_logger.debug("Checking if policy " + policyId + " " + policyName + " exists");
        return true;
    }

    @Override
    public boolean assignVMToBackupPolicy(String policyId, String vmId) {
        s_logger.debug("Assigning VM " + vmId+ " to the policy " + policyId);
        return true;
    }

    @Override
    public boolean unregisterProvider() {
        s_logger.debug("Unregistering Dummy Backup and Recovery provider");
        return true;
    }

    @Override
    public void restoreVMFromBackup() {

    }

    @Override
    public void restoreAndAttachVolumeToVM() {

    }

}
