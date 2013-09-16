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
package org.apache.cloudstack.network.element;

import java.util.Arrays;
import java.util.HashMap;

import org.apache.cloudstack.engine.orchestration.service.NetworkOrchestrationService;
import org.apache.cloudstack.framework.config.dao.ConfigurationDao;
import org.apache.cloudstack.network.dao.SspCredentialDao;
import org.apache.cloudstack.network.dao.SspCredentialVO;
import org.apache.cloudstack.network.dao.SspTenantDao;
import org.apache.cloudstack.network.dao.SspUuidDao;
import org.apache.cloudstack.network.element.SspElement;

import org.junit.Before;
import org.junit.Test;

import com.cloud.dc.dao.DataCenterDao;
import com.cloud.host.Host;
import com.cloud.host.HostVO;
import com.cloud.host.dao.HostDao;
import com.cloud.network.Network;
import com.cloud.network.NetworkModel;
import com.cloud.network.PhysicalNetworkServiceProvider;
import com.cloud.network.dao.NetworkServiceMapDao;
import com.cloud.network.dao.PhysicalNetworkDao;
import com.cloud.network.dao.PhysicalNetworkServiceProviderDao;
import com.cloud.network.dao.PhysicalNetworkServiceProviderVO;
import com.cloud.network.dao.PhysicalNetworkVO;
import com.cloud.resource.ResourceManager;
import com.cloud.vm.dao.NicDao;

import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertTrue;
import static org.mockito.Mockito.*;

public class SspElementTest {
    SspElement _element = new SspElement();

    @Before
    public void setUp(){
        _element._configDao = mock(ConfigurationDao.class);
        _element._dcDao = mock(DataCenterDao.class);
        _element._hostDao = mock(HostDao.class);
        _element._networkMgr = mock(NetworkOrchestrationService.class);
        _element._networkModel = mock(NetworkModel.class);
        _element._nicDao = mock(NicDao.class);
        _element._physicalNetworkDao = mock(PhysicalNetworkDao.class);
        _element._physicalNetworkServiceProviderDao = mock(PhysicalNetworkServiceProviderDao.class);
        _element._resourceMgr = mock(ResourceManager.class);
        _element._ntwkSrvcDao = mock(NetworkServiceMapDao.class);

        _element._sspCredentialDao = mock(SspCredentialDao.class);
        _element._sspTenantDao = mock(SspTenantDao.class);
        _element._sspUuidDao = mock(SspUuidDao.class);
    }

    Long dataCenterId = new Long(21);
    Long physicalNetworkId = new Long(22);
    Long networkId = new Long(23);
    PhysicalNetworkVO psvo = mock(PhysicalNetworkVO.class);
    PhysicalNetworkServiceProviderVO nspvo = mock(PhysicalNetworkServiceProviderVO.class);
    SspCredentialVO credential = mock(SspCredentialVO.class);
    HostVO host = mock(HostVO.class);

    public void fullyConfigured(){
        // when physicalNetworkServiceProvider is configured
        when(psvo.getId()).thenReturn(physicalNetworkId);
        when(psvo.getDataCenterId()).thenReturn(dataCenterId);

        when(_element._physicalNetworkDao.findById(physicalNetworkId)).thenReturn(psvo);

        when(nspvo.getState()).thenReturn(PhysicalNetworkServiceProvider.State.Enabled);
        when(nspvo.getPhysicalNetworkId()).thenReturn(physicalNetworkId);

        when(_element._physicalNetworkServiceProviderDao.findByServiceProvider(physicalNetworkId, "StratosphereSsp")).thenReturn(nspvo);

        // and zone api server, credentail is configured
        when(credential.getUsername()).thenReturn("foo");
        when(credential.getPassword()).thenReturn("bar");

        when(_element._sspCredentialDao.findByZone(dataCenterId.longValue())).thenReturn(credential);

        HashMap<String,String> details = new HashMap<String, String>();
        details.put("sspHost", "v1Api");
        details.put("url", "http://a.example.jp/");

        when(host.getDataCenterId()).thenReturn(dataCenterId);
        when(host.getDetails()).thenReturn(details);
        when(host.getDetail("sspHost")).thenReturn(details.get("sspHost"));
        when(host.getDetail("url")).thenReturn(details.get("url"));

        when(_element._resourceMgr.listAllHostsInOneZoneByType(Host.Type.L2Networking, dataCenterId)).thenReturn(Arrays.<HostVO>asList(host));

        when(_element._ntwkSrvcDao.canProviderSupportServiceInNetwork(networkId, Network.Service.Connectivity, _element.getProvider())).thenReturn(true);
    }

    @Test
    public void isReadyTest(){
        fullyConfigured();

        // isReady is called in changing the networkserviceprovider state to Enabled.
        when(nspvo.getState()).thenReturn(PhysicalNetworkServiceProvider.State.Disabled);

        // ssp is ready
        assertTrue(_element.isReady(nspvo));

        // If you don't call addstratospheressp api, ssp won't be ready
        when(_element._sspCredentialDao.findByZone(dataCenterId.longValue())).thenReturn(null);
        when(_element._resourceMgr.listAllHostsInOneZoneByType(Host.Type.L2Networking, dataCenterId)).thenReturn(Arrays.<HostVO>asList());
        assertFalse(_element.isReady(nspvo));
    }

    @Test
    public void canHandleTest() {
        fullyConfigured();

        // ssp is active
        assertTrue(_element.canHandle(psvo));

        // You can disable ssp temporary by truning the state disabled
        when(nspvo.getState()).thenReturn(PhysicalNetworkServiceProvider.State.Disabled);
        assertFalse(_element.canHandle(psvo));

        // If you don't want ssp for a specific physicalnetwork, you don't need to
        // setup physicalNetworkProvider.
        when(_element._physicalNetworkServiceProviderDao.findByServiceProvider(physicalNetworkId, "StratosphereSsp")).thenReturn(null);
        assertFalse(_element.canHandle(psvo));

        // restore...
        when(nspvo.getState()).thenReturn(PhysicalNetworkServiceProvider.State.Enabled);
        when(_element._physicalNetworkServiceProviderDao.findByServiceProvider(physicalNetworkId, "StratosphereSsp")).thenReturn(nspvo);

        // If you don't call addstratospheressp api, ssp won't be active
        when(_element._sspCredentialDao.findByZone(dataCenterId.longValue())).thenReturn(null);
        when(_element._resourceMgr.listAllHostsInOneZoneByType(Host.Type.L2Networking, dataCenterId)).thenReturn(Arrays.<HostVO>asList());
        assertFalse(_element.canHandle(psvo));
    }
}
