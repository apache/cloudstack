package org.cloud.network.router.deployment;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;

import javax.inject.Inject;

import org.apache.cloudstack.engine.orchestration.service.NetworkOrchestrationService;

import com.cloud.dc.dao.HostPodDao;
import com.cloud.dc.dao.VlanDao;
import com.cloud.deploy.DeployDestination;
import com.cloud.network.IpAddressManager;
import com.cloud.network.Network;
import com.cloud.network.NetworkModel;
import com.cloud.network.dao.IPAddressDao;
import com.cloud.network.dao.NetworkDao;
import com.cloud.network.dao.PhysicalNetworkDao;
import com.cloud.network.dao.PhysicalNetworkServiceProviderDao;
import com.cloud.network.dao.UserIpv6AddressDao;
import com.cloud.network.dao.VirtualRouterProviderDao;
import com.cloud.network.router.NetworkGeneralHelper;
import com.cloud.network.router.VpcVirtualNetworkHelperImpl;
import com.cloud.network.vpc.Vpc;
import com.cloud.network.vpc.VpcManager;
import com.cloud.network.vpc.dao.VpcDao;
import com.cloud.network.vpc.dao.VpcOfferingDao;
import com.cloud.offerings.dao.NetworkOfferingDao;
import com.cloud.user.Account;
import com.cloud.user.AccountManager;
import com.cloud.vm.DomainRouterVO;
import com.cloud.vm.VirtualMachineProfile.Param;
import com.cloud.vm.dao.DomainRouterDao;
import com.cloud.vm.dao.NicDao;
import com.cloud.vm.dao.VMInstanceDao;

public class RouterDeploymentDefinitionBuilder {

    @Inject
    protected NetworkDao networkDao;
    @Inject
    private DomainRouterDao routerDao = null;
    @Inject
    private PhysicalNetworkServiceProviderDao physicalProviderDao;
    @Inject
    private NetworkModel networkModel;
    @Inject
    private VirtualRouterProviderDao vrProviderDao;
    @Inject
    private NetworkOfferingDao networkOfferingDao;
    @Inject
    private IpAddressManager ipAddrMgr;
    @Inject
    private VMInstanceDao vmDao;
    @Inject
    private HostPodDao podDao;
    @Inject
    private AccountManager accountMgr;
    @Inject
    private NetworkOrchestrationService networkMgr;
    @Inject
    private NicDao nicDao;
    @Inject
    private UserIpv6AddressDao ipv6Dao;
    @Inject
    private IPAddressDao ipAddressDao;
    @Inject
    private VpcDao vpcDao;
    @Inject
    private VpcOfferingDao vpcOffDao;
    @Inject
    private PhysicalNetworkDao pNtwkDao;
    @Inject
    private VpcManager vpcMgr;
    @Inject
    private VlanDao vlanDao;

    @Inject
    protected NetworkGeneralHelper nwHelper;
    @Inject
    protected VpcVirtualNetworkHelperImpl vpcHelper;

    protected Long offeringId;

    public void setOfferingId(Long offeringId) {
        this.offeringId = offeringId;
    }

    public IntermediateStateBuilder create() {
        return new IntermediateStateBuilder(this);
    }

    protected RouterDeploymentDefinition injectDependencies(
            final RouterDeploymentDefinition routerDeploymentDefinition) {

        routerDeploymentDefinition.networkDao = this.networkDao;
        routerDeploymentDefinition.routerDao = this.routerDao;
        routerDeploymentDefinition.physicalProviderDao = this.physicalProviderDao;
        routerDeploymentDefinition.networkModel = this.networkModel;
        routerDeploymentDefinition.vrProviderDao = this.vrProviderDao;
        routerDeploymentDefinition.networkOfferingDao = this.networkOfferingDao;
        routerDeploymentDefinition.ipAddrMgr = this.ipAddrMgr;
        routerDeploymentDefinition.vmDao = this.vmDao;
        routerDeploymentDefinition.podDao = this.podDao;
        routerDeploymentDefinition.accountMgr = this.accountMgr;
        routerDeploymentDefinition.networkMgr = this.networkMgr;
        routerDeploymentDefinition.nicDao = this.nicDao;
        routerDeploymentDefinition.ipv6Dao = this.ipv6Dao;
        routerDeploymentDefinition.ipAddressDao = this.ipAddressDao;
        routerDeploymentDefinition.offeringId = this.offeringId;

        routerDeploymentDefinition.nwHelper = this.nwHelper;
        routerDeploymentDefinition.vpcHelper = this.vpcHelper;

        if (routerDeploymentDefinition instanceof VpcRouterDeploymentDefinition) {
            this.injectVpcDependencies((VpcRouterDeploymentDefinition) routerDeploymentDefinition);
        }

        return routerDeploymentDefinition;
    }

    protected void injectVpcDependencies(
            final VpcRouterDeploymentDefinition routerDeploymentDefinition) {

        routerDeploymentDefinition.vpcDao = this.vpcDao;
        routerDeploymentDefinition.vpcOffDao = this.vpcOffDao;
        routerDeploymentDefinition.pNtwkDao = this.pNtwkDao;
        routerDeploymentDefinition.vpcMgr = this.vpcMgr;
        routerDeploymentDefinition.vlanDao = this.vlanDao;
    }


    public class IntermediateStateBuilder {

        RouterDeploymentDefinitionBuilder builder;

        protected Vpc vpc;
        protected Network guestNetwork;
        protected DeployDestination dest;
        protected Account owner;
        protected Map<Param, Object> params;
        protected boolean isRedundant;
        protected List<DomainRouterVO> routers = new ArrayList<>();

        protected IntermediateStateBuilder(RouterDeploymentDefinitionBuilder builder) {
            this.builder = builder;
        }

        public IntermediateStateBuilder makeRedundant() {
            this.isRedundant = true;
            return this;
        }

        public IntermediateStateBuilder setRedundant(boolean isRedundant) {
            this.isRedundant = isRedundant;
            return this;
        }

        public IntermediateStateBuilder setVpc(final Vpc vpc) {
            this.vpc = vpc;
            return this;
        }

        public IntermediateStateBuilder setGuestNetwork(final Network nw) {
            this.guestNetwork = nw;
            return this;
        }

        public IntermediateStateBuilder setAccountOwner(final Account owner) {
            this.owner = owner;
            return this;
        }

        public IntermediateStateBuilder setDeployDestination(final DeployDestination dest) {
            this.dest = dest;
            return this;
        }

        public IntermediateStateBuilder setParams(final Map<Param, Object> params) {
            this.params = params;
            return this;
        }

        public RouterDeploymentDefinition build() {
            RouterDeploymentDefinition routerDeploymentDefinition = null;
            if (this.vpc != null) {
                routerDeploymentDefinition = new VpcRouterDeploymentDefinition(vpc, dest, owner, params, isRedundant);
            } else {
                routerDeploymentDefinition = new RouterDeploymentDefinition(guestNetwork, dest, owner, params, isRedundant);
            }

            return builder.injectDependencies(routerDeploymentDefinition);
        }
    }

}
