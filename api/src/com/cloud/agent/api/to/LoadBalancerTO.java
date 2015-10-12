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
package com.cloud.agent.api.to;

import java.io.Serializable;
import java.util.ArrayList;
import java.util.List;

import com.cloud.network.as.AutoScalePolicy;
import com.cloud.network.as.AutoScaleVmGroup;
import com.cloud.network.as.AutoScaleVmProfile;
import com.cloud.network.as.Condition;
import com.cloud.network.as.Counter;
import com.cloud.network.lb.LoadBalancingRule.LbAutoScalePolicy;
import com.cloud.network.lb.LoadBalancingRule.LbAutoScaleVmGroup;
import com.cloud.network.lb.LoadBalancingRule.LbAutoScaleVmProfile;
import com.cloud.network.lb.LoadBalancingRule.LbCondition;
import com.cloud.network.lb.LoadBalancingRule.LbDestination;
import com.cloud.network.lb.LoadBalancingRule.LbHealthCheckPolicy;
import com.cloud.network.lb.LoadBalancingRule.LbSslCert;
import com.cloud.network.lb.LoadBalancingRule.LbStickinessPolicy;
import com.cloud.utils.Pair;

public class LoadBalancerTO {
    String uuid;
    String srcIp;
    Integer srcPort;
    String protocol;
    String lbProtocol;
    String algorithm;
    Boolean revoked;
    Boolean alreadyAdded;
    Boolean inline;
    DestinationTO[] destinations;
    private StickinessPolicyTO[] stickinessPolicies;
    private HealthCheckPolicyTO[] healthCheckPolicies;
    private LbSslCert sslCert; /* XXX: Should this be SslCertTO?  */
    private AutoScaleVmGroupTO autoScaleVmGroupTO;
    final static Integer MAX_STICKINESS_POLICIES = 1;
    final static Integer MAX_HEALTHCHECK_POLICIES = 1;

    public LoadBalancerTO(String uuid, String srcIp, Integer srcPort, String protocol, String algorithm, Boolean revoked, Boolean alreadyAdded, Boolean inline,
            List<LbDestination> destinations) {
        if (destinations == null) { // for autoscaleconfig destinations will be null;
            destinations = new ArrayList<LbDestination>();
        }
        this.uuid = uuid;
        this.srcIp = srcIp;
        this.srcPort = srcPort;
        this.protocol = protocol;
        this.algorithm = algorithm;
        this.revoked = revoked;
        this.alreadyAdded = alreadyAdded;
        this.inline = inline;
        this.destinations = new DestinationTO[destinations.size()];
        this.stickinessPolicies = null;
        this.sslCert = null;
        this.lbProtocol = null;
        int i = 0;
        for (LbDestination destination : destinations) {
            this.destinations[i++] = new DestinationTO(destination.getIpAddress(), destination.getDestinationPortStart(), destination.isRevoked(), false);
        }
    }

    public LoadBalancerTO(String id, String srcIp, Integer srcPort, String protocol, String algorithm, Boolean revoked, Boolean alreadyAdded, Boolean inline,
            List<LbDestination> argDestinations, List<LbStickinessPolicy> stickinessPolicies) {

        this(id, srcIp, srcPort, protocol, algorithm, revoked, alreadyAdded, inline, argDestinations, stickinessPolicies, null, null, null);
    }

    public LoadBalancerTO(String id, String srcIp, Integer srcPort, String protocol, String algorithm, Boolean revoked, Boolean alreadyAdded, Boolean inline,
            List<LbDestination> argDestinations, List<LbStickinessPolicy> stickinessPolicies, List<LbHealthCheckPolicy> healthCheckPolicies, LbSslCert sslCert,
            String lbProtocol) {
        this(id, srcIp, srcPort, protocol, algorithm, revoked, alreadyAdded, inline, argDestinations);
        this.stickinessPolicies = null;
        this.healthCheckPolicies = null;
        if (stickinessPolicies != null && stickinessPolicies.size() > 0) {
            this.stickinessPolicies = new StickinessPolicyTO[MAX_STICKINESS_POLICIES];
            int index = 0;
            for (LbStickinessPolicy stickinesspolicy : stickinessPolicies) {
                if (!stickinesspolicy.isRevoked()) {
                    this.stickinessPolicies[index] = new StickinessPolicyTO(stickinesspolicy.getMethodName(), stickinesspolicy.getParams());
                    index++;
                    if (index == MAX_STICKINESS_POLICIES)
                        break;
                }
            }
            if (index == 0)
                this.stickinessPolicies = null;
        }

        if (healthCheckPolicies != null && healthCheckPolicies.size() > 0) {
            this.healthCheckPolicies = new HealthCheckPolicyTO[MAX_HEALTHCHECK_POLICIES];
            int index = 0;
            for (LbHealthCheckPolicy hcp : healthCheckPolicies) {
                this.healthCheckPolicies[0] =
                    new HealthCheckPolicyTO(hcp.getpingpath(), hcp.getDescription(), hcp.getResponseTime(), hcp.getHealthcheckInterval(), hcp.getHealthcheckThresshold(),
                        hcp.getUnhealthThresshold(), hcp.isRevoked());
                index++;
                if (index == MAX_HEALTHCHECK_POLICIES)
                    break;
            }

            if (index == 0)
                this.healthCheckPolicies = null;
        }

        this.sslCert = sslCert;
        this.lbProtocol = lbProtocol;
    }

    protected LoadBalancerTO() {
    }

    public String getUuid() {
        return uuid;
    }

    public String getSrcIp() {
        return srcIp;
    }

    public Integer getSrcPort() {
        return srcPort;
    }

    public String getAlgorithm() {
        return algorithm;
    }

    public String getProtocol() {
        return protocol;
    }

    public String getLbProtocol() {
        return lbProtocol;
    }

    public Boolean isRevoked() {
        return revoked;
    }

    public Boolean isAlreadyAdded() {
        return alreadyAdded;
    }

    public Boolean isInline() {
        return inline;
    }

    public StickinessPolicyTO[] getStickinessPolicies() {
        return stickinessPolicies;
    }

    public HealthCheckPolicyTO[] getHealthCheckPolicies() {
        return healthCheckPolicies;
    }

    public DestinationTO[] getDestinations() {
        return destinations;
    }

    public AutoScaleVmGroupTO getAutoScaleVmGroupTO() {
        return autoScaleVmGroupTO;
    }

    public void setAutoScaleVmGroupTO(AutoScaleVmGroupTO autoScaleVmGroupTO) {
        this.autoScaleVmGroupTO = autoScaleVmGroupTO;
    }

    public Boolean isAutoScaleVmGroupTO() {
        return this.autoScaleVmGroupTO != null;
    }

    public LbSslCert getSslCert() {
        return this.sslCert;
    }

    public static class StickinessPolicyTO {
        private String _methodName;
        private List<Pair<String, String>> _paramsList;

        public String getMethodName() {
            return _methodName;
        }

        public List<Pair<String, String>> getParams() {
            return _paramsList;
        }

        public StickinessPolicyTO(String methodName, List<Pair<String, String>> paramsList) {
            this._methodName = methodName;
            this._paramsList = paramsList;
        }
    }

    public static class HealthCheckPolicyTO {
        private String pingPath;
        private String description;
        private Integer responseTime;
        private Integer healthcheckInterval;
        private Integer healthcheckThresshold;
        private Integer unhealthThresshold;
        private Boolean revoke = false;

        public HealthCheckPolicyTO(String pingPath, String description, Integer responseTime, Integer healthcheckInterval, Integer healthcheckThresshold, Integer unhealthThresshold,
                Boolean revoke) {

            this.description = description;
            this.pingPath = pingPath;
            this.responseTime = responseTime;
            this.healthcheckInterval = healthcheckInterval;
            this.healthcheckThresshold = healthcheckThresshold;
            this.unhealthThresshold = unhealthThresshold;
            this.revoke = revoke;
        }

        public HealthCheckPolicyTO() {

        }

        public String getpingPath() {
            return pingPath;
        }

        public String getDescription() {
            return description;
        }

        public Integer getResponseTime() {
            return responseTime;
        }

        public Integer getHealthcheckInterval() {
            return healthcheckInterval;
        }

        public Integer getHealthcheckThresshold() {
            return healthcheckThresshold;
        }

        public Integer getUnhealthThresshold() {
            return unhealthThresshold;
        }

        public void setRevoke(Boolean revoke) {
            this.revoke = revoke;
        }

        public Boolean isRevoked() {
            return revoke;
        }

    }

    public static class DestinationTO {
        String destIp;
        Integer destPort;
        Boolean revoked;
        Boolean alreadyAdded;
        String monitorState;

        public DestinationTO(String destIp, Integer destPort, Boolean revoked, Boolean alreadyAdded) {
            this.destIp = destIp;
            this.destPort = destPort;
            this.revoked = revoked;
            this.alreadyAdded = alreadyAdded;
        }

        protected DestinationTO() {
        }

        public String getDestIp() {
            return destIp;
        }

        public Integer getDestPort() {
            return destPort;
        }

        public Boolean isRevoked() {
            return revoked;
        }

        public Boolean isAlreadyAdded() {
            return alreadyAdded;
        }

        public void setMonitorState(String state) {
            this.monitorState = state;
        }

        public String getMonitorState() {
            return monitorState;
        }

    }

    public static class CounterTO implements Serializable {
        private static final long serialVersionUID = 2L;
        private final String name;
        private final String source;
        private final String value;

        public CounterTO(String name, String source, String value) {
            this.name = name;
            this.source = source;
            this.value = value;
        }

        public String getName() {
            return name;
        }

        public String getSource() {
            return source;
        }

        public String getValue() {
            return value;
        }
    }

    public static class ConditionTO implements Serializable {
        private static final long serialVersionUID = 2L;
        private final long threshold;
        private final String relationalOperator;
        private final CounterTO counter;

        public ConditionTO(long threshold, String relationalOperator, CounterTO counter) {
            this.threshold = threshold;
            this.relationalOperator = relationalOperator;
            this.counter = counter;
        }

        public long getThreshold() {
            return threshold;
        }

        public String getRelationalOperator() {
            return relationalOperator;
        }

        public CounterTO getCounter() {
            return counter;
        }
    }

    public static class AutoScalePolicyTO implements Serializable {
        private static final long serialVersionUID = 2L;
        private final long id;
        private final Integer duration;
        private final Integer quietTime;
        private String action;
        Boolean revoked;
        private final List<ConditionTO> conditions;

        public AutoScalePolicyTO(long id, Integer duration, Integer quietTime, String action, List<ConditionTO> conditions, Boolean revoked) {
            this.id = id;
            this.duration = duration;
            this.quietTime = quietTime;
            this.conditions = conditions;
            this.action = action;
            this.revoked = revoked;
        }

        public long getId() {
            return id;
        }

        public Integer getDuration() {
            return duration;
        }

        public Integer getQuietTime() {
            return quietTime;
        }

        public String getAction() {
            return action;
        }

        public Boolean isRevoked() {
            return revoked;
        }

        public List<ConditionTO> getConditions() {
            return conditions;
        }
    }

    public static class AutoScaleVmProfileTO implements Serializable {
        private static final long serialVersionUID = 2L;
        private final String zoneId;
        private final String domainId;
        private final String serviceOfferingId;
        private final String templateId;
        private final String otherDeployParams;
        private final List<Pair<String, String>> counterParamList;
        private final Integer destroyVmGraceperiod;
        private final String cloudStackApiUrl;
        private final String autoScaleUserApiKey;
        private final String autoScaleUserSecretKey;
        private final String vmName;
        private final String networkId;

        public AutoScaleVmProfileTO(String zoneId, String domainId, String cloudStackApiUrl, String autoScaleUserApiKey, String autoScaleUserSecretKey,
                String serviceOfferingId, String templateId, String vmName, String networkId, String otherDeployParams, List<Pair<String, String>> counterParamList,
                Integer destroyVmGraceperiod) {
            this.zoneId = zoneId;
            this.domainId = domainId;
            this.serviceOfferingId = serviceOfferingId;
            this.templateId = templateId;
            this.otherDeployParams = otherDeployParams;
            this.counterParamList = counterParamList;
            this.destroyVmGraceperiod = destroyVmGraceperiod;
            this.cloudStackApiUrl = cloudStackApiUrl;
            this.autoScaleUserApiKey = autoScaleUserApiKey;
            this.autoScaleUserSecretKey = autoScaleUserSecretKey;
            this.vmName = vmName;
            this.networkId = networkId;
        }

        public String getZoneId() {
            return zoneId;
        }

        public String getDomainId() {
            return domainId;
        }

        public String getServiceOfferingId() {
            return serviceOfferingId;
        }

        public String getTemplateId() {
            return templateId;
        }

        public String getOtherDeployParams() {
            return otherDeployParams;
        }

        public List<Pair<String, String>> getCounterParamList() {
            return counterParamList;
        }

        public Integer getDestroyVmGraceperiod() {
            return destroyVmGraceperiod;
        }

        public String getCloudStackApiUrl() {
            return cloudStackApiUrl;
        }

        public String getAutoScaleUserApiKey() {
            return autoScaleUserApiKey;
        }

        public String getAutoScaleUserSecretKey() {
            return autoScaleUserSecretKey;
        }

        public String getVmName() {
            return vmName;
        }

        public String getNetworkId() {
            return networkId;
        }
    }

    public static class AutoScaleVmGroupTO implements Serializable {
        private static final long serialVersionUID = 2L;
        private final String uuid;
        private final Integer minMembers;
        private final Integer maxMembers;
        private final Integer memberPort;
        private final Integer interval;
        private final List<AutoScalePolicyTO> policies;
        private final AutoScaleVmProfileTO profile;
        private final String state;
        private final String currentState;

        AutoScaleVmGroupTO(String uuid, Integer minMembers, Integer maxMembers, Integer memberPort, Integer interval, List<AutoScalePolicyTO> policies, AutoScaleVmProfileTO profile,
                String state, String currentState) {
            this.uuid = uuid;
            this.minMembers = minMembers;
            this.maxMembers = maxMembers;
            this.memberPort = memberPort;
            this.interval = interval;
            this.policies = policies;
            this.profile = profile;
            this.state = state;
            this.currentState = currentState;
        }

        public String getUuid() {
            return uuid;
        }

        public Integer getMinMembers() {
            return minMembers;
        }

        public Integer getMaxMembers() {
            return maxMembers;
        }

        public Integer getMemberPort() {
            return memberPort;
        }

        public Integer getInterval() {
            return interval;
        }

        public List<AutoScalePolicyTO> getPolicies() {
            return policies;
        }

        public AutoScaleVmProfileTO getProfile() {
            return profile;
        }

        public String getState() {
            return state;
        }

        public String getCurrentState() {
            return currentState;
        }
    }

    public void setAutoScaleVmGroup(LbAutoScaleVmGroup lbAutoScaleVmGroup) {
        List<LbAutoScalePolicy> lbAutoScalePolicies = lbAutoScaleVmGroup.getPolicies();
        List<AutoScalePolicyTO> autoScalePolicyTOs = new ArrayList<AutoScalePolicyTO>(lbAutoScalePolicies.size());
        for (LbAutoScalePolicy lbAutoScalePolicy : lbAutoScalePolicies) {
            List<LbCondition> lbConditions = lbAutoScalePolicy.getConditions();
            List<ConditionTO> conditionTOs = new ArrayList<ConditionTO>(lbConditions.size());
            for (LbCondition lbCondition : lbConditions) {
                Counter counter = lbCondition.getCounter();
                CounterTO counterTO = new CounterTO(counter.getName(), counter.getSource().toString(), "" + counter.getValue());
                Condition condition = lbCondition.getCondition();
                ConditionTO conditionTO = new ConditionTO(condition.getThreshold(), condition.getRelationalOperator().toString(), counterTO);
                conditionTOs.add(conditionTO);
            }
            AutoScalePolicy autoScalePolicy = lbAutoScalePolicy.getPolicy();
            autoScalePolicyTOs.add(new AutoScalePolicyTO(autoScalePolicy.getId(), autoScalePolicy.getDuration(), autoScalePolicy.getQuietTime(),
                autoScalePolicy.getAction(), conditionTOs, lbAutoScalePolicy.isRevoked()));
        }
        LbAutoScaleVmProfile lbAutoScaleVmProfile = lbAutoScaleVmGroup.getProfile();
        AutoScaleVmProfile autoScaleVmProfile = lbAutoScaleVmProfile.getProfile();

        AutoScaleVmProfileTO autoScaleVmProfileTO =
            new AutoScaleVmProfileTO(lbAutoScaleVmProfile.getZoneId(), lbAutoScaleVmProfile.getDomainId(), lbAutoScaleVmProfile.getCsUrl(),
                lbAutoScaleVmProfile.getAutoScaleUserApiKey(), lbAutoScaleVmProfile.getAutoScaleUserSecretKey(), lbAutoScaleVmProfile.getServiceOfferingId(),
                lbAutoScaleVmProfile.getTemplateId(), lbAutoScaleVmProfile.getVmName(), lbAutoScaleVmProfile.getNetworkId(), autoScaleVmProfile.getOtherDeployParams(),
                autoScaleVmProfile.getCounterParams(), autoScaleVmProfile.getDestroyVmGraceperiod());

        AutoScaleVmGroup autoScaleVmGroup = lbAutoScaleVmGroup.getVmGroup();
        autoScaleVmGroupTO =
            new AutoScaleVmGroupTO(autoScaleVmGroup.getUuid(), autoScaleVmGroup.getMinMembers(), autoScaleVmGroup.getMaxMembers(), autoScaleVmGroup.getMemberPort(),
                autoScaleVmGroup.getInterval(), autoScalePolicyTOs, autoScaleVmProfileTO, autoScaleVmGroup.getState(), lbAutoScaleVmGroup.getCurrentState());
    }
}
