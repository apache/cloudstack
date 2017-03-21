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
package org.apache.cloudstack.network.lb;

import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertTrue;
import static org.mockito.Matchers.any;
import static org.mockito.Matchers.anyBoolean;
import static org.mockito.Matchers.anyLong;
import static org.mockito.Matchers.matches;
import static org.mockito.Mockito.reset;
import static org.mockito.Mockito.when;

import java.lang.reflect.Field;
import java.net.URI;
import java.net.URISyntaxException;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.Map;

import javax.inject.Inject;

import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.Mockito;
import org.mockito.invocation.InvocationOnMock;
import org.springframework.test.context.ContextConfiguration;
import org.springframework.test.context.junit4.SpringJUnit4ClassRunner;

import com.google.common.base.Predicate;
import com.google.common.collect.Collections2;
import com.google.common.collect.Lists;
import com.google.common.collect.Maps;

import org.apache.cloudstack.acl.ControlledEntity;
import org.apache.cloudstack.api.InternalIdentity;
import org.apache.cloudstack.engine.orchestration.service.NetworkOrchestrationService;
import org.apache.cloudstack.framework.config.dao.ConfigurationDao;
import org.apache.cloudstack.lb.ApplicationLoadBalancerRuleVO;
import org.apache.cloudstack.network.VpcInlineLbMappingVO;
import org.apache.cloudstack.network.dao.VpcInlineLbMappingDao;
import org.apache.cloudstack.resourcedetail.dao.VpcDetailsDao;

import com.cloud.agent.AgentManager;
import com.cloud.agent.api.Answer;
import com.cloud.agent.manager.Commands;
import com.cloud.dc.DataCenter.NetworkType;
import com.cloud.dc.DataCenterVO;
import com.cloud.dc.dao.DataCenterDao;
import com.cloud.deploy.DeployDestination;
import com.cloud.exception.ConcurrentOperationException;
import com.cloud.exception.InsufficientAddressCapacityException;
import com.cloud.exception.InsufficientCapacityException;
import com.cloud.exception.InvalidParameterValueException;
import com.cloud.exception.ResourceUnavailableException;
import com.cloud.hypervisor.Hypervisor.HypervisorType;
import com.cloud.network.IpAddressManager;
import com.cloud.network.Network;
import com.cloud.network.NetworkModel;
import com.cloud.network.Networks;
import com.cloud.network.VirtualRouterProvider;
import com.cloud.network.dao.IPAddressDao;
import com.cloud.network.dao.IPAddressVO;
import com.cloud.network.dao.LoadBalancerDao;
import com.cloud.network.dao.NetworkDao;
import com.cloud.network.dao.NetworkVO;
import com.cloud.network.dao.PhysicalNetworkServiceProviderDao;
import com.cloud.network.dao.PhysicalNetworkServiceProviderVO;
import com.cloud.network.dao.VirtualRouterProviderDao;
import com.cloud.network.element.IpDeployer;
import com.cloud.network.element.StaticNatServiceProvider;
import com.cloud.network.element.VirtualRouterProviderVO;
import com.cloud.network.lb.LoadBalancingRule;
import com.cloud.network.router.VirtualRouter;
import com.cloud.network.router.VirtualRouter.Role;
import com.cloud.network.rules.FirewallRule;
import com.cloud.network.rules.LoadBalancerContainer.Scheme;
import com.cloud.network.vpc.NetworkACL;
import com.cloud.network.vpc.NetworkACLManager;
import com.cloud.network.vpc.Vpc;
import com.cloud.network.vpc.VpcVO;
import com.cloud.network.vpc.dao.VpcDao;
import com.cloud.offering.NetworkOffering;
import com.cloud.offerings.NetworkOfferingVO;
import com.cloud.offerings.dao.NetworkOfferingDao;
import com.cloud.service.ServiceOfferingVO;
import com.cloud.service.dao.ServiceOfferingDao;
import com.cloud.storage.Storage;
import com.cloud.user.AccountManager;
import com.cloud.user.AccountVO;
import com.cloud.utils.component.ComponentContext;
import com.cloud.utils.exception.CloudRuntimeException;
import com.cloud.utils.net.Ip;
import com.cloud.vm.DomainRouterVO;
import com.cloud.vm.NicProfile;
import com.cloud.vm.NicVO;
import com.cloud.vm.VirtualMachine;
import com.cloud.vm.VirtualMachine.State;
import com.cloud.vm.VirtualMachineManager;
import com.cloud.vm.dao.DomainRouterDao;
import com.cloud.vm.dao.NicDao;
import com.cloud.vm.dao.NicSecondaryIpDao;
import com.cloud.vm.dao.NicSecondaryIpVO;

@RunWith(SpringJUnit4ClassRunner.class)
@ContextConfiguration(locations="classpath:/lb_mgr.xml")
public class VpcInlineLoadBalancerVmManagerTest {
    public static final String IP_REGEX = "\\d{1,3}(\\.\\d{1,3}){3}";
    //The interface to test
    @Inject
    private VpcInlineLoadBalancerVmManager _lbVmMgr;

    //Mocked interfaces
    @Inject
    private AccountManager _accountMgr;
    @Inject
    private ServiceOfferingDao _svcOffDao;
    @Inject
    private DomainRouterDao _domainRouterDao;
    @Inject
    private NicDao _nicDao;
    @Inject
    private AgentManager _agentMgr;
    @Inject
    private NetworkModel _ntwkModel;
    @Inject
    private NetworkACLManager _ntwkAclMgr;
    @Inject
    private VirtualMachineManager _itMgr;
    @Inject
    private DataCenterDao _dcDao;
    @Inject
    private NetworkOfferingDao _offeringDao;
    @Inject
    private VpcInlineLbMappingDao _vpcInlineLbMappingDao;
    @Inject
    private IPAddressDao _ipAddressDao;
    @Inject
    private NetworkOrchestrationService _ntwkMgr;
    @Inject
    private NetworkDao _ntwkDao;
    @Inject
    private VpcDao _vpcDao;
    @Inject
    private VpcDetailsDao _vpcDetailsDao;
    @Inject
    private LoadBalancerDao _lbDao;
    @Inject
    private PhysicalNetworkServiceProviderDao _physicalProviderDao;
    @Inject
    private VirtualRouterProviderDao _vrProviderDao;
    @Inject
    private NicSecondaryIpDao _nicSecondaryIpDao;
    @Inject
    private IpAddressManager _ipAddrMgr;
    @Inject
    private ConfigurationDao _configurationDao;

    private long validNtwkId = 1L;
    private long invalidNtwkId = 2L;
    private String requestedPublicIp = "172.1.1.1";
    private String invalidPublicIp = "172.1.1.2";
    private String lbGuestIp = "10.1.1.1";
    private String lbSecondaryIp = "10.1.1.2";
    private String lbSecondaryIp2 = "10.1.1.3";

    private long lbSecondaryIpId = 20L;

    private DomainRouterVO vm = null;
    private NetworkVO ntwk = createNetwork();
    private VpcVO vpc = createVpc();
    private long validVmId = 1L;
    private long invalidVmId = 2L;
    private long validVmNicId = 1L;
    private long nonVpcInlineLbVmId = 3L;

    private Map<Ip, String> publicIpGuestIpMapping = Maps.newHashMap();

    private static final AccountVO ACCOUNT = new AccountVO(1L);

    private static final DataCenterVO dc = new DataCenterVO
            (1L, null, null, null, null, null, null, null, null, null, NetworkType.Advanced, null, null);

    @Before
    public void setUp() throws InsufficientAddressCapacityException {
        //mock system offering creation as it's used by configure() method called by initComponentsLifeCycle
        when(_accountMgr.getAccount(1L)).thenReturn(ACCOUNT);
        ServiceOfferingVO off = new ServiceOfferingVO("alena", 1, 1,
                1, 1, 1, false, "alena", Storage.ProvisioningType.THIN, false, false, null, false, VirtualMachine.Type.VpcInlineLoadBalancerVm, false);
        off = setId(off, 1);
        when(_svcOffDao.persistSystemServiceOffering(any(ServiceOfferingVO.class))).thenReturn(off);

        ComponentContext.initComponentsLifeCycle();

        vm = new DomainRouterVO(1L, off.getId(), 1, "alena", 1, HypervisorType.XenServer, 1, 1, 1, 1,
                false, VirtualRouter.RedundantState.UNKNOWN, false, false, VirtualMachine.Type.VpcInlineLoadBalancerVm, null);
        vm.setRole(Role.LB);
        vm = setId(vm, validVmId);
        vm.setPrivateIpAddress("10.2.2.2");
        NicVO nic = new NicVO("somereserver", 1L, 1L, VirtualMachine.Type.VpcInlineLoadBalancerVm);
        nic.setIPv4Address(lbGuestIp);
        nic.setNetworkId(validNtwkId);
        nic = setId(nic, 1L);

        List<DomainRouterVO> emptyList = new ArrayList<DomainRouterVO>();
        List<DomainRouterVO> nonEmptyList = new ArrayList<DomainRouterVO>();
        nonEmptyList.add(vm);

        when(_domainRouterDao.listByNetworkAndRole(invalidNtwkId, Role.LB)).thenReturn(emptyList);
        when(_domainRouterDao.listByNetworkAndRole(validNtwkId, Role.LB)).thenReturn(nonEmptyList);

        when(_nicDao.findByNtwkIdAndInstanceId(validNtwkId, 1)).thenReturn(nic);
        when(_nicDao.findByNtwkIdAndInstanceId(invalidNtwkId, 1)).thenReturn(nic);
        when(_nicDao.findById(validVmNicId)).thenReturn(nic);
        when(_nicDao.listByVmId(validVmId)).thenReturn(Arrays.asList(nic));

        VpcInlineLbMappingVO mapping = new VpcInlineLbMappingVO(111L, validVmId, validVmNicId, lbSecondaryIpId);
        List<VpcInlineLbMappingVO> mappings = Arrays.asList(mapping);

        when(_vpcInlineLbMappingDao.findByPublicIpAddress(111L)).thenReturn(mapping);
        when(_vpcInlineLbMappingDao.findByNicSecondaryIp(lbSecondaryIpId)).thenReturn(mapping);
        when(_vpcInlineLbMappingDao.listByVmId(validVmNicId)).thenReturn(mappings);
        when(_vpcInlineLbMappingDao.listByNicId(validVmNicId)).thenReturn(mappings);

        when(_ipAddressDao.findByIpAndDcId(anyLong(), matches(IP_REGEX))).thenAnswer(new org.mockito.stubbing.Answer<IPAddressVO>() {
            @Override
            public IPAddressVO answer(InvocationOnMock invocationOnMock) throws Throwable {
                long dcId = (Long)invocationOnMock.getArguments()[0];
                String publicIp = (String)invocationOnMock.getArguments()[1];
                return new IPAddressVO(new Ip(publicIp), dcId, 0L, 0L, false);
            }
        });

        when(_ipAddressDao.findById(anyLong())).thenAnswer(new org.mockito.stubbing.Answer<IPAddressVO>() {
            @Override
            public IPAddressVO answer(InvocationOnMock invocationOnMock) throws Throwable {
                long id = (Long)invocationOnMock.getArguments()[0];
                String publicIp = "172.1.1." + id;
                IPAddressVO result = new IPAddressVO(new Ip(publicIp), 1L, 0L, 0L, false);
                result.setAllocatedToAccountId(ACCOUNT.getAccountId());
                result.setAllocatedInDomainId(ACCOUNT.getDomainId());
                result = setId(result, id);
                if (publicIp.equals(requestedPublicIp)) {
                    result.setVmIp(lbSecondaryIp);
                }
                return result;
            }
        });

        when(_ipAddressDao.findByIpAndNetworkId(anyLong(), matches(IP_REGEX))).thenAnswer(new org.mockito.stubbing.Answer<IPAddressVO>() {
            @Override
            public IPAddressVO answer(InvocationOnMock invocationOnMock) throws Throwable {
                long ntwkId = (Long)invocationOnMock.getArguments()[0];
                String publicIp = (String)invocationOnMock.getArguments()[1];
                IPAddressVO result = new IPAddressVO(new Ip(publicIp), 1L, 0L, 0L, false);
                result.setAssociatedWithNetworkId(ntwkId);
                result.setAssociatedWithVmId(1L);
                result = setId(result, Long.parseLong(publicIp.substring(publicIp.lastIndexOf(".") + 1)));
                if (publicIp.equals(requestedPublicIp)) {
                    result.setVmIp(lbSecondaryIp);
                }
                return result;
            }
        });

        NicSecondaryIpVO secondaryIp = new NicSecondaryIpVO(20L, lbSecondaryIp, 1L, ACCOUNT.getAccountId(), ACCOUNT.getDomainId(), validNtwkId);

        when(_nicSecondaryIpDao.findByIp4AddressAndNicId(lbSecondaryIp, 1L)).thenReturn(secondaryIp);
        when(_nicSecondaryIpDao.findById(lbSecondaryIpId)).thenReturn(secondaryIp);
        when(_nicSecondaryIpDao.persist(any(NicSecondaryIpVO.class))).thenAnswer(new org.mockito.stubbing.Answer<Object>() {
            long count = 2L;
            @Override
            public Object answer(InvocationOnMock invocationOnMock) throws Throwable {
                NicSecondaryIpVO entity = (NicSecondaryIpVO)invocationOnMock.getArguments()[0];
                if (entity != null) {
                    long id = count++;
                    entity = setId(entity, id);
                    when(_nicSecondaryIpDao.findById(id)).thenReturn(entity);
                }
                return entity;
            }
        });

        Answer answer = new Answer(null, true, null);
        Answer[] answers = new Answer[1];
        answers[0] = answer;

        try {
            when(_agentMgr.send(anyLong(), any(Commands.class))).thenReturn(answers);
        } catch (Exception e) {
        }

        when(_vpcDao.acquireInLockTable(anyLong(), Mockito.anyInt())).thenReturn(vpc);
        when(_vpcDao.findById(anyLong())).thenReturn(vpc);

        when(_ntwkModel.getNetwork(anyLong())).thenReturn(ntwk);
        when(_ntwkDao.acquireInLockTable(anyLong(), Mockito.anyInt())).thenReturn(ntwk);

        final StaticNatServiceProvider staticNatServiceProvider = Mockito.mock(StaticNatServiceProvider.class);
        when(staticNatServiceProvider.getIpDeployer(any(Network.class))).thenReturn( Mockito.mock(IpDeployer.class));
        when(_ntwkMgr.getStaticNatProviderForNetwork(any(Network.class))).thenReturn(staticNatServiceProvider);

        reset(_ntwkAclMgr);
        when(_ntwkAclMgr.createNetworkACL(Mockito.anyString(), Mockito.anyString(), anyLong(), anyBoolean())).thenAnswer(new org.mockito.stubbing.Answer<NetworkACL>() {
            long count = 2L;

            @Override
            public NetworkACL answer(InvocationOnMock invocationOnMock) throws Throwable {
                String name = (String) invocationOnMock.getArguments()[0];
                String description = (String) invocationOnMock.getArguments()[1];
                Long vpcId = (Long) invocationOnMock.getArguments()[2];
                long id = count++;

                NetworkACL entity = Mockito.mock(NetworkACL.class);
                when(entity.getName()).thenReturn(name);
                when(entity.getDescription()).thenReturn(description);
                when(entity.getId()).thenReturn(id);
                when(entity.getVpcId()).thenReturn(vpcId);

                when(_ntwkAclMgr.getNetworkACL(id)).thenReturn(entity);

                return entity;
            }
        });

        when(_itMgr.toNicTO(any(NicProfile.class), any(HypervisorType.class))).thenReturn(null);
        when(_domainRouterDao.findById(anyLong())).thenReturn(vm);

        when(_dcDao.findById(anyLong())).thenReturn(dc);
        final NetworkOfferingVO networkOfferingVO = new NetworkOfferingVO();
        networkOfferingVO.setConcurrentConnections(500);
        when(_offeringDao.findById(anyLong())).thenReturn(networkOfferingVO);
        when(_ntwkModel.getSystemAccountNetworkOfferings(NetworkOffering.SystemControlNetwork)).thenAnswer(new org.mockito.stubbing.Answer<List<NetworkOfferingVO>>() {
            @Override
            public List<NetworkOfferingVO> answer(InvocationOnMock invocationOnMock) throws Throwable {
                return Lists.newArrayList(networkOfferingVO);
            }
        });

        when(_ipAddrMgr.allocateGuestIP(createNetwork(), null)).thenReturn(lbSecondaryIp2);

        when(_domainRouterDao.findById(validVmId)).thenReturn(vm);
        when(_domainRouterDao.findById(invalidVmId)).thenReturn(null);
        when(_domainRouterDao.listByNetworkAndRole(validNtwkId, Role.LB)).thenReturn(Arrays.asList(vm));

        DomainRouterVO nonVpcInlineLbVm = new DomainRouterVO(validVmId, off.getId(), 1, "alena", 1, HypervisorType.XenServer, 1, 1, 1, 1,
                false, VirtualRouter.RedundantState.UNKNOWN, false, false, VirtualMachine.Type.DomainRouter, null);
        nonVpcInlineLbVm.setRole(Role.VIRTUAL_ROUTER);
        when(_domainRouterDao.findById(nonVpcInlineLbVmId)).thenReturn(nonVpcInlineLbVm);

        PhysicalNetworkServiceProviderVO phnp = new PhysicalNetworkServiceProviderVO(1L, "VpcInlineLbVm");
        phnp = setId(phnp, 1L);
        VirtualRouterProviderVO vrp = new VirtualRouterProviderVO(1L, VirtualRouterProvider.Type.VpcInlineLbVm);
        vrp = setId(vrp, 1L);
        when(_physicalProviderDao.findByServiceProvider(0L, "VpcInlineLbVm")).thenReturn(phnp);
        when(_vrProviderDao.findByNspIdAndType(1L, VirtualRouterProvider.Type.VpcInlineLbVm)).thenReturn(vrp);

        publicIpGuestIpMapping.put(new Ip(requestedPublicIp), lbSecondaryIp);
    }

    protected VpcVO createVpc() {
        VpcVO vpc = new VpcVO();
        vpc.setName("test");
        vpc.setState(Vpc.State.Enabled);
        vpc = setId(vpc, 1L);
        return vpc;
    }

    protected NetworkVO createNetwork() {
        ntwk = new NetworkVO(validNtwkId, Networks.TrafficType.Guest, Networks.Mode.Dhcp, Networks.BroadcastDomainType.Vsp, 1L,
                ACCOUNT.getDomainId(), ACCOUNT.getAccountId(), 0, "", "", "", Network.GuestType.Isolated, dc.getId(),
                1L, ControlledEntity.ACLType.Account, true, 1L, true);
        try {
            ntwk.setBroadcastUri(new URI("somevlan"));
            ntwk.setCidr("10.1.1.0/24");
            ntwk.setGateway("10.1.1.254");
            ntwk.setState(Network.State.Implemented);
        } catch (URISyntaxException e) {
            throw new CloudRuntimeException("Failed to create network");
        }
        ntwk = setId(ntwk, 1L);
        return ntwk;
    }

    @Test
    public void findVpcInlineLbVmsForInvalidNetwork() {
        List<? extends VirtualRouter> vms = _lbVmMgr.findVpcInlineLbVms(new NetworkVO(), new Ip(requestedPublicIp));
        assertTrue("Non empty vm list was returned for invalid network id", vms.isEmpty());
    }

    @Test
    public void findVpcInlineLbVmsForInvalidPublicIp() {
        List<? extends VirtualRouter> vms = _lbVmMgr.findVpcInlineLbVms(ntwk, new Ip(invalidPublicIp));
        assertTrue("Non empty vm list was returned for invalid network id", vms.isEmpty());
    }

    @Test
    public void findVpcInlineLbVmsForValidNetwork() {
        List<? extends VirtualRouter> vms = _lbVmMgr.findVpcInlineLbVms(ntwk, null);
        assertTrue("Empty vm list was returned for valid network id", !vms.isEmpty());
    }

    @Test
    public void applyEmptyRulesSet() {
        boolean result = false;
        try {
            result = _lbVmMgr.applyLoadBalancingRules(new NetworkVO(), new ArrayList<LoadBalancingRule>(), vm, publicIpGuestIpMapping);
        } catch (ResourceUnavailableException e) {
        } finally {
            assertTrue("Got failure when tried to apply empty list of rules", result);
        }
    }

    @Test (expected = CloudRuntimeException.class)
    public void applyWithEmptyVmsSet() {
        boolean result = false;
        List<LoadBalancingRule> rules = new ArrayList<LoadBalancingRule>();
        LoadBalancingRule rule = new LoadBalancingRule(null, null, null, null, null, null, null);

        rules.add(rule);
        try {
            result = _lbVmMgr.applyLoadBalancingRules(new NetworkVO(), rules, null, publicIpGuestIpMapping);
        } catch (ResourceUnavailableException e) {
        } finally {
            assertFalse("Got success when tried to apply with the empty vpcInline lb vm list", result);
        }
    }

    @Test (expected = ResourceUnavailableException.class)
    public void applyToVmInStartingState() throws ResourceUnavailableException {
        boolean result = false;
        vm.setState(State.Starting);

        List<LoadBalancingRule> rules = new ArrayList<LoadBalancingRule>();
        LoadBalancingRule rule = new LoadBalancingRule(null, null, null, null, null, null, null);

        rules.add(rule);
        try {
            result = _lbVmMgr.applyLoadBalancingRules(new NetworkVO(), rules,  vm, publicIpGuestIpMapping);
        } finally {
            assertFalse("Rules were applied to vm in Starting state", result);
        }
    }

    @Test
    public void applyToVmInStoppedState() throws ResourceUnavailableException {
        boolean result = false;
        vm.setState(State.Stopped);

        List<LoadBalancingRule> rules = new ArrayList<LoadBalancingRule>();
        LoadBalancingRule rule = new LoadBalancingRule(null, null, null, null, null, null, null);

        rules.add(rule);
        try {
            result = _lbVmMgr.applyLoadBalancingRules(new NetworkVO(), rules,  vm, publicIpGuestIpMapping);
        } finally {
            assertTrue("Rules failed to apply to vm in Stopped state", result);
        }
    }

    @Test
    public void applyToVmInStoppingState() throws ResourceUnavailableException {
        boolean result = false;
        List<DomainRouterVO> vms = new ArrayList<DomainRouterVO>();
        vm.setState(State.Stopping);
        vms.add(vm);

        List<LoadBalancingRule> rules = new ArrayList<LoadBalancingRule>();
        LoadBalancingRule rule = new LoadBalancingRule(null, null, null, null, null, null, null);

        rules.add(rule);
        try {
            result = _lbVmMgr.applyLoadBalancingRules(new NetworkVO(), rules,  vm, publicIpGuestIpMapping);
        } finally {
            assertTrue("Rules failed to apply to vm in Stopping state", result);
        }
    }

    @Test
    public void applyToVmInRunningState() throws ResourceUnavailableException {
        boolean result = false;
        List<DomainRouterVO> vms = new ArrayList<DomainRouterVO>();
        vm.setState(State.Running);
        vms.add(vm);

        List<LoadBalancingRule> rules = new ArrayList<LoadBalancingRule>();
        ApplicationLoadBalancerRuleVO lb = new ApplicationLoadBalancerRuleVO(null, null, 22, 22, "roundrobin",
                1L, 1L, 1L, new Ip(requestedPublicIp), 1L, Scheme.Public);
        lb.setState(FirewallRule.State.Add);

        LoadBalancingRule rule = new LoadBalancingRule(lb, null, null, null, new Ip(requestedPublicIp));
        rules.add(rule);
        try {
            result = _lbVmMgr.applyLoadBalancingRules(ntwk, rules,  vm, publicIpGuestIpMapping);
        } finally {
            assertTrue("Rules failed to apply to vm in Running state", result);
        }
    }

    @Test
    public void destroyNonExistingVM() throws ResourceUnavailableException, ConcurrentOperationException {
        boolean result = false;

        try {
             result = _lbVmMgr.destroyVpcInlineLbVm(invalidVmId, ACCOUNT, 1L);
        } finally {
            assertTrue("Failed to destroy non-existing vm", result);
        }
    }

    @Test
    public void destroyExistingVM() throws ResourceUnavailableException, ConcurrentOperationException {
        boolean result = false;

        try {
             result = _lbVmMgr.destroyVpcInlineLbVm(validVmId, ACCOUNT, 1L);
        } finally {
            assertTrue("Failed to destroy valid vm", result);
        }
    }

    @Test
    public void deployLbVM() throws ResourceUnavailableException, ConcurrentOperationException, InsufficientCapacityException {
        List<DomainRouterVO> result = _lbVmMgr.deployVpcInlineLbVm(ntwk, new DeployDestination(dc, null, null, null), ACCOUNT, null);
        assertTrue("Failed to deploy lb vm", !result.isEmpty());
    }

    @Test (expected = InvalidParameterValueException.class)
    public void startNonExistingVm() {
        try {
            _lbVmMgr.startVpcInlineLbVm(invalidVmId, _accountMgr.getAccount(1L), 1L);
        } catch (InsufficientCapacityException | ResourceUnavailableException e) {
            throw new CloudRuntimeException(e);
        }
    }

    @Test (expected = InvalidParameterValueException.class)
    public void startNonVpcInlineLbVmVm() {
        try {
            _lbVmMgr.startVpcInlineLbVm(nonVpcInlineLbVmId, _accountMgr.getAccount(1L), 1L);
        } catch (InsufficientCapacityException | ResourceUnavailableException e) {
            throw new CloudRuntimeException(e);
        }
    }

    @Test
    public void startValidLbVmVm() {
        VirtualRouter vr = null;
        try {
            vr = _lbVmMgr.startVpcInlineLbVm(validVmId, _accountMgr.getAccount(1L), 1L);
        } catch (InsufficientCapacityException | ResourceUnavailableException e) {
            throw new CloudRuntimeException(e);
        } finally {
            assertNotNull("VpcInline LB vm is null which means it failed to start " + vr, vr);
        }
    }

    @Test (expected = InvalidParameterValueException.class)
    public void stopNonExistingVm() {
        try {
            _lbVmMgr.stopVpcInlineLbVm(invalidVmId, false,_accountMgr.getAccount(1L), 1L);
        } catch (ResourceUnavailableException e) {
            throw new CloudRuntimeException(e);
        }
    }

    @Test (expected = InvalidParameterValueException.class)
    public void stopNonVpcInlineLbVmVm() {
        try {
            _lbVmMgr.stopVpcInlineLbVm(nonVpcInlineLbVmId, false, _accountMgr.getAccount(1L), 1L);
        } catch (ResourceUnavailableException e) {
            throw new CloudRuntimeException(e);
        }
    }

    @Test
    public void stopValidLbVmVm() {
        VirtualRouter vr = null;
        try {
            vr = _lbVmMgr.stopVpcInlineLbVm(validVmId, false, _accountMgr.getAccount(1L), 1L);
        } catch (ResourceUnavailableException e) {
            throw new CloudRuntimeException(e);
        } finally {
            assertNotNull("VpcInline LB vm is null which means it failed to stop " + vr, vr);
        }
    }

    private static <T extends InternalIdentity> T setId(T vo, long id) {
        T voToReturn = vo;
        Class<?> c = voToReturn.getClass();
        try {
            Predicate<Field> isId = new Predicate<Field>() {
                @Override
                public boolean apply(Field field) {
                    return field.getName().equals("id");
                }
            };
            while (Collections2.filter(Arrays.asList(c.getDeclaredFields()), isId).isEmpty()) {
                c = c.getSuperclass();
            }
            Field f = c.getDeclaredField("id");
            f.setAccessible(true);
            f.setLong(voToReturn, id);
        } catch (NoSuchFieldException ex) {
           return null;
        } catch (IllegalAccessException ex) {
            return null;
        }

        return voToReturn;
    }
}
