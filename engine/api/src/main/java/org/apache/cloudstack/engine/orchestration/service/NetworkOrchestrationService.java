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
package org.apache.cloudstack.engine.orchestration.service;

import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

import org.apache.cloudstack.acl.ControlledEntity.ACLType;
import org.apache.cloudstack.framework.config.ConfigKey;
import org.apache.cloudstack.framework.config.ConfigKey.Scope;

import com.cloud.deploy.DataCenterDeployment;
import com.cloud.deploy.DeployDestination;
import com.cloud.deploy.DeploymentPlan;
import com.cloud.exception.ConcurrentOperationException;
import com.cloud.exception.InsufficientAddressCapacityException;
import com.cloud.exception.InsufficientCapacityException;
import com.cloud.exception.InsufficientVirtualNetworkCapacityException;
import com.cloud.exception.ResourceAllocationException;
import com.cloud.exception.ResourceUnavailableException;
import com.cloud.network.Network;
import com.cloud.network.Network.Provider;
import com.cloud.network.Network.Service;
import com.cloud.network.NetworkProfile;
import com.cloud.network.PhysicalNetwork;
import com.cloud.network.element.DhcpServiceProvider;
import com.cloud.network.element.DnsServiceProvider;
import com.cloud.network.element.LoadBalancingServiceProvider;
import com.cloud.network.element.StaticNatServiceProvider;
import com.cloud.network.element.UserDataServiceProvider;
import com.cloud.network.guru.NetworkGuru;
import com.cloud.network.router.VirtualRouter;
import com.cloud.network.rules.LoadBalancerContainer.Scheme;
import com.cloud.offering.NetworkOffering;
import com.cloud.user.Account;
import com.cloud.user.User;
import com.cloud.utils.Pair;
import com.cloud.vm.Nic;
import com.cloud.vm.NicProfile;
import com.cloud.vm.ReservationContext;
import com.cloud.vm.VirtualMachine;
import com.cloud.vm.VirtualMachine.Type;
import com.cloud.vm.VirtualMachineProfile;

/**
 * NetworkManager manages the network for the different end users.
 *
 */
public interface NetworkOrchestrationService {
    String NetworkLockTimeoutCK = "network.lock.timeout";
    String GuestDomainSuffixCK = "guest.domain.suffix";
    String NetworkThrottlingRateCK = "network.throttling.rate";
    String MinVRVersionCK = "minreq.sysvmtemplate.version";

    /**
     * The redundant router handover time which is defined by VRRP2 spec as:
     * (3 * advertisement interval + skew_seconds) or 10s with CloudStack default
     */
    Long RVRHandoverTime = 10000L;

    ConfigKey<String> MinVRVersion = new ConfigKey<String>(String.class, MinVRVersionCK, "Advanced", "4.10.0",
            "What version should the Virtual Routers report", true, ConfigKey.Scope.Zone, null);

    ConfigKey<Integer> NetworkLockTimeout = new ConfigKey<Integer>(Integer.class, NetworkLockTimeoutCK, "Network", "600",
        "Lock wait timeout (seconds) while implementing network", true, Scope.Global, null);

    ConfigKey<String> GuestDomainSuffix = new ConfigKey<String>(String.class, GuestDomainSuffixCK, "Network", "cloud.internal",
        "Default domain name for vms inside virtualized networks fronted by router", true, ConfigKey.Scope.Zone, null);

    ConfigKey<Integer> NetworkThrottlingRate = new ConfigKey<Integer>("Network", Integer.class, NetworkThrottlingRateCK, "200",
        "Default data transfer rate in megabits per second allowed in network.", true, ConfigKey.Scope.Zone);

    ConfigKey<Boolean> PromiscuousMode = new ConfigKey<Boolean>("Advanced", Boolean.class, "network.promiscuous.mode", "false",
            "Whether to allow or deny promiscuous mode on nics for applicable network elements such as for vswitch/dvswitch portgroups.", true);

    ConfigKey<Boolean> MacAddressChanges = new ConfigKey<Boolean>("Advanced", Boolean.class, "network.mac.address.changes", "true",
            "Whether to allow or deny mac address changes on nics for applicable network elements such as for vswitch/dvswitch porgroups.", true);

    ConfigKey<Boolean> ForgedTransmits = new ConfigKey<Boolean>("Advanced", Boolean.class, "network.forged.transmits", "true",
            "Whether to allow or deny forged transmits on nics for applicable network elements such as for vswitch/dvswitch portgroups.", true);

    ConfigKey<Boolean> RollingRestartEnabled = new ConfigKey<Boolean>("Advanced", Boolean.class, "network.rolling.restart", "true",
            "Whether to allow or deny rolling restart of network routers.", true);

    List<? extends Network> setupNetwork(Account owner, NetworkOffering offering, DeploymentPlan plan, String name, String displayText, boolean isDefault)
        throws ConcurrentOperationException;

    List<? extends Network> setupNetwork(Account owner, NetworkOffering offering, Network predefined, DeploymentPlan plan, String name, String displayText,
        boolean errorIfAlreadySetup, Long domainId, ACLType aclType, Boolean subdomainAccess, Long vpcId, Boolean isDisplayNetworkEnabled)
        throws ConcurrentOperationException;

    void allocate(VirtualMachineProfile vm, LinkedHashMap<? extends Network, List<? extends NicProfile>> networks, Map<String, Map<Integer, String>> extraDhcpOptions) throws InsufficientCapacityException,
        ConcurrentOperationException;

    /**
     * configures the provided dhcp options on the given nic.
     * @param network of the nic
     * @param nicId
     * @param extraDhcpOptions
     */
    void configureExtraDhcpOptions(Network network, long nicId, Map<Integer, String> extraDhcpOptions);

    /**
     * configures dhcp options on the given nic.
     * @param network of the nic
     * @param nicId
     */
    void configureExtraDhcpOptions(Network network, long nicId);

    void prepare(VirtualMachineProfile profile, DeployDestination dest, ReservationContext context) throws InsufficientCapacityException, ConcurrentOperationException,
        ResourceUnavailableException;

    void release(VirtualMachineProfile vmProfile, boolean forced) throws ConcurrentOperationException, ResourceUnavailableException;

    void cleanupNics(VirtualMachineProfile vm);

    void removeNics(VirtualMachineProfile vm);

    List<NicProfile> getNicProfiles(VirtualMachine vm);

    Map<String, String> getSystemVMAccessDetails(VirtualMachine vm);

    Pair<? extends NetworkGuru, ? extends Network> implementNetwork(long networkId, DeployDestination dest, ReservationContext context)
        throws ConcurrentOperationException, ResourceUnavailableException, InsufficientCapacityException;

    Map<Integer, String> getExtraDhcpOptions(long nicId);

    /**
     * Returns all extra dhcp options which are set on the provided nic
     * @param nicId
     * @return map which maps the dhcp value on it's option code
     */
    /**
     * prepares vm nic change for migration
     *
     * This method will be called in migration transaction before the vm migration.
     * @param vm
     * @param dest
     */
    void prepareNicForMigration(VirtualMachineProfile vm, DeployDestination dest);

    /**
     * commit vm nic change for migration
     *
     * This method will be called in migration transaction after the successful
     * vm migration.
     * @param src
     * @param dst
     */
    void commitNicForMigration(VirtualMachineProfile src, VirtualMachineProfile dst);

    /**
     * rollback vm nic change for migration
     *
     * This method will be called in migaration transaction after vm migration
     * failure.
     * @param src
     * @param dst
     */
    void rollbackNicForMigration(VirtualMachineProfile src, VirtualMachineProfile dst);

    boolean shutdownNetwork(long networkId, ReservationContext context, boolean cleanupElements);

    boolean destroyNetwork(long networkId, ReservationContext context, boolean forced);

    Network createPrivateNetwork(long networkOfferingId, String name, String displayText, String gateway, String cidr, String vlanId, boolean bypassVlanOverlapCheck, Account owner, PhysicalNetwork pNtwk, Long vpcId) throws ConcurrentOperationException, InsufficientCapacityException, ResourceAllocationException;

    Network createGuestNetwork(long networkOfferingId, String name, String displayText, String gateway, String cidr, String vlanId, boolean bypassVlanOverlapCheck, String networkDomain, Account owner,
                               Long domainId, PhysicalNetwork physicalNetwork, long zoneId, ACLType aclType, Boolean subdomainAccess, Long vpcId, String ip6Gateway, String ip6Cidr,
                               Boolean displayNetworkEnabled, String isolatedPvlan, Network.PVlanType isolatedPvlanType, String externalId, String routerIp, String routerIpv6) throws ConcurrentOperationException, InsufficientCapacityException, ResourceAllocationException;

    UserDataServiceProvider getPasswordResetProvider(Network network);

    UserDataServiceProvider getSSHKeyResetProvider(Network network);

    boolean startNetwork(long networkId, DeployDestination dest, ReservationContext context) throws ConcurrentOperationException, ResourceUnavailableException,
        InsufficientCapacityException;

    boolean reallocate(VirtualMachineProfile vm, DataCenterDeployment dest) throws InsufficientCapacityException, ConcurrentOperationException;

    void saveExtraDhcpOptions(String networkUuid, Long nicId, Map<String, Map<Integer, String>> extraDhcpOptionMap);

    /**
     * @param requested
     * @param network
     * @param isDefaultNic
     * @param deviceId
     * @param vm
     * @return
     * @throws InsufficientVirtualNetworkCapacityException
     * @throws InsufficientAddressCapacityException
     * @throws ConcurrentOperationException
     */
    Pair<NicProfile, Integer> allocateNic(NicProfile requested, Network network, Boolean isDefaultNic, int deviceId, VirtualMachineProfile vm)
        throws InsufficientVirtualNetworkCapacityException, InsufficientAddressCapacityException, ConcurrentOperationException;

    /**
     * @param vmProfile
     * @param dest
     * @param context
     * @param nicId
     * @param network
     * @return
     * @throws InsufficientVirtualNetworkCapacityException
     * @throws InsufficientAddressCapacityException
     * @throws ConcurrentOperationException
     * @throws InsufficientCapacityException
     * @throws ResourceUnavailableException
     */
    NicProfile prepareNic(VirtualMachineProfile vmProfile, DeployDestination dest, ReservationContext context, long nicId, Network network)
        throws InsufficientVirtualNetworkCapacityException, InsufficientAddressCapacityException, ConcurrentOperationException, InsufficientCapacityException,
        ResourceUnavailableException;

    /**
     * Removes the provided nic from the given vm
     * @param vm
     * @param nic
     */
    void removeNic(VirtualMachineProfile vm, Nic nic);

    /**
     * @param network
     * @param provider
     * @return
     */
    boolean setupDns(Network network, Provider provider);

    void releaseNic(VirtualMachineProfile vmProfile, Nic nic) throws ConcurrentOperationException, ResourceUnavailableException;

    NicProfile createNicForVm(Network network, NicProfile requested, ReservationContext context, VirtualMachineProfile vmProfile, boolean prepare)
        throws InsufficientVirtualNetworkCapacityException, InsufficientAddressCapacityException, ConcurrentOperationException, InsufficientCapacityException,
        ResourceUnavailableException;

    NetworkProfile convertNetworkToNetworkProfile(long networkId);

    boolean restartNetwork(Long networkId, Account callerAccount, User callerUser, boolean cleanup) throws ConcurrentOperationException, ResourceUnavailableException,
        InsufficientCapacityException;

    boolean shutdownNetworkElementsAndResources(ReservationContext context, boolean b, Network network);

    void implementNetworkElementsAndResources(DeployDestination dest, ReservationContext context, Network network, NetworkOffering findById)
        throws ConcurrentOperationException, InsufficientAddressCapacityException, ResourceUnavailableException, InsufficientCapacityException;

    Map<String, String> finalizeServicesAndProvidersForNetwork(NetworkOffering offering, Long physicalNetworkId);

    List<Provider> getProvidersForServiceInNetwork(Network network, Service service);

    StaticNatServiceProvider getStaticNatProviderForNetwork(Network network);

    boolean isNetworkInlineMode(Network network);

    LoadBalancingServiceProvider getLoadBalancingProviderForNetwork(Network network, Scheme lbScheme);

    boolean isSecondaryIpSetForNic(long nicId);

    List<? extends Nic> listVmNics(long vmId, Long nicId, Long networkId, String keyword);

    Nic savePlaceholderNic(Network network, String ip4Address, String ip6Address, Type vmType);

    DhcpServiceProvider getDhcpServiceProvider(Network network);

    DnsServiceProvider getDnsServiceProvider(Network network);

    void removeDhcpServiceInSubnet(Nic nic);

    boolean resourceCountNeedsUpdate(NetworkOffering ntwkOff, ACLType aclType);

    void prepareAllNicsForMigration(VirtualMachineProfile vm, DeployDestination dest);

    boolean canUpdateInSequence(Network network, boolean forced);

    List<String> getServicesNotSupportedInNewOffering(Network network, long newNetworkOfferingId);

    void cleanupConfigForServicesInNetwork(List<String> services, Network network);

    void configureUpdateInSequence(Network network);

    int getResourceCount(Network network);

    void finalizeUpdateInSequence(Network network, boolean success);

    /**
     * Adds hypervisor hostname to a file - hypervisor-host-name if the userdata
     * service provider is ConfigDrive or VirtualRouter
     * @param vm holds the details of the Virtual Machine
     * @param dest holds information of the destination
     * @param migrationSuccessful
     * @throws ResourceUnavailableException in case Datastore or agent to which a command is to be sent is unavailable
     */
    void setHypervisorHostname(VirtualMachineProfile vm, DeployDestination dest, boolean migrationSuccessful) throws ResourceUnavailableException;

    List<NetworkGuru> getNetworkGurus();

    /**
     * destroyExpendableRouters will find and destroy safely destroyable routers
     * that are in bad states or are backup routers
     * @param routers list of routers
     * @param context reservation context
     * @throws ResourceUnavailableException
     */
    void destroyExpendableRouters(final List<? extends VirtualRouter> routers, final ReservationContext context) throws ResourceUnavailableException;

    /**
     * areRoutersRunning check if the given list of routers are running
     * @param routers list of routers
     * @return returns true is all routers are running
     */
    boolean areRoutersRunning(final List<? extends VirtualRouter> routers);

    /**
     * Remove entry from /etc/dhcphosts and /etc/hosts on virtual routers
     */
    void cleanupNicDhcpDnsEntry(Network network, VirtualMachineProfile vmProfile, NicProfile nicProfile);

    Pair<NicProfile, Integer> importNic(final String macAddress, int deviceId, final Network network, final Boolean isDefaultNic, final VirtualMachine vm, final Network.IpAddresses ipAddresses, boolean forced) throws InsufficientVirtualNetworkCapacityException, InsufficientAddressCapacityException;

    void unmanageNics(VirtualMachineProfile vm);
}
