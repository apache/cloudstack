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



package org.apache.cloudstack.api.command.user.template;

import com.cloud.exception.ResourceAllocationException;
import com.cloud.template.TemplateApiService;
import org.apache.cloudstack.api.ApiErrorCode;
import org.apache.cloudstack.api.ServerApiException;
import org.apache.cloudstack.api.command.admin.template.RegisterTemplateCmdByAdmin;
import org.junit.Assert;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.MockitoJUnitRunner;
import java.util.ArrayList;

@RunWith(MockitoJUnitRunner.class)
public class RegisterTemplateCmdByAdminTest{

    @InjectMocks
    private RegisterTemplateCmdByAdmin registerTemplateCmdByAdmin;

    @Mock
    public TemplateApiService _templateService;

    @Test
    public void testZoneidAndZoneIdListEmpty() throws ResourceAllocationException {
        try {
            registerTemplateCmdByAdmin = new RegisterTemplateCmdByAdmin();
            registerTemplateCmdByAdmin.execute();
        } catch (ServerApiException e) {
            if(e.getErrorCode() != ApiErrorCode.PARAM_ERROR) {
                Assert.fail("Api should fail when both zoneid and zoneids aren't passed");
            }
        }
    }

    @Test
    public void testZoneidAndZoneIdListBothPresent() throws ResourceAllocationException {
        try {
            registerTemplateCmdByAdmin = new RegisterTemplateCmdByAdmin();
            registerTemplateCmdByAdmin.zoneId = -1L;
            registerTemplateCmdByAdmin.zoneIds = new ArrayList<>();
            registerTemplateCmdByAdmin.zoneIds.add(-1L);

            registerTemplateCmdByAdmin.execute();
        } catch (ServerApiException e) {
            if(e.getErrorCode() != ApiErrorCode.PARAM_ERROR) {
                Assert.fail("Api should fail when both zoneid and zoneids are passed");
            }
        }
    }


    @Test
    public void testZoneidMinusOne() throws ResourceAllocationException {
        // If zoneId is passed as -1, then zone ids list should be null.
        registerTemplateCmdByAdmin = new RegisterTemplateCmdByAdmin();
        registerTemplateCmdByAdmin.zoneId = -1L;

        Assert.assertNull(registerTemplateCmdByAdmin.getZoneIds());
    }

    @Test
    public void testZoneidListMinusOne() throws ResourceAllocationException {
        // If zoneId List has only one parameter -1, then zone ids list should be null.
        registerTemplateCmdByAdmin = new RegisterTemplateCmdByAdmin();
        registerTemplateCmdByAdmin.zoneIds = new ArrayList<>();
        registerTemplateCmdByAdmin.zoneIds.add(-1L);

        Assert.assertNull(registerTemplateCmdByAdmin.getZoneIds());
    }

    @Test
    public void testZoneidListMoreThanMinusOne() throws ResourceAllocationException {
        try {
            registerTemplateCmdByAdmin = new RegisterTemplateCmdByAdmin();
            registerTemplateCmdByAdmin.zoneIds = new ArrayList<>();
            registerTemplateCmdByAdmin.zoneIds.add(-1L);
            registerTemplateCmdByAdmin.zoneIds.add(1L);
            registerTemplateCmdByAdmin.execute();
        } catch (ServerApiException e) {
            if (e.getErrorCode() != ApiErrorCode.PARAM_ERROR) {
                Assert.fail("Parameter zoneids cannot combine all zones (-1) option with other zones");
            }
        }
    }
   @Test
    public void testZoneidPresentZoneidListAbsent() throws ResourceAllocationException {
           registerTemplateCmdByAdmin  = new RegisterTemplateCmdByAdmin();
           registerTemplateCmdByAdmin.zoneIds = null;
           registerTemplateCmdByAdmin.zoneId = 1L;
           Assert.assertEquals((Long)1L,registerTemplateCmdByAdmin.getZoneIds().get(0));
    }
}
