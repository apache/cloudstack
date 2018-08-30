//
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
//

package com.cloud.agent.api;

import java.util.UUID;

/**
 * This command will tell the hypervisor to cleanup any resources dedicated for
 * this particular nic. Orginally implemented to cleanup dedicated portgroups
 * from a vmware standard switch
 *
 */
public class UnregisterNicCommand extends Command {
    private String vmName;
    private String trafficLabel;
    private UUID nicUuid;

    public UnregisterNicCommand(String vmName, String trafficLabel, UUID nicUuid) {
        this.nicUuid = nicUuid;
        this.vmName = vmName;
        this.trafficLabel = trafficLabel;
    }

    public UUID getNicUuid() {
        return nicUuid;
    }

    public String getVmName() {
        return vmName;
    }

    public String getTrafficLabel() {
        return trafficLabel;
    }

    @Override
    public boolean executeInSequence() {
        return false;
    }

}
