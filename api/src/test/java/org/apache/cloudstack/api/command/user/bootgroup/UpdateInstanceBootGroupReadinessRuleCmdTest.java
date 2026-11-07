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
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertNull;
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

public class UpdateInstanceBootGroupReadinessRuleCmdTest extends BaseBootGroupCmdTest {

    private UpdateInstanceBootGroupReadinessRuleCmd createCmd() throws Exception {
        UpdateInstanceBootGroupReadinessRuleCmd cmd = new UpdateInstanceBootGroupReadinessRuleCmd();
        setField(cmd, "instanceBootGroupService", instanceBootGroupService);
        setField(cmd, "id", ENTITY_ID);
        setField(cmd, "name", "renamed-rule");
        setField(cmd, "enabled", false);
        return cmd;
    }

    @Test
    public void testAccessors() throws Exception {
        UpdateInstanceBootGroupReadinessRuleCmd cmd = createCmd();
        assertEquals(Long.valueOf(ENTITY_ID), cmd.getId());
        assertEquals("renamed-rule", cmd.getName());
        assertEquals(Boolean.FALSE, cmd.getEnabled());
    }

    @Test
    public void testGetEnabledNullWhenNotSet() throws Exception {
        UpdateInstanceBootGroupReadinessRuleCmd cmd = createCmd();
        setField(cmd, "enabled", null);
        assertNull(cmd.getEnabled());
    }

    @Test
    public void testGetEntityOwnerId() throws Exception {
        UpdateInstanceBootGroupReadinessRuleCmd cmd = createCmd();
        assertEquals(ACCOUNT_ID, cmd.getEntityOwnerId());
    }

    @Test
    public void testGetApiResourceType() throws Exception {
        UpdateInstanceBootGroupReadinessRuleCmd cmd = createCmd();
        assertEquals(ApiCommandResourceType.InstanceBootGroupReadinessRule, cmd.getApiResourceType());
        assertEquals(Long.valueOf(ENTITY_ID), cmd.getApiResourceId());
    }

    @Test
    public void testGetDetailsNull() throws Exception {
        UpdateInstanceBootGroupReadinessRuleCmd cmd = createCmd();
        setField(cmd, "details", null);
        assertNull(cmd.getDetails());
    }

    @Test
    public void testGetDetailsEmptyMap() throws Exception {
        UpdateInstanceBootGroupReadinessRuleCmd cmd = createCmd();
        setField(cmd, "details", new HashMap<>());
        assertNull(cmd.getDetails());
    }

    @Test
    public void testGetDetailsPopulatedMap() throws Exception {
        UpdateInstanceBootGroupReadinessRuleCmd cmd = createCmd();

        Map<String, String> innerMap = new HashMap<>();
        innerMap.put("port", "9090");
        innerMap.put("protocol", "udp");

        Map<String, Map<String, String>> outerMap = new HashMap<>();
        outerMap.put("0", innerMap);

        setField(cmd, "details", outerMap);

        Map<String, String> details = cmd.getDetails();
        assertNotNull(details);
        assertEquals("9090", details.get("port"));
        assertEquals("udp", details.get("protocol"));
    }

    @Test
    public void testExecuteSuccess() throws Exception {
        UpdateInstanceBootGroupReadinessRuleCmd cmd = createCmd();

        InstanceBootGroupReadinessRule rule = mock(InstanceBootGroupReadinessRule.class);
        InstanceBootGroupReadinessRuleResponse mockResponse = new InstanceBootGroupReadinessRuleResponse();

        when(instanceBootGroupService.updateInstanceBootGroupReadinessRule(cmd)).thenReturn(rule);
        when(instanceBootGroupService.createInstanceBootGroupReadinessRuleResponse(rule)).thenReturn(mockResponse);

        cmd.execute();

        InstanceBootGroupReadinessRuleResponse response = (InstanceBootGroupReadinessRuleResponse) cmd.getResponseObject();
        assertNotNull(response);
        assertEquals("updateinstancebootgroupreadinessruleresponse", response.getResponseName());
        verify(instanceBootGroupService).updateInstanceBootGroupReadinessRule(cmd);
        verify(instanceBootGroupService).createInstanceBootGroupReadinessRuleResponse(rule);
    }

    @Test(expected = ServerApiException.class)
    public void testExecuteReturnsNull() throws Exception {
        UpdateInstanceBootGroupReadinessRuleCmd cmd = createCmd();
        when(instanceBootGroupService.updateInstanceBootGroupReadinessRule(cmd)).thenReturn(null);
        cmd.execute();
    }

    @Test(expected = RuntimeException.class)
    public void testExecutePropagatesServiceException() throws Exception {
        UpdateInstanceBootGroupReadinessRuleCmd cmd = createCmd();
        when(instanceBootGroupService.updateInstanceBootGroupReadinessRule(cmd)).thenThrow(new RuntimeException("db error"));
        cmd.execute();
    }
}
