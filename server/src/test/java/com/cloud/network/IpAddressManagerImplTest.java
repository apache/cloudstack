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

import org.junit.Assert;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.Mockito;
import org.mockito.junit.MockitoJUnitRunner;

import com.cloud.network.IpAddress.State;
import com.cloud.network.dao.IPAddressDao;
import com.cloud.network.dao.IPAddressVO;

@RunWith(MockitoJUnitRunner.class)
public class IpAddressManagerImplTest {

    @Mock
    IPAddressDao ipAddressDao;

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
}
