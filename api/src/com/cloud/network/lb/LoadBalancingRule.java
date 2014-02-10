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
package com.cloud.network.lb;

import java.util.List;

import com.cloud.network.as.AutoScalePolicy;
import com.cloud.network.as.AutoScaleVmGroup;
import com.cloud.network.as.AutoScaleVmProfile;
import com.cloud.network.as.Condition;
import com.cloud.network.as.Counter;
import com.cloud.network.rules.FirewallRule;
import com.cloud.network.rules.LoadBalancer;
import com.cloud.network.rules.LoadBalancerContainer.Scheme;
import com.cloud.utils.Pair;
import com.cloud.utils.net.Ip;

public class LoadBalancingRule {
    private LoadBalancer lb;
    private Ip sourceIp;
    private List<LbDestination> destinations;
    private List<LbStickinessPolicy> stickinessPolicies;
    private LbAutoScaleVmGroup autoScaleVmGroup;
    private List<LbHealthCheckPolicy> healthCheckPolicies;
    private LbSslCert sslCert;
    private String lbProtocol;

    public LoadBalancingRule(LoadBalancer lb, List<LbDestination> destinations, List<LbStickinessPolicy> stickinessPolicies,
            List<LbHealthCheckPolicy> healthCheckPolicies, Ip sourceIp) {
        this.lb = lb;
        this.destinations = destinations;
        this.stickinessPolicies = stickinessPolicies;
        this.healthCheckPolicies = healthCheckPolicies;
        this.sourceIp = sourceIp;
    }

    public LoadBalancingRule(LoadBalancer lb, List<LbDestination> destinations, List<LbStickinessPolicy> stickinessPolicies,
            List<LbHealthCheckPolicy> healthCheckPolicies, Ip sourceIp, LbSslCert sslCert, String lbProtocol) {
        this.lb = lb;
        this.destinations = destinations;
        this.stickinessPolicies = stickinessPolicies;
        this.healthCheckPolicies = healthCheckPolicies;
        this.sourceIp = sourceIp;
        this.sslCert = sslCert;
        this.lbProtocol = lbProtocol;
    }

    public long getId() {
        return lb.getId();
    }

    public String getName() {
        return lb.getName();
    }

    public String getDescription() {
        return lb.getDescription();
    }

    public int getDefaultPortStart() {
        return lb.getDefaultPortStart();
    }

    public int getDefaultPortEnd() {
        return lb.getDefaultPortEnd();
    }

    public String getAlgorithm() {
        return lb.getAlgorithm();
    }

    public String getUuid() {
        return lb.getUuid();
    }

    public String getXid() {
        return lb.getXid();
    }

    public Integer getSourcePortStart() {
        return lb.getSourcePortStart();
    }

    public Integer getSourcePortEnd() {
        return lb.getSourcePortEnd();
    }

    public String getProtocol() {
        return lb.getProtocol();
    }

    public String getLbProtocol() {
        return this.lbProtocol;
    }

    public FirewallRule.Purpose getPurpose() {
        return FirewallRule.Purpose.LoadBalancing;
    }

    public FirewallRule.State getState() {
        return lb.getState();
    }

    public long getNetworkId() {
        return lb.getNetworkId();
    }

    public void setDestinations(List<LbDestination> destinations) {
        this.destinations = destinations;
    }

    public List<LbDestination> getDestinations() {
        return destinations;
    }

    public List<LbStickinessPolicy> getStickinessPolicies() {
        return stickinessPolicies;
    }

    public void setHealthCheckPolicies(List<LbHealthCheckPolicy> healthCheckPolicies) {
        this.healthCheckPolicies = healthCheckPolicies;
    }

    public List<LbHealthCheckPolicy> getHealthCheckPolicies() {
        return healthCheckPolicies;
    }

    public LbSslCert getLbSslCert() {
        return sslCert;
    }

    public interface Destination {
        String getIpAddress();

        int getDestinationPortStart();

        int getDestinationPortEnd();

        boolean isRevoked();
    }

    public static class LbStickinessPolicy {
        private String _methodName;
        private List<Pair<String, String>> _params;
        private boolean _revoke;

        public LbStickinessPolicy(String methodName, List<Pair<String, String>> params, boolean revoke) {
            this._methodName = methodName;
            this._params = params;
            this._revoke = revoke;
        }

        public LbStickinessPolicy(String methodName, List<Pair<String, String>> params) {
            this._methodName = methodName;
            this._params = params;
            this._revoke = false;
        }

        public String getMethodName() {
            return _methodName;
        }

        public List<Pair<String, String>> getParams() {
            return _params;
        }

        public boolean isRevoked() {
            return _revoke;
        }
    }

    public static class LbHealthCheckPolicy {
        private String pingpath;
        private String description;
        private int responseTime;
        private int healthcheckInterval;
        private int healthcheckThresshold;
        private int unhealthThresshold;
        private boolean _revoke;

        public LbHealthCheckPolicy(String pingpath, String description, int responseTime, int healthcheckInterval, int healthcheckThresshold, int unhealthThresshold) {
            this(pingpath, description, responseTime, healthcheckInterval, healthcheckThresshold, unhealthThresshold, false);
        }

        public LbHealthCheckPolicy(String pingpath, String description, int responseTime, int healthcheckInterval, int healthcheckThresshold, int unhealthThresshold,
                boolean revoke) {
            this.pingpath = pingpath;
            this.description = description;
            this.responseTime = responseTime;
            this.healthcheckInterval = healthcheckInterval;
            this.healthcheckThresshold = healthcheckThresshold;
            this.unhealthThresshold = unhealthThresshold;
            this._revoke = revoke;
        }

        public LbHealthCheckPolicy() {
        }

        public String getpingpath() {
            return pingpath;
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

        public boolean isRevoked() {
            return _revoke;
        }

    }

    public static class LbDestination implements Destination {
        private int portStart;
        private int portEnd;
        private String ip;
        boolean revoked;

        public LbDestination(int portStart, int portEnd, String ip, boolean revoked) {
            this.portStart = portStart;
            this.portEnd = portEnd;
            this.ip = ip;
            this.revoked = revoked;
        }

        @Override
        public String getIpAddress() {
            return ip;
        }

        @Override
        public int getDestinationPortStart() {
            return portStart;
        }

        @Override
        public int getDestinationPortEnd() {
            return portEnd;
        }

        @Override
        public boolean isRevoked() {
            return revoked;
        }

        public void setRevoked(boolean revoked) {
            this.revoked = revoked;
        }
    }

    public LbAutoScaleVmGroup getAutoScaleVmGroup() {
        return autoScaleVmGroup;
    }

    public boolean isAutoScaleConfig() {
        return this.autoScaleVmGroup != null;
    }

    public void setAutoScaleVmGroup(LbAutoScaleVmGroup autoScaleVmGroup) {
        this.autoScaleVmGroup = autoScaleVmGroup;
    }

    public static class LbCondition {
        private final Condition condition;
        private final Counter counter;

        public LbCondition(Counter counter, Condition condition) {
            this.condition = condition;
            this.counter = counter;
        }

        public Condition getCondition() {
            return condition;
        }

        public Counter getCounter() {
            return counter;
        }
    }

    public static class LbAutoScalePolicy {
        private final List<LbCondition> conditions;
        private final AutoScalePolicy policy;
        private boolean revoked;

        public LbAutoScalePolicy(AutoScalePolicy policy, List<LbCondition> conditions) {
            this.policy = policy;
            this.conditions = conditions;
        }

        public List<LbCondition> getConditions() {
            return conditions;
        }

        public AutoScalePolicy getPolicy() {
            return policy;
        }

        public boolean isRevoked() {
            return revoked;
        }

        public void setRevoked(boolean revoked) {
            this.revoked = revoked;
        }
    }

    public static class LbAutoScaleVmProfile {
        AutoScaleVmProfile profile;
        private final String autoScaleUserApiKey;
        private final String autoScaleUserSecretKey;
        private final String csUrl;
        private final String zoneId;
        private final String domainId;
        private final String serviceOfferingId;
        private final String templateId;
        private final String networkId;
        private final String vmName;

        public LbAutoScaleVmProfile(AutoScaleVmProfile profile, String autoScaleUserApiKey, String autoScaleUserSecretKey, String csUrl, String zoneId, String domainId,
                String serviceOfferingId, String templateId, String vmName, String networkId) {
            this.profile = profile;
            this.autoScaleUserApiKey = autoScaleUserApiKey;
            this.autoScaleUserSecretKey = autoScaleUserSecretKey;
            this.csUrl = csUrl;
            this.zoneId = zoneId;
            this.domainId = domainId;
            this.serviceOfferingId = serviceOfferingId;
            this.templateId = templateId;
            this.vmName = vmName;
            this.networkId = networkId;
        }

        public AutoScaleVmProfile getProfile() {
            return profile;
        }

        public String getAutoScaleUserApiKey() {
            return autoScaleUserApiKey;
        }

        public String getAutoScaleUserSecretKey() {
            return autoScaleUserSecretKey;
        }

        public String getCsUrl() {
            return csUrl;
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

        public String getVmName() {
            return vmName;
        }

        public String getNetworkId() {
            return networkId;
        }
    }

    public static class LbAutoScaleVmGroup {
        AutoScaleVmGroup vmGroup;
        private final List<LbAutoScalePolicy> policies;
        private final LbAutoScaleVmProfile profile;
        private final String currentState;

        public LbAutoScaleVmGroup(AutoScaleVmGroup vmGroup, List<LbAutoScalePolicy> policies, LbAutoScaleVmProfile profile, String currentState) {
            this.vmGroup = vmGroup;
            this.policies = policies;
            this.profile = profile;
            this.currentState = currentState;
        }

        public AutoScaleVmGroup getVmGroup() {
            return vmGroup;
        }

        public List<LbAutoScalePolicy> getPolicies() {
            return policies;
        }

        public LbAutoScaleVmProfile getProfile() {
            return profile;
        }

        public String getCurrentState() {
            return currentState;
        }
    }

    public static class LbSslCert {
        private String cert;
        private String key;
        private String password = null;
        private String chain = null;
        private String fingerprint;
        private boolean revoked;

        public LbSslCert(String cert, String key, String password, String chain, String fingerprint, boolean revoked) {
            this.cert = cert;
            this.key = key;
            this.password = password;
            this.chain = chain;
            this.fingerprint = fingerprint;
            this.revoked = revoked;
        }

        public String getCert() {

            return cert;
        }

        public String getKey() {
            return key;
        }

        public String getPassword() {
            return password;
        }

        public String getChain() {
            return chain;
        }

        public String getFingerprint() {
            return fingerprint;
        }

        public boolean isRevoked() {
            return revoked;
        }
    }

    public Ip getSourceIp() {
        return sourceIp;
    }

    public Scheme getScheme() {
        return lb.getScheme();
    }
}
