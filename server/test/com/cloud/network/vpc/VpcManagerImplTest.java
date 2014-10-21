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
package com.cloud.network.vpc;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotNull;
import static org.mockito.Mockito.when;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.Set;

import org.junit.Before;
import org.junit.Test;
import org.mockito.Mock;
import org.mockito.Mockito;
import org.mockito.MockitoAnnotations;

import com.cloud.network.Network.Provider;
import com.cloud.network.Network.Service;
import com.cloud.network.vpc.dao.VpcOfferingServiceMapDao;

public class VpcManagerImplTest {

    @Mock
    VpcOfferingServiceMapDao vpcOffSvcMapDao;
    VpcManagerImpl manager;

    @Before
    public void setup()
    {
        MockitoAnnotations.initMocks(this);
        manager = new VpcManagerImpl();
        manager._vpcOffSvcMapDao = vpcOffSvcMapDao;
    }
    @Test
    public void getVpcOffSvcProvidersMapForEmptyServiceTest() {
        long vpcOffId = 1L;
        List<VpcOfferingServiceMapVO> list = new ArrayList<VpcOfferingServiceMapVO>();
        list.add(Mockito.mock(VpcOfferingServiceMapVO.class));
        when(manager._vpcOffSvcMapDao.listByVpcOffId(vpcOffId)).thenReturn(list);

        Map<Service, Set<Provider>> map = manager.getVpcOffSvcProvidersMap(vpcOffId);

        assertNotNull(map);
        assertEquals(map.size(),1);
    }

}
