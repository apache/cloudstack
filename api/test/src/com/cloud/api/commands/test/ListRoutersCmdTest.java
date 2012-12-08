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
import java.util.List;
import java.util.UUID;

import junit.framework.TestCase;

import org.apache.cloudstack.api.command.admin.router.ListRoutersCmd;
import org.junit.Before;
import org.junit.Rule;
import org.junit.Test;
import org.junit.rules.ExpectedException;
import org.mockito.Mockito;

import org.apache.cloudstack.api.ResponseGenerator;
import org.apache.cloudstack.api.ServerApiException;
import com.cloud.api.response.DomainRouterResponse;
import com.cloud.api.response.ListResponse;
import com.cloud.api.view.vo.DomainRouterJoinVO;
import com.cloud.server.ManagementService;
import com.cloud.utils.Pair;


public class ListRoutersCmdTest extends TestCase {

    private ListRoutersCmd listVrCmd;
    private ManagementService mgrService;
    private ResponseGenerator responseGenerator;

    @Rule
    public ExpectedException expectedException = ExpectedException.none();

    @Before
    public void setUp() {
        listVrCmd = new ListRoutersCmd();
        mgrService = Mockito.mock(ManagementService.class);
        responseGenerator = Mockito.mock(ResponseGenerator.class);

        listVrCmd._mgr = mgrService;
        listVrCmd._responseGenerator = responseGenerator;
    }

    @Test
    public void testExecuteForSingleResult() throws Exception {

        List<DomainRouterJoinVO> vrList = new ArrayList<DomainRouterJoinVO>();
        DomainRouterJoinVO vr = new DomainRouterJoinVO();
        String uuid = UUID.randomUUID().toString();
        vr.setUuid(uuid);
        vrList.add(vr);

        List<DomainRouterResponse> respList = new ArrayList<DomainRouterResponse>();
        DomainRouterResponse resp = new DomainRouterResponse();
        resp.setId(uuid);
        respList.add(resp);

        Mockito.when(mgrService.searchForRouters(listVrCmd))
                .thenReturn(new Pair<List<DomainRouterJoinVO>, Integer>(vrList, 1));
        Mockito.when(responseGenerator.createDomainRouterResponse(vr)).thenReturn(respList);


        try {
            listVrCmd.execute();
            ListResponse<DomainRouterResponse> listResp = (ListResponse<DomainRouterResponse>)listVrCmd.getResponseObject();
            assertNotNull(listResp);
            assertEquals(1, listResp.getCount().intValue());
            List<DomainRouterResponse> vrResp = listResp.getResponses();
            assertTrue(vrResp != null && vrResp.size() == 1);
            DomainRouterResponse v = vrResp.get(0);
            assertEquals(uuid, v.getId());
        } catch (ServerApiException exception) {
            assertEquals("Failed to list domain routers",
                    exception.getDescription());
        }
    }


    @Test
    public void testExecuteForPagedResult() throws Exception {


        List<DomainRouterJoinVO> vrList = new ArrayList<DomainRouterJoinVO>();
        DomainRouterJoinVO vr1 = new DomainRouterJoinVO();
        String uuid1 = UUID.randomUUID().toString();
        vr1.setUuid(uuid1);
        vrList.add(vr1);
        DomainRouterJoinVO vr2 = new DomainRouterJoinVO();
        String uuid2 = UUID.randomUUID().toString();
        vrList.add(vr2);

        List<DomainRouterResponse> respList = new ArrayList<DomainRouterResponse>();
        DomainRouterResponse resp1 = new DomainRouterResponse();
        resp1.setId(uuid1);
        respList.add(resp1);
        DomainRouterResponse resp2 = new DomainRouterResponse();
        resp2.setId(uuid2);
        respList.add(resp2);

        // without paging
        Mockito.when(mgrService.searchForRouters(listVrCmd))
                    .thenReturn(new Pair<List<DomainRouterJoinVO>, Integer>(vrList, 2));
        Mockito.when(responseGenerator.createDomainRouterResponse(vr1, vr2)).thenReturn(respList);
        try {
            listVrCmd.execute();
            ListResponse<DomainRouterResponse> listResp = (ListResponse<DomainRouterResponse>)listVrCmd.getResponseObject();
            assertNotNull(listResp);
            assertEquals(2, listResp.getCount().intValue());
            List<DomainRouterResponse> vrResp = listResp.getResponses();
            assertTrue(vrResp != null && vrResp.size() == 2);
        } catch (ServerApiException exception) {
            assertEquals("Failed to list domain routers without pagination",
                    exception.getDescription());
        }

        // with pagination
        List<DomainRouterJoinVO> pVrList = new ArrayList<DomainRouterJoinVO>();
        pVrList.add(vr1);

        List<DomainRouterResponse> pRespList = new ArrayList<DomainRouterResponse>();
        pRespList.add(resp1);

        listVrCmd = new ListRoutersCmd() {
            public Integer getPage() {
                return 1;
            }

            public Integer getPageSize() {
                return 1;
            }

        };
        Mockito.when(mgrService.searchForRouters(listVrCmd))
                .thenReturn(new Pair<List<DomainRouterJoinVO>, Integer>(pVrList, 2));
        Mockito.when(responseGenerator.createDomainRouterResponse(vr1)).thenReturn(pRespList);

        try {
            listVrCmd.execute();
            ListResponse<DomainRouterResponse> listResp = (ListResponse<DomainRouterResponse>)listVrCmd.getResponseObject();
            assertNotNull(listResp);
            assertEquals(2, listResp.getCount().intValue());
            List<DomainRouterResponse> vmResp = listResp.getResponses();
            assertTrue(vmResp != null && vmResp.size() == 1);
            DomainRouterResponse v = vmResp.get(0);
            assertEquals(uuid1, v.getId());
        } catch (ServerApiException exception) {
            assertEquals("Failed to list domain routers with pagination",
                    exception.getDescription());
        }
    }
}

