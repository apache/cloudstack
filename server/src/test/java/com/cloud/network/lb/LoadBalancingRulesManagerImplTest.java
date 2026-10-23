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

package com.cloud.network.lb;

import static org.mockito.ArgumentMatchers.anyLong;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.when;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.UUID;

import org.apache.cloudstack.acl.SecurityChecker;
import org.apache.cloudstack.api.ApiConstants;
import org.apache.cloudstack.api.ServerApiException;
import org.apache.cloudstack.api.command.user.loadbalancer.UpdateLoadBalancerRuleCmd;
import org.apache.cloudstack.context.CallContext;
import org.apache.cloudstack.engine.orchestration.service.NetworkOrchestrationService;
import org.apache.cloudstack.resourcedetail.FirewallRuleDetailVO;
import org.apache.cloudstack.resourcedetail.dao.FirewallRuleDetailsDao;
import org.junit.Assert;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.Mockito;
import org.mockito.Spy;
import org.mockito.junit.MockitoJUnitRunner;
import org.springframework.test.util.ReflectionTestUtils;

import com.cloud.exception.InvalidParameterValueException;
import com.cloud.exception.ResourceUnavailableException;
import com.cloud.network.Network;
import com.cloud.network.NetworkModel;
import com.cloud.network.dao.LoadBalancerCertMapDao;
import com.cloud.network.dao.LoadBalancerCertMapVO;
import com.cloud.network.dao.LoadBalancerDao;
import com.cloud.network.dao.LoadBalancerVO;
import com.cloud.network.dao.NetworkDao;
import com.cloud.network.dao.NetworkVO;
import com.cloud.network.dao.SslCertVO;
import com.cloud.network.rules.LoadBalancer;
import com.cloud.network.vpc.VpcManager;
import com.cloud.offering.NetworkOffering;
import com.cloud.offerings.dao.NetworkOfferingServiceMapDao;
import com.cloud.user.Account;
import com.cloud.user.AccountManager;
import com.cloud.user.AccountVO;
import com.cloud.user.User;
import com.cloud.user.UserVO;
import com.cloud.uservm.UserVm;
import com.cloud.utils.db.EntityManager;
import com.cloud.utils.exception.CloudRuntimeException;
import com.cloud.utils.net.NetUtils;
import com.cloud.vm.Nic;

@RunWith(MockitoJUnitRunner.class)
public class LoadBalancingRulesManagerImplTest{

    @Mock
    NetworkDao _networkDao;

    @Mock
    NetworkOrchestrationService _networkMgr;

    @Mock
    LoadBalancerDao _lbDao;

    @Mock
    EntityManager _entityMgr;

    @Mock
    AccountManager _accountMgr;

    @Mock
    NetworkModel _networkModel;

    @Mock
    LoadBalancerCertMapDao _lbCertMapDao;

    @Mock
    NetworkOfferingServiceMapDao _networkOfferingServiceDao;

    @Mock
    NetworkVO networkMock;

    @Mock
    VpcManager vpcManager;

    @Mock
    FirewallRuleDetailsDao _firewallRuleDetailsDao;

    @Spy
    @InjectMocks
    LoadBalancingRulesManagerImpl lbr = new LoadBalancingRulesManagerImpl();

    @Mock
    LoadBalancerVO loadBalancerMock;

    private long accountId = 10L;
    private long lbRuleId = 2L;
    private long certMapRuleId = 3L;
    private long networkId = 4L;

    @Test
    public void generateCidrStringTestNullCidrList() {
        String result = lbr.generateCidrString(null);
        Assert.assertNull(result);
    }

    @Test
    public void generateCidrStringTestWithCidrList() {
        List<String> cidrList = new ArrayList<>();
        cidrList.add("1.1.1.1");
        cidrList.add("2.2.2.2/24");
        String result = lbr.generateCidrString(cidrList);
        Assert.assertEquals("1.1.1.1 2.2.2.2/24", result);
    }

    @Test (expected = ServerApiException.class)
    public void generateCidrStringTestWithInvalidCidrList() {
        List<String> cidrList = new ArrayList<>();
        cidrList.add("1.1");
        cidrList.add("2.2.2.2/24");
        String result = lbr.generateCidrString(cidrList);
        Assert.assertEquals("1.1.1.1 2.2.2.2/24", result);
    }

    @Test
    public void testGetLoadBalancerServiceProvider() {
        LoadBalancerVO loadBalancerMock = Mockito.mock(LoadBalancerVO.class);
        NetworkVO networkMock = Mockito.mock(NetworkVO.class);
        List<Network.Provider> providers = Arrays.asList(Network.Provider.VirtualRouter);

        when(loadBalancerMock.getNetworkId()).thenReturn(10L);
        when(_networkDao.findById(anyLong())).thenReturn(networkMock);
        when(_networkMgr.getProvidersForServiceInNetwork(networkMock, Network.Service.Lb)).thenReturn(providers);

        Network.Provider provider = lbr.getLoadBalancerServiceProvider(loadBalancerMock);

        Assert.assertEquals(Network.Provider.VirtualRouter, provider);
    }

    @Test(expected = CloudRuntimeException.class)
    public void testGetLoadBalancerServiceProviderFail() {
        LoadBalancerVO loadBalancerMock = Mockito.mock(LoadBalancerVO.class);
        NetworkVO networkMock = Mockito.mock(NetworkVO.class);

        when(_networkDao.findById(Mockito.any())).thenReturn(networkMock);
        when(_networkMgr.getProvidersForServiceInNetwork(networkMock, Network.Service.Lb)).thenReturn(new ArrayList<>());

        Network.Provider provider = lbr.getLoadBalancerServiceProvider(loadBalancerMock);
    }

    @Test
    public void testAssignCertToLoadBalancer() throws Exception {
        long accountId = 10L;
        long lbRuleId = 2L;
        long certId = 3L;
        long networkId = 4L;

        AccountVO account = new AccountVO("testaccount", 1L, "networkdomain", Account.Type.NORMAL, "uuid");
        account.setId(accountId);
        UserVO user = new UserVO(1, "testuser", "password", "firstname", "lastName", "email", "timezone",
                UUID.randomUUID().toString(), User.Source.UNKNOWN);
        CallContext.register(user, account);

        LoadBalancerVO loadBalancerMock = Mockito.mock(LoadBalancerVO.class);
        when(_lbDao.findById(lbRuleId)).thenReturn(loadBalancerMock);
        when(loadBalancerMock.getId()).thenReturn(lbRuleId);
        when(loadBalancerMock.getAccountId()).thenReturn(accountId);
        when(loadBalancerMock.getNetworkId()).thenReturn(networkId);
        when(loadBalancerMock.getLbProtocol()).thenReturn(NetUtils.SSL_PROTO);

        SslCertVO certVO = Mockito.mock(SslCertVO.class);
        when(_entityMgr.findById(SslCertVO.class, certId)).thenReturn(certVO);
        when(certVO.getAccountId()).thenReturn(accountId);

        LoadBalancerCertMapVO certMapRule = Mockito.mock(LoadBalancerCertMapVO.class);
        when(_lbCertMapDao.findByLbRuleId(lbRuleId)).thenReturn(certMapRule);

        Mockito.doNothing().when(_accountMgr).checkAccess(Mockito.any(Account.class), Mockito.isNull(SecurityChecker.AccessType.class), Mockito.eq(true), Mockito.any(LoadBalancerVO.class));

        Mockito.doReturn("LB").when(lbr).getLBCapability(networkId, Network.Capability.SslTermination.getName());
        Mockito.doReturn(true).when(lbr).applyLoadBalancerConfig(lbRuleId);

        lbr.assignCertToLoadBalancer(lbRuleId, certId, true);

        Mockito.verify(lbr, times(2)).applyLoadBalancerConfig(lbRuleId);
    }

    private void setupUpdateLoadBalancerRule() throws Exception{
        AccountVO account = new AccountVO("testaccount", 1L, "networkdomain", Account.Type.NORMAL, "uuid");
        account.setId(accountId);
        UserVO user = new UserVO(1, "testuser", "password", "firstname", "lastName", "email", "timezone",
                UUID.randomUUID().toString(), User.Source.UNKNOWN);
        CallContext.register(user, account);

        when(_lbDao.findById(lbRuleId)).thenReturn(loadBalancerMock);
        when(loadBalancerMock.getId()).thenReturn(lbRuleId);
        when(loadBalancerMock.getNetworkId()).thenReturn(networkId);

        when(_networkDao.findById(networkId)).thenReturn(networkMock);

        Mockito.doNothing().when(_accountMgr).checkAccess(Mockito.any(Account.class), Mockito.isNull(SecurityChecker.AccessType.class), Mockito.eq(true), Mockito.any(LoadBalancerVO.class));

        LoadBalancingRule loadBalancingRule = Mockito.mock(LoadBalancingRule.class);
        Mockito.doReturn(loadBalancingRule).when(lbr).getLoadBalancerRuleToApply(loadBalancerMock);
        Mockito.doReturn(true).when(lbr).validateLbRule(loadBalancingRule);
        Mockito.doReturn(true).when(lbr).applyLoadBalancerConfig(lbRuleId);

        when(_lbDao.update(lbRuleId, loadBalancerMock)).thenReturn(true);

        LoadBalancerCertMapVO certMapRule = Mockito.mock(LoadBalancerCertMapVO.class);
        when(_lbCertMapDao.findByLbRuleId(lbRuleId)).thenReturn(certMapRule);
        when(certMapRule.getId()).thenReturn(certMapRuleId);
    }

    @Test
    public void testUpdateLoadBalancerRule1() throws Exception {
        setupUpdateLoadBalancerRule();

        // Update protocol from TCP to SSL
        UpdateLoadBalancerRuleCmd cmd = new UpdateLoadBalancerRuleCmd();
        ReflectionTestUtils.setField(cmd, ApiConstants.ID, lbRuleId);
        ReflectionTestUtils.setField(cmd, "lbProtocol", NetUtils.SSL_PROTO);
        when(loadBalancerMock.getLbProtocol()).thenReturn(NetUtils.TCP_PROTO).thenReturn(NetUtils.SSL_PROTO);

        lbr.updateLoadBalancerRule(cmd);

        Mockito.verify(lbr, times(1)).applyLoadBalancerConfig(lbRuleId);
        Mockito.verify(_lbCertMapDao, never()).remove(anyLong());
    }

    @Test
    public void testUpdateLoadBalancerRule2() throws Exception {
        setupUpdateLoadBalancerRule();

        // Update protocol from SSL to TCP
        UpdateLoadBalancerRuleCmd cmd = new UpdateLoadBalancerRuleCmd();
        ReflectionTestUtils.setField(cmd, ApiConstants.ID, lbRuleId);
        ReflectionTestUtils.setField(cmd, "lbProtocol", NetUtils.TCP_PROTO);
        when(loadBalancerMock.getLbProtocol()).thenReturn(NetUtils.SSL_PROTO).thenReturn(NetUtils.TCP_PROTO);

        lbr.updateLoadBalancerRule(cmd);

        Mockito.verify(_lbCertMapDao, times(1)).remove(anyLong());
        Mockito.verify(lbr, times(1)).applyLoadBalancerConfig(lbRuleId);
    }

    @Test
    public void testUpdateLoadBalancerRule3() throws Exception {
        setupUpdateLoadBalancerRule();

        // Update algorithm from source to roundrobin, lb protocol is same
        UpdateLoadBalancerRuleCmd cmd = new UpdateLoadBalancerRuleCmd();
        ReflectionTestUtils.setField(cmd, ApiConstants.ID, lbRuleId);
        ReflectionTestUtils.setField(cmd, "algorithm", "roundrobin");
        ReflectionTestUtils.setField(cmd, "lbProtocol", NetUtils.SSL_PROTO);
        when(loadBalancerMock.getAlgorithm()).thenReturn("source");
        when(loadBalancerMock.getLbProtocol()).thenReturn(NetUtils.SSL_PROTO);

        lbr.updateLoadBalancerRule(cmd);

        Mockito.verify(lbr, times(1)).applyLoadBalancerConfig(lbRuleId);
        Mockito.verify(_lbCertMapDao, never()).remove(anyLong());
    }

    @Test
    public void testUpdateLoadBalancerRule4() throws Exception {
        setupUpdateLoadBalancerRule();

        // Update with same algorithm and protocol
        UpdateLoadBalancerRuleCmd cmd = new UpdateLoadBalancerRuleCmd();
        ReflectionTestUtils.setField(cmd, ApiConstants.ID, lbRuleId);
        ReflectionTestUtils.setField(cmd, "algorithm", "roundrobin");
        ReflectionTestUtils.setField(cmd, "lbProtocol", NetUtils.SSL_PROTO);
        when(loadBalancerMock.getAlgorithm()).thenReturn("roundrobin");
        when(loadBalancerMock.getLbProtocol()).thenReturn(NetUtils.SSL_PROTO);

        lbr.updateLoadBalancerRule(cmd);

        Mockito.verify(lbr, never()).applyLoadBalancerConfig(lbRuleId);
        Mockito.verify(_lbCertMapDao, never()).remove(anyLong());
    }

    @Test(expected = CloudRuntimeException.class)
    public void testUpdateLoadBalancerRule5() throws Exception {
        setupUpdateLoadBalancerRule();

        // Update protocol from SSL to TCP, throws an exception
        UpdateLoadBalancerRuleCmd cmd = new UpdateLoadBalancerRuleCmd();
        ReflectionTestUtils.setField(cmd, ApiConstants.ID, lbRuleId);
        ReflectionTestUtils.setField(cmd, "lbProtocol", NetUtils.TCP_PROTO);
        when(loadBalancerMock.getLbProtocol()).thenReturn(NetUtils.SSL_PROTO).thenReturn(NetUtils.TCP_PROTO);
        Mockito.doThrow(ResourceUnavailableException.class).when(lbr).applyLoadBalancerConfig(lbRuleId);

        List<Network.Provider> providers = Arrays.asList(Network.Provider.VirtualRouter);
        when(_networkDao.findById(anyLong())).thenReturn(networkMock);
        when(_networkMgr.getProvidersForServiceInNetwork(networkMock, Network.Service.Lb)).thenReturn(providers);

        lbr.updateLoadBalancerRule(cmd);

        Mockito.verify(_lbCertMapDao, never()).remove(anyLong());
        Mockito.verify(lbr, times(1)).applyLoadBalancerConfig(lbRuleId);
        Mockito.verify(loadBalancerMock, times(1)).setLbProtocol(NetUtils.TCP_PROTO);
        Mockito.verify(loadBalancerMock, times(1)).setLbProtocol(NetUtils.SSL_PROTO);
    }

    @Test
    public void testGetVmNicInLoadBalancerDefaultCase() {
        UserVm userVm = Mockito.mock(UserVm.class);
        LoadBalancerVO loadBalancer = Mockito.mock(LoadBalancerVO.class);
        Network loadBalancerNetwork = Mockito.mock(Network.class);
        Account owner = Mockito.mock(Account.class);

        when(vpcManager.isNetworkOnVpcEnabledConserveMode(Mockito.eq(loadBalancerNetwork))).thenReturn(false);

        when(loadBalancer.getNetworkId()).thenReturn(networkId);
        Nic nic = Mockito.mock(Nic.class);
        when(nic.getNetworkId()).thenReturn(networkId);
        List<? extends Nic> nics = Collections.singletonList(nic);
        Mockito.doReturn(nics).when(_networkModel).getNics(anyLong());
        Nic nicInLb = lbr.getVmNicInLoadBalancer(userVm, loadBalancer, loadBalancerNetwork, null, owner);
        Assert.assertEquals(nic, nicInLb);
    }

    @Test
    public void testGetVmNicInLoadBalancerVPCConserveMode() {
        long vmId = 30L;
        UserVm userVm = Mockito.mock(UserVm.class);
        when(userVm.getId()).thenReturn(vmId);
        LoadBalancerVO loadBalancer = Mockito.mock(LoadBalancerVO.class);
        Network loadBalancerNetwork = Mockito.mock(Network.class);
        Account owner = Mockito.mock(Account.class);

        long networkTier2Id = 20L;
        NetworkVO networkTier2 = Mockito.mock(NetworkVO.class);
        Map<Long, Long> vmIdNetworkIdMap = new HashMap<>();
        vmIdNetworkIdMap.put(vmId, networkTier2Id);

        when(vpcManager.isNetworkOnVpcEnabledConserveMode(Mockito.eq(loadBalancerNetwork))).thenReturn(true);
        when(_networkDao.findById(networkTier2Id)).thenReturn(networkTier2);
        when(networkTier2.getVpcId()).thenReturn(10L);
        when(loadBalancerNetwork.getVpcId()).thenReturn(10L);
        Nic nic = Mockito.mock(Nic.class);
        when(_networkModel.getNicInNetwork(Mockito.eq(vmId), Mockito.eq(networkTier2Id))).thenReturn(nic);

        Nic nicInLb = lbr.getVmNicInLoadBalancer(userVm, loadBalancer, loadBalancerNetwork, vmIdNetworkIdMap, owner);
        Assert.assertEquals(nic, nicInLb);
    }

    @Test(expected = InvalidParameterValueException.class)
    public void createPublicLoadBalancerRuleWithDnsPortAndNoIpDoesNotNpe() throws Exception {
        long lbOwnerId = accountId;
        long networkOfferingId = 7L;
        when(_accountMgr.getAccount(lbOwnerId)).thenReturn(Mockito.mock(Account.class));
        when(_networkModel.getNetwork(networkId)).thenReturn(networkMock);
        when(networkMock.getNetworkOfferingId()).thenReturn(networkOfferingId);
        NetworkOffering off = Mockito.mock(NetworkOffering.class);
        when(_entityMgr.findById(NetworkOffering.class, networkOfferingId)).thenReturn(off);
        when(off.isElasticLb()).thenReturn(false);

        lbr.createPublicLoadBalancerRule("xid", "name", "desc", 53, 53, 53, 53,
                null, "tcp", "roundrobin", networkId, lbOwnerId, false, "tcp", null, null);
    }

    @Test
    public void testValidateConnectionTimeoutAcceptsZeroAndAbove() {
        lbr.validateConnectionTimeout(ApiConstants.IDLE_TIMEOUT, null);
        lbr.validateConnectionTimeout(ApiConstants.IDLE_TIMEOUT, 0L);
        lbr.validateConnectionTimeout(ApiConstants.IDLE_TIMEOUT, 600000L);
    }

    @Test(expected = InvalidParameterValueException.class)
    public void testValidateConnectionTimeoutRejectsNegative() {
        lbr.validateConnectionTimeout(ApiConstants.IDLE_TIMEOUT, -1L);
    }

    private void stubConnectionSettings(String keepAlive, String idleTimeout, String keepAliveTimeout) {
        stubConnectionSetting(LoadBalancer.KEEPALIVE, keepAlive);
        stubConnectionSetting(LoadBalancer.IDLE_TIMEOUT, idleTimeout);
        stubConnectionSetting(LoadBalancer.KEEPALIVE_TIMEOUT, keepAliveTimeout);
    }

    private void stubConnectionSetting(String key, String value) {
        when(_firewallRuleDetailsDao.findDetail(lbRuleId, key))
                .thenReturn(value == null ? null : new FirewallRuleDetailVO(lbRuleId, key, value, true));
    }

    private void verifyNoConnectionSettingWrites() {
        Mockito.verify(_firewallRuleDetailsDao, never()).removeDetail(anyLong(), Mockito.anyString());
        Mockito.verify(_firewallRuleDetailsDao, never()).addDetail(anyLong(), Mockito.anyString(), Mockito.anyString(), Mockito.anyBoolean());
    }

    @Test
    public void testConnectionSettingsCleanupResetsAllToInherit() {
        stubConnectionSettings("true", "5000", "1000");

        Assert.assertTrue(lbr.updateLoadBalancerConnectionSettings(lbRuleId, null, null, null, true));

        Mockito.verify(_firewallRuleDetailsDao).removeDetail(lbRuleId, LoadBalancer.KEEPALIVE);
        Mockito.verify(_firewallRuleDetailsDao).removeDetail(lbRuleId, LoadBalancer.IDLE_TIMEOUT);
        Mockito.verify(_firewallRuleDetailsDao).removeDetail(lbRuleId, LoadBalancer.KEEPALIVE_TIMEOUT);
        Mockito.verify(_firewallRuleDetailsDao, never()).addDetail(anyLong(), Mockito.anyString(), Mockito.anyString(), Mockito.anyBoolean());
    }

    @Test
    public void testConnectionSettingsCleanupKeepsValuesPassedAlongside() {
        stubConnectionSettings("true", "5000", "1000");

        Assert.assertTrue(lbr.updateLoadBalancerConnectionSettings(lbRuleId, null, 5000L, null, true));

        Mockito.verify(_firewallRuleDetailsDao).removeDetail(lbRuleId, LoadBalancer.KEEPALIVE);
        Mockito.verify(_firewallRuleDetailsDao).removeDetail(lbRuleId, LoadBalancer.KEEPALIVE_TIMEOUT);
        Mockito.verify(_firewallRuleDetailsDao, never()).removeDetail(lbRuleId, LoadBalancer.IDLE_TIMEOUT);
        Mockito.verify(_firewallRuleDetailsDao, never()).addDetail(anyLong(), Mockito.anyString(), Mockito.anyString(), Mockito.anyBoolean());
    }

    @Test
    public void testConnectionSettingsCleanupWithNothingSetIsNoChange() {
        stubConnectionSettings(null, null, null);

        Assert.assertFalse(lbr.updateLoadBalancerConnectionSettings(lbRuleId, null, null, null, true));

        verifyNoConnectionSettingWrites();
    }

    @Test
    public void testConnectionSettingsNullKeepsCurrentValue() {
        stubConnectionSettings(null, "5000", null);

        Assert.assertTrue(lbr.updateLoadBalancerConnectionSettings(lbRuleId, null, null, 2000L, false));

        Mockito.verify(_firewallRuleDetailsDao).addDetail(lbRuleId, LoadBalancer.KEEPALIVE_TIMEOUT, "2000", true);
        Mockito.verify(_firewallRuleDetailsDao, never()).removeDetail(anyLong(), Mockito.anyString());
    }

    @Test
    public void testConnectionSettingsSameValueIsNoChange() {
        stubConnectionSettings(null, "5000", null);

        Assert.assertFalse(lbr.updateLoadBalancerConnectionSettings(lbRuleId, null, 5000L, null, false));

        verifyNoConnectionSettingWrites();
    }

    @Test
    public void testConnectionSettingsChangedValueIsReplaced() {
        stubConnectionSettings("false", null, null);

        Assert.assertTrue(lbr.updateLoadBalancerConnectionSettings(lbRuleId, true, null, null, false));

        Mockito.verify(_firewallRuleDetailsDao).removeDetail(lbRuleId, LoadBalancer.KEEPALIVE);
        Mockito.verify(_firewallRuleDetailsDao).addDetail(lbRuleId, LoadBalancer.KEEPALIVE, "true", true);
    }

    @Test
    public void testUpdateLoadBalancerRuleCleanupReappliesConfig() throws Exception {
        setupUpdateLoadBalancerRule();
        stubConnectionSettings(null, "5000", null);

        // Only the connection settings change, algorithm and protocol are the same
        UpdateLoadBalancerRuleCmd cmd = new UpdateLoadBalancerRuleCmd();
        ReflectionTestUtils.setField(cmd, ApiConstants.ID, lbRuleId);
        ReflectionTestUtils.setField(cmd, "algorithm", "roundrobin");
        ReflectionTestUtils.setField(cmd, "lbProtocol", NetUtils.SSL_PROTO);
        ReflectionTestUtils.setField(cmd, "cleanupConnectionSettings", true);
        when(loadBalancerMock.getAlgorithm()).thenReturn("roundrobin");
        when(loadBalancerMock.getLbProtocol()).thenReturn(NetUtils.SSL_PROTO);

        lbr.updateLoadBalancerRule(cmd);

        Mockito.verify(_firewallRuleDetailsDao).removeDetail(lbRuleId, LoadBalancer.IDLE_TIMEOUT);
        Mockito.verify(lbr, times(1)).applyLoadBalancerConfig(lbRuleId);
    }

    /**
     * Backs the details dao with a map, so a value written and then rolled back can be seen.
     */
    private Map<String, String> fakeConnectionSettings(Map<String, String> initial) {
        Map<String, String> store = new HashMap<>(initial);
        when(_firewallRuleDetailsDao.findDetail(Mockito.eq(lbRuleId), Mockito.anyString())).thenAnswer(invocation -> {
            String key = invocation.getArgument(1);
            String value = store.get(key);
            return value == null ? null : new FirewallRuleDetailVO(lbRuleId, key, value, true);
        });
        Mockito.doAnswer(invocation -> store.remove(invocation.<String>getArgument(1)))
                .when(_firewallRuleDetailsDao).removeDetail(Mockito.eq(lbRuleId), Mockito.anyString());
        Mockito.doAnswer(invocation -> store.put(invocation.getArgument(1), invocation.getArgument(2)))
                .when(_firewallRuleDetailsDao).addDetail(Mockito.eq(lbRuleId), Mockito.anyString(), Mockito.anyString(), Mockito.anyBoolean());
        return store;
    }

    @Test
    public void testUpdateLoadBalancerRuleRollsBackSettingsWhenApplyFails() throws Exception {
        setupUpdateLoadBalancerRule();
        Map<String, String> before = Map.of(LoadBalancer.KEEPALIVE, "true", LoadBalancer.IDLE_TIMEOUT, "5000");
        Map<String, String> store = fakeConnectionSettings(before);

        // Drop keepalive, change idletimeout, add keepalivetimeout, then fail to reach the router
        UpdateLoadBalancerRuleCmd cmd = new UpdateLoadBalancerRuleCmd();
        ReflectionTestUtils.setField(cmd, ApiConstants.ID, lbRuleId);
        ReflectionTestUtils.setField(cmd, "algorithm", "roundrobin");
        ReflectionTestUtils.setField(cmd, "lbProtocol", NetUtils.SSL_PROTO);
        ReflectionTestUtils.setField(cmd, "idleTimeout", 2000L);
        ReflectionTestUtils.setField(cmd, "keepAliveTimeout", 1000L);
        ReflectionTestUtils.setField(cmd, "cleanupConnectionSettings", true);
        when(loadBalancerMock.getAlgorithm()).thenReturn("roundrobin");
        when(loadBalancerMock.getLbProtocol()).thenReturn(NetUtils.SSL_PROTO);
        Mockito.doThrow(ResourceUnavailableException.class).when(lbr).applyLoadBalancerConfig(lbRuleId);
        when(_networkMgr.getProvidersForServiceInNetwork(networkMock, Network.Service.Lb))
                .thenReturn(Collections.singletonList(Network.Provider.VirtualRouter));

        try {
            lbr.updateLoadBalancerRule(cmd);
            Assert.fail("Expected the update to fail when the router is unavailable");
        } catch (CloudRuntimeException e) {
            // expected
        }

        Mockito.verify(lbr, times(1)).applyLoadBalancerConfig(lbRuleId);
        Assert.assertEquals(before, store);
    }

    @Test
    public void testUpdateLoadBalancerRuleLeavesSettingsAloneWhenValidationFails() {
        AccountVO account = new AccountVO("testaccount", 1L, "networkdomain", Account.Type.NORMAL, "uuid");
        account.setId(accountId);
        UserVO user = new UserVO(1, "testuser", "password", "firstname", "lastName", "email", "timezone",
                UUID.randomUUID().toString(), User.Source.UNKNOWN);
        CallContext.register(user, account);

        when(_lbDao.findById(lbRuleId)).thenReturn(loadBalancerMock);
        when(loadBalancerMock.getNetworkId()).thenReturn(networkId);
        when(_networkDao.findById(networkId)).thenReturn(networkMock);
        LoadBalancingRule loadBalancingRule = Mockito.mock(LoadBalancingRule.class);
        Mockito.doReturn(loadBalancingRule).when(lbr).getLoadBalancerRuleToApply(loadBalancerMock);
        Mockito.doReturn(false).when(lbr).validateLbRule(loadBalancingRule);

        UpdateLoadBalancerRuleCmd cmd = new UpdateLoadBalancerRuleCmd();
        ReflectionTestUtils.setField(cmd, ApiConstants.ID, lbRuleId);
        ReflectionTestUtils.setField(cmd, "idleTimeout", 2000L);

        try {
            lbr.updateLoadBalancerRule(cmd);
            Assert.fail("Expected the provider to reject the update");
        } catch (InvalidParameterValueException e) {
            // expected
        }

        verifyNoConnectionSettingWrites();
    }
}
