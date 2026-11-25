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

package com.cloud.network.router;

import java.util.Collections;
import java.util.HashMap;

import org.apache.cloudstack.framework.config.ConfigKey;
import org.apache.cloudstack.framework.config.impl.ConfigDepotImpl;
import org.apache.cloudstack.network.BgpPeer;
import org.apache.cloudstack.network.dao.BgpPeerDetailsDao;
import org.junit.After;
import org.junit.Assert;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.ArgumentMatchers;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.Mockito;
import org.mockito.junit.MockitoJUnitRunner;
import org.springframework.test.util.ReflectionTestUtils;

import com.cloud.agent.api.Command;
import com.cloud.agent.api.routing.SetBgpPeersCommand;
import com.cloud.agent.manager.Commands;
import com.cloud.dc.ASNumberVO;
import com.cloud.dc.DataCenter;
import com.cloud.dc.DataCenterVO;
import com.cloud.dc.dao.ASNumberDao;
import com.cloud.dc.dao.DataCenterDao;
import com.cloud.network.Network;
import com.cloud.network.dao.NetworkDao;
import com.cloud.network.dao.NetworkVO;
import com.cloud.offering.NetworkOffering;
import com.cloud.offerings.NetworkOfferingVO;
import com.cloud.offerings.dao.NetworkOfferingDao;

/**
 * The BGP peers command carries the minimum hold time to the router, so a network or
 * VPC with BGP peers gets bgp minimum-holdtime. The value is the setting
 * vpc.gateway.bgp.min.holdtime (BGPService.MinHoldtime).
 */
@RunWith(MockitoJUnitRunner.class)
public class CommandSetupHelperBgpPeersMinHoldtimeTest {

    private static final long ROUTER_ID = 14L;
    private static final long ZONE_ID = 1L;
    private static final long ACCOUNT_ID = 2L;
    private static final long VPC_ID = 30L;
    private static final long TIER_ID = 31L;
    private static final long OFFERING_ID = 32L;
    private static final long PEER_ID = 40L;
    private static final String MIN_HOLDTIME = "vpc.gateway.bgp.min.holdtime";

    @InjectMocks
    protected CommandSetupHelper commandSetupHelper = new CommandSetupHelper();
    @Mock
    ASNumberDao asNumberDao;
    @Mock
    NetworkDao networkDao;
    @Mock
    NetworkOfferingDao networkOfferingDao;
    @Mock
    BgpPeerDetailsDao bgpPeerDetailsDao;
    @Mock
    RouterControlHelper routerControlHelper;
    @Mock
    DataCenterDao dcDao;
    @Mock
    ConfigDepotImpl configDepot;
    @Mock
    VirtualRouter router;
    @Mock
    NetworkVO tier;
    @Mock
    NetworkOfferingVO tierOffering;
    @Mock
    ASNumberVO asNumber;
    @Mock
    BgpPeer operatorPeer;
    @Mock
    Network network;

    private Object previousDepot;

    @Before
    public void setUp() {
        ReflectionTestUtils.setField(commandSetupHelper, "asNumberDao", asNumberDao);
        ReflectionTestUtils.setField(commandSetupHelper, "_networkDao", networkDao);
        ReflectionTestUtils.setField(commandSetupHelper, "_networkOfferingDao", networkOfferingDao);
        ReflectionTestUtils.setField(commandSetupHelper, "bgpPeerDetailsDao", bgpPeerDetailsDao);
        ReflectionTestUtils.setField(commandSetupHelper, "_routerControlHelper", routerControlHelper);
        ReflectionTestUtils.setField(commandSetupHelper, "_dcDao", dcDao);
        previousDepot = ReflectionTestUtils.getField(ConfigKey.class, "s_depot");
        ConfigKey.init(configDepot);

        DataCenterVO dc = new DataCenterVO(ZONE_ID, null, null, null, null, null, null, null, null, null, DataCenter.NetworkType.Advanced, null, null);

        Mockito.lenient().when(router.getId()).thenReturn(ROUTER_ID);
        Mockito.lenient().when(router.getDataCenterId()).thenReturn(ZONE_ID);
        Mockito.lenient().when(router.getAccountId()).thenReturn(ACCOUNT_ID);
        Mockito.lenient().when(router.getVpcId()).thenReturn(VPC_ID);
        Mockito.lenient().when(router.getInstanceName()).thenReturn("r-14-VM");
        Mockito.lenient().when(asNumber.getAsNumber()).thenReturn(64600L);
        Mockito.lenient().when(asNumberDao.findByZoneAndVpcId(ZONE_ID, VPC_ID)).thenReturn(asNumber);
        Mockito.lenient().when(tier.getId()).thenReturn(TIER_ID);
        Mockito.lenient().when(tier.getCidr()).thenReturn("172.16.10.0/24");
        Mockito.lenient().when(tier.getNetworkOfferingId()).thenReturn(OFFERING_ID);
        Mockito.lenient().when(networkDao.listByVpc(VPC_ID)).thenReturn(Collections.singletonList(tier));
        Mockito.lenient().when(tierOffering.getRoutingMode()).thenReturn(NetworkOffering.RoutingMode.Dynamic);
        Mockito.lenient().when(networkOfferingDao.findByIdIncludingRemoved(OFFERING_ID)).thenReturn(tierOffering);
        Mockito.lenient().when(operatorPeer.getId()).thenReturn(PEER_ID);
        Mockito.lenient().when(operatorPeer.getIp4Address()).thenReturn("198.51.100.1");
        Mockito.lenient().when(operatorPeer.getAsNumber()).thenReturn(65100L);
        Mockito.lenient().when(bgpPeerDetailsDao.getBgpPeerDetails(PEER_ID)).thenReturn(new HashMap<>());
        Mockito.lenient().when(routerControlHelper.getRouterControlIp(ROUTER_ID)).thenReturn("192.0.2.10");
        Mockito.lenient().when(dcDao.findById(ZONE_ID)).thenReturn(dc);
    }

    @After
    public void tearDown() {
        ConfigKey.init((ConfigDepotImpl) previousDepot);
    }

    private void setting(String key, String value) {
        Mockito.lenient().when(configDepot.getConfigStringValue(ArgumentMatchers.eq(key), ArgumentMatchers.any(ConfigKey.Scope.class),
                ArgumentMatchers.nullable(Long.class))).thenReturn(value);
    }

    private void settingIn(String key, ConfigKey.Scope scope, Long scopeId, String value) {
        Mockito.lenient().when(configDepot.getConfigStringValue(ArgumentMatchers.eq(key), ArgumentMatchers.eq(scope),
                scopeId == null ? ArgumentMatchers.isNull() : ArgumentMatchers.eq(scopeId))).thenReturn(value);
    }

    private SetBgpPeersCommand sent() {
        Commands cmds = new Commands(Command.OnError.Stop);
        commandSetupHelper.createBgpPeersCommands(Collections.singletonList(operatorPeer), router, cmds, network);
        Assert.assertEquals(1, cmds.size());
        Command cmd = cmds.toCommands()[0];
        Assert.assertTrue(cmd instanceof SetBgpPeersCommand);
        return (SetBgpPeersCommand) cmd;
    }

    // The default minimum hold time is 3 s.
    @Test
    public void operatorBgpCommandCarriesDefaultMinHoldtime() {
        Assert.assertEquals(Integer.valueOf(3), sent().getMinHoldtime());
    }

    // A raised minimum hold time reaches the command as set.
    @Test
    public void operatorBgpCommandCarriesRaisedMinHoldtime() {
        setting(MIN_HOLDTIME, "9");
        Assert.assertEquals(Integer.valueOf(9), sent().getMinHoldtime());
    }

    // The value of the router's account is used when the account has one.
    @Test
    public void operatorBgpCommandUsesAccountMinHoldtime() {
        settingIn(MIN_HOLDTIME, ConfigKey.Scope.Global, null, "5");
        settingIn(MIN_HOLDTIME, ConfigKey.Scope.Account, ACCOUNT_ID, "9");
        Assert.assertEquals(Integer.valueOf(9), sent().getMinHoldtime());
    }

    // An account without its own value uses the global value.
    @Test
    public void operatorBgpCommandFallsBackToGlobalMinHoldtime() {
        settingIn(MIN_HOLDTIME, ConfigKey.Scope.Global, null, "5");
        Assert.assertEquals(Integer.valueOf(5), sent().getMinHoldtime());
    }
}
