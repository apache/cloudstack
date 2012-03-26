package com.cloud.network;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.Set;

import javax.ejb.Local;
import javax.naming.ConfigurationException;

import com.cloud.acl.ControlledEntity.ACLType;
import com.cloud.api.commands.CreateNetworkCmd;
import com.cloud.api.commands.ListNetworksCmd;
import com.cloud.api.commands.ListTrafficTypeImplementorsCmd;
import com.cloud.api.commands.RestartNetworkCmd;
import com.cloud.dc.DataCenter;
import com.cloud.dc.Vlan;
import com.cloud.dc.Vlan.VlanType;
import com.cloud.deploy.DataCenterDeployment;
import com.cloud.deploy.DeployDestination;
import com.cloud.deploy.DeploymentPlan;
import com.cloud.exception.ConcurrentOperationException;
import com.cloud.exception.InsufficientAddressCapacityException;
import com.cloud.exception.InsufficientCapacityException;
import com.cloud.exception.InsufficientVirtualNetworkCapcityException;
import com.cloud.exception.ResourceAllocationException;
import com.cloud.exception.ResourceUnavailableException;
import com.cloud.hypervisor.Hypervisor.HypervisorType;
import com.cloud.network.Network.Capability;
import com.cloud.network.Network.GuestType;
import com.cloud.network.Network.Provider;
import com.cloud.network.Network.Service;
import com.cloud.network.Networks.TrafficType;
import com.cloud.network.addr.PublicIp;
import com.cloud.network.element.RemoteAccessVPNServiceProvider;
import com.cloud.network.element.UserDataServiceProvider;
import com.cloud.network.guru.NetworkGuru;
import com.cloud.network.rules.FirewallRule;
import com.cloud.network.rules.StaticNat;
import com.cloud.offering.NetworkOffering;
import com.cloud.offerings.NetworkOfferingVO;
import com.cloud.user.Account;
import com.cloud.user.User;
import com.cloud.utils.Pair;
import com.cloud.utils.component.Manager;
import com.cloud.vm.Nic;
import com.cloud.vm.NicProfile;
import com.cloud.vm.ReservationContext;
import com.cloud.vm.VMInstanceVO;
import com.cloud.vm.VirtualMachine;
import com.cloud.vm.VirtualMachineProfile;

@Local(value = { NetworkManager.class, NetworkService.class })
public class MockNetworkManagerImpl implements NetworkManager, Manager, NetworkService {

    @Override
    public List<? extends Network> getIsolatedNetworksOwnedByAccountInZone(long zoneId, Account owner) {
        // TODO Auto-generated method stub
        return null;
    }

    @Override
    public IpAddress associateIP(long ipId) throws ResourceAllocationException, InsufficientAddressCapacityException, ConcurrentOperationException, ResourceUnavailableException {
        // TODO Auto-generated method stub
        return null;
    }

    @Override
    public boolean disassociateIpAddress(long ipAddressId) {
        // TODO Auto-generated method stub
        return false;
    }

    @Override
    public Network createNetwork(CreateNetworkCmd cmd) throws InsufficientCapacityException, ConcurrentOperationException {
        // TODO Auto-generated method stub
        return null;
    }

    @Override
    public List<? extends Network> searchForNetworks(ListNetworksCmd cmd) {
        // TODO Auto-generated method stub
        return null;
    }

    @Override
    public boolean deleteNetwork(long networkId) {
        // TODO Auto-generated method stub
        return false;
    }

    @Override
    public int getActiveNicsInNetwork(long networkId) {
        // TODO Auto-generated method stub
        return 0;
    }

    @Override
    public Network getNetwork(long networkId) {
        // TODO Auto-generated method stub
        return null;
    }

    @Override
    public IpAddress getIp(long id) {
        // TODO Auto-generated method stub
        return null;
    }

    @Override
    public NetworkProfile convertNetworkToNetworkProfile(long networkId) {
        // TODO Auto-generated method stub
        return null;
    }

    @Override
    public Map<Service, Map<Capability, String>> getNetworkCapabilities(long networkId) {
        // TODO Auto-generated method stub
        return null;
    }

    @Override
    public boolean isNetworkAvailableInDomain(long networkId, long domainId) {
        // TODO Auto-generated method stub
        return false;
    }

    @Override
    public Long getDedicatedNetworkDomain(long networkId) {
        // TODO Auto-generated method stub
        return null;
    }

    @Override
    public Integer getNetworkRate(long networkId, Long vmId) {
        // TODO Auto-generated method stub
        return null;
    }

    @Override
    public Network getSystemNetworkByZoneAndTrafficType(long zoneId, TrafficType trafficType) {
        // TODO Auto-generated method stub
        return null;
    }

    @Override
    public boolean configure(String name, Map<String, Object> params) throws ConfigurationException {
        return true;
    }

    @Override
    public boolean start() {
        return true;
    }

    @Override
    public boolean stop() {
        // TODO Auto-generated method stub
        return false;
    }

    @Override
    public String getName() {
        // TODO Auto-generated method stub
        return null;
    }



    @Override
    public PublicIp assignSourceNatIpAddress(Account owner, Network network, long callerId) throws ConcurrentOperationException, InsufficientAddressCapacityException {
        // TODO Auto-generated method stub
        return null;
    }

    @Override
    public boolean releasePublicIpAddress(long id, long userId, Account caller) {
        // TODO Auto-generated method stub
        return false;
    }

    @Override
    public List<IPAddressVO> listPublicIpAddressesInVirtualNetwork(long accountId, long dcId, Boolean sourceNat, Long associatedNetworkId) {
        // TODO Auto-generated method stub
        return null;
    }

    @Override
    public List<NetworkVO> setupNetwork(Account owner, NetworkOfferingVO offering, DeploymentPlan plan, String name, String displayText, boolean isDefault)
            throws ConcurrentOperationException {
        // TODO Auto-generated method stub
        return null;
    }

    @Override
    public List<NetworkVO> setupNetwork(Account owner, NetworkOfferingVO offering, Network predefined, DeploymentPlan plan, String name, String displayText, boolean errorIfAlreadySetup, Long domainId,
            ACLType aclType, Boolean subdomainAccess) throws ConcurrentOperationException {
        // TODO Auto-generated method stub
        return null;
    }

    @Override
    public List<NetworkOfferingVO> getSystemAccountNetworkOfferings(String... offeringNames) {
        // TODO Auto-generated method stub
        return null;
    }

    @Override
    public void allocate(VirtualMachineProfile<? extends VMInstanceVO> vm, List<Pair<NetworkVO, NicProfile>> networks) throws InsufficientCapacityException, ConcurrentOperationException {
        // TODO Auto-generated method stub

    }

    @Override
    public void prepare(VirtualMachineProfile<? extends VMInstanceVO> profile, DeployDestination dest, ReservationContext context) throws InsufficientCapacityException, ConcurrentOperationException,
    ResourceUnavailableException {
        // TODO Auto-generated method stub

    }

    @Override
    public void release(VirtualMachineProfile<? extends VMInstanceVO> vmProfile, boolean forced) {
        // TODO Auto-generated method stub

    }

    @Override
    public void cleanupNics(VirtualMachineProfile<? extends VMInstanceVO> vm) {
        // TODO Auto-generated method stub

    }

    @Override
    public void expungeNics(VirtualMachineProfile<? extends VMInstanceVO> vm) {
        // TODO Auto-generated method stub

    }

    @Override
    public List<? extends Nic> getNics(long vmId) {
        // TODO Auto-generated method stub
        return null;
    }

    @Override
    public List<NicProfile> getNicProfiles(VirtualMachine vm) {
        // TODO Auto-generated method stub
        return null;
    }

    @Override
    public String getNextAvailableMacAddressInNetwork(long networkConfigurationId) throws InsufficientAddressCapacityException {
        // TODO Auto-generated method stub
        return null;
    }

    @Override
    public boolean applyRules(List<? extends FirewallRule> rules, boolean continueOnError) throws ResourceUnavailableException {
        // TODO Auto-generated method stub
        return false;
    }

    @Override
    public PublicIpAddress getPublicIpAddress(long ipAddressId) {
        // TODO Auto-generated method stub
        return null;
    }

    @Override
    public List<? extends Vlan> listPodVlans(long podId) {
        // TODO Auto-generated method stub
        return null;
    }

    @Override
    public Pair<NetworkGuru, NetworkVO> implementNetwork(long networkId, DeployDestination dest, ReservationContext context) throws ConcurrentOperationException, ResourceUnavailableException,
    InsufficientCapacityException {
        // TODO Auto-generated method stub
        return null;
    }

    @Override
    public List<NetworkVO> listNetworksUsedByVm(long vmId, boolean isSystem) {
        // TODO Auto-generated method stub
        return null;
    }

    @Override
    public <T extends VMInstanceVO> void prepareNicForMigration(VirtualMachineProfile<T> vm, DeployDestination dest) {
        // TODO Auto-generated method stub

    }

    @Override
    public boolean destroyNetwork(long networkId, ReservationContext context) {
        // TODO Auto-generated method stub
        return false;
    }

    @Override
    public Network createGuestNetwork(long networkOfferingId, String name, String displayText, String gateway, String cidr, String vlanId, String networkDomain, Account owner, boolean isSecurityGroupEnabled,
            Long domainId, PhysicalNetwork physicalNetwork, long zoneId, ACLType aclType, Boolean subdomainAccess) throws ConcurrentOperationException, InsufficientCapacityException {
        // TODO Auto-generated method stub
        return null;
    }

    @Override
    public boolean associateIpAddressListToAccount(long userId, long accountId, long zoneId, Long vlanId, Network networkToAssociateWith) throws InsufficientCapacityException,
    ConcurrentOperationException, ResourceUnavailableException {
        // TODO Auto-generated method stub
        return false;
    }

    @Override
    public Nic getNicInNetwork(long vmId, long networkId) {
        // TODO Auto-generated method stub
        return null;
    }

    @Override
    public List<? extends Nic> getNicsForTraffic(long vmId, TrafficType type) {
        // TODO Auto-generated method stub
        return null;
    }

    @Override
    public Network getDefaultNetworkForVm(long vmId) {
        // TODO Auto-generated method stub
        return null;
    }

    @Override
    public Nic getDefaultNic(long vmId) {
        // TODO Auto-generated method stub
        return null;
    }

    @Override
    public boolean applyIpAssociations(Network network, boolean continueOnError) throws ResourceUnavailableException {
        // TODO Auto-generated method stub
        return false;
    }

    @Override
    public boolean areServicesSupportedByNetworkOffering(long networkOfferingId, Service... services) {
        // TODO Auto-generated method stub
        return false;
    }

    @Override
    public NetworkVO getNetworkWithSecurityGroupEnabled(Long zoneId) {
        // TODO Auto-generated method stub
        return null;
    }

    @Override
    public boolean startNetwork(long networkId, DeployDestination dest, ReservationContext context) throws ConcurrentOperationException, ResourceUnavailableException, InsufficientCapacityException {
        // TODO Auto-generated method stub
        return false;
    }

    @Override
    public String getIpOfNetworkElementInVirtualNetwork(long accountId, long dataCenterId) {
        // TODO Auto-generated method stub
        return null;
    }

    @Override
    public IPAddressVO markIpAsUnavailable(long addrId) {
        // TODO Auto-generated method stub
        return null;
    }

    @Override
    public String acquireGuestIpAddress(Network network, String requestedIp) {
        // TODO Auto-generated method stub
        return null;
    }

    @Override
    public String getGlobalGuestDomainSuffix() {
        // TODO Auto-generated method stub
        return null;
    }

    @Override
    public String getStartIpAddress(long networkId) {
        // TODO Auto-generated method stub
        return null;
    }

    @Override
    public boolean applyStaticNats(List<? extends StaticNat> staticNats, boolean continueOnError) throws ResourceUnavailableException {
        // TODO Auto-generated method stub
        return false;
    }

    @Override
    public String getIpInNetwork(long vmId, long networkId) {
        // TODO Auto-generated method stub
        return null;
    }

    @Override
    public String getIpInNetworkIncludingRemoved(long vmId, long networkId) {
        // TODO Auto-generated method stub
        return null;
    }

    public Map<Service, Set<Provider>> getNetworkOfferingServiceProvidersMap(long networkOfferingId) {
        return null;
    }

    @Override
    public List<? extends RemoteAccessVPNServiceProvider> getRemoteAccessVpnElements() {
        return null;
    }

    @Override
    public boolean isProviderSupportServiceInNetwork(long networkId, Service service, Provider provider) {
        // TODO Auto-generated method stub
        return false;
    }

    @Override
    public PhysicalNetwork createPhysicalNetwork(Long zoneId, String vnetRange, String networkSpeed, List<String> isolationMethods, String broadcastDomainRange, Long domainId, List<String> tags, String name) {
        // TODO Auto-generated method stub
        return null;
    }

    @Override
    public List<? extends PhysicalNetwork> searchPhysicalNetworks(Long id, Long zoneId, String keyword, Long startIndex, Long pageSize, String name) {
        // TODO Auto-generated method stub
        return null;
    }

    @Override
    public PhysicalNetwork updatePhysicalNetwork(Long id, String networkSpeed, List<String> tags, String newVnetRangeString, String state) {
        // TODO Auto-generated method stub
        return null;
    }

    @Override
    public boolean deletePhysicalNetwork(Long id) {
        // TODO Auto-generated method stub
        return false;
    }

    @Override
    public List<? extends Service> listNetworkServices(String providerName) {
        // TODO Auto-generated method stub
        return null;
    }

    @Override
    public List<? extends Provider> listSupportedNetworkServiceProviders(String serviceName) {
        // TODO Auto-generated method stub
        return null;
    }

    @Override
    public PhysicalNetworkServiceProvider addProviderToPhysicalNetwork(Long physicalNetworkId, String providerName, Long destinationPhysicalNetworkId, List<String> enabledServices) {
        // TODO Auto-generated method stub
        return null;
    }

    @Override
    public List<? extends PhysicalNetworkServiceProvider> listNetworkServiceProviders(Long physicalNetworkId, String name, String state, Long startIndex, Long pageSize) {
        // TODO Auto-generated method stub
        return null;
    }

    @Override
    public boolean deleteNetworkServiceProvider(Long id) {
        // TODO Auto-generated method stub
        return false;
    }

    @Override
    public PhysicalNetwork getPhysicalNetwork(Long physicalNetworkId) {
        // TODO Auto-generated method stub
        return null;
    }

    @Override
    public PhysicalNetwork getCreatedPhysicalNetwork(Long physicalNetworkId) {
        // TODO Auto-generated method stub
        return null;
    }

    @Override
    public PhysicalNetworkServiceProvider getPhysicalNetworkServiceProvider(Long providerId) {
        // TODO Auto-generated method stub
        return null;
    }

    @Override
    public PhysicalNetworkServiceProvider getCreatedPhysicalNetworkServiceProvider(Long providerId) {
        // TODO Auto-generated method stub
        return null;
    }

    @Override
    public long findPhysicalNetworkId(long zoneId, String tag, TrafficType trafficType) {
        // TODO Auto-generated method stub
        return 0;
    }

    @Override
    public PhysicalNetworkTrafficType getPhysicalNetworkTrafficType(Long id) {
        // TODO Auto-generated method stub
        return null;
    }

    @Override
    public PhysicalNetworkTrafficType updatePhysicalNetworkTrafficType(Long id, String xenLabel, String kvmLabel, String vmwareLabel) {
        // TODO Auto-generated method stub
        return null;
    }

    @Override
    public boolean deletePhysicalNetworkTrafficType(Long id) {
        // TODO Auto-generated method stub
        return false;
    }

    @Override
    public List<? extends PhysicalNetworkTrafficType> listTrafficTypes(Long physicalNetworkId) {
        // TODO Auto-generated method stub
        return null;
    }

    @Override
    public PhysicalNetwork getDefaultPhysicalNetworkByZoneAndTrafficType(long zoneId, TrafficType trafficType) {
        // TODO Auto-generated method stub
        return null;
    }

    @Override
    public Network getExclusiveGuestNetwork(long zoneId) {
        // TODO Auto-generated method stub
        return null;
    }

    @Override
    public Long getPodIdForVlan(long vlanDbId) {
        // TODO Auto-generated method stub
        return null;
    }

    @Override
    public boolean networkIsConfiguredForExternalNetworking(long zoneId, long networkId) {
        // TODO Auto-generated method stub
        return false;
    }

    @Override
    public Map<Capability, String> getNetworkServiceCapabilities(long networkId, Service service) {
        // TODO Auto-generated method stub
        return null;
    }

    @Override
    public List<NetworkVO> listNetworksForAccount(long accountId, long zoneId, GuestType type) {
        // TODO Auto-generated method stub
        return null;
    }

    @Override
    public List<Long> listNetworkOfferingsForUpgrade(long networkId) {
        // TODO Auto-generated method stub
        return null;
    }

    @Override
    public PhysicalNetwork translateZoneIdToPhysicalNetwork(long zoneId) {
        // TODO Auto-generated method stub
        return null;
    }

    @Override
    public boolean isSecurityGroupSupportedInNetwork(Network network) {
        // TODO Auto-generated method stub
        return false;
    }

    @Override
    public boolean isProviderEnabledInPhysicalNetwork(long physicalNetowrkId, String providerName) {
        // TODO Auto-generated method stub
        return false;
    }

    @Override
    public List<Service> getElementServices(Provider provider) {
        // TODO Auto-generated method stub
        return null;
    }

    @Override
    public boolean canElementEnableIndividualServices(Provider provider) {
        // TODO Auto-generated method stub
        return false;
    }

    @Override
    public List<? extends UserDataServiceProvider> getPasswordResetElements() {
        // TODO Auto-generated method stub
        return null;
    }

    @Override
    public PhysicalNetworkServiceProvider updateNetworkServiceProvider(Long id, String state, List<String> enabledServices) {
        // TODO Auto-generated method stub
        return null;
    }

    @Override
    public PhysicalNetworkTrafficType addTrafficTypeToPhysicalNetwork(Long physicalNetworkId, String trafficType, String xenLabel, String kvmLabel, String vmwareLabel, String simulatorLabel, String vlan) {
        // TODO Auto-generated method stub
        return null;
    }

    @Override
    public boolean areServicesSupportedInNetwork(long networkId, Service... services) {
        // TODO Auto-generated method stub
        return false;
    }

    @Override
    public boolean isNetworkSystem(Network network) {
        // TODO Auto-generated method stub
        return false;
    }

    @Override
    public PhysicalNetworkServiceProvider addDefaultVirtualRouterToPhysicalNetwork(long physicalNetworkId) {
        // TODO Auto-generated method stub
        return null;
    }

    @Override
    public Map<Capability, String> getNetworkOfferingServiceCapabilities(NetworkOffering offering, Service service) {
        // TODO Auto-generated method stub
        return null;
    }

    @Override
    public boolean reallocate(VirtualMachineProfile<? extends VMInstanceVO> vm, DataCenterDeployment dest) throws InsufficientCapacityException, ConcurrentOperationException {
        // TODO Auto-generated method stub
        return false;
    }

	@Override
	public Long getPhysicalNetworkId(Network network) {
		// TODO Auto-generated method stub
		return null;
	}

	@Override
	public boolean getAllowSubdomainAccessGlobal() {
		// TODO Auto-generated method stub
		return false;
	}

	@Override
	public boolean isProviderForNetwork(Provider provider, long networkId) {
		// TODO Auto-generated method stub
		return false;
	}

	@Override
	public boolean restartNetwork(RestartNetworkCmd cmd, boolean cleanup)
			throws ConcurrentOperationException, ResourceUnavailableException,
			InsufficientCapacityException {
		// TODO Auto-generated method stub
		return false;
	}

	@Override
	public String getNetworkTag(HypervisorType hType, Network network) {
		// TODO Auto-generated method stub
		return null;
	}

	@Override
	public void canProviderSupportServices(
			Map<Provider, Set<Service>> providersMap) {
		// TODO Auto-generated method stub
		
	}

    @Override
    public boolean isProviderForNetworkOffering(Provider provider, long networkOfferingId) {
        // TODO Auto-generated method stub
        return false;
    }

    @Override
    public PhysicalNetworkServiceProvider addDefaultSecurityGroupProviderToPhysicalNetwork(long physicalNetworkId) {
        // TODO Auto-generated method stub
        return null;
    }

    @Override
    public List<PhysicalNetworkSetupInfo> getPhysicalNetworkInfo(long dcId, HypervisorType hypervisorType) {
        // TODO Auto-generated method stub
        return null;
    }

	@Override
	public boolean canAddDefaultSecurityGroup() {
		// TODO Auto-generated method stub
		return false;
	}

    @Override
    public List<Pair<TrafficType, String>> listTrafficTypeImplementor(ListTrafficTypeImplementorsCmd cmd) {
        // TODO Auto-generated method stub
        return null;
    }

    @Override
    public List<Service> listNetworkOfferingServices(long networkOfferingId) {
        // TODO Auto-generated method stub
        return null;
    }

    @Override
    public boolean areServicesEnabledInZone(long zoneId, NetworkOffering offering, List<Service> services) {
        // TODO Auto-generated method stub
        return false;
    }

    @Override
    public Map<PublicIp, Set<Service>> getIpToServices(List<PublicIp> publicIps, boolean rulesRevoked, boolean includingFirewall) {
        // TODO Auto-generated method stub
        return null;
    }

    @Override
    public Map<Provider, ArrayList<PublicIp>> getProviderToIpList(Network network, Map<PublicIp, Set<Service>> ipToServices) {
        // TODO Auto-generated method stub
        return null;
    }

    @Override
    public boolean checkIpForService(IPAddressVO ip, Service service) {
        // TODO Auto-generated method stub
        return false;
    }

    @Override
    public void checkVirtualNetworkCidrOverlap(Long zoneId, String cidr) {
        // TODO Auto-generated method stub
        
    }

	@Override
	public IpAddress allocateIP(long networkId, Account ipOwner,
			boolean isSystem) throws ResourceAllocationException,
			InsufficientAddressCapacityException, ConcurrentOperationException {
		// TODO Auto-generated method stub
		return null;
	}

	@Override
	public PublicIp assignPublicIpAddress(long dcId, Long podId, Account owner,
			VlanType type, Long networkId, String requestedIp, boolean isSystem)
			throws InsufficientAddressCapacityException {
		// TODO Auto-generated method stub
		return null;
	}

	@Override
	public void checkCapabilityForProvider(Set<Provider> providers,
			Service service, Capability cap, String capValue) {
		// TODO Auto-generated method stub
		
	}

	@Override
	public Provider getDefaultUniqueProviderForService(String serviceName) {
		// TODO Auto-generated method stub
		return null;
	}

	@Override
	public IpAddress assignSystemIp(long networkId, Account owner,
			boolean forElasticLb, boolean forElasticIp)
			throws InsufficientAddressCapacityException {
		// TODO Auto-generated method stub
		return null;
	}

	@Override
	public boolean handleSystemIpRelease(IpAddress ip) {
		// TODO Auto-generated method stub
		return false;
	}

	@Override
	public void checkNetworkPermissions(Account owner, Network network) {
		// TODO Auto-generated method stub
		
	}

	@Override
	public void allocateDirectIp(NicProfile nic, DataCenter dc,
			VirtualMachineProfile<? extends VirtualMachine> vm,
			Network network, String requestedIp)
			throws InsufficientVirtualNetworkCapcityException,
			InsufficientAddressCapacityException {
		// TODO Auto-generated method stub
		
	}

	@Override
	public boolean validateRule(FirewallRule rule) {
		// TODO Auto-generated method stub
		return false;
	}

    @Override
    public String getDefaultManagementTrafficLabel(long zoneId, HypervisorType hypervisorType) {
        // TODO Auto-generated method stub
        return null;
    }

    @Override
    public String getDefaultStorageTrafficLabel(long zoneId, HypervisorType hypervisorType) {
        // TODO Auto-generated method stub
        return null;
    }
    @Override
    public Network updateGuestNetwork(long networkId, String name, String displayText, Account callerAccount, User callerUser, String domainSuffix, Long networkOfferingId, Boolean changeCidr) {
        // TODO Auto-generated method stub
        return null;
    }

    @Override
    public boolean shutdownNetwork(long networkId, ReservationContext context, boolean cleanupElements) {
        // TODO Auto-generated method stub
        return false;
    }

    @Override
    public List<? extends Network> getIsolatedNetworksWithSourceNATOwnedByAccountInZone(long zoneId, Account owner) {
        // TODO Auto-generated method stub
        return null;
    }

}
