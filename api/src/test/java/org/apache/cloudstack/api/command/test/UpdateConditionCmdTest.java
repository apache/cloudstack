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

package org.apache.cloudstack.api.command.test;

import com.cloud.event.EventTypes;
import com.cloud.exception.ResourceInUseException;
import com.cloud.network.as.AutoScaleService;
import com.cloud.network.as.Condition;
import com.cloud.user.Account;
import com.cloud.utils.db.EntityManager;

import org.apache.cloudstack.api.ApiCommandResourceType;
import org.apache.cloudstack.api.ApiErrorCode;
import org.apache.cloudstack.api.ResponseGenerator;
import org.apache.cloudstack.api.ServerApiException;
import org.apache.cloudstack.api.command.user.autoscale.UpdateConditionCmd;
import org.apache.cloudstack.api.response.ConditionResponse;

import org.junit.Assert;
import org.junit.Before;
import org.junit.Test;
import org.mockito.Mockito;
import org.springframework.test.util.ReflectionTestUtils;

import static org.mockito.Mockito.when;

public class UpdateConditionCmdTest {

    private UpdateConditionCmd updateConditionCmd;
    private Condition condition;

    private AutoScaleService autoScaleService;
    private EntityManager entityMgr;
    private ResponseGenerator responseGenerator;

    private static final Long conditionId = 10L;
    private static final String relationalOperator = "GT";
    private static final Long threshold = 100L;

    private static final long accountId = 5L;

    @Before
    public void setUp() {

        autoScaleService = Mockito.spy(AutoScaleService.class);
        entityMgr = Mockito.spy(EntityManager.class);
        responseGenerator = Mockito.spy(ResponseGenerator.class);

        updateConditionCmd = new UpdateConditionCmd();

        ReflectionTestUtils.setField(updateConditionCmd,"_autoScaleService", autoScaleService);
        ReflectionTestUtils.setField(updateConditionCmd,"_entityMgr", entityMgr);
        ReflectionTestUtils.setField(updateConditionCmd,"_responseGenerator", responseGenerator);

        ReflectionTestUtils.setField(updateConditionCmd,"id", conditionId);
        ReflectionTestUtils.setField(updateConditionCmd,"relationalOperator", relationalOperator);
        ReflectionTestUtils.setField(updateConditionCmd,"threshold", threshold);

        condition = Mockito.mock(Condition.class);
    }

    @Test
    public void verifyUpdateConditionCmd() {
        Assert.assertEquals(conditionId, updateConditionCmd.getId());
        Assert.assertEquals(relationalOperator, updateConditionCmd.getRelationalOperator());
        Assert.assertEquals(threshold, updateConditionCmd.getThreshold());

        Assert.assertEquals(ApiCommandResourceType.Condition, updateConditionCmd.getApiResourceType());
        Assert.assertEquals("updateconditionresponse", updateConditionCmd.getCommandName());
        Assert.assertEquals(EventTypes.EVENT_CONDITION_UPDATE, updateConditionCmd.getEventType());
        Assert.assertEquals("Updating a condition.", updateConditionCmd.getEventDescription());

        when(entityMgr.findById(Condition.class, conditionId)).thenReturn(condition);
        when(condition.getAccountId()).thenReturn(accountId);
        Assert.assertEquals(accountId, updateConditionCmd.getEntityOwnerId());

        when(entityMgr.findById(Condition.class, conditionId)).thenReturn(null);
        Assert.assertEquals(Account.ACCOUNT_ID_SYSTEM, updateConditionCmd.getEntityOwnerId());
    }

    @Test
    public void testCreateSuccess() {
        try {
            Mockito.when(autoScaleService.updateCondition(Mockito.any(UpdateConditionCmd.class))).thenReturn(condition);
        } catch (ResourceInUseException ex) {
            Assert.fail("Got exception: " + ex.getMessage());
        }
        ConditionResponse conditionResponse = Mockito.mock(ConditionResponse.class);
        Mockito.when(responseGenerator.createConditionResponse(condition)).thenReturn(conditionResponse);

        try {
            updateConditionCmd.execute();
        } catch (Exception ex) {
            Assert.fail("Got exception: " + ex.getMessage());
        }

        Mockito.verify(responseGenerator).createConditionResponse(condition);
    }

    @Test
    public void testCreateFailure() {

        ResourceInUseException exception = new ResourceInUseException("Resource in use");
        try {
            Mockito.when(autoScaleService.updateCondition(Mockito.any(UpdateConditionCmd.class))).thenThrow(exception);
        } catch (ResourceInUseException ex) {
            Assert.fail("Got exception: " + ex.getMessage());
        }

        try {
            updateConditionCmd.execute();
        } catch (ServerApiException ex) {
            Assert.assertEquals(ApiErrorCode.RESOURCE_IN_USE_ERROR, ex.getErrorCode());
            Assert.assertEquals("Resource in use", ex.getDescription());
        }
    }
}
