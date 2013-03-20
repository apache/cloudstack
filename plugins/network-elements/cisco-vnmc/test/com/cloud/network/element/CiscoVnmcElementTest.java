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
package com.cloud.network.element;

import java.util.Collections;
import java.util.List;

import javax.naming.ConfigurationException;

import org.junit.Before;
import org.junit.Test;
import org.mockito.internal.matchers.Any;

import com.cloud.api.response.CiscoVnmcResourceResponse;
import com.cloud.configuration.ConfigurationManager;
import com.cloud.dc.DataCenter;
import com.cloud.deploy.DeployDestination;
import com.cloud.domain.Domain;
import com.cloud.exception.ConcurrentOperationException;
import com.cloud.exception.InsufficientCapacityException;
import com.cloud.exception.ResourceUnavailableException;
import com.cloud.network.Network;
import com.cloud.network.Network.GuestType;
import com.cloud.network.Network.Provider;
import com.cloud.network.Network.Service;
import com.cloud.network.NetworkManager;
import com.cloud.network.NetworkModel;
import com.cloud.network.Networks.BroadcastDomainType;
import com.cloud.network.Networks.TrafficType;
import com.cloud.network.cisco.CiscoVnmcControllerVO;
import com.cloud.network.dao.CiscoVnmcDao;
import com.cloud.network.dao.CiscoVnmcDaoImpl;
import com.cloud.network.dao.NetworkServiceMapDao;
import com.cloud.offering.NetworkOffering;
import com.cloud.resource.ResourceManager;
import com.cloud.user.Account;
import com.cloud.vm.ReservationContext;

import static org.junit.Assert.*;
import static org.mockito.Mockito.*;

public class CiscoVnmcElementTest {

    CiscoVnmcElement _element = new CiscoVnmcElement();
    NetworkManager _networkMgr = mock(NetworkManager.class);
    NetworkModel _networkModel = mock(NetworkModel.class);
    NetworkServiceMapDao _ntwkSrvcDao = mock(NetworkServiceMapDao.class);
    ConfigurationManager _configMgr = mock(ConfigurationManager.class);
    CiscoVnmcDao _ciscoVnmcDao = mock(CiscoVnmcDao.class);

    @Before
    public void setUp() throws ConfigurationException {
        _element._resourceMgr = mock(ResourceManager.class);
        _element._networkMgr = _networkMgr;
        _element._configMgr = _configMgr;
        _element._ciscoVnmcDao = _ciscoVnmcDao;

        // Standard responses
        when(_networkModel.isProviderForNetwork(Provider.CiscoVnmc, 1L)).thenReturn(true);

        _element.configure("CiscoVnmcTestElement", Collections.<String, Object> emptyMap());
    }

    @Test
    public void canHandleTest() {
        Network network = mock(Network.class);
        when(network.getId()).thenReturn(1L);
        when(network.getBroadcastDomainType()).thenReturn(BroadcastDomainType.Vlan);
        assertTrue(_element.canHandle(network));

        when(network.getBroadcastDomainType()).thenReturn(BroadcastDomainType.UnDecided);
        assertFalse(_element.canHandle(network));
    }

    @Test
    public void implementTest() throws ConcurrentOperationException, ResourceUnavailableException, InsufficientCapacityException {
        Network network = mock(Network.class);
        when(network.getId()).thenReturn(1L);
        when(network.getBroadcastDomainType()).thenReturn(BroadcastDomainType.Vlan);
        when(network.getDataCenterId()).thenReturn(1L);

        NetworkOffering offering = mock(NetworkOffering.class);
        when(offering.getId()).thenReturn(1L);
        when(offering.getTrafficType()).thenReturn(TrafficType.Guest);
        when(offering.getGuestType()).thenReturn(GuestType.Isolated);

        DeployDestination dest = mock(DeployDestination.class);

        Domain dom = mock(Domain.class);
        when(dom.getName()).thenReturn("domain");
        Account acc = mock(Account.class);
        when(acc.getAccountName()).thenReturn("accountname");
        ReservationContext context = mock(ReservationContext.class);
        when(context.getDomain()).thenReturn(dom);
        when(context.getAccount()).thenReturn(acc);

        DataCenter dc = mock(DataCenter.class);
        when(_configMgr.getZone(network.getDataCenterId())).thenReturn(dc);

        @SuppressWarnings("unchecked")
        List<CiscoVnmcControllerVO> devices = mock(List.class);
        when(devices.isEmpty()).thenReturn(false);
        when(_ciscoVnmcDao.listByPhysicalNetwork(network.getId())).thenReturn(devices);
        //assertTrue(_element.implement(network, offering, dest, context));
    }

}
