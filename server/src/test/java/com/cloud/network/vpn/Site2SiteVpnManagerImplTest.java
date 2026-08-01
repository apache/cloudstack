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
package com.cloud.network.vpn;

import java.util.List;
import java.util.UUID;

import org.apache.cloudstack.acl.SecurityChecker;
import org.apache.cloudstack.api.command.user.vpn.CreateVpnGatewayCmd;
import org.apache.cloudstack.context.CallContext;
import org.junit.After;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.MockitoJUnitRunner;

import com.cloud.exception.InvalidParameterValueException;
import com.cloud.network.Network;
import com.cloud.network.Site2SiteVpnGateway;
import com.cloud.network.dao.IPAddressDao;
import com.cloud.network.dao.IPAddressVO;
import com.cloud.network.dao.Site2SiteVpnGatewayDao;
import com.cloud.network.dao.Site2SiteVpnGatewayVO;
import com.cloud.network.vpc.VpcManager;
import com.cloud.network.vpc.VpcOfferingServiceMapVO;
import com.cloud.network.vpc.VpcVO;
import com.cloud.network.vpc.dao.VpcDao;
import com.cloud.network.vpc.dao.VpcOfferingServiceMapDao;
import com.cloud.user.Account;
import com.cloud.user.AccountManager;
import com.cloud.user.AccountVO;
import com.cloud.user.User;
import com.cloud.user.UserVO;

import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertThrows;
import static org.junit.Assert.assertTrue;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyBoolean;
import static org.mockito.ArgumentMatchers.anyLong;
import static org.mockito.ArgumentMatchers.nullable;
import static org.mockito.Mockito.doNothing;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.mockito.Mockito.when;

@RunWith(MockitoJUnitRunner.Silent.class)
public class Site2SiteVpnManagerImplTest {

    private static final long ACCOUNT_ID = 1L;
    private static final long DOMAIN_ID = 2L;
    private static final long VPC_ID = 3L;
    private static final long IP_ADDRESS_ID = 4L;
    private static final long VPC_OFFERING_ID = 5L;

    @Mock
    private Site2SiteVpnGatewayDao vpnGatewayDao;
    @Mock
    private VpcDao vpcDao;
    @Mock
    private IPAddressDao ipAddressDao;
    @Mock
    private AccountManager accountManager;
    @Mock
    private VpcOfferingServiceMapDao vpcOfferingServiceMapDao;
    @Mock
    private VpcManager vpcManager;
    @InjectMocks
    private Site2SiteVpnManagerImpl manager;

    private AccountVO account;
    private VpcVO vpc;
    private IPAddressVO ipAddress;

    @Before
    public void setUp() {
        account = new AccountVO("test-account", DOMAIN_ID, "network-domain", Account.Type.NORMAL, UUID.randomUUID().toString());
        account.setId(ACCOUNT_ID);
        UserVO user = new UserVO(1, "test-user", "password", "first", "last", "test@example.invalid", "UTC",
                UUID.randomUUID().toString(), User.Source.UNKNOWN);
        CallContext.register(user, account);

        vpc = mock(VpcVO.class);
        when(vpc.getVpcOfferingId()).thenReturn(VPC_OFFERING_ID);

        ipAddress = mock(IPAddressVO.class);
        when(ipAddress.getId()).thenReturn(IP_ADDRESS_ID);

        when(accountManager.getAccount(ACCOUNT_ID)).thenReturn(account);
        doNothing().when(accountManager).checkAccess(any(Account.class), nullable(SecurityChecker.AccessType.class), anyBoolean(), any());
    }

    @After
    public void tearDown() {
        CallContext.unregister();
    }

    @Test
    public void testCreateVpnGatewayRejectsVpcWithoutVirtualRouterVpnProviderBeforeIpLookup() {
        CreateVpnGatewayCmd command = createCommand();
        when(vpcDao.findById(VPC_ID)).thenReturn(vpc);
        when(vpcManager.isProviderSupportServiceInVpc(VPC_ID, Network.Service.Vpn,
                Network.Provider.VPCVirtualRouter)).thenReturn(false);

        InvalidParameterValueException exception = assertThrows(InvalidParameterValueException.class,
                () -> manager.createVpnGateway(command));

        assertTrue(exception.getMessage().contains("does not support Site-to-Site VPN"));
        verify(vpcManager).isProviderSupportServiceInVpc(VPC_ID, Network.Service.Vpn,
                Network.Provider.VPCVirtualRouter);
        verifyNoInteractions(ipAddressDao);
        verify(vpnGatewayDao, never()).persist(any(Site2SiteVpnGatewayVO.class));
    }

    @Test
    public void testCreateVpnGatewayUsesSourceNatWhenVirtualRouterProvidesVpnAndSourceNat() {
        CreateVpnGatewayCmd command = createCommand();
        when(vpcDao.findById(VPC_ID)).thenReturn(vpc);
        when(vpcManager.isProviderSupportServiceInVpc(VPC_ID, Network.Service.Vpn,
                Network.Provider.VPCVirtualRouter)).thenReturn(true);
        when(vpcOfferingServiceMapDao.findByServiceProviderAndOfferingId(Network.Service.SourceNat.getName(),
                Network.Provider.VPCVirtualRouter.getName(), VPC_OFFERING_ID)).thenReturn(mock(VpcOfferingServiceMapVO.class));
        when(vpcOfferingServiceMapDao.findByServiceProviderAndOfferingId(Network.Service.Vpn.getName(),
                Network.Provider.VPCVirtualRouter.getName(), VPC_OFFERING_ID)).thenReturn(mock(VpcOfferingServiceMapVO.class));
        when(ipAddressDao.listByAssociatedVpc(VPC_ID, true)).thenReturn(List.of(ipAddress));

        Site2SiteVpnGateway result = manager.createVpnGateway(command);

        assertNotNull(result);
        verify(ipAddressDao).listByAssociatedVpc(VPC_ID, true);
        verify(vpcManager, never()).getIpAddressForVpcVr(any(), any(), anyBoolean());
        verify(vpnGatewayDao).persist(any(Site2SiteVpnGatewayVO.class));
    }

    @Test
    public void testCreateVpnGatewayUsesRouterIpWhenVirtualRouterProvidesVpnWithoutSourceNat() {
        CreateVpnGatewayCmd command = createCommand();
        when(vpcDao.findById(VPC_ID)).thenReturn(vpc);
        when(vpcManager.isProviderSupportServiceInVpc(VPC_ID, Network.Service.Vpn,
                Network.Provider.VPCVirtualRouter)).thenReturn(true);
        when(vpcOfferingServiceMapDao.findByServiceProviderAndOfferingId(Network.Service.Vpn.getName(),
                Network.Provider.VPCVirtualRouter.getName(), VPC_OFFERING_ID)).thenReturn(mock(VpcOfferingServiceMapVO.class));
        when(vpcManager.getIpAddressForVpcVr(vpc, null, true)).thenReturn(ipAddress);
        when(vpcManager.configStaticNatForVpcVr(vpc, ipAddress)).thenReturn(true);

        Site2SiteVpnGateway result = manager.createVpnGateway(command);

        assertNotNull(result);
        verify(vpcManager).getIpAddressForVpcVr(vpc, null, true);
        verify(vpcManager).configStaticNatForVpcVr(vpc, ipAddress);
        verify(ipAddressDao, never()).listByAssociatedVpc(anyLong(), anyBoolean());
        verify(vpnGatewayDao).persist(any(Site2SiteVpnGatewayVO.class));
    }

    private CreateVpnGatewayCmd createCommand() {
        CreateVpnGatewayCmd command = mock(CreateVpnGatewayCmd.class);
        when(command.getVpcId()).thenReturn(VPC_ID);
        when(command.getEntityOwnerId()).thenReturn(ACCOUNT_ID);
        return command;
    }
}
