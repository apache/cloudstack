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
package org.apache.cloudstack.internallbvmmgr;

import java.lang.reflect.Field;
import java.net.URI;
import java.net.URISyntaxException;
import java.util.ArrayList;
import java.util.List;

import javax.inject.Inject;

import org.apache.cloudstack.lb.ApplicationLoadBalancerRuleVO;
import org.apache.cloudstack.network.lb.InternalLoadBalancerVMManager;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.Matchers;
import org.mockito.Mockito;
import org.springframework.test.context.ContextConfiguration;
import org.springframework.test.context.junit4.SpringJUnit4ClassRunner;

import com.cloud.agent.AgentManager;
import com.cloud.agent.api.Answer;
import com.cloud.agent.manager.Commands;
import com.cloud.dc.DataCenter.NetworkType;
import com.cloud.dc.DataCenterVO;
import com.cloud.dc.dao.DataCenterDao;
import com.cloud.exception.AgentUnavailableException;
import com.cloud.exception.ConcurrentOperationException;
import com.cloud.exception.OperationTimedoutException;
import com.cloud.exception.ResourceUnavailableException;
import com.cloud.hypervisor.Hypervisor.HypervisorType;
import com.cloud.network.NetworkModel;
import com.cloud.network.dao.NetworkVO;
import com.cloud.network.lb.LoadBalancingRule;
import com.cloud.network.router.VirtualRouter;
import com.cloud.network.router.VirtualRouter.Role;
import com.cloud.network.rules.FirewallRule;
import com.cloud.network.rules.LoadBalancerContainer.Scheme;
import com.cloud.offerings.NetworkOfferingVO;
import com.cloud.offerings.dao.NetworkOfferingDao;
import com.cloud.service.ServiceOfferingVO;
import com.cloud.service.dao.ServiceOfferingDao;
import com.cloud.storage.Storage;
import com.cloud.storage.Storage.ProvisioningType;
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

import junit.framework.TestCase;

/**
 * Set of unittests for InternalLoadBalancerVMManager
 *
 */

@RunWith(SpringJUnit4ClassRunner.class)
@ContextConfiguration(locations = "classpath:/lb_mgr.xml")
public class InternalLBVMManagerTest extends TestCase {
    //The interface to test
    @Inject
    InternalLoadBalancerVMManager _lbVmMgr;

    //Mocked interfaces
    @Inject
    AccountManager _accountMgr;
    @Inject
    ServiceOfferingDao _svcOffDao;
    @Inject
    DomainRouterDao _domainRouterDao;
    @Inject
    NicDao _nicDao;
    @Inject
    AgentManager _agentMgr;
    @Inject
    NetworkModel _ntwkModel;
    @Inject
    VirtualMachineManager _itMgr;
    @Inject
    DataCenterDao _dcDao;
    @Inject
    NetworkOfferingDao _offeringDao;
    long validNtwkId = 1L;
    long invalidNtwkId = 2L;
    String requestedIp = "10.1.1.1";
    DomainRouterVO vm = null;
    NetworkVO ntwk = createNetwork();
    long validVmId = 1L;
    long invalidVmId = 2L;

    @Override
    @Before
    public void setUp() {
        //mock system offering creation as it's used by configure() method called by initComponentsLifeCycle
        Mockito.when(_accountMgr.getAccount(1L)).thenReturn(new AccountVO());
        ServiceOfferingVO off = new ServiceOfferingVO("alena", 1, 1,
                1, 1, 1, false, "alena", Storage.ProvisioningType.THIN, false, false, null, false, VirtualMachine.Type.InternalLoadBalancerVm, false);
        off = setId(off, 1);
        List<ServiceOfferingVO> list = new ArrayList<ServiceOfferingVO>();
        list.add(off);
        list.add(off);
        Mockito.when(_svcOffDao.createSystemServiceOfferings(Matchers.anyString(), Matchers.anyString(), Matchers.anyInt(), Matchers.anyInt(), Matchers.anyInt(),
                Matchers.anyInt(), Matchers.anyInt(), Matchers.anyBoolean(), Matchers.anyString(), Matchers.any(ProvisioningType.class), Matchers.anyBoolean(),
                Matchers.anyString(), Matchers.anyBoolean(), Matchers.any(VirtualMachine.Type.class), Matchers.anyBoolean())).thenReturn(list);

        ComponentContext.initComponentsLifeCycle();

        vm =
                new DomainRouterVO(1L, off.getId(), 1, "alena", 1, HypervisorType.XenServer, 1, 1, 1, 1, false, null, false, false,
                        VirtualMachine.Type.InternalLoadBalancerVm, null);
        vm.setRole(Role.INTERNAL_LB_VM);
        vm = setId(vm, 1);
        vm.setPrivateIpAddress("10.2.2.2");
        final NicVO nic = new NicVO("somereserver", 1L, 1L, VirtualMachine.Type.InternalLoadBalancerVm);
        nic.setIPv4Address(requestedIp);

        final List<DomainRouterVO> emptyList = new ArrayList<DomainRouterVO>();
        final List<DomainRouterVO> nonEmptyList = new ArrayList<DomainRouterVO>();
        nonEmptyList.add(vm);

        Mockito.when(_domainRouterDao.listByNetworkAndRole(invalidNtwkId, Role.INTERNAL_LB_VM)).thenReturn(emptyList);
        Mockito.when(_domainRouterDao.listByNetworkAndRole(validNtwkId, Role.INTERNAL_LB_VM)).thenReturn(nonEmptyList);

        Mockito.when(_nicDao.findByNtwkIdAndInstanceId(validNtwkId, 1)).thenReturn(nic);
        Mockito.when(_nicDao.findByNtwkIdAndInstanceId(invalidNtwkId, 1)).thenReturn(nic);

        final Answer answer = new Answer(null, true, null);
        final Answer[] answers = new Answer[1];
        answers[0] = answer;

        try {
            Mockito.when(_agentMgr.send(Matchers.anyLong(), Matchers.any(Commands.class))).thenReturn(answers);
        } catch (final AgentUnavailableException e) {
            // TODO Auto-generated catch block
            e.printStackTrace();
        } catch (final OperationTimedoutException e) {
            // TODO Auto-generated catch block
            e.printStackTrace();
        }

        createNetwork();
        Mockito.when(_ntwkModel.getNetwork(Matchers.anyLong())).thenReturn(ntwk);

        Mockito.when(_itMgr.toNicTO(Matchers.any(NicProfile.class), Matchers.any(HypervisorType.class))).thenReturn(null);
        Mockito.when(_domainRouterDao.findById(Matchers.anyLong())).thenReturn(vm);
        final DataCenterVO dc = new DataCenterVO(1L, null, null, null, null, null, null, null, null, null, NetworkType.Advanced, null, null);
        Mockito.when(_dcDao.findById(Matchers.anyLong())).thenReturn(dc);
        final NetworkOfferingVO networkOfferingVO = new NetworkOfferingVO();
        networkOfferingVO.setConcurrentConnections(500);
        Mockito.when(_offeringDao.findById(Matchers.anyLong())).thenReturn(networkOfferingVO);

        Mockito.when(_domainRouterDao.findById(validVmId)).thenReturn(vm);
        Mockito.when(_domainRouterDao.findById(invalidVmId)).thenReturn(null);

    }

    protected NetworkVO createNetwork() {
        ntwk = new NetworkVO();
        try {
            ntwk.setBroadcastUri(new URI("somevlan"));
        } catch (final URISyntaxException e) {
            // TODO Auto-generated catch block
            e.printStackTrace();
        }
        ntwk = setId(ntwk, 1L);
        return ntwk;
    }

    //TESTS FOR findInternalLbVms METHOD

    @Test
    public void findInternalLbVmsForInvalidNetwork() {
        final List<? extends VirtualRouter> vms = _lbVmMgr.findInternalLbVms(invalidNtwkId, new Ip(requestedIp));
        assertTrue("Non empty vm list was returned for invalid network id", vms.isEmpty());
    }

    @Test
    public void findInternalLbVmsForValidNetwork() {
        final List<? extends VirtualRouter> vms = _lbVmMgr.findInternalLbVms(validNtwkId, new Ip(requestedIp));
        assertTrue("Empty vm list was returned for valid network id", !vms.isEmpty());
    }

    //TESTS FOR applyLoadBalancingRules METHOD
    @Test
    public void applyEmptyRulesSet() {
        boolean result = false;
        final List<DomainRouterVO> vms = new ArrayList<DomainRouterVO>();
        try {
            result = _lbVmMgr.applyLoadBalancingRules(new NetworkVO(), new ArrayList<LoadBalancingRule>(), vms);
        } catch (final ResourceUnavailableException e) {

        } finally {
            assertTrue("Got failure when tried to apply empty list of rules", result);
        }
    }

    @Test(expected = CloudRuntimeException.class)
    public void applyWithEmptyVmsSet() {
        boolean result = false;
        final List<DomainRouterVO> vms = new ArrayList<DomainRouterVO>();
        final List<LoadBalancingRule> rules = new ArrayList<LoadBalancingRule>();
        final LoadBalancingRule rule = new LoadBalancingRule(null, null, null, null, null, null, null);

        rules.add(rule);
        try {
            result = _lbVmMgr.applyLoadBalancingRules(new NetworkVO(), rules, vms);
        } catch (final ResourceUnavailableException e) {
        } finally {
            assertFalse("Got success when tried to apply with the empty internal lb vm list", result);
        }
    }

    @Test(expected = ResourceUnavailableException.class)
    public void applyToVmInStartingState() throws ResourceUnavailableException {
        boolean result = false;
        final List<DomainRouterVO> vms = new ArrayList<DomainRouterVO>();
        vm.setState(State.Starting);
        vms.add(vm);

        final List<LoadBalancingRule> rules = new ArrayList<LoadBalancingRule>();
        final LoadBalancingRule rule = new LoadBalancingRule(null, null, null, null, null, null, null);

        rules.add(rule);
        try {
            result = _lbVmMgr.applyLoadBalancingRules(new NetworkVO(), rules, vms);
        } finally {
            assertFalse("Rules were applied to vm in Starting state", result);
        }
    }

    @Test
    public void applyToVmInStoppedState() throws ResourceUnavailableException {
        boolean result = false;
        final List<DomainRouterVO> vms = new ArrayList<DomainRouterVO>();
        vm.setState(State.Stopped);
        vms.add(vm);

        final List<LoadBalancingRule> rules = new ArrayList<LoadBalancingRule>();
        final LoadBalancingRule rule = new LoadBalancingRule(null, null, null, null, null, null, null);

        rules.add(rule);
        try {
            result = _lbVmMgr.applyLoadBalancingRules(new NetworkVO(), rules, vms);
        } finally {
            assertTrue("Rules failed to apply to vm in Stopped state", result);
        }
    }

    @Test
    public void applyToVmInStoppingState() throws ResourceUnavailableException {
        boolean result = false;
        final List<DomainRouterVO> vms = new ArrayList<DomainRouterVO>();
        vm.setState(State.Stopping);
        vms.add(vm);

        final List<LoadBalancingRule> rules = new ArrayList<LoadBalancingRule>();
        final LoadBalancingRule rule = new LoadBalancingRule(null, null, null, null, null, null, null);

        rules.add(rule);
        try {
            result = _lbVmMgr.applyLoadBalancingRules(new NetworkVO(), rules, vms);
        } finally {
            assertTrue("Rules failed to apply to vm in Stopping state", result);
        }
    }

    @Test
    public void applyToVmInRunningState() throws ResourceUnavailableException {
        boolean result = false;
        final List<DomainRouterVO> vms = new ArrayList<DomainRouterVO>();
        vm.setState(State.Running);
        vms.add(vm);

        final List<LoadBalancingRule> rules = new ArrayList<LoadBalancingRule>();
        final ApplicationLoadBalancerRuleVO lb = new ApplicationLoadBalancerRuleVO(null, null, 22, 22, "roundrobin", 1L, 1L, 1L, new Ip(requestedIp), 1L, Scheme.Internal);
        lb.setState(FirewallRule.State.Add);

        final LoadBalancingRule rule = new LoadBalancingRule(lb, null, null, null, new Ip(requestedIp));

        rules.add(rule);

        ntwk.getId();

        try {
            result = _lbVmMgr.applyLoadBalancingRules(ntwk, rules, vms);
        } finally {
            assertTrue("Rules failed to apply to vm in Running state", result);
        }
    }

    //TESTS FOR destroyInternalLbVm METHOD
    @Test
    public void destroyNonExistingVM() throws ResourceUnavailableException, ConcurrentOperationException {
        boolean result = false;

        try {
            result = _lbVmMgr.destroyInternalLbVm(invalidVmId, new AccountVO(), 1L);
        } finally {
            assertTrue("Failed to destroy non-existing vm", result);
        }
    }

    @Test
    public void destroyExistingVM() throws ResourceUnavailableException, ConcurrentOperationException {
        boolean result = false;

        try {
            result = _lbVmMgr.destroyInternalLbVm(validVmId, new AccountVO(), 1L);
        } finally {
            assertTrue("Failed to destroy valid vm", result);
        }
    }

    private static ServiceOfferingVO setId(final ServiceOfferingVO vo, final long id) {
        final ServiceOfferingVO voToReturn = vo;
        final Class<?> c = voToReturn.getClass();
        try {
            final Field f = c.getSuperclass().getDeclaredField("id");
            f.setAccessible(true);
            f.setLong(voToReturn, id);
        } catch (final NoSuchFieldException ex) {
            return null;
        } catch (final IllegalAccessException ex) {
            return null;
        }

        return voToReturn;
    }

    private static NetworkVO setId(final NetworkVO vo, final long id) {
        final NetworkVO voToReturn = vo;
        final Class<?> c = voToReturn.getClass();
        try {
            final Field f = c.getDeclaredField("id");
            f.setAccessible(true);
            f.setLong(voToReturn, id);
        } catch (final NoSuchFieldException ex) {
            return null;
        } catch (final IllegalAccessException ex) {
            return null;
        }

        return voToReturn;
    }

    private static DomainRouterVO setId(final DomainRouterVO vo, final long id) {
        final DomainRouterVO voToReturn = vo;
        final Class<?> c = voToReturn.getClass();
        try {
            final Field f = c.getSuperclass().getDeclaredField("id");
            f.setAccessible(true);
            f.setLong(voToReturn, id);
        } catch (final NoSuchFieldException ex) {
            return null;
        } catch (final IllegalAccessException ex) {
            return null;
        }

        return voToReturn;
    }

}
