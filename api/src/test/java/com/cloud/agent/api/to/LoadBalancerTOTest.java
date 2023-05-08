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

import com.cloud.network.as.AutoScalePolicy;
import com.cloud.network.as.AutoScaleVmGroup;
import com.cloud.network.as.Condition;
import com.cloud.network.as.Counter;
import com.cloud.utils.Pair;
import org.junit.Assert;
import org.junit.Before;
import org.junit.Test;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Date;
import java.util.List;

public class LoadBalancerTOTest {

    LoadBalancerTO.CounterTO counter;
    LoadBalancerTO.ConditionTO condition;
    LoadBalancerTO.AutoScalePolicyTO scaleUpPolicy;
    LoadBalancerTO.AutoScalePolicyTO scaleDownPolicy;

    LoadBalancerTO.AutoScaleVmProfileTO vmProfile;
    LoadBalancerTO.AutoScaleVmGroupTO vmGroup;

    private static final Long counterId = 1L;
    private static final String counterName = "counter name";
    private static final Counter.Source counterSource = Counter.Source.CPU;
    private static final String counterValue = "counter value";
    private static final String counterProvider = "VIRTUALROUTER";

    private static final Long conditionId = 2L;
    private static final Long threshold = 100L;
    private static final Condition.Operator relationalOperator = Condition.Operator.GT;

    private static final Long scaleUpPolicyId = 11L;
    private static final int scaleUpPolicyDuration = 61;
    private static final int scaleUpPolicyQuietTime = 31;
    private static final Date scaleUpPolicyLastQuietTime = new Date();

    private static final Long scaleDownPolicyId = 12L;
    private static final int scaleDownPolicyDuration = 62;
    private static final int scaleDownPolicyQuietTime = 32;
    private static final Date scaleDownPolicyLastQuietTime = new Date();

    private static final String zoneId = "1111-1111-1112";
    private static final String domainId = "1111-1111-1113";
    private static final String serviceOfferingId = "1111-1111-1114";
    private static final String templateId = "1111-1111-1115";
    private static final String otherDeployParams = "otherDeployParams";
    private static final List<Pair<String, String>> counterParamList = new ArrayList<>();
    private static final Integer expungeVmGracePeriod = 33;
    private static final String cloudStackApiUrl = "cloudstack url";
    private static final String autoScaleUserApiKey = "cloudstack api key";
    private static final String autoScaleUserSecretKey = "cloudstack secret key";
    private static final String vmName = "vm name";
    private static final String networkId = "1111-1111-1116";

    private static final Long vmGroupId = 22L;
    private static final String vmGroupUuid = "2222-2222-1111";
    private static final int minMembers = 2;
    private static final int maxMembers = 3;
    private static final int memberPort = 8080;
    private static final int interval = 30;
    private static final AutoScaleVmGroup.State state = AutoScaleVmGroup.State.ENABLED;
    private static final AutoScaleVmGroup.State currentState = AutoScaleVmGroup.State.DISABLED;
    private static final Long loadBalancerId = 21L;

    @Before
    public void setUp() {
        counter = new LoadBalancerTO.CounterTO(counterId, counterName, counterSource, counterValue, counterProvider);
        condition = new LoadBalancerTO.ConditionTO(conditionId, threshold, relationalOperator, counter);
        scaleUpPolicy = new LoadBalancerTO.AutoScalePolicyTO(scaleUpPolicyId, scaleUpPolicyDuration, scaleUpPolicyQuietTime,
                scaleUpPolicyLastQuietTime, AutoScalePolicy.Action.SCALEUP,
                Arrays.asList(new LoadBalancerTO.ConditionTO[]{ condition }), false);
        scaleDownPolicy = new LoadBalancerTO.AutoScalePolicyTO(scaleDownPolicyId, scaleDownPolicyDuration, scaleDownPolicyQuietTime,
                scaleDownPolicyLastQuietTime, AutoScalePolicy.Action.SCALEDOWN,
                Arrays.asList(new LoadBalancerTO.ConditionTO[]{ condition }), false);
        vmProfile = new LoadBalancerTO.AutoScaleVmProfileTO(zoneId, domainId, cloudStackApiUrl, autoScaleUserApiKey,
                autoScaleUserSecretKey, serviceOfferingId, templateId, vmName, networkId, otherDeployParams,
                counterParamList, expungeVmGracePeriod);
        vmGroup = new LoadBalancerTO.AutoScaleVmGroupTO(vmGroupId, vmGroupUuid, minMembers, maxMembers, memberPort,
                interval, Arrays.asList(new LoadBalancerTO.AutoScalePolicyTO[]{ scaleUpPolicy, scaleDownPolicy }),
                vmProfile, state, currentState, loadBalancerId);
    }

    @Test
    public void testCounterTO() {
        Assert.assertEquals(counterId, counter.getId());
        Assert.assertEquals(counterName, counter.getName());
        Assert.assertEquals(counterSource, counter.getSource());
        Assert.assertEquals(counterValue, counter.getValue());
        Assert.assertEquals(counterProvider, counter.getProvider());
    }

    @Test
    public void testConditionTO() {
        Assert.assertEquals(conditionId, condition.getId());
        Assert.assertEquals((long) threshold, condition.getThreshold());
        Assert.assertEquals(relationalOperator, condition.getRelationalOperator());
        Assert.assertEquals(counter, condition.getCounter());
    }

    @Test
    public void testAutoScalePolicyTO() {
        Assert.assertEquals((long) scaleUpPolicyId, scaleUpPolicy.getId());
        Assert.assertEquals(scaleUpPolicyDuration, scaleUpPolicy.getDuration());
        Assert.assertEquals(scaleUpPolicyQuietTime, scaleUpPolicy.getQuietTime());
        Assert.assertEquals(scaleUpPolicyLastQuietTime, scaleUpPolicy.getLastQuietTime());
        Assert.assertEquals(AutoScalePolicy.Action.SCALEUP, scaleUpPolicy.getAction());
        Assert.assertFalse(scaleUpPolicy.isRevoked());
        List<LoadBalancerTO.ConditionTO> scaleUpPolicyConditions = scaleUpPolicy.getConditions();
        Assert.assertEquals(1, scaleUpPolicyConditions.size());
        Assert.assertEquals(condition, scaleUpPolicyConditions.get(0));

        Assert.assertEquals((long) scaleDownPolicyId, scaleDownPolicy.getId());
        Assert.assertEquals(scaleDownPolicyDuration, scaleDownPolicy.getDuration());
        Assert.assertEquals(scaleDownPolicyQuietTime, scaleDownPolicy.getQuietTime());
        Assert.assertEquals(scaleDownPolicyLastQuietTime, scaleDownPolicy.getLastQuietTime());
        Assert.assertEquals(AutoScalePolicy.Action.SCALEDOWN, scaleDownPolicy.getAction());
        Assert.assertFalse(scaleDownPolicy.isRevoked());
        List<LoadBalancerTO.ConditionTO> scaleDownPolicyConditions = scaleDownPolicy.getConditions();
        Assert.assertEquals(1, scaleDownPolicyConditions.size());
        Assert.assertEquals(condition, scaleDownPolicyConditions.get(0));
    }

    @Test
    public void testAutoScaleVmProfileTO() {
        Assert.assertEquals(zoneId, vmProfile.getZoneId());
        Assert.assertEquals(domainId, vmProfile.getDomainId());
        Assert.assertEquals(templateId, vmProfile.getTemplateId());
        Assert.assertEquals(serviceOfferingId, vmProfile.getServiceOfferingId());
        Assert.assertEquals(otherDeployParams, vmProfile.getOtherDeployParams());
        Assert.assertEquals(counterParamList, vmProfile.getCounterParamList());
        Assert.assertEquals(expungeVmGracePeriod, vmProfile.getExpungeVmGracePeriod());
        Assert.assertEquals(cloudStackApiUrl, vmProfile.getCloudStackApiUrl());
        Assert.assertEquals(autoScaleUserApiKey, vmProfile.getAutoScaleUserApiKey());
        Assert.assertEquals(autoScaleUserSecretKey, vmProfile.getAutoScaleUserSecretKey());
        Assert.assertEquals(vmName, vmProfile.getVmName());
        Assert.assertEquals(networkId, vmProfile.getNetworkId());
    }

    @Test
    public void testAutoScaleVmGroupTO() {
        Assert.assertEquals(vmGroupId, vmGroup.getId());
        Assert.assertEquals(vmGroupUuid, vmGroup.getUuid());
        Assert.assertEquals(minMembers, vmGroup.getMinMembers());
        Assert.assertEquals(maxMembers, vmGroup.getMaxMembers());
        Assert.assertEquals(memberPort, vmGroup.getMemberPort());
        Assert.assertEquals(interval, vmGroup.getInterval());
        Assert.assertEquals(vmProfile, vmGroup.getProfile());
        Assert.assertEquals(state, vmGroup.getState());
        Assert.assertEquals(currentState, vmGroup.getCurrentState());
        Assert.assertEquals(loadBalancerId, vmGroup.getLoadBalancerId());

        List<LoadBalancerTO.AutoScalePolicyTO> policies = vmGroup.getPolicies();
        Assert.assertEquals(2, policies.size());
    }
}
