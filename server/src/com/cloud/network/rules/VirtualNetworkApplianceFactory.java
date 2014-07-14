package com.cloud.network.rules;

import java.util.List;

import javax.inject.Inject;

import org.apache.cloudstack.framework.config.dao.ConfigurationDao;

import com.cloud.dc.dao.DataCenterDao;
import com.cloud.network.Network;
import com.cloud.network.NetworkModel;
import com.cloud.network.dao.FirewallRulesDao;
import com.cloud.network.dao.LoadBalancerDao;
import com.cloud.network.dao.NetworkDao;
import com.cloud.network.lb.LoadBalancingRule;
import com.cloud.network.lb.LoadBalancingRulesManager;
import com.cloud.network.router.NEWVirtualNetworkApplianceManager;
import com.cloud.network.router.RouterControlHelper;
import com.cloud.offerings.dao.NetworkOfferingDao;
import com.cloud.vm.VirtualMachineManager;
import com.cloud.vm.dao.DomainRouterDao;
import com.cloud.vm.dao.NicDao;

public class VirtualNetworkApplianceFactory {

    @Inject
    protected NetworkModel networkModel;

    @Inject
    protected LoadBalancingRulesManager lbMgr;

    @Inject
    protected LoadBalancerDao loadBalancerDao;

    @Inject
    protected ConfigurationDao configDao;

    @Inject
    protected NicDao nicDao;

    @Inject
    protected VirtualMachineManager itMgr;

    @Inject
    protected NetworkOfferingDao networkOfferingDao;

    @Inject
    protected DataCenterDao dcDao;

    @Inject
    protected DomainRouterDao routerDao;

    @Inject
    protected NetworkDao networkDao;

    @Inject
    protected FirewallRulesDao rulesDao;

    @Inject
    protected RouterControlHelper routerControlHelper;

    @Inject
    protected NEWVirtualNetworkApplianceManager applianceManager;


    public LoadBalancingRules createLoadBalancingRules(final Network network,
            final List<LoadBalancingRule> rules) {
        LoadBalancingRules lbRules = new LoadBalancingRules(network, rules);

        initBeans(lbRules);

        return lbRules;
    }

    public FirewallRules createFirewallRules(final Network network,
            final List<? extends FirewallRule> rules) {
        FirewallRules fwRules = new FirewallRules(network, rules);

        initBeans(fwRules);

        fwRules.networkDao = networkDao;
        fwRules.rulesDao = rulesDao;

        return fwRules;
    }

    private void initBeans(final RuleApplier applier) {
        applier.networkModel = networkModel;
        applier.dcDao = dcDao;
        applier.lbMgr = lbMgr;
        applier.loadBalancerDao = loadBalancerDao;
        applier.configDao = configDao;
        applier.nicDao = nicDao;
        applier.itMgr = itMgr;
        applier.networkOfferingDao = networkOfferingDao;
        applier.routerDao = routerDao;
        applier.routerControlHelper = routerControlHelper;
        applier.applianceManager = applianceManager;
    }
}
