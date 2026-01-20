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
package org.apache.cloudstack.service;

import com.cloud.dc.DataCenterVO;
import com.cloud.dc.dao.DataCenterDao;
import com.cloud.exception.InvalidParameterValueException;
import com.cloud.host.dao.HostDetailsDao;
import com.cloud.network.Network;
import com.cloud.network.Networks;
import com.cloud.network.dao.NetrisProviderDao;
import com.cloud.network.dao.NetworkDao;
import com.cloud.network.dao.NetworkVO;
import com.cloud.network.dao.PhysicalNetworkDao;
import com.cloud.network.dao.PhysicalNetworkVO;
import com.cloud.network.element.NetrisProviderVO;
import com.cloud.network.netris.NetrisService;
import com.cloud.resource.ResourceManager;
import com.cloud.utils.db.Transaction;
import com.cloud.utils.exception.CloudRuntimeException;
import org.apache.cloudstack.api.BaseResponse;
import org.apache.cloudstack.api.response.NetrisProviderResponse;
import org.junit.After;
import org.junit.Assert;
import org.junit.Before;
import org.junit.Test;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.MockedStatic;
import org.mockito.Mockito;
import org.mockito.MockitoAnnotations;

import java.util.Arrays;
import java.util.List;

public class NetrisProviderServiceImplTest {

    @Mock
    private DataCenterDao dataCenterDao;
    @Mock
    private ResourceManager resourceManager;
    @Mock
    private NetrisProviderDao netrisProviderDao;
    @Mock
    private HostDetailsDao hostDetailsDao;
    @Mock
    private PhysicalNetworkDao physicalNetworkDao;
    @Mock
    private NetworkDao networkDao;
    @Mock
    private NetrisService netrisService;

    @InjectMocks
    private NetrisProviderServiceImpl netrisProviderService;

    private AutoCloseable closeable;
    private MockedStatic<Transaction> transactionMockedStatic;

    private static final long ZONE_ID = 1L;
    private static final String NAME = "test-provider";
    private static final String HOSTNAME = "test-host";
    private static final String PORT = "8080";
    private static final String USERNAME = "test-user";
    private static final String PASSWORD = "test-password";
    private static final String SITE_NAME = "test-site";
    private static final String TENANT_NAME = "test-tenant";
    private static final String NETRIS_TAG = "test-tag";
    private static final long HOST_ID = 2L;
    private static final long PROVIDER_ID = 3L;

    @Before
    public void setup() {
        closeable = MockitoAnnotations.openMocks(this);
        transactionMockedStatic = Mockito.mockStatic(Transaction.class);
    }

    @After
    public void tearDown() throws Exception {
        closeable.close();
        transactionMockedStatic.close();
    }

    @Test
    public void testListNetrisProvidersWithZoneId() {
        // Setup
        NetrisProviderVO providerVO = Mockito.mock(NetrisProviderVO.class);
        Mockito.when(netrisProviderDao.findByZoneId(ZONE_ID)).thenReturn(providerVO);
        Mockito.when(providerVO.getZoneId()).thenReturn(ZONE_ID);

        DataCenterVO zone = Mockito.mock(DataCenterVO.class);
        Mockito.when(dataCenterDao.findById(ZONE_ID)).thenReturn(zone);
        Mockito.when(zone.getName()).thenReturn("test-zone");

        // Execute
        List<BaseResponse> result = netrisProviderService.listNetrisProviders(ZONE_ID);

        // Verify
        Assert.assertEquals(1, result.size());
        Assert.assertTrue(result.get(0) instanceof NetrisProviderResponse);
    }

    @Test
    public void testListNetrisProvidersWithoutZoneId() {
        // Setup
        NetrisProviderVO providerVO = Mockito.mock(NetrisProviderVO.class);
        Mockito.when(netrisProviderDao.listAll()).thenReturn(Arrays.asList(providerVO));

        DataCenterVO zone = Mockito.mock(DataCenterVO.class);
        Mockito.when(dataCenterDao.findById(Mockito.anyLong())).thenReturn(zone);
        Mockito.when(zone.getName()).thenReturn("test-zone");

        // Execute
        List<BaseResponse> result = netrisProviderService.listNetrisProviders(null);

        // Verify
        Assert.assertEquals(1, result.size());
        Assert.assertTrue(result.get(0) instanceof NetrisProviderResponse);
    }

    @Test
    public void testDeleteNetrisProviderSuccess() {
        // Setup
        NetrisProviderVO providerVO = Mockito.mock(NetrisProviderVO.class);
        Mockito.when(providerVO.getZoneId()).thenReturn(ZONE_ID);
        Mockito.when(netrisProviderDao.findById(PROVIDER_ID)).thenReturn(providerVO);

        PhysicalNetworkVO physicalNetwork = Mockito.mock(PhysicalNetworkVO.class);
        Mockito.when(physicalNetworkDao.listByZone(ZONE_ID)).thenReturn(Arrays.asList(physicalNetwork));

        NetworkVO network = Mockito.mock(NetworkVO.class);
        Mockito.when(networkDao.listByPhysicalNetwork(Mockito.anyLong())).thenReturn(Arrays.asList(network));
        Mockito.when(network.getBroadcastDomainType()).thenReturn(Networks.BroadcastDomainType.Netris);
        Mockito.when(network.getState()).thenReturn(Network.State.Shutdown);

        // Execute
        boolean result = netrisProviderService.deleteNetrisProvider(PROVIDER_ID);

        // Verify
        Assert.assertTrue(result);
        Mockito.verify(netrisProviderDao).remove(PROVIDER_ID);
    }

    @Test(expected = InvalidParameterValueException.class)
    public void testDeleteNetrisProviderNotFound() {
        // Setup
        Mockito.when(netrisProviderDao.findById(PROVIDER_ID)).thenReturn(null);

        // Execute
        netrisProviderService.deleteNetrisProvider(PROVIDER_ID);
    }

    @Test(expected = CloudRuntimeException.class)
    public void testDeleteNetrisProviderWithActiveNetworks() {
        // Setup
        NetrisProviderVO providerVO = Mockito.mock(NetrisProviderVO.class);
        Mockito.when(providerVO.getZoneId()).thenReturn(ZONE_ID);
        Mockito.when(netrisProviderDao.findById(PROVIDER_ID)).thenReturn(providerVO);

        PhysicalNetworkVO physicalNetwork = Mockito.mock(PhysicalNetworkVO.class);
        Mockito.when(physicalNetworkDao.listByZone(ZONE_ID)).thenReturn(Arrays.asList(physicalNetwork));

        NetworkVO network = Mockito.mock(NetworkVO.class);
        Mockito.when(networkDao.listByPhysicalNetwork(Mockito.anyLong())).thenReturn(Arrays.asList(network));
        Mockito.when(network.getBroadcastDomainType()).thenReturn(Networks.BroadcastDomainType.Netris);
        Mockito.when(network.getState()).thenReturn(Network.State.Implemented);

        // Execute
        netrisProviderService.deleteNetrisProvider(PROVIDER_ID);
    }

    @Test
    public void testCreateNetrisProviderResponse() {
        // Setup
        NetrisProviderVO provider = Mockito.mock(NetrisProviderVO.class);
        Mockito.when(provider.getZoneId()).thenReturn(ZONE_ID);
        Mockito.when(provider.getName()).thenReturn(NAME);
        Mockito.when(provider.getSiteName()).thenReturn(SITE_NAME);
        Mockito.when(provider.getTenantName()).thenReturn(TENANT_NAME);
        Mockito.when(provider.getNetrisTag()).thenReturn(NETRIS_TAG);

        DataCenterVO zone = Mockito.mock(DataCenterVO.class);
        Mockito.when(dataCenterDao.findById(ZONE_ID)).thenReturn(zone);
        Mockito.when(zone.getName()).thenReturn("test-zone");

        // Execute
        NetrisProviderResponse response = netrisProviderService.createNetrisProviderResponse(provider);

        // Verify
        Assert.assertNotNull(response);
        Assert.assertEquals(NAME, response.getName());
        Assert.assertEquals(SITE_NAME, response.getSiteName());
        Assert.assertEquals(TENANT_NAME, response.getTenantName());
        Assert.assertEquals(NETRIS_TAG, response.getNetrisTag());
    }

    @Test(expected = CloudRuntimeException.class)
    public void testCreateNetrisProviderResponseZoneNotFound() {
        // Setup
        NetrisProviderVO provider = Mockito.mock(NetrisProviderVO.class);
        Mockito.when(provider.getZoneId()).thenReturn(ZONE_ID);
        Mockito.when(dataCenterDao.findById(ZONE_ID)).thenReturn(null);

        // Execute
        netrisProviderService.createNetrisProviderResponse(provider);
    }
}
