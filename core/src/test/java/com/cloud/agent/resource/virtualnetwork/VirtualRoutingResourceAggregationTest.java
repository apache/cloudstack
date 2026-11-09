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
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertTrue;

import java.util.HashMap;
import java.util.List;

import org.joda.time.Duration;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.mockito.Mockito;
import org.mockito.junit.MockitoJUnitRunner;

import com.cloud.agent.api.Answer;
import com.cloud.agent.api.routing.AggregationControlCommand;
import com.cloud.agent.api.routing.AggregationControlCommand.Action;
import com.cloud.agent.api.routing.NetworkElementCommand;
import com.cloud.agent.api.routing.SetPortForwardingRulesCommand;
import com.cloud.agent.api.to.PortForwardingRuleTO;
import com.cloud.utils.ExecutionResult;

@RunWith(MockitoJUnitRunner.class)
public class VirtualRoutingResourceAggregationTest {

    private static final String ROUTER_IP = "169.254.3.4";
    private static final String ROUTER_GUEST_IP = "10.200.1.1";
    private static final String ROUTER_NAME = "r-4-VM";

    @Mock
    private VirtualRouterDeployer deployer;

    private VirtualRoutingResource resource;

    @Before
    public void setup() throws Exception {
        Mockito.when(deployer.prepareCommand(Mockito.any())).thenAnswer(invocation -> {
            NetworkElementCommand cmd = invocation.getArgument(0);
            cmd.setRouterAccessIp(ROUTER_IP);
            return new ExecutionResult(true, null);
        });
        Mockito.lenient().when(deployer.cleanupCommand(Mockito.any())).thenReturn(new ExecutionResult(true, null));
        Mockito.lenient().when(deployer.createFileInVR(Mockito.anyString(), Mockito.anyString(), Mockito.anyString(), Mockito.anyString()))
                .thenReturn(new ExecutionResult(true, null));
        Mockito.lenient().when(deployer.executeInVR(Mockito.anyString(), Mockito.anyString(), Mockito.anyString(), Mockito.any(Duration.class)))
                .thenReturn(new ExecutionResult(true, null));
        resource = new VirtualRoutingResource(deployer);
        resource.configure("VRResource", new HashMap<>());
    }

    private Answer execute(NetworkElementCommand cmd) {
        return resource.executeRequest(cmd);
    }

    private AggregationControlCommand control(Action action) {
        return new AggregationControlCommand(action, ROUTER_NAME, ROUTER_IP, ROUTER_GUEST_IP);
    }

    private SetPortForwardingRulesCommand portForwarding(String publicIp) {
        SetPortForwardingRulesCommand cmd = new SetPortForwardingRulesCommand(List.of(
                new PortForwardingRuleTO(1, publicIp, 22, 22, "10.10.1.10", 22, 22, "TCP", false, false)));
        cmd.setAccessDetail(NetworkElementCommand.ROUTER_NAME, ROUTER_NAME);
        return cmd;
    }

    private List<String> appliedConfigFiles() {
        ArgumentCaptor<String> content = ArgumentCaptor.forClass(String.class);
        Mockito.verify(deployer, Mockito.atLeast(0)).createFileInVR(Mockito.eq(ROUTER_IP), Mockito.anyString(), Mockito.anyString(), content.capture());
        return content.getAllValues();
    }

    @Test
    public void overlappingAggregationsKeepCommandsOfBothJobs() {
        assertTrue(execute(control(Action.Start)).getResult());
        assertTrue(execute(portForwarding("64.1.1.10")).getResult());
        assertTrue(execute(control(Action.Start)).getResult());
        assertTrue(execute(portForwarding("64.1.1.20")).getResult());

        assertTrue(execute(control(Action.Finish)).getResult());
        List<String> files = appliedConfigFiles();
        assertEquals(1, files.size());
        assertTrue(files.get(0).contains("64.1.1.10"));
        assertTrue(files.get(0).contains("64.1.1.20"));

        Answer second = execute(control(Action.Finish));
        assertTrue(second.getResult());
        assertEquals(1, appliedConfigFiles().size());
    }

    @Test
    public void finishOfJoinedAggregationReportsFailureOfTheApply() {
        Mockito.when(deployer.executeInVR(Mockito.anyString(), Mockito.eq(VRScripts.VR_CFG), Mockito.anyString(), Mockito.any(Duration.class)))
                .thenReturn(new ExecutionResult(false, "vr_cfg.sh failed"));

        execute(control(Action.Start));
        execute(portForwarding("64.1.1.10"));
        execute(control(Action.Start));
        execute(portForwarding("64.1.1.20"));

        Answer first = execute(control(Action.Finish));
        assertFalse(first.getResult());
        Answer second = execute(control(Action.Finish));
        assertFalse(second.getResult());
        assertEquals("vr_cfg.sh failed", second.getDetails());
    }

    @Test
    public void laterSuccessfulApplyClearsRecordedFailure() {
        Mockito.when(deployer.executeInVR(Mockito.anyString(), Mockito.eq(VRScripts.VR_CFG), Mockito.anyString(), Mockito.any(Duration.class)))
                .thenReturn(new ExecutionResult(false, "vr_cfg.sh failed"), new ExecutionResult(true, null));

        execute(control(Action.Start));
        assertFalse(execute(control(Action.Finish)).getResult());
        execute(control(Action.Start));
        assertTrue(execute(control(Action.Finish)).getResult());

        assertTrue(execute(control(Action.Finish)).getResult());
    }

    @Test
    public void finishWithoutStartDoesNotFail() {
        Answer answer = execute(control(Action.Finish));
        assertTrue(answer.getResult());
        assertTrue(appliedConfigFiles().isEmpty());
    }

    @Test
    public void commandsAfterFinishAreAppliedDirectly() {
        execute(control(Action.Start));
        execute(control(Action.Finish));

        assertTrue(execute(portForwarding("64.1.1.30")).getResult());
        Mockito.verify(deployer, Mockito.atLeastOnce()).executeInVR(Mockito.eq(ROUTER_IP), Mockito.eq(VRScripts.UPDATE_CONFIG), Mockito.anyString(), Mockito.any(Duration.class));
    }
}
