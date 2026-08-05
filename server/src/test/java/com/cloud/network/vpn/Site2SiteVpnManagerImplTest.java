/*
 * Licensed to the Apache Software Foundation (ASF) under one
 * or more contributor license agreements.  See the NOTICE file
 * distributed with this work for additional information
 * regarding copyright ownership.  The ASF licenses this file
 * to you under the Apache License, Version 2.0 (the
 * "License"); you may not use this file except in compliance
 * with the License.  You may obtain a copy of the License at
 *
 *   http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing,
 * software distributed under the License is distributed on an
 * "AS IS" BASIS, WITHOUT WARRANTIES OR CONDITIONS OF ANY
 * KIND, either express or implied.  See the License for the
 * specific language governing permissions and limitations
 * under the License.
 */
package com.cloud.network.vpn;

import com.cloud.exception.InvalidParameterValueException;
import com.cloud.exception.PermissionDeniedException;
import com.cloud.exception.ResourceUnavailableException;
import com.cloud.network.Site2SiteVpnConnection;
import com.cloud.network.Site2SiteVpnConnection.State;
import com.cloud.network.Site2SiteVpnGateway;
import com.cloud.network.Network;
import com.cloud.network.dao.IPAddressDao;
import com.cloud.network.dao.IPAddressVO;
import com.cloud.network.dao.Site2SiteCustomerGatewayDao;
import com.cloud.network.dao.Site2SiteCustomerGatewayVO;
import com.cloud.network.dao.Site2SiteVpnConnectionDao;
import com.cloud.network.dao.Site2SiteVpnConnectionVO;
import com.cloud.network.dao.Site2SiteVpnGatewayDao;
import com.cloud.network.dao.Site2SiteVpnGatewayVO;
import com.cloud.network.element.Site2SiteVpnServiceProvider;
import com.cloud.network.element.NetworkElement;
import com.cloud.network.vpc.VpcManager;
import com.cloud.network.vpc.VpcVO;
import com.cloud.network.vpc.dao.VpcDao;
import com.cloud.network.vpc.dao.VpcOfferingServiceMapDao;
import com.cloud.user.Account;
import com.cloud.user.AccountManager;
import com.cloud.user.AccountVO;
import com.cloud.user.User;
import com.cloud.user.UserVO;
import com.cloud.utils.db.DB;
import com.cloud.utils.exception.CloudRuntimeException;
import com.cloud.utils.net.Ip;
import com.cloud.utils.net.NetUtils;
import com.cloud.vm.DomainRouterVO;
import org.apache.cloudstack.acl.SecurityChecker;
import org.apache.cloudstack.annotation.dao.AnnotationDao;
import org.apache.cloudstack.api.command.user.vpn.CreateVpnConnectionCmd;
import org.apache.cloudstack.api.command.user.vpn.CreateVpnCustomerGatewayCmd;
import org.apache.cloudstack.api.command.user.vpn.CreateVpnGatewayCmd;
import org.apache.cloudstack.api.command.user.vpn.DeleteVpnConnectionCmd;
import org.apache.cloudstack.api.command.user.vpn.DeleteVpnCustomerGatewayCmd;
import org.apache.cloudstack.api.command.user.vpn.DeleteVpnGatewayCmd;
import org.apache.cloudstack.api.command.user.vpn.ResetVpnConnectionCmd;
import org.apache.cloudstack.api.command.user.vpn.UpdateVpnCustomerGatewayCmd;
import org.apache.cloudstack.context.CallContext;
import org.apache.cloudstack.framework.config.ConfigKey;
import org.junit.After;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.InjectMocks;
import org.mockito.InOrder;
import org.mockito.Mock;
import org.mockito.MockedStatic;
import org.mockito.Mockito;
import org.mockito.junit.MockitoJUnitRunner;
import org.springframework.test.util.ReflectionTestUtils;

import java.lang.reflect.Field;
import java.util.ArrayList;
import java.util.List;
import java.util.UUID;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertTrue;
import static org.junit.Assert.assertThrows;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyBoolean;
import static org.mockito.ArgumentMatchers.anyLong;
import static org.mockito.ArgumentMatchers.nullable;
import static org.mockito.Mockito.doNothing;
import static org.mockito.Mockito.inOrder;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@RunWith(MockitoJUnitRunner.Silent.class)
public class Site2SiteVpnManagerImplTest {

    @Mock
    private Site2SiteCustomerGatewayDao _customerGatewayDao;
    @Mock
    private Site2SiteVpnGatewayDao _vpnGatewayDao;
    @Mock
    private Site2SiteVpnConnectionDao _vpnConnectionDao;
    @Mock
    private VpcDao _vpcDao;
    @Mock
    private IPAddressDao _ipAddressDao;
    @Mock
    private VpcManager _vpcMgr;
    @Mock
    private AccountManager _accountMgr;
    @Mock
    private AnnotationDao annotationDao;
    @Mock
    private List<Site2SiteVpnServiceProvider> _s2sProviders;
    @Mock
    VpcOfferingServiceMapDao vpcOfferingServiceMapDao;

    @InjectMocks
    private Site2SiteVpnManagerImpl site2SiteVpnManager;

    private AccountVO account;
    private UserVO user;
    private VpcVO vpc;
    private IPAddressVO ipAddress;
    private Site2SiteVpnGatewayVO vpnGateway;
    private Site2SiteCustomerGatewayVO customerGateway;
    private Site2SiteVpnConnectionVO vpnConnection;

    private static final Long ACCOUNT_ID = 1L;
    private static final Long DOMAIN_ID = 2L;
    private static final Long VPC_ID = 3L;
    private static final Long VPN_GATEWAY_ID = 4L;
    private static final Long CUSTOMER_GATEWAY_ID = 5L;
    private static final Long VPN_CONNECTION_ID = 6L;
    private static final Long IP_ADDRESS_ID = 7L;

    @Before
    public void setUp() throws Exception {
        account = new AccountVO("testaccount", DOMAIN_ID, "networkdomain", Account.Type.NORMAL, UUID.randomUUID().toString());
        account.setId(ACCOUNT_ID);
        user = new UserVO(1, "testuser", "password", "firstname", "lastName", "email", "timezone",
                UUID.randomUUID().toString(), User.Source.UNKNOWN);
        CallContext.register(user, account);

        vpc = mock(VpcVO.class);
        when(vpc.getId()).thenReturn(VPC_ID);
        when(vpc.getAccountId()).thenReturn(ACCOUNT_ID);
        when(vpc.getDomainId()).thenReturn(DOMAIN_ID);
        when(vpc.getCidr()).thenReturn("10.0.0.0/16");

        ipAddress = mock(IPAddressVO.class);
        when(ipAddress.getId()).thenReturn(IP_ADDRESS_ID);
        when(ipAddress.getVpcId()).thenReturn(VPC_ID);

        vpnGateway = mock(Site2SiteVpnGatewayVO.class);
        when(vpnGateway.getId()).thenReturn(VPN_GATEWAY_ID);
        when(vpnGateway.getVpcId()).thenReturn(VPC_ID);
        when(vpnGateway.getAccountId()).thenReturn(ACCOUNT_ID);
        when(vpnGateway.getDomainId()).thenReturn(DOMAIN_ID);

        customerGateway = mock(Site2SiteCustomerGatewayVO.class);
        when(customerGateway.getId()).thenReturn(CUSTOMER_GATEWAY_ID);
        when(customerGateway.getAccountId()).thenReturn(ACCOUNT_ID);
        when(customerGateway.getDomainId()).thenReturn(DOMAIN_ID);
        when(customerGateway.getGuestCidrList()).thenReturn("192.168.1.0/24");
        when(customerGateway.getIkePolicy()).thenReturn("aes128-sha256;modp2048");
        when(customerGateway.getEspPolicy()).thenReturn("aes128-sha256;modp2048");
        when(customerGateway.getIkeVersion()).thenReturn("ike");

        vpnConnection = new Site2SiteVpnConnectionVO(ACCOUNT_ID, DOMAIN_ID, VPN_GATEWAY_ID, CUSTOMER_GATEWAY_ID, false);
        ReflectionTestUtils.setField(vpnConnection, "id", VPN_CONNECTION_ID);
        vpnConnection.setState(State.Pending);

        when(_accountMgr.getAccount(ACCOUNT_ID)).thenReturn(account);
        doNothing().when(_accountMgr).checkAccess(any(Account.class), nullable(SecurityChecker.AccessType.class), anyBoolean(), any());
        when(_s2sProviders.iterator()).thenReturn(List.<Site2SiteVpnServiceProvider>of().iterator());
    }

    private Site2SiteVpnServiceProvider mockVpcVirtualRouterProvider() {
        Site2SiteVpnServiceProvider provider = mock(Site2SiteVpnServiceProvider.class,
                Mockito.withSettings().extraInterfaces(NetworkElement.class));
        when(((NetworkElement) provider).getProvider()).thenReturn(Network.Provider.VPCVirtualRouter);
        when(_vpcMgr.isProviderSupportServiceInVpc(VPC_ID, Network.Service.Vpn, Network.Provider.VPCVirtualRouter))
                .thenReturn(true);
        when(_s2sProviders.iterator()).thenAnswer(invocation -> List.of(provider).iterator());
        return provider;
    }

    private Site2SiteVpnServiceProvider mockNsxProvider() {
        Site2SiteVpnServiceProvider provider = mock(Site2SiteVpnServiceProvider.class,
                Mockito.withSettings().extraInterfaces(NetworkElement.class));
        when(((NetworkElement) provider).getProvider()).thenReturn(Network.Provider.Nsx);
        when(_vpcMgr.isProviderSupportServiceInVpc(VPC_ID, Network.Service.Vpn, Network.Provider.Nsx))
                .thenReturn(true);
        when(_s2sProviders.iterator()).thenAnswer(invocation -> List.of(provider).iterator());
        return provider;
    }

    @After
    public void tearDown() throws Exception {
        resetConfigKeyValue(Site2SiteVpnManagerImpl.VpnCustomerGatewayExcludedIkeVersions);
        resetConfigKeyValue(Site2SiteVpnManagerImpl.VpnCustomerGatewayExcludedEncryptionAlgorithms);
        resetConfigKeyValue(Site2SiteVpnManagerImpl.VpnCustomerGatewayExcludedHashingAlgorithms);
        resetConfigKeyValue(Site2SiteVpnManagerImpl.VpnCustomerGatewayExcludedDhGroup);
        resetConfigKeyValue(Site2SiteVpnManagerImpl.VpnCustomerGatewayObsoleteIkeVersions);
        resetConfigKeyValue(Site2SiteVpnManagerImpl.VpnCustomerGatewayObsoleteEncryptionAlgorithms);
        resetConfigKeyValue(Site2SiteVpnManagerImpl.VpnCustomerGatewayObsoleteHashingAlgorithms);
        resetConfigKeyValue(Site2SiteVpnManagerImpl.VpnCustomerGatewayObsoleteDhGroup);
        CallContext.unregister();
    }

    private void setConfigKeyValue(ConfigKey<String> configKey, String value) {
        try {
            Field valueField = ConfigKey.class.getDeclaredField("_value");
            valueField.setAccessible(true);
            valueField.set(configKey, value);

            Field dynamicField = ConfigKey.class.getDeclaredField("_isDynamic");
            dynamicField.setAccessible(true);
            dynamicField.setBoolean(configKey, false);
        } catch (IllegalAccessException | NoSuchFieldException e) {
            throw new RuntimeException("Failed to set ConfigKey value", e);
        }
    }

    private void resetConfigKeyValue(ConfigKey<String> configKey) {
        try {
            Field valueField = ConfigKey.class.getDeclaredField("_value");
            valueField.setAccessible(true);
            valueField.set(configKey, null);

            Field dynamicField = ConfigKey.class.getDeclaredField("_isDynamic");
            dynamicField.setAccessible(true);
            dynamicField.setBoolean(configKey, true);
        } catch (IllegalAccessException | NoSuchFieldException e) {
            throw new RuntimeException("Failed to reset ConfigKey value", e);
        }
    }

    @Test
    public void testCreateVpnGatewaySuccess() {
        CreateVpnGatewayCmd cmd = mock(CreateVpnGatewayCmd.class);
        when(cmd.getVpcId()).thenReturn(VPC_ID);
        when(cmd.getEntityOwnerId()).thenReturn(ACCOUNT_ID);
        when(cmd.isDisplay()).thenReturn(true);
        when(cmd.getIpAddressId()).thenReturn(null);

        when(_vpcDao.findById(VPC_ID)).thenReturn(vpc);
        when(_vpnGatewayDao.findByVpcId(VPC_ID)).thenReturn(null);
        mockVpcVirtualRouterProvider();
        when(_ipAddressDao.listByAssociatedVpc(VPC_ID, true)).thenReturn(List.of(ipAddress));
        when(_vpnGatewayDao.persist(any(Site2SiteVpnGatewayVO.class))).thenReturn(vpnGateway);

        Site2SiteVpnGateway result = site2SiteVpnManager.createVpnGateway(cmd);

        assertNotNull(result);
        verify(_vpnGatewayDao).persist(any(Site2SiteVpnGatewayVO.class));
    }

    @Test
    public void testCreateVpnGatewayAcceptsValidProviderOwnedIp() {
        CreateVpnGatewayCmd cmd = mock(CreateVpnGatewayCmd.class);
        when(cmd.getVpcId()).thenReturn(VPC_ID);
        when(cmd.getEntityOwnerId()).thenReturn(ACCOUNT_ID);
        when(cmd.getIpAddressId()).thenReturn(null);
        when(_vpcDao.findById(VPC_ID)).thenReturn(vpc);
        when(_vpnGatewayDao.findByVpcId(VPC_ID)).thenReturn(null);
        Site2SiteVpnServiceProvider provider = mockNsxProvider();
        when(provider.acquireVpnGatewayIp(vpc, null)).thenReturn(ipAddress);
        when(_ipAddressDao.findById(IP_ADDRESS_ID)).thenReturn(ipAddress);
        when(ipAddress.getAddress()).thenReturn(new Ip("203.0.113.10"));
        when(ipAddress.readyToUse()).thenReturn(true);
        when(_vpnGatewayDao.persist(any(Site2SiteVpnGatewayVO.class))).thenReturn(vpnGateway);

        Site2SiteVpnGateway result = site2SiteVpnManager.createVpnGateway(cmd);

        assertNotNull(result);
        verify(provider, never()).releaseVpnGatewayIp(any(Site2SiteVpnGateway.class));
    }

    @Test
    public void testCreateVpnGatewayRejectsProviderIpOutsideVpcAndCleansUp() {
        CreateVpnGatewayCmd cmd = mock(CreateVpnGatewayCmd.class);
        when(cmd.getVpcId()).thenReturn(VPC_ID);
        when(cmd.getEntityOwnerId()).thenReturn(ACCOUNT_ID);
        when(cmd.getIpAddressId()).thenReturn(null);
        when(_vpcDao.findById(VPC_ID)).thenReturn(vpc);
        when(_vpnGatewayDao.findByVpcId(VPC_ID)).thenReturn(null);
        Site2SiteVpnServiceProvider provider = mockNsxProvider();
        when(provider.acquireVpnGatewayIp(vpc, null)).thenReturn(ipAddress);
        when(_ipAddressDao.findById(IP_ADDRESS_ID)).thenReturn(ipAddress);
        when(ipAddress.getAddress()).thenReturn(new Ip("203.0.113.10"));
        when(ipAddress.readyToUse()).thenReturn(true);
        when(ipAddress.getVpcId()).thenReturn(99L);

        assertThrows(CloudRuntimeException.class, () -> site2SiteVpnManager.createVpnGateway(cmd));

        verify(provider).releaseVpnGatewayIp(any(Site2SiteVpnGateway.class));
        verify(_vpnGatewayDao, never()).persist(any(Site2SiteVpnGatewayVO.class));
    }

    @Test(expected = InvalidParameterValueException.class)
    public void testCreateVpnGatewayInvalidVpc() {
        CreateVpnGatewayCmd cmd = mock(CreateVpnGatewayCmd.class);
        when(cmd.getVpcId()).thenReturn(VPC_ID);
        when(cmd.getEntityOwnerId()).thenReturn(ACCOUNT_ID);

        when(_vpcDao.findById(VPC_ID)).thenReturn(null);

        site2SiteVpnManager.createVpnGateway(cmd);
    }

    @Test(expected = InvalidParameterValueException.class)
    public void testCreateVpnGatewayAlreadyExists() {
        CreateVpnGatewayCmd cmd = mock(CreateVpnGatewayCmd.class);
        when(cmd.getVpcId()).thenReturn(VPC_ID);
        when(cmd.getEntityOwnerId()).thenReturn(ACCOUNT_ID);

        when(_vpcDao.findById(VPC_ID)).thenReturn(vpc);
        when(_vpnGatewayDao.findByVpcId(VPC_ID)).thenReturn(vpnGateway);

        site2SiteVpnManager.createVpnGateway(cmd);
    }

    @Test(expected = CloudRuntimeException.class)
    public void testCreateVpnGatewayNoSourceNatIp() {
        CreateVpnGatewayCmd cmd = mock(CreateVpnGatewayCmd.class);
        when(cmd.getVpcId()).thenReturn(VPC_ID);
        when(cmd.getEntityOwnerId()).thenReturn(ACCOUNT_ID);

        when(_vpcDao.findById(VPC_ID)).thenReturn(vpc);
        when(_vpnGatewayDao.findByVpcId(VPC_ID)).thenReturn(null);
        mockVpcVirtualRouterProvider();
        when(_ipAddressDao.listByAssociatedVpc(VPC_ID, true)).thenReturn(new ArrayList<>());

        site2SiteVpnManager.createVpnGateway(cmd);
    }

    @Test(expected = InvalidParameterValueException.class)
    public void testCreateCustomerGatewayInvalidIp() {
        CreateVpnCustomerGatewayCmd cmd = mock(CreateVpnCustomerGatewayCmd.class);
        when(cmd.getGatewayIp()).thenReturn("invalid-ip");
        when(cmd.getEntityOwnerId()).thenReturn(ACCOUNT_ID);

        try (MockedStatic<NetUtils> netUtilsMock = Mockito.mockStatic(NetUtils.class)) {
            netUtilsMock.when(() -> NetUtils.isValidIp4("invalid-ip")).thenReturn(false);
            netUtilsMock.when(() -> NetUtils.verifyDomainName("invalid-ip")).thenReturn(false);

            site2SiteVpnManager.createCustomerGateway(cmd);
        }
    }

    @Test(expected = InvalidParameterValueException.class)
    public void testCreateCustomerGatewayInvalidCidrList() {
        CreateVpnCustomerGatewayCmd cmd = mock(CreateVpnCustomerGatewayCmd.class);
        when(cmd.getGatewayIp()).thenReturn("1.2.3.4");
        when(cmd.getGuestCidrList()).thenReturn("invalid-cidr");
        when(cmd.getEntityOwnerId()).thenReturn(ACCOUNT_ID);

        try (MockedStatic<NetUtils> netUtilsMock = Mockito.mockStatic(NetUtils.class)) {
            netUtilsMock.when(() -> NetUtils.isValidIp4("1.2.3.4")).thenReturn(true);
            netUtilsMock.when(() -> NetUtils.isValidCidrList("invalid-cidr")).thenReturn(false);

            site2SiteVpnManager.createCustomerGateway(cmd);
        }
    }

    @Test(expected = InvalidParameterValueException.class)
    public void testCreateCustomerGatewayInvalidIkePolicy() {
        CreateVpnCustomerGatewayCmd cmd = mock(CreateVpnCustomerGatewayCmd.class);
        when(cmd.getGatewayIp()).thenReturn("1.2.3.4");
        when(cmd.getGuestCidrList()).thenReturn("192.168.1.0/24");
        when(cmd.getIkePolicy()).thenReturn("invalid-policy");
        when(cmd.getEntityOwnerId()).thenReturn(ACCOUNT_ID);

        try (MockedStatic<NetUtils> netUtilsMock = Mockito.mockStatic(NetUtils.class)) {
            netUtilsMock.when(() -> NetUtils.isValidIp4("1.2.3.4")).thenReturn(true);
            netUtilsMock.when(() -> NetUtils.isValidCidrList("192.168.1.0/24")).thenReturn(true);
            netUtilsMock.when(() -> NetUtils.isValidS2SVpnPolicy("ike", "invalid-policy")).thenReturn(false);

            site2SiteVpnManager.createCustomerGateway(cmd);
        }
    }

    @Test(expected = InvalidParameterValueException.class)
    public void testCreateCustomerGatewayInvalidEspPolicy() {
        CreateVpnCustomerGatewayCmd cmd = mock(CreateVpnCustomerGatewayCmd.class);
        when(cmd.getGatewayIp()).thenReturn("1.2.3.4");
        when(cmd.getGuestCidrList()).thenReturn("192.168.1.0/24");
        when(cmd.getIkePolicy()).thenReturn("aes128-sha256;modp2048");
        when(cmd.getEspPolicy()).thenReturn("invalid-policy");
        when(cmd.getEntityOwnerId()).thenReturn(ACCOUNT_ID);

        try (MockedStatic<NetUtils> netUtilsMock = Mockito.mockStatic(NetUtils.class)) {
            netUtilsMock.when(() -> NetUtils.isValidIp4("1.2.3.4")).thenReturn(true);
            netUtilsMock.when(() -> NetUtils.isValidCidrList("192.168.1.0/24")).thenReturn(true);
            netUtilsMock.when(() -> NetUtils.isValidS2SVpnPolicy("ike", "aes128-sha256;modp2048")).thenReturn(true);
            netUtilsMock.when(() -> NetUtils.isValidS2SVpnPolicy("esp", "invalid-policy")).thenReturn(false);

            site2SiteVpnManager.createCustomerGateway(cmd);
        }
    }

    @Test(expected = InvalidParameterValueException.class)
    public void testCreateCustomerGatewayWithExcludedParameters() throws Exception {
        CreateVpnCustomerGatewayCmd cmd = mock(CreateVpnCustomerGatewayCmd.class);
        when(cmd.getName()).thenReturn("test-gateway");
        when(cmd.getGatewayIp()).thenReturn("1.2.3.4");
        when(cmd.getGuestCidrList()).thenReturn("192.168.1.0/24");
        when(cmd.getIpsecPsk()).thenReturn("test-psk");
        when(cmd.getIkePolicy()).thenReturn("3des-sha256;modp2048");
        when(cmd.getEspPolicy()).thenReturn("aes128-sha256;modp2048");
        when(cmd.getIkeVersion()).thenReturn("ike");
        when(cmd.getEntityOwnerId()).thenReturn(ACCOUNT_ID);

        setConfigKeyValue(Site2SiteVpnManagerImpl.VpnCustomerGatewayExcludedEncryptionAlgorithms, "3des");
        setConfigKeyValue(Site2SiteVpnManagerImpl.VpnCustomerGatewayExcludedHashingAlgorithms, "");
        setConfigKeyValue(Site2SiteVpnManagerImpl.VpnCustomerGatewayExcludedIkeVersions, "");
        setConfigKeyValue(Site2SiteVpnManagerImpl.VpnCustomerGatewayExcludedDhGroup, "");

        try (MockedStatic<NetUtils> netUtilsMock = Mockito.mockStatic(NetUtils.class)) {
            netUtilsMock.when(() -> NetUtils.isValidIp4("1.2.3.4")).thenReturn(true);
            netUtilsMock.when(() -> NetUtils.isValidCidrList("192.168.1.0/24")).thenReturn(true);
            netUtilsMock.when(() -> NetUtils.isValidS2SVpnPolicy("ike", "3des-sha256;modp2048")).thenReturn(true);
            netUtilsMock.when(() -> NetUtils.isValidS2SVpnPolicy("esp", "aes128-sha256;modp2048")).thenReturn(true);

            site2SiteVpnManager.createCustomerGateway(cmd);
        }
    }

    @Test(expected = InvalidParameterValueException.class)
    public void testCreateCustomerGatewayDuplicateName() {
        CreateVpnCustomerGatewayCmd cmd = mock(CreateVpnCustomerGatewayCmd.class);
        when(cmd.getName()).thenReturn("test-gateway");
        when(cmd.getGatewayIp()).thenReturn("1.2.3.4");
        when(cmd.getGuestCidrList()).thenReturn("192.168.1.0/24");
        when(cmd.getIkePolicy()).thenReturn("aes128-sha256;modp2048");
        when(cmd.getEspPolicy()).thenReturn("aes128-sha256;modp2048");
        when(cmd.getEntityOwnerId()).thenReturn(ACCOUNT_ID);

        try (MockedStatic<NetUtils> netUtilsMock = Mockito.mockStatic(NetUtils.class)) {
            netUtilsMock.when(() -> NetUtils.isValidIp4("1.2.3.4")).thenReturn(true);
            netUtilsMock.when(() -> NetUtils.isValidCidrList("192.168.1.0/24")).thenReturn(true);
            netUtilsMock.when(() -> NetUtils.isValidS2SVpnPolicy("ike", "aes128-sha256;modp2048")).thenReturn(true);
            netUtilsMock.when(() -> NetUtils.isValidS2SVpnPolicy("esp", "aes128-sha256;modp2048")).thenReturn(true);

            when(_customerGatewayDao.findByNameAndAccountId("test-gateway", ACCOUNT_ID)).thenReturn(customerGateway);

            site2SiteVpnManager.createCustomerGateway(cmd);
        }
    }

    @Test(expected = InvalidParameterValueException.class)
    public void testCreateCustomerGatewayInvalidIkeLifetime() {
        CreateVpnCustomerGatewayCmd cmd = mock(CreateVpnCustomerGatewayCmd.class);
        when(cmd.getGatewayIp()).thenReturn("1.2.3.4");
        when(cmd.getGuestCidrList()).thenReturn("192.168.1.0/24");
        when(cmd.getIkePolicy()).thenReturn("aes128-sha256;modp2048");
        when(cmd.getEspPolicy()).thenReturn("aes128-sha256;modp2048");
        when(cmd.getIkeLifetime()).thenReturn(86401L);
        when(cmd.getEntityOwnerId()).thenReturn(ACCOUNT_ID);

        try (MockedStatic<NetUtils> netUtilsMock = Mockito.mockStatic(NetUtils.class)) {
            netUtilsMock.when(() -> NetUtils.isValidIp4("1.2.3.4")).thenReturn(true);
            netUtilsMock.when(() -> NetUtils.isValidCidrList("192.168.1.0/24")).thenReturn(true);
            netUtilsMock.when(() -> NetUtils.isValidS2SVpnPolicy("ike", "aes128-sha256;modp2048")).thenReturn(true);
            netUtilsMock.when(() -> NetUtils.isValidS2SVpnPolicy("esp", "aes128-sha256;modp2048")).thenReturn(true);

            site2SiteVpnManager.createCustomerGateway(cmd);
        }
    }

    @Test(expected = InvalidParameterValueException.class)
    public void testCreateCustomerGatewayInvalidEspLifetime() {
        CreateVpnCustomerGatewayCmd cmd = mock(CreateVpnCustomerGatewayCmd.class);
        when(cmd.getGatewayIp()).thenReturn("1.2.3.4");
        when(cmd.getGuestCidrList()).thenReturn("192.168.1.0/24");
        when(cmd.getIkePolicy()).thenReturn("aes128-sha256;modp2048");
        when(cmd.getEspPolicy()).thenReturn("aes128-sha256;modp2048");
        when(cmd.getEspLifetime()).thenReturn(86401L);
        when(cmd.getEntityOwnerId()).thenReturn(ACCOUNT_ID);

        try (MockedStatic<NetUtils> netUtilsMock = Mockito.mockStatic(NetUtils.class)) {
            netUtilsMock.when(() -> NetUtils.isValidIp4("1.2.3.4")).thenReturn(true);
            netUtilsMock.when(() -> NetUtils.isValidCidrList("192.168.1.0/24")).thenReturn(true);
            netUtilsMock.when(() -> NetUtils.isValidS2SVpnPolicy("ike", "aes128-sha256;modp2048")).thenReturn(true);
            netUtilsMock.when(() -> NetUtils.isValidS2SVpnPolicy("esp", "aes128-sha256;modp2048")).thenReturn(true);

            site2SiteVpnManager.createCustomerGateway(cmd);
        }
    }

    @Test(expected = InvalidParameterValueException.class)
    public void testCreateCustomerGatewayTooManySubnets() {
        CreateVpnCustomerGatewayCmd cmd = mock(CreateVpnCustomerGatewayCmd.class);
        when(cmd.getGatewayIp()).thenReturn("1.2.3.4");
        String tooManyCidrs = "192.168.1.0/24,192.168.2.0/24,192.168.3.0/24,192.168.4.0/24,192.168.5.0/24," +
                "192.168.6.0/24,192.168.7.0/24,192.168.8.0/24,192.168.9.0/24,192.168.10.0/24,192.168.11.0/24";
        when(cmd.getGuestCidrList()).thenReturn(tooManyCidrs);
        when(cmd.getIkePolicy()).thenReturn("aes128-sha256;modp2048");
        when(cmd.getEspPolicy()).thenReturn("aes128-sha256;modp2048");
        when(cmd.getEntityOwnerId()).thenReturn(ACCOUNT_ID);

        try (MockedStatic<NetUtils> netUtilsMock = Mockito.mockStatic(NetUtils.class)) {
            netUtilsMock.when(() -> NetUtils.isValidIp4("1.2.3.4")).thenReturn(true);
            netUtilsMock.when(() -> NetUtils.isValidCidrList(tooManyCidrs)).thenReturn(true);
            netUtilsMock.when(() -> NetUtils.getCleanIp4CidrList(tooManyCidrs)).thenReturn(tooManyCidrs);
            netUtilsMock.when(() -> NetUtils.isValidS2SVpnPolicy("ike", "aes128-sha256;modp2048")).thenReturn(true);
            netUtilsMock.when(() -> NetUtils.isValidS2SVpnPolicy("esp", "aes128-sha256;modp2048")).thenReturn(true);

            site2SiteVpnManager.createCustomerGateway(cmd);
        }
    }

    @Test(expected = InvalidParameterValueException.class)
    public void testCreateCustomerGatewayOverlappingSubnets() {
        CreateVpnCustomerGatewayCmd cmd = mock(CreateVpnCustomerGatewayCmd.class);
        when(cmd.getGatewayIp()).thenReturn("1.2.3.4");
        when(cmd.getGuestCidrList()).thenReturn("192.168.1.0/24,192.168.1.0/25");
        when(cmd.getIkePolicy()).thenReturn("aes128-sha256;modp2048");
        when(cmd.getEspPolicy()).thenReturn("aes128-sha256;modp2048");
        when(cmd.getEntityOwnerId()).thenReturn(ACCOUNT_ID);

        try (MockedStatic<NetUtils> netUtilsMock = Mockito.mockStatic(NetUtils.class)) {
            String cidrList = "192.168.1.0/24,192.168.1.0/25";
            netUtilsMock.when(() -> NetUtils.isValidIp4("1.2.3.4")).thenReturn(true);
            netUtilsMock.when(() -> NetUtils.isValidCidrList(cidrList)).thenReturn(true);
            netUtilsMock.when(() -> NetUtils.getCleanIp4CidrList(cidrList)).thenReturn(cidrList);
            netUtilsMock.when(() -> NetUtils.isValidS2SVpnPolicy("ike", "aes128-sha256;modp2048")).thenReturn(true);
            netUtilsMock.when(() -> NetUtils.isValidS2SVpnPolicy("esp", "aes128-sha256;modp2048")).thenReturn(true);
            netUtilsMock.when(() -> NetUtils.isNetworksOverlap("192.168.1.0/24", "192.168.1.0/25")).thenReturn(true);

            site2SiteVpnManager.createCustomerGateway(cmd);
        }
    }

    @Test(expected = InvalidParameterValueException.class)
    public void testCreateVpnConnectionCidrOverlapWithVpc() {
        CreateVpnConnectionCmd cmd = mock(CreateVpnConnectionCmd.class);
        when(cmd.getVpnGatewayId()).thenReturn(VPN_GATEWAY_ID);
        when(cmd.getCustomerGatewayId()).thenReturn(CUSTOMER_GATEWAY_ID);
        when(cmd.getEntityOwnerId()).thenReturn(ACCOUNT_ID);

        Site2SiteCustomerGatewayVO customerGw = mock(Site2SiteCustomerGatewayVO.class);
        when(customerGw.getGuestCidrList()).thenReturn("10.0.0.0/24");
        when(customerGw.getAccountId()).thenReturn(ACCOUNT_ID);
        when(customerGw.getDomainId()).thenReturn(DOMAIN_ID);

        when(_customerGatewayDao.findById(CUSTOMER_GATEWAY_ID)).thenReturn(customerGw);
        when(_vpnGatewayDao.findById(VPN_GATEWAY_ID)).thenReturn(vpnGateway);
        when(_vpnConnectionDao.findByVpnGatewayIdAndCustomerGatewayId(VPN_GATEWAY_ID, CUSTOMER_GATEWAY_ID)).thenReturn(null);
        when(_vpnGatewayDao.findByVpcId(VPC_ID)).thenReturn(vpnGateway);
        when(_vpcDao.findById(VPC_ID)).thenReturn(vpc);
        mockVpcVirtualRouterProvider();

        try (MockedStatic<NetUtils> netUtilsMock = Mockito.mockStatic(NetUtils.class)) {
            netUtilsMock.when(() -> NetUtils.isNetworksOverlap("10.0.0.0/16", "10.0.0.0/24")).thenReturn(true);

            site2SiteVpnManager.createVpnConnection(cmd);
        }
    }

    @Test(expected = InvalidParameterValueException.class)
    public void testCreateVpnConnectionExceedsLimit() {
        CreateVpnConnectionCmd cmd = mock(CreateVpnConnectionCmd.class);
        when(cmd.getVpnGatewayId()).thenReturn(VPN_GATEWAY_ID);
        when(cmd.getCustomerGatewayId()).thenReturn(CUSTOMER_GATEWAY_ID);
        when(cmd.getEntityOwnerId()).thenReturn(ACCOUNT_ID);

        when(_customerGatewayDao.findById(CUSTOMER_GATEWAY_ID)).thenReturn(customerGateway);
        when(_vpnGatewayDao.findById(VPN_GATEWAY_ID)).thenReturn(vpnGateway);
        when(_vpnConnectionDao.findByVpnGatewayIdAndCustomerGatewayId(VPN_GATEWAY_ID, CUSTOMER_GATEWAY_ID)).thenReturn(null);
        when(_vpnGatewayDao.findByVpcId(VPC_ID)).thenReturn(vpnGateway);
        when(_vpcDao.findById(VPC_ID)).thenReturn(vpc);
        mockVpcVirtualRouterProvider();

        List<Site2SiteVpnConnectionVO> existingConns = new ArrayList<>();
        for (int i = 0; i < 4; i++) {
            existingConns.add(mock(Site2SiteVpnConnectionVO.class));
        }
        when(_vpnConnectionDao.listByVpnGatewayId(VPN_GATEWAY_ID)).thenReturn(existingConns);

        site2SiteVpnManager.createVpnConnection(cmd);
    }

    @Test
    public void testCreateVpnConnectionValidatesCustomerGatewayWithOwningProviderBeforePersist() {
        CreateVpnConnectionCmd cmd = mock(CreateVpnConnectionCmd.class);
        when(cmd.getVpnGatewayId()).thenReturn(VPN_GATEWAY_ID);
        when(cmd.getCustomerGatewayId()).thenReturn(CUSTOMER_GATEWAY_ID);
        when(cmd.getEntityOwnerId()).thenReturn(ACCOUNT_ID);
        when(_customerGatewayDao.findById(CUSTOMER_GATEWAY_ID)).thenReturn(customerGateway);
        when(_vpnGatewayDao.findById(VPN_GATEWAY_ID)).thenReturn(vpnGateway);
        when(_vpnConnectionDao.findByVpnGatewayIdAndCustomerGatewayId(VPN_GATEWAY_ID, CUSTOMER_GATEWAY_ID)).thenReturn(null);
        when(_vpnGatewayDao.findByVpcId(VPC_ID)).thenReturn(vpnGateway);
        Site2SiteVpnServiceProvider provider = mockNsxProvider();
        when(provider.ownsVpnGateway(vpnGateway)).thenReturn(true);
        Mockito.doThrow(new InvalidParameterValueException("unsupported by provider"))
                .when(provider).validateSite2SiteVpnCustomerGateway(customerGateway);

        assertThrows(InvalidParameterValueException.class, () -> site2SiteVpnManager.createVpnConnection(cmd));

        verify(_vpnConnectionDao, never()).persist(any(Site2SiteVpnConnectionVO.class));
    }

    @Test
    public void testUpdateCustomerGatewayValidatesProposedValuesBeforePersist() {
        UpdateVpnCustomerGatewayCmd cmd = mock(UpdateVpnCustomerGatewayCmd.class);
        when(cmd.getId()).thenReturn(CUSTOMER_GATEWAY_ID);
        when(cmd.getGatewayIp()).thenReturn("203.0.113.10");
        when(cmd.getGuestCidrList()).thenReturn("192.168.100.0/24");
        when(cmd.getIpsecPsk()).thenReturn("presharedkey");
        when(cmd.getIkePolicy()).thenReturn("aes256-sha256;modp2048");
        when(cmd.getEspPolicy()).thenReturn("aes256-sha256;modp2048");
        when(cmd.getIkeVersion()).thenReturn("ikev2");
        when(_customerGatewayDao.findById(CUSTOMER_GATEWAY_ID)).thenReturn(customerGateway);
        when(_vpnConnectionDao.listByCustomerGatewayId(CUSTOMER_GATEWAY_ID)).thenReturn(List.of(vpnConnection));
        when(_vpnGatewayDao.findById(VPN_GATEWAY_ID)).thenReturn(vpnGateway);
        Site2SiteVpnServiceProvider provider = mockNsxProvider();
        when(provider.ownsVpnGateway(vpnGateway)).thenReturn(true);
        Mockito.doThrow(new InvalidParameterValueException("unsupported by provider"))
                .when(provider).validateSite2SiteVpnCustomerGateway(any(Site2SiteCustomerGatewayVO.class));

        try (MockedStatic<NetUtils> netUtilsMock = Mockito.mockStatic(NetUtils.class)) {
            netUtilsMock.when(() -> NetUtils.isValidIp4("203.0.113.10")).thenReturn(true);
            netUtilsMock.when(() -> NetUtils.isValidCidrList("192.168.100.0/24")).thenReturn(true);
            netUtilsMock.when(() -> NetUtils.getCleanIp4CidrList("192.168.100.0/24"))
                    .thenReturn("192.168.100.0/24");
            netUtilsMock.when(() -> NetUtils.isValidS2SVpnPolicy("ike", "aes256-sha256;modp2048")).thenReturn(true);
            netUtilsMock.when(() -> NetUtils.isValidS2SVpnPolicy("esp", "aes256-sha256;modp2048")).thenReturn(true);

            assertThrows(InvalidParameterValueException.class,
                    () -> site2SiteVpnManager.updateCustomerGateway(cmd));
        }

        verify(_customerGatewayDao, never()).persist(customerGateway);
    }

    @Test
    public void testCustomerGatewayUpdateRestartUsesSingleConnectionLock() throws ResourceUnavailableException {
        vpnConnection.setState(State.Connected);
        when(_vpnConnectionDao.listByCustomerGatewayId(CUSTOMER_GATEWAY_ID)).thenReturn(List.of(vpnConnection));
        when(_vpnConnectionDao.acquireInLockTable(VPN_CONNECTION_ID)).thenReturn(vpnConnection);
        when(_vpnGatewayDao.findById(VPN_GATEWAY_ID)).thenReturn(vpnGateway);
        when(_vpcMgr.applyStaticRouteForVpcVpnIfNeeded(VPC_ID, false)).thenReturn(true);
        Site2SiteVpnServiceProvider provider = mockVpcVirtualRouterProvider();
        when(provider.stopSite2SiteVpn(vpnConnection)).thenReturn(true);
        when(provider.startSite2SiteVpn(vpnConnection)).thenReturn(true);

        ReflectionTestUtils.invokeMethod(site2SiteVpnManager, "setupVpnConnection", account, CUSTOMER_GATEWAY_ID);

        verify(_vpnConnectionDao, times(1)).acquireInLockTable(VPN_CONNECTION_ID);
        InOrder lifecycle = inOrder(provider, _vpcMgr, _vpnConnectionDao);
        lifecycle.verify(provider).stopSite2SiteVpn(vpnConnection);
        lifecycle.verify(_vpcMgr).applyStaticRouteForVpcVpnIfNeeded(VPC_ID, false);
        lifecycle.verify(provider).startSite2SiteVpn(vpnConnection);
        lifecycle.verify(_vpnConnectionDao).releaseFromLockTable(VPN_CONNECTION_ID);
    }

    @Test
    public void testUpdateCustomerGatewayDefinesDatabaseContextForConnectionLock() throws NoSuchMethodException {
        assertTrue(Site2SiteVpnManagerImpl.class.getMethod("updateCustomerGateway", UpdateVpnCustomerGatewayCmd.class)
                .isAnnotationPresent(DB.class));
    }

    @Test
    public void testCustomerGatewayUpdateLockFailureIsReported() {
        when(_vpnConnectionDao.listByCustomerGatewayId(CUSTOMER_GATEWAY_ID)).thenReturn(List.of(vpnConnection));
        when(_vpnConnectionDao.acquireInLockTable(VPN_CONNECTION_ID)).thenReturn(null);

        assertThrows(CloudRuntimeException.class,
                () -> ReflectionTestUtils.invokeMethod(site2SiteVpnManager, "setupVpnConnection", account, CUSTOMER_GATEWAY_ID));

        verify(_vpnConnectionDao, never()).releaseFromLockTable(VPN_CONNECTION_ID);
    }

    @Test
    public void testCustomerGatewayUpdateDoesNotRestartPendingConnection() {
        vpnConnection.setState(State.Pending);
        when(_vpnConnectionDao.listByCustomerGatewayId(CUSTOMER_GATEWAY_ID)).thenReturn(List.of(vpnConnection));
        when(_vpnConnectionDao.acquireInLockTable(VPN_CONNECTION_ID)).thenReturn(vpnConnection);

        ReflectionTestUtils.invokeMethod(site2SiteVpnManager, "setupVpnConnection", account, CUSTOMER_GATEWAY_ID);

        verify(_vpnGatewayDao, never()).findById(anyLong());
        verify(_vpnConnectionDao).releaseFromLockTable(VPN_CONNECTION_ID);
    }

    @Test
    public void testDeleteCustomerGatewaySuccess() {
        DeleteVpnCustomerGatewayCmd cmd = mock(DeleteVpnCustomerGatewayCmd.class);
        when(cmd.getId()).thenReturn(CUSTOMER_GATEWAY_ID);

        when(_customerGatewayDao.findById(CUSTOMER_GATEWAY_ID)).thenReturn(customerGateway);
        when(_vpnConnectionDao.listByCustomerGatewayId(CUSTOMER_GATEWAY_ID)).thenReturn(new ArrayList<>());

        boolean result = site2SiteVpnManager.deleteCustomerGateway(cmd);

        assertTrue(result);
        verify(_customerGatewayDao).remove(CUSTOMER_GATEWAY_ID);
    }

    @Test(expected = InvalidParameterValueException.class)
    public void testDeleteCustomerGatewayWithConnections() {
        DeleteVpnCustomerGatewayCmd cmd = mock(DeleteVpnCustomerGatewayCmd.class);
        when(cmd.getId()).thenReturn(CUSTOMER_GATEWAY_ID);

        when(_customerGatewayDao.findById(CUSTOMER_GATEWAY_ID)).thenReturn(customerGateway);
        when(_vpnConnectionDao.listByCustomerGatewayId(CUSTOMER_GATEWAY_ID)).thenReturn(List.of(vpnConnection));

        site2SiteVpnManager.deleteCustomerGateway(cmd);
    }

    @Test
    public void testDeleteVpnGatewaySuccess() {
        DeleteVpnGatewayCmd cmd = mock(DeleteVpnGatewayCmd.class);
        when(cmd.getId()).thenReturn(VPN_GATEWAY_ID);

        when(_vpnGatewayDao.findById(VPN_GATEWAY_ID)).thenReturn(vpnGateway);
        when(_vpnConnectionDao.listByVpnGatewayId(VPN_GATEWAY_ID)).thenReturn(new ArrayList<>());
        mockVpcVirtualRouterProvider();

        boolean result = site2SiteVpnManager.deleteVpnGateway(cmd);

        assertTrue(result);
        verify(_vpnGatewayDao).remove(VPN_GATEWAY_ID);
    }

    @Test(expected = InvalidParameterValueException.class)
    public void testDeleteVpnGatewayWithConnections() {
        DeleteVpnGatewayCmd cmd = mock(DeleteVpnGatewayCmd.class);
        when(cmd.getId()).thenReturn(VPN_GATEWAY_ID);

        when(_vpnGatewayDao.findById(VPN_GATEWAY_ID)).thenReturn(vpnGateway);
        when(_vpnConnectionDao.listByVpnGatewayId(VPN_GATEWAY_ID)).thenReturn(List.of(vpnConnection));

        site2SiteVpnManager.deleteVpnGateway(cmd);
    }

    @Test
    public void testDeleteVpnConnectionSuccess() throws ResourceUnavailableException {
        DeleteVpnConnectionCmd cmd = mock(DeleteVpnConnectionCmd.class);
        when(cmd.getId()).thenReturn(VPN_CONNECTION_ID);

        vpnConnection.setState(State.Pending);
        when(_vpnConnectionDao.acquireInLockTable(VPN_CONNECTION_ID)).thenReturn(vpnConnection);
        when(_vpnGatewayDao.findById(VPN_GATEWAY_ID)).thenReturn(vpnGateway);
        when(_vpcMgr.applyStaticRouteForVpcVpnIfNeeded(anyLong(), anyBoolean())).thenReturn(true);
        Site2SiteVpnServiceProvider provider = mockVpcVirtualRouterProvider();
        when(provider.deleteSite2SiteVpn(any(Site2SiteVpnConnection.class))).thenReturn(true);

        boolean result = site2SiteVpnManager.deleteVpnConnection(cmd);

        assertTrue(result);
        assertEquals(State.Removed, vpnConnection.getState());
        verify(_vpnConnectionDao, times(1)).acquireInLockTable(VPN_CONNECTION_ID);
        InOrder lifecycle = inOrder(provider, _vpnConnectionDao, _vpcMgr);
        lifecycle.verify(provider).deleteSite2SiteVpn(vpnConnection);
        lifecycle.verify(_vpnConnectionDao).update(VPN_CONNECTION_ID, vpnConnection);
        lifecycle.verify(_vpcMgr).applyStaticRouteForVpcVpnIfNeeded(VPC_ID, false);
        lifecycle.verify(_vpnConnectionDao).remove(VPN_CONNECTION_ID);
        lifecycle.verify(_vpnConnectionDao).releaseFromLockTable(VPN_CONNECTION_ID);
    }

    @Test
    public void testDeleteVpnConnectionProviderFailureKeepsRowAndReleasesLock() throws ResourceUnavailableException {
        DeleteVpnConnectionCmd cmd = mock(DeleteVpnConnectionCmd.class);
        when(cmd.getId()).thenReturn(VPN_CONNECTION_ID);
        vpnConnection.setState(State.Connected);
        when(_vpnConnectionDao.acquireInLockTable(VPN_CONNECTION_ID)).thenReturn(vpnConnection);
        when(_vpnGatewayDao.findById(VPN_GATEWAY_ID)).thenReturn(vpnGateway);
        Site2SiteVpnServiceProvider provider = mockVpcVirtualRouterProvider();
        when(provider.deleteSite2SiteVpn(vpnConnection)).thenReturn(false);

        assertThrows(ResourceUnavailableException.class,
                () -> site2SiteVpnManager.deleteVpnConnection(cmd));

        assertEquals(State.Error, vpnConnection.getState());
        verify(_vpnConnectionDao, never()).update(VPN_CONNECTION_ID, vpnConnection);
        verify(_vpnConnectionDao, never()).remove(VPN_CONNECTION_ID);
        verify(_vpnConnectionDao).releaseFromLockTable(VPN_CONNECTION_ID);
    }

    @Test
    public void testDeleteVpnConnectionLockFailureIsNotReportedAsMissing() {
        DeleteVpnConnectionCmd cmd = mock(DeleteVpnConnectionCmd.class);
        when(cmd.getId()).thenReturn(VPN_CONNECTION_ID);
        when(_vpnConnectionDao.acquireInLockTable(VPN_CONNECTION_ID)).thenReturn(null);
        when(_vpnConnectionDao.findById(VPN_CONNECTION_ID)).thenReturn(vpnConnection);

        assertThrows(CloudRuntimeException.class,
                () -> site2SiteVpnManager.deleteVpnConnection(cmd));
    }

    @Test
    public void testStartVpnConnectionSuccess() throws ResourceUnavailableException {
        when(_vpnConnectionDao.acquireInLockTable(VPN_CONNECTION_ID)).thenReturn(vpnConnection);
        vpnConnection.setState(State.Pending);
        when(_vpnGatewayDao.findById(VPN_GATEWAY_ID)).thenReturn(vpnGateway);
        Site2SiteVpnServiceProvider provider = mockVpcVirtualRouterProvider();
        when(provider.startSite2SiteVpn(any(Site2SiteVpnConnection.class))).thenReturn(true);
        when(_vpnConnectionDao.persist(any(Site2SiteVpnConnectionVO.class))).thenReturn(vpnConnection);
        when(_vpcMgr.applyStaticRouteForVpcVpnIfNeeded(anyLong(), anyBoolean())).thenReturn(true);

        Site2SiteVpnConnection result = site2SiteVpnManager.startVpnConnection(VPN_CONNECTION_ID);

        assertNotNull(result);
        verify(_vpnConnectionDao, org.mockito.Mockito.atLeastOnce()).persist(any(Site2SiteVpnConnectionVO.class));
    }

    @Test
    public void testStartVpnConnectionProviderFailureLeavesConnectionInError() throws ResourceUnavailableException {
        when(_vpnConnectionDao.acquireInLockTable(VPN_CONNECTION_ID)).thenReturn(vpnConnection);
        vpnConnection.setState(State.Pending);
        when(_vpnGatewayDao.findById(VPN_GATEWAY_ID)).thenReturn(vpnGateway);
        Site2SiteVpnServiceProvider provider = mockVpcVirtualRouterProvider();
        when(provider.startSite2SiteVpn(any(Site2SiteVpnConnection.class)))
                .thenThrow(new InvalidParameterValueException("unsupported VPN policy"));
        when(_vpcMgr.applyStaticRouteForVpcVpnIfNeeded(anyLong(), anyBoolean())).thenReturn(true);

        assertThrows(InvalidParameterValueException.class,
                () -> site2SiteVpnManager.startVpnConnection(VPN_CONNECTION_ID));

        assertEquals(State.Error, vpnConnection.getState());
        verify(_vpnConnectionDao, org.mockito.Mockito.atLeastOnce()).persist(vpnConnection);
    }

    @Test
    public void testStartVpnConnectionMissingGatewayLeavesConnectionInError() {
        when(_vpnConnectionDao.acquireInLockTable(VPN_CONNECTION_ID)).thenReturn(vpnConnection);
        vpnConnection.setState(State.Pending);
        when(_vpnGatewayDao.findById(VPN_GATEWAY_ID)).thenReturn(null);

        assertThrows(CloudRuntimeException.class,
                () -> site2SiteVpnManager.startVpnConnection(VPN_CONNECTION_ID));

        assertEquals(State.Error, vpnConnection.getState());
        verify(_vpnConnectionDao, times(2)).persist(vpnConnection);
        verify(_vpnConnectionDao).releaseFromLockTable(VPN_CONNECTION_ID);
    }

    @Test
    public void testGatewayLifecycleUsesPersistedProviderWhenOfferingChanges() throws ResourceUnavailableException {
        when(_vpnConnectionDao.acquireInLockTable(VPN_CONNECTION_ID)).thenReturn(vpnConnection);
        when(_vpnGatewayDao.findById(VPN_GATEWAY_ID)).thenReturn(vpnGateway);
        Site2SiteVpnServiceProvider originalProvider = mockVpcVirtualRouterProvider();
        Site2SiteVpnServiceProvider replacementProvider = mock(Site2SiteVpnServiceProvider.class,
                Mockito.withSettings().extraInterfaces(NetworkElement.class));
        when(((NetworkElement) replacementProvider).getProvider()).thenReturn(Network.Provider.Nsx);
        when(_vpcMgr.isProviderSupportServiceInVpc(VPC_ID, Network.Service.Vpn, Network.Provider.Nsx))
                .thenReturn(true);
        when(originalProvider.ownsVpnGateway(vpnGateway)).thenReturn(true);
        when(originalProvider.startSite2SiteVpn(vpnConnection)).thenReturn(true);
        when(_vpnConnectionDao.persist(any(Site2SiteVpnConnectionVO.class))).thenReturn(vpnConnection);

        site2SiteVpnManager.startVpnConnection(VPN_CONNECTION_ID);

        verify(originalProvider).startSite2SiteVpn(vpnConnection);
        verify(replacementProvider, never()).startSite2SiteVpn(any(Site2SiteVpnConnection.class));
    }

    @Test
    public void testUnmarkedGatewayUsesLegacyVirtualRouterWhenOfferingNowUsesNsx() throws ResourceUnavailableException {
        when(_vpnConnectionDao.acquireInLockTable(VPN_CONNECTION_ID)).thenReturn(vpnConnection);
        when(_vpnGatewayDao.findById(VPN_GATEWAY_ID)).thenReturn(vpnGateway);
        Site2SiteVpnServiceProvider nsxProvider = mock(Site2SiteVpnServiceProvider.class,
                Mockito.withSettings().extraInterfaces(NetworkElement.class));
        Site2SiteVpnServiceProvider virtualRouterProvider = mock(Site2SiteVpnServiceProvider.class,
                Mockito.withSettings().extraInterfaces(NetworkElement.class));
        when(((NetworkElement) nsxProvider).getProvider()).thenReturn(Network.Provider.Nsx);
        when(((NetworkElement) virtualRouterProvider).getProvider()).thenReturn(Network.Provider.VPCVirtualRouter);
        when(_vpcMgr.isProviderSupportServiceInVpc(VPC_ID, Network.Service.Vpn, Network.Provider.Nsx))
                .thenReturn(true);
        when(nsxProvider.ownsVpnGateway(vpnGateway)).thenReturn(false);
        when(virtualRouterProvider.ownsVpnGateway(vpnGateway)).thenReturn(false);
        when(_s2sProviders.iterator()).thenAnswer(invocation -> List.of(nsxProvider, virtualRouterProvider).iterator());
        when(virtualRouterProvider.startSite2SiteVpn(vpnConnection)).thenReturn(true);
        when(_vpnConnectionDao.persist(any(Site2SiteVpnConnectionVO.class))).thenReturn(vpnConnection);

        site2SiteVpnManager.startVpnConnection(VPN_CONNECTION_ID);

        verify(virtualRouterProvider).startSite2SiteVpn(vpnConnection);
        verify(nsxProvider, never()).startSite2SiteVpn(any(Site2SiteVpnConnection.class));
    }

    @Test(expected = InvalidParameterValueException.class)
    public void testStartVpnConnectionWrongState() throws ResourceUnavailableException {
        when(_vpnConnectionDao.acquireInLockTable(VPN_CONNECTION_ID)).thenReturn(vpnConnection);
        vpnConnection.setState(State.Connected);

        site2SiteVpnManager.startVpnConnection(VPN_CONNECTION_ID);
    }

    @Test
    public void testResetVpnConnectionSuccess() throws ResourceUnavailableException {
        ResetVpnConnectionCmd cmd = mock(ResetVpnConnectionCmd.class);
        when(cmd.getId()).thenReturn(VPN_CONNECTION_ID);

        vpnConnection.setState(State.Connected);
        when(_vpnConnectionDao.acquireInLockTable(VPN_CONNECTION_ID)).thenReturn(vpnConnection);
        when(_vpnGatewayDao.findById(VPN_GATEWAY_ID)).thenReturn(vpnGateway);
        Site2SiteVpnServiceProvider provider = mockVpcVirtualRouterProvider();
        when(provider.stopSite2SiteVpn(any(Site2SiteVpnConnection.class))).thenReturn(true);
        when(provider.startSite2SiteVpn(any(Site2SiteVpnConnection.class))).thenReturn(true);
        when(_vpnConnectionDao.persist(any(Site2SiteVpnConnectionVO.class))).thenReturn(vpnConnection);
        when(_vpcMgr.applyStaticRouteForVpcVpnIfNeeded(anyLong(), anyBoolean())).thenReturn(true);

        Site2SiteVpnConnection result = site2SiteVpnManager.resetVpnConnection(cmd);

        assertNotNull(result);
        assertEquals(State.Connecting, result.getState());
        verify(_vpnConnectionDao, times(1)).acquireInLockTable(VPN_CONNECTION_ID);
        InOrder lifecycle = inOrder(provider, _vpnConnectionDao, _vpcMgr);
        lifecycle.verify(provider).stopSite2SiteVpn(vpnConnection);
        lifecycle.verify(_vpcMgr).applyStaticRouteForVpcVpnIfNeeded(VPC_ID, false);
        lifecycle.verify(provider).startSite2SiteVpn(vpnConnection);
        lifecycle.verify(_vpnConnectionDao).releaseFromLockTable(VPN_CONNECTION_ID);
    }

    @Test
    public void testResetVpnConnectionStartFailureLeavesErrorAndReleasesLock() throws ResourceUnavailableException {
        ResetVpnConnectionCmd cmd = mock(ResetVpnConnectionCmd.class);
        when(cmd.getId()).thenReturn(VPN_CONNECTION_ID);
        vpnConnection.setState(State.Connected);
        when(_vpnConnectionDao.acquireInLockTable(VPN_CONNECTION_ID)).thenReturn(vpnConnection);
        when(_vpnGatewayDao.findById(VPN_GATEWAY_ID)).thenReturn(vpnGateway);
        Site2SiteVpnServiceProvider provider = mockVpcVirtualRouterProvider();
        when(provider.stopSite2SiteVpn(vpnConnection)).thenReturn(true);
        when(provider.startSite2SiteVpn(vpnConnection)).thenReturn(false);
        when(_vpcMgr.applyStaticRouteForVpcVpnIfNeeded(VPC_ID, false)).thenReturn(true);

        assertThrows(ResourceUnavailableException.class,
                () -> site2SiteVpnManager.resetVpnConnection(cmd));

        assertEquals(State.Error, vpnConnection.getState());
        verify(_vpnConnectionDao, times(1)).acquireInLockTable(VPN_CONNECTION_ID);
        verify(_vpnConnectionDao).releaseFromLockTable(VPN_CONNECTION_ID);
    }

    @Test
    public void testResetVpnConnectionAllowsPendingConnection() throws ResourceUnavailableException {
        ResetVpnConnectionCmd cmd = mock(ResetVpnConnectionCmd.class);
        when(cmd.getId()).thenReturn(VPN_CONNECTION_ID);
        vpnConnection.setState(State.Pending);
        when(_vpnConnectionDao.acquireInLockTable(VPN_CONNECTION_ID)).thenReturn(vpnConnection);
        when(_vpnGatewayDao.findById(VPN_GATEWAY_ID)).thenReturn(vpnGateway);
        Site2SiteVpnServiceProvider provider = mockVpcVirtualRouterProvider();
        when(provider.stopSite2SiteVpn(vpnConnection)).thenReturn(true);
        when(provider.startSite2SiteVpn(vpnConnection)).thenReturn(true);

        Site2SiteVpnConnection result = site2SiteVpnManager.resetVpnConnection(cmd);

        assertEquals(State.Connecting, result.getState());
        verify(provider).stopSite2SiteVpn(vpnConnection);
        verify(provider).startSite2SiteVpn(vpnConnection);
        verify(_vpnConnectionDao).releaseFromLockTable(VPN_CONNECTION_ID);
    }

    @Test
    public void testResetVpnConnectionStopFailureDoesNotStartAndReleasesLock() throws ResourceUnavailableException {
        ResetVpnConnectionCmd cmd = mock(ResetVpnConnectionCmd.class);
        when(cmd.getId()).thenReturn(VPN_CONNECTION_ID);
        vpnConnection.setState(State.Connected);
        when(_vpnConnectionDao.acquireInLockTable(VPN_CONNECTION_ID)).thenReturn(vpnConnection);
        when(_vpnGatewayDao.findById(VPN_GATEWAY_ID)).thenReturn(vpnGateway);
        Site2SiteVpnServiceProvider provider = mockVpcVirtualRouterProvider();
        when(provider.stopSite2SiteVpn(vpnConnection)).thenReturn(false);

        assertThrows(ResourceUnavailableException.class,
                () -> site2SiteVpnManager.resetVpnConnection(cmd));

        assertEquals(State.Error, vpnConnection.getState());
        verify(provider, never()).startSite2SiteVpn(vpnConnection);
        verify(_vpnConnectionDao).releaseFromLockTable(VPN_CONNECTION_ID);
    }

    @Test
    public void testResetVpnConnectionAccessDeniedReleasesLock() {
        ResetVpnConnectionCmd cmd = mock(ResetVpnConnectionCmd.class);
        when(cmd.getId()).thenReturn(VPN_CONNECTION_ID);
        when(_vpnConnectionDao.acquireInLockTable(VPN_CONNECTION_ID)).thenReturn(vpnConnection);
        Mockito.doThrow(new PermissionDeniedException("denied"))
                .when(_accountMgr).checkAccess(account, null, false, vpnConnection);

        assertThrows(PermissionDeniedException.class,
                () -> site2SiteVpnManager.resetVpnConnection(cmd));

        verify(_vpnConnectionDao).releaseFromLockTable(VPN_CONNECTION_ID);
        verify(_vpnGatewayDao, never()).findById(anyLong());
    }

    @Test
    public void testResetVpnConnectionLockFailureIsNotReportedAsMissing() {
        ResetVpnConnectionCmd cmd = mock(ResetVpnConnectionCmd.class);
        when(cmd.getId()).thenReturn(VPN_CONNECTION_ID);
        when(_vpnConnectionDao.acquireInLockTable(VPN_CONNECTION_ID)).thenReturn(null);
        when(_vpnConnectionDao.findById(VPN_CONNECTION_ID)).thenReturn(vpnConnection);

        assertThrows(CloudRuntimeException.class,
                () -> site2SiteVpnManager.resetVpnConnection(cmd));
    }

    @Test
    public void testCleanupVpnConnectionByVpc() {
        when(_vpnConnectionDao.listByVpcId(VPC_ID)).thenReturn(List.of(vpnConnection));

        boolean result = site2SiteVpnManager.cleanupVpnConnectionByVpc(VPC_ID);

        assertTrue(result);
        verify(_vpnConnectionDao).remove(vpnConnection.getId());
    }

    @Test
    public void testCleanupVpnGatewayByVpc() {
        when(_vpnGatewayDao.findByVpcId(VPC_ID)).thenReturn(vpnGateway);
        when(_vpnConnectionDao.listByVpnGatewayId(VPN_GATEWAY_ID)).thenReturn(new ArrayList<>());
        mockVpcVirtualRouterProvider();

        boolean result = site2SiteVpnManager.cleanupVpnGatewayByVpc(VPC_ID);

        assertTrue(result);
        verify(_vpnGatewayDao).remove(VPN_GATEWAY_ID);
    }

    @Test
    public void testCleanupVpnGatewayByVpcNotFound() {
        when(_vpnGatewayDao.findByVpcId(VPC_ID)).thenReturn(null);

        boolean result = site2SiteVpnManager.cleanupVpnGatewayByVpc(VPC_ID);

        assertTrue(result);
        verify(_vpnGatewayDao, never()).remove(anyLong());
    }

    @Test
    public void testGetConnectionsForRouter() {
        DomainRouterVO router = mock(DomainRouterVO.class);
        when(router.getVpcId()).thenReturn(VPC_ID);
        when(_vpnGatewayDao.findByVpcId(VPC_ID)).thenReturn(vpnGateway);
        mockVpcVirtualRouterProvider();
        when(_vpnConnectionDao.listByVpcId(VPC_ID)).thenReturn(List.of(vpnConnection));

        List<Site2SiteVpnConnectionVO> result = site2SiteVpnManager.getConnectionsForRouter(router);

        assertNotNull(result);
        assertEquals(1, result.size());
    }

    @Test
    public void testGetConnectionsForRouterNoVpc() {
        DomainRouterVO router = mock(DomainRouterVO.class);
        when(router.getVpcId()).thenReturn(null);

        List<Site2SiteVpnConnectionVO> result = site2SiteVpnManager.getConnectionsForRouter(router);

        assertNotNull(result);
        assertTrue(result.isEmpty());
    }

    @Test
    public void testDeleteCustomerGatewayByAccount() {
        when(_customerGatewayDao.listByAccountId(ACCOUNT_ID)).thenReturn(List.of(customerGateway));
        when(_vpnConnectionDao.listByCustomerGatewayId(CUSTOMER_GATEWAY_ID)).thenReturn(new ArrayList<>());

        boolean result = site2SiteVpnManager.deleteCustomerGatewayByAccount(ACCOUNT_ID);

        assertTrue(result);
        verify(_customerGatewayDao).remove(CUSTOMER_GATEWAY_ID);
    }

    @Test
    public void testReconnectDisconnectedVpnByVpc() throws ResourceUnavailableException {
        Site2SiteVpnConnectionVO conn = mock(Site2SiteVpnConnectionVO.class);
        when(conn.getId()).thenReturn(VPN_CONNECTION_ID);
        when(conn.getState()).thenReturn(State.Disconnected);
        when(conn.getCustomerGatewayId()).thenReturn(CUSTOMER_GATEWAY_ID);
        when(conn.getVpnGatewayId()).thenReturn(VPN_GATEWAY_ID);
        when(_vpnConnectionDao.listByVpcId(VPC_ID)).thenReturn(List.of(conn));
        when(_customerGatewayDao.findById(CUSTOMER_GATEWAY_ID)).thenReturn(customerGateway);
        when(_vpnConnectionDao.acquireInLockTable(VPN_CONNECTION_ID)).thenReturn(conn);
        when(_vpnGatewayDao.findById(VPN_GATEWAY_ID)).thenReturn(vpnGateway);
        Site2SiteVpnServiceProvider provider = mockVpcVirtualRouterProvider();
        when(provider.startSite2SiteVpn(any(Site2SiteVpnConnection.class))).thenReturn(true);
        when(_vpnConnectionDao.persist(any(Site2SiteVpnConnectionVO.class))).thenReturn(conn);
        when(_vpcMgr.applyStaticRouteForVpcVpnIfNeeded(anyLong(), anyBoolean())).thenReturn(true);

        site2SiteVpnManager.reconnectDisconnectedVpnByVpc(VPC_ID);

        verify(_vpnConnectionDao, org.mockito.Mockito.atLeastOnce()).persist(any(Site2SiteVpnConnectionVO.class));
    }

    @Test
    public void testUpdateVpnConnection() {
        when(_vpnConnectionDao.findById(VPN_CONNECTION_ID)).thenReturn(vpnConnection);
        when(_vpnConnectionDao.update(anyLong(), any(Site2SiteVpnConnectionVO.class))).thenReturn(true);
        when(_vpnConnectionDao.findById(VPN_CONNECTION_ID)).thenReturn(vpnConnection);

        Site2SiteVpnConnection result = site2SiteVpnManager.updateVpnConnection(VPN_CONNECTION_ID, "custom-id", true);

        assertNotNull(result);
    }

    @Test
    public void testUpdateVpnGateway() {
        when(_vpnGatewayDao.findById(VPN_GATEWAY_ID)).thenReturn(vpnGateway);
        when(_vpnGatewayDao.update(anyLong(), any(Site2SiteVpnGatewayVO.class))).thenReturn(true);
        when(_vpnGatewayDao.findById(VPN_GATEWAY_ID)).thenReturn(vpnGateway);

        Site2SiteVpnGateway result = site2SiteVpnManager.updateVpnGateway(VPN_GATEWAY_ID, "custom-id", true);

        assertNotNull(result);
    }

    @Test
    public void testVpnGatewayContainsExcludedParametersWithExcludedIkeVersion() throws Exception {
        Site2SiteCustomerGatewayVO gw = mock(Site2SiteCustomerGatewayVO.class);
        when(gw.getIkePolicy()).thenReturn("aes128-sha256;modp2048");
        when(gw.getEspPolicy()).thenReturn("aes128-sha256;modp2048");
        when(gw.getIkeVersion()).thenReturn("ikev1");
        when(gw.getDomainId()).thenReturn(DOMAIN_ID);

        setConfigKeyValue(Site2SiteVpnManagerImpl.VpnCustomerGatewayExcludedIkeVersions, "ikev1");
        setConfigKeyValue(Site2SiteVpnManagerImpl.VpnCustomerGatewayExcludedEncryptionAlgorithms, "");
        setConfigKeyValue(Site2SiteVpnManagerImpl.VpnCustomerGatewayExcludedHashingAlgorithms, "");
        setConfigKeyValue(Site2SiteVpnManagerImpl.VpnCustomerGatewayExcludedDhGroup, "");

        java.util.Set<String> result = site2SiteVpnManager.getExcludedVpnGatewayParameters(gw);
        assertFalse("Should detect excluded IKE version", result.isEmpty());
        assertEquals("Should detect excluded IKE version", "[ikev1]", result.toString());
    }

    @Test
    public void testVpnGatewayContainsExcludedParametersWithExcludedEncryption() throws Exception {
        Site2SiteCustomerGatewayVO gw = mock(Site2SiteCustomerGatewayVO.class);
        when(gw.getIkePolicy()).thenReturn("3des-sha256;modp2048");
        when(gw.getEspPolicy()).thenReturn("aes128-sha256;modp2048");
        when(gw.getIkeVersion()).thenReturn("ike");
        when(gw.getDomainId()).thenReturn(DOMAIN_ID);

        setConfigKeyValue(Site2SiteVpnManagerImpl.VpnCustomerGatewayExcludedIkeVersions, "");
        setConfigKeyValue(Site2SiteVpnManagerImpl.VpnCustomerGatewayExcludedEncryptionAlgorithms, "3des");
        setConfigKeyValue(Site2SiteVpnManagerImpl.VpnCustomerGatewayExcludedHashingAlgorithms, "");
        setConfigKeyValue(Site2SiteVpnManagerImpl.VpnCustomerGatewayExcludedDhGroup, "");

        java.util.Set<String> result = site2SiteVpnManager.getExcludedVpnGatewayParameters(gw);
        assertFalse("Should detect excluded encryption algorithm", result.isEmpty());
        assertEquals("Should detect excluded encryption algorithm", "[3des]", result.toString());
    }

    @Test
    public void testVpnGatewayContainsExcludedParametersWithExcludedHashing() throws Exception {
        Site2SiteCustomerGatewayVO gw = mock(Site2SiteCustomerGatewayVO.class);
        when(gw.getIkePolicy()).thenReturn("aes128-md5;modp2048");
        when(gw.getEspPolicy()).thenReturn("aes128-sha256;modp2048");
        when(gw.getIkeVersion()).thenReturn("ike");
        when(gw.getDomainId()).thenReturn(DOMAIN_ID);

        setConfigKeyValue(Site2SiteVpnManagerImpl.VpnCustomerGatewayExcludedIkeVersions, "");
        setConfigKeyValue(Site2SiteVpnManagerImpl.VpnCustomerGatewayExcludedEncryptionAlgorithms, "aes128");
        setConfigKeyValue(Site2SiteVpnManagerImpl.VpnCustomerGatewayExcludedHashingAlgorithms, "md5");
        setConfigKeyValue(Site2SiteVpnManagerImpl.VpnCustomerGatewayExcludedDhGroup, "");

        java.util.Set<String> result = site2SiteVpnManager.getExcludedVpnGatewayParameters(gw);
        assertFalse("Should detect excluded algorithms", result.isEmpty());
        assertEquals("Should detect excluded algorithms", "[aes128, md5]", result.toString());
    }

    @Test
    public void testVpnGatewayContainsExcludedParametersWithExcludedDhGroup() {
        Site2SiteCustomerGatewayVO gw = mock(Site2SiteCustomerGatewayVO.class);
        when(gw.getIkePolicy()).thenReturn("aes128-sha256;modp1024");
        when(gw.getEspPolicy()).thenReturn("aes128-sha256;modp2048");
        when(gw.getIkeVersion()).thenReturn("ike");
        when(gw.getDomainId()).thenReturn(DOMAIN_ID);

        setConfigKeyValue(Site2SiteVpnManagerImpl.VpnCustomerGatewayExcludedIkeVersions, "");
        setConfigKeyValue(Site2SiteVpnManagerImpl.VpnCustomerGatewayExcludedEncryptionAlgorithms, "");
        setConfigKeyValue(Site2SiteVpnManagerImpl.VpnCustomerGatewayExcludedHashingAlgorithms, "");
        setConfigKeyValue(Site2SiteVpnManagerImpl.VpnCustomerGatewayExcludedDhGroup, "modp1024");

        java.util.Set<String> result = site2SiteVpnManager.getExcludedVpnGatewayParameters(gw);
        assertFalse("Should detect excluded DH group", result.isEmpty());
        assertEquals("Should detect excluded DH group", "[modp1024]", result.toString());
    }

    @Test
    public void testVpnGatewayContainsExcludedParametersNoExcludedParameters() {
        Site2SiteCustomerGatewayVO gw = mock(Site2SiteCustomerGatewayVO.class);
        when(gw.getIkePolicy()).thenReturn("aes128-sha256;modp2048");
        when(gw.getEspPolicy()).thenReturn("aes128-sha256;modp2048");
        when(gw.getIkeVersion()).thenReturn("ike");
        when(gw.getDomainId()).thenReturn(DOMAIN_ID);

        setConfigKeyValue(Site2SiteVpnManagerImpl.VpnCustomerGatewayExcludedIkeVersions, "");
        setConfigKeyValue(Site2SiteVpnManagerImpl.VpnCustomerGatewayExcludedEncryptionAlgorithms, "");
        setConfigKeyValue(Site2SiteVpnManagerImpl.VpnCustomerGatewayExcludedHashingAlgorithms, "");
        setConfigKeyValue(Site2SiteVpnManagerImpl.VpnCustomerGatewayExcludedDhGroup, "");

        java.util.Set<String> result = site2SiteVpnManager.getExcludedVpnGatewayParameters(gw);
        assertTrue("Should not detect excluded parameters when none are configured", result.isEmpty());
    }

    @Test
    public void testVpnGatewayContainsExcludedParametersWithExcludedEspPolicy() {
        Site2SiteCustomerGatewayVO gw = mock(Site2SiteCustomerGatewayVO.class);
        when(gw.getIkePolicy()).thenReturn("aes128-sha256;modp2048");
        when(gw.getEspPolicy()).thenReturn("3des-sha256;modp2048");
        when(gw.getIkeVersion()).thenReturn("ike");
        when(gw.getDomainId()).thenReturn(DOMAIN_ID);

        setConfigKeyValue(Site2SiteVpnManagerImpl.VpnCustomerGatewayExcludedIkeVersions, "");
        setConfigKeyValue(Site2SiteVpnManagerImpl.VpnCustomerGatewayExcludedEncryptionAlgorithms, "3des");
        setConfigKeyValue(Site2SiteVpnManagerImpl.VpnCustomerGatewayExcludedHashingAlgorithms, "");
        setConfigKeyValue(Site2SiteVpnManagerImpl.VpnCustomerGatewayExcludedDhGroup, "");

        java.util.Set<String> result = site2SiteVpnManager.getExcludedVpnGatewayParameters(gw);
        assertFalse("Should detect excluded encryption in ESP policy", result.isEmpty());
        assertEquals("Should detect excluded encryption in ESP policy", "[3des]", result.toString());
    }

    @Test
    public void testVpnGatewayContainsObsoleteParametersWithObsoleteIkeVersion() {
        Site2SiteCustomerGatewayVO gw = mock(Site2SiteCustomerGatewayVO.class);
        when(gw.getIkePolicy()).thenReturn("aes128-sha256;modp2048");
        when(gw.getEspPolicy()).thenReturn("aes128-sha256;modp2048");
        when(gw.getIkeVersion()).thenReturn("ikev1");
        when(gw.getDomainId()).thenReturn(DOMAIN_ID);

        setConfigKeyValue(Site2SiteVpnManagerImpl.VpnCustomerGatewayObsoleteIkeVersions, "ikev1");
        setConfigKeyValue(Site2SiteVpnManagerImpl.VpnCustomerGatewayObsoleteEncryptionAlgorithms, "");
        setConfigKeyValue(Site2SiteVpnManagerImpl.VpnCustomerGatewayObsoleteHashingAlgorithms, "");
        setConfigKeyValue(Site2SiteVpnManagerImpl.VpnCustomerGatewayObsoleteDhGroup, "");

        java.util.Set<String> result = site2SiteVpnManager.getObsoleteVpnGatewayParameters(gw);
        assertFalse("Should detect obsolete IKE version", result.isEmpty());
        assertEquals("Should detect obsolete IKE version", "[ikev1]", result.toString());
    }

    @Test
    public void testVpnGatewayContainsObsoleteParametersWithObsoleteEncryption() {
        Site2SiteCustomerGatewayVO gw = mock(Site2SiteCustomerGatewayVO.class);
        when(gw.getIkePolicy()).thenReturn("3des-sha256;modp2048");
        when(gw.getEspPolicy()).thenReturn("aes128-sha256;modp2048");
        when(gw.getIkeVersion()).thenReturn("ike");
        when(gw.getDomainId()).thenReturn(DOMAIN_ID);

        setConfigKeyValue(Site2SiteVpnManagerImpl.VpnCustomerGatewayObsoleteIkeVersions, "");
        setConfigKeyValue(Site2SiteVpnManagerImpl.VpnCustomerGatewayObsoleteEncryptionAlgorithms, "3des");
        setConfigKeyValue(Site2SiteVpnManagerImpl.VpnCustomerGatewayObsoleteHashingAlgorithms, "");
        setConfigKeyValue(Site2SiteVpnManagerImpl.VpnCustomerGatewayObsoleteDhGroup, "");

        java.util.Set<String> result = site2SiteVpnManager.getObsoleteVpnGatewayParameters(gw);
        assertFalse("Should detect obsolete encryption algorithm", result.isEmpty());
        assertEquals("Should detect obsolete encryption algorithm", "[3des]", result.toString());
    }

    @Test
    public void testVpnGatewayContainsObsoleteParametersWithObsoleteHashing() {
        Site2SiteCustomerGatewayVO gw = mock(Site2SiteCustomerGatewayVO.class);
        when(gw.getIkePolicy()).thenReturn("aes128-md5;modp2048");
        when(gw.getEspPolicy()).thenReturn("aes128-sha256;modp2048");
        when(gw.getIkeVersion()).thenReturn("ike");
        when(gw.getDomainId()).thenReturn(DOMAIN_ID);

        setConfigKeyValue(Site2SiteVpnManagerImpl.VpnCustomerGatewayObsoleteIkeVersions, "");
        setConfigKeyValue(Site2SiteVpnManagerImpl.VpnCustomerGatewayObsoleteEncryptionAlgorithms, "");
        setConfigKeyValue(Site2SiteVpnManagerImpl.VpnCustomerGatewayObsoleteHashingAlgorithms, "md5");
        setConfigKeyValue(Site2SiteVpnManagerImpl.VpnCustomerGatewayObsoleteDhGroup, "");

        java.util.Set<String> result = site2SiteVpnManager.getObsoleteVpnGatewayParameters(gw);
        assertFalse("Should detect obsolete hashing algorithm", result.isEmpty());
        assertEquals("Should detect obsolete hashing algorithm", "[md5]", result.toString());
    }

    @Test
    public void testVpnGatewayContainsObsoleteParametersWithObsoleteDhGroup() {
        Site2SiteCustomerGatewayVO gw = mock(Site2SiteCustomerGatewayVO.class);
        when(gw.getIkePolicy()).thenReturn("aes128-sha256;modp1024");
        when(gw.getEspPolicy()).thenReturn("aes128-sha256;modp2048");
        when(gw.getIkeVersion()).thenReturn("ike");
        when(gw.getDomainId()).thenReturn(DOMAIN_ID);

        setConfigKeyValue(Site2SiteVpnManagerImpl.VpnCustomerGatewayObsoleteIkeVersions, "");
        setConfigKeyValue(Site2SiteVpnManagerImpl.VpnCustomerGatewayObsoleteEncryptionAlgorithms, "");
        setConfigKeyValue(Site2SiteVpnManagerImpl.VpnCustomerGatewayObsoleteHashingAlgorithms, "");
        setConfigKeyValue(Site2SiteVpnManagerImpl.VpnCustomerGatewayObsoleteDhGroup, "modp1024");

        java.util.Set<String> result = site2SiteVpnManager.getObsoleteVpnGatewayParameters(gw);
        assertFalse("Should detect obsolete DH group", result.isEmpty());
        assertEquals("Should detect obsolete DH group", "[modp1024]", result.toString());
    }

    @Test
    public void testVpnGatewayContainsObsoleteParametersNoObsoleteParameters() {
        Site2SiteCustomerGatewayVO gw = mock(Site2SiteCustomerGatewayVO.class);
        when(gw.getIkePolicy()).thenReturn("aes128-sha256;modp2048");
        when(gw.getEspPolicy()).thenReturn("aes128-sha256;modp2048");
        when(gw.getIkeVersion()).thenReturn("ike");
        when(gw.getDomainId()).thenReturn(DOMAIN_ID);

        setConfigKeyValue(Site2SiteVpnManagerImpl.VpnCustomerGatewayObsoleteIkeVersions, "");
        setConfigKeyValue(Site2SiteVpnManagerImpl.VpnCustomerGatewayObsoleteEncryptionAlgorithms, "");
        setConfigKeyValue(Site2SiteVpnManagerImpl.VpnCustomerGatewayObsoleteHashingAlgorithms, "");
        setConfigKeyValue(Site2SiteVpnManagerImpl.VpnCustomerGatewayObsoleteDhGroup, "");

        java.util.Set<String> result = site2SiteVpnManager.getObsoleteVpnGatewayParameters(gw);
        assertTrue("Should not detect obsolete parameters when none are configured", result.isEmpty());
    }

    @Test
    public void testVpnGatewayContainsObsoleteParametersWithObsoleteEspPolicy() {
        Site2SiteCustomerGatewayVO gw = mock(Site2SiteCustomerGatewayVO.class);
        when(gw.getIkePolicy()).thenReturn("aes128-sha256;modp2048");
        when(gw.getEspPolicy()).thenReturn("3des-sha256;modp2048");
        when(gw.getIkeVersion()).thenReturn("ike");
        when(gw.getDomainId()).thenReturn(DOMAIN_ID);

        setConfigKeyValue(Site2SiteVpnManagerImpl.VpnCustomerGatewayObsoleteIkeVersions, "");
        setConfigKeyValue(Site2SiteVpnManagerImpl.VpnCustomerGatewayObsoleteEncryptionAlgorithms, "3des");
        setConfigKeyValue(Site2SiteVpnManagerImpl.VpnCustomerGatewayObsoleteHashingAlgorithms, "");
        setConfigKeyValue(Site2SiteVpnManagerImpl.VpnCustomerGatewayObsoleteDhGroup, "");

        java.util.Set<String> result = site2SiteVpnManager.getObsoleteVpnGatewayParameters(gw);
        assertFalse("Should detect obsolete encryption in ESP policy", result.isEmpty());
        assertEquals("Should detect obsolete encryption in ESP policy", "[3des]", result.toString());
    }
}
