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

public class CreateNsxTier1GatewayCommand extends NsxCommand {

    private Long networkResourceId;
    private String networkResourceName;
    private boolean isResourceVpc;
    private boolean sourceNatEnabled;

    public CreateNsxTier1GatewayCommand(long domainId, long accountId, long zoneId,
                                        Long networkResourceId, String networkResourceName, boolean isResourceVpc,
                                        boolean sourceNatEnabled) {
        super(domainId, accountId, zoneId);
        this.networkResourceId = networkResourceId;
        this.networkResourceName = networkResourceName;
        this.isResourceVpc = isResourceVpc;
        this.sourceNatEnabled = sourceNatEnabled;
    }

    public Long getNetworkResourceId() {
        return networkResourceId;
    }

    public boolean isResourceVpc() {
        return isResourceVpc;
    }

    public String getNetworkResourceName() {
        return networkResourceName;
    }

    public boolean isSourceNatEnabled() {
        return sourceNatEnabled;
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;
        if (!super.equals(o)) return false;
        CreateNsxTier1GatewayCommand that = (CreateNsxTier1GatewayCommand) o;
        return Objects.equals(networkResourceName, that.networkResourceName);
    }

    @Override
    public int hashCode() {
        return Objects.hash(super.hashCode(), networkResourceName);
    }
}
