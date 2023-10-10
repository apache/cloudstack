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

package com.cloud.upgrade;

import com.cloud.hypervisor.Hypervisor;
import com.cloud.storage.GuestOSHypervisorVO;
import com.cloud.storage.dao.GuestOSHypervisorDao;
import org.junit.After;
import org.junit.Assert;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.Mockito;
import org.mockito.MockitoAnnotations;
import org.mockito.Spy;
import org.mockito.junit.MockitoJUnitRunner;

import java.util.ArrayList;
import java.util.List;

@RunWith(MockitoJUnitRunner.class)
public class GuestOsMapperTest {

    @Spy
    @InjectMocks
    GuestOsMapper guestOsMapper = new GuestOsMapper();

    @Mock
    GuestOSHypervisorDao guestOSHypervisorDao;

    private AutoCloseable closeable;

    @Before
    public void setup() {
        closeable = MockitoAnnotations.openMocks(this);
    }

    @After
    public void tearDown() throws Exception {
        closeable.close();
    }

    @Test
    public void testCopyGuestOSHypervisorMappingsFailures() {
        boolean result = guestOsMapper.copyGuestOSHypervisorMappings(Hypervisor.HypervisorType.Any, "6.0", "7.0");
        Assert.assertFalse(result);

        result = guestOsMapper.copyGuestOSHypervisorMappings(Hypervisor.HypervisorType.None, "6.0", "7.0");
        Assert.assertFalse(result);

        result = guestOsMapper.copyGuestOSHypervisorMappings(Hypervisor.HypervisorType.XenServer, "", "7.0");
        Assert.assertFalse(result);

        result = guestOsMapper.copyGuestOSHypervisorMappings(Hypervisor.HypervisorType.XenServer, "6.0", "");
        Assert.assertFalse(result);

        Mockito.when(guestOSHypervisorDao.listByHypervisorTypeAndVersion(Mockito.anyString(), Mockito.anyString())).thenReturn(null);
        result = guestOsMapper.copyGuestOSHypervisorMappings(Hypervisor.HypervisorType.XenServer, "6.0", "7.0");
        Assert.assertFalse(result);
    }

    @Test
    public void testCopyGuestOSHypervisorMappingsSuccess() {
        GuestOSHypervisorVO guestOSHypervisorVO = Mockito.mock(GuestOSHypervisorVO.class);
        List<GuestOSHypervisorVO> guestOSHypervisorVOS = new ArrayList<>();
        guestOSHypervisorVOS.add(guestOSHypervisorVO);
        Mockito.when(guestOSHypervisorDao.listByHypervisorTypeAndVersion(Mockito.anyString(), Mockito.anyString())).thenReturn(guestOSHypervisorVOS);
        Mockito.when(guestOSHypervisorVO.getGuestOsName()).thenReturn("centos");

        boolean result = guestOsMapper.copyGuestOSHypervisorMappings(Hypervisor.HypervisorType.XenServer, "6.0", "7.0");
        Assert.assertTrue(result);
    }
}
