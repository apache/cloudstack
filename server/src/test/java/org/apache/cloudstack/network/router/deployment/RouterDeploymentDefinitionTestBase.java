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
package org.apache.cloudstack.network.router.deployment;

import static org.mockito.Mockito.lenient;
import static org.mockito.Mockito.when;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.MockitoJUnitRunner;

import com.cloud.dc.DataCenter;
import com.cloud.dc.HostPodVO;
import com.cloud.dc.Pod;
import com.cloud.dc.dao.HostPodDao;
import com.cloud.deploy.DeployDestination;
import com.cloud.network.IpAddressManager;
import com.cloud.network.NetworkModel;
import com.cloud.network.dao.NetworkDao;
import com.cloud.network.dao.NetworkDetailsDao;
import com.cloud.network.dao.NetworkVO;
import com.cloud.network.dao.PhysicalNetworkServiceProviderDao;
import com.cloud.network.dao.VirtualRouterProviderDao;
import com.cloud.network.router.NetworkHelper;
import com.cloud.network.router.VpcNetworkHelperImpl;
import com.cloud.offerings.NetworkOfferingVO;
import com.cloud.offerings.dao.NetworkOfferingDao;
import com.cloud.service.ServiceOfferingVO;
import com.cloud.service.dao.ServiceOfferingDao;
import com.cloud.user.Account;
import com.cloud.user.AccountManager;
import com.cloud.vm.VirtualMachineProfile.Param;
import com.cloud.vm.dao.DomainRouterDao;
import com.cloud.vm.dao.VMInstanceDao;

@RunWith(MockitoJUnitRunner.class)
public class RouterDeploymentDefinitionTestBase {

    protected static final String LOCK_NOT_CORRECTLY_GOT = "Lock not correctly got";
    protected static final String NUMBER_OF_ROUTERS_TO_DEPLOY_IS_NOT_THE_EXPECTED = "Number of routers to deploy is not the expected";
    protected static final String ONLY_THE_PROVIDED_AS_DEFAULT_DESTINATION_WAS_EXPECTED = "Only the provided as default destination was expected";

    protected static final long OFFERING_ID = 16L;
    protected static final long DEFAULT_OFFERING_ID = 17L;
    protected static final Long DATA_CENTER_ID = 100l;
    protected static final Long NW_ID_1 = 101l;
    protected static final Long NW_ID_2= 102l;
    protected static final Long POD_ID1 = 111l;
    protected static final Long POD_ID2 = 112l;
    protected static final Long POD_ID3 = 113l;
    protected static final Long ROUTER1_ID = 121l;
    protected static final Long ROUTER2_ID = 122l;
    protected static final long PROVIDER_ID = 131L;
    protected static final long PHYSICAL_NW_ID = 141L;

    // General delegates (Daos, Mgrs...)
    @Mock
    protected NetworkDao mockNwDao;
    @Mock
    protected DomainRouterDao mockRouterDao;
    @Mock
    protected NetworkHelper mockNetworkHelper;
    @Mock
    protected NetworkDetailsDao networkDetailsDao;
    @Mock
    protected VpcNetworkHelperImpl vpcNwHelper;
    @Mock
    protected VMInstanceDao mockVmDao;
    @Mock
    protected HostPodDao mockPodDao;
    @Mock
    protected VirtualRouterProviderDao mockVrProviderDao;
    @Mock
    protected PhysicalNetworkServiceProviderDao physicalProviderDao;
    @Mock
    protected NetworkModel mockNetworkModel;
    @Mock
    protected IpAddressManager mockIpAddrMgr;
    @Mock
    protected NetworkOfferingDao mockNetworkOfferingDao;
    @Mock
    protected ServiceOfferingDao mockServiceOfferingDao;
    @Mock
    protected AccountManager mockAccountMgr;

    // Instance specific parameters to use during build
    @Mock
    protected DeployDestination mockDestination;
    @Mock
    protected DataCenter mockDataCenter;
    @Mock
    protected Pod mockPod;
    @Mock
    protected HostPodVO mockHostPodVO1;
    @Mock
    protected HostPodVO mockHostPodVO2;
    @Mock
    protected HostPodVO mockHostPodVO3;
    @Mock
    protected NetworkVO mockNw;
    @Mock
    NetworkOfferingVO mockNwOfferingVO;
    @Mock
    ServiceOfferingVO mockSvcOfferingVO;
    @Mock
    protected Account mockOwner;


    protected List<HostPodVO> mockPods = new ArrayList<>();
    protected Map<Param, Object> params = new HashMap<>();

    @InjectMocks
    protected RouterDeploymentDefinitionBuilder builder = new RouterDeploymentDefinitionBuilder();


    protected void initMocks() {
        when(mockDestination.getDataCenter()).thenReturn(mockDataCenter);
        lenient().when(mockDataCenter.getId()).thenReturn(DATA_CENTER_ID);
        lenient().when(mockPod.getId()).thenReturn(POD_ID1);
        lenient().when(mockHostPodVO1.getId()).thenReturn(POD_ID1);
        lenient().when(mockHostPodVO2.getId()).thenReturn(POD_ID2);
        lenient().when(mockHostPodVO3.getId()).thenReturn(POD_ID3);
        lenient().when(mockNw.getId()).thenReturn(NW_ID_1);
    }

    @Test
    public void mockTest() {
        return;
    }

}
