package com.cloud.network;

import java.util.List;
import java.util.Map;
import java.util.Set;

import javax.ejb.Local;
import javax.naming.ConfigurationException;

import com.cloud.api.commands.AssociateIPAddrCmd;
import com.cloud.api.commands.CreateNetworkCmd;
import com.cloud.api.commands.ListNetworksCmd;
import com.cloud.api.commands.RestartNetworkCmd;
import com.cloud.dc.Vlan;
import com.cloud.dc.Vlan.VlanType;
import com.cloud.deploy.DeployDestination;
import com.cloud.deploy.DeploymentPlan;
import com.cloud.exception.ConcurrentOperationException;
import com.cloud.exception.InsufficientAddressCapacityException;
import com.cloud.exception.InsufficientCapacityException;
import com.cloud.exception.ResourceAllocationException;
import com.cloud.exception.ResourceUnavailableException;
import com.cloud.network.Network.Capability;
import com.cloud.network.Network.GuestIpType;
import com.cloud.network.Network.Provider;
import com.cloud.network.Network.Service;
import com.cloud.network.Networks.TrafficType;
import com.cloud.network.addr.PublicIp;
import com.cloud.network.element.PasswordServiceProvider;
import com.cloud.network.element.RemoteAccessVPNServiceProvider;
import com.cloud.network.guru.NetworkGuru;
import com.cloud.network.rules.FirewallRule;
import com.cloud.network.rules.StaticNat;
import com.cloud.offering.NetworkOffering;
import com.cloud.offerings.NetworkOfferingVO;
import com.cloud.user.Account;
import com.cloud.user.AccountVO;
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
    public List<? extends NetworkOffering> listNetworkOfferings() {
        // TODO Auto-generated method stub
        return null;
    }

    @Override
    public IpAddress allocateIP(AssociateIPAddrCmd cmd) throws ResourceAllocationException, InsufficientAddressCapacityException, ConcurrentOperationException {
        // TODO Auto-generated method stub
        return null;
    }

    @Override
    public IpAddress associateIP(AssociateIPAddrCmd cmd) throws ResourceAllocationException, InsufficientAddressCapacityException, ConcurrentOperationException, ResourceUnavailableException {
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
    public Map<Service, Map<Capability, String>> getZoneCapabilities(long zoneId) {
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
    public List<NetworkVO> setupNetwork(Account owner, NetworkOfferingVO offering, DeploymentPlan plan, String name, String displayText, boolean isDefault, boolean isShared)
            throws ConcurrentOperationException {
        // TODO Auto-generated method stub
        return null;
    }

    @Override
    public List<NetworkVO> setupNetwork(Account owner, NetworkOfferingVO offering, Network predefined, DeploymentPlan plan, String name, String displayText, boolean isDefault, boolean errorIfAlreadySetup,
            Long domainId, List<String> tags, boolean isShared) throws ConcurrentOperationException {
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
    public List<? extends Nic> getNicsIncludingRemoved(VirtualMachine vm) {
        // TODO Auto-generated method stub
        return null;
    }

    @Override
    public List<NicProfile> getNicProfiles(VirtualMachine vm) {
        // TODO Auto-generated method stub
        return null;
    }

    @Override
    public List<AccountVO> getAccountsUsingNetwork(long configurationId) {
        // TODO Auto-generated method stub
        return null;
    }

    @Override
    public AccountVO getNetworkOwner(long configurationId) {
        // TODO Auto-generated method stub
        return null;
    }

    @Override
    public List<NetworkVO> getNetworksforOffering(long offeringId, long dataCenterId, long accountId) {
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
    public void shutdownNetwork(long networkId, ReservationContext context) {
        // TODO Auto-generated method stub

    }

    @Override
    public boolean destroyNetwork(long networkId, ReservationContext context) {
        // TODO Auto-generated method stub
        return false;
    }

    @Override
    public Network createNetwork(long networkOfferingId, String name, String displayText, Boolean isDefault, String gateway, String cidr, String vlanId, String networkDomain, Account owner,
            boolean isSecurityGroupEnabled, Long domainId, List<String> tags, Boolean isShared, PhysicalNetwork physicalNetwork) throws ConcurrentOperationException, InsufficientCapacityException {
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
    public Nic getNicInNetworkIncludingRemoved(long vmId, long networkId) {
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
    public boolean zoneIsConfiguredForExternalNetworking(long zoneId) {
        // TODO Auto-generated method stub
        return false;
    }

    @Override
    public Map<Capability, String> getServiceCapabilities(long zoneId, Long networkOfferingId, Service service) {
        // TODO Auto-generated method stub
        return null;
    }

    @Override
    public boolean applyIpAssociations(Network network, boolean continueOnError) throws ResourceUnavailableException {
        // TODO Auto-generated method stub
        return false;
    }

    @Override
    public boolean isServiceSupportedByNetworkOffering(long networkOfferingId, Service service) {
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
    public List<NetworkVO> listNetworksForAccount(long accountId, long zoneId, GuestIpType guestType, Boolean isDefault) {
        // TODO Auto-generated method stub
        return null;
    }

    @Override
    public IPAddressVO markIpAsUnavailable(long addrId) {
        // TODO Auto-generated method stub
        return null;
    }

    @Override
    public PublicIp assignPublicIpAddress(long dcId, Long podId, Account owner, VlanType type, Long networkId, String requestedIp) throws InsufficientAddressCapacityException {
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

    @Override
    public Network updateNetwork(long networkId, String name, String displayText, List<String> tags, Account caller, String domainSuffix, Long networkOfferingId) {
        // TODO Auto-generated method stub
        return null;
    }
    
    @Override
    public Map<String, Set<String>> listNetworkOfferingServices(long networkOfferingId) {
        return null;
    }
    
    @Override
    public boolean restartNetwork(RestartNetworkCmd cmd, boolean cleanup) throws ConcurrentOperationException, ResourceUnavailableException, InsufficientCapacityException {
        return false;
    }
    
    @Override
    public List<? extends RemoteAccessVPNServiceProvider> getRemoteAccessVpnElements() {
        return null;
    }

    @Override
    public List<? extends PasswordServiceProvider> getPasswordResetElements() {
        // TODO Auto-generated method stub
        return null;
    }

    @Override
    public Long getPodIdForVlan(long vlanDbId) {
        // TODO Auto-generated method stub
        return null;
    }

    @Override
    public boolean isProviderSupported(long networkOfferingId, Service service, Provider provider) {
        // TODO Auto-generated method stub
        return false;
    }
}
