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

import com.cloud.domain.DomainVO;
import com.cloud.exception.ResourceAllocationException;
import com.cloud.exception.ResourceUnavailableException;
import com.cloud.exception.InsufficientCapacityException;
import com.cloud.exception.InvalidParameterValueException;
import com.cloud.hypervisor.Hypervisor;
import com.cloud.network.Network;
import com.cloud.network.NetworkModelImpl;
import com.cloud.network.as.dao.AutoScaleVmGroupDao;
import com.cloud.network.dao.LoadBalancerDao;
import com.cloud.network.dao.LoadBalancerVMMapDao;
import com.cloud.network.dao.LoadBalancerVMMapVO;
import com.cloud.network.dao.LoadBalancerVO;
import com.cloud.network.dao.NetworkDao;
import com.cloud.network.rules.FirewallRule;
import com.cloud.network.rules.RulesManagerImpl;
import com.cloud.user.Account;
import com.cloud.user.AccountManager;
import com.cloud.user.AccountVO;
import com.cloud.user.User;
import com.cloud.user.UserVO;
import com.cloud.user.dao.AccountDao;
import com.cloud.uservm.UserVm;
import com.cloud.vm.Nic;
import com.cloud.vm.NicVO;
import com.cloud.vm.UserVmVO;
import com.cloud.vm.dao.NicSecondaryIpDao;
import com.cloud.vm.dao.UserVmDao;
import org.apache.cloudstack.acl.SecurityChecker;
import org.apache.cloudstack.api.ResponseGenerator;
import org.apache.cloudstack.api.command.user.loadbalancer.AssignToLoadBalancerRuleCmd;
import org.apache.cloudstack.api.response.SuccessResponse;
import org.apache.cloudstack.context.CallContext;
import org.junit.After;
import org.junit.Before;
import org.junit.Rule;
import org.junit.Test;
import org.junit.rules.ExpectedException;
import org.mockito.Mock;
import org.mockito.Mockito;
import org.mockito.Spy;

import java.util.UUID;
import java.util.HashMap;
import java.util.Map;
import java.util.List;
import java.util.ArrayList;

import static org.mockito.Matchers.anyBoolean;
import static org.mockito.Matchers.anyLong;
import static org.mockito.Matchers.anyString;
import static org.mockito.Mockito.any;
import static org.mockito.Mockito.when;

public class AssignLoadBalancerTest {

    @Mock
    List<LoadBalancerVMMapVO> _lbvmMapList;

    @Mock
    List<Nic> nic;

    @Spy
    RulesManagerImpl _rulesMgr = new RulesManagerImpl() {
        @Override
        public void checkRuleAndUserVm (FirewallRule rule, UserVm userVm, Account caller) {

        }
    };


    @Spy
    NicVO nicvo = new NicVO() {

    };

    @Spy
    NetworkModelImpl _networkModel = new NetworkModelImpl() {
        @Override
        public List<? extends Nic> getNics(long vmId) {
            nic = new ArrayList<Nic>();
            nicvo.setNetworkId(204L);
            nic.add(nicvo);
            return nic;
        }
    };


    LoadBalancingRulesManagerImpl _lbMgr = new LoadBalancingRulesManagerImpl();

    private AssignToLoadBalancerRuleCmd assignToLbRuleCmd;
    private ResponseGenerator responseGenerator;
    private SuccessResponse successResponseGenerator;

    @Rule
    public ExpectedException expectedException = ExpectedException.none();

    private static long domainId = 5L;
    private static long accountId = 5L;
    private static String accountName = "admin";

    @Before
    public void setUp() {
        assignToLbRuleCmd = new AssignToLoadBalancerRuleCmd() {
        };

        // ComponentContext.initComponentsLifeCycle();
        AccountVO account = new AccountVO(accountName, domainId, "networkDomain", Account.Type.NORMAL, "uuid");
        DomainVO domain = new DomainVO("rootDomain", 5L, 5L, "networkDomain");

        UserVO user = new UserVO(1, "testuser", "password", "firstname", "lastName", "email", "timezone", UUID.randomUUID().toString(), User.Source.UNKNOWN);

        CallContext.register(user, account);

    }

    @Test(expected = InvalidParameterValueException.class)
    public void testBothArgsEmpty() throws ResourceAllocationException, ResourceUnavailableException, InsufficientCapacityException {

        Map<Long, List<String>> emptyMap = new HashMap<Long, List<String>>();

        LoadBalancerDao lbdao = Mockito.mock(LoadBalancerDao.class);
        _lbMgr._lbDao =  lbdao;

        AutoScaleVmGroupDao autoScaleVmGroupDao = Mockito.mock(AutoScaleVmGroupDao.class);
        _lbMgr._autoScaleVmGroupDao = autoScaleVmGroupDao;

        when(lbdao.findById(anyLong())).thenReturn(Mockito.mock(LoadBalancerVO.class));
        when(autoScaleVmGroupDao.isAutoScaleLoadBalancer(anyLong())).thenReturn(Boolean.FALSE);

        _lbMgr.assignToLoadBalancer(1L, null, emptyMap, false);

    }

    @Test(expected = InvalidParameterValueException.class)
    public void testNicIsNotInNw() throws ResourceAllocationException, ResourceUnavailableException, InsufficientCapacityException {

        Map<Long, List<String>> vmIdIpMap = new HashMap<Long, List<String>>();
        List<String> secIp = new ArrayList<String>();
        secIp.add("10.1.1.175");
        vmIdIpMap.put(1L,secIp);

        List<Long> vmIds = new ArrayList<Long>();
        vmIds.add(2L);

        LoadBalancerVO lbVO = new LoadBalancerVO("1", "L1", "Lbrule", 1, 22, 22, "rb", 204, 0, 0, "tcp", null);
        UserVmVO vm = new UserVmVO(2L, "test", "test", 101L, Hypervisor.HypervisorType.Any, 21L, false, false, domainId, 200L, 1, 5L, "", 1L, null, "test");

        LoadBalancerDao lbDao = Mockito.mock(LoadBalancerDao.class);
        LoadBalancerVMMapDao lb2VmMapDao = Mockito.mock(LoadBalancerVMMapDao.class);
        UserVmDao userVmDao = Mockito.mock(UserVmDao.class);
        AccountDao accountDao = Mockito.mock(AccountDao.class);
        NetworkDao networkDao = Mockito.mock(NetworkDao.class);
        AccountManager accountMgr = Mockito.mock(AccountManager.class);
        AutoScaleVmGroupDao autoScaleVmGroupDao = Mockito.mock(AutoScaleVmGroupDao.class);

        _lbMgr._lbDao = lbDao;
        _lbMgr._lb2VmMapDao = lb2VmMapDao;
        _lbMgr._vmDao = userVmDao;
        _lbMgr._accountDao = accountDao;
        _lbMgr._accountMgr = accountMgr;
        _lbMgr._networkDao = networkDao;
        _lbvmMapList = new ArrayList<>();
        _lbMgr._rulesMgr = _rulesMgr;
        _lbMgr._networkModel = _networkModel;
        _lbMgr._autoScaleVmGroupDao = autoScaleVmGroupDao;

        when(lbDao.findById(anyLong())).thenReturn(Mockito.mock(LoadBalancerVO.class));
        when(userVmDao.findById(anyLong())).thenReturn(vm);
        when(lb2VmMapDao.listByLoadBalancerId(anyLong(), anyBoolean())).thenReturn(_lbvmMapList);
        when(accountDao.findById(anyLong())).thenReturn(Mockito.mock(AccountVO.class));
        Mockito.doNothing().when(accountMgr).checkAccess(any(Account.class), any(SecurityChecker.AccessType.class), any(Boolean.class), any(Network.class));
        when(autoScaleVmGroupDao.isAutoScaleLoadBalancer(anyLong())).thenReturn(Boolean.FALSE);

        _lbMgr.assignToLoadBalancer(1L, null, vmIdIpMap, false);
    }


    @Test(expected = InvalidParameterValueException.class)
    public void tesSecIpNotSetToVm() throws ResourceAllocationException, ResourceUnavailableException, InsufficientCapacityException {

        AssignToLoadBalancerRuleCmd assignLbRuleCmd = Mockito.mock(AssignToLoadBalancerRuleCmd.class);

        Map<Long, List<String>> vmIdIpMap = new HashMap<Long, List<String>>();
        List<String> secIp = new ArrayList<String>();
        secIp.add("10.1.1.175");
        vmIdIpMap.put(1L,secIp);

        List<Long> vmIds = new ArrayList<Long>();
        vmIds.add(2L);

        LoadBalancerVO lbVO = new LoadBalancerVO("1", "L1", "Lbrule", 1, 22, 22, "rb", 204, 0, 0, "tcp", null);
        UserVmVO vm = new UserVmVO(2L, "test", "test", 101L, Hypervisor.HypervisorType.Any, 21L, false, false, domainId, 200L, 1, 5L, "", 1L, null, "test");

        LoadBalancerDao lbDao = Mockito.mock(LoadBalancerDao.class);
        LoadBalancerVMMapDao lb2VmMapDao = Mockito.mock(LoadBalancerVMMapDao.class);
        UserVmDao userVmDao = Mockito.mock(UserVmDao.class);
        AccountDao accountDao = Mockito.mock(AccountDao.class);
        NetworkDao networkDao = Mockito.mock(NetworkDao.class);
        AccountManager accountMgr = Mockito.mock(AccountManager.class);
        NicSecondaryIpDao nicSecIpDao =  Mockito.mock(NicSecondaryIpDao.class);
        AutoScaleVmGroupDao autoScaleVmGroupDao = Mockito.mock(AutoScaleVmGroupDao.class);

        _lbMgr._lbDao = lbDao;
        _lbMgr._lb2VmMapDao = lb2VmMapDao;
        _lbMgr._vmDao = userVmDao;
        _lbMgr._accountDao = accountDao;
        _lbMgr._accountMgr = accountMgr;
        _lbMgr._networkDao = networkDao;
        _lbMgr._nicSecondaryIpDao = nicSecIpDao;
        _lbvmMapList = new ArrayList<>();
        _lbMgr._rulesMgr = _rulesMgr;
        _lbMgr._networkModel = _networkModel;
        _lbMgr._autoScaleVmGroupDao = autoScaleVmGroupDao;

        when(lbDao.findById(anyLong())).thenReturn(lbVO);
        when(userVmDao.findById(anyLong())).thenReturn(vm);
        when(lb2VmMapDao.listByLoadBalancerId(anyLong(), anyBoolean())).thenReturn(_lbvmMapList);
        when (nicSecIpDao.findByIp4AddressAndNicId(anyString(), anyLong())).thenReturn(null);
        when(autoScaleVmGroupDao.isAutoScaleLoadBalancer(anyLong())).thenReturn(Boolean.FALSE);

        _lbMgr.assignToLoadBalancer(1L, null, vmIdIpMap, false);
    }



    @Test(expected = InvalidParameterValueException.class)
    public void testVmIdAlreadyExist() throws ResourceAllocationException, ResourceUnavailableException, InsufficientCapacityException {

        AssignToLoadBalancerRuleCmd assignLbRuleCmd = Mockito.mock(AssignToLoadBalancerRuleCmd.class);

        Map<Long, List<String>> vmIdIpMap = new HashMap<Long, List<String>>();
        List<String> secIp = new ArrayList<String>();
        secIp.add("10.1.1.175");
        vmIdIpMap.put(1L,secIp);

        List<Long> vmIds = new ArrayList<Long>();
        vmIds.add(2L);

        LoadBalancerVO lbVO = new LoadBalancerVO("1", "L1", "Lbrule", 1, 22, 22, "rb", 204, 0, 0, "tcp", null);
        UserVmVO vm = new UserVmVO(2L, "test", "test", 101L, Hypervisor.HypervisorType.Any, 21L, false, false, domainId, 200L, 1, 5L, "", 1L, null, "test");

        LoadBalancerDao lbDao = Mockito.mock(LoadBalancerDao.class);
        LoadBalancerVMMapDao lb2VmMapDao = Mockito.mock(LoadBalancerVMMapDao.class);
        UserVmDao userVmDao = Mockito.mock(UserVmDao.class);
        AccountDao accountDao = Mockito.mock(AccountDao.class);
        AccountManager accountMgr = Mockito.mock(AccountManager.class);
        NetworkDao networkDao = Mockito.mock(NetworkDao.class);
        NicSecondaryIpDao nicSecIpDao =  Mockito.mock(NicSecondaryIpDao.class);
        LoadBalancerVMMapVO lbVmMapVO = new LoadBalancerVMMapVO(1L, 1L, "10.1.1.175", false);
        AutoScaleVmGroupDao autoScaleVmGroupDao = Mockito.mock(AutoScaleVmGroupDao.class);

        _lbMgr._lbDao = lbDao;
        _lbMgr._lb2VmMapDao = lb2VmMapDao;
        _lbMgr._vmDao = userVmDao;
        _lbMgr._accountDao = accountDao;
        _lbMgr._accountMgr = accountMgr;
        _lbMgr._networkDao = networkDao;
        _lbMgr._nicSecondaryIpDao = nicSecIpDao;
        _lbvmMapList = new ArrayList<>();
        _lbvmMapList.add(lbVmMapVO);
        _lbMgr._rulesMgr = _rulesMgr;
        _lbMgr._networkModel = _networkModel;
        _lbMgr._autoScaleVmGroupDao = autoScaleVmGroupDao;

        when(lbDao.findById(anyLong())).thenReturn(lbVO);
        when(userVmDao.findById(anyLong())).thenReturn(vm);
        when(lb2VmMapDao.listByLoadBalancerId(anyLong(), anyBoolean())).thenReturn(_lbvmMapList);
        when (nicSecIpDao.findByIp4AddressAndNicId(anyString(), anyLong())).thenReturn(null);
        when(autoScaleVmGroupDao.isAutoScaleLoadBalancer(anyLong())).thenReturn(Boolean.FALSE);

        _lbMgr.assignToLoadBalancer(1L, null, vmIdIpMap, false);
    }

    @After
    public void tearDown() {
        CallContext.unregister();
    }

}
