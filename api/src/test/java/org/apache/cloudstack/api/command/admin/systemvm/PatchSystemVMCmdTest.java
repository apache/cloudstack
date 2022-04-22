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
// under the License.A
package org.apache.cloudstack.api.command.admin.systemvm;

import com.cloud.server.ManagementService;
import com.cloud.user.Account;
import com.cloud.utils.Pair;
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
public class PatchSystemVMCmdTest {

    @Mock
    private ManagementService _mgr;

    @InjectMocks
    PatchSystemVMCmd cmd = new PatchSystemVMCmd();

    @Before
    public void setUp() throws Exception {
        MockitoAnnotations.initMocks(this);
    }

    @Test
    public void patchValidSystemVM() {
        ReflectionTestUtils.setField(cmd, "id", 1L);
        Pair successResponse = new Pair<>(true, "");
        Mockito.doReturn(successResponse).when(_mgr).patchSystemVM(cmd);
        try {
            cmd.execute();
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    @Test
    public void patchInvalidSystemVM() {
        ReflectionTestUtils.setField(cmd, "id", null);
        Pair<Boolean, String> failureResponse = new Pair<>(false, "Please provide a valid ID of a system VM to be patched");
        Mockito.doReturn(failureResponse).when(_mgr).patchSystemVM(cmd);
        try {
            cmd.execute();
        } catch (Exception e) {
            Assert.assertEquals(failureResponse.second(), e.getMessage());
        }
    }

    @Test
    public void validateArgsForPatchSystemVMApi() {
        PowerMockito.mockStatic(CallContext.class);
        CallContext callContextMock = PowerMockito.mock(CallContext.class);
        PowerMockito.when(CallContext.current()).thenReturn(callContextMock);
        Account accountMock = PowerMockito.mock(Account.class);
        PowerMockito.when(callContextMock.getCallingAccount()).thenReturn(accountMock);
        Mockito.when(accountMock.getId()).thenReturn(2L);
        ReflectionTestUtils.setField(cmd, "id", 1L);
        Assert.assertEquals((long)cmd.getId(), 1L);
        Assert.assertFalse(cmd.isForced());
        Assert.assertEquals(cmd.getEntityOwnerId(), 2L);


    }
}
