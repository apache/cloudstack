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
import org.junit.Assert;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.ArgumentMatchers;
import org.mockito.Mock;
import org.mockito.Mockito;
import org.mockito.MockitoAnnotations;
import org.powermock.api.mockito.PowerMockito;
import org.powermock.core.classloader.annotations.PrepareForTest;
import org.powermock.modules.junit4.PowerMockRunner;
import org.powermock.reflect.Whitebox;

import java.util.Arrays;
import java.util.List;

@RunWith(PowerMockRunner.class)
@PrepareForTest(CreateTungstenFabricPublicNetworkCmd.class)
public class CreateTungstenFabricPublicNetworkCmdTest {

    @Mock
    VlanDao vlanDao;
    @Mock
    NetworkModel networkModel;
    @Mock
    TungstenService tungstenService;

    CreateTungstenFabricPublicNetworkCmd createTungstenFabricPublicNetworkCmd;

    @Before
    public void setup() {
        MockitoAnnotations.initMocks(this);
        createTungstenFabricPublicNetworkCmd = new CreateTungstenFabricPublicNetworkCmd();
        createTungstenFabricPublicNetworkCmd.tungstenService = tungstenService;
        createTungstenFabricPublicNetworkCmd.vlanDao = vlanDao;
        createTungstenFabricPublicNetworkCmd.networkModel = networkModel;
        Whitebox.setInternalState(createTungstenFabricPublicNetworkCmd, "zoneId", 1L);
    }

    @Test
    public void executeTest() throws Exception {
        SuccessResponse successResponse = Mockito.mock(SuccessResponse.class);
        Network publicNetwork = Mockito.mock(Network.class);
        SearchCriteria<VlanVO> sc = Mockito.mock(SearchCriteria.class);
        List<VlanVO> pubVlanVOList = Arrays.asList(Mockito.mock(VlanVO.class));
        Mockito.when(networkModel.getSystemNetworkByZoneAndTrafficType(ArgumentMatchers.anyLong(),
                ArgumentMatchers.any())).thenReturn(publicNetwork);
        Mockito.when(vlanDao.createSearchCriteria()).thenReturn(sc);
        Mockito.when(vlanDao.listVlansByNetworkId(ArgumentMatchers.anyLong())).thenReturn(pubVlanVOList);

        Mockito.when(tungstenService.createPublicNetwork(ArgumentMatchers.anyLong())).thenReturn(true);
        Mockito.when(tungstenService.addPublicNetworkSubnet(ArgumentMatchers.any())).thenReturn(true);
        PowerMockito.whenNew(SuccessResponse.class).withAnyArguments().thenReturn(successResponse);
        createTungstenFabricPublicNetworkCmd.execute();
        Assert.assertEquals(successResponse, createTungstenFabricPublicNetworkCmd.getResponseObject());
    }
}
