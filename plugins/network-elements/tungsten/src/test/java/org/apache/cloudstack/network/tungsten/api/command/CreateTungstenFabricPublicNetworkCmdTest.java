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
package org.apache.cloudstack.network.tungsten.api.command;

import com.cloud.dc.VlanVO;
import com.cloud.dc.dao.VlanDao;
import com.cloud.network.Network;
import com.cloud.network.NetworkModel;
import com.cloud.utils.db.SearchCriteria;
import org.apache.cloudstack.api.response.SuccessResponse;
import org.apache.cloudstack.network.tungsten.service.TungstenService;
import org.junit.After;
import org.junit.Assert;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.ArgumentMatchers;
import org.mockito.Mock;
import org.mockito.Mockito;
import org.mockito.MockitoAnnotations;
import org.mockito.junit.MockitoJUnitRunner;
import org.springframework.test.util.ReflectionTestUtils;

import java.util.Arrays;
import java.util.List;

@RunWith(MockitoJUnitRunner.class)
public class CreateTungstenFabricPublicNetworkCmdTest {

    @Mock
    VlanDao vlanDao;
    @Mock
    NetworkModel networkModel;
    @Mock
    TungstenService tungstenService;

    CreateTungstenFabricPublicNetworkCmd createTungstenFabricPublicNetworkCmd;

    AutoCloseable closeable;

    @Before
    public void setup() {
        closeable = MockitoAnnotations.openMocks(this);
        createTungstenFabricPublicNetworkCmd = new CreateTungstenFabricPublicNetworkCmd();
        createTungstenFabricPublicNetworkCmd.tungstenService = tungstenService;
        createTungstenFabricPublicNetworkCmd.vlanDao = vlanDao;
        createTungstenFabricPublicNetworkCmd.networkModel = networkModel;
        ReflectionTestUtils.setField(createTungstenFabricPublicNetworkCmd, "zoneId", 1L);
    }

    @After
    public void tearDown() throws Exception {
        closeable.close();
    }

    @Test
    public void executeTest() throws Exception {
        Network publicNetwork = Mockito.mock(Network.class);
        SearchCriteria<VlanVO> sc = Mockito.mock(SearchCriteria.class);
        List<VlanVO> pubVlanVOList = Arrays.asList(Mockito.mock(VlanVO.class));
        Mockito.when(networkModel.getSystemNetworkByZoneAndTrafficType(ArgumentMatchers.anyLong(),
                ArgumentMatchers.any())).thenReturn(publicNetwork);
        Mockito.when(vlanDao.createSearchCriteria()).thenReturn(sc);
        Mockito.when(vlanDao.listVlansByNetworkId(ArgumentMatchers.anyLong())).thenReturn(pubVlanVOList);

        Mockito.when(tungstenService.createPublicNetwork(ArgumentMatchers.anyLong())).thenReturn(true);
        Mockito.when(tungstenService.addPublicNetworkSubnet(ArgumentMatchers.any())).thenReturn(true);
        createTungstenFabricPublicNetworkCmd.execute();
        Assert.assertTrue(((SuccessResponse) createTungstenFabricPublicNetworkCmd.getResponseObject()).getSuccess());
    }
}
