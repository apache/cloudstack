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

package org.apache.cloudstack.api.command.user.userdata;

import com.cloud.storage.Storage;
import com.cloud.template.TemplateApiService;
import com.cloud.template.VirtualMachineTemplate;
import com.cloud.user.Account;
import com.cloud.user.UserData;
import com.cloud.utils.db.EntityManager;
import org.apache.cloudstack.api.ResponseGenerator;
import org.apache.cloudstack.api.response.TemplateResponse;
import org.apache.cloudstack.context.CallContext;
import org.junit.Assert;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.Mockito;
import org.mockito.MockitoAnnotations;
import org.powermock.api.mockito.PowerMockito;
import org.powermock.core.classloader.annotations.PowerMockIgnore;
import org.powermock.core.classloader.annotations.PrepareForTest;
import org.powermock.modules.junit4.PowerMockRunner;
import org.springframework.test.util.ReflectionTestUtils;

@RunWith(PowerMockRunner.class)
@PrepareForTest(CallContext.class)
@PowerMockIgnore({"javax.xml.*", "org.w3c.dom.*", "org.apache.xerces.*", "org.xml.*"})
public class LinkUserDataToTemplateCmdTest {

    @Mock
    private ResponseGenerator _responseGenerator;

    @Mock
    private EntityManager _entityMgr;

    @InjectMocks
    LinkUserDataToTemplateCmd cmd = new LinkUserDataToTemplateCmd();

    @Mock
    TemplateApiService _templateService;

    @Mock
    VirtualMachineTemplate virtualMachineTemplate;

    @Before
    public void setUp() throws Exception {
        MockitoAnnotations.initMocks(this);
    }

    @Test
    public void testValidIds() {
        ReflectionTestUtils.setField(cmd, "userdataId", 1L);
        ReflectionTestUtils.setField(cmd, "templateId", 1L);
        TemplateResponse response = Mockito.mock(TemplateResponse.class);
        Mockito.doReturn(virtualMachineTemplate).when(_templateService).linkUserDataToTemplate(cmd);
        Mockito.doReturn(Storage.TemplateType.USER).when(virtualMachineTemplate).getTemplateType();
        Mockito.doReturn(response).when(_responseGenerator).createTemplateUpdateResponse(cmd.getResponseView(), virtualMachineTemplate);

        try {
            cmd.execute();
        } catch (Exception e) {
            e.printStackTrace();
        }
        Assert.assertEquals(response, cmd.getResponseObject());
    }

    @Test
    public void validateArgsCmd() {
        PowerMockito.mockStatic(CallContext.class);
        CallContext callContextMock = PowerMockito.mock(CallContext.class);
        PowerMockito.when(CallContext.current()).thenReturn(callContextMock);
        Account accountMock = PowerMockito.mock(Account.class);
        PowerMockito.when(callContextMock.getCallingAccount()).thenReturn(accountMock);
        Mockito.when(accountMock.getId()).thenReturn(2L);
        ReflectionTestUtils.setField(cmd, "templateId", 1L);
        ReflectionTestUtils.setField(cmd, "userdataId", 3L);

        Mockito.doReturn(virtualMachineTemplate).when(_entityMgr).findById(VirtualMachineTemplate.class, cmd.getTemplateId());
        PowerMockito.when(virtualMachineTemplate.getAccountId()).thenReturn(1L);

        Assert.assertEquals(1L, (long)cmd.getTemplateId());
        Assert.assertEquals(3L, (long)cmd.getUserdataId());
        Assert.assertEquals(1L, cmd.getEntityOwnerId());
    }

    @Test
    public void testDefaultOverridePolicy() {
        Assert.assertEquals(UserData.UserDataOverridePolicy.ALLOWOVERRIDE, cmd.getUserdataPolicy());
    }

}
