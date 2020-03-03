package org.apache.cloudstack.network.ip;

import com.cloud.dc.dao.VlanDao;
import com.cloud.exception.ResourceUnavailableException;
import com.cloud.network.IpAddressManagerImpl;
import com.cloud.network.Network;
import com.cloud.network.NetworkModel;
import com.cloud.network.addr.PublicIp;
import com.cloud.network.dao.FirewallRulesDao;
import com.cloud.network.dao.IPAddressDao;
import com.cloud.network.dao.IPAddressVO;
import com.cloud.network.dao.NetworkDao;
import com.cloud.network.element.StaticNatServiceProvider;
import com.cloud.network.rules.FirewallRule;
import com.cloud.network.rules.StaticNat;
import org.apache.cloudstack.engine.orchestration.service.NetworkOrchestrationService;
import org.apache.log4j.Logger;

import javax.inject.Inject;
import java.util.ArrayList;
import java.util.List;

public class StaticNatter {
    private static final Logger LOG = Logger.getLogger(StaticNatter.class);
    private final IpAddressManagerImpl ipAddressManagerImpl;

    @Inject
    FirewallRulesDao firewallDao;
    @Inject
    IPAddressDao ipAddressDao;
    @Inject
    NetworkOrchestrationService networkMgr;
    @Inject
    NetworkModel networkModel;
    @Inject
    NetworkDao networksDao;
    @Inject
    VlanDao vlanDao;

    public StaticNatter(IpAddressManagerImpl ipAddressManagerImpl) {
        this.ipAddressManagerImpl = ipAddressManagerImpl;
    }

    public boolean applyStaticNats(List<? extends StaticNat> staticNats, boolean continueOnError, boolean forRevoke) throws ResourceUnavailableException {
        if (staticNats == null || staticNats.size() == 0) {
            LOG.debug("There are no static nat rules for the network elements");
            return true;
        }

        Network network = networksDao.findById(staticNats.get(0).getNetworkId());
        boolean success = true;

        // Check if the StaticNat service is supported
        if (!networkModel.areServicesSupportedInNetwork(network.getId(), Network.Service.StaticNat)) {
            LOG.debug("StaticNat service is not supported in specified network id");
            return true;
        }

        List<IPAddressVO> userIps = ipAddressManagerImpl.getStaticNatSourceIps(staticNats);

        List<PublicIp> publicIps = new ArrayList<PublicIp>();
        if (userIps != null && !userIps.isEmpty()) {
            for (IPAddressVO userIp : userIps) {
                PublicIp publicIp = PublicIp.createFromAddrAndVlan(userIp, vlanDao.findById(userIp.getVlanId()));
                publicIps.add(publicIp);
            }
        }

        // static NAT rules can not programmed unless IP is associated with source NAT service provider, so run IP
        // association for the network so as to ensure IP is associated before applying rules
        if (checkStaticNatIPAssocRequired(network, false, forRevoke, publicIps)) {
            ipAddressManagerImpl.applyIpAssociations(network, false, continueOnError, publicIps);
        }

        // get provider
        StaticNatServiceProvider element = networkMgr.getStaticNatProviderForNetwork(network);
        try {
            success = element.applyStaticNats(network, staticNats);
        } catch (ResourceUnavailableException e) {
            if (!continueOnError) {
                throw e;
            }
            LOG.warn("Problems with " + element.getName() + " but pushing on", e);
            success = false;
        }

        // For revoked static nat IP, set the vm_id to null, indicate it should be revoked
        for (StaticNat staticNat : staticNats) {
            if (staticNat.isForRevoke()) {
                for (PublicIp publicIp : publicIps) {
                    if (publicIp.getId() == staticNat.getSourceIpAddressId()) {
                        publicIps.remove(publicIp);
                        IPAddressVO ip  = ipAddressDao.findByIdIncludingRemoved(staticNat.getSourceIpAddressId());
                        // ip can't be null, otherwise something wrong happened
                        ip.setAssociatedWithVmId(null);
                        publicIp = PublicIp.createFromAddrAndVlan(ip, vlanDao.findById(ip.getVlanId()));
                        publicIps.add(publicIp);
                        break;
                    }
                }
            }
        }

        // if the static NAT rules configured on public IP is revoked then, dis-associate IP with static NAT service provider
        if (checkStaticNatIPAssocRequired(network, true, forRevoke, publicIps)) {
            ipAddressManagerImpl.applyIpAssociations(network, true, continueOnError, publicIps);
        }

        return success;
    }// checks if there are any public IP assigned to network, that are marked for one-to-one NAT that

    // needs to be associated/dis-associated with static-nat provider
    public boolean checkStaticNatIPAssocRequired(Network network, boolean postApplyRules, boolean forRevoke, List<PublicIp> publicIps) {
        for (PublicIp ip : publicIps) {
            if (ip.isOneToOneNat()) {
                Long activeFwCount = null;
                activeFwCount = firewallDao.countRulesByIpIdAndState(ip.getId(), FirewallRule.State.Active);

                if (!postApplyRules && !forRevoke) {
                    if (activeFwCount > 0) {
                        continue;
                    } else {
                        return true;
                    }
                } else if (postApplyRules && forRevoke) {
                    return true;
                }
            } else {
                continue;
            }
        }
        return false;
    }
}