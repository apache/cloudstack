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

import static org.junit.Assert.fail;
import static org.mockito.ArgumentMatchers.nullable;
import static org.mockito.Matchers.anyBoolean;
import static org.mockito.Matchers.anyLong;
import static org.mockito.Matchers.anyString;
import static org.mockito.Matchers.eq;
import static org.mockito.Mockito.when;

import java.util.ArrayList;
import java.util.List;
import java.util.UUID;

import org.apache.cloudstack.acl.ControlledEntity.ACLType;
import org.apache.cloudstack.engine.orchestration.service.NetworkOrchestrationService;
import org.apache.log4j.Logger;
import org.junit.Before;
import org.junit.Test;
import org.mockito.Mock;
import org.mockito.MockitoAnnotations;

import com.cloud.dc.DataCenter.NetworkType;
import com.cloud.dc.DataCenterVO;
import com.cloud.dc.dao.DataCenterDao;
import com.cloud.exception.ConcurrentOperationException;
import com.cloud.exception.InsufficientCapacityException;
import com.cloud.exception.InvalidParameterValueException;
import com.cloud.exception.ResourceAllocationException;
import com.cloud.network.Network.GuestType;
import com.cloud.network.Networks.BroadcastDomainType;
import com.cloud.network.Networks.Mode;
import com.cloud.network.Networks.TrafficType;
import com.cloud.network.dao.NetworkDao;
import com.cloud.network.dao.NetworkVO;
import com.cloud.network.dao.PhysicalNetworkDao;
import com.cloud.network.dao.PhysicalNetworkVO;
import com.cloud.network.vpc.dao.PrivateIpDao;
import com.cloud.offerings.NetworkOfferingVO;
import com.cloud.offerings.dao.NetworkOfferingDao;
import com.cloud.user.Account;
import com.cloud.user.AccountManager;
import com.cloud.user.AccountVO;
import com.cloud.utils.db.DB;
import com.cloud.utils.db.TransactionLegacy;
import com.cloud.utils.exception.CloudRuntimeException;

import junit.framework.Assert;

//@Ignore("Requires database to be set up")
public class CreatePrivateNetworkTest {

    private static final Logger s_logger = Logger.getLogger(CreatePrivateNetworkTest.class);

    NetworkServiceImpl networkService = new NetworkServiceImpl();

    @Mock
    AccountManager _accountMgr;
    @Mock
    NetworkOfferingDao _networkOfferingDao;
    @Mock
    PhysicalNetworkDao _physicalNetworkDao;
    @Mock
    DataCenterDao _dcDao;
    @Mock
    NetworkDao _networkDao;
    @Mock
    NetworkOrchestrationService _networkMgr;
    @Mock
    PrivateIpDao _privateIpDao;

    @Before
    public void setup() throws Exception {
        MockitoAnnotations.initMocks(this);

        networkService._accountMgr = _accountMgr;
        networkService._networkOfferingDao = _networkOfferingDao;
        networkService._physicalNetworkDao = _physicalNetworkDao;
        networkService._dcDao = _dcDao;
        networkService._networksDao = _networkDao;
        networkService._networkMgr = _networkMgr;
        networkService._privateIpDao = _privateIpDao;

        Account account = new AccountVO("testaccount", 1, "networkdomain", (short)0, UUID.randomUUID().toString());
        when(networkService._accountMgr.getAccount(anyLong())).thenReturn(account);

        NetworkOfferingVO ntwkOff =
            new NetworkOfferingVO("offer", "fakeOffer", TrafficType.Guest, true, true, null, null, false, null, null, GuestType.Isolated, false, false, false, false,
                false, false, false, false, false, false, false, false, false, false, false, false);

        when(networkService._networkOfferingDao.findById(anyLong())).thenReturn(ntwkOff);
        List<NetworkOfferingVO> netofferlist = new ArrayList<NetworkOfferingVO>();
        netofferlist.add(ntwkOff);
        when(networkService._networkOfferingDao.listSystemNetworkOfferings()).thenReturn(netofferlist);

        PhysicalNetworkVO physicalNetwork = new PhysicalNetworkVO(1L, 1L, "2-5", "200", 1L, null, "testphysicalnetwork");
        when(networkService._physicalNetworkDao.findById(anyLong())).thenReturn(physicalNetwork);

        DataCenterVO dc = new DataCenterVO(1L, "hut", "op de hei", null, null, null, null, "10.1.1.0/24", "unreal.net", 1L, NetworkType.Advanced, null, null);
        when(networkService._dcDao.lockRow(anyLong(), anyBoolean())).thenReturn(dc);

        when(networkService._networksDao.getPrivateNetwork(anyString(), anyString(), eq(1L), eq(1L), anyLong(), anyLong())).thenReturn(null);

        Network net =
            new NetworkVO(1L, TrafficType.Guest, Mode.None, BroadcastDomainType.Vlan, 1L, 1L, 1L, 1L, "bla", "fake", "eet.net", GuestType.Isolated, 1L, 1L,
                ACLType.Account, false, 1L, false);
        when(networkService._networkMgr.createGuestNetwork(eq(ntwkOff.getId()), eq("bla"), eq("fake"), eq("10.1.1.1"), eq("10.1.1.0/24"), nullable(String.class), nullable(Boolean.class), nullable(String.class),
                        eq(account), nullable(Long.class), eq(physicalNetwork), eq(physicalNetwork.getDataCenterId()), eq(ACLType.Account), nullable(Boolean.class), eq(1L), nullable(String.class), nullable(String.class),
                        nullable(Boolean.class), nullable(String.class), nullable(Network.PVlanType.class), nullable(String.class), nullable(String.class), nullable(String.class))).thenReturn(net);
        when(
            networkService._networkMgr.createPrivateNetwork(eq(ntwkOff.getId()), eq("bla"), eq("fake"), eq("10.1.1.1"), eq("10.1.1.0/24"), anyString(), anyBoolean(), eq(account), eq(physicalNetwork), eq(1L))).thenReturn(net);

        when(networkService._privateIpDao.findByIpAndSourceNetworkId(net.getId(), "10.1.1.2")).thenReturn(null);
        when(networkService._privateIpDao.findByIpAndSourceNetworkIdAndVpcId(eq(1L), anyString(), eq(1L))).thenReturn(null);
    }

    @Test
    @DB
    public void createInvalidlyHostedPrivateNetwork() {
        TransactionLegacy __txn;
        __txn = TransactionLegacy.open("createInvalidlyHostedPrivateNetworkTest");
        /* Network nw; */
        try {
            /* nw = */
            networkService.createPrivateNetwork("bla", "fake", 1L, "vlan:1", "10.1.1.2", null, "10.1.1.1", "255.255.255.0", 1L, 1L, true, 1L, false);
            /* nw = */
            networkService.createPrivateNetwork("bla", "fake", 1L, "lswitch:3", "10.1.1.2", null, "10.1.1.1", "255.255.255.0", 1L, 1L, false, 1L, false);
            boolean invalid = false;
            boolean unsupported = false;
            try {
                /* nw = */
                networkService.createPrivateNetwork("bla", "fake", 1, "bla:2", "10.1.1.2", null, "10.1.1.1", "255.255.255.0", 1, 1L, true, 1L, false);
            } catch (CloudRuntimeException e) {
                Assert.assertEquals("unexpected parameter exception", "string 'bla:2' has an unknown BroadcastDomainType.", e.getMessage());
                invalid = true;
            }
            try {
                /* nw = */
                networkService.createPrivateNetwork("bla", "fake", 1, "mido://4", "10.1.1.2", null, "10.1.1.1", "255.255.255.0", 1, 1L, false, 1L, false);
            } catch (InvalidParameterValueException e) {
                Assert.assertEquals("unexpected parameter exception", "unsupported type of broadcastUri specified: mido://4", e.getMessage());
                unsupported = true;
            }
            Assert.assertEquals("'bla' should not be accepted as scheme", true, invalid);
            Assert.assertEquals("'mido' should not yet be supported as scheme", true, unsupported);
        } catch (ResourceAllocationException e) {
            s_logger.error("no resources", e);
            fail("no resources");
        } catch (ConcurrentOperationException e) {
            s_logger.error("another one is in the way", e);
            fail("another one is in the way");
        } catch (InsufficientCapacityException e) {
            s_logger.error("no capacity", e);
            fail("no capacity");
        } finally {
            __txn.close("createInvalidlyHostedPrivateNetworkTest");
        }
    }

}
