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

import com.cloud.agent.api.to.NicTO;

/**
 * Applies the full desired VLAN membership (primary + associated networks) to an already-plugged
 * multi-VLAN trunk nic on a running Instance. Carries desired state, not a delta, so it is
 * idempotent and safe to retry.
 */
public class UpdateNicVlanMembershipCommand extends Command {

    private NicTO nic;
    private String instanceName;

    protected UpdateNicVlanMembershipCommand() {
    }

    public UpdateNicVlanMembershipCommand(NicTO nic, String instanceName) {
        this.nic = nic;
        this.instanceName = instanceName;
    }

    public NicTO getNic() {
        return nic;
    }

    public String getVmName() {
        return instanceName;
    }

    @Override
    public boolean executeInSequence() {
        return true;
    }
}
