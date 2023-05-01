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
import java.util.Date;
import java.util.List;

import com.cloud.network.as.AutoScalePolicy;
import com.cloud.network.as.AutoScaleVmGroup;
import com.cloud.network.as.Condition;
import com.cloud.network.as.Counter;
import com.cloud.network.lb.LoadBalancingRule.LbDestination;
import com.cloud.network.lb.LoadBalancingRule.LbHealthCheckPolicy;
import com.cloud.network.lb.LoadBalancingRule.LbSslCert;
import com.cloud.network.lb.LoadBalancingRule.LbStickinessPolicy;
import com.cloud.utils.Pair;

import org.apache.cloudstack.network.lb.LoadBalancerConfig;
import org.springframework.util.CollectionUtils;

public class LoadBalancerTO {
    String uuid;
    String srcIp;
    int srcPort;
    String protocol;
    String lbProtocol;
    String algorithm;
    boolean revoked;
    boolean alreadyAdded;
    boolean inline;
    String srcIpVlan;
    String srcIpGateway;
    String srcIpNetmask;
    Long networkId;
    DestinationTO[] destinations;
    private LoadBalancerConfigTO[] lbConfigs;
    private StickinessPolicyTO[] stickinessPolicies;
    private HealthCheckPolicyTO[] healthCheckPolicies;
    private LbSslCert sslCert; /* XXX: Should this be SslCertTO?  */
    private AutoScaleVmGroupTO autoScaleVmGroupTO;
    final static int MAX_STICKINESS_POLICIES = 1;
    final static int MAX_HEALTHCHECK_POLICIES = 1;

    private String cidrList;

    public LoadBalancerTO(String uuid, String srcIp, int srcPort, String protocol, String algorithm, boolean revoked, boolean alreadyAdded, boolean inline,
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

    public LoadBalancerTO(String id, String srcIp, int srcPort, String protocol, String algorithm, boolean revoked, boolean alreadyAdded, boolean inline,
            List<LbDestination> argDestinations, List<LbStickinessPolicy> stickinessPolicies) {

        this(id, srcIp, srcPort, protocol, algorithm, revoked, alreadyAdded, inline, argDestinations, stickinessPolicies, null, null, null);
    }

    public LoadBalancerTO(String id, List<DestinationTO> destinations) {
        this.uuid = id;
        int i = 0;
        this.destinations = new DestinationTO[destinations.size()];
        for (DestinationTO destination : destinations) {
            this.destinations[i++] = new DestinationTO(destination.getDestIp(), destination.getDestPort(), destination.getMonitorState());
        }
    }

    public LoadBalancerTO(String id, String srcIp, int srcPort, String protocol, String algorithm, boolean revoked, boolean alreadyAdded, boolean inline,
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

    public int getSrcPort() {
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

    public void setLbProtocol(String lbProtocol) {
        this.lbProtocol = lbProtocol;
    }

    public boolean isRevoked() {
        return revoked;
    }

    public boolean isAlreadyAdded() {
        return alreadyAdded;
    }

    public boolean isInline() {
        return inline;
    }

    public LoadBalancerConfigTO[] getLbConfigs() {
        return this.lbConfigs;
    }

    public void setLbConfigs(List<? extends LoadBalancerConfig> lbConfigs) {
        if (CollectionUtils.isEmpty(lbConfigs)) {
            this.lbConfigs = new LoadBalancerConfigTO[0];
            return;
        }
        this.lbConfigs = new LoadBalancerConfigTO[lbConfigs.size()];
        int i = 0;
        for (LoadBalancerConfig lbConfig : lbConfigs) {
            this.lbConfigs[i++] = new LoadBalancerConfigTO(lbConfig);
        }
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

    public boolean isAutoScaleVmGroupTO() {
        return this.autoScaleVmGroupTO != null;
    }

    public LbSslCert getSslCert() {
        return this.sslCert;
    }

    public void setLbSslCert(LbSslCert sslCert) {
        this.sslCert = sslCert;
    }

    public String getSrcIpVlan() {
        return srcIpVlan;
    }

    public void setSrcIpVlan(String srcIpVlan) {
        this.srcIpVlan = srcIpVlan;
    }

    public Long getNetworkId() {
        return networkId;
    }

    public void setNetworkId(long id) {
        this.networkId = id;
    }

    public String getSrcIpGateway() {
        return srcIpGateway;
    }

    public void setSrcIpGateway(String srcIpGateway) {
        this.srcIpGateway = srcIpGateway;
    }

    public String getSrcIpNetmask() {
        return srcIpNetmask;
    }

    public void setSrcIpNetmask(String srcIpNetmask) {
        this.srcIpNetmask = srcIpNetmask;
    }

    public void setCidrList(String cidrList){
        this.cidrList = cidrList;
    }

    public String getCidrList() {
        return cidrList;
    }

    public static class StickinessPolicyTO {
        private String methodName;
        private List<Pair<String, String>> params;

        public String getMethodName() {
            return methodName;
        }

        public List<Pair<String, String>> getParams() {
            return params;
        }

        public StickinessPolicyTO(String methodName, List<Pair<String, String>> paramsList) {
            this.methodName = methodName;
            this.params = paramsList;
        }
    }

    public static class HealthCheckPolicyTO {
        private String pingPath;
        private String description;
        private int responseTime;
        private int healthcheckInterval;
        private int healthcheckThresshold;
        private int unhealthThresshold;
        private boolean revoked = false;

        public HealthCheckPolicyTO(String pingPath, String description, int responseTime, int healthcheckInterval, int healthcheckThresshold, int unhealthThresshold,
                boolean revoke) {

            this.description = description;
            this.pingPath = pingPath;
            this.responseTime = responseTime;
            this.healthcheckInterval = healthcheckInterval;
            this.healthcheckThresshold = healthcheckThresshold;
            this.unhealthThresshold = unhealthThresshold;
            this.revoked = revoke;
        }

        public HealthCheckPolicyTO() {

        }

        public String getpingPath() {
            return pingPath;
        }

        public String getDescription() {
            return description;
        }

        public int getResponseTime() {
            return responseTime;
        }

        public int getHealthcheckInterval() {
            return healthcheckInterval;
        }

        public int getHealthcheckThresshold() {
            return healthcheckThresshold;
        }

        public int getUnhealthThresshold() {
            return unhealthThresshold;
        }

        public void setRevoke(boolean revoke) {
            this.revoked = revoke;
        }

        public boolean isRevoked() {
            return revoked;
        }

    }

    public static class DestinationTO {
        String destIp;
        int destPort;
        boolean revoked;
        boolean alreadyAdded;
        String monitorState;

        public DestinationTO(String destIp, int destPort, boolean revoked, boolean alreadyAdded) {
            this.destIp = destIp;
            this.destPort = destPort;
            this.revoked = revoked;
            this.alreadyAdded = alreadyAdded;
        }

        public DestinationTO(String destIp, int destPort, String monitorState) {
            this.destIp = destIp;
            this.destPort = destPort;
            this.monitorState = monitorState;
        }

        protected DestinationTO() {
        }

        public String getDestIp() {
            return destIp;
        }

        public int getDestPort() {
            return destPort;
        }

        public boolean isRevoked() {
            return revoked;
        }

        public boolean isAlreadyAdded() {
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
        private final Long id;
        private final String name;
        private final Counter.Source source;
        private final String value;
        private final String provider;

        public CounterTO(Long id, String name, Counter.Source source, String value, String provider) {
            this.id = id;
            this.name = name;
            this.source = source;
            this.value = value;
            this.provider = provider;
        }

        public Long getId() {
            return id;
        }

        public String getName() {
            return name;
        }

        public Counter.Source getSource() {
            return source;
        }

        public String getValue() {
            return value;
        }

        public String getProvider() {
            return provider;
        }
    }

    public static class ConditionTO implements Serializable {
        private static final long serialVersionUID = 2L;
        private final Long id;
        private final long threshold;
        private final Condition.Operator relationalOperator;
        private final CounterTO counter;

        public ConditionTO(Long id, long threshold, Condition.Operator relationalOperator, CounterTO counter) {
            this.id = id;
            this.threshold = threshold;
            this.relationalOperator = relationalOperator;
            this.counter = counter;
        }

        public Long getId() {
            return id;
        }

        public long getThreshold() {
            return threshold;
        }

        public Condition.Operator getRelationalOperator() {
            return relationalOperator;
        }

        public CounterTO getCounter() {
            return counter;
        }
    }

    public static class AutoScalePolicyTO implements Serializable {
        private static final long serialVersionUID = 2L;
        private final long id;
        private final int duration;
        private final int quietTime;
        private final Date lastQuietTime;
        private AutoScalePolicy.Action action;
        boolean revoked;
        private final List<ConditionTO> conditions;

        public AutoScalePolicyTO(long id, int duration, int quietTime, Date lastQuietTime, AutoScalePolicy.Action action, List<ConditionTO> conditions, boolean revoked) {
            this.id = id;
            this.duration = duration;
            this.quietTime = quietTime;
            this.lastQuietTime = lastQuietTime;
            this.conditions = conditions;
            this.action = action;
            this.revoked = revoked;
        }

        public long getId() {
            return id;
        }

        public int getDuration() {
            return duration;
        }

        public int getQuietTime() {
            return quietTime;
        }

        public Date getLastQuietTime() {
            return lastQuietTime;
        }

        public AutoScalePolicy.Action getAction() {
            return action;
        }

        public boolean isRevoked() {
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
        private final Integer expungeVmGracePeriod;
        private final String cloudStackApiUrl;
        private final String autoScaleUserApiKey;
        private final String autoScaleUserSecretKey;
        private final String vmName;
        private final String networkId;

        public AutoScaleVmProfileTO(String zoneId, String domainId, String cloudStackApiUrl, String autoScaleUserApiKey, String autoScaleUserSecretKey,
                String serviceOfferingId, String templateId, String vmName, String networkId, String otherDeployParams, List<Pair<String, String>> counterParamList,
                Integer expungeVmGracePeriod) {
            this.zoneId = zoneId;
            this.domainId = domainId;
            this.serviceOfferingId = serviceOfferingId;
            this.templateId = templateId;
            this.otherDeployParams = otherDeployParams;
            this.counterParamList = counterParamList;
            this.expungeVmGracePeriod = expungeVmGracePeriod;
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

        public Integer getExpungeVmGracePeriod() {
            return expungeVmGracePeriod;
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

        private final Long id;
        private final String uuid;
        private final int minMembers;
        private final int maxMembers;
        private final int memberPort;
        private final int interval;
        private final List<AutoScalePolicyTO> policies;
        private final AutoScaleVmProfileTO profile;
        private final AutoScaleVmGroup.State state;
        private final AutoScaleVmGroup.State currentState;
        private final Long loadBalancerId;

        public AutoScaleVmGroupTO(Long id, String uuid, int minMembers, int maxMembers, int memberPort, int interval, List<AutoScalePolicyTO> policies, AutoScaleVmProfileTO profile,
                           AutoScaleVmGroup.State state, AutoScaleVmGroup.State currentState, Long loadBalancerId) {
            this.id = id;
            this.uuid = uuid;
            this.minMembers = minMembers;
            this.maxMembers = maxMembers;
            this.memberPort = memberPort;
            this.interval = interval;
            this.policies = policies;
            this.profile = profile;
            this.state = state;
            this.currentState = currentState;
            this.loadBalancerId = loadBalancerId;
        }

        public Long getId() {
            return id;
        }

        public String getUuid() {
            return uuid;
        }

        public int getMinMembers() {
            return minMembers;
        }

        public int getMaxMembers() {
            return maxMembers;
        }

        public int getMemberPort() {
            return memberPort;
        }

        public int getInterval() {
            return interval;
        }

        public List<AutoScalePolicyTO> getPolicies() {
            return policies;
        }

        public AutoScaleVmProfileTO getProfile() {
            return profile;
        }

        public AutoScaleVmGroup.State getState() {
            return state;
        }

        public AutoScaleVmGroup.State getCurrentState() {
            return currentState;
        }

        public Long getLoadBalancerId() {
            return loadBalancerId;
        }
    }
}
