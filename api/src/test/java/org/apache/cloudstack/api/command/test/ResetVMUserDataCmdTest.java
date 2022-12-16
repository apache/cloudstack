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

import com.cloud.user.AccountService;
import com.cloud.uservm.UserVm;
import com.cloud.vm.UserVmService;
import org.apache.cloudstack.api.ResponseGenerator;
import org.apache.cloudstack.api.ResponseObject;
import org.apache.cloudstack.api.command.user.vm.ResetVMUserDataCmd;
import org.apache.cloudstack.api.response.UserVmResponse;
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

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import static org.mockito.Mockito.when;

@RunWith(PowerMockRunner.class)
@PrepareForTest(CallContext.class)
@PowerMockIgnore({"javax.xml.*", "org.w3c.dom.*", "org.apache.xerces.*", "org.xml.*"})
public class ResetVMUserDataCmdTest {

    @InjectMocks
    ResetVMUserDataCmd cmd = new ResetVMUserDataCmd();

    @Mock
    AccountService _accountService;

    @Mock
    ResponseGenerator _responseGenerator;

    @Mock
    UserVmService _userVmService;

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
    public void testValidResetVMUserDataExecute() {
        UserVm result = Mockito.mock(UserVm.class);

        UserVmResponse response = new UserVmResponse();
        List<UserVmResponse> responseList = new ArrayList<>();
        responseList.add(response);
        Mockito.doReturn(responseList).when(_responseGenerator).createUserVmResponse(ResponseObject.ResponseView.Restricted, "virtualmachine", result);

        try {
            Mockito.doReturn(result).when(_userVmService).resetVMUserData(cmd);
            cmd.execute();
        } catch (Exception e) {
            e.printStackTrace();
        }
        Assert.assertEquals(response, cmd.getResponseObject());
        Assert.assertEquals("resetuserdataforvirtualmachineresponse", response.getResponseName());
    }

    @Test
    public void validateArgsCmd() {
        PowerMockito.mockStatic(CallContext.class);
        CallContext callContextMock = PowerMockito.mock(CallContext.class);
        PowerMockito.when(CallContext.current()).thenReturn(callContextMock);

        ReflectionTestUtils.setField(cmd, "id", 1L);
        ReflectionTestUtils.setField(cmd, "userdataId", 2L);
        ReflectionTestUtils.setField(cmd, "userData", "testUserdata");

        UserVm vm = Mockito.mock(UserVm.class);
        when(_responseGenerator.findUserVmById(1L)).thenReturn(vm);
        when(vm.getAccountId()).thenReturn(200L);

        Assert.assertEquals(1L, (long)cmd.getId());
        Assert.assertEquals(2L, (long)cmd.getUserdataId());
        Assert.assertEquals("testUserdata", cmd.getUserData());
        Assert.assertEquals(200L, cmd.getEntityOwnerId());
    }

    @Test
    public void testUserdataDetails() {
        Map<String, String> values1 = new HashMap<>();
        values1.put("key1", "value1");
        values1.put("key2", "value2");

        Map<String, String> values2 = new HashMap<>();
        values1.put("key3", "value3");
        values1.put("key4", "value4");

        Map<Integer, Map<String, String>> userdataDetails = new HashMap<>();
        userdataDetails.put(0, values1);
        userdataDetails.put(1, values2);

        ReflectionTestUtils.setField(cmd, "userdataDetails", userdataDetails);

        Map<String, String> result = cmd.getUserdataDetails();

        values1.putAll(values2);
        Assert.assertEquals(values1, result);
    }

}
