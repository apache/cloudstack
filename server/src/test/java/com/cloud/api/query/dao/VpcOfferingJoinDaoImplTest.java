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
package com.cloud.api.query.dao;

import org.apache.cloudstack.api.response.VpcOfferingResponse;
import org.junit.Assert;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.Mockito;
import org.mockito.junit.MockitoJUnitRunner;
import org.springframework.test.util.ReflectionTestUtils;

import com.cloud.api.query.vo.VpcOfferingJoinVO;
import com.cloud.network.vpc.VpcOffering;
import com.cloud.network.vpc.dao.VpcOfferingServiceMapDao;

@RunWith(MockitoJUnitRunner.Silent.class)
public class VpcOfferingJoinDaoImplTest {

    @Mock
    private VpcOfferingServiceMapDao vpcOfferingServiceMapDao;

    @InjectMocks
    private VpcOfferingJoinDaoImpl dao = new VpcOfferingJoinDaoImpl();

    private VpcOfferingResponse responseFor(Integer publicNetworkRate) {
        final VpcOfferingJoinVO offering = Mockito.mock(VpcOfferingJoinVO.class);
        Mockito.when(offering.getState()).thenReturn(VpcOffering.State.Enabled);
        Mockito.when(offering.getPublicNetworkRate()).thenReturn(publicNetworkRate);
        return dao.newVpcOfferingResponse(offering);
    }

    @Test
    public void responseCarriesPublicNetworkRate() {
        Assert.assertEquals(100, ReflectionTestUtils.getField(responseFor(100), "publicNetworkRate"));
    }

    @Test
    public void responseCarriesUnlimitedPublicNetworkRate() {
        Assert.assertEquals(-1, ReflectionTestUtils.getField(responseFor(-1), "publicNetworkRate"));
    }

    @Test
    public void responseLeavesPublicNetworkRateUnsetWhenOfferingHasNone() {
        Assert.assertNull(ReflectionTestUtils.getField(responseFor(null), "publicNetworkRate"));
    }
}
