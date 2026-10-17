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
package org.apache.cloudstack.agent.api;

import java.util.Objects;

public class GetNsxVpnSessionStatusCommand extends NsxCommand {

    private Long vpcId;
    private String vpcName;
    private long connectionId;

    public GetNsxVpnSessionStatusCommand(long domainId, long accountId, long zoneId,
                                         Long vpcId, String vpcName, long connectionId) {
        super(domainId, accountId, zoneId);
        this.vpcId = vpcId;
        this.vpcName = vpcName;
        this.connectionId = connectionId;
    }

    public Long getVpcId() {
        return vpcId;
    }

    public String getVpcName() {
        return vpcName;
    }

    public long getConnectionId() {
        return connectionId;
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) {
            return true;
        }
        if (o == null || getClass() != o.getClass() || !super.equals(o)) {
            return false;
        }
        GetNsxVpnSessionStatusCommand that = (GetNsxVpnSessionStatusCommand) o;
        return connectionId == that.connectionId && Objects.equals(vpcId, that.vpcId)
                && Objects.equals(vpcName, that.vpcName);
    }

    @Override
    public int hashCode() {
        return Objects.hash(super.hashCode(), vpcId, vpcName, connectionId);
    }
}
