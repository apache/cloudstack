//
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
//

package com.cloud.network.bigswitch;

import static org.junit.Assert.assertEquals;

import org.junit.After;
import org.junit.Before;
import org.junit.Test;
import org.mockito.Mock;
import org.mockito.MockitoAnnotations;

import com.cloud.agent.AgentManager;
import com.cloud.dc.dao.VlanDao;
import com.cloud.host.dao.HostDao;
import com.cloud.network.NetworkModel;
import com.cloud.network.dao.BigSwitchBcfDao;
import com.cloud.network.dao.FirewallRulesCidrsDao;
import com.cloud.network.dao.FirewallRulesDao;
import com.cloud.network.dao.IPAddressDao;
import com.cloud.network.dao.NetworkDao;
import com.cloud.network.vpc.NetworkACLItemCidrsDao;
import com.cloud.network.vpc.NetworkACLItemDao;
import com.cloud.network.vpc.dao.VpcDao;
import com.cloud.vm.dao.NicDao;
import com.cloud.vm.dao.VMInstanceDao;

public class BigSwitchBcfUtilsTest {

    @Mock
    NetworkDao networkDao;
    @Mock
    NicDao nicDao;
    @Mock
    VMInstanceDao vmDao;
    @Mock
    HostDao hostDao;
    @Mock
    VpcDao vpcDao;
    @Mock
    BigSwitchBcfDao bigswitchBcfDao;
    @Mock
    AgentManager agentMgr;
    @Mock
    VlanDao vlanDao;
    @Mock
    IPAddressDao ipAddressDao;
    @Mock
    FirewallRulesDao fwRulesDao;
    @Mock
    FirewallRulesCidrsDao fwCidrsDao;
    @Mock
    NetworkACLItemDao aclItemDao;
    @Mock
    NetworkACLItemCidrsDao aclItemCidrsDao;
    @Mock
    NetworkModel networkModel;
    @Mock
    BigSwitchBcfUtils bsUtil;
    private AutoCloseable closeable;

    @Before
    public void setUp() {
        closeable = MockitoAnnotations.openMocks(this);
        bsUtil = new BigSwitchBcfUtils(networkDao, nicDao, vmDao, hostDao,
                vpcDao, bigswitchBcfDao, agentMgr, vlanDao, ipAddressDao,
                fwRulesDao, fwCidrsDao, aclItemDao, aclItemCidrsDao,
                networkModel);
    }

    @After
    public void tearDown() throws Exception {
        closeable.close();
    }

    @Test
    public void getSubnetMaskLengthTest() {
        Integer rc = bsUtil.getSubnetMaskLength("255.255.255.254");
        assertEquals("failed", new Integer(31), rc);
        rc = bsUtil.getSubnetMaskLength("128.255.255.254");
        assertEquals("failed", new Integer(1), rc);
    }
}
