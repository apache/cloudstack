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
package com.cloud.utils;

import org.apache.commons.lang.RandomStringUtils;
import org.apache.commons.lang.StringUtils;

import java.util.ArrayList;
import java.util.List;

// TODO : will determine later
public class TungstenUtils {
    private static final String tungstenBridge = "-tungsten";
    private static final String proxyVm = "proxyvm";
    private static final String secstoreVm = "secstorevm";
    private static final String userVm = "userVm";
    private static final String guestType = "guest";
    private static final String publicType = "public";
    private static final String managementType = "management";
    private static final String controlType = "control";

    public static final int MAX_CIDR = 32;
    public static final int WEB_SERVICE_PORT = 8080;
    public static final String ALL_IP4_PREFIX = "0.0.0.0";
    public static final String ANY_PROTO = "any";
    public static final String DENY_ACTION = "deny";
    public static final String PASS_ACTION = "pass";
    public static final String ONE_WAY_DIRECTION = ">";
    public static final String TWO_WAY_DIRECTION = "<>";
    public static final String FABRIC_NETWORK_FQN = "default-domain:default-project:ip-fabric";

    public static String getTapName(final String macAddress) {
        return "tap" + macAddress.replace(":", "");
    }

    public static boolean isTungstenBridge(final String bridgeName) {
        return bridgeName.contains(tungstenBridge);
    }

    public static String getBridgeName() {
        return tungstenBridge;
    }

    public static String getVmiName(String trafficType, String vmType, String vmName, long nicId) {
        if (nicId != 0 && trafficType == getGuestType())
            return "vmi" + trafficType + vmType + nicId;
        else
            return "vmi" + trafficType + vmType + vmName;
    }

    public static String getInstanceIpName(String trafficType, String vmType, String vmName, long nicId) {
        if (nicId != 0 && trafficType == getGuestType())
            return "instanceIp" + trafficType + vmType + nicId;
        else
            return "instanceIp" + trafficType + vmType + vmName;
    }

    public static String getLogicalRouterName(long networkId) {
        return "logicalRouter" + networkId;
    }

    public static String getNetworkGatewayVmiName(long vnId) {
        return "internalGatewayVmi" + vnId;
    }

    public static String getNetworkGatewayIiName(long pvnId) {
        return "internalGatewayIi" + pvnId;
    }

    public static String getPublicNetworkName(long zoneId) {
        return "publicNetwork" + zoneId;
    }

    public static String getGuestNetworkName(String networkName) {
        return networkName + "-" + RandomStringUtils.random(6, true, true);
    }

    public static String getManagementNetworkName(long mvnId) {
        return "managementNetwork" + mvnId;
    }

    public static String getControlNetworkName(long cvnId) {
        return "controlNetwork" + cvnId;
    }

    public static String getProxyVm() {
        return proxyVm;
    }

    public static String getSecstoreVm() {
        return secstoreVm;
    }

    public static String getUserVm() {
        return userVm;
    }

    public static String getGuestType() {
        return guestType;
    }

    public static String getPublicType() {
        return publicType;
    }

    public static String getManagementType() {
        return managementType;
    }

    public static String getVgwName(long zoneId) {
        return "vgw" + zoneId;
    }

    public static String getVrfNetworkName(List<String> networkQualifiedName) {
        List<String> vrfList = new ArrayList<>(networkQualifiedName);
        vrfList.add(networkQualifiedName.get(networkQualifiedName.size() - 1));
        return StringUtils.join(vrfList, ":");
    }

    public static String getFloatingIpPoolName(long zoneId) {
        return "floating-ip-pool" + zoneId;
    }

    public static String getFloatingIpName(long nicId) {
        return "floating-ip" + nicId;
    }

    public static String getSnatNetworkStartName(List<String> projectFqn, String logicalRouterUuid) {
        return StringUtils.join(projectFqn, "__") + "__snat_" + logicalRouterUuid;
    }

    public static String getSnatNetworkEndName() {
        return "right";
    }

    public static String getPublicNetworkPolicyName(long publicIpAddressId) {
        return "public-network-policy" + publicIpAddressId;
    }

    public static String getVirtualNetworkPolicyName(long networkId) {
        return "virtual-network-policy" + networkId;
    }

    public static String getDefaultPublicNetworkPolicyName(long vlanId) {
        return "default-public-network-policy" + vlanId;
    }

    public static String getFabricNetworkPolicyName() {
        return "fabric-network-policy";
    }

    public static String getRuleNetworkPolicyName(long ruleId) {
        return "rule-network-policy" + ruleId;
    }

    public static String getSubnetName(long networkId) {
        return "subnet-name" + networkId;
    }

    public static String getLoadBalancerVmiName(long publicIpId) {
        return "loadbalancer-vmi" + publicIpId;
    }

    public static String getLoadBalancerIiName(long publicIpId) {
        return "loadbalancer-ii" + publicIpId;
    }

    public static String getLoadBalancerName(long publicIpId) {
        return "loadbalancer" + publicIpId;
    }

    public static String getLoadBalancerListenerName(long ruleId) {
        return "loadbalancer-listener" + ruleId;
    }

    public static String getLoadBalancerHealthMonitorName(long publicIpId) {
        return "loadbalancer-healthmonitor" + publicIpId;
    }

    public static String getLoadBalancerPoolName(long ruleId) {
        return "loadbalancer-pool" + ruleId;
    }

    public static String getLoadBalancerMemberName(long ruleId, String memberIp) {
        return "loadbalancer-member" + ruleId + memberIp;
    }

    public static String getLoadBalancerAlgorithm(String algorithm) {
        switch (algorithm) {
            case "leastconn":
                return "LEAST_CONNECTIONS";
            case "source":
                return "SOURCE_IP";
            default:
                return "ROUND_ROBIN";
        }
    }

    public static String getLoadBalancerSession(String stickiness) {
        switch (stickiness) {
            case "LbCookie":
                return "HTTP_COOKIE";
            case "AppCookie":
                return "APP_COOKIE";
            case "SourceBased":
                return "SOURCE_IP";
            default:
                return null;
        }
    }
}
