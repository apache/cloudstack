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
package com.cloud.network;

import java.util.Arrays;

import org.apache.cloudstack.api.ApiConstants;
import org.apache.cloudstack.resourcedetail.VpcDetailVO;
import org.apache.cloudstack.resourcedetail.dao.VpcDetailsDao;
import org.junit.Assert;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.ArgumentCaptor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.Mockito;
import org.mockito.junit.MockitoJUnitRunner;

import com.cloud.network.dao.NetworkDetailVO;
import com.cloud.network.dao.NetworkDetailsDao;

@RunWith(MockitoJUnitRunner.class)
public class NetworkMigrationManagerImplTest {

    @Mock
    private VpcDetailsDao vpcDetailsDao;

    @Mock
    private NetworkDetailsDao networkDetailsDao;

    @InjectMocks
    private NetworkMigrationManagerImpl networkMigrationManager;

    @Test
    public void testCopyVpcDetailsSkipsPublicNetworkRate() {
        final long srcVpcId = 10L;
        final long dstVpcId = 11L;
        Mockito.when(vpcDetailsDao.listDetails(srcVpcId)).thenReturn(Arrays.asList(
                new VpcDetailVO(srcVpcId, ApiConstants.PUBLIC_NETWORK_RATE, "200", true),
                new VpcDetailVO(srcVpcId, "someKey", "someValue", false)));

        networkMigrationManager.copyVpcDetails(srcVpcId, dstVpcId);

        Mockito.verify(vpcDetailsDao, Mockito.never()).addDetail(Mockito.eq(dstVpcId), Mockito.eq(ApiConstants.PUBLIC_NETWORK_RATE),
                Mockito.anyString(), Mockito.anyBoolean());
        Mockito.verify(vpcDetailsDao).addDetail(dstVpcId, "someKey", "someValue", false);
    }

    @Test
    public void testCopyNetworkDetailsSkipsNetworkRate() {
        final long srcNetworkId = 20L;
        final long dstNetworkId = 21L;
        Mockito.when(networkDetailsDao.listDetails(srcNetworkId)).thenReturn(Arrays.asList(
                new NetworkDetailVO(srcNetworkId, ApiConstants.NETWORKRATE, "100", true),
                new NetworkDetailVO(srcNetworkId, "someKey", "someValue", false)));

        networkMigrationManager.copyNetworkDetails(srcNetworkId, dstNetworkId);

        ArgumentCaptor<NetworkDetailVO> captor = ArgumentCaptor.forClass(NetworkDetailVO.class);
        Mockito.verify(networkDetailsDao, Mockito.times(1)).persist(captor.capture());
        Assert.assertEquals("someKey", captor.getValue().getName());
        Assert.assertEquals(dstNetworkId, captor.getValue().getResourceId());
    }
}
