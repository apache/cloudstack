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

import java.util.Map;

import org.apache.cloudstack.acl.InfrastructureEntity;
import org.apache.cloudstack.kernel.Partition;

import com.cloud.org.Grouping;

/**
 *
 */
public interface DataCenter extends InfrastructureEntity, Grouping, Partition {

    public enum NetworkType {
        Basic, Advanced,
    }

    public enum Type {
        Core, Edge,
    }

    String getDns1();

    String getDns2();

    String getIp6Dns1();

    String getIp6Dns2();

    String getGuestNetworkCidr();

    String getName();

    Long getDomainId();

    String getDescription();

    String getDomain();

    NetworkType getNetworkType();

    String getInternalDns1();

    String getInternalDns2();

    String getDnsProvider();

    String getGatewayProvider();

    String getFirewallProvider();

    String getDhcpProvider();

    String getLoadBalancerProvider();

    String getUserDataProvider();

    String getVpnProvider();

    boolean isSecurityGroupEnabled();

    Map<String, String> getDetails();

    void setDetails(Map<String, String> details);

    AllocationState getAllocationState();

    String getZoneToken();

    boolean isLocalStorageEnabled();

    int getSortKey();

    Type getType();
}
