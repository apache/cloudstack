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
import com.cloud.network.router.RouterControlHelper;
import com.cloud.offerings.dao.NetworkOfferingDao;
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
    
    private void initBeans(RuleApplier applier) {
    	applier.networkModel = this.networkModel;
        applier.dcDao = this.dcDao;
        applier.lbMgr = this.lbMgr;
        applier.loadBalancerDao = this.loadBalancerDao;
        applier.configDao = this.configDao;
        applier.nicDao = this.nicDao;
        applier.networkOfferingDao = this.networkOfferingDao;
        applier.routerDao = this.routerDao;
        applier.routerControlHelper = this.routerControlHelper;
    }
}
