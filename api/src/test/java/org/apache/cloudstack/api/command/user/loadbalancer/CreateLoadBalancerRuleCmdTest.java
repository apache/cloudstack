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
package org.apache.cloudstack.api.command.user.loadbalancer;

import org.apache.cloudstack.api.ApiErrorCode;
import org.apache.cloudstack.api.ServerApiException;
import org.junit.Assert;
import org.junit.Test;
import org.mockito.Mockito;
import org.springframework.test.util.ReflectionTestUtils;

import com.cloud.exception.InvalidParameterValueException;
import com.cloud.network.lb.LoadBalancingRulesService;

public class CreateLoadBalancerRuleCmdTest {

    @Test
    public void testCreateRefusesBadTimeoutBeforeTheRuleExists() throws Exception {
        LoadBalancingRulesService lbService = Mockito.mock(LoadBalancingRulesService.class);
        Mockito.doThrow(new InvalidParameterValueException("keepalivetimeout must be 0 or greater"))
                .when(lbService).validateLoadBalancerConnectionSettings(null, -1L);

        CreateLoadBalancerRuleCmd cmd = new CreateLoadBalancerRuleCmd();
        cmd._lbService = lbService;
        ReflectionTestUtils.setField(cmd, "keepAliveTimeout", -1L);

        try {
            cmd.create();
            Assert.fail("A negative keepalivetimeout should be refused");
        } catch (ServerApiException e) {
            Assert.assertEquals(ApiErrorCode.PARAM_ERROR, e.getErrorCode());
        }
        Mockito.verify(lbService).validateLoadBalancerConnectionSettings(null, -1L);
        Mockito.verifyNoMoreInteractions(lbService);
    }
}
