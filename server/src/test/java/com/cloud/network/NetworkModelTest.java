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

import static org.mockito.Matchers.any;
import static org.mockito.Matchers.anyLong;
import static org.mockito.Mockito.doReturn;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;
import static org.mockito.Mockito.spy;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;

import junit.framework.Assert;

import org.junit.Before;
import org.junit.Test;

import com.cloud.dc.VlanVO;
import com.cloud.dc.dao.VlanDao;
import com.cloud.exception.UnsupportedServiceException;
import com.cloud.network.dao.IPAddressDao;
import com.cloud.network.dao.IPAddressVO;
import com.cloud.network.element.NetworkElement;
import com.cloud.user.Account;
import com.cloud.utils.db.Filter;
import com.cloud.utils.db.SearchBuilder;
import com.cloud.utils.db.SearchCriteria;
import com.cloud.utils.net.Ip;
import com.cloud.network.Network.Provider;

public class NetworkModelTest {
    @Before
    public void setUp() {

    }

    @Test
    public void testGetSourceNatIpAddressForGuestNetwork() {
        NetworkModelImpl modelImpl = new NetworkModelImpl();
        IPAddressDao ipAddressDao = mock(IPAddressDao.class);
        modelImpl._ipAddressDao = ipAddressDao;
        List<IPAddressVO> fakeList = new ArrayList<IPAddressVO>();
        IPAddressVO fakeIp = new IPAddressVO(new Ip("75.75.75.75"), 1, 0xaabbccddeeffL, 10, false);
        fakeList.add(fakeIp);
        SearchBuilder<IPAddressVO> fakeSearch = mock(SearchBuilder.class);
        modelImpl.IpAddressSearch = fakeSearch;
        VlanDao fakeVlanDao = mock(VlanDao.class);
        when(fakeVlanDao.findById(anyLong())).thenReturn(mock(VlanVO.class));
        modelImpl._vlanDao = fakeVlanDao;
        when(fakeSearch.create()).thenReturn(mock(SearchCriteria.class));
        when(ipAddressDao.search(any(SearchCriteria.class), (Filter)org.mockito.Matchers.isNull())).thenReturn(fakeList);
        when(ipAddressDao.findById(anyLong())).thenReturn(fakeIp);
        Account fakeAccount = mock(Account.class);
        when(fakeAccount.getId()).thenReturn(1L);
        Network fakeNetwork = mock(Network.class);
        when(fakeNetwork.getId()).thenReturn(1L);
        PublicIpAddress answer = modelImpl.getSourceNatIpAddressForGuestNetwork(fakeAccount, fakeNetwork);
        Assert.assertNull(answer);
        IPAddressVO fakeIp2 = new IPAddressVO(new Ip("76.75.75.75"), 1, 0xaabb10ddeeffL, 10, true);
        fakeList.add(fakeIp2);
        when(ipAddressDao.findById(anyLong())).thenReturn(fakeIp2);
        answer = modelImpl.getSourceNatIpAddressForGuestNetwork(fakeAccount, fakeNetwork);
        Assert.assertNotNull(answer);
        Assert.assertEquals(answer.getAddress().addr(), "76.75.75.75");

    }

    @Test
    public void testCapabilityForProvider() {
        NetworkModelImpl modelImpl = spy(NetworkModelImpl.class);
        Set<Provider> providers = new HashSet<>();
        providers.add(Provider.NuageVsp);
        NetworkElement nuageVspElement = mock(NetworkElement.class);
        HashMap<Network.Service, Map<Network.Capability, String>> nuageVspCap = new HashMap<Network.Service, Map<Network.Capability, String>>();
        HashMap<Network.Capability, String> nuageVspConnectivity = new HashMap<Network.Capability, String>();
        nuageVspConnectivity.put(Network.Capability.NoVlan, "FindMe");
        nuageVspConnectivity.put(Network.Capability.PublicAccess, "");

        nuageVspCap.put(Network.Service.Connectivity, nuageVspConnectivity);
        when(nuageVspElement.getName()).thenReturn("NuageVsp");
        doReturn(nuageVspCap).when(nuageVspElement).getCapabilities();
        doReturn(nuageVspElement).when(modelImpl).getElementImplementingProvider("NuageVsp");

        try {
            modelImpl.checkCapabilityForProvider(providers, Network.Service.UserData, null, null);
            Assert.fail();
        } catch (UnsupportedServiceException e) {
            Assert.assertEquals(e.getMessage(), "Service " + Network.Service.UserData.getName() + " is not supported by the element=NuageVsp implementing Provider=" + Provider.NuageVsp.getName());
        }

        try {
            modelImpl.checkCapabilityForProvider(providers, Network.Service.Connectivity, Network.Capability.ElasticIp, null);
            Assert.fail();
        } catch (UnsupportedServiceException e) {
            Assert.assertEquals(e.getMessage(), "Service " + Network.Service.Connectivity.getName() + " doesn't have capability " + Network.Capability.ElasticIp.getName() + " for element=NuageVsp implementing Provider=" + Provider.NuageVsp.getName());
        }
        try {
            modelImpl.checkCapabilityForProvider(providers, Network.Service.Connectivity, Network.Capability.PublicAccess, "NonExistingVal");
            Assert.fail();
        } catch (UnsupportedServiceException e){
            Assert.assertEquals(e.getMessage(),"Service Connectivity doesn't have capability PublicAccess for element=NuageVsp implementing Provider=NuageVsp");
        }

        modelImpl.checkCapabilityForProvider(providers, Network.Service.Connectivity, Network.Capability.NoVlan, "FindMe");

        NetworkElement nuageVspElement2 = mock(NetworkElement.class);
        doReturn(null).when(nuageVspElement).getCapabilities();
        try {
            modelImpl.checkCapabilityForProvider(providers, Network.Service.Connectivity, Network.Capability.PublicAccess, "");
            Assert.fail();
        } catch (UnsupportedServiceException e) {
            Assert.assertEquals(e.getMessage(), "Service Connectivity is not supported by the element=NuageVsp implementing Provider=NuageVsp");
        }
    }


}
