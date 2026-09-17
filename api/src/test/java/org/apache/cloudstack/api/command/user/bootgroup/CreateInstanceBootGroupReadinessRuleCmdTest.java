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
package org.apache.cloudstack.api.command.user.bootgroup;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertNull;
import static org.junit.Assert.assertTrue;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import java.util.HashMap;
import java.util.Map;

import org.apache.cloudstack.api.ApiCommandResourceType;
import org.apache.cloudstack.api.ServerApiException;
import org.apache.cloudstack.api.response.InstanceBootGroupReadinessRuleResponse;
import org.apache.cloudstack.vm.bootgroup.readiness.InstanceBootGroupReadinessRule;
import org.junit.Test;

public class CreateInstanceBootGroupReadinessRuleCmdTest extends BaseBootGroupCmdTest {

    private CreateInstanceBootGroupReadinessRuleCmd createCmd() throws Exception {
        CreateInstanceBootGroupReadinessRuleCmd cmd = new CreateInstanceBootGroupReadinessRuleCmd();
        setField(cmd, "instanceBootGroupService", instanceBootGroupService);
        setField(cmd, "bootGroupId", ENTITY_ID);
        setField(cmd, "virtualMachineId", 500L);
        setField(cmd, "instanceGroupId", null);
        setField(cmd, "ruleType", "PortCheck");
        setField(cmd, "name", "port-check-rule");
        return cmd;
    }

    @Test
    public void testAccessors() throws Exception {
        CreateInstanceBootGroupReadinessRuleCmd cmd = createCmd();
        assertEquals(Long.valueOf(ENTITY_ID), cmd.getBootGroupId());
        assertEquals(Long.valueOf(500L), cmd.getVirtualMachineId());
        assertNull(cmd.getInstanceGroupId());
        assertEquals("PortCheck", cmd.getRuleType());
        assertEquals("port-check-rule", cmd.getName());
    }

    @Test
    public void testGetEntityOwnerId() throws Exception {
        CreateInstanceBootGroupReadinessRuleCmd cmd = createCmd();
        assertEquals(ACCOUNT_ID, cmd.getEntityOwnerId());
    }

    @Test
    public void testGetApiResourceType() throws Exception {
        CreateInstanceBootGroupReadinessRuleCmd cmd = createCmd();
        assertEquals(ApiCommandResourceType.InstanceBootGroupReadinessRule, cmd.getApiResourceType());
    }

    @Test
    public void testIsEnabledDefaultsToTrueWhenNull() throws Exception {
        CreateInstanceBootGroupReadinessRuleCmd cmd = createCmd();
        setField(cmd, "enabled", null);
        assertTrue(cmd.isEnabled());
    }

    @Test
    public void testIsEnabledFalse() throws Exception {
        CreateInstanceBootGroupReadinessRuleCmd cmd = createCmd();
        setField(cmd, "enabled", false);
        assertFalse(cmd.isEnabled());
    }

    @Test
    public void testIsEnabledTrue() throws Exception {
        CreateInstanceBootGroupReadinessRuleCmd cmd = createCmd();
        setField(cmd, "enabled", true);
        assertTrue(cmd.isEnabled());
    }

    @Test
    public void testGetDetailsNull() throws Exception {
        CreateInstanceBootGroupReadinessRuleCmd cmd = createCmd();
        setField(cmd, "details", null);
        assertNull(cmd.getDetails());
    }

    @Test
    public void testGetDetailsEmptyMap() throws Exception {
        CreateInstanceBootGroupReadinessRuleCmd cmd = createCmd();
        setField(cmd, "details", new HashMap<>());
        assertNull(cmd.getDetails());
    }

    @Test
    public void testGetDetailsPopulatedMap() throws Exception {
        CreateInstanceBootGroupReadinessRuleCmd cmd = createCmd();

        Map<String, String> innerMap = new HashMap<>();
        innerMap.put("port", "8080");
        innerMap.put("protocol", "tcp");

        Map<String, Map<String, String>> outerMap = new HashMap<>();
        outerMap.put("0", innerMap);

        setField(cmd, "details", outerMap);

        Map<String, String> details = cmd.getDetails();
        assertNotNull(details);
        assertEquals("8080", details.get("port"));
        assertEquals("tcp", details.get("protocol"));
    }

    @Test
    public void testExecuteSuccess() throws Exception {
        CreateInstanceBootGroupReadinessRuleCmd cmd = createCmd();

        InstanceBootGroupReadinessRule rule = mock(InstanceBootGroupReadinessRule.class);
        InstanceBootGroupReadinessRuleResponse mockResponse = new InstanceBootGroupReadinessRuleResponse();

        when(instanceBootGroupService.createInstanceBootGroupReadinessRule(cmd)).thenReturn(rule);
        when(instanceBootGroupService.createInstanceBootGroupReadinessRuleResponse(rule)).thenReturn(mockResponse);

        cmd.execute();

        InstanceBootGroupReadinessRuleResponse response = (InstanceBootGroupReadinessRuleResponse) cmd.getResponseObject();
        assertNotNull(response);
        assertEquals("createinstancebootgroupreadinessruleresponse", response.getResponseName());
        verify(instanceBootGroupService).createInstanceBootGroupReadinessRule(cmd);
        verify(instanceBootGroupService).createInstanceBootGroupReadinessRuleResponse(rule);
    }

    @Test(expected = ServerApiException.class)
    public void testExecuteReturnsNull() throws Exception {
        CreateInstanceBootGroupReadinessRuleCmd cmd = createCmd();
        when(instanceBootGroupService.createInstanceBootGroupReadinessRule(cmd)).thenReturn(null);
        cmd.execute();
    }

    @Test(expected = RuntimeException.class)
    public void testExecutePropagatesServiceException() throws Exception {
        CreateInstanceBootGroupReadinessRuleCmd cmd = createCmd();
        when(instanceBootGroupService.createInstanceBootGroupReadinessRule(cmd)).thenThrow(new RuntimeException("db error"));
        cmd.execute();
    }
}
