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
package com.cloud.dc;

import org.apache.cloudstack.acl.InfrastructureEntity;
import org.apache.cloudstack.api.Identity;
import org.apache.cloudstack.api.InternalIdentity;

public interface Vlan extends InfrastructureEntity, InternalIdentity, Identity {
    public enum VlanType {
        DirectAttached,
        VirtualNetwork
    }

    public final static String UNTAGGED = "untagged";

    public String getVlanTag();

    public String getVlanGateway();

    public String getVlanNetmask();

    public long getDataCenterId();

    public String getIpRange();

    public VlanType getVlanType();

    public Long getNetworkId();

    public Long getPhysicalNetworkId();

	public String getIp6Gateway();

	public String getIp6Cidr();

	public String getIp6Range();
}
