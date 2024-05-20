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
package org.apache.cloudstack.utils;

import com.cloud.agent.AgentManager;
import com.cloud.agent.api.Answer;
import com.cloud.exception.InvalidParameterValueException;
import com.cloud.network.dao.NsxProviderDao;
import com.cloud.network.element.NsxProviderVO;
import org.apache.cloudstack.NsxAnswer;
import org.apache.cloudstack.agent.api.NsxCommand;
import org.apache.cloudstack.service.NsxApiClient;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.springframework.stereotype.Component;

import javax.inject.Inject;

import static java.util.Objects.isNull;

@Component
public class NsxControllerUtils {
    protected Logger logger = LogManager.getLogger(getClass());

    @Inject
    private AgentManager agentMgr;
    @Inject
    private NsxProviderDao nsxProviderDao;

    public static String getNsxNatRuleId(long domainId, long accountId, long dataCenterId, long resourceId, boolean isForVpc) {
        String resourcePrefix = isForVpc ? "V" : "N";
        return String.format("D%s-A%s-Z%s-%s%s-NAT", domainId, accountId, dataCenterId, resourcePrefix, resourceId);
    }

    public static String getNsxDistributedFirewallPolicyRuleId(String segmentName, long ruleId) {
        return String.format("%s-R%s", segmentName, ruleId);
    }

    public NsxAnswer sendNsxCommand(NsxCommand cmd, long zoneId) throws IllegalArgumentException {
        NsxProviderVO nsxProviderVO = nsxProviderDao.findByZoneId(zoneId);
        if (nsxProviderVO == null) {
            logger.error("No NSX controller was found!");
            throw new InvalidParameterValueException("Failed to find an NSX controller");
        }
        Answer answer = agentMgr.easySend(nsxProviderVO.getHostId(), cmd);

        if (answer == null || !answer.getResult()) {
            logger.error("NSX API Command failed");
            throw new InvalidParameterValueException("Failed API call to NSX controller");
        }

        return (NsxAnswer) answer;
    }

    /**
     * Generates the Tier 1 Gateway name and identifier for the resource on the NSX manager
     */
    public static String getTier1GatewayName(long domainId, long accountId, long zoneId,
                                             Long networkResourceId, boolean isResourceVpc) {
        String resourcePrefix = isResourceVpc ? "V" : "N";
        return String.format("D%s-A%s-Z%s-%s%s", domainId, accountId, zoneId, resourcePrefix, networkResourceId);
    }

    public static String getNsxSegmentId(long domainId, long accountId, long zoneId, Long vpcId, long networkId) {
        String segmentName = String.format("D%s-A%s-Z%s",  domainId, accountId, zoneId);
        if (isNull(vpcId)) {
            return String.format("%s-S%s", segmentName, networkId);
        }
        return String.format("%s-V%s-S%s",segmentName, vpcId, networkId);
    }

    public static String getNsxDhcpRelayConfigId(long zoneId, long domainId, long accountId, Long vpcId, long networkId) {
        String suffix = "Relay";
        if (isNull(vpcId)) {
            return String.format("D%s-A%s-Z%s-S%s-%s", domainId, accountId, zoneId, networkId, suffix);
        }
        return String.format("D%s-A%s-Z%s-V%s-S%s-%s", domainId, accountId, zoneId, vpcId, networkId, suffix);
    }

    public static String getStaticNatRuleName(long domainId, long accountId, long zoneId, Long networkResourceId, boolean isVpcResource) {
        String suffix = "-STATICNAT";
       return getTier1GatewayName(domainId, accountId, zoneId, networkResourceId, isVpcResource) + suffix;
    }

    public static String getPortForwardRuleName(long domainId, long accountId, long zoneId, Long networkResourceId, long ruleId, boolean isVpcResource) {
        String suffix = "-PF";
        return getTier1GatewayName(domainId, accountId, zoneId, networkResourceId, isVpcResource) + suffix + ruleId;
    }

    public static String getServiceName(String ruleName, String port, String protocol, Integer icmpType, Integer icmpCode) {
        return protocol.equalsIgnoreCase("icmp") ?
                String.format("%s-SVC-%s-%s-%s", ruleName, icmpType, icmpCode, protocol) :
                String.format("%s-SVC-%s-%s", ruleName, port, protocol);
    }

    public static String getServiceEntryName(String ruleName, String port, String protocol) {
        return ruleName + "-SE-" + port + "-" + protocol;
    }

    public static String getLoadBalancerName(String tier1GatewayName) {
        return tier1GatewayName + "-LB";
    }

    public static String getLoadBalancerRuleName(String tier1GatewayName, long lbId) {
        return tier1GatewayName + "-LB" + lbId;
    }

    public static String getServerPoolName(String tier1GatewayName, long lbId) {
        return  getLoadBalancerRuleName(tier1GatewayName, lbId) + "-SP";
    }

    public static String getActiveMonitorProfileName(String lbServerPoolName, String port, String protocol) {
        if (protocol.equalsIgnoreCase("udp")) {
            protocol =  "ICMP";
        }
        return lbServerPoolName + "-" + protocol + "-" + port + "-AM";
    }

    public static String  getVirtualServerName(String tier1GatewayName, long lbId) {
        return getLoadBalancerRuleName(tier1GatewayName, lbId) + "-VS";
    }

    public static String getServerPoolMemberName(String tier1GatewayName, long vmId) {
        return tier1GatewayName + "-VM" + vmId;
    }

    public static String getLoadBalancerAlgorithm(String algorithm) {
        switch (algorithm) {
            case "leastconn":
                return NsxApiClient.LBAlgorithm.LEAST_CONNECTION.name();
            case "source":
                return NsxApiClient.LBAlgorithm.IP_HASH.name();
            default:
                return NsxApiClient.LBAlgorithm.ROUND_ROBIN.name();
        }
    }
}
