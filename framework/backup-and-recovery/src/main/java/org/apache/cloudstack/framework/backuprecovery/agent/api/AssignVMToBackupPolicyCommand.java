/*
 * Licensed to the Apache Software Foundation (ASF) under one
 * or more contributor license agreements.  See the NOTICE file
 * distributed with this work for additional information
 * regarding copyright ownership.  The ASF licenses this file
 * to you under the Apache License, Version 2.0 (the
 * "License"); you may not use this file except in compliance
 * with the License.  You may obtain a copy of the License at
 *
 *   http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing,
 * software distributed under the License is distributed on an
 * "AS IS" BASIS, WITHOUT WARRANTIES OR CONDITIONS OF ANY
 * KIND, either express or implied.  See the License for the
 * specific language governing permissions and limitations
 * under the License.
 */

package org.apache.cloudstack.framework.backuprecovery.agent.api;

import com.cloud.agent.api.Command;

public class AssignVMToBackupPolicyCommand extends Command {

    private long vmId;
    private String vmUuid;
    private String backupPolicyUuid;

    public AssignVMToBackupPolicyCommand(final long vmId, final String vmUuid, final String backupPolicyUuid) {
        this.vmId = vmId;
        this.vmUuid = vmUuid;
        this.backupPolicyUuid = backupPolicyUuid;
    }

    public long getVmId() {
        return vmId;
    }

    public String getVmUuid() {
        return vmUuid;
    }

    public String getBackupPolicyUuid() {
        return backupPolicyUuid;
    }

    @Override
    public boolean executeInSequence() {
        return false;
    }
}
