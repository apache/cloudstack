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

public class DeleteNetrisVnetCommand extends NetrisCommand {
    private String vpcName;
    private Long vpcId;
    private final String vNetCidr;
    private String vNetV6Cidr;

    public DeleteNetrisVnetCommand(long zoneId, long accountId, long domainId, String name, long id, String vpcName, Long vpcId, String vNetCidr, boolean isVpc) {
        super(zoneId, accountId, domainId, name, id, isVpc);
        this.vpcName = vpcName;
        this.vpcId = vpcId;
        this.vNetCidr = vNetCidr;
    }

    public String getVpcName() {
        return vpcName;
    }

    public Long getVpcId() {
        return vpcId;
    }

    public String getVNetCidr() {
        return vNetCidr;
    }

    public String getvNetV6Cidr() {
        return vNetV6Cidr;
    }

    public void setvNetV6Cidr(String vNetV6Cidr) {
        this.vNetV6Cidr = vNetV6Cidr;
    }
}
