// Licensed to the Apache Software Foundation (ASF) under one or more
// contributor license agreements.  See the NOTICE file distributed with
// this work for additional information regarding copyright ownership.
// The ASF licenses this file to You under the Apache License, Version 2.0
// (the "License"); you may not use this file except in compliance with
// the License.  You may obtain a copy of the License at
//
//   http://www.apache.org/licenses/LICENSE-2.0
//
// Unless required by applicable law or agreed to in writing, software
// distributed under the License is distributed on an "AS IS" BASIS,
// WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
// See the License for the specific language governing permissions and
// limitations under the License.

package org.apache.cloudstack.region.gslb;

import static org.mockito.Matchers.any;
import static org.mockito.Matchers.anyInt;
import static org.mockito.Matchers.anyLong;
import static org.mockito.Mockito.when;

import java.lang.reflect.Field;
import java.util.ArrayList;
import java.util.List;
import java.util.UUID;

import junit.framework.Assert;
import junit.framework.TestCase;

import org.apache.log4j.Logger;
import org.junit.After;
import org.junit.Before;
import org.junit.Test;
import org.mockito.Mockito;

import org.apache.cloudstack.api.command.user.region.ha.gslb.AssignToGlobalLoadBalancerRuleCmd;
import org.apache.cloudstack.api.command.user.region.ha.gslb.CreateGlobalLoadBalancerRuleCmd;
import org.apache.cloudstack.api.command.user.region.ha.gslb.DeleteGlobalLoadBalancerRuleCmd;
import org.apache.cloudstack.api.command.user.region.ha.gslb.RemoveFromGlobalLoadBalancerRuleCmd;
import org.apache.cloudstack.context.CallContext;
import org.apache.cloudstack.region.RegionVO;
import org.apache.cloudstack.region.dao.RegionDao;

import com.cloud.agent.AgentManager;
import com.cloud.configuration.dao.ConfigurationDao;
import com.cloud.exception.InvalidParameterValueException;
import com.cloud.network.dao.IPAddressDao;
import com.cloud.network.dao.IPAddressVO;
import com.cloud.network.dao.LoadBalancerDao;
import com.cloud.network.dao.LoadBalancerVO;
import com.cloud.network.dao.NetworkDao;
import com.cloud.network.dao.NetworkVO;
import com.cloud.network.rules.FirewallRule;
import com.cloud.network.rules.RulesManager;
import com.cloud.region.ha.GlobalLoadBalancerRule;
import com.cloud.user.Account;
import com.cloud.user.AccountManager;
import com.cloud.user.AccountVO;
import com.cloud.user.UserVO;
import com.cloud.utils.db.Transaction;
import com.cloud.utils.net.Ip;

public class GlobalLoadBalancingRulesServiceImplTest extends TestCase {

    private static final Logger s_logger = Logger.getLogger( GlobalLoadBalancingRulesServiceImplTest.class);

    @Override
    @Before
    public void setUp() {
        Account account = new AccountVO("testaccount", 1, "networkdomain", (short)0, UUID.randomUUID().toString());

        UserVO user = new UserVO(1, "testuser", "password", "firstname", "lastName", "email", "timezone", UUID.randomUUID().toString());

        CallContext.register(user, account);
    }

    @Override
    @After
    public void tearDown() {
        CallContext.unregister();
    }

    @Test
    public void testCreateGlobalLoadBalancerRule() throws Exception {

        s_logger.info("Running tests for CreateGlobalLoadBalancerRule() service API");

        /*
         * TEST 1: given valid parameters CreateGlobalLoadBalancerRule should succeed
         */
        runCreateGlobalLoadBalancerRulePostiveTest();

        /*
         * TEST 2: given invalid algorithm CreateGlobalLoadBalancerRule should fail
         */
        runCreateGlobalLoadBalancerRuleInvalidAlgorithm();

        /*
         * TEST 3: given invalid persistence method CreateGlobalLoadBalancerRule should fail
         */
        runCreateGlobalLoadBalancerRuleInvalidStickyMethod();

        /*
         * TEST 4: given invalid service type CreateGlobalLoadBalancerRule should fail
         */
        runCreateGlobalLoadBalancerRuleInvalidServiceType();

        /*
         * TEST 5: given 'domain name' that is already used by a different GSLB rule CreateGlobalLoadBalancerRule should fail
         */
        runCreateGlobalLoadBalancerRuleInvalidDomainName();
    }

    @Test
    public void testAssignToGlobalLoadBalancerRule() throws Exception {

        s_logger.info("Running tests for AssignToGlobalLoadBalancerRule() service API");

        /*
         * TEST 1: given valid gslb rule id, valid lb rule id, and  caller has access to both the rules
         * assignToGlobalLoadBalancerRule service API should succeed
         */
        runAssignToGlobalLoadBalancerRuleTest();

        /*
         * TEST 2: given valid gslb rule id, two valid Lb rules but both belong to same zone then
         * assignToGlobalLoadBalancerRule service API should fail
         */
        runAssignToGlobalLoadBalancerRuleTestSameZoneLb();

        /*
         * TEST 3: if gslb rule is in revoke state assignToGlobalLoadBalancerRule service API should fail
         */
        runAssignToGlobalLoadBalancerRuleTestRevokedState();
    }

    @Test
    public void testRemoveFromGlobalLoadBalancerRule() throws Exception {

        s_logger.info("Running tests for RemoveFromGlobalLoadBalancerRule() service API");

        /*
         * TEST 1: given valid gslb rule id, valid lb rule id and is assigned to given gslb rule id
         * then RemoveFromGlobalLoadBalancerRule service API should succeed
         */
        runRemoveFromGlobalLoadBalancerRuleTest();

        /*
         * TEST 2: given valid gslb rule id, valid lb rule id but NOT assigned to given gslb rule id
         * then RemoveFromGlobalLoadBalancerRule service API should fail
         */
        runRemoveFromGlobalLoadBalancerRuleTestUnassignedLb();

        /*
         * TEST 3: given valid gslb rule id, INVALID lb rule id then RemoveFromGlobalLoadBalancerRule
         * service API should fail
         */
        runRemoveFromGlobalLoadBalancerRuleTestInvalidLb();
    }

    @Test
    public void testDeleteGlobalLoadBalancerRule() throws Exception {

        s_logger.info("Running tests for DeleteGlobalLoadBalancerRule() service API");

        /*
         * TEST 1: given valid gslb rule id with assigned Lb rules, DeleteGlobalLoadBalancerRule()
         * call should succeed, and Gslb rule should be set to revoke state
         */
        runDeleteGlobalLoadBalancerRuleTestWithNoLbRules();

        /*
         * TEST 2: given valid gslb rule id with assigned Lb rules, DeleteGlobalLoadBalancerRule()
         * call should succeed, and Gslb rule should be set to revoke state
         */
        runDeleteGlobalLoadBalancerRuleTestWithLbRules();


    }

    void runCreateGlobalLoadBalancerRulePostiveTest() throws Exception {

        Transaction txn = Transaction.open("runCreateGlobalLoadBalancerRulePostiveTest");

        GlobalLoadBalancingRulesServiceImpl gslbServiceImpl =  new GlobalLoadBalancingRulesServiceImpl();

        gslbServiceImpl._accountMgr = Mockito.mock(AccountManager.class);
        Account account = new AccountVO("testaccount", 1,
                "networkdomain", (short) 0, UUID.randomUUID().toString());
        when(gslbServiceImpl._accountMgr.getAccount(anyLong())).thenReturn(account);

        gslbServiceImpl._gslbRuleDao = Mockito.mock(GlobalLoadBalancerRuleDao.class);
        when(gslbServiceImpl._gslbRuleDao.persist(any(GlobalLoadBalancerRuleVO.class))).thenReturn(new GlobalLoadBalancerRuleVO());
        gslbServiceImpl._gslbLbMapDao = Mockito.mock(GlobalLoadBalancerLbRuleMapDao.class);

        gslbServiceImpl._regionDao = Mockito.mock(RegionDao.class);
        RegionVO region = new RegionVO();
        region.setGslbEnabled(true);
        when(gslbServiceImpl._regionDao.findById(anyInt())).thenReturn(region);

        gslbServiceImpl._rulesMgr = Mockito.mock(RulesManager.class);
        gslbServiceImpl._lbDao = Mockito.mock(LoadBalancerDao.class);
        gslbServiceImpl._networkDao = Mockito.mock(NetworkDao.class);
        gslbServiceImpl._globalConfigDao = Mockito.mock(ConfigurationDao.class);
        gslbServiceImpl._ipAddressDao = Mockito.mock(IPAddressDao.class);
        gslbServiceImpl._agentMgr = Mockito.mock(AgentManager.class);

        CreateGlobalLoadBalancerRuleCmd createCmd = new CreateGlobalLoadBalancerRuleCmdExtn();
        Class<?> _class = createCmd.getClass().getSuperclass();

        Field regionIdField = _class.getDeclaredField("regionId");
        regionIdField.setAccessible(true);
        regionIdField.set(createCmd, new Integer(1));

        Field algoField = _class.getDeclaredField("algorithm");
        algoField.setAccessible(true);
        algoField.set(createCmd, "roundrobin");

        Field stickyField = _class.getDeclaredField("stickyMethod");
        stickyField.setAccessible(true);
        stickyField.set(createCmd, "sourceip");

        Field nameField = _class.getDeclaredField("globalLoadBalancerRuleName");
        nameField.setAccessible(true);
        nameField.set(createCmd, "gslb-rule");

        Field descriptionField = _class.getDeclaredField("description");
        descriptionField.setAccessible(true);
        descriptionField.set(createCmd, "testing create gslb-rule");

        Field serviceDomainField = _class.getDeclaredField("serviceDomainName");
        serviceDomainField.setAccessible(true);
        serviceDomainField.set(createCmd, "gslb-rule-domain");

        Field serviceTypeField = _class.getDeclaredField("serviceType");
        serviceTypeField.setAccessible(true);
        serviceTypeField.set(createCmd, "tcp");

        try {
            gslbServiceImpl.createGlobalLoadBalancerRule(createCmd);
        } catch (Exception e) {
            s_logger.info("exception in testing runCreateGlobalLoadBalancerRulePostiveTest message: " + e.toString());
        }
    }

    void runCreateGlobalLoadBalancerRuleInvalidAlgorithm() throws Exception {

        Transaction txn = Transaction.open("runCreateGlobalLoadBalancerRulePostiveTest");

        GlobalLoadBalancingRulesServiceImpl gslbServiceImpl =  new GlobalLoadBalancingRulesServiceImpl();

        gslbServiceImpl._accountMgr = Mockito.mock(AccountManager.class);
        Account account = new AccountVO("testaccount", 1,
                "networkdomain", (short) 0, UUID.randomUUID().toString());
        when(gslbServiceImpl._accountMgr.getAccount(anyLong())).thenReturn(account);

        gslbServiceImpl._gslbRuleDao = Mockito.mock(GlobalLoadBalancerRuleDao.class);
        when(gslbServiceImpl._gslbRuleDao.persist(any(GlobalLoadBalancerRuleVO.class))).thenReturn(new GlobalLoadBalancerRuleVO());
        gslbServiceImpl._gslbLbMapDao = Mockito.mock(GlobalLoadBalancerLbRuleMapDao.class);

        gslbServiceImpl._regionDao = Mockito.mock(RegionDao.class);
        RegionVO region = new RegionVO();
        region.setGslbEnabled(true);
        when(gslbServiceImpl._regionDao.findById(anyInt())).thenReturn(region);

        gslbServiceImpl._rulesMgr = Mockito.mock(RulesManager.class);
        gslbServiceImpl._lbDao = Mockito.mock(LoadBalancerDao.class);
        gslbServiceImpl._networkDao = Mockito.mock(NetworkDao.class);
        gslbServiceImpl._globalConfigDao = Mockito.mock(ConfigurationDao.class);
        gslbServiceImpl._ipAddressDao = Mockito.mock(IPAddressDao.class);
        gslbServiceImpl._agentMgr = Mockito.mock(AgentManager.class);

        CreateGlobalLoadBalancerRuleCmd createCmd = new CreateGlobalLoadBalancerRuleCmdExtn();
        Class<?> _class = createCmd.getClass().getSuperclass();

        Field regionIdField = _class.getDeclaredField("regionId");
        regionIdField.setAccessible(true);
        regionIdField.set(createCmd, new Integer(1));

        Field algoField = _class.getDeclaredField("algorithm");
        algoField.setAccessible(true);
        algoField.set(createCmd, "invalidalgo");

        Field stickyField = _class.getDeclaredField("stickyMethod");
        stickyField.setAccessible(true);
        stickyField.set(createCmd, "sourceip");

        Field nameField = _class.getDeclaredField("globalLoadBalancerRuleName");
        nameField.setAccessible(true);
        nameField.set(createCmd, "gslb-rule");

        Field descriptionField = _class.getDeclaredField("description");
        descriptionField.setAccessible(true);
        descriptionField.set(createCmd, "testing create gslb-rule");

        Field serviceDomainField = _class.getDeclaredField("serviceDomainName");
        serviceDomainField.setAccessible(true);
        serviceDomainField.set(createCmd, "gslb-rule-domain");

        Field serviceTypeField = _class.getDeclaredField("serviceType");
        serviceTypeField.setAccessible(true);
        serviceTypeField.set(createCmd, "tcp");

        try {
            gslbServiceImpl.createGlobalLoadBalancerRule(createCmd);
        } catch (InvalidParameterValueException e) {
            Assert.assertTrue(e.getMessage().contains("Invalid Algorithm"));
        }
    }

    void runCreateGlobalLoadBalancerRuleInvalidStickyMethod() throws Exception {

        Transaction txn = Transaction.open("runCreateGlobalLoadBalancerRulePostiveTest");

        GlobalLoadBalancingRulesServiceImpl gslbServiceImpl =  new GlobalLoadBalancingRulesServiceImpl();

        gslbServiceImpl._accountMgr = Mockito.mock(AccountManager.class);
        Account account = new AccountVO("testaccount", 1,
                "networkdomain", (short) 0, UUID.randomUUID().toString());
        when(gslbServiceImpl._accountMgr.getAccount(anyLong())).thenReturn(account);

        gslbServiceImpl._gslbRuleDao = Mockito.mock(GlobalLoadBalancerRuleDao.class);
        when(gslbServiceImpl._gslbRuleDao.persist(any(GlobalLoadBalancerRuleVO.class))).thenReturn(new GlobalLoadBalancerRuleVO());
        gslbServiceImpl._gslbLbMapDao = Mockito.mock(GlobalLoadBalancerLbRuleMapDao.class);

        gslbServiceImpl._regionDao = Mockito.mock(RegionDao.class);
        RegionVO region = new RegionVO();
        region.setGslbEnabled(true);
        when(gslbServiceImpl._regionDao.findById(anyInt())).thenReturn(region);

        gslbServiceImpl._rulesMgr = Mockito.mock(RulesManager.class);
        gslbServiceImpl._lbDao = Mockito.mock(LoadBalancerDao.class);
        gslbServiceImpl._networkDao = Mockito.mock(NetworkDao.class);
        gslbServiceImpl._globalConfigDao = Mockito.mock(ConfigurationDao.class);
        gslbServiceImpl._ipAddressDao = Mockito.mock(IPAddressDao.class);
        gslbServiceImpl._agentMgr = Mockito.mock(AgentManager.class);

        CreateGlobalLoadBalancerRuleCmd createCmd = new CreateGlobalLoadBalancerRuleCmdExtn();
        Class<?> _class = createCmd.getClass().getSuperclass();

        Field regionIdField = _class.getDeclaredField("regionId");
        regionIdField.setAccessible(true);
        regionIdField.set(createCmd, new Integer(1));

        Field algoField = _class.getDeclaredField("algorithm");
        algoField.setAccessible(true);
        algoField.set(createCmd, "roundrobin");

        Field stickyField = _class.getDeclaredField("stickyMethod");
        stickyField.setAccessible(true);
        stickyField.set(createCmd, "ivalidstickymethod");

        Field nameField = _class.getDeclaredField("globalLoadBalancerRuleName");
        nameField.setAccessible(true);
        nameField.set(createCmd, "gslb-rule");

        Field descriptionField = _class.getDeclaredField("description");
        descriptionField.setAccessible(true);
        descriptionField.set(createCmd, "testing create gslb-rule");

        Field serviceDomainField = _class.getDeclaredField("serviceDomainName");
        serviceDomainField.setAccessible(true);
        serviceDomainField.set(createCmd, "gslb-rule-domain");

        Field serviceTypeField = _class.getDeclaredField("serviceType");
        serviceTypeField.setAccessible(true);
        serviceTypeField.set(createCmd, "tcp");

        try {
            gslbServiceImpl.createGlobalLoadBalancerRule(createCmd);
        } catch (InvalidParameterValueException e) {
            Assert.assertTrue(e.getMessage().contains("Invalid persistence"));
        }
    }

    void runCreateGlobalLoadBalancerRuleInvalidServiceType() throws Exception {

        Transaction txn = Transaction.open("runCreateGlobalLoadBalancerRulePostiveTest");

        GlobalLoadBalancingRulesServiceImpl gslbServiceImpl =  new GlobalLoadBalancingRulesServiceImpl();

        gslbServiceImpl._accountMgr = Mockito.mock(AccountManager.class);
        Account account = new AccountVO("testaccount", 1,
                "networkdomain", (short) 0, UUID.randomUUID().toString());
        when(gslbServiceImpl._accountMgr.getAccount(anyLong())).thenReturn(account);

        gslbServiceImpl._gslbRuleDao = Mockito.mock(GlobalLoadBalancerRuleDao.class);
        when(gslbServiceImpl._gslbRuleDao.persist(any(GlobalLoadBalancerRuleVO.class))).thenReturn(new GlobalLoadBalancerRuleVO());
        gslbServiceImpl._gslbLbMapDao = Mockito.mock(GlobalLoadBalancerLbRuleMapDao.class);

        gslbServiceImpl._regionDao = Mockito.mock(RegionDao.class);
        RegionVO region = new RegionVO();
        region.setGslbEnabled(true);
        when(gslbServiceImpl._regionDao.findById(anyInt())).thenReturn(region);

        gslbServiceImpl._rulesMgr = Mockito.mock(RulesManager.class);
        gslbServiceImpl._lbDao = Mockito.mock(LoadBalancerDao.class);
        gslbServiceImpl._networkDao = Mockito.mock(NetworkDao.class);
        gslbServiceImpl._globalConfigDao = Mockito.mock(ConfigurationDao.class);
        gslbServiceImpl._ipAddressDao = Mockito.mock(IPAddressDao.class);
        gslbServiceImpl._agentMgr = Mockito.mock(AgentManager.class);

        CreateGlobalLoadBalancerRuleCmd createCmd = new CreateGlobalLoadBalancerRuleCmdExtn();
        Class<?> _class = createCmd.getClass().getSuperclass();

        Field regionIdField = _class.getDeclaredField("regionId");
        regionIdField.setAccessible(true);
        regionIdField.set(createCmd, new Integer(1));

        Field algoField = _class.getDeclaredField("algorithm");
        algoField.setAccessible(true);
        algoField.set(createCmd, "roundrobin");

        Field stickyField = _class.getDeclaredField("stickyMethod");
        stickyField.setAccessible(true);
        stickyField.set(createCmd, "sourceip");

        Field nameField = _class.getDeclaredField("globalLoadBalancerRuleName");
        nameField.setAccessible(true);
        nameField.set(createCmd, "gslb-rule");

        Field descriptionField = _class.getDeclaredField("description");
        descriptionField.setAccessible(true);
        descriptionField.set(createCmd, "testing create gslb-rule");

        Field serviceDomainField = _class.getDeclaredField("serviceDomainName");
        serviceDomainField.setAccessible(true);
        serviceDomainField.set(createCmd, "gslb-rule-domain");

        Field serviceTypeField = _class.getDeclaredField("serviceType");
        serviceTypeField.setAccessible(true);
        serviceTypeField.set(createCmd, "invalidtcp");

        try {
            gslbServiceImpl.createGlobalLoadBalancerRule(createCmd);
        } catch (InvalidParameterValueException e) {
            Assert.assertTrue(e.getMessage().contains("Invalid service type"));
        }
    }

    void runCreateGlobalLoadBalancerRuleInvalidDomainName() throws Exception {

        Transaction txn = Transaction.open("runCreateGlobalLoadBalancerRulePostiveTest");

        GlobalLoadBalancingRulesServiceImpl gslbServiceImpl =  new GlobalLoadBalancingRulesServiceImpl();

        gslbServiceImpl._accountMgr = Mockito.mock(AccountManager.class);
        Account account = new AccountVO("testaccount", 1,
                "networkdomain", (short) 0, UUID.randomUUID().toString());
        when(gslbServiceImpl._accountMgr.getAccount(anyLong())).thenReturn(account);

        gslbServiceImpl._gslbRuleDao = Mockito.mock(GlobalLoadBalancerRuleDao.class);
        gslbServiceImpl._gslbLbMapDao = Mockito.mock(GlobalLoadBalancerLbRuleMapDao.class);

        gslbServiceImpl._regionDao = Mockito.mock(RegionDao.class);
        RegionVO region = new RegionVO();
        region.setGslbEnabled(true);
        when(gslbServiceImpl._regionDao.findById(anyInt())).thenReturn(region);

        gslbServiceImpl._rulesMgr = Mockito.mock(RulesManager.class);
        gslbServiceImpl._lbDao = Mockito.mock(LoadBalancerDao.class);
        gslbServiceImpl._networkDao = Mockito.mock(NetworkDao.class);
        gslbServiceImpl._globalConfigDao = Mockito.mock(ConfigurationDao.class);
        gslbServiceImpl._ipAddressDao = Mockito.mock(IPAddressDao.class);
        gslbServiceImpl._agentMgr = Mockito.mock(AgentManager.class);

        CreateGlobalLoadBalancerRuleCmd createCmd = new CreateGlobalLoadBalancerRuleCmdExtn();
        Class<?> _class = createCmd.getClass().getSuperclass();

        Field regionIdField = _class.getDeclaredField("regionId");
        regionIdField.setAccessible(true);
        regionIdField.set(createCmd, new Integer(1));

        Field algoField = _class.getDeclaredField("algorithm");
        algoField.setAccessible(true);
        algoField.set(createCmd, "roundrobin");

        Field stickyField = _class.getDeclaredField("stickyMethod");
        stickyField.setAccessible(true);
        stickyField.set(createCmd, "sourceip");

        Field nameField = _class.getDeclaredField("globalLoadBalancerRuleName");
        nameField.setAccessible(true);
        nameField.set(createCmd, "gslb-rule");

        Field descriptionField = _class.getDeclaredField("description");
        descriptionField.setAccessible(true);
        descriptionField.set(createCmd, "testing create gslb-rule");

        Field serviceDomainField = _class.getDeclaredField("serviceDomainName");
        serviceDomainField.setAccessible(true);
        serviceDomainField.set(createCmd, "gslb-rule-domain");
        GlobalLoadBalancerRuleVO gslbRule = new GlobalLoadBalancerRuleVO();
        when(gslbServiceImpl._gslbRuleDao.findByDomainName("gslb-rule-domain")).thenReturn(gslbRule);

        Field serviceTypeField = _class.getDeclaredField("serviceType");
        serviceTypeField.setAccessible(true);
        serviceTypeField.set(createCmd, "tcp");

        try {
            gslbServiceImpl.createGlobalLoadBalancerRule(createCmd);
        } catch (InvalidParameterValueException e) {
            Assert.assertTrue(e.getMessage().contains("Domain name " + "gslb-rule-domain" + "is in use"));
        }
    }

    void runAssignToGlobalLoadBalancerRuleTest() throws Exception {

        Transaction txn = Transaction.open("runAssignToGlobalLoadBalancerRuleTest");

        GlobalLoadBalancingRulesServiceImpl gslbServiceImpl =  new GlobalLoadBalancingRulesServiceImpl();

        gslbServiceImpl._accountMgr = Mockito.mock(AccountManager.class);
        gslbServiceImpl._gslbRuleDao = Mockito.mock(GlobalLoadBalancerRuleDao.class);
        gslbServiceImpl._gslbLbMapDao = Mockito.mock(GlobalLoadBalancerLbRuleMapDao.class);
        gslbServiceImpl._regionDao = Mockito.mock(RegionDao.class);
        gslbServiceImpl._rulesMgr = Mockito.mock(RulesManager.class);
        gslbServiceImpl._lbDao = Mockito.mock(LoadBalancerDao.class);
        gslbServiceImpl._networkDao = Mockito.mock(NetworkDao.class);
        gslbServiceImpl._globalConfigDao = Mockito.mock(ConfigurationDao.class);
        gslbServiceImpl._ipAddressDao = Mockito.mock(IPAddressDao.class);
        gslbServiceImpl._agentMgr = Mockito.mock(AgentManager.class);

        AssignToGlobalLoadBalancerRuleCmd assignCmd = new AssignToGlobalLoadBalancerRuleCmdExtn();
        Class<?> _class = assignCmd.getClass().getSuperclass();

        Account account = new AccountVO("testaccount", 1, "networkdomain", (short) 0, UUID.randomUUID().toString());
        when(gslbServiceImpl._accountMgr.getAccount(anyLong())).thenReturn(account);

        Field gslbRuleId = _class.getDeclaredField("id");
        gslbRuleId.setAccessible(true);
        gslbRuleId.set(assignCmd, new Long(1));

        GlobalLoadBalancerRuleVO gslbRule = new  GlobalLoadBalancerRuleVO("test-gslb-rule", "test-gslb-rule",
                "test-domain", "roundrobin", "sourceip", "tcp", 1, 1, 1, GlobalLoadBalancerRule.State.Active);
        when(gslbServiceImpl._gslbRuleDao.findById(new Long(1))).thenReturn(gslbRule);

        LoadBalancerVO lbRule = new LoadBalancerVO();
        lbRule.setState(FirewallRule.State.Active);
        Field networkIdField = LoadBalancerVO.class.getSuperclass().getDeclaredField("networkId");
        networkIdField.setAccessible(true);
        networkIdField.set(lbRule, new Long(1));
        Field sourceIpAddressId = LoadBalancerVO.class.getSuperclass().getDeclaredField("sourceIpAddressId");
        sourceIpAddressId.setAccessible(true);
        sourceIpAddressId.set(lbRule, new Long(1));

        when(gslbServiceImpl._lbDao.findById(new Long(1))).thenReturn(lbRule);
        Field lbRules = _class.getDeclaredField("loadBalancerRulesIds");
        lbRules.setAccessible(true);
        List<Long> lbRuleIds = new ArrayList<Long>();
        lbRuleIds.add(new Long(1));
        lbRules.set(assignCmd, lbRuleIds);

        NetworkVO networkVo = new NetworkVO();
        Field dcID = NetworkVO.class.getDeclaredField("dataCenterId");
        dcID.setAccessible(true);
        dcID.set(networkVo, new Long(1));
        when(gslbServiceImpl._networkDao.findById(new Long(1))).thenReturn(networkVo);

        IPAddressVO ip = new IPAddressVO(new Ip("10.1.1.1"), 1, 1,1 ,true);
        when(gslbServiceImpl._ipAddressDao.findById(new Long(1))).thenReturn(ip);

        try {
            gslbServiceImpl.assignToGlobalLoadBalancerRule(assignCmd);
        } catch (Exception e) {
            s_logger.info("exception in testing runAssignToGlobalLoadBalancerRuleTest message: " + e.toString());
        }
    }

    void runAssignToGlobalLoadBalancerRuleTestSameZoneLb() throws  Exception {

        Transaction txn = Transaction.open("runAssignToGlobalLoadBalancerRuleTestSameZoneLb");

        GlobalLoadBalancingRulesServiceImpl gslbServiceImpl =  new GlobalLoadBalancingRulesServiceImpl();

        gslbServiceImpl._accountMgr = Mockito.mock(AccountManager.class);
        gslbServiceImpl._gslbRuleDao = Mockito.mock(GlobalLoadBalancerRuleDao.class);
        gslbServiceImpl._gslbLbMapDao = Mockito.mock(GlobalLoadBalancerLbRuleMapDao.class);
        gslbServiceImpl._regionDao = Mockito.mock(RegionDao.class);
        gslbServiceImpl._rulesMgr = Mockito.mock(RulesManager.class);
        gslbServiceImpl._lbDao = Mockito.mock(LoadBalancerDao.class);
        gslbServiceImpl._networkDao = Mockito.mock(NetworkDao.class);
        gslbServiceImpl._globalConfigDao = Mockito.mock(ConfigurationDao.class);
        gslbServiceImpl._ipAddressDao = Mockito.mock(IPAddressDao.class);
        gslbServiceImpl._agentMgr = Mockito.mock(AgentManager.class);

        AssignToGlobalLoadBalancerRuleCmd assignCmd = new AssignToGlobalLoadBalancerRuleCmdExtn();
        Class<?> _class = assignCmd.getClass().getSuperclass();

        Account account = new AccountVO("testaccount", 3, "networkdomain", (short) 0, UUID.randomUUID().toString());
        when(gslbServiceImpl._accountMgr.getAccount(anyLong())).thenReturn(account);

        Field gslbRuleId = _class.getDeclaredField("id");
        gslbRuleId.setAccessible(true);
        gslbRuleId.set(assignCmd, new Long(1));

        GlobalLoadBalancerRuleVO gslbRule = new  GlobalLoadBalancerRuleVO("test-gslb-rule", "test-gslb-rule",
                "test-domain", "roundrobin", "sourceip", "tcp", 1, 3, 1, GlobalLoadBalancerRule.State.Active);
        when(gslbServiceImpl._gslbRuleDao.findById(new Long(1))).thenReturn(gslbRule);

        LoadBalancerVO lbRule1 = new LoadBalancerVO();
        lbRule1.setState(FirewallRule.State.Active);
        Field networkIdField1 = LoadBalancerVO.class.getSuperclass().getDeclaredField("networkId");
        Field accountIdField1 = LoadBalancerVO.class.getSuperclass().getDeclaredField("accountId");
        Field domainIdField1 = LoadBalancerVO.class.getSuperclass().getDeclaredField("domainId");
        networkIdField1.setAccessible(true);
        accountIdField1.setAccessible(true);
        domainIdField1.setAccessible(true);
        networkIdField1.set(lbRule1, new Long(1));
        accountIdField1.set(lbRule1, new Long(3));
        domainIdField1.set(lbRule1, new Long(1));
        Field idField1 = LoadBalancerVO.class.getSuperclass().getDeclaredField("id");
        idField1.setAccessible(true);
        idField1.set(lbRule1, new Long(1));

        LoadBalancerVO lbRule2 = new LoadBalancerVO();
        lbRule2.setState(FirewallRule.State.Active);
        Field networkIdField2 = LoadBalancerVO.class.getSuperclass().getDeclaredField("networkId");
        Field accountIdField2 = LoadBalancerVO.class.getSuperclass().getDeclaredField("accountId");
        Field domainIdField2 = LoadBalancerVO.class.getSuperclass().getDeclaredField("domainId");
        networkIdField2.setAccessible(true);
        accountIdField2.setAccessible(true);
        domainIdField2.setAccessible(true);
        networkIdField2.set(lbRule2, new Long(1));
        accountIdField2.set(lbRule2, new Long(3));
        domainIdField2.set(lbRule2, new Long(1));
        Field idField2 = LoadBalancerVO.class.getSuperclass().getDeclaredField("id");
        idField2.setAccessible(true);
        idField2.set(lbRule2, new Long(2));

        when(gslbServiceImpl._lbDao.findById(new Long(1))).thenReturn(lbRule1);
        when(gslbServiceImpl._lbDao.findById(new Long(2))).thenReturn(lbRule2);

        Field lbRules = _class.getDeclaredField("loadBalancerRulesIds");
        lbRules.setAccessible(true);
        List<Long> lbRuleIds = new ArrayList<Long>();
        lbRuleIds.add(new Long(1));
        lbRuleIds.add(new Long(2));
        lbRules.set(assignCmd, lbRuleIds);

        NetworkVO networkVo = new NetworkVO();
        Field dcID = NetworkVO.class.getDeclaredField("dataCenterId");
        dcID.setAccessible(true);
        dcID.set(networkVo, new Long(1));
        when(gslbServiceImpl._networkDao.findById(new Long(1))).thenReturn(networkVo);

        try {
            gslbServiceImpl.assignToGlobalLoadBalancerRule(assignCmd);
        } catch (InvalidParameterValueException e) {
            s_logger.info(e.getMessage());
            Assert.assertTrue(e.getMessage().contains("Load balancer rule specified should be in unique zone"));
        }
    }

    void runAssignToGlobalLoadBalancerRuleTestRevokedState() throws Exception {

        Transaction txn = Transaction.open("runAssignToGlobalLoadBalancerRuleTestRevokedState");

        GlobalLoadBalancingRulesServiceImpl gslbServiceImpl =  new GlobalLoadBalancingRulesServiceImpl();

        gslbServiceImpl._accountMgr = Mockito.mock(AccountManager.class);
        gslbServiceImpl._gslbRuleDao = Mockito.mock(GlobalLoadBalancerRuleDao.class);
        gslbServiceImpl._gslbLbMapDao = Mockito.mock(GlobalLoadBalancerLbRuleMapDao.class);
        gslbServiceImpl._regionDao = Mockito.mock(RegionDao.class);
        gslbServiceImpl._rulesMgr = Mockito.mock(RulesManager.class);
        gslbServiceImpl._lbDao = Mockito.mock(LoadBalancerDao.class);
        gslbServiceImpl._networkDao = Mockito.mock(NetworkDao.class);
        gslbServiceImpl._globalConfigDao = Mockito.mock(ConfigurationDao.class);
        gslbServiceImpl._ipAddressDao = Mockito.mock(IPAddressDao.class);
        gslbServiceImpl._agentMgr = Mockito.mock(AgentManager.class);

        AssignToGlobalLoadBalancerRuleCmd assignCmd = new AssignToGlobalLoadBalancerRuleCmdExtn();
        Class<?> _class = assignCmd.getClass().getSuperclass();

        Account account = new AccountVO("testaccount", 1, "networkdomain", (short) 0, UUID.randomUUID().toString());
        when(gslbServiceImpl._accountMgr.getAccount(anyLong())).thenReturn(account);

        Field gslbRuleId = _class.getDeclaredField("id");
        gslbRuleId.setAccessible(true);
        gslbRuleId.set(assignCmd, new Long(1));

        GlobalLoadBalancerRuleVO gslbRule = new  GlobalLoadBalancerRuleVO("test-gslb-rule", "test-gslb-rule",
                "test-domain", "roundrobin", "sourceip", "tcp", 1, 1, 1, GlobalLoadBalancerRule.State.Revoke);
        when(gslbServiceImpl._gslbRuleDao.findById(new Long(1))).thenReturn(gslbRule);

        LoadBalancerVO lbRule = new LoadBalancerVO();
        lbRule.setState(FirewallRule.State.Active);
        Field networkIdField = LoadBalancerVO.class.getSuperclass().getDeclaredField("networkId");
        networkIdField.setAccessible(true);
        networkIdField.set(lbRule, new Long(1));

        when(gslbServiceImpl._lbDao.findById(new Long(1))).thenReturn(lbRule);
        Field lbRules = _class.getDeclaredField("loadBalancerRulesIds");
        lbRules.setAccessible(true);
        List<Long> lbRuleIds = new ArrayList<Long>();
        lbRuleIds.add(new Long(1));
        lbRules.set(assignCmd, lbRuleIds);

        NetworkVO networkVo = new NetworkVO();
        Field dcID = NetworkVO.class.getDeclaredField("dataCenterId");
        dcID.setAccessible(true);
        dcID.set(networkVo, new Long(1));
        when(gslbServiceImpl._networkDao.findById(new Long(1))).thenReturn(networkVo);

        try {
            gslbServiceImpl.assignToGlobalLoadBalancerRule(assignCmd);
        } catch (InvalidParameterValueException e) {
            Assert.assertTrue(e.getMessage().contains("revoked state"));
        }
    }

    void runRemoveFromGlobalLoadBalancerRuleTest() throws Exception {

        Transaction txn = Transaction.open("runRemoveFromGlobalLoadBalancerRuleTest");

        GlobalLoadBalancingRulesServiceImpl gslbServiceImpl =  new GlobalLoadBalancingRulesServiceImpl();

        gslbServiceImpl._accountMgr = Mockito.mock(AccountManager.class);
        gslbServiceImpl._gslbRuleDao = Mockito.mock(GlobalLoadBalancerRuleDao.class);
        gslbServiceImpl._gslbLbMapDao = Mockito.mock(GlobalLoadBalancerLbRuleMapDao.class);
        gslbServiceImpl._regionDao = Mockito.mock(RegionDao.class);
        gslbServiceImpl._rulesMgr = Mockito.mock(RulesManager.class);
        gslbServiceImpl._lbDao = Mockito.mock(LoadBalancerDao.class);
        gslbServiceImpl._networkDao = Mockito.mock(NetworkDao.class);
        gslbServiceImpl._globalConfigDao = Mockito.mock(ConfigurationDao.class);
        gslbServiceImpl._ipAddressDao = Mockito.mock(IPAddressDao.class);
        gslbServiceImpl._agentMgr = Mockito.mock(AgentManager.class);
        gslbServiceImpl._gslbProvider = Mockito.mock(GslbServiceProvider.class);

        RemoveFromGlobalLoadBalancerRuleCmd removeFromGslbCmd = new RemoveFromGlobalLoadBalancerRuleCmdExtn();
        Class<?> _class = removeFromGslbCmd.getClass().getSuperclass();

        Account account = new AccountVO("testaccount", 1, "networkdomain", (short) 0, UUID.randomUUID().toString());
        when(gslbServiceImpl._accountMgr.getAccount(anyLong())).thenReturn(account);

        Field gslbRuleId = _class.getDeclaredField("id");
        gslbRuleId.setAccessible(true);
        gslbRuleId.set(removeFromGslbCmd, new Long(1));

        GlobalLoadBalancerRuleVO gslbRule = new  GlobalLoadBalancerRuleVO("test-gslb-rule", "test-gslb-rule",
                "test-domain", "roundrobin", "sourceip", "tcp", 1, 1, 1, GlobalLoadBalancerRule.State.Active);
        when(gslbServiceImpl._gslbRuleDao.findById(new Long(1))).thenReturn(gslbRule);

        LoadBalancerVO lbRule = new LoadBalancerVO();
        lbRule.setState(FirewallRule.State.Active);
        Field networkIdField = LoadBalancerVO.class.getSuperclass().getDeclaredField("networkId");
        networkIdField.setAccessible(true);
        networkIdField.set(lbRule, new Long(1));
        Field idField = LoadBalancerVO.class.getSuperclass().getDeclaredField("id");
        idField.setAccessible(true);
        idField.set(lbRule, new Long(1));
        Field sourceIpAddressId = LoadBalancerVO.class.getSuperclass().getDeclaredField("sourceIpAddressId");
        sourceIpAddressId.setAccessible(true);
        sourceIpAddressId.set(lbRule, new Long(1));

        when(gslbServiceImpl._lbDao.findById(new Long(1))).thenReturn(lbRule);
        Field lbRules = _class.getDeclaredField("loadBalancerRulesIds");
        lbRules.setAccessible(true);
        List<Long> lbRuleIds = new ArrayList<Long>();
        lbRuleIds.add(new Long(1));
        lbRules.set(removeFromGslbCmd, lbRuleIds);

        NetworkVO networkVo = new NetworkVO();
        Field dcID = NetworkVO.class.getDeclaredField("dataCenterId");
        dcID.setAccessible(true);
        dcID.set(networkVo, new Long(1));
        Field phyNetworkId = NetworkVO.class.getDeclaredField("physicalNetworkId");
        phyNetworkId.setAccessible(true);
        phyNetworkId.set(networkVo, new Long(200));
        when(gslbServiceImpl._networkDao.findById(new Long(1))).thenReturn(networkVo);

        GlobalLoadBalancerLbRuleMapVO gslbLbMap = new GlobalLoadBalancerLbRuleMapVO(1, 1, 1);
        List<GlobalLoadBalancerLbRuleMapVO>  listSslbLbMap = new ArrayList<GlobalLoadBalancerLbRuleMapVO>();
        listSslbLbMap.add(gslbLbMap);
        when(gslbServiceImpl._gslbLbMapDao.listByGslbRuleId(new Long(1))).thenReturn(listSslbLbMap);

        when(gslbServiceImpl._gslbLbMapDao.findByGslbRuleIdAndLbRuleId(new Long(1), new Long(1))).thenReturn(gslbLbMap);

        IPAddressVO ip = new IPAddressVO(new Ip("10.1.1.1"), 1, 1,1 ,true);
        when(gslbServiceImpl._ipAddressDao.findById(new Long(1))).thenReturn(ip);

        gslbServiceImpl.removeFromGlobalLoadBalancerRule(removeFromGslbCmd);
    }

    void runRemoveFromGlobalLoadBalancerRuleTestUnassignedLb() throws Exception {

        Transaction txn = Transaction.open("runRemoveFromGlobalLoadBalancerRuleTestUnassignedLb");

        GlobalLoadBalancingRulesServiceImpl gslbServiceImpl =  new GlobalLoadBalancingRulesServiceImpl();

        gslbServiceImpl._accountMgr = Mockito.mock(AccountManager.class);
        gslbServiceImpl._gslbRuleDao = Mockito.mock(GlobalLoadBalancerRuleDao.class);
        gslbServiceImpl._gslbLbMapDao = Mockito.mock(GlobalLoadBalancerLbRuleMapDao.class);
        gslbServiceImpl._regionDao = Mockito.mock(RegionDao.class);
        gslbServiceImpl._rulesMgr = Mockito.mock(RulesManager.class);
        gslbServiceImpl._lbDao = Mockito.mock(LoadBalancerDao.class);
        gslbServiceImpl._networkDao = Mockito.mock(NetworkDao.class);
        gslbServiceImpl._globalConfigDao = Mockito.mock(ConfigurationDao.class);
        gslbServiceImpl._ipAddressDao = Mockito.mock(IPAddressDao.class);
        gslbServiceImpl._agentMgr = Mockito.mock(AgentManager.class);

        RemoveFromGlobalLoadBalancerRuleCmd removeFromGslbCmd = new RemoveFromGlobalLoadBalancerRuleCmdExtn();
        Class<?> _class = removeFromGslbCmd.getClass().getSuperclass();

        Account account = new AccountVO("testaccount", 1, "networkdomain", (short) 0, UUID.randomUUID().toString());
        when(gslbServiceImpl._accountMgr.getAccount(anyLong())).thenReturn(account);

        Field gslbRuleId = _class.getDeclaredField("id");
        gslbRuleId.setAccessible(true);
        gslbRuleId.set(removeFromGslbCmd, new Long(1));

        GlobalLoadBalancerRuleVO gslbRule = new  GlobalLoadBalancerRuleVO("test-gslb-rule", "test-gslb-rule",
                "test-domain", "roundrobin", "sourceip", "tcp", 1, 1, 1, GlobalLoadBalancerRule.State.Active);
        when(gslbServiceImpl._gslbRuleDao.findById(new Long(1))).thenReturn(gslbRule);

        LoadBalancerVO lbRule = new LoadBalancerVO();
        lbRule.setState(FirewallRule.State.Active);
        Field networkIdField = LoadBalancerVO.class.getSuperclass().getDeclaredField("networkId");
        networkIdField.setAccessible(true);
        networkIdField.set(lbRule, new Long(1));
        Field idField = LoadBalancerVO.class.getSuperclass().getDeclaredField("id");
        idField.setAccessible(true);
        idField.set(lbRule, new Long(1));

        when(gslbServiceImpl._lbDao.findById(new Long(1))).thenReturn(lbRule);

        Field lbRules = _class.getDeclaredField("loadBalancerRulesIds");
        lbRules.setAccessible(true);
        List<Long> lbRuleIds = new ArrayList<Long>();
        lbRuleIds.add(new Long(1));
        lbRules.set(removeFromGslbCmd, lbRuleIds);

        NetworkVO networkVo = new NetworkVO();
        Field dcID = NetworkVO.class.getDeclaredField("dataCenterId");
        dcID.setAccessible(true);
        dcID.set(networkVo, new Long(1));
        when(gslbServiceImpl._networkDao.findById(new Long(1))).thenReturn(networkVo);

        try {
            gslbServiceImpl.removeFromGlobalLoadBalancerRule(removeFromGslbCmd);
        } catch (InvalidParameterValueException e) {
            Assert.assertTrue(e.getMessage().contains("not assigned to global load balancer rule"));
        }
    }

    void runRemoveFromGlobalLoadBalancerRuleTestInvalidLb() throws Exception {

        Transaction txn = Transaction.open("runRemoveFromGlobalLoadBalancerRuleTestInvalidLb");

        GlobalLoadBalancingRulesServiceImpl gslbServiceImpl =  new GlobalLoadBalancingRulesServiceImpl();

        gslbServiceImpl._accountMgr = Mockito.mock(AccountManager.class);
        gslbServiceImpl._gslbRuleDao = Mockito.mock(GlobalLoadBalancerRuleDao.class);
        gslbServiceImpl._gslbLbMapDao = Mockito.mock(GlobalLoadBalancerLbRuleMapDao.class);
        gslbServiceImpl._regionDao = Mockito.mock(RegionDao.class);
        gslbServiceImpl._rulesMgr = Mockito.mock(RulesManager.class);
        gslbServiceImpl._lbDao = Mockito.mock(LoadBalancerDao.class);
        gslbServiceImpl._networkDao = Mockito.mock(NetworkDao.class);
        gslbServiceImpl._globalConfigDao = Mockito.mock(ConfigurationDao.class);
        gslbServiceImpl._ipAddressDao = Mockito.mock(IPAddressDao.class);
        gslbServiceImpl._agentMgr = Mockito.mock(AgentManager.class);

        RemoveFromGlobalLoadBalancerRuleCmd removeFromGslbCmd = new RemoveFromGlobalLoadBalancerRuleCmdExtn();
        Class<?> _class = removeFromGslbCmd.getClass().getSuperclass();

        Account account = new AccountVO("testaccount", 1, "networkdomain", (short) 0, UUID.randomUUID().toString());
        when(gslbServiceImpl._accountMgr.getAccount(anyLong())).thenReturn(account);

        Field gslbRuleId = _class.getDeclaredField("id");
        gslbRuleId.setAccessible(true);
        gslbRuleId.set(removeFromGslbCmd, new Long(1));

        GlobalLoadBalancerRuleVO gslbRule = new  GlobalLoadBalancerRuleVO("test-gslb-rule", "test-gslb-rule",
                "test-domain", "roundrobin", "sourceip", "tcp", 1, 1, 1, GlobalLoadBalancerRule.State.Active);
        when(gslbServiceImpl._gslbRuleDao.findById(new Long(1))).thenReturn(gslbRule);

        Field lbRules = _class.getDeclaredField("loadBalancerRulesIds");
        lbRules.setAccessible(true);
        List<Long> lbRuleIds = new ArrayList<Long>();
        lbRuleIds.add(new Long(1));
        lbRules.set(removeFromGslbCmd, lbRuleIds);

        try {
            gslbServiceImpl.removeFromGlobalLoadBalancerRule(removeFromGslbCmd);
        } catch (InvalidParameterValueException e) {
            Assert.assertTrue(e.getMessage().contains("load balancer rule ID does not exist"));
        }
    }

    void runDeleteGlobalLoadBalancerRuleTestWithNoLbRules() throws Exception {

        Transaction txn = Transaction.open("runDeleteGlobalLoadBalancerRuleTestWithNoLbRules");

        GlobalLoadBalancingRulesServiceImpl gslbServiceImpl =  new GlobalLoadBalancingRulesServiceImpl();

        gslbServiceImpl._accountMgr = Mockito.mock(AccountManager.class);
        gslbServiceImpl._gslbRuleDao = Mockito.mock(GlobalLoadBalancerRuleDao.class);
        gslbServiceImpl._gslbLbMapDao = Mockito.mock(GlobalLoadBalancerLbRuleMapDao.class);
        gslbServiceImpl._regionDao = Mockito.mock(RegionDao.class);
        gslbServiceImpl._rulesMgr = Mockito.mock(RulesManager.class);
        gslbServiceImpl._lbDao = Mockito.mock(LoadBalancerDao.class);
        gslbServiceImpl._networkDao = Mockito.mock(NetworkDao.class);
        gslbServiceImpl._globalConfigDao = Mockito.mock(ConfigurationDao.class);
        gslbServiceImpl._ipAddressDao = Mockito.mock(IPAddressDao.class);
        gslbServiceImpl._agentMgr = Mockito.mock(AgentManager.class);

        DeleteGlobalLoadBalancerRuleCmd deleteCmd = new DeleteGlobalLoadBalancerRuleCmdExtn();
        Class<?> _class = deleteCmd.getClass().getSuperclass();

        Account account = new AccountVO("testaccount", 1, "networkdomain", (short) 0, UUID.randomUUID().toString());
        when(gslbServiceImpl._accountMgr.getAccount(anyLong())).thenReturn(account);

        Field gslbRuleId = _class.getDeclaredField("id");
        gslbRuleId.setAccessible(true);
        gslbRuleId.set(deleteCmd, new Long(1));

        GlobalLoadBalancerRuleVO gslbRule = new  GlobalLoadBalancerRuleVO("test-gslb-rule", "test-gslb-rule",
                "test-domain", "roundrobin", "sourceip", "tcp", 1, 1, 1, GlobalLoadBalancerRule.State.Active);
        when(gslbServiceImpl._gslbRuleDao.findById(new Long(1))).thenReturn(gslbRule);

        GlobalLoadBalancerLbRuleMapVO gslbLbMap = new GlobalLoadBalancerLbRuleMapVO();
        gslbLbMap.setGslbLoadBalancerId(1);
        gslbLbMap.setLoadBalancerId(1);
        List<GlobalLoadBalancerLbRuleMapVO>  gslbLbMapList = new ArrayList<GlobalLoadBalancerLbRuleMapVO>();
        gslbLbMapList.add(gslbLbMap);
        when(gslbServiceImpl._gslbLbMapDao.listByGslbRuleId(new Long(1))).thenReturn(gslbLbMapList);

        try {
            gslbServiceImpl.deleteGlobalLoadBalancerRule(deleteCmd);
            Assert.assertTrue(gslbRule.getState() == GlobalLoadBalancerRule.State.Revoke);
        } catch (Exception e) {
            s_logger.info("exception in testing runDeleteGlobalLoadBalancerRuleTestWithNoLbRules. " + e.toString());
        }
    }

    void runDeleteGlobalLoadBalancerRuleTestWithLbRules() throws Exception {

        Transaction txn = Transaction.open("runDeleteGlobalLoadBalancerRuleTestWithLbRules");

        GlobalLoadBalancingRulesServiceImpl gslbServiceImpl =  new GlobalLoadBalancingRulesServiceImpl();

        gslbServiceImpl._accountMgr = Mockito.mock(AccountManager.class);
        gslbServiceImpl._gslbRuleDao = Mockito.mock(GlobalLoadBalancerRuleDao.class);
        gslbServiceImpl._gslbLbMapDao = Mockito.mock(GlobalLoadBalancerLbRuleMapDao.class);
        gslbServiceImpl._regionDao = Mockito.mock(RegionDao.class);
        gslbServiceImpl._rulesMgr = Mockito.mock(RulesManager.class);
        gslbServiceImpl._lbDao = Mockito.mock(LoadBalancerDao.class);
        gslbServiceImpl._networkDao = Mockito.mock(NetworkDao.class);
        gslbServiceImpl._globalConfigDao = Mockito.mock(ConfigurationDao.class);
        gslbServiceImpl._ipAddressDao = Mockito.mock(IPAddressDao.class);
        gslbServiceImpl._agentMgr = Mockito.mock(AgentManager.class);

        DeleteGlobalLoadBalancerRuleCmd deleteCmd = new DeleteGlobalLoadBalancerRuleCmdExtn();
        Class<?> _class = deleteCmd.getClass().getSuperclass();

        Account account = new AccountVO("testaccount", 1, "networkdomain", (short) 0, UUID.randomUUID().toString());
        when(gslbServiceImpl._accountMgr.getAccount(anyLong())).thenReturn(account);

        Field gslbRuleId = _class.getDeclaredField("id");
        gslbRuleId.setAccessible(true);
        gslbRuleId.set(deleteCmd, new Long(1));

        GlobalLoadBalancerRuleVO gslbRule = new  GlobalLoadBalancerRuleVO("test-gslb-rule", "test-gslb-rule",
                "test-domain", "roundrobin", "sourceip", "tcp", 1, 1, 1, GlobalLoadBalancerRule.State.Active);
        when(gslbServiceImpl._gslbRuleDao.findById(new Long(1))).thenReturn(gslbRule);


        GlobalLoadBalancerLbRuleMapVO gslbLmMap = new GlobalLoadBalancerLbRuleMapVO(1,1,1);
        List<GlobalLoadBalancerLbRuleMapVO> gslbLbMapVos = new ArrayList<GlobalLoadBalancerLbRuleMapVO>();
        gslbLbMapVos.add(gslbLmMap);
        when(gslbServiceImpl._gslbLbMapDao.listByGslbRuleId(new Long(1))).thenReturn(gslbLbMapVos);

        try {
            gslbServiceImpl.deleteGlobalLoadBalancerRule(deleteCmd);
            Assert.assertTrue(gslbRule.getState() == GlobalLoadBalancerRule.State.Revoke);
            Assert.assertTrue(gslbLmMap.isRevoke() == true);
        } catch (Exception e) {
            s_logger.info("exception in testing runDeleteGlobalLoadBalancerRuleTestWithLbRules. " + e.toString());
        }
    }

    public class CreateGlobalLoadBalancerRuleCmdExtn extends CreateGlobalLoadBalancerRuleCmd {
        @Override
        public long getEntityOwnerId() {
            return 1;
        }
    }

    public class AssignToGlobalLoadBalancerRuleCmdExtn extends AssignToGlobalLoadBalancerRuleCmd {
        @Override
        public long getEntityOwnerId() {
            return 1;
        }
    }

    public class RemoveFromGlobalLoadBalancerRuleCmdExtn extends RemoveFromGlobalLoadBalancerRuleCmd {
        @Override
        public long getEntityOwnerId() {
            return 1;
        }
    }

    public class DeleteGlobalLoadBalancerRuleCmdExtn extends DeleteGlobalLoadBalancerRuleCmd {
        @Override
        public long getEntityOwnerId() {
            return 1;
        }
    }
}
