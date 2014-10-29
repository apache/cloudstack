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
package com.cloud.network.vpc;


public class PrivateGatewayProfile implements PrivateGateway {
    VpcGateway vpcGateway;
    long physicalNetworkId;

    /**
     * @param vpcGateway
     * @param physicalNetworkId TODO
     */
    public PrivateGatewayProfile(VpcGateway vpcGateway, long physicalNetworkId) {
        super();
        this.vpcGateway = vpcGateway;
        this.physicalNetworkId = physicalNetworkId;
    }

    @Override
    public long getId() {
        return vpcGateway.getId();
    }

    @Override
    public String getIp4Address() {
        return vpcGateway.getIp4Address();
    }

    @Override
    public Type getType() {
        return vpcGateway.getType();
    }

    @Override
    public Long getVpcId() {
        return vpcGateway.getVpcId();
    }

    @Override
    public long getZoneId() {
        return vpcGateway.getZoneId();
    }

    @Override
    public long getNetworkId() {
        return vpcGateway.getNetworkId();
    }

    @Override
    public String getUuid() {
        return vpcGateway.getUuid();
    }

    @Override
    public String getBroadcastUri() {
        return vpcGateway.getBroadcastUri();
    }

    @Override
    public String getGateway() {
        return vpcGateway.getGateway();
    }

    @Override
    public String getNetmask() {
        return vpcGateway.getNetmask();
    }

    @Override
    public long getPhysicalNetworkId() {
        return physicalNetworkId;
    }

    @Override
    public long getAccountId() {
        return vpcGateway.getAccountId();
    }

    @Override
    public long getDomainId() {
        return vpcGateway.getDomainId();
    }

    @Override
    public State getState() {
        return vpcGateway.getState();
    }

    @Override
    public boolean getSourceNat() {
        return vpcGateway.getSourceNat();
    }

    @Override
    public long getNetworkACLId() {
        return vpcGateway.getNetworkACLId();
    }

    @Override
    public Class<?> getEntityType() {
        return VpcGateway.class;
    }
}
