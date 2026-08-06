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

import com.cloud.network.IpAddress;
import com.cloud.network.Network;
import com.cloud.network.dao.FirewallRulesDao;
import com.cloud.network.dao.IPAddressDao;
import com.cloud.network.dao.IPAddressVO;
import com.cloud.network.dao.LoadBalancerDao;
import com.cloud.network.nsx.NsxVpnGatewayResult;
import com.cloud.network.rules.dao.PortForwardingRulesDao;
import com.cloud.network.vpc.VpcManager;
import com.cloud.network.vpc.VpcVO;
import com.cloud.utils.exception.CloudRuntimeException;
import com.cloud.utils.net.Ip;
import org.apache.cloudstack.resourcedetail.UserIpAddressDetailVO;
import org.apache.cloudstack.resourcedetail.dao.UserIpAddressDetailsDao;
import org.junit.Assert;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.Mock;
import org.mockito.junit.MockitoJUnitRunner;

import java.util.List;

import static org.mockito.ArgumentMatchers.anyBoolean;
import static org.mockito.ArgumentMatchers.anyLong;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@RunWith(MockitoJUnitRunner.class)
public class NsxVpnGatewayOwnershipTest {

    private static final long VPC_ID = 9L;
    private static final long IP_ADDRESS_ID = 20L;
    private static final String IP_ADDRESS = "203.0.113.20";

    @Mock
    private NsxServiceImpl nsxService;
    @Mock
    private VpcManager vpcManager;
    @Mock
    private IPAddressDao ipAddressDao;
    @Mock
    private UserIpAddressDetailsDao userIpAddressDetailsDao;
    @Mock
    private FirewallRulesDao firewallRulesDao;
    @Mock
    private PortForwardingRulesDao portForwardingRulesDao;
    @Mock
    private LoadBalancerDao loadBalancerDao;
    @Mock
    private VpcVO vpc;
    @Mock
    private IpAddress requestedIp;
    @Mock
    private IPAddressVO ipAddress;

    private NsxElement element;

    @Before
    public void setUp() {
        element = new NsxElement();
        element.nsxService = nsxService;
        element.vpcManager = vpcManager;
        element.ipAddressDao = ipAddressDao;
        element.userIpAddressDetailsDao = userIpAddressDetailsDao;
        element.firewallRulesDao = firewallRulesDao;
        element.portForwardingRulesDao = portForwardingRulesDao;
        element.loadBalancerDao = loadBalancerDao;

        when(vpc.getId()).thenReturn(VPC_ID);
        when(vpc.getName()).thenReturn("vpc");
        when(vpcManager.isProviderSupportServiceInVpc(VPC_ID, Network.Service.Vpn, Network.Provider.Nsx))
                .thenReturn(true);
        when(requestedIp.getId()).thenReturn(IP_ADDRESS_ID);
        when(ipAddressDao.findById(IP_ADDRESS_ID)).thenReturn(ipAddress);
        when(ipAddress.getId()).thenReturn(IP_ADDRESS_ID);
        when(ipAddress.getVpcId()).thenReturn(VPC_ID);
        when(ipAddress.readyToUse()).thenReturn(true);
        when(ipAddress.getAddress()).thenReturn(new Ip(IP_ADDRESS));
        when(firewallRulesDao.listByIpAndNotRevoked(IP_ADDRESS_ID)).thenReturn(List.of());
        when(portForwardingRulesDao.listByIpAndNotRevoked(IP_ADDRESS_ID)).thenReturn(List.of());
        when(loadBalancerDao.listByIpAddress(IP_ADDRESS_ID)).thenReturn(List.of());
    }

    @Test
    public void testExistingOwnershipMarkerIsNotOverwrittenOrRemovedOnFailure() {
        UserIpAddressDetailVO existingDetail = new UserIpAddressDetailVO(
                IP_ADDRESS_ID, NsxElement.NSX_VPN_GATEWAY_IP_DETAIL, "true", false);
        when(userIpAddressDetailsDao.findDetail(IP_ADDRESS_ID, NsxElement.NSX_VPN_GATEWAY_IP_DETAIL))
                .thenReturn(existingDetail);
        when(nsxService.createVpnGateway(vpc, IP_ADDRESS)).thenReturn(new NsxVpnGatewayResult(false, false));

        Assert.assertThrows(CloudRuntimeException.class,
                () -> element.acquireVpnGatewayIp(vpc, requestedIp));

        verify(userIpAddressDetailsDao, never()).addDetail(anyLong(), anyString(), anyString(), anyBoolean());
        verify(userIpAddressDetailsDao, never()).removeDetail(IP_ADDRESS_ID, NsxElement.NSX_VPN_GATEWAY_IP_DETAIL);
    }

    @Test
    public void testTemporaryOwnershipMarkerIsRemovedAfterUnambiguousFailure() {
        when(userIpAddressDetailsDao.findDetail(IP_ADDRESS_ID, NsxElement.NSX_VPN_GATEWAY_IP_DETAIL))
                .thenReturn(null);
        when(nsxService.createVpnGateway(vpc, IP_ADDRESS)).thenReturn(new NsxVpnGatewayResult(false, false));

        Assert.assertThrows(CloudRuntimeException.class,
                () -> element.acquireVpnGatewayIp(vpc, requestedIp));

        verify(userIpAddressDetailsDao).addDetail(
                IP_ADDRESS_ID, NsxElement.NSX_VPN_GATEWAY_IP_DETAIL, "false", false);
        verify(userIpAddressDetailsDao).removeDetail(IP_ADDRESS_ID, NsxElement.NSX_VPN_GATEWAY_IP_DETAIL);
    }

    @Test
    public void testTemporaryOwnershipMarkerIsRetainedAfterAmbiguousFailure() {
        when(userIpAddressDetailsDao.findDetail(IP_ADDRESS_ID, NsxElement.NSX_VPN_GATEWAY_IP_DETAIL))
                .thenReturn(null);
        when(nsxService.createVpnGateway(vpc, IP_ADDRESS)).thenReturn(new NsxVpnGatewayResult(false, true));

        Assert.assertThrows(CloudRuntimeException.class,
                () -> element.acquireVpnGatewayIp(vpc, requestedIp));

        verify(userIpAddressDetailsDao).addDetail(
                IP_ADDRESS_ID, NsxElement.NSX_VPN_GATEWAY_IP_DETAIL, "false", false);
        verify(userIpAddressDetailsDao, never()).removeDetail(IP_ADDRESS_ID, NsxElement.NSX_VPN_GATEWAY_IP_DETAIL);
    }
}
