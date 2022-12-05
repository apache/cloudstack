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
import com.cloud.network.as.AutoScaleService;
import com.cloud.network.as.AutoScaleVmProfile;
import com.cloud.utils.db.EntityManager;
import org.apache.cloudstack.api.ResponseGenerator;
import org.apache.cloudstack.api.ServerApiException;
import org.apache.cloudstack.api.command.user.autoscale.UpdateAutoScaleVmProfileCmd;
import org.apache.cloudstack.api.response.AutoScaleVmProfileResponse;

import org.junit.Assert;
import org.junit.Before;
import org.junit.Test;
import org.mockito.Mockito;
import org.springframework.test.util.ReflectionTestUtils;

import java.util.HashMap;
import java.util.Map;

import static org.mockito.Mockito.when;

public class UpdateAutoScaleVmProfileCmdTest {
    private UpdateAutoScaleVmProfileCmd updateAutoScaleVmProfileCmd;
    private AutoScaleService autoScaleService;
    private AutoScaleVmProfile autoScaleVmProfile;
    private EntityManager entityMgr;
    private ResponseGenerator responseGenerator;
    private AutoScaleVmProfileResponse response;

    private static final Long profileId = 1L;
    private static final Long serviceOfferingId = 2L;
    private static final Long templateId = 3L;
    private static final String userData = "This is userdata";
    private static final int expungeVmGracePeriod = 30;
    private static final Long autoscaleUserId = 4L;
    final Map<String, String> otherDeployParams = new HashMap<>();
    final Map<String, Object> counterParamList = new HashMap<>();

    @Before
    public void setUp() {
        otherDeployParams.put("rootdiskdize", "10");
        otherDeployParams.put("diskofferingid", "2222-3333-4444");
        otherDeployParams.put("size", "20");

        autoScaleService = Mockito.spy(AutoScaleService.class);
        entityMgr = Mockito.spy(EntityManager.class);
        responseGenerator = Mockito.spy(ResponseGenerator.class);

        updateAutoScaleVmProfileCmd = new UpdateAutoScaleVmProfileCmd();
        ReflectionTestUtils.setField(updateAutoScaleVmProfileCmd,"_autoScaleService", autoScaleService);
        ReflectionTestUtils.setField(updateAutoScaleVmProfileCmd,"_entityMgr", entityMgr);
        ReflectionTestUtils.setField(updateAutoScaleVmProfileCmd,"_responseGenerator", responseGenerator);

        ReflectionTestUtils.setField(updateAutoScaleVmProfileCmd,"id", profileId);
        ReflectionTestUtils.setField(updateAutoScaleVmProfileCmd,"serviceOfferingId", serviceOfferingId);
        ReflectionTestUtils.setField(updateAutoScaleVmProfileCmd,"templateId", templateId);
        ReflectionTestUtils.setField(updateAutoScaleVmProfileCmd,"userData", userData);
        ReflectionTestUtils.setField(updateAutoScaleVmProfileCmd,"expungeVmGracePeriod", expungeVmGracePeriod);
        ReflectionTestUtils.setField(updateAutoScaleVmProfileCmd,"autoscaleUserId", autoscaleUserId);
        ReflectionTestUtils.setField(updateAutoScaleVmProfileCmd,"otherDeployParams", otherDeployParams);
        ReflectionTestUtils.setField(updateAutoScaleVmProfileCmd,"counterParamList", counterParamList);

        autoScaleVmProfile = Mockito.mock(AutoScaleVmProfile.class);

        response = new AutoScaleVmProfileResponse();
        response.setUserData(userData);
        response.setExpungeVmGracePeriod(expungeVmGracePeriod);
    }

    @Test
    public void verifyUpdateAutoScaleVmProfileCmd() {
        Assert.assertEquals(profileId, updateAutoScaleVmProfileCmd.getId());
        Assert.assertEquals(serviceOfferingId, updateAutoScaleVmProfileCmd.getServiceOfferingId());
        Assert.assertEquals(templateId, updateAutoScaleVmProfileCmd.getTemplateId());
        Assert.assertNull(updateAutoScaleVmProfileCmd.getDisplay());
        Assert.assertTrue(updateAutoScaleVmProfileCmd.isDisplay());
        Assert.assertEquals(userData, updateAutoScaleVmProfileCmd.getUserData());
        Assert.assertEquals(autoscaleUserId, updateAutoScaleVmProfileCmd.getAutoscaleUserId());
        Assert.assertEquals(expungeVmGracePeriod, (long) updateAutoScaleVmProfileCmd.getExpungeVmGracePeriod());
        Assert.assertEquals(counterParamList, updateAutoScaleVmProfileCmd.getCounterParamList());
        Assert.assertEquals(otherDeployParams, updateAutoScaleVmProfileCmd.getOtherDeployParams());

        Assert.assertEquals("updateautoscalevmprofileresponse", updateAutoScaleVmProfileCmd.getCommandName());
        Assert.assertEquals(EventTypes.EVENT_AUTOSCALEVMPROFILE_UPDATE, updateAutoScaleVmProfileCmd.getEventType());
        Assert.assertEquals("Updating AutoScale Vm Profile. Vm Profile Id: " + profileId, updateAutoScaleVmProfileCmd.getEventDescription());
    }

    @Test
    public void testUpdateAutoScaleVmProfileSuccess() {
        when(autoScaleVmProfile.getId()).thenReturn(1L);
        when(autoScaleVmProfile.getUuid()).thenReturn("1111-2222-3333-4444");
        when(autoScaleService.updateAutoScaleVmProfile(updateAutoScaleVmProfileCmd)).thenReturn(autoScaleVmProfile);

        when(entityMgr.findById(AutoScaleVmProfile.class, 1L)).thenReturn(autoScaleVmProfile);
        when(responseGenerator.createAutoScaleVmProfileResponse(autoScaleVmProfile)).thenReturn(response);

        updateAutoScaleVmProfileCmd.execute();

        AutoScaleVmProfileResponse autoScaleVmProfileResponse = (AutoScaleVmProfileResponse) updateAutoScaleVmProfileCmd.getResponseObject();
        Assert.assertEquals(updateAutoScaleVmProfileCmd.getCommandName(), autoScaleVmProfileResponse.getResponseName());
        Assert.assertEquals(userData, ReflectionTestUtils.getField(autoScaleVmProfileResponse, "userData"));
        Assert.assertEquals(expungeVmGracePeriod, (int) ReflectionTestUtils.getField(autoScaleVmProfileResponse, "expungeVmGracePeriod"));
    }

    @Test(expected = ServerApiException.class)
    public void testUpdateAutoScaleVmProfileFail() {
        when(autoScaleService.updateAutoScaleVmProfile(updateAutoScaleVmProfileCmd)).thenReturn(null);

        updateAutoScaleVmProfileCmd.execute();
    }
}
