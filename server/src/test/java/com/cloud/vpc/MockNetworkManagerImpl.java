// Licensed to the Apache Software Foundation (ASF) under one
// or more contributor license agreements.  See the NOTICE file
// distributed with this work for additional information
// regarding copyright ownership.  The ASF licenses this file
// to you under the Apache License, Version 2.0 (the
// "License"); you may not use this file except in compliance
// with the License.  You may obtain a copy of the License at
//
//   http://www.apache.org/licenses/LICENSE-2.0
//
// Unless required by applicable law or agreed to in writing,
// software distributed under the License is distributed on an
// "AS IS" BASIS, WITHOUT WARRANTIES OR CONDITIONS OF ANY
// KIND, either express or implied.  See the License for the
// specific language governing permissions and limitations
// under the License.
package com.cloud.vpc;

import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

import javax.inject.Inject;
import javax.naming.ConfigurationException;

import org.apache.cloudstack.acl.ControlledEntity.ACLType;
import org.apache.cloudstack.api.command.admin.address.ReleasePodIpCmdByAdmin;
import org.apache.cloudstack.api.command.admin.network.DedicateGuestVlanRangeCmd;
import org.apache.cloudstack.api.command.admin.network.ListDedicatedGuestVlanRangesCmd;
import org.apache.cloudstack.api.command.admin.usage.ListTrafficTypeImplementorsCmd;
import org.apache.cloudstack.api.command.user.network.CreateNetworkCmd;
import org.apache.cloudstack.api.command.user.network.ListNetworksCmd;
import org.apache.cloudstack.api.command.user.network.RestartNetworkCmd;
import org.apache.cloudstack.api.command.user.network.UpdateNetworkCmd;
import org.apache.cloudstack.api.command.user.vm.ListNicsCmd;
import org.apache.cloudstack.api.response.AcquirePodIpCmdResponse;
import org.apache.cloudstack.engine.orchestration.service.NetworkOrchestrationService;
import org.apache.log4j.Logger;
import org.springframework.stereotype.Component;

import com.cloud.deploy.DataCenterDeployment;
import com.cloud.deploy.DeployDestination;
import com.cloud.deploy.DeploymentPlan;
import com.cloud.exception.ConcurrentOperationException;
import com.cloud.exception.InsufficientAddressCapacityException;
import com.cloud.exception.InsufficientCapacityException;
import com.cloud.exception.InsufficientVirtualNetworkCapacityException;
import com.cloud.exception.ResourceAllocationException;
import com.cloud.exception.ResourceUnavailableException;
import com.cloud.network.GuestVlan;
import com.cloud.network.IpAddress;
import com.cloud.network.Network;
import com.cloud.network.Network.IpAddresses;
import com.cloud.network.Network.Provider;
import com.cloud.network.Network.Service;
import com.cloud.network.NetworkProfile;
import com.cloud.network.NetworkService;
import com.cloud.network.Networks.TrafficType;
import com.cloud.network.PhysicalNetwork;
import com.cloud.network.PhysicalNetworkServiceProvider;
import com.cloud.network.PhysicalNetworkTrafficType;
import com.cloud.network.dao.NetworkServiceMapDao;
import com.cloud.network.dao.NetworkVO;
import com.cloud.network.element.DhcpServiceProvider;
import com.cloud.network.element.DnsServiceProvider;
import com.cloud.network.element.LoadBalancingServiceProvider;
import com.cloud.network.element.NetworkElement;
import com.cloud.network.element.StaticNatServiceProvider;
import com.cloud.network.element.UserDataServiceProvider;
import com.cloud.network.guru.NetworkGuru;
import com.cloud.network.router.VirtualRouter;
import com.cloud.network.rules.LoadBalancerContainer.Scheme;
import com.cloud.network.vpc.Vpc;
import com.cloud.offering.NetworkOffering;
import com.cloud.offerings.dao.NetworkOfferingServiceMapDao;
import com.cloud.user.Account;
import com.cloud.user.User;
import com.cloud.utils.Pair;
import com.cloud.utils.component.ManagerBase;
import com.cloud.utils.exception.CloudRuntimeException;
import com.cloud.vm.Nic;
import com.cloud.vm.NicProfile;
import com.cloud.vm.NicSecondaryIp;
import com.cloud.vm.NicVO;
import com.cloud.vm.ReservationContext;
import com.cloud.vm.VirtualMachine;
import com.cloud.vm.VirtualMachine.Type;
import com.cloud.vm.VirtualMachineProfile;

@Component
public class MockNetworkManagerImpl extends ManagerBase implements NetworkOrchestrationService, NetworkService {
    @Inject
    NetworkServiceMapDao _ntwkSrvcDao;
    @Inject
    NetworkOfferingServiceMapDao _ntwkOfferingSrvcDao;

    @Inject
    List<NetworkElement> _networkElements;

    private static HashMap<String, String> s_providerToNetworkElementMap = new HashMap<String, String>();
    private static final Logger s_logger = Logger.getLogger(MockNetworkManagerImpl.class);

    /* (non-Javadoc)
     * @see com.cloud.utils.component.Manager#start()
     */
    @Override
    public boolean start() {
        for (NetworkElement element : _networkElements) {
            Provider implementedProvider = element.getProvider();
            if (implementedProvider != null) {
                if (s_providerToNetworkElementMap.containsKey(implementedProvider.getName())) {
                    s_logger.error("Cannot start MapNetworkManager: Provider <-> NetworkElement must be a one-to-one map, " +
                        "multiple NetworkElements found for Provider: " + implementedProvider.getName());
                    return false;
                }
                s_providerToNetworkElementMap.put(implementedProvider.getName(), element.getName());
            }
        }
        return true;
    }

    /* (non-Javadoc)
     * @see com.cloud.utils.component.Manager#configure(java.lang.String, java.util.Map)
     */
    @Override
    public boolean configure(String name, Map<String, Object> params) throws ConfigurationException {
        // TODO Auto-generated method stub
        return false;
    }

    /* (non-Javadoc)
     * @see com.cloud.utils.component.Manager#stop()
     */
    @Override
    public boolean stop() {
        // TODO Auto-generated method stub
        return false;
    }

    /* (non-Javadoc)
     * @see com.cloud.utils.component.Manager#getName()
     */
    @Override
    public String getName() {
        // TODO Auto-generated method stub
        return null;
    }

    /* (non-Javadoc)
     * @see com.cloud.network.NetworkService#getIsolatedNetworksOwnedByAccountInZone(long, com.cloud.user.Account)
     */
    @Override
    public List<? extends Network> getIsolatedNetworksOwnedByAccountInZone(long zoneId, Account owner) {
        // TODO Auto-generated method stub
        return null;
    }

    /* (non-Javadoc)
     * @see com.cloud.network.NetworkService#allocateIP(com.cloud.user.Account, long, java.lang.Long)
     */
    @Override
    public IpAddress allocateIP(Account ipOwner, long zoneId, Long networkId, Boolean displayIp, String ipaddress) throws ResourceAllocationException, InsufficientAddressCapacityException,
        ConcurrentOperationException {
        // TODO Auto-generated method stub
        return null;
    }

    @Override
    public IpAddress allocatePortableIP(Account ipOwner, int regionId, Long zoneId, Long networkId, Long vpcId) throws ResourceAllocationException,
        InsufficientAddressCapacityException, ConcurrentOperationException {
        return null;
    }

    @Override
    public boolean releasePortableIpAddress(long ipAddressId) {
        return false;// TODO Auto-generated method stub
    }

    /* (non-Javadoc)
    * @see com.cloud.network.NetworkService#releaseIpAddress(long)
    */
    @Override
    public boolean releaseIpAddress(long ipAddressId) throws InsufficientAddressCapacityException {
        // TODO Auto-generated method stub
        return false;
    }

    /* (non-Javadoc)
     * @see com.cloud.network.NetworkService#createGuestNetwork(com.cloud.api.commands.CreateNetworkCmd)
     */
    @Override
    public Network createGuestNetwork(CreateNetworkCmd cmd) throws InsufficientCapacityException, ConcurrentOperationException, ResourceAllocationException {
        // TODO Auto-generated method stub
        return null;
    }

    /* (non-Javadoc)
     * @see com.cloud.network.NetworkService#searchForNetworks(com.cloud.api.commands.ListNetworksCmd)
     */
    @Override
    public Pair<List<? extends Network>, Integer> searchForNetworks(ListNetworksCmd cmd) {
        // TODO Auto-generated method stub
        return null;
    }

    /* (non-Javadoc)
     * @see com.cloud.network.NetworkService#deleteNetwork(long)
     */
    @Override
    public boolean deleteNetwork(long networkId, boolean forced) {
        // TODO Auto-generated method stub
        return false;
    }

    /* (non-Javadoc)
     * @see com.cloud.network.NetworkService#restartNetwork(com.cloud.api.commands.RestartNetworkCmd, boolean)
     */
    @Override
    public boolean restartNetwork(RestartNetworkCmd cmd, boolean cleanup, boolean makeRedundant) throws ConcurrentOperationException, ResourceUnavailableException,
        InsufficientCapacityException {
        // TODO Auto-generated method stub
        return false;
    }

    /* (non-Javadoc)
     * @see com.cloud.network.NetworkService#getActiveNicsInNetwork(long)
     */
    @Override
    public int getActiveNicsInNetwork(long networkId) {
        // TODO Auto-generated method stub
        return 0;
    }

    /* (non-Javadoc)
     * @see com.cloud.network.NetworkService#getNetwork(long)
     */
    @Override
    public Network getNetwork(long networkId) {
        // TODO Auto-generated method stub
        return null;
    }

    /* (non-Javadoc)
     * @see com.cloud.network.NetworkService#getIp(long)
     */
    @Override
    public IpAddress getIp(long id) {
        // TODO Auto-generated method stub
        return null;
    }

    @Override
    public Network updateGuestNetwork(final UpdateNetworkCmd cmd) {
        // TODO Auto-generated method stub
        return null;
    }

    @Override
    public Network migrateGuestNetwork(long networkId, long networkOfferingId, Account callerAccount, User callerUser, boolean resume) {
        return null;
    }

    @Override public Vpc migrateVpcNetwork(long vpcId, long vpcNetworkofferingId, Map<String, String> networkToOffering, Account account, User callerUser, boolean resume) {
        return null;
    }

    /* (non-Javadoc)
             * @see com.cloud.network.NetworkService#createPhysicalNetwork(java.lang.Long, java.lang.String, java.lang.String, java.util.List, java.lang.String, java.lang.Long, java.util.List, java.lang.String)
             */
    @Override
    public PhysicalNetwork createPhysicalNetwork(Long zoneId, String vnetRange, String networkSpeed, List<String> isolationMethods, String broadcastDomainRange,
        Long domainId, List<String> tags, String name) {
        // TODO Auto-generated method stub
        return null;
    }

    /* (non-Javadoc)
     * @see com.cloud.network.NetworkService#searchPhysicalNetworks(java.lang.Long, java.lang.Long, java.lang.String, java.lang.Long, java.lang.Long, java.lang.String)
     */
    @Override
    public Pair<List<? extends PhysicalNetwork>, Integer> searchPhysicalNetworks(Long id, Long zoneId, String keyword, Long startIndex, Long pageSize, String name) {
        // TODO Auto-generated method stub
        return null;
    }

    /* (non-Javadoc)
     * @see com.cloud.network.NetworkService#updatePhysicalNetwork(java.lang.Long, java.lang.String, java.util.List, java.lang.String, java.lang.String)
     */
    @Override
    public PhysicalNetwork updatePhysicalNetwork(Long id, String networkSpeed, List<String> tags, String newVnetRangeString, String state) {
        // TODO Auto-generated method stub
        return null;
    }

    /* (non-Javadoc)
     * @see com.cloud.network.NetworkService#deletePhysicalNetwork(java.lang.Long)
     */
    @Override
    public boolean deletePhysicalNetwork(Long id) {
        // TODO Auto-generated method stub
        return false;
    }

    @Override
    public GuestVlan dedicateGuestVlanRange(DedicateGuestVlanRangeCmd cmd) {
        // TODO Auto-generated method stub
        return null;
    }

    @Override
    public Pair<List<? extends GuestVlan>, Integer> listDedicatedGuestVlanRanges(ListDedicatedGuestVlanRangesCmd cmd) {
        // TODO Auto-generated method stub
        return null;
    }

    @Override
    public boolean releaseDedicatedGuestVlanRange(Long dedicatedGuestVlanRangeId) {
        // TODO Auto-generated method stub
        return true;

    }

    /* (non-Javadoc)
     * @see com.cloud.network.NetworkService#listNetworkServices(java.lang.String)
     */
    @Override
    public List<? extends Service> listNetworkServices(String providerName) {
        // TODO Auto-generated method stub
        return null;
    }

    /* (non-Javadoc)
     * @see com.cloud.network.NetworkService#addProviderToPhysicalNetwork(java.lang.Long, java.lang.String, java.lang.Long, java.util.List)
     */
    @Override
    public PhysicalNetworkServiceProvider addProviderToPhysicalNetwork(Long physicalNetworkId, String providerName, Long destinationPhysicalNetworkId,
        List<String> enabledServices) {
        // TODO Auto-generated method stub
        return null;
    }

    /* (non-Javadoc)
     * @see com.cloud.network.NetworkService#listNetworkServiceProviders(java.lang.Long, java.lang.String, java.lang.String, java.lang.Long, java.lang.Long)
     */
    @Override
    public Pair<List<? extends PhysicalNetworkServiceProvider>, Integer> listNetworkServiceProviders(Long physicalNetworkId, String name, String state, Long startIndex,
        Long pageSize) {
        // TODO Auto-generated method stub
        return null;
    }

    /* (non-Javadoc)
     * @see com.cloud.network.NetworkService#updateNetworkServiceProvider(java.lang.Long, java.lang.String, java.util.List)
     */
    @Override
    public PhysicalNetworkServiceProvider updateNetworkServiceProvider(Long id, String state, List<String> enabledServices) {
        // TODO Auto-generated method stub
        return null;
    }

    /* (non-Javadoc)
     * @see com.cloud.network.NetworkService#deleteNetworkServiceProvider(java.lang.Long)
     */
    @Override
    public boolean deleteNetworkServiceProvider(Long id) throws ConcurrentOperationException, ResourceUnavailableException {
        // TODO Auto-generated method stub
        return false;
    }

    /* (non-Javadoc)
     * @see com.cloud.network.NetworkService#getPhysicalNetwork(java.lang.Long)
     */
    @Override
    public PhysicalNetwork getPhysicalNetwork(Long physicalNetworkId) {
        // TODO Auto-generated method stub
        return null;
    }

    /* (non-Javadoc)
     * @see com.cloud.network.NetworkService#getCreatedPhysicalNetwork(java.lang.Long)
     */
    @Override
    public PhysicalNetwork getCreatedPhysicalNetwork(Long physicalNetworkId) {
        // TODO Auto-generated method stub
        return null;
    }

    /* (non-Javadoc)
     * @see com.cloud.network.NetworkService#getPhysicalNetworkServiceProvider(java.lang.Long)
     */
    @Override
    public PhysicalNetworkServiceProvider getPhysicalNetworkServiceProvider(Long providerId) {
        // TODO Auto-generated method stub
        return null;
    }

    /* (non-Javadoc)
     * @see com.cloud.network.NetworkService#getCreatedPhysicalNetworkServiceProvider(java.lang.Long)
     */
    @Override
    public PhysicalNetworkServiceProvider getCreatedPhysicalNetworkServiceProvider(Long providerId) {
        // TODO Auto-generated method stub
        return null;
    }

    /* (non-Javadoc)
     * @see com.cloud.network.NetworkService#findPhysicalNetworkId(long, java.lang.String, com.cloud.network.Networks.TrafficType)
     */
    @Override
    public long findPhysicalNetworkId(long zoneId, String tag, TrafficType trafficType) {
        // TODO Auto-generated method stub
        return 0;
    }

    /* (non-Javadoc)
     * @see com.cloud.network.NetworkService#addTrafficTypeToPhysicalNetwork(java.lang.Long, java.lang.String, java.lang.String, java.lang.String, java.lang.String, java.lang.String, java.lang.String, java.lang.String)
     */
    @Override
    public PhysicalNetworkTrafficType addTrafficTypeToPhysicalNetwork(Long physicalNetworkId, String trafficType, String isolationMethod, String xenLabel, String kvmLabel, String vmwareLabel,
        String simulatorLabel, String vlan, String hypervLabel, String ovm3Label) {
        // TODO Auto-generated method stub
        return null;
    }

    /* (non-Javadoc)
     * @see com.cloud.network.NetworkService#getPhysicalNetworkTrafficType(java.lang.Long)
     */
    @Override
    public PhysicalNetworkTrafficType getPhysicalNetworkTrafficType(Long id) {
        // TODO Auto-generated method stub
        return null;
    }

    /* (non-Javadoc)
     * @see com.cloud.network.NetworkService#updatePhysicalNetworkTrafficType(java.lang.Long, java.lang.String, java.lang.String, java.lang.String)
     */
    @Override
    public PhysicalNetworkTrafficType updatePhysicalNetworkTrafficType(Long id, String xenLabel, String kvmLabel, String vmwareLabel, String hypervLabel, String ovm3Label) {
        // TODO Auto-generated method stub
        return null;
    }

    /* (non-Javadoc)
     * @see com.cloud.network.NetworkService#deletePhysicalNetworkTrafficType(java.lang.Long)
     */
    @Override
    public boolean deletePhysicalNetworkTrafficType(Long id) {
        // TODO Auto-generated method stub
        return false;
    }

    /* (non-Javadoc)
     * @see com.cloud.network.NetworkService#listTrafficTypes(java.lang.Long)
     */
    @Override
    public Pair<List<? extends PhysicalNetworkTrafficType>, Integer> listTrafficTypes(Long physicalNetworkId) {
        // TODO Auto-generated method stub
        return null;
    }

    /* (non-Javadoc)
     * @see com.cloud.network.NetworkService#getExclusiveGuestNetwork(long)
     */
    @Override
    public Network getExclusiveGuestNetwork(long zoneId) {
        // TODO Auto-generated method stub
        return null;
    }

    /* (non-Javadoc)
     * @see com.cloud.network.NetworkService#listTrafficTypeImplementor(org.apache.cloudstack.api.commands.ListTrafficTypeImplementorsCmd)
     */
    @Override
    public List<Pair<TrafficType, String>> listTrafficTypeImplementor(ListTrafficTypeImplementorsCmd cmd) {
        // TODO Auto-generated method stub
        return null;
    }

    /* (non-Javadoc)
     * @see com.cloud.network.NetworkService#getIsolatedNetworksWithSourceNATOwnedByAccountInZone(long, com.cloud.user.Account)
     */
    @Override
    public List<? extends Network> getIsolatedNetworksWithSourceNATOwnedByAccountInZone(long zoneId, Account owner) {
        // TODO Auto-generated method stub
        return null;
    }

    /* (non-Javadoc)
     * @see com.cloud.network.NetworkService#associateIPToNetwork(long, long)
     */
    @Override
    public IpAddress associateIPToNetwork(long ipId, long networkId) throws InsufficientAddressCapacityException, ResourceAllocationException,
        ResourceUnavailableException, ConcurrentOperationException {
        // TODO Auto-generated method stub
        return null;
    }

    /* (non-Javadoc)
     * @see com.cloud.network.NetworkService#createPrivateNetwork(java.lang.String, java.lang.String, long, java.lang.String, java.lang.String, java.lang.String, java.lang.String, java.lang.String, long, java.lang.Long)
     */
    @Override
    public Network createPrivateNetwork(String networkName, String displayText, long physicalNetworkId, String vlan, String startIp, String endIP, String gateway,
        String netmask, long networkOwnerId, Long vpcId, Boolean sourceNat, Long networkOfferingId) throws ResourceAllocationException, ConcurrentOperationException,
        InsufficientCapacityException {
        // TODO Auto-generated method stub
        return null;
    }

    /* (non-Javadoc)
     * @see com.cloud.network.NetworkManager#setupNetwork(com.cloud.user.Account, com.cloud.offerings.NetworkOfferingVO, com.cloud.deploy.DeploymentPlan, java.lang.String, java.lang.String, boolean)
     */
    @Override
    public List<NetworkVO> setupNetwork(Account owner, NetworkOffering offering, DeploymentPlan plan, String name, String displayText, boolean isDefault)
        throws ConcurrentOperationException {
        // TODO Auto-generated method stub
        return null;
    }

    /* (non-Javadoc)
     * @see com.cloud.network.NetworkManager#setupNetwork(com.cloud.user.Account, com.cloud.offerings.NetworkOfferingVO, com.cloud.network.Network, com.cloud.deploy.DeploymentPlan, java.lang.String, java.lang.String, boolean, java.lang.Long, org.apache.cloudstack.acl.ControlledEntity.ACLType, java.lang.Boolean, java.lang.Long)
     */
    @Override
    public List<NetworkVO> setupNetwork(Account owner, NetworkOffering offering, Network predefined, DeploymentPlan plan, String name, String displayText,
        boolean errorIfAlreadySetup, Long domainId, ACLType aclType, Boolean subdomainAccess, Long vpcId, Boolean isNetworkDisplayEnabled)
        throws ConcurrentOperationException {
        // TODO Auto-generated method stub
        return null;
    }

    /* (non-Javadoc)
     * @see com.cloud.network.NetworkManager#allocate(com.cloud.vm.VirtualMachineProfile, java.util.List)
     */
    @Override
    public void allocate(VirtualMachineProfile vm, LinkedHashMap<? extends Network, List<? extends NicProfile>> networks, final Map<String, Map<Integer, String>> extraDhcpOptions)
            throws InsufficientCapacityException, ConcurrentOperationException {
        // TODO Auto-generated method stub
    }

    @Override
    public void configureExtraDhcpOptions(Network network, long nicId, Map<Integer, String> extraDhcpOptions) {

    }

    @Override public void configureExtraDhcpOptions(Network network, long nicId) {

    }

    /* (non-Javadoc)
     * @see com.cloud.network.NetworkManager#prepare(com.cloud.vm.VirtualMachineProfile, com.cloud.deploy.DeployDestination, com.cloud.vm.ReservationContext)
     */
    @Override
    public void prepare(VirtualMachineProfile profile, DeployDestination dest, ReservationContext context) throws InsufficientCapacityException,
        ConcurrentOperationException, ResourceUnavailableException {
        // TODO Auto-generated method stub

    }

    /* (non-Javadoc)
     * @see com.cloud.network.NetworkManager#release(com.cloud.vm.VirtualMachineProfile, boolean)
     */
    @Override
    public void release(VirtualMachineProfile vmProfile, boolean forced) throws ConcurrentOperationException, ResourceUnavailableException {
        // TODO Auto-generated method stub

    }

    /* (non-Javadoc)
     * @see com.cloud.network.NetworkManager#cleanupNics(com.cloud.vm.VirtualMachineProfile)
     */
    @Override
    public void cleanupNics(VirtualMachineProfile vm) {
        // TODO Auto-generated method stub

    }

    /* (non-Javadoc)
     * @see com.cloud.network.NetworkManager#expungeNics(com.cloud.vm.VirtualMachineProfile)
     */
    @Override
    public void expungeNics(VirtualMachineProfile vm) {
        // TODO Auto-generated method stub

    }

    /* (non-Javadoc)
     * @see com.cloud.network.NetworkManager#getNicProfiles(com.cloud.vm.VirtualMachine)
     */
    @Override
    public List<NicProfile> getNicProfiles(VirtualMachine vm) {
        // TODO Auto-generated method stub
        return null;
    }

    @Override
    public Map<String, String> getSystemVMAccessDetails(VirtualMachine vm) {
        return null;
    }

    /* (non-Javadoc)
     * @see com.cloud.network.NetworkManager#implementNetwork(long, com.cloud.deploy.DeployDestination, com.cloud.vm.ReservationContext)
     */
    @Override
    public Pair<NetworkGuru, NetworkVO> implementNetwork(long networkId, DeployDestination dest, ReservationContext context) throws ConcurrentOperationException,
        ResourceUnavailableException, InsufficientCapacityException {
        // TODO Auto-generated method stub
        return null;
    }

    @Override
    public Map<Integer, String> getExtraDhcpOptions(long nicId) {
        return null;
    }

    /* (non-Javadoc)
     * @see com.cloud.network.NetworkManager#shutdownNetwork(long, com.cloud.vm.ReservationContext, boolean)
     */
    @Override
    public boolean shutdownNetwork(long networkId, ReservationContext context, boolean cleanupElements) {
        // TODO Auto-generated method stub
        return false;
    }

    /* (non-Javadoc)
     * @see com.cloud.network.NetworkManager#destroyNetwork(long, com.cloud.vm.ReservationContext)
     */
    @Override
    public boolean destroyNetwork(long networkId, ReservationContext context, boolean forced) {
        // TODO Auto-generated method stub
        return false;
    }

    /* (non-Javadoc)
     * @see com.cloud.network.NetworkManager#createGuestNetwork(long, java.lang.String, java.lang.String, java.lang.String, java.lang.String, java.lang.String, java.lang.String, com.cloud.user.Account, java.lang.Long, com.cloud.network.PhysicalNetwork, long, org.apache.cloudstack.acl.ControlledEntity.ACLType, java.lang.Boolean, java.lang.Long)
     */
    @Override
    public Network createGuestNetwork(long networkOfferingId, String name, String displayText, String gateway, String cidr, String vlanId, boolean bypassVlanOverlapCheck, String networkDomain,
                                      Account owner, Long domainId, PhysicalNetwork physicalNetwork, long zoneId, ACLType aclType, Boolean subdomainAccess, Long vpcId, String gatewayv6,
                                      String cidrv6, Boolean displayNetworkEnabled, String isolatedPvlan, String externalId) throws ConcurrentOperationException, InsufficientCapacityException,
        ResourceAllocationException {
        // TODO Auto-generated method stub
        return null;
    }

    /* (non-Javadoc)
     * @see com.cloud.network.NetworkManager#getPasswordResetProvider(com.cloud.network.Network)
     */
    @Override
    public UserDataServiceProvider getPasswordResetProvider(Network network) {
        // TODO Auto-generated method stub
        return null;
    }

    @Override
    public UserDataServiceProvider getSSHKeyResetProvider(Network network) {
        // TODO Auto-generated method stub
        return null;
    }

    /* (non-Javadoc)
     * @see com.cloud.network.NetworkManager#startNetwork(long, com.cloud.deploy.DeployDestination, com.cloud.vm.ReservationContext)
     */
    @Override
    public boolean startNetwork(long networkId, DeployDestination dest, ReservationContext context) throws ConcurrentOperationException, ResourceUnavailableException,
        InsufficientCapacityException {
        // TODO Auto-generated method stub
        return false;
    }

    /* (non-Javadoc)
     * @see com.cloud.network.NetworkManager#reallocate(com.cloud.vm.VirtualMachineProfile, com.cloud.deploy.DataCenterDeployment)
     */
    @Override
    public boolean reallocate(VirtualMachineProfile vm, DataCenterDeployment dest) throws InsufficientCapacityException, ConcurrentOperationException {
        // TODO Auto-generated method stub
        return false;
    }

    @Override
    public void saveExtraDhcpOptions(String networkUuid, Long nicId, Map<String, Map<Integer, String>> extraDhcpOptionMap) {

    }

    /* (non-Javadoc)
     * @see com.cloud.network.NetworkManager#allocateNic(com.cloud.vm.NicProfile, com.cloud.network.Network, java.lang.Boolean, int, com.cloud.vm.VirtualMachineProfile)
     */
    @Override
    public Pair<NicProfile, Integer> allocateNic(NicProfile requested, Network network, Boolean isDefaultNic, int deviceId, VirtualMachineProfile vm)
        throws InsufficientVirtualNetworkCapacityException, InsufficientAddressCapacityException, ConcurrentOperationException {
        // TODO Auto-generated method stub
        return null;
    }

    @Override
    public NicProfile prepareNic(VirtualMachineProfile vmProfile, DeployDestination dest, ReservationContext context, long nicId, Network network)
        throws InsufficientVirtualNetworkCapacityException, InsufficientAddressCapacityException, ConcurrentOperationException, InsufficientCapacityException,
        ResourceUnavailableException {
        // TODO Auto-generated method stub
        return null;
    }

    /* (non-Javadoc)
     * @see com.cloud.network.NetworkManager#removeNic(com.cloud.vm.VirtualMachineProfile, com.cloud.vm.Nic)
     */
    @Override
    public void removeNic(VirtualMachineProfile vm, Nic nic) {
        // TODO Auto-generated method stub

    }

    /* (non-Javadoc)
    * @see com.cloud.network.NetworkManager#setupDns(com.cloud.network.Network, com.cloud.network.Network.Provider)
    */
    @Override
    public boolean setupDns(Network network, Provider provider) {
        // TODO Auto-generated method stub
        return false;
    }

    /* (non-Javadoc)
     * @see com.cloud.network.NetworkManager#releaseNic(com.cloud.vm.VirtualMachineProfile, com.cloud.vm.Nic)
     */
    @Override
    public void releaseNic(VirtualMachineProfile vmProfile, Nic nic) throws ConcurrentOperationException, ResourceUnavailableException {
        // TODO Auto-generated method stub

    }

    /* (non-Javadoc)
     * @see com.cloud.network.NetworkManager#createNicForVm(com.cloud.network.Network, com.cloud.vm.NicProfile, com.cloud.vm.ReservationContext, com.cloud.vm.VirtualMachineProfileImpl, boolean, boolean)
     */
    @Override
    public NicProfile createNicForVm(Network network, NicProfile requested, ReservationContext context, VirtualMachineProfile vmProfile, boolean prepare)
        throws InsufficientVirtualNetworkCapacityException, InsufficientAddressCapacityException, ConcurrentOperationException, InsufficientCapacityException,
        ResourceUnavailableException {
        // TODO Auto-generated method stub
        return null;
    }

    /* (non-Javadoc)
     * @see com.cloud.network.NetworkManager#convertNetworkToNetworkProfile(long)
     */
    @Override
    public NetworkProfile convertNetworkToNetworkProfile(long networkId) {
        // TODO Auto-generated method stub
        return null;
    }

    /* (non-Javadoc)
     * @see com.cloud.network.NetworkManager#restartNetwork(java.lang.Long, com.cloud.user.Account, com.cloud.user.User, boolean)
     */
    @Override
    public boolean restartNetwork(Long networkId, Account callerAccount, User callerUser, boolean cleanup) throws ConcurrentOperationException,
        ResourceUnavailableException, InsufficientCapacityException {
        // TODO Auto-generated method stub
        return false;
    }

    /* (non-Javadoc)
     * @see com.cloud.network.NetworkManager#shutdownNetworkElementsAndResources(com.cloud.vm.ReservationContext, boolean, com.cloud.network.NetworkVO)
     */
    @Override
    public boolean shutdownNetworkElementsAndResources(ReservationContext context, boolean b, Network network) {
        // TODO Auto-generated method stub
        return false;
    }

    /* (non-Javadoc)
     * @see com.cloud.network.NetworkManager#implementNetworkElementsAndResources(com.cloud.deploy.DeployDestination, com.cloud.vm.ReservationContext, com.cloud.network.NetworkVO, com.cloud.offerings.NetworkOfferingVO)
     */
    @Override
    public void implementNetworkElementsAndResources(DeployDestination dest, ReservationContext context, Network network, NetworkOffering findById)
        throws ConcurrentOperationException, InsufficientAddressCapacityException, ResourceUnavailableException, InsufficientCapacityException {
        // TODO Auto-generated method stub

    }

    /* (non-Javadoc)
     * @see com.cloud.network.NetworkManager#finalizeServicesAndProvidersForNetwork(com.cloud.offering.NetworkOffering, java.lang.Long)
     */
    @Override
    public Map<String, String> finalizeServicesAndProvidersForNetwork(NetworkOffering offering, Long physicalNetworkId) {
        // TODO Auto-generated method stub
        return null;
    }

    @Override
    public boolean isNetworkInlineMode(Network network) {
        // TODO Auto-generated method stub
        return false;
    }

    @Override
    public List<Provider> getProvidersForServiceInNetwork(Network network, Service service) {
        // TODO Auto-generated method stub
        return null;
    }

    @Override
    public StaticNatServiceProvider getStaticNatProviderForNetwork(Network network) {
        // TODO Auto-generated method stub
        return null;
    }

    @Override
    public LoadBalancingServiceProvider getLoadBalancingProviderForNetwork(Network network, Scheme lbScheme) {
        // TODO Auto-generated method stub
        return null;
    }

    /* (non-Javadoc)
     * @see com.cloud.network.NetworkService#getNetwork(java.lang.String)
     */
    @Override
    public Network getNetwork(String networkUuid) {
        // TODO Auto-generated method stub
        return null;
    }

    @Override
    public boolean isSecondaryIpSetForNic(long nicId) {
        // TODO Auto-generated method stub
        return false;
    }

    @Override
    public NicSecondaryIp allocateSecondaryGuestIP(long nicId, IpAddresses requestedIpPair) {
        // TODO Auto-generated method stub
        return null;
    }

    @Override
    public boolean releaseSecondaryIpFromNic(long ipAddressId) {
        // TODO Auto-generated method stub
        return false;
    }

    @Override
    public List<? extends Nic> listVmNics(long vmId, Long nicId, Long networkId, String keyword) {
        // TODO Auto-generated method stub
        return null;
    }

    @Override
    public List<? extends Nic> listNics(ListNicsCmd listNicsCmd) {
        // TODO Auto-generated method stub
        return null;
    }

    @Override
    public Map<Network.Capability, String> getNetworkOfferingServiceCapabilities(NetworkOffering offering, Service service) {
        return null;  //To change body of implemented methods use File | Settings | File Templates.
    }

    @Override
    public NicVO savePlaceholderNic(Network network, String ip4Address, String ip6Address, Type vmType) {
        // TODO Auto-generated method stub
        return null;
    }

    @Override
    public DhcpServiceProvider getDhcpServiceProvider(Network network) {
        return null;  //To change body of implemented methods use File | Settings | File Templates.
    }

    @Override
    public DnsServiceProvider getDnsServiceProvider(Network network) {
        return null;  //To change body of implemented methods use File | Settings | File Templates.
    }

    @Override
    public void removeDhcpServiceInSubnet(Nic nic) {
        //To change body of implemented methods use File | Settings | File Templates.
    }

    @Override
    public boolean resourceCountNeedsUpdate(NetworkOffering ntwkOff, ACLType aclType) {
        return false;  //To change body of implemented methods use File | Settings | File Templates.
    }

    @Override
    public void prepareAllNicsForMigration(VirtualMachineProfile vm, DeployDestination dest) {
        return;
    }

    @Override
    public boolean canUpdateInSequence(Network network, boolean forced) {
        return false;
    }

    @Override
    public List<String> getServicesNotSupportedInNewOffering(Network network, long newNetworkOfferingId) {
        return null;
    }

    @Override
    public void cleanupConfigForServicesInNetwork(List<String> services, Network network) {
        return;
    }

    @Override
    public void configureUpdateInSequence(Network network) {
        return;
    }

    @Override
    public int getResourceCount(Network network) {
        return 0;
    }

    @Override public List<NetworkGuru> getNetworkGurus() {
        return null;
    }

    @Override
    public void destroyExpendableRouters(final List<? extends VirtualRouter> routers, final ReservationContext context) throws ResourceUnavailableException {
    }

    @Override
    public boolean areRoutersRunning(final List<? extends VirtualRouter> routers) {
        return false;
    }

    @Override
    public void cleanupNicDhcpDnsEntry(Network network, VirtualMachineProfile vmProfile, NicProfile nicProfile) {
    }

    @Override
    public void finalizeUpdateInSequence(Network network, boolean success) {
        return;
    }

    @Override
    public void prepareNicForMigration(VirtualMachineProfile vm, DeployDestination dest) {
        // TODO Auto-generated method stub

    }

    @Override
    public void commitNicForMigration(VirtualMachineProfile src, VirtualMachineProfile dst) {
        // TODO Auto-generated method stub

    }

    @Override
    public void rollbackNicForMigration(VirtualMachineProfile src, VirtualMachineProfile dst) {
        // TODO Auto-generated method stub

    }

    @Override
    public IpAddress updateIP(Long id, String customId, Boolean displayIp) {
        // TODO Auto-generated method stub
        return null;
    }

    @Override
    public boolean configureNicSecondaryIp(NicSecondaryIp secIp, boolean isZoneSgEnabled) {
        return false;  //To change body of implemented methods use File | Settings | File Templates.
    }

    @Override
    public List<? extends NicSecondaryIp> listVmNicSecondaryIps(ListNicsCmd listNicsCmd) {
        return null;
    }

    @Override
    public boolean releasePodIp(ReleasePodIpCmdByAdmin ip) throws CloudRuntimeException {
        return true;
    }

    @Override
    public AcquirePodIpCmdResponse allocatePodIp(Account account, String zoneId, String podId) throws ResourceAllocationException, ConcurrentOperationException {
        return null;
    }
}
