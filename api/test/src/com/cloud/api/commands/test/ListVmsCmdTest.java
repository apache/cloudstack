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
package src.com.cloud.api.commands.test;

import java.util.ArrayList;
import java.util.EnumSet;
import java.util.List;
import java.util.UUID;

import junit.framework.TestCase;

import org.apache.cloudstack.api.command.user.vm.ListVMsCmd;
import org.junit.Before;
import org.junit.Rule;
import org.junit.Test;
import org.junit.rules.ExpectedException;
import org.mockito.Mockito;

import org.apache.cloudstack.api.ApiConstants.VMDetails;
import org.apache.cloudstack.api.ResponseGenerator;
import org.apache.cloudstack.api.ServerApiException;
import org.apache.cloudstack.api.response.ListResponse;
import org.apache.cloudstack.api.response.UserVmResponse;
import org.apache.cloudstack.api.view.vo.UserVmJoinVO;
import com.cloud.utils.Pair;
import com.cloud.vm.UserVmService;

public class ListVmsCmdTest extends TestCase {

    private ListVMsCmd listVmCmd;
    private UserVmService userVmService;
    private ResponseGenerator responseGenerator;

    @Rule
    public ExpectedException expectedException = ExpectedException.none();

    @Before
    public void setUp() {
        listVmCmd = new ListVMsCmd();
        userVmService = Mockito.mock(UserVmService.class);
        responseGenerator = Mockito.mock(ResponseGenerator.class);

        listVmCmd._userVmService = userVmService;
        listVmCmd._responseGenerator = responseGenerator;
    }

    @Test
    public void testExecuteForSingleResult() throws Exception {

        List<UserVmJoinVO> vmList = new ArrayList<UserVmJoinVO>();
        UserVmJoinVO vm = new UserVmJoinVO();
        String uuid = UUID.randomUUID().toString();
        vm.setUuid(uuid);
        vmList.add(vm);

        List<UserVmResponse> respList = new ArrayList<UserVmResponse>();
        UserVmResponse resp = new UserVmResponse();
        resp.setId(uuid);
        respList.add(resp);

        Mockito.when(userVmService.searchForUserVMs(listVmCmd))
                .thenReturn(new Pair<List<UserVmJoinVO>, Integer>(vmList, 1));
        Mockito.when(responseGenerator.createUserVmResponse("virtualmachine", EnumSet.of(VMDetails.all), vm)).thenReturn(respList);


        try {
            listVmCmd.execute();
            ListResponse<UserVmResponse> listResp = (ListResponse<UserVmResponse>)listVmCmd.getResponseObject();
            assertNotNull(listResp);
            assertEquals(1, listResp.getCount().intValue());
            List<UserVmResponse> vmResp = listResp.getResponses();
            assertTrue(vmResp != null && vmResp.size() == 1);
            UserVmResponse v = vmResp.get(0);
            assertEquals(uuid, v.getId());
        } catch (ServerApiException exception) {
            assertEquals("Failed to list user vms",
                    exception.getDescription());
        }
    }


    @Test
    public void testExecuteForPagedResult() throws Exception {


        List<UserVmJoinVO> vmList = new ArrayList<UserVmJoinVO>();
        UserVmJoinVO vm1 = new UserVmJoinVO();
        String uuid1 = UUID.randomUUID().toString();
        vm1.setUuid(uuid1);
        vmList.add(vm1);
        UserVmJoinVO vm2 = new UserVmJoinVO();
        String uuid2 = UUID.randomUUID().toString();
        vmList.add(vm2);

        List<UserVmResponse> respList = new ArrayList<UserVmResponse>();
        UserVmResponse resp1 = new UserVmResponse();
        resp1.setId(uuid1);
        respList.add(resp1);
        UserVmResponse resp2 = new UserVmResponse();
        resp2.setId(uuid2);
        respList.add(resp2);

        // without paging
        Mockito.when(userVmService.searchForUserVMs(listVmCmd))
                    .thenReturn(new Pair<List<UserVmJoinVO>, Integer>(vmList, 2));
        Mockito.when(responseGenerator.createUserVmResponse("virtualmachine", EnumSet.of(VMDetails.all), vm1, vm2)).thenReturn(respList);
        try {
            listVmCmd.execute();
            ListResponse<UserVmResponse> listResp = (ListResponse<UserVmResponse>)listVmCmd.getResponseObject();
            assertNotNull(listResp);
            assertEquals(2, listResp.getCount().intValue());
            List<UserVmResponse> vmResp = listResp.getResponses();
            assertTrue(vmResp != null && vmResp.size() == 2);
        } catch (ServerApiException exception) {
            assertEquals("Failed to list user vms without pagination",
                    exception.getDescription());
        }

        // with pagination
        List<UserVmJoinVO> pVmList = new ArrayList<UserVmJoinVO>();
        pVmList.add(vm1);

        List<UserVmResponse> pRespList = new ArrayList<UserVmResponse>();
        pRespList.add(resp1);

        listVmCmd = new ListVMsCmd() {
            public Integer getPage() {
                return 1;
            }

            public Integer getPageSize() {
                return 1;
            }

        };
        Mockito.when(userVmService.searchForUserVMs(listVmCmd))
                .thenReturn(new Pair<List<UserVmJoinVO>, Integer>(pVmList, 2));
        Mockito.when(responseGenerator.createUserVmResponse("virtualmachine", EnumSet.of(VMDetails.all), vm1)).thenReturn(pRespList);

        try {
            listVmCmd.execute();
            ListResponse<UserVmResponse> listResp = (ListResponse<UserVmResponse>)listVmCmd.getResponseObject();
            assertNotNull(listResp);
            assertEquals(2, listResp.getCount().intValue());
            List<UserVmResponse> vmResp = listResp.getResponses();
            assertTrue(vmResp != null && vmResp.size() == 1);
            UserVmResponse v = vmResp.get(0);
            assertEquals(uuid1, v.getId());
        } catch (ServerApiException exception) {
            assertEquals("Failed to list user vms with pagination",
                    exception.getDescription());
        }
    }
}
