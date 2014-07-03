package com.cloud.network.rules;

import java.util.List;

import javax.inject.Inject;

import org.apache.cloudstack.framework.config.dao.ConfigurationDao;

import com.cloud.dc.dao.DataCenterDao;
import com.cloud.network.Network;
import com.cloud.network.NetworkModel;
import com.cloud.network.dao.LoadBalancerDao;
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
    protected RouterControlHelper routerControlHelper;


    public LoadBalancingRules createLoadBalancingRules(final Network network,
            final List<LoadBalancingRule> rules) {
        LoadBalancingRules lbRules = new LoadBalancingRules(network, rules);

        lbRules.networkModel = this.networkModel;
        lbRules.dcDao = this.dcDao;
        lbRules.lbMgr = this.lbMgr;
        lbRules.loadBalancerDao = this.loadBalancerDao;
        lbRules.configDao = this.configDao;
        lbRules.nicDao = this.nicDao;
        lbRules.networkOfferingDao = this.networkOfferingDao;
        lbRules.routerDao = this.routerDao;
        lbRules.routerControlHelper = this.routerControlHelper;

        return lbRules;
    }
}
