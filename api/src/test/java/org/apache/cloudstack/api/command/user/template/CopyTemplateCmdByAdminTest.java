// Licensed to the Apache Software Foundation (ASF) under one
// or more contributor license agreements.  See the NOTICE file
// distributed with this work for additional information
// regarding copyright ownership.  The ASF licenses this file
// to you under the Apache License, Version 2.0 (the
// "License"); you may not use this file except in compliance
// with the License.  You may obtain a copy of the License at
//
//  http://www.apache.org/licenses/LICENSE-2.0
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
import org.apache.cloudstack.api.command.admin.template.CopyTemplateCmdByAdmin;
import org.junit.Assert;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.runners.MockitoJUnitRunner;

import java.util.ArrayList;

/**
 * Created by stack on 7/21/16.
 */
@RunWith(MockitoJUnitRunner.class)
public class CopyTemplateCmdByAdminTest{

    @InjectMocks
    private CopyTemplateCmdByAdmin copyTemplateCmdByAdmin;

    @Mock
    public TemplateApiService _templateService;

    @Test
    public void testZoneidAndZoneIdListEmpty() throws ResourceAllocationException {
        try {
            copyTemplateCmdByAdmin = new CopyTemplateCmdByAdmin();
            copyTemplateCmdByAdmin.execute();
        } catch (ServerApiException e) {
            if(e.getErrorCode() != ApiErrorCode.PARAM_ERROR) {
                Assert.fail("API should fail when no parameters are passed.");
            }
        }
    }

    @Test
    public void testDestZoneidAndDestZoneIdListBothPresent() throws ResourceAllocationException {
        try {
            copyTemplateCmdByAdmin = new CopyTemplateCmdByAdmin();
            copyTemplateCmdByAdmin.destZoneId = -1L;
            copyTemplateCmdByAdmin.destZoneIds = new ArrayList<>();
            copyTemplateCmdByAdmin.destZoneIds.add(-1L);

            copyTemplateCmdByAdmin.execute();
        } catch (ServerApiException e) {
            if(e.getErrorCode() != ApiErrorCode.PARAM_ERROR) {
                Assert.fail("Api should fail when both destzoneid and destzoneids are passed");
            }
        }
    }


}
