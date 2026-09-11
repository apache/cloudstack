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

import static org.mockito.Mockito.when;

import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.Mockito;
import org.mockito.junit.MockitoJUnitRunner;

import com.cloud.dc.Vlan.VlanType;
import com.cloud.exception.InsufficientAddressCapacityException;
import com.cloud.network.dao.IPAddressDao;
import com.cloud.user.Account;

@RunWith(MockitoJUnitRunner.class)
public class IpAddressManagerImplSourceNatTest {

    @Mock
    private IPAddressDao _ipAddressDao;

    @InjectMocks
    private IpAddressManagerImpl ipAddressManagerImpl = new IpAddressManagerImpl();

    @Test(expected = InsufficientAddressCapacityException.class)
    public void assignSourceNatPublicIpAddressThrowsWhenRequestedIpNotFound() throws Exception {
        // findByIpAndDcId returns null when the requested IP is not a known
        // public IP in the zone. The method must throw a capacity exception
        // rather than dereference the null IPAddressVO with getState().
        long dcId = 1L;
        long networkId = 2L;
        String requestedIp = "10.1.1.1";
        Account owner = Mockito.mock(Account.class);

        when(_ipAddressDao.findByIpAndNetworkIdAndDcId(networkId, dcId, requestedIp)).thenReturn(null);
        when(_ipAddressDao.findByIpAndDcId(dcId, requestedIp)).thenReturn(null);

        ipAddressManagerImpl.assignSourceNatPublicIpAddress(dcId, null, owner, VlanType.VirtualNetwork,
                networkId, requestedIp, false, false);
    }
}
