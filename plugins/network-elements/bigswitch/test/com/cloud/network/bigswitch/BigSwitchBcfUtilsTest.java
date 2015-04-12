package com.cloud.network.bigswitch;

import static org.junit.Assert.assertEquals;

import org.junit.Before;
import org.junit.Test;
import org.mockito.Mock;
import org.mockito.MockitoAnnotations;

import com.cloud.agent.AgentManager;
import com.cloud.dc.dao.VlanDao;
import com.cloud.host.dao.HostDao;
import com.cloud.network.NetworkModel;
import com.cloud.network.dao.BigSwitchBcfDao;
import com.cloud.network.dao.FirewallRulesCidrsDao;
import com.cloud.network.dao.FirewallRulesDao;
import com.cloud.network.dao.IPAddressDao;
import com.cloud.network.dao.NetworkDao;
import com.cloud.network.vpc.NetworkACLItemCidrsDao;
import com.cloud.network.vpc.NetworkACLItemDao;
import com.cloud.network.vpc.dao.VpcDao;
import com.cloud.vm.dao.NicDao;
import com.cloud.vm.dao.VMInstanceDao;

public class BigSwitchBcfUtilsTest {

    @Mock
    NetworkDao networkDao;
    @Mock
    NicDao nicDao;
    @Mock
    VMInstanceDao vmDao;
    @Mock
    HostDao hostDao;
    @Mock
    VpcDao vpcDao;
    @Mock
    BigSwitchBcfDao bigswitchBcfDao;
    @Mock
    AgentManager agentMgr;
    @Mock
    VlanDao vlanDao;
    @Mock
    IPAddressDao ipAddressDao;
    @Mock
    FirewallRulesDao fwRulesDao;
    @Mock
    FirewallRulesCidrsDao fwCidrsDao;
    @Mock
    NetworkACLItemDao aclItemDao;
    @Mock
    NetworkACLItemCidrsDao aclItemCidrsDao;
    @Mock
    NetworkModel networkModel;
    @Mock
    BigSwitchBcfUtils bsUtil;

    @Before
    public void setUp() {
        MockitoAnnotations.initMocks(this);
        bsUtil = new BigSwitchBcfUtils(networkDao, nicDao, vmDao, hostDao,
                vpcDao, bigswitchBcfDao, agentMgr, vlanDao, ipAddressDao,
                fwRulesDao, fwCidrsDao, aclItemDao, aclItemCidrsDao,
                networkModel);
    }

    @Test
    public void getSubnetMaskLengthTest() {
        Integer rc = bsUtil.getSubnetMaskLength("255.255.255.254");
        assertEquals("failed", new Integer(31), rc);
        rc = bsUtil.getSubnetMaskLength("128.255.255.254");
        assertEquals("failed", new Integer(1), rc);
    }
}
