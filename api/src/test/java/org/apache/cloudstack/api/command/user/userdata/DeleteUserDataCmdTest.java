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

import com.cloud.server.ManagementService;
import com.cloud.user.AccountService;
import com.cloud.user.UserData;
import com.cloud.utils.db.EntityManager;
import org.apache.cloudstack.api.ServerApiException;
import org.apache.cloudstack.api.response.SuccessResponse;
import org.junit.After;
import org.junit.Assert;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.Mockito;
import org.mockito.MockitoAnnotations;
import org.mockito.junit.MockitoJUnitRunner;
import org.springframework.test.util.ReflectionTestUtils;

@RunWith(MockitoJUnitRunner.class)
public class DeleteUserDataCmdTest {

    @InjectMocks
    DeleteUserDataCmd cmd =  new DeleteUserDataCmd();

    @Mock
    AccountService _accountService;
    @Mock
    ManagementService _mgr;

    @Mock
    private EntityManager entityManagerMock;

    @Mock
    private UserData userDataMock;

    private static final long DOMAIN_ID = 5L;
    private static final long PROJECT_ID = 10L;
    private static final String ACCOUNT_NAME = "user";

    private AutoCloseable closeable;

    @Before
    public void setUp() throws Exception {
        closeable = MockitoAnnotations.openMocks(this);
        ReflectionTestUtils.setField(cmd, "accountName", ACCOUNT_NAME);
        ReflectionTestUtils.setField(cmd, "domainId", DOMAIN_ID);
        ReflectionTestUtils.setField(cmd, "projectId", PROJECT_ID);
    }

    @After
    public void tearDown() throws Exception {
        closeable.close();
    }

    @Test
    public void testValidUserDataExecute() {
        Mockito.doReturn(true).when(_mgr).deleteUserData(cmd);

        try {
            cmd.execute();
        } catch (Exception e) {
            e.printStackTrace();
        }
        Assert.assertEquals(cmd.getResponseObject().getClass(), SuccessResponse.class);
    }

    @Test(expected = ServerApiException.class)
    public void testDeleteFailure() {
        Mockito.doReturn(false).when(_mgr).deleteUserData(cmd);
        cmd.execute();
    }

    @Test
    public void getEntityOwnerIdTestReturnUserDataOwnerWhenUserDataIdIsProvided() {
        long userDataId = 1L;
        long userDataOwnerId = 2L;
        ReflectionTestUtils.setField(cmd, "id", userDataId);
        Mockito.when(entityManagerMock.findById(UserData.class, userDataId)).thenReturn(userDataMock);
        Mockito.when(userDataMock.getAccountId()).thenReturn(userDataOwnerId);

        Assert.assertEquals(userDataOwnerId, cmd.getEntityOwnerId());
    }
}
