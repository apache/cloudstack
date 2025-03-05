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

public class UpdateNetrisVnetCommand extends NetrisCommand{
    private String prevNetworkName;
    private String vpcName;
    private Long vpcId;

    public UpdateNetrisVnetCommand(long zoneId, Long accountId, Long domainId, String name, Long id, boolean isVpc) {
        super(zoneId, accountId, domainId, name, id, isVpc);
    }

    public String getPrevNetworkName() {
        return prevNetworkName;
    }

    public void setPrevNetworkName(String prevNetworkName) {
        this.prevNetworkName = prevNetworkName;
    }

    public String getVpcName() {
        return vpcName;
    }

    public void setVpcName(String vpcName) {
        this.vpcName = vpcName;
    }

    public Long getVpcId() {
        return vpcId;
    }

    public void setVpcId(Long vpcId) {
        this.vpcId = vpcId;
    }
}
