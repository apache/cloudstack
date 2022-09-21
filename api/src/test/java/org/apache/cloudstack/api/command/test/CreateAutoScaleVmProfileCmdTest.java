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
import com.cloud.exception.ResourceAllocationException;
import com.cloud.network.as.AutoScaleService;
import com.cloud.network.as.AutoScaleVmProfile;
import com.cloud.utils.db.EntityManager;
import org.apache.cloudstack.api.ApiCommandResourceType;
import org.apache.cloudstack.api.ResponseGenerator;
import org.apache.cloudstack.api.ServerApiException;
import org.apache.cloudstack.api.command.user.autoscale.CreateAutoScaleVmProfileCmd;
import org.apache.cloudstack.api.response.AutoScaleVmProfileResponse;

import org.junit.Assert;
import org.junit.Before;
import org.junit.Test;
import org.mockito.Mockito;
import org.springframework.test.util.ReflectionTestUtils;

import java.util.HashMap;
import java.util.Map;

import static org.mockito.Mockito.when;

public class CreateAutoScaleVmProfileCmdTest {
    private CreateAutoScaleVmProfileCmd createAutoScaleVmProfileCmd;
    private AutoScaleService autoScaleService;
    private AutoScaleVmProfile autoScaleVmProfile;
    private EntityManager entityMgr;
    private ResponseGenerator responseGenerator;
    private AutoScaleVmProfileResponse response;

    private static final Long domainId = 1L;
    private static final Long projectId = 2L;
    private static final String accountName = "testuser";

    private static final Long zoneId = 1L;
    private static final Long serviceOfferingId = 2L;
    private static final Long templateId = 3L;
    private static final String userData = "This is userdata";
    private static final int expungeVmGracePeriod = 30;
    private static final Long autoscaleUserId = 4L;
    final Map<String, Object> otherDeployParams = new HashMap<>();
    final Map<String, Object> counterParamList = new HashMap<>();

    @Before
    public void setUp() {
        otherDeployParams.put("0", new HashMap<>(Map.ofEntries(
                Map.entry("name", "rootdisksize"),
                Map.entry("value", "10")
        )));
        otherDeployParams.put("1", new HashMap<>(Map.ofEntries(
                Map.entry("name", "diskofferingid"),
                Map.entry("value", "2222-3333-4444")
        )));
        otherDeployParams.put("2", new HashMap<>(Map.ofEntries(
                Map.entry("name", "size"),
                Map.entry("value", "20")
        )));

        autoScaleService = Mockito.spy(AutoScaleService.class);
        entityMgr = Mockito.spy(EntityManager.class);
        responseGenerator = Mockito.spy(ResponseGenerator.class);

        createAutoScaleVmProfileCmd = new CreateAutoScaleVmProfileCmd();
        ReflectionTestUtils.setField(createAutoScaleVmProfileCmd,"_autoScaleService", autoScaleService);
        ReflectionTestUtils.setField(createAutoScaleVmProfileCmd,"_entityMgr", entityMgr);
        ReflectionTestUtils.setField(createAutoScaleVmProfileCmd,"_responseGenerator", responseGenerator);

        ReflectionTestUtils.setField(createAutoScaleVmProfileCmd,"zoneId", zoneId);
        ReflectionTestUtils.setField(createAutoScaleVmProfileCmd,"serviceOfferingId", serviceOfferingId);
        ReflectionTestUtils.setField(createAutoScaleVmProfileCmd,"templateId", templateId);
        ReflectionTestUtils.setField(createAutoScaleVmProfileCmd,"userData", userData);
        ReflectionTestUtils.setField(createAutoScaleVmProfileCmd,"expungeVmGracePeriod", expungeVmGracePeriod);
        ReflectionTestUtils.setField(createAutoScaleVmProfileCmd,"autoscaleUserId", autoscaleUserId);
        ReflectionTestUtils.setField(createAutoScaleVmProfileCmd,"otherDeployParams", otherDeployParams);
        ReflectionTestUtils.setField(createAutoScaleVmProfileCmd,"counterParamList", counterParamList);
        ReflectionTestUtils.setField(createAutoScaleVmProfileCmd,"domainId", domainId);
        ReflectionTestUtils.setField(createAutoScaleVmProfileCmd,"projectId", projectId);
        ReflectionTestUtils.setField(createAutoScaleVmProfileCmd,"accountName", accountName);

        autoScaleVmProfile = Mockito.mock(AutoScaleVmProfile.class);

        response = new AutoScaleVmProfileResponse();
        response.setUserData(userData);
        response.setExpungeVmGracePeriod(expungeVmGracePeriod);
    }

    @Test
    public void verifyCreateAutoScaleVmProfileCmd() {
        Assert.assertEquals(zoneId, createAutoScaleVmProfileCmd.getZoneId());
        Assert.assertEquals(serviceOfferingId, createAutoScaleVmProfileCmd.getServiceOfferingId());
        Assert.assertEquals(templateId, createAutoScaleVmProfileCmd.getTemplateId());
        Assert.assertNull(createAutoScaleVmProfileCmd.getDisplay());
        Assert.assertTrue(createAutoScaleVmProfileCmd.isDisplay());
        Assert.assertEquals(userData, createAutoScaleVmProfileCmd.getUserData());
        Assert.assertEquals(autoscaleUserId, createAutoScaleVmProfileCmd.getAutoscaleUserId());
        Assert.assertEquals(expungeVmGracePeriod, (long) createAutoScaleVmProfileCmd.getExpungeVmGracePeriod());
        Assert.assertEquals(counterParamList, createAutoScaleVmProfileCmd.getCounterParamList());
        Assert.assertEquals(otherDeployParams, createAutoScaleVmProfileCmd.getOtherDeployParams());
        Assert.assertEquals(domainId, createAutoScaleVmProfileCmd.getDomainId());
        Assert.assertEquals(projectId, createAutoScaleVmProfileCmd.getProjectId());
        Assert.assertEquals(accountName, createAutoScaleVmProfileCmd.getAccountName());

        Assert.assertEquals("autoscalevmprofileresponse", createAutoScaleVmProfileCmd.getCommandName());
        Assert.assertEquals("autoscalevmprofile", CreateAutoScaleVmProfileCmd.getResultObjectName());
        Assert.assertEquals(EventTypes.EVENT_AUTOSCALEVMPROFILE_CREATE, createAutoScaleVmProfileCmd.getEventType());
        Assert.assertEquals("creating AutoScale Vm Profile", createAutoScaleVmProfileCmd.getEventDescription());
        Assert.assertEquals(ApiCommandResourceType.AutoScaleVmProfile, createAutoScaleVmProfileCmd.getApiResourceType());
    }

    @Test
    public void verifyCreateAutoScaleVmProfileCmdParams() {
        HashMap<String, String> deployParamMap = createAutoScaleVmProfileCmd.getDeployParamMap();
        Assert.assertEquals(otherDeployParams.size() + 4, deployParamMap.size());
        Assert.assertEquals("deployVirtualMachine", deployParamMap.get("command"));
        Assert.assertEquals(zoneId.toString(), deployParamMap.get("zoneId"));
        Assert.assertEquals(serviceOfferingId.toString(), deployParamMap.get("serviceOfferingId"));
        Assert.assertEquals(templateId.toString(), deployParamMap.get("templateId"));
        Assert.assertEquals("10", deployParamMap.get("rootdisksize"));
        Assert.assertEquals("2222-3333-4444", deployParamMap.get("diskofferingid"));
        Assert.assertEquals("20", deployParamMap.get("size"));
    }

    @Test
    public void testCreateAutoScaleVmProfileSuccess() {
        when(autoScaleVmProfile.getId()).thenReturn(1L);
        when(autoScaleVmProfile.getUuid()).thenReturn("1111-2222-3333-4444");
        when(autoScaleService.createAutoScaleVmProfile(createAutoScaleVmProfileCmd)).thenReturn(autoScaleVmProfile);

        try {
            createAutoScaleVmProfileCmd.create();
        } catch (ResourceAllocationException ex) {
            Assert.fail("Got exception: " + ex.getMessage());
        }
        Assert.assertEquals(1L, (long) createAutoScaleVmProfileCmd.getEntityId());

        when(entityMgr.findById(AutoScaleVmProfile.class, 1L)).thenReturn(autoScaleVmProfile);
        when(responseGenerator.createAutoScaleVmProfileResponse(autoScaleVmProfile)).thenReturn(response);

        createAutoScaleVmProfileCmd.execute();

        AutoScaleVmProfileResponse autoScaleVmProfileResponse = (AutoScaleVmProfileResponse) createAutoScaleVmProfileCmd.getResponseObject();
        Assert.assertEquals(createAutoScaleVmProfileCmd.getCommandName(), autoScaleVmProfileResponse.getResponseName());
        Assert.assertEquals(userData, ReflectionTestUtils.getField(autoScaleVmProfileResponse, "userData"));
        Assert.assertEquals(expungeVmGracePeriod, (int) ReflectionTestUtils.getField(autoScaleVmProfileResponse, "expungeVmGracePeriod"));
    }

    @Test(expected = ServerApiException.class)
    public void testCreateAutoScaleVmProfileFail() {
        when(autoScaleService.createAutoScaleVmProfile(createAutoScaleVmProfileCmd)).thenReturn(null);

        try {
            createAutoScaleVmProfileCmd.create();
        } catch (ResourceAllocationException ex) {
            Assert.fail("Got exception: " + ex.getMessage());
        }
    }
}
