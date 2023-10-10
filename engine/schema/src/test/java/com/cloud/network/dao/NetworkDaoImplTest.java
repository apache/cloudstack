/*
 * Licensed to the Apache Software Foundation (ASF) under one
 * or more contributor license agreements.  See the NOTICE file
 * distributed with this work for additional information
 * regarding copyright ownership.  The ASF licenses this file
 * to you under the Apache License, Version 2.0 (the
 * "License"); you may not use this file except in compliance
 * with the License.  You may obtain a copy of the License at
 *
 *   http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing,
 * software distributed under the License is distributed on an
 * "AS IS" BASIS, WITHOUT WARRANTIES OR CONDITIONS OF ANY
 * KIND, either express or implied.  See the License for the
 * specific language governing permissions and limitations
 * under the License.
 */

package com.cloud.network.dao;

import com.cloud.network.Networks;
import com.cloud.utils.db.SearchBuilder;
import com.cloud.utils.db.SearchCriteria;
import com.cloud.utils.db.TransactionLegacy;
import org.junit.Assert;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.Mock;
import org.mockito.Mockito;
import org.mockito.junit.MockitoJUnitRunner;


import java.util.List;

@RunWith(MockitoJUnitRunner.class)
public class NetworkDaoImplTest {

    @Mock
    SearchBuilder<NetworkVO> searchBuilderNetworkVoMock;

    @Mock
    SearchCriteria<NetworkVO> searchCriteriaNetworkVoMock;

    @Mock
    List<NetworkVO> listNetworkVoMock;

    @Test
    public void listByPhysicalNetworkTrafficTypeTestSetParametersValidation() throws Exception {
        NetworkDaoImpl networkDaoImplSpy = Mockito.spy(NetworkDaoImpl.class);
        TransactionLegacy txn = TransactionLegacy.open("runNetworkDaoImplTest");
        try {
            networkDaoImplSpy.AllFieldsSearch = searchBuilderNetworkVoMock;
            Mockito.doReturn(searchCriteriaNetworkVoMock).when(searchBuilderNetworkVoMock).create();
            Mockito.doNothing().when(searchCriteriaNetworkVoMock).setParameters(Mockito.anyString(), Mockito.any());
            Mockito.doReturn(listNetworkVoMock).when(networkDaoImplSpy).listBy(Mockito.any(SearchCriteria.class));

            long expectedPhysicalNetwork = 2513l;

            for (Networks.TrafficType trafficType : Networks.TrafficType.values()) {
                List<NetworkVO> result = networkDaoImplSpy.listByPhysicalNetworkTrafficType(expectedPhysicalNetwork, trafficType);
                Assert.assertEquals(listNetworkVoMock, result);
                Mockito.verify(searchCriteriaNetworkVoMock).setParameters("trafficType", trafficType);
            }

            Mockito.verify(searchCriteriaNetworkVoMock, Mockito.times(Networks.TrafficType.values().length)).setParameters("physicalNetwork", expectedPhysicalNetwork);
        } finally {
            txn.close();
        }
    }
}
