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
package com.cloud.configuration;

import com.cloud.dc.VlanVO;
import com.cloud.network.Network;
import com.cloud.network.NetworkModel;
import com.cloud.utils.Pair;
import org.junit.After;
import org.junit.Assert;
import org.junit.Before;
import org.junit.Test;
import org.mockito.Mock;
import org.mockito.MockitoAnnotations;

import java.util.ArrayList;
import java.util.List;

import static org.mockito.Mockito.when;

public class ValidateIpRangeTest {
    @Mock
    NetworkModel _networkModel;
    @Mock
    VlanVO vlan;
    @Mock
    Network network;
    ConfigurationManagerImpl configurationMgr = new ConfigurationManagerImpl();
    List<VlanVO> vlanVOList = new ArrayList<VlanVO>();
    private AutoCloseable closeable;

    @Before
    public void setup() {
        closeable = MockitoAnnotations.openMocks(this);
        configurationMgr._networkModel = _networkModel;
        vlanVOList.add(vlan);
        when(vlan.getVlanGateway()).thenReturn("10.147.33.1");
        when(vlan.getVlanNetmask()).thenReturn("255.255.255.128");

    }

    @After
    public void tearDown() throws Exception {
        closeable.close();
    }

    @Test
    public void SameSubnetTest() {
        Pair<Boolean, Pair<String, String>> sameSubnet =
            configurationMgr.validateIpRange("10.147.33.104", "10.147.33.105", "10.147.33.1", "255.255.255.128", vlanVOList, true, false, null, null, null, null, network);
        Assert.assertTrue(sameSubnet.first());
    }

    @Test
    public void NewSubnetTest() {
        Pair<Boolean, Pair<String, String>> sameSubnet =
            configurationMgr.validateIpRange("10.147.33.140", "10.147.33.145", "10.147.33.130", "255.255.255.192", vlanVOList, true, false, null, null, null, null,
                network);
        Assert.assertTrue(!sameSubnet.first());
    }

    @Test
    public void SuperSetTest() {
        try {
            configurationMgr.validateIpRange("10.147.33.10", "10.147.33.20", "10.147.33.21", "255.255.255.0", vlanVOList, true, false, null, null, null, null, network);
        } catch (Exception e) {
            junit.framework.Assert.assertTrue(e.getMessage().contains("superset"));
        }
    }

}
