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
package com.cloud.agent.resource.virtualnetwork;

import static org.junit.Assert.assertEquals;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

import java.util.HashMap;

import javax.naming.ConfigurationException;

import org.joda.time.Duration;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.ArgumentCaptor;
import org.mockito.junit.MockitoJUnitRunner;

import com.cloud.agent.api.Answer;
import com.cloud.agent.api.routing.NetworkElementCommand;
import com.cloud.utils.ExecutionResult;

import org.apache.cloudstack.vm.bootgroup.readiness.InstanceReadinessCheckAnswer;
import org.apache.cloudstack.vm.bootgroup.readiness.InstanceReadinessCheckCommand;

@RunWith(MockitoJUnitRunner.class)
public class InstanceReadinessCheckExecutionTest {

    private static final String ROUTER_IP = "169.254.3.4";

    VirtualRoutingResource resource;
    VirtualRouterDeployer deployer;

    @Before
    public void setUp() throws ConfigurationException {
        deployer = mock(VirtualRouterDeployer.class);
        when(deployer.prepareCommand(any(NetworkElementCommand.class))).thenAnswer(invocation -> {
            NetworkElementCommand cmd = invocation.getArgument(0);
            cmd.setRouterAccessIp(ROUTER_IP);
            return new ExecutionResult(true, null);
        });
        when(deployer.cleanupCommand(any(NetworkElementCommand.class))).thenReturn(new ExecutionResult(true, null));
        when(deployer.executeInVR(anyString(), anyString(), anyString(), any(Duration.class))).thenReturn(new ExecutionResult(true, "out&&err&&0"));

        resource = new VirtualRoutingResource(deployer);
        resource.configure("VRResource", new HashMap<>());
    }

    @Test
    public void zeroWaitIsFlooredToOneSecondTimeout() {
        InstanceReadinessCheckCommand cmd = new InstanceReadinessCheckCommand("10.1.1.5", false);
        cmd.setWait(0);

        resource.executeRequest(cmd);

        ArgumentCaptor<Duration> captor = ArgumentCaptor.forClass(Duration.class);
        org.mockito.Mockito.verify(deployer).executeInVR(eq(ROUTER_IP), anyString(), anyString(), captor.capture());
        assertEquals(1L, captor.getValue().getStandardSeconds());
    }

    @Test
    public void positiveWaitIsUsedAsTimeoutSeconds() {
        InstanceReadinessCheckCommand cmd = new InstanceReadinessCheckCommand("10.1.1.5", false);
        cmd.setWait(5);

        resource.executeRequest(cmd);

        ArgumentCaptor<Duration> captor = ArgumentCaptor.forClass(Duration.class);
        org.mockito.Mockito.verify(deployer).executeInVR(eq(ROUTER_IP), anyString(), anyString(), captor.capture());
        assertEquals(5L, captor.getValue().getStandardSeconds());
    }

    @Test
    public void pingCommandOmitsPortFromArgs() {
        InstanceReadinessCheckCommand cmd = new InstanceReadinessCheckCommand("10.1.1.5", false);

        resource.executeRequest(cmd);

        ArgumentCaptor<String> argsCaptor = ArgumentCaptor.forClass(String.class);
        org.mockito.Mockito.verify(deployer).executeInVR(eq(ROUTER_IP), anyString(), argsCaptor.capture(), any(Duration.class));
        assertEquals("ping 10.1.1.5", argsCaptor.getValue());
    }

    @Test
    public void portCheckCommandIncludesPortInArgs() {
        InstanceReadinessCheckCommand cmd = new InstanceReadinessCheckCommand("10.1.1.5", 8080, false);

        resource.executeRequest(cmd);

        ArgumentCaptor<String> argsCaptor = ArgumentCaptor.forClass(String.class);
        org.mockito.Mockito.verify(deployer).executeInVR(eq(ROUTER_IP), anyString(), argsCaptor.capture(), any(Duration.class));
        assertEquals("portcheck 10.1.1.5 8080", argsCaptor.getValue());
    }

    @Test
    public void resultIsWrappedInInstanceReadinessCheckAnswer() {
        InstanceReadinessCheckCommand cmd = new InstanceReadinessCheckCommand("10.1.1.5", false);

        Answer answer = resource.executeRequest(cmd);

        org.junit.Assert.assertTrue(answer instanceof InstanceReadinessCheckAnswer);
        assertEquals("0", ((InstanceReadinessCheckAnswer) answer).getExecutionDetails().get(InstanceReadinessCheckAnswer.EXITCODE));
    }
}
