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

import com.cloud.dc.DataCenter;
import com.cloud.network.Network;
import com.cloud.network.NetworkModel;
import com.cloud.network.dao.NetworkServiceMapDao;
import com.cloud.network.dao.PhysicalNetworkServiceProviderDao;
import com.cloud.offerings.NetworkOfferingVO;
import com.cloud.offerings.dao.NetworkOfferingDao;
import com.cloud.offerings.dao.NetworkOfferingServiceMapDao;
import com.cloud.utils.db.EntityManager;
import com.cloud.utils.db.Transaction;
import com.cloud.utils.db.TransactionCallbackNoReturn;
import org.apache.cloudstack.api.response.SuccessResponse;
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

import static org.mockito.ArgumentMatchers.any;
import static org.powermock.api.mockito.PowerMockito.mockStatic;

@RunWith(PowerMockRunner.class)
@PrepareForTest({Transaction.class, ConfigTungstenFabricServiceCmd.class})
public class ConfigTungstenFabricServiceCmdTest {
    @Mock
    EntityManager entityManager;
    @Mock
    NetworkModel networkModel;
    @Mock
    NetworkOfferingDao networkOfferingDao;
    @Mock
    NetworkOfferingServiceMapDao networkOfferingServiceMapDao;
    @Mock
    NetworkServiceMapDao networkServiceMapDao;
    @Mock
    PhysicalNetworkServiceProviderDao physicalNetworkServiceProviderDao;

    ConfigTungstenFabricServiceCmd configTungstenFabricServiceCmd;

    @Before
    public void setup() {
        MockitoAnnotations.initMocks(this);
        configTungstenFabricServiceCmd = new ConfigTungstenFabricServiceCmd();
        configTungstenFabricServiceCmd._entityMgr = entityManager;
        configTungstenFabricServiceCmd.networkModel = networkModel;
        configTungstenFabricServiceCmd.networkOfferingDao = networkOfferingDao;
        configTungstenFabricServiceCmd.networkOfferingServiceMapDao = networkOfferingServiceMapDao;
        configTungstenFabricServiceCmd.networkServiceMapDao = networkServiceMapDao;
        configTungstenFabricServiceCmd.physicalNetworkServiceProviderDao = physicalNetworkServiceProviderDao;
        Whitebox.setInternalState(configTungstenFabricServiceCmd, "zoneId", 1L);
        Whitebox.setInternalState(configTungstenFabricServiceCmd, "physicalNetworkId", 1L);
    }

    @Test
    public void executeTest() throws Exception {
        SuccessResponse successResponse = Mockito.mock(SuccessResponse.class);
        DataCenter dataCenter = Mockito.mock(DataCenter.class);
        Network managementNetwork = Mockito.mock(Network.class);
        TransactionCallbackNoReturn transactionCallbackNoReturn = Mockito.mock(TransactionCallbackNoReturn.class);
        List<NetworkOfferingVO> systemNetworkOffering = Arrays.asList(Mockito.mock(NetworkOfferingVO.class));
        mockStatic(Transaction.class);
        Mockito.when(entityManager.findById(ArgumentMatchers.any(), ArgumentMatchers.anyLong())).thenReturn(dataCenter);
        Mockito.when(dataCenter.isSecurityGroupEnabled()).thenReturn(true);
        Mockito.when(networkModel.getSystemNetworkByZoneAndTrafficType(ArgumentMatchers.anyLong(),
                ArgumentMatchers.any())).thenReturn(managementNetwork);
        Mockito.when(networkOfferingDao.listSystemNetworkOfferings()).thenReturn(systemNetworkOffering);
        PowerMockito.when(Transaction.execute(any(TransactionCallbackNoReturn.class))).thenReturn(transactionCallbackNoReturn);
        PowerMockito.whenNew(SuccessResponse.class).withAnyArguments().thenReturn(successResponse);
        configTungstenFabricServiceCmd.execute();
        Assert.assertEquals(successResponse, configTungstenFabricServiceCmd.getResponseObject());
    }

}
