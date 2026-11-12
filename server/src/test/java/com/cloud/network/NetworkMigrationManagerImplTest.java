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
package com.cloud.network;

import java.util.Arrays;
import java.util.List;

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
import org.springframework.test.util.ReflectionTestUtils;

import com.cloud.network.dao.NetworkDetailVO;
import com.cloud.network.dao.NetworkDetailsDao;
import com.cloud.vm.NicVO;
import com.cloud.vm.dao.NicDao;
import com.cloud.vm.dao.NicNetworkMapDao;
import com.cloud.vm.dao.NicNetworkMapVO;

@RunWith(MockitoJUnitRunner.class)
public class NetworkMigrationManagerImplTest {

    @Mock
    private VpcDetailsDao vpcDetailsDao;

    @Mock
    private NetworkDetailsDao networkDetailsDao;

    @Mock
    private NicDao nicDao;
    @Mock
    private NicNetworkMapDao nicNetworkMapDao;

    @InjectMocks
    private NetworkMigrationManagerImpl manager = new NetworkMigrationManagerImpl();

    @Test
    public void testCopyVpcDetailsSkipsPublicNetworkRate() {
        final long srcVpcId = 10L;
        final long dstVpcId = 11L;
        Mockito.when(vpcDetailsDao.listDetails(srcVpcId)).thenReturn(Arrays.asList(
                new VpcDetailVO(srcVpcId, ApiConstants.PUBLIC_NETWORK_RATE, "200", true),
                new VpcDetailVO(srcVpcId, "someKey", "someValue", false)));

        manager.copyVpcDetails(srcVpcId, dstVpcId);

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

        manager.copyNetworkDetails(srcNetworkId, dstNetworkId);

        ArgumentCaptor<NetworkDetailVO> captor = ArgumentCaptor.forClass(NetworkDetailVO.class);
        Mockito.verify(networkDetailsDao, Mockito.times(1)).persist(captor.capture());
        Assert.assertEquals("someKey", captor.getValue().getName());
        Assert.assertEquals(dstNetworkId, captor.getValue().getResourceId());
    }

    @Test
    public void moveTrunkAssociationsRepointsAssociationsAndFlagsNewNic() {
        NicVO originalNic = Mockito.spy(new NicVO("guru", 1L, 10L, null));
        originalNic.setMultiNetwork(true);
        Mockito.doReturn(1L).when(originalNic).getId();
        NicVO newNic = Mockito.spy(new NicVO("guru", 2L, 20L, null));
        Mockito.doReturn(2L).when(newNic).getId();
        NicNetworkMapVO association = new NicNetworkMapVO(1L, 30L, "10.0.0.5", null);
        Mockito.when(nicNetworkMapDao.listByNicId(originalNic.getId())).thenReturn(List.of(association));

        ReflectionTestUtils.invokeMethod(manager, "moveTrunkAssociations", originalNic, newNic);

        Mockito.verify(nicNetworkMapDao).update(association.getId(), association);
        org.junit.Assert.assertEquals(2L, association.getNicId());
        org.junit.Assert.assertTrue(newNic.getMultiNetwork());
        org.junit.Assert.assertFalse(originalNic.getMultiNetwork());
    }

    @Test
    public void moveTrunkAssociationsSkipsNonTrunkNic() {
        NicVO originalNic = new NicVO("guru", 1L, 10L, null);
        NicVO newNic = new NicVO("guru", 2L, 20L, null);

        ReflectionTestUtils.invokeMethod(manager, "moveTrunkAssociations", originalNic, newNic);

        Mockito.verifyNoInteractions(nicNetworkMapDao);
        org.junit.Assert.assertFalse(newNic.getMultiNetwork());
    }
}
