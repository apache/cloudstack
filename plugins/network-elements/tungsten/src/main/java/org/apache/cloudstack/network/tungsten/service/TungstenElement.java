package org.apache.cloudstack.network.tungsten.service;

import com.cloud.deploy.DeployDestination;
import com.cloud.exception.ConcurrentOperationException;
import com.cloud.exception.InsufficientCapacityException;
import com.cloud.exception.ResourceUnavailableException;
import com.cloud.network.Network;
import com.cloud.network.NetworkModel;
import com.cloud.network.PhysicalNetworkServiceProvider;
import com.cloud.network.PublicIpAddress;
import com.cloud.network.element.IpDeployer;
import com.cloud.network.element.StaticNatServiceProvider;
import com.cloud.network.rules.StaticNat;
import com.cloud.offering.NetworkOffering;
import com.cloud.utils.component.AdapterBase;
import com.cloud.vm.NicProfile;
import com.cloud.vm.ReservationContext;
import com.cloud.vm.VirtualMachineProfile;
import org.apache.log4j.Logger;
import org.springframework.stereotype.Component;

import javax.inject.Inject;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;

@Component
public class TungstenElement extends AdapterBase implements StaticNatServiceProvider, IpDeployer {

    @Inject
    NetworkModel _networkModel;
    @Inject
    TungstenService _tungstenService;

    private static final Logger s_logger = Logger.getLogger(TungstenElement.class);
    private final Map<Network.Service, Map<Network.Capability, String>> _capabilities = InitCapabilities();

    protected boolean canHandle(final Network network, final Network.Service service) {
        s_logger.debug("Checking if OvsElement can handle service "
                + service.getName() + " on network " + network.getDisplayText());

        if (!_networkModel.isProviderForNetwork(getProvider(), network.getId())) {
            s_logger.debug("OvsElement is not a provider for network "
                    + network.getDisplayText());
            return false;
        }

        return true;
    }

    @Override
    public boolean applyStaticNats(Network config, List<? extends StaticNat> rules) throws ResourceUnavailableException {
        return false;
    }

    @Override
    public IpDeployer getIpDeployer(Network network) {
        return this;
    }

    @Override
    public Map<Network.Service, Map<Network.Capability, String>> getCapabilities() {
        return _capabilities;
    }

    private static Map<Network.Service, Map<Network.Capability, String>> InitCapabilities() {
        Map<Network.Service, Map<Network.Capability, String>> capabilities = new HashMap<Network.Service, Map<Network.Capability, String>>();
        capabilities.put(Network.Service.Dhcp, new HashMap<Network.Capability, String>());
        Map<Network.Capability, String> sourceNatCapabilities = new HashMap<Network.Capability, String>();
        sourceNatCapabilities.put(Network.Capability.SupportedSourceNatTypes, "peraccount");
        capabilities.put(Network.Service.SourceNat, sourceNatCapabilities);
        return capabilities;
    }

    @Override
    public boolean applyIps(Network network, List<? extends PublicIpAddress> ipAddress, Set<Network.Service> services) throws ResourceUnavailableException {
        return true;
    }

    @Override
    public Network.Provider getProvider() {
        return Network.Provider.Tungsten;
    }

    @Override
    public boolean implement(Network network, NetworkOffering offering, DeployDestination dest, ReservationContext context) throws ConcurrentOperationException, ResourceUnavailableException, InsufficientCapacityException {
        return true;
    }

    @Override
    public boolean prepare(Network network, NicProfile nic, VirtualMachineProfile vm, DeployDestination dest, ReservationContext context) throws ConcurrentOperationException, ResourceUnavailableException, InsufficientCapacityException {
        return false;
    }

    @Override
    public boolean release(Network network, NicProfile nic, VirtualMachineProfile vm, ReservationContext context) throws ConcurrentOperationException, ResourceUnavailableException {
        return false;
    }

    @Override
    public boolean shutdown(Network network, ReservationContext context, boolean cleanup) throws ConcurrentOperationException, ResourceUnavailableException {
        if (!canHandle(network, Network.Service.Connectivity)) {
            return false;
        }
        return true;
    }

    @Override
    public boolean destroy(Network network, ReservationContext context) throws ConcurrentOperationException, ResourceUnavailableException {
        return true;
    }

    @Override
    public boolean isReady(PhysicalNetworkServiceProvider provider) {
        return true;
    }

    @Override
    public boolean shutdownProviderInstances(PhysicalNetworkServiceProvider provider, ReservationContext context) throws ConcurrentOperationException, ResourceUnavailableException {
        return false;
    }

    @Override
    public boolean canEnableIndividualServices() {
        return true;
    }

    @Override
    public boolean verifyServicesCombination(Set<Network.Service> services) {
        return true;
    }
}
