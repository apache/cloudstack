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

import java.lang.reflect.Method;
import java.util.Collections;

import org.junit.Assert;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.Mockito;
import org.mockito.Spy;
import org.mockito.junit.MockitoJUnitRunner;

import com.cloud.dc.Vlan.VlanType;
import com.cloud.network.IpAddress.State;
import com.cloud.network.addr.PublicIp;
import com.cloud.network.dao.IPAddressDao;
import com.cloud.network.dao.IPAddressVO;
import com.cloud.user.Account;

@RunWith(MockitoJUnitRunner.class)
public class IpAddressManagerImplTest {

    @Mock
    IPAddressDao ipAddressDao;

    @Spy
    @InjectMocks
    IpAddressManagerImpl ipAddressManager = new IpAddressManagerImpl();

    private Method assignIpAddressWithLock;

    @Before
    public void setUp() throws Exception {
        assignIpAddressWithLock = IpAddressManagerImpl.class.getDeclaredMethod("assignIpAddressWithLock", IPAddressVO.class);
        assignIpAddressWithLock.setAccessible(true);
    }

    private IPAddressVO invoke(IPAddressVO candidate) throws Exception {
        return (IPAddressVO) assignIpAddressWithLock.invoke(ipAddressManager, candidate);
    }

    @Test
    public void testAssignAllocatesWhenRowLockedReadIsFree() throws Exception {
        IPAddressVO candidate = Mockito.mock(IPAddressVO.class);
        Mockito.when(candidate.getId()).thenReturn(2L);
        IPAddressVO lockedRow = Mockito.mock(IPAddressVO.class);
        Mockito.when(lockedRow.getState()).thenReturn(State.Free);
        // the fix must re-read the row under a FOR UPDATE lock, not a plain read
        Mockito.when(ipAddressDao.lockRow(2L, true)).thenReturn(lockedRow);
        Mockito.when(ipAddressDao.update(Mockito.eq(2L), Mockito.eq(candidate))).thenReturn(true);

        IPAddressVO result = invoke(candidate);

        Assert.assertSame(candidate, result);
        Mockito.verify(ipAddressDao).lockRow(2L, true);
        Mockito.verify(candidate).setState(State.Allocating);
        Mockito.verify(ipAddressDao).update(2L, candidate);
    }

    @Test
    public void testAssignReturnsNullWhenRowLockedReadIsNotFree() throws Exception {
        IPAddressVO candidate = Mockito.mock(IPAddressVO.class);
        Mockito.when(candidate.getId()).thenReturn(2L);
        IPAddressVO lockedRow = Mockito.mock(IPAddressVO.class);
        // the winning thread already flipped it; the loser must see the committed state and back off
        Mockito.when(lockedRow.getState()).thenReturn(State.Allocating);
        Mockito.when(ipAddressDao.lockRow(2L, true)).thenReturn(lockedRow);

        IPAddressVO result = invoke(candidate);

        Assert.assertNull(result);
        Mockito.verify(ipAddressDao).lockRow(2L, true);
        Mockito.verify(candidate, Mockito.never()).setState(State.Allocating);
        Mockito.verify(ipAddressDao, Mockito.never()).update(Mockito.anyLong(), Mockito.any(IPAddressVO.class));
    }

    @Test
    public void testAssignReturnsNullWhenRowIsGone() throws Exception {
        IPAddressVO candidate = Mockito.mock(IPAddressVO.class);
        Mockito.when(candidate.getId()).thenReturn(2L);
        Mockito.when(ipAddressDao.lockRow(2L, true)).thenReturn(null);

        IPAddressVO result = invoke(candidate);

        Assert.assertNull(result);
        Mockito.verify(ipAddressDao, Mockito.never()).update(Mockito.anyLong(), Mockito.any(IPAddressVO.class));
    }

    @Test
    public void testFetchNewPublicIpRetriesWhenTheFirstPickIsTakenConcurrently() throws Exception {
        IPAddressVO taken = Mockito.mock(IPAddressVO.class);     // first pick, lost to a concurrent allocation
        IPAddressVO allocated = Mockito.mock(IPAddressVO.class); // the free ip the retry succeeds with
        PublicIp expected = Mockito.mock(PublicIp.class);

        // each selection returns a single candidate (listAvailablePublicIps is called with lockOneRow=true)
        Mockito.doReturn(Collections.singletonList(taken)).when(ipAddressManager).listAvailablePublicIps(
                Mockito.anyLong(), Mockito.any(), Mockito.any(), Mockito.any(), Mockito.any(), Mockito.any(),
                Mockito.anyBoolean(), Mockito.anyBoolean(), Mockito.anyBoolean(), Mockito.any(), Mockito.any(),
                Mockito.anyBoolean(), Mockito.any(), Mockito.any(), Mockito.anyBoolean(), Mockito.anyBoolean());
        // the first allocation loses the race (null), the retry gets another ip
        Mockito.doReturn(null).doReturn(allocated).when(ipAddressManager).assignAndAllocateIpAddressEntry(
                Mockito.any(), Mockito.any(), Mockito.any(), Mockito.anyBoolean(), Mockito.anyBoolean(),
                Mockito.anyBoolean(), Mockito.any(), Mockito.any(), Mockito.anyList());
        Mockito.doReturn(expected).when(ipAddressManager).buildPublicIp(allocated);

        PublicIp result = ipAddressManager.fetchNewPublicIp(1L, null, null, Mockito.mock(Account.class),
                VlanType.DirectAttached, null, false, true, true, null, null, true, null, null, false);

        // the losing pick did not fail the call; it retried and allocated a different free ip
        Assert.assertSame(expected, result);
        Mockito.verify(ipAddressManager, Mockito.times(2)).assignAndAllocateIpAddressEntry(
                Mockito.any(), Mockito.any(), Mockito.any(), Mockito.anyBoolean(), Mockito.anyBoolean(),
                Mockito.anyBoolean(), Mockito.any(), Mockito.any(), Mockito.anyList());
    }
}
