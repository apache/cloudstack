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

import com.cloud.exception.InvalidParameterValueException;
import com.cloud.server.ManagementService;
import com.cloud.user.Account;
import com.cloud.user.AccountService;
import com.cloud.user.UserData;
import org.apache.cloudstack.api.ResponseGenerator;
import org.apache.cloudstack.api.response.UserDataResponse;
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

import static org.mockito.Mockito.when;

@RunWith(PowerMockRunner.class)
@PrepareForTest(CallContext.class)
@PowerMockIgnore({"javax.xml.*", "org.w3c.dom.*", "org.apache.xerces.*", "org.xml.*"})
public class RegisterUserDataCmdTest {

    @InjectMocks
    RegisterUserDataCmd cmd = new RegisterUserDataCmd();

    @Mock
    AccountService _accountService;

    @Mock
    ResponseGenerator _responseGenerator;

    @Mock
    ManagementService _mgr;

    private static final long DOMAIN_ID = 5L;
    private static final long PROJECT_ID = 10L;
    private static final String ACCOUNT_NAME = "user";

    @Before
    public void setUp() throws Exception {
        MockitoAnnotations.initMocks(this);
        ReflectionTestUtils.setField(cmd, "accountName", ACCOUNT_NAME);
        ReflectionTestUtils.setField(cmd, "domainId", DOMAIN_ID);
        ReflectionTestUtils.setField(cmd, "projectId", PROJECT_ID);
    }

    @Test
    public void testValidUserDataExecute() {
        UserData result = Mockito.mock(UserData.class);
        Mockito.doReturn(result).when(_mgr).registerUserData(cmd);

        UserDataResponse response = Mockito.mock(UserDataResponse.class);
        Mockito.doReturn(response).when(_responseGenerator).createUserDataResponse(result);

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
        ReflectionTestUtils.setField(cmd, "name", "testUserdataName");
        ReflectionTestUtils.setField(cmd, "userData", "testUserdata");

        when(_accountService.finalyzeAccountId(ACCOUNT_NAME, DOMAIN_ID, PROJECT_ID, true)).thenReturn(200L);

        Assert.assertEquals("testUserdataName", cmd.getName());
        Assert.assertEquals("testUserdata", cmd.getUserData());
        Assert.assertEquals(200L, cmd.getEntityOwnerId());
    }

    @Test(expected = InvalidParameterValueException.class)
    public void validateIfUserdataParamsHaveMetadataFileNames() {
        // If the userdata params have any key matched to the VR metadata file names, then it will throw exception
        ReflectionTestUtils.setField(cmd, "params", "key1,key2,key3,vm-id");
        cmd.getParams();
    }

}
