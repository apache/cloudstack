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
import com.cloud.user.UserData;
import com.cloud.utils.Pair;
import org.apache.cloudstack.api.response.ListResponse;
import org.junit.After;
import org.junit.Assert;
import org.apache.cloudstack.api.ResponseGenerator;
import org.apache.cloudstack.api.response.UserDataResponse;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.Mockito;
import org.mockito.MockitoAnnotations;
import org.mockito.junit.MockitoJUnitRunner;

import java.util.ArrayList;
import java.util.List;

@RunWith(MockitoJUnitRunner.class)
public class ListUserDataCmdTest {

    @InjectMocks
    ListUserDataCmd cmd = new ListUserDataCmd();

    @Mock
    ManagementService _mgr;

    @Mock
    ResponseGenerator _responseGenerator;

    private AutoCloseable closeable;

    @Before
    public void setUp() throws Exception {
        closeable = MockitoAnnotations.openMocks(this);
    }

    @After
    public void tearDown() throws Exception {
        closeable.close();
    }

    @Test
    public void testListSuccess() {
        UserData userData = Mockito.mock(UserData.class);
        List<UserData> userDataList = new ArrayList<UserData>();
        userDataList.add(userData);
        Pair<List<? extends UserData>, Integer> result = new Pair<List<? extends UserData>, Integer>(userDataList, 1);
        UserDataResponse userDataResponse = Mockito.mock(UserDataResponse.class);

        Mockito.when(_mgr.listUserDatas(cmd)).thenReturn(result);
        Mockito.when(_responseGenerator.createUserDataResponse(userData)).thenReturn(userDataResponse);

        cmd.execute();

        ListResponse<UserDataResponse> actualResponse = (ListResponse<UserDataResponse>)cmd.getResponseObject();
        Assert.assertEquals(userDataResponse, actualResponse.getResponses().get(0));
    }

    @Test
    public void testEmptyList() {
        List<UserData> userDataList = new ArrayList<UserData>();
        Pair<List<? extends UserData>, Integer> result = new Pair<List<? extends UserData>, Integer>(userDataList, 0);

        Mockito.when(_mgr.listUserDatas(cmd)).thenReturn(result);

        cmd.execute();

        ListResponse<UserDataResponse> actualResponse = (ListResponse<UserDataResponse>)cmd.getResponseObject();
        Assert.assertEquals(new ArrayList<>(), actualResponse.getResponses());
    }
}
