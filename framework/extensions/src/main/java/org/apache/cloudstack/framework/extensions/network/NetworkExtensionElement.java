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
package org.apache.cloudstack.framework.extensions.network;

import java.io.File;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.net.InetAddress;
import java.nio.ByteBuffer;
import java.nio.file.Files;

import javax.inject.Inject;
import javax.naming.ConfigurationException;

import com.cloud.agent.api.to.LoadBalancerTO;
import com.cloud.dc.DataCenter;
import com.cloud.dc.Vlan;
import com.cloud.dc.VlanVO;
import com.cloud.dc.dao.DataCenterDao;
import com.cloud.dc.dao.VlanDao;
import com.cloud.deploy.DeployDestination;
import com.cloud.exception.ConcurrentOperationException;
import com.cloud.exception.InsufficientCapacityException;
import com.cloud.exception.ResourceUnavailableException;
import com.cloud.host.dao.HostDao;
import com.cloud.hypervisor.Hypervisor;
import com.cloud.network.IpAddressManager;
import com.cloud.network.IpAddress;
import com.cloud.network.Network;
import com.cloud.network.Network.Capability;
import com.cloud.network.Network.Provider;
import com.cloud.network.Network.Service;
import com.cloud.network.NetworkModel;
import com.cloud.network.Networks;
import com.cloud.network.dao.NetworkDetailVO;
import com.cloud.network.dao.PhysicalNetworkDao;
import com.cloud.network.dao.PhysicalNetworkVO;
import com.cloud.network.PhysicalNetworkServiceProvider;
import com.cloud.network.PublicIpAddress;
import com.cloud.network.addr.PublicIp;
import com.cloud.network.dao.FirewallRulesDao;
import com.cloud.network.dao.IPAddressDao;
import com.cloud.network.dao.IPAddressVO;
import com.cloud.network.dao.NetworkDetailsDao;
import com.cloud.network.dao.NetworkServiceMapDao;
import com.cloud.network.vpc.dao.VpcDao;
import com.cloud.network.element.AggregatedCommandExecutor;
import com.cloud.network.element.DhcpServiceProvider;
import com.cloud.network.element.DnsServiceProvider;
import com.cloud.network.element.FirewallServiceProvider;
import com.cloud.network.element.IpDeployer;
import com.cloud.network.element.LoadBalancingServiceProvider;
import com.cloud.network.element.NetworkACLServiceProvider;
import com.cloud.network.element.NetworkElement;
import com.cloud.network.element.PortForwardingServiceProvider;
import com.cloud.network.element.SourceNatServiceProvider;
import com.cloud.network.element.StaticNatServiceProvider;
import com.cloud.network.element.UserDataServiceProvider;
import com.cloud.network.element.VpcProvider;
import com.cloud.network.vpc.NetworkACLItem;
import com.cloud.network.vpc.PrivateGateway;
import com.cloud.network.vpc.StaticRouteProfile;
import com.cloud.network.vpc.Vpc;
import com.cloud.network.lb.LoadBalancingRule;
import com.cloud.network.rules.FirewallRule;
import com.cloud.network.rules.FirewallRuleVO;
import com.cloud.network.rules.PortForwardingRule;
import com.cloud.network.rules.StaticNat;
import com.cloud.offerings.NetworkOfferingVO;
import com.cloud.offerings.dao.NetworkOfferingDao;
import com.cloud.service.ServiceOfferingVO;
import com.cloud.service.dao.ServiceOfferingDao;
import com.cloud.storage.dao.GuestOSCategoryDao;
import com.cloud.storage.dao.GuestOSDao;
import com.cloud.user.AccountService;
import com.cloud.uservm.UserVm;
import com.cloud.offering.NetworkOffering;
import com.cloud.user.Account;
import com.cloud.utils.Pair;
import com.cloud.utils.component.AdapterBase;
import com.cloud.utils.exception.CloudRuntimeException;
import com.cloud.vm.Nic;
import com.cloud.vm.NicProfile;
import com.cloud.vm.NicVO;
import com.cloud.vm.ReservationContext;
import com.cloud.vm.UserVmVO;
import com.cloud.vm.VirtualMachine;
import com.cloud.vm.VirtualMachineManager;
import com.cloud.vm.VirtualMachineProfile;
import com.cloud.vm.VMInstanceDetailVO;
import com.cloud.vm.VmDetailConstants;
import com.cloud.vm.dao.NicDao;
import com.cloud.vm.dao.UserVmDao;
import com.cloud.vm.dao.VMInstanceDetailsDao;
import com.google.gson.Gson;
import com.google.gson.JsonObject;

import org.apache.cloudstack.api.ApiConstants;
import org.apache.cloudstack.engine.orchestration.service.NetworkOrchestrationService;
import org.apache.cloudstack.extension.Extension;
import org.apache.cloudstack.extension.ExtensionHelper;
import org.apache.cloudstack.extension.NetworkCustomActionProvider;
import org.apache.cloudstack.resourcedetail.dao.VpcDetailsDao;

import java.nio.charset.StandardCharsets;
import java.util.Base64;
import java.util.stream.Collectors;


/**
 * NetworkExtensionElement is a network plugin that delegates all network
 * configuration to an external script via a registered {@link Extension} of
 * type {@code NetworkOrchestrator}.
 *
 * <h3>Script invocation model</h3>
 * The script is called with a command name and optional CLI arguments.
 * Two JSON blobs are always forwarded as named CLI arguments:
 * <ul>
 *   <li>{@value #ARG_PHYSICAL_NETWORK_EXTENSION_DETAILS} {@code <json>} – all
 *       details stored in {@code extension_resource_map_details} when the
 *       extension was registered with the physical network (connection info,
 *       host list, credentials, etc.).  The script owns the schema.</li>
 *   <li>{@value #ARG_NETWORK_EXTENSION_DETAILS} {@code <json>} – the
 *       per-network JSON blob stored in {@code network_details} under key
 *       {@value #NETWORK_DETAIL_EXTENSION_DETAILS}.  Populated by the
 *       script's {@code ensure-network-device} response and updated on
 *       failover (e.g. selected host, namespace, segment ID).</li>
 * </ul>
 *
 * <h3>Script resolution</h3>
 * The script is resolved from the extension path set when the extension was
 * created.  Lookup order (first match wins):
 * <ol>
 *   <li>{@code <extensionPath>/<extensionName>.sh} — preferred convention,
 *       e.g. for an extension named {@code network-extension} the script is
 *       {@code network-extension.sh}.</li>
 *   <li>{@code <extensionPath>} itself, if it is a file and is executable.</li>
 * </ol>
 *
 * <h3>Physical-network extension details</h3>
 * Any key/value pairs stored in {@code extension_resource_map_details} at
 * registration time are passed verbatim as a JSON object.  There are no
 * pre-defined keys — the user and the script agree on the schema.  The only
 * special treatment is that keys named {@code password} or {@code sshkey} are
 * redacted in log output.
 *
 * <p>Two well-known optional keys control which host network interfaces the
 * wrapper script uses to create bridges and veth pairs:</p>
 * <ul>
 *   <li>{@code guest.network.device} — host NIC for guest (internal) traffic;
 *       defaults to {@code eth1} when absent.</li>
 *   <li>{@code public.network.device} — host NIC for public (NAT/external)
 *       traffic; defaults to {@code eth1} when absent.</li>
 * </ul>
 *
 * <p>Example registration for a KVM-namespace backend:</p>
 * <pre>
 *   cmk registerExtension id=&lt;ext-uuid&gt; resourcetype=PhysicalNetwork \
 *       resourceid=&lt;phys-uuid&gt; \
 *       details[0].key=hosts                details[0].value=192.168.1.10,192.168.1.11 \
 *       details[1].key=port                 details[1].value=22 \
 *       details[2].key=username             details[2].value=root \
 *       details[3].key=sshkey               details[3].value="$(cat ~/.ssh/id_rsa)" \
 *       details[4].key=guest.network.device details[4].value=eth1 \
 *       details[5].key=public.network.device details[5].value=eth1
 * </pre>
 *
 * <h3>Per-network extension details</h3>
 * On first {@code implement}, the script is called with
 * {@code ensure-network-device}.  The script selects a host (e.g. from the
 * {@code hosts} list in the physical-network details), checks it is reachable,
 * and prints a JSON object to stdout.  CloudStack stores this verbatim in
 * {@code network_details} under key {@value #NETWORK_DETAIL_EXTENSION_DETAILS}
 * and forwards it on every subsequent call via
 * {@value #ARG_NETWORK_EXTENSION_DETAILS}.
 *
 * <p>Example per-network details (KVM-namespace backend):</p>
 * <pre>{"host":"192.168.1.10","namespace":"cs-net-42"}</pre>
 *
 * <h3>Network capabilities</h3>
 * When creating the extension, set detail {@code network.capabilities} to a
 * JSON object describing the services and their capabilities:
 * <pre>
 * {
 *   "services": ["SourceNat", "StaticNat", "PortForwarding", "Firewall"],
 *   "capabilities": {
 *     "SourceNat": { "SupportedSourceNatTypes": "peraccount", "RedundantRouter": "false" }
 *   }
 * }
 * </pre>
 */
public class NetworkExtensionElement extends AdapterBase implements
        NetworkElement, SourceNatServiceProvider, StaticNatServiceProvider,
        PortForwardingServiceProvider, IpDeployer, NetworkCustomActionProvider,
        DhcpServiceProvider, DnsServiceProvider, FirewallServiceProvider,
        UserDataServiceProvider, LoadBalancingServiceProvider,
        VpcProvider, NetworkACLServiceProvider, AggregatedCommandExecutor {

    private static final Map<Service, Map<Capability, String>> DEFAULT_CAPABILITIES = new HashMap<>();


    /**
     * When non-null, restricts all operations to the extension whose name
     * matches this provider name.
     */
    private String providerName;

    @Inject
    private NetworkModel networkModel;
    @Inject
    private NetworkServiceMapDao ntwkSrvcDao;
    @Inject
    private ExtensionHelper extensionHelper;
    @Inject
    private NetworkDetailsDao networkDetailsDao;
    @Inject
    private IpAddressManager ipAddressManager;
    @Inject
    private NetworkOrchestrationService networkManager;
    @Inject
    private AccountService accountService;
    @Inject
    private PhysicalNetworkDao physicalNetworkDao;
    @Inject
    private DataCenterDao dataCenterDao;
    @Inject
    private VlanDao vlanDao;
    @Inject
    private GuestOSCategoryDao guestOSCategoryDao;
    @Inject
    private GuestOSDao guestOSDao;
    @Inject
    private HostDao hostDao;
    @Inject
    private VMInstanceDetailsDao vmInstanceDetailsDao;
    @Inject
    private UserVmDao userVmDao;
    @Inject
    private NicDao nicDao;
    @Inject
    private NetworkOfferingDao networkOfferingDao;
    @Inject
    private ServiceOfferingDao serviceOfferingDao;
    @Inject
    private FirewallRulesDao firewallRulesDao;
    @Inject
    private IPAddressDao ipAddressDao;
    @Inject
    private VpcDao vpcDao;
    @Inject
    private VpcDetailsDao vpcDetailsDao;

    // ---- Script argument names ----

    /** CLI argument carrying physical-network extension details as a JSON object. */
    public static final String ARG_PHYSICAL_NETWORK_EXTENSION_DETAILS = "--physical-network-extension-details";

    /** CLI argument carrying per-network opaque JSON blob. */
    public static final String ARG_NETWORK_EXTENSION_DETAILS = "--network-extension-details";

    /** CLI argument carrying per-action parameters as a JSON object. */
    public static final String ARG_ACTION_PARAMS = "--action-params";

    // ---- Network detail key ----

    /**
     * Key used to persist the per-network JSON blob in {@code network_details}.
     * The blob is produced by the network-extension.sh's {@code ensure-network-device}
     * command and may contain any fields the script needs (e.g. selected host,
     * namespace name, VRF ID, …).
     */
    public static final String NETWORK_DETAIL_EXTENSION_DETAILS = "extension.details";

    public String getProviderName() {
        return providerName;
    }

    /**
     * Returns a new {@link NetworkExtensionElement} scoped to {@code providerName},
     * sharing all injected dependencies with this instance.
     */
    public NetworkExtensionElement withProviderName(String providerName) {
        NetworkExtensionElement copy = new NetworkExtensionElement();
        copy.networkModel                   = this.networkModel;
        copy.ntwkSrvcDao                    = this.ntwkSrvcDao;
        copy.extensionHelper                = this.extensionHelper;
        copy.networkDetailsDao              = this.networkDetailsDao;
        copy.ipAddressManager               = this.ipAddressManager;
        copy.physicalNetworkDao             = this.physicalNetworkDao;
        copy.dataCenterDao                  = this.dataCenterDao;
        copy.vlanDao                        = this.vlanDao;
        copy.guestOSCategoryDao             = this.guestOSCategoryDao;
        copy.guestOSDao                     = this.guestOSDao;
        copy.hostDao                        = this.hostDao;
        copy.vmInstanceDetailsDao           = this.vmInstanceDetailsDao;
        copy.userVmDao                      = this.userVmDao;
        copy.nicDao                         = this.nicDao;
        copy.networkManager                 = this.networkManager;
        copy.networkOfferingDao             = this.networkOfferingDao;
        copy.serviceOfferingDao             = this.serviceOfferingDao;
        copy.accountService                 = this.accountService;
        copy.firewallRulesDao               = this.firewallRulesDao;
        copy.ipAddressDao                   = this.ipAddressDao;
        copy.vpcDao                         = this.vpcDao;
        copy.vpcDetailsDao                  = this.vpcDetailsDao;
        copy.providerName                   = providerName;

        logger.debug("NetworkExtensionElement initialised with provider name '{}'", providerName);
        return copy;
    }

    // ---- Capabilities ----

    @Override
    public Map<Service, Map<Capability, String>> getCapabilities() {
        try {
            // If this element is scoped to a provider name, prefer capabilities stored
            // in the extension's "network.capabilities" detail.  The ExtensionHelper
            // exposes a helper that loads the Service→Capability map from the DB.
            if (providerName != null && !providerName.isBlank()) {
                Map<Service, Map<Capability, String>> caps = extensionHelper.getNetworkCapabilitiesForProvider(null, providerName);
                if (caps != null && !caps.isEmpty()) {
                    return caps;
                }
            }
        } catch (Exception e) {
            logger.warn("Failed to load network capabilities from extension details for provider '{}': {}", providerName, e.getMessage());
        }

        return DEFAULT_CAPABILITIES;
    }

    @Override
    public Provider getProvider() {
        if (providerName != null) {
            return Provider.createTransientProvider(providerName);
        }
        return Provider.NetworkExtension;
    }

    // ---- Extension / provider resolution ----

    protected Extension resolveExtension(Network network) {
        Long physicalNetworkId = network.getPhysicalNetworkId();
        if (physicalNetworkId == null) {
            logger.warn("Network {} has no physical network — cannot resolve extension", network.getId());
            return null;
        }
        if (providerName != null && !providerName.isBlank()) {
            Extension ext = extensionHelper.getExtensionForPhysicalNetworkAndProvider(physicalNetworkId, providerName);
            if (ext != null) {
                return ext;
            }
            logger.warn("No extension found for scoped provider '{}' on physical network {}", providerName, physicalNetworkId);
        }
        List<String> providers = ntwkSrvcDao.getDistinctProviders(network.getId());
        if (providers != null) {
            for (String p : providers) {
                Extension ext = extensionHelper.getExtensionForPhysicalNetworkAndProvider(physicalNetworkId, p);
                if (ext != null) {
                    return ext;
                }
            }
        }
        return extensionHelper.getExtensionForPhysicalNetwork(physicalNetworkId);
    }

    protected boolean canHandle(Network network, Service service) {
        Long physicalNetworkId = network.getPhysicalNetworkId();
        if (physicalNetworkId == null) {
            return false;
        }
        if (providerName != null && !providerName.isBlank()) {
            boolean hasExt = extensionHelper.getExtensionForPhysicalNetworkAndProvider(physicalNetworkId, providerName) != null;
            if (!hasExt) {
                return false;
            }
            if (service == null) {
                return true;
            }
            List<String> sp = ntwkSrvcDao.getProvidersForServiceInNetwork(network.getId(), service);
            return sp != null && sp.stream()
                    .anyMatch(p -> extensionHelper.getExtensionForPhysicalNetworkAndProvider(physicalNetworkId, p) != null);
        }
        List<String> providers = ntwkSrvcDao.getDistinctProviders(network.getId());
        if (providers == null || providers.isEmpty()) {
            return false;
        }
        boolean hasExtProv = providers.stream().anyMatch(
                p -> extensionHelper.getExtensionForPhysicalNetworkAndProvider(physicalNetworkId, p) != null);
        if (!hasExtProv) {
            return false;
        }
        if (service == null) {
            return true;
        }
        List<String> sp = ntwkSrvcDao.getProvidersForServiceInNetwork(network.getId(), service);
        return sp != null && sp.stream()
                .anyMatch(p -> extensionHelper.getExtensionForPhysicalNetworkAndProvider(physicalNetworkId, p) != null);
    }

    @Override
    public boolean configure(String name, Map<String, Object> params) throws ConfigurationException {
        super.configure(name, params);
        return true;
    }

    // ---- NetworkElement lifecycle ----

    @Override
    public boolean implement(Network network, NetworkOffering offering, DeployDestination dest,
            ReservationContext context) throws ConcurrentOperationException,
            ResourceUnavailableException, InsufficientCapacityException {
        if (!canHandle(network, null)) {
            return false;
        }
        logger.info("Implementing network extension for network {} (VLAN {})", network.getId(), network.getBroadcastUri());

        // Step 1: Ensure a network device is selected and its details stored.
        ensureExtensionDetails(network);

        // Step 2: Allocate the IPs for DHCP/DNS/UserData service if needed
        String extensionIp = ensureExtensionIp(network);

        String vlanId = getVlanId(network);

        // Build common vpc/network args
        List<String> vpcArgs = getVpcIdArgs(network);

        // Step 2: Create the network on the device.
        List<String> implArgs = new ArrayList<>();
        implArgs.add("--network-id");   implArgs.add(String.valueOf(network.getId()));
        implArgs.add("--vlan");         implArgs.add(safeStr(vlanId));
        implArgs.add("--gateway");      implArgs.add(safeStr(network.getGateway()));
        implArgs.add("--cidr");         implArgs.add(safeStr(network.getCidr()));
        implArgs.add("--extension-ip"); implArgs.add(safeStr(extensionIp));
        implArgs.addAll(vpcArgs);

        boolean result = executeScript(network, "implement-network", implArgs.toArray(new String[0]));

        if (!result) {
            return false;
        }

        // Step 3: Configure source NAT for both VPC and non-VPC networks for
        // compatibility (other network-element providers may also implement VPC tiers).
        // When this is a VPC tier, the script's assign-ip does nothing for source-nat
        // because VPC source NAT is managed at the VPC level by implementVpc().
        if (canHandle(network, Service.SourceNat)) {
            try {
                if (network.getVpcId() == null) {
                    // Isolated network: apply the network's own source NAT IP.
                    Account owner = accountService.getAccount(network.getAccountId());
                    PublicIpAddress existingIp = networkModel.getSourceNatIpAddressForGuestNetwork(owner, network);
                    if (existingIp != null) {
                        applyIps(network, List.of(existingIp), Set.of(Service.SourceNat));
                    }
                } else {
                    // VPC tier: apply the VPC-level source NAT IP (script is a no-op for SNAT).
                    final PublicIpAddress vpcSourceNatIp = getVpcSourceNatIp(network.getVpcId());
                    if (vpcSourceNatIp != null) {
                        applyIps(network, List.of(vpcSourceNatIp), Set.of(Service.SourceNat));
                    }
                }
            } catch (Exception e) {
                logger.warn("Failed to configure source NAT IP for network {}: {}", network.getId(), e.getMessage(), e);
            }
        }

        return true;
    }

    @Override
    public boolean prepare(Network network, NicProfile nic, VirtualMachineProfile vm,
            DeployDestination dest, ReservationContext context)
            throws ConcurrentOperationException, ResourceUnavailableException, InsufficientCapacityException {
        // Copy from VirtualRouterElement.java
        if (vm.getType() != VirtualMachine.Type.User || vm.getHypervisorType() == Hypervisor.HypervisorType.BareMetal) {
            return false;
        }

        if (!canHandle(network, null)) {
            return false;
        }

        if (!networkModel.isProviderEnabledInPhysicalNetwork(networkModel.getPhysicalNetworkId(network), getProvider().getName())) {
            return false;
        }

        final NetworkOfferingVO offering = networkOfferingDao.findById(network.getNetworkOfferingId());
        implement(network, offering, dest, context);

        return true;
    }

    @Override
    public boolean release(Network network, NicProfile nic, VirtualMachineProfile vm,
            ReservationContext context) throws ConcurrentOperationException, ResourceUnavailableException {
        return true;
    }

    @Override
    public boolean shutdown(Network network, ReservationContext context, boolean cleanup)
            throws ConcurrentOperationException, ResourceUnavailableException {
        logger.info("Shutting down network extension for network {}", network.getId());
        List<String> args = new ArrayList<>();
        args.add("--network-id"); args.add(String.valueOf(network.getId()));
        args.add("--vlan");       args.add(safeStr(getVlanId(network)));
        args.addAll(getVpcIdArgs(network));
        boolean result = executeScript(network, "shutdown-network", args.toArray(new String[0]));
        if (result) {
            // Remove stored per-network extension details (e.g. namespace). For VPC-backed networks
            // the namespace is named cs-vpc-<vpcId>, stored in the extension details. Removing the
            // stored details ensures the namespace is deleted/forgotten on shutdown.
            try {
                networkDetailsDao.removeDetail(network.getId(), NETWORK_DETAIL_EXTENSION_DETAILS);
            } catch (Exception e) {
                logger.warn("Failed to remove network extension details for network {}: {}", network.getId(), e.getMessage());
            }
        }
        return result;
    }

    @Override
    public boolean destroy(Network network, ReservationContext context)
            throws ConcurrentOperationException, ResourceUnavailableException {
        logger.info("Destroying network extension for network {}", network.getId());
        List<String> args = new ArrayList<>();
        args.add("--network-id"); args.add(String.valueOf(network.getId()));
        args.add("--vlan");       args.add(safeStr(getVlanId(network)));
        args.addAll(getVpcIdArgs(network));
        // For both isolated and VPC tier networks, use destroy-network.
        // For VPC tiers, the script preserves the shared namespace;
        // the VPC namespace is removed only when shutdownVpc() calls shutdown-vpc.
        boolean result = executeScript(network, "destroy-network", args.toArray(new String[0]));
        if (result) {
            cleanupPlaceholderNicIp(network, context);
            networkDetailsDao.removeDetail(network.getId(), NETWORK_DETAIL_EXTENSION_DETAILS);
        }
        return result;
    }

    /**
     * Releases placeholder NIC IPs allocated for DHCP/DNS/UserData extension traffic,
     * then removes the placeholder NIC record(s) for this network.
     */
    protected void cleanupPlaceholderNicIp(Network network, ReservationContext context) {
        List<NicVO> placeholderNics = nicDao.listPlaceholderNicsByNetworkIdAndVmType(
                network.getId(), VirtualMachine.Type.DomainRouter);
        if (placeholderNics == null || placeholderNics.isEmpty()) {
            return;
        }

        long userId = accountService.getSystemUser().getId();
        Account caller = accountService.getSystemAccount();
        if (context != null && context.getAccount() != null) {
            caller = context.getAccount();
        }

        for (NicVO placeholderNic : placeholderNics) {
            try {
                String ip = placeholderNic.getIPv4Address();
                if (ip != null && !ip.isBlank()) {
                    logger.debug("Cleaning up PlaceHolder IP {} on network {}", ip, network.getId());
                    IPAddressVO ipAddress = ipAddressDao.findByIpAndSourceNetworkId(network.getId(), ip);
                    if (ipAddress != null) {
                        if (Network.GuestType.Shared.equals(network.getGuestType())) {
                            ipAddressManager.disassociatePublicIpAddress(ipAddress, userId, caller);
                        } else {
                            ipAddressManager.markIpAsUnavailable(ipAddress.getId());
                            ipAddressDao.unassignIpAddress(ipAddress.getId());
                        }
                    }
                }
            } catch (Exception e) {
                logger.warn("Failed to release placeholder IP for network {} and nic {}: {}",
                        network.getId(), placeholderNic.getId(), e.getMessage());
            }

            try {
                nicDao.remove(placeholderNic.getId());
            } catch (Exception e) {
                logger.warn("Failed to remove placeholder nic {} for network {}: {}",
                        placeholderNic.getId(), network.getId(), e.getMessage());
            }
        }
    }

    @Override
    public boolean isReady(PhysicalNetworkServiceProvider provider) {
        return true;
    }

    @Override
    public boolean shutdownProviderInstances(PhysicalNetworkServiceProvider provider, ReservationContext context)
            throws ConcurrentOperationException, ResourceUnavailableException {
        return true;
    }

    @Override
    public boolean canEnableIndividualServices() {
        return true;
    }

    @Override
    public boolean verifyServicesCombination(Set<Service> services) {
        return true;
    }

    // ---- ensure-network-device ----

    /**
     * Calls the network-extension.sh script with {@code ensure-network-device} before
     * the first network operation.  The script verifies the previously selected
     * device is reachable (using the {@code hosts} list in the physical-network
     * extension details) and performs failover if needed.  The returned JSON is
     * persisted in {@code network_details} and forwarded to all subsequent calls
     * via {@value #ARG_NETWORK_EXTENSION_DETAILS}.
     *
     * <p>For VPC tier networks the extension details are inherited from the VPC-level
     * details (stored in {@code vpc_details}) so all tiers in the same VPC share
     * the same host/namespace binding.  The script's {@code ensure-network-device}
     * is only called at the VPC level (see {@link #ensureExtensionDetails(Vpc)}).</p>
     */
    protected void ensureExtensionDetails(Network network) {
        if (network.getVpcId() != null) {
            Vpc vpc = vpcDao.findById(network.getVpcId());
            ensureExtensionDetails(vpc);
            return;
        }

        // Isolated network: run ensure-network-device to select / validate the host.
        Map<String, String> stored = networkDetailsDao.listDetailsKeyPairs(network.getId());
        String currentDetails = stored != null
                ? stored.getOrDefault(NETWORK_DETAIL_EXTENSION_DETAILS, "{}") : "{}";

        logger.info("Ensuring network device for network {} (current={})", network.getId(), currentDetails);

        Extension extension = resolveExtension(network);
        File scriptFile = resolveScriptFile(network, extension);

        String physicalNetworkDetailsJson = buildPhysicalNetworkDetailsJson(network.getPhysicalNetworkId(), extension);

        List<String> cmdLine = new ArrayList<>();
        cmdLine.add(scriptFile.getAbsolutePath());
        cmdLine.add("ensure-network-device");
        cmdLine.add("--network-id");
        cmdLine.add(String.valueOf(network.getId()));
        cmdLine.add("--vlan");
        cmdLine.add(safeStr(getVlanId(network)));
        cmdLine.add("--zone-id");
        cmdLine.add(String.valueOf(network.getDataCenterId()));
        // Pass VPC ID so the script can derive the correct namespace (cs-net-<vpcId>)
        if (network.getVpcId() != null) {
            cmdLine.add("--vpc-id");
            cmdLine.add(String.valueOf(network.getVpcId()));
        }
        cmdLine.add("--current-details");
        cmdLine.add(currentDetails);
        cmdLine.add(ARG_PHYSICAL_NETWORK_EXTENSION_DETAILS);
        cmdLine.add(physicalNetworkDetailsJson);
        cmdLine.add(ARG_NETWORK_EXTENSION_DETAILS);
        cmdLine.add(currentDetails);

        try {
            ProcessBuilder pb = new ProcessBuilder(cmdLine);
            pb.redirectErrorStream(true);
            Process process = pb.start();
            String output = new String(process.getInputStream().readAllBytes()).trim();
            int exitCode = process.waitFor();

            if (exitCode != 0) {
                logger.warn("ensure-network-device exited {} for network {} — keeping current details",
                        exitCode, network.getId());
                if ("{}".equals(currentDetails)) {
                    networkDetailsDao.addDetail(network.getId(), NETWORK_DETAIL_EXTENSION_DETAILS, "{}", false);
                }
                return;
            }
            if (output.isEmpty()) {
                output = "{}".equals(currentDetails) ? "{}" : currentDetails;
            }
            if (!output.equals(currentDetails)) {
                logger.info("Network device updated for network {}: {}", network.getId(), output);
                networkDetailsDao.addDetail(network.getId(), NETWORK_DETAIL_EXTENSION_DETAILS, output, false);
            } else {
                logger.debug("Network device unchanged for network {}: {}", network.getId(), output);
            }
        } catch (Exception e) {
            logger.warn("Failed ensure-network-device for network {}: {}", network.getId(), e.getMessage());
            if ("{}".equals(currentDetails)) {
                networkDetailsDao.addDetail(network.getId(), NETWORK_DETAIL_EXTENSION_DETAILS, "{}", false);
            }
        }
    }

    /*
    * If the network supports DHCP/DNS/UserData but not SourceNat/Gateway,
    * an additional IP is needed on the external network to host these services.
    * This method ensures that IP is allocated and configured on the external network and returns its address.
     */
    protected String ensureExtensionIp(Network network) {
        if (networkModel.isAnyServiceSupportedInNetwork(network.getId(), this.getProvider(),
                Service.SourceNat, Service.Gateway)) {
            // Gateway or Source NAT will be configured on the external network
            return network.getGateway();
        }

        if (networkModel.isAnyServiceSupportedInNetwork(network.getId(), this.getProvider(),
                Service.Dhcp, Service.Dns, Service.UserData)) {
                try {
                    // An extra IP will be allocated and configured on the external network
                    Nic placeholderNic = networkModel.getPlaceholderNicForRouter(network, null);
                    if (placeholderNic == null) {
                        NetworkDetailVO routerIpDetail = networkDetailsDao.findDetail(network.getId(), ApiConstants.ROUTER_IP);
                        String routerIp = routerIpDetail != null ? routerIpDetail.getValue() : null;
                        Account account = accountService.getAccount(network.getAccountId());
                        String extensionIp = Network.GuestType.Shared.equals(network.getGuestType()) ?
                                ipAddressManager.assignPublicIpAddress(network.getDataCenterId(), null, account, Vlan.VlanType.DirectAttached, network.getId(), routerIp, false, false).getAddress().toString():
                                ipAddressManager.acquireGuestIpAddress(network, routerIp);
                        logger.debug("Saving placeholder nic with ip4 address {} for the network", extensionIp, network);
                        networkManager.savePlaceholderNic(network, extensionIp, null, VirtualMachine.Type.DomainRouter);
                        return extensionIp;
                    }
                    return placeholderNic.getIPv4Address();
                } catch (Exception e) {
                    logger.warn("Failed to acquire extension IP for network {}: {}", network.getId(), e.getMessage());
                }
        }
        return null;
    }

    // ---- IpDeployer ----

    @Override
    public boolean applyIps(Network network, List<? extends PublicIpAddress> ipAddress, Set<Service> services)
            throws ResourceUnavailableException {
        if (ipAddress == null || ipAddress.isEmpty()) {
            return true;
        }
        logger.info("Applying {} IPs for network {}", ipAddress.size(), network.getId());
        String vlanId = getVlanId(network);

        for (PublicIpAddress ip : ipAddress) {
            boolean isSourceNat = ip.isSourceNat();
            boolean isRevoke = ip.getState() == IpAddress.State.Releasing;
            String action = isRevoke ? "release-ip" : "assign-ip";

            // Public VLAN tag (e.g. "101") from the IP's VLAN record.
            String publicVlanTag = safeStr(ip.getVlanTag());

            // Compute public IP gateway and CIDR (from the PublicIpAddress if available)
            String publicGateway;
            String publicCidr;
            try {
                publicGateway = ip.getGateway();
                String publicIpStr = ip.getAddress() != null ? ip.getAddress().addr() : null;
                String publicNetmask = ip.getNetmask();
                publicCidr = buildCidrFromIpAndNetmask(publicIpStr, publicNetmask);
            } catch (Exception e) {
                publicGateway = null;
                publicCidr = null;
            }

            List<String> args = new ArrayList<>();
            args.add("--network-id");         args.add(String.valueOf(network.getId()));
            args.add("--vlan");               args.add(safeStr(vlanId));
            args.add("--public-ip");          args.add(ip.getAddress().addr());
            args.add("--source-nat");         args.add(String.valueOf(isSourceNat));
            args.add("--gateway");            args.add(safeStr(network.getGateway()));
            args.add("--cidr");               args.add(safeStr(network.getCidr()));
            args.add("--public-gateway");     args.add(safeStr(publicGateway));
            args.add("--public-cidr");        args.add(safeStr(publicCidr));
            args.add("--public-vlan");        args.add(publicVlanTag);
            args.addAll(getVpcIdArgs(network));

             boolean result = executeScript(network, action, args.toArray(new String[0]));
             if (!result) {
                 throw new ResourceUnavailableException(
                         "Failed to " + action + " for IP " + ip.getAddress().addr(),
                         Network.class, network.getId());
             }
         }
         return true;
     }

    /**
     * Build a CIDR string from IP address and dotted netmask (or prefix).
     * Returns "" if either value is null or parsing fails.
     */
    private String buildCidrFromIpAndNetmask(String ipStr, String netmaskStr) {
        if (ipStr == null || ipStr.isEmpty() || netmaskStr == null || netmaskStr.isEmpty()) {
            return "";
        }
        // If netmask is already CIDR (contains '/'), try to return network/prefix
        if (netmaskStr.contains("/")) {
            return netmaskStr;
        }
        try {
            InetAddress ip = InetAddress.getByName(ipStr);
            InetAddress mask = InetAddress.getByName(netmaskStr);
            int maskInt = ByteBuffer.wrap(mask.getAddress()).getInt();
            int prefix = Integer.bitCount(maskInt);
            // Return the provided IP with the calculated prefix so the address retains its host value
            return ip.getHostAddress() + "/" + prefix;
        } catch (Exception e) {
            logger.debug("Failed to compute CIDR from ip/netmask {} {}: {}", ipStr, netmaskStr, e.getMessage());
            return "";
        }
    }

    // ---- StaticNatServiceProvider ----

    @Override
    public boolean applyStaticNats(Network config, List<? extends StaticNat> rules)
            throws ResourceUnavailableException {
        if (rules == null || rules.isEmpty()) {
            return true;
        }
        if (!canHandle(config, Service.StaticNat)) {
            return false;
        }
        logger.info("Applying {} static NAT rules for network {}", rules.size(), config.getId());
        String vlanId = getVlanId(config);
        List<String> vpcArgs = getVpcIdArgs(config);

        for (StaticNat rule : rules) {
            String action = rule.isForRevoke() ? "delete-static-nat" : "add-static-nat";
            String publicCidr = getPublicCidr(rule.getSourceIpAddressId());
            String publicVlanTag = getPublicVlanTag(rule.getSourceIpAddressId());

            List<String> args = new ArrayList<>();
            args.add("--network-id");        args.add(String.valueOf(config.getId()));
            args.add("--vlan");              args.add(safeStr(vlanId));
            args.add("--public-ip");         args.add(getIpAddress(rule.getSourceIpAddressId()));
            args.add("--public-cidr");       args.add(safeStr(publicCidr));
            args.add("--public-vlan");       args.add(publicVlanTag);
            args.add("--private-ip");        args.add(safeStr(rule.getDestIpAddress()));
            args.addAll(vpcArgs);
            boolean result = executeScript(config, action, args.toArray(new String[0]));
            if (!result) {
                throw new ResourceUnavailableException("Failed to " + action + " for static NAT rule",
                        Network.class, config.getId());
            }
        }
        return true;
    }

    // ---- PortForwardingServiceProvider ----

    @Override
    public boolean applyPFRules(Network network, List<PortForwardingRule> rules)
            throws ResourceUnavailableException {
        if (rules == null || rules.isEmpty()) {
            return true;
        }
        if (!canHandle(network, Service.PortForwarding)) {
            return false;
        }
        logger.info("Applying {} port forwarding rules for network {}", rules.size(), network.getId());
        String vlanId = getVlanId(network);
        List<String> vpcArgs = getVpcIdArgs(network);

        for (PortForwardingRule rule : rules) {
            boolean isRevoke = rule.getState() == FirewallRule.State.Revoke;
            String action = isRevoke ? "delete-port-forward" : "add-port-forward";
            String publicPort  = PortForwardingServiceProvider.getPublicPortRange(rule);
            String privatePort = PortForwardingServiceProvider.getPrivatePFPortRange(rule);
            String publicCidr  = getPublicCidr(rule.getSourceIpAddressId());
            String publicVlanTag = getPublicVlanTag(rule.getSourceIpAddressId());

            List<String> args = new ArrayList<>();
            args.add("--network-id");        args.add(String.valueOf(network.getId()));
            args.add("--vlan");              args.add(safeStr(vlanId));
            args.add("--public-ip");         args.add(getIpAddress(rule.getSourceIpAddressId()));
            args.add("--public-cidr");       args.add(safeStr(publicCidr));
            args.add("--public-vlan");       args.add(publicVlanTag);
            args.add("--public-port");       args.add(safeStr(publicPort));
            args.add("--private-ip");        args.add(safeStr(rule.getDestinationIpAddress() != null
                    ? rule.getDestinationIpAddress().addr() : null));
            args.add("--private-port");      args.add(safeStr(privatePort));
            args.add("--protocol");          args.add(safeStr(rule.getProtocol()));
            args.addAll(vpcArgs);
            boolean result = executeScript(network, action, args.toArray(new String[0]));
            if (!result) {
                throw new ResourceUnavailableException("Failed to " + action + " for port forwarding rule",
                        Network.class, network.getId());
            }
        }
        return true;
    }

    // ---- Script execution ----

    /**
     * Executes the network-extension.sh script with the given command and arguments.
     *
     * <p>Two JSON blobs are always appended as named CLI arguments:</p>
     * <ul>
     *   <li>{@value #ARG_PHYSICAL_NETWORK_EXTENSION_DETAILS} {@code <json>} – all
     *       {@code extension_resource_map_details} for this extension on the physical
     *       network.  Sensitive keys (password, sshkey) are included but redacted in
     *       log output.</li>
     *   <li>{@value #ARG_NETWORK_EXTENSION_DETAILS} {@code <json>} – the per-network
     *       JSON blob from {@code network_details} ({@code {}} if not yet set).</li>
     * </ul>
     */
    protected boolean executeScript(Network network, String command, String... args) {
        Extension extension = resolveExtension(network);
        File scriptFile = resolveScriptFile(network, extension);

        ensureExtensionDetails(network);

        String physicalNetworkDetailsJson = buildPhysicalNetworkDetailsJson(network.getPhysicalNetworkId(), extension);
        String networkExtensionDetailsJson = getNetworkExtensionDetailsJson(network);

        // Log the JSON blobs so we can diagnose missing-argument issues in runtime logs
        logger.debug("Physical network details JSON: {}", physicalNetworkDetailsJson);
        logger.debug("Network extension details JSON: {}", networkExtensionDetailsJson);

        List<String> cmdLine = new ArrayList<>();
        cmdLine.add(scriptFile.getAbsolutePath());
        cmdLine.add(command);
        cmdLine.addAll(Arrays.asList(args));
        cmdLine.add(ARG_PHYSICAL_NETWORK_EXTENSION_DETAILS);
        cmdLine.add(physicalNetworkDetailsJson);
        cmdLine.add(ARG_NETWORK_EXTENSION_DETAILS);
        cmdLine.add(networkExtensionDetailsJson);

        logger.debug("Executing network extension script: {}", String.join(" ", cmdLine));

        try {
            ProcessBuilder pb = new ProcessBuilder(cmdLine);
            pb.redirectErrorStream(true);
            Process process = pb.start();
            byte[] output = process.getInputStream().readAllBytes();
            int exitCode = process.waitFor();

            String outputStr = new String(output).trim();
            if (!outputStr.isEmpty()) {
                logger.debug("Script output: {}", outputStr);
            }
            if (exitCode != 0) {
                logger.error("Network extension script failed with exit code {}: {}", exitCode, outputStr);
                return false;
            }
            return true;
        } catch (Exception e) {
            logger.error("Failed to execute network extension script: {}", e.getMessage(), e);
            throw new CloudRuntimeException("Failed to execute network extension script", e);
        }
    }

    /**
     * Writes a potentially large payload to a temporary file and passes the file path
     * to the extension script via {@code payloadArgName}. This avoids argv size limits
     * for multi-MB payloads.
     */
    protected boolean executeScriptWithFilePayload(Network network, String command,
            String payloadArgName, String payload, String... args) {
        File payloadFile = null;
        try {
            payloadFile = File.createTempFile("cs-extnet-" + command + "-", ".payload");
            Files.writeString(payloadFile.toPath(), payload != null ? payload : "", StandardCharsets.UTF_8);

            List<String> cmdArgs = new ArrayList<>();
            cmdArgs.addAll(Arrays.asList(args));
            cmdArgs.add(payloadArgName);
            cmdArgs.add(payloadFile.getAbsolutePath());

            return executeScript(network, command, cmdArgs.toArray(new String[0]));
        } catch (Exception e) {
            throw new CloudRuntimeException(
                    String.format("Failed preparing payload file for command %s", command), e);
        } finally {
            if (payloadFile != null && payloadFile.exists() && !payloadFile.delete()) {
                payloadFile.deleteOnExit();
            }
        }
    }

    // ---- Detail helpers ----

    /**
     * Returns all {@code extension_resource_map_details} for the given extension
     * on the physical network as a plain map, enriched with physical-network
     * metadata (name, kvmnetworklabel, vmwarenetworklabel, xennetworklabel,
     * public_kvmnetworklabel) so the wrapper script can derive bridge names and
     * interface names without extra lookups.
     */
    private Map<String, String> buildPhysicalNetworkDetailsMap(Long physicalNetworkId, Extension extension) {
        Map<String, String> details = new HashMap<>();
        if (physicalNetworkId == null || extension == null) {
            return details;
        }
        // Start with registered extension_resource_map_details
        Map<String, String> mapDetails = extensionHelper.getAllResourceMapDetailsForExtensionOnPhysicalNetwork(
                physicalNetworkId, extension.getId());
        if (mapDetails != null) {
            details.putAll(mapDetails);
        }

        // Enrich with physical-network record fields
        PhysicalNetworkVO pn = physicalNetworkDao.findById(physicalNetworkId);
        if (pn != null && pn.getName() != null) {
            details.put("physicalnetworkname", pn.getName());
        }

        return details;
    }


    /**
     * Returns {@code ["--vpc-id", "<vpcId>"]} when the network belongs to a VPC,
     * or an empty list otherwise.  Appended to every script invocation so the
     * wrapper script can derive the correct namespace (cs-net-&lt;vpcId&gt;).
     */
    private List<String> getVpcIdArgs(Network network) {
        if (network.getVpcId() != null) {
            return List.of("--vpc-id", String.valueOf(network.getVpcId()));
        }
        return List.of();
    }

    /**
     * Serialises the physical-network extension details to a compact JSON object string.
     */
    private String buildPhysicalNetworkDetailsJson(Long physicalNetworkId, Extension extension) {
        return mapToJson(buildPhysicalNetworkDetailsMap(physicalNetworkId, extension));
    }

    /**
     * Reads the per-network JSON blob from {@code network_details}
     * (returns {@code {}} if not yet set).
     */
    private String getNetworkExtensionDetailsJson(Network network) {
        if (network.getVpcId() != null) {
            return getVpcExtensionDetailsJson(network.getVpcId());
        } else {
            Map<String, String> networkDetails = networkDetailsDao.listDetailsKeyPairs(network.getId());
            return networkDetails != null
                    ? networkDetails.getOrDefault(NETWORK_DETAIL_EXTENSION_DETAILS, "{}") : "{}";
        }
    }


    /**
     * Serialises a {@code Map<String, String>} to a compact JSON object string.
     * Returns {@code {}} for null or empty maps.
     */
    private String mapToJson(Map<String, String> map) {
        if (map == null || map.isEmpty()) {
            return "{}";
        }
        JsonObject obj = new JsonObject();
        for (Map.Entry<String, String> entry : map.entrySet()) {
            if (entry.getValue() != null) {
                obj.addProperty(entry.getKey(), entry.getValue());
            }
        }
        return new Gson().toJson(obj);
    }

    // ---- Custom action ----

    @Override
    public boolean canHandleCustomAction(Network network) {
        return canHandle(network, null);
    }

    /**
     * Runs a custom action on the external network device.
     * Per-action parameters are passed as a JSON object via
     * {@value #ARG_ACTION_PARAMS}, e.g.:
     * <pre>--action-params '{"key1":"value1","key2":"value2"}'</pre>
     * The wrapper script receives the `--action-params` JSON string and forwards
     * it unchanged to hook scripts as the `--action-params` CLI argument; hook
     * scripts should parse the JSON themselves (for example using `jq` or a
     * small shell/awk parser).
     */
    public String runCustomAction(Network network, String actionName, Map<String, Object> parameters) {
        Extension extension = resolveExtension(network);
        File scriptFile = resolveScriptFile(network, extension);

        String physicalNetworkDetailsJson = buildPhysicalNetworkDetailsJson(network.getPhysicalNetworkId(), extension);
        String networkExtensionDetailsJson = getNetworkExtensionDetailsJson(network);
        String actionParamsJson = buildActionParamsJson(parameters);

        List<String> cmdLine = new ArrayList<>();
        cmdLine.add(scriptFile.getAbsolutePath());
        cmdLine.add("custom-action");
        cmdLine.add("--network-id");
        cmdLine.add(String.valueOf(network.getId()));
        cmdLine.add("--action");
        cmdLine.add(actionName);
        cmdLine.add(ARG_ACTION_PARAMS);
        cmdLine.add(actionParamsJson);
        cmdLine.add(ARG_PHYSICAL_NETWORK_EXTENSION_DETAILS);
        cmdLine.add(physicalNetworkDetailsJson);
        cmdLine.add(ARG_NETWORK_EXTENSION_DETAILS);
        cmdLine.add(networkExtensionDetailsJson);

        logger.info("Running custom action '{}' on network {} (extension: {}, params: {} key(s))",
                actionName, network.getId(), extension != null ? extension.getName() : "unknown",
                parameters != null ? parameters.size() : 0);

        try {
            ProcessBuilder pb = new ProcessBuilder(cmdLine);
            pb.redirectErrorStream(true);
            Process process = pb.start();
            byte[] output = process.getInputStream().readAllBytes();
            int exitCode = process.waitFor();
            String outputStr = new String(output).trim();

            logger.debug("Running custom action script: {}", String.join(" ", cmdLine));

            if (exitCode != 0) {
                logger.error("Custom action '{}' failed (exit {}): {}", actionName, exitCode, outputStr);
                return null;
            }
            logger.info("Custom action '{}' completed successfully", actionName);
            return outputStr.isEmpty() ? "OK" : outputStr;
        } catch (Exception e) {
            logger.error("Failed to execute custom action '{}': {}", actionName, e.getMessage(), e);
            throw new CloudRuntimeException("Failed to execute custom action: " + actionName, e);
        }
    }

    /**
     * Serialises custom-action parameters to a compact JSON object string.
     * Returns {@code {}} for null or empty maps.
     */
    private String buildActionParamsJson(Map<String, Object> parameters) {
        if (parameters == null || parameters.isEmpty()) {
            return "{}";
        }
        JsonObject obj = new JsonObject();
        for (Map.Entry<String, Object> entry : parameters.entrySet()) {
            obj.addProperty(entry.getKey(),
                    entry.getValue() != null ? entry.getValue().toString() : "");
        }
        return new Gson().toJson(obj);
    }

    // ---- Script file resolution ----

    /**
     * Resolves the executable script file from the given extension.
     *
     * <p>Lookup order (first match wins):</p>
     * <ol>
     *   <li>{@code <extensionPath>/<extensionName>.sh} — preferred convention,
     *       e.g. for an extension named {@code network-extension} the script is
     *       {@code network-extension.sh}.</li>
     *   <li>{@code <extensionPath>} itself, if it is a file and is executable.</li>
     * </ol>
     */
    protected File resolveScriptFile(Network network, Extension extension) {
        Long physicalNetworkId = network.getPhysicalNetworkId();
        if (physicalNetworkId == null) {
            throw new CloudRuntimeException("Network " + network.getId() + " has no physical network");
        }
        if (extension == null) {
            throw new CloudRuntimeException(
                    "No NetworkOrchestrator extension found for network " + network.getId()
                    + " on physical network " + physicalNetworkId);
        }
        if (!Extension.Type.NetworkOrchestrator.equals(extension.getType())) {
            throw new CloudRuntimeException("Extension " + extension.getName() + " is not of type NetworkOrchestrator");
        }
        if (!Extension.State.Enabled.equals(extension.getState())) {
            throw new CloudRuntimeException("Extension " + extension.getName() + " is not enabled");
        }
        if (!extension.isPathReady()) {
            throw new CloudRuntimeException("Extension " + extension.getName() + " path is not ready");
        }

        String extensionPath = extensionHelper.getExtensionScriptPath(extension);
        if (extensionPath == null) {
            throw new CloudRuntimeException("Could not resolve path for extension " + extension.getName());
        }

        File extensionDir = new File(extensionPath);

        // <extensionPath>/<extensionName>.sh  (preferred convention)
        File namedScript = new File(extensionDir, extension.getName() + ".sh");
        if (namedScript.exists() && namedScript.canExecute()) {
            return namedScript;
        }
        // <extensionPath> itself is the script file
        if (extensionDir.isFile() && extensionDir.canExecute()) {
            return extensionDir;
        }

        throw new CloudRuntimeException(
                "No executable script found in extension path " + extensionPath
                + ". Expected '" + extension.getName() + ".sh' inside the extension directory.");
    }

    // ---- Helpers ----

    private String getVlanId(Network network) {
        return network.getBroadcastUri() != null
                ? Networks.BroadcastDomainType.getValue(network.getBroadcastUri()) : null;
    }

    private String getIpAddress(Long ipAddressId) {
        if (ipAddressId == null) {
            return "";
        }
        IpAddress ip = networkModel.getIp(ipAddressId);
        return ip != null ? ip.getAddress().addr() : "";
    }

    private String getPublicCidr(Long ipAddressId) {
        if (ipAddressId == null) {
            return "";
        }
        IpAddress ip = networkModel.getIp(ipAddressId);
        if (ip.getAddress() == null) {
            return "";
        }
        VlanVO vlan = vlanDao.findById(ip.getVlanId());
        return buildCidrFromIpAndNetmask(ip.getAddress().addr(), vlan.getVlanNetmask());
    }

    private String getPublicVlanTag(Long ipAddressId) {
        if (ipAddressId == null) {
            return "";
        }
        IpAddress ip = networkModel.getIp(ipAddressId);
        if (ip == null) {
            return "";
        }
        VlanVO vlan = vlanDao.findById(ip.getVlanId());
        return vlan != null ? safeStr(vlan.getVlanTag()) : "";
    }

    private String safeStr(String value) {
        return value != null ? value : "";
    }

    // ---- DhcpServiceProvider ----

    private String getNetworkDns(final Network network) {
        final DataCenter dc = dataCenterDao.findById(network.getDataCenterId());
        Pair<String, String> dnsList = networkModel.getNetworkIp4Dns(network, dc);
        return dnsList.first() + (dnsList.second() != null ? "," + dnsList.second() : "");
    }

    @Override
    public boolean addDhcpEntry(Network network, NicProfile nic, VirtualMachineProfile vm,
            DeployDestination dest, ReservationContext context)
            throws ConcurrentOperationException, InsufficientCapacityException, ResourceUnavailableException {
        if (!canHandle(network, Service.Dhcp)) {
            return false;
        }
        String extensionIp = ensureExtensionIp(network);
        logger.debug("addDhcpEntry: network={} mac={} ip={}", network.getId(),
                nic.getMacAddress(), nic.getIPv4Address());
        List<String> args = new ArrayList<>();
        args.add("--network-id");   args.add(String.valueOf(network.getId()));
        args.add("--mac");          args.add(safeStr(nic.getMacAddress()));
        args.add("--ip");           args.add(safeStr(nic.getIPv4Address()));
        args.add("--hostname");     args.add(safeStr(vm.getHostName()));
        args.add("--gateway");      args.add(safeStr(network.getGateway()));
        args.add("--cidr");         args.add(safeStr(network.getCidr()));
        args.add("--dns");          args.add(safeStr(getNetworkDns(network)));
        args.add("--default-nic");  args.add(String.valueOf(nic.isDefaultNic()));
        args.add("--domain");       args.add(safeStr(network.getNetworkDomain()));
        args.add("--extension-ip"); args.add(safeStr(extensionIp));
        args.addAll(getVpcIdArgs(network));
        return executeScript(network, "add-dhcp-entry", args.toArray(new String[0]));
    }

    @Override
    public boolean configDhcpSupportForSubnet(Network network, NicProfile nic, VirtualMachineProfile vm,
            DeployDestination dest, ReservationContext context)
            throws ConcurrentOperationException, InsufficientCapacityException, ResourceUnavailableException {
        if (!canHandle(network, Service.Dhcp)) {
            return false;
        }
        logger.debug("configDhcpSupportForSubnet: network={}", network.getId());
        String extensionIp = ensureExtensionIp(network);
        List<String> args = new ArrayList<>();
        args.add("--network-id");   args.add(String.valueOf(network.getId()));
        args.add("--gateway");      args.add(safeStr(network.getGateway()));
        args.add("--cidr");         args.add(safeStr(network.getCidr()));
        args.add("--dns");          args.add(safeStr(getNetworkDns(network)));
        args.add("--vlan");         args.add(safeStr(getVlanId(network)));
        args.add("--domain");       args.add(safeStr(network.getNetworkDomain()));
        args.add("--extension-ip"); args.add(safeStr(extensionIp));
        args.addAll(getVpcIdArgs(network));
        return executeScript(network, "config-dhcp-subnet", args.toArray(new String[0]));
    }

    @Override
    public boolean removeDhcpSupportForSubnet(Network network) throws ResourceUnavailableException {
        if (!canHandle(network, Service.Dhcp)) {
            return false;
        }
        logger.debug("removeDhcpSupportForSubnet: network={}", network.getId());
        String extensionIp = ensureExtensionIp(network);
        List<String> args = new ArrayList<>();
        args.add("--network-id");   args.add(String.valueOf(network.getId()));
        args.add("--extension-ip"); args.add(safeStr(extensionIp));
        args.addAll(getVpcIdArgs(network));
        return executeScript(network, "remove-dhcp-subnet", args.toArray(new String[0]));
    }

    @Override
    public boolean setExtraDhcpOptions(Network network, long nicId, Map<Integer, String> dhcpOptions) {
        if (!canHandle(network, Service.Dhcp)) {
            return false;
        }
        if (dhcpOptions == null || dhcpOptions.isEmpty()) {
            return true;
        }
        logger.debug("setExtraDhcpOptions: network={} nicId={} options={}", network.getId(), nicId, dhcpOptions.size());
        // Serialise options as a compact JSON object: {"<code>":"<value>", ...}
        StringBuilder json = new StringBuilder("{");
        boolean first = true;
        for (Map.Entry<Integer, String> e : dhcpOptions.entrySet()) {
            if (!first) json.append(",");
            json.append("\"").append(e.getKey()).append("\":\"")
                .append(e.getValue() != null ? e.getValue().replace("\"", "\\\"") : "")
                .append("\"");
            first = false;
        }
        json.append("}");
        String extensionIp = ensureExtensionIp(network);
        List<String> args = new ArrayList<>();
        args.add("--network-id");   args.add(String.valueOf(network.getId()));
        args.add("--nic-id");       args.add(String.valueOf(nicId));
        args.add("--options");      args.add(json.toString());
        args.add("--extension-ip"); args.add(safeStr(extensionIp));
        args.addAll(getVpcIdArgs(network));
        try {
            return executeScript(network, "set-dhcp-options", args.toArray(new String[0]));
        } catch (Exception e) {
            logger.warn("setExtraDhcpOptions failed for network {}: {}", network.getId(), e.getMessage());
            return false;
        }
    }

    @Override
    public boolean removeDhcpEntry(Network network, NicProfile nic, VirtualMachineProfile vmProfile)
            throws ResourceUnavailableException {
        if (!canHandle(network, Service.Dhcp)) {
            return false;
        }
        logger.debug("removeDhcpEntry: network={} mac={} ip={}", network.getId(),
                nic.getMacAddress(), nic.getIPv4Address());
        String extensionIp = ensureExtensionIp(network);
        List<String> args = new ArrayList<>();
        args.add("--network-id");   args.add(String.valueOf(network.getId()));
        args.add("--mac");          args.add(safeStr(nic.getMacAddress()));
        args.add("--ip");           args.add(safeStr(nic.getIPv4Address()));
        args.add("--extension-ip"); args.add(safeStr(extensionIp));
        args.addAll(getVpcIdArgs(network));
        return executeScript(network, "remove-dhcp-entry", args.toArray(new String[0]));
    }

    // ---- DnsServiceProvider ----

    @Override
    public boolean addDnsEntry(Network network, NicProfile nic, VirtualMachineProfile vm,
            DeployDestination dest, ReservationContext context)
            throws ConcurrentOperationException, InsufficientCapacityException, ResourceUnavailableException {
        if (!canHandle(network, Service.Dns)) {
            return false;
        }
        String hostname = vm.getHostName();
        logger.debug("addDnsEntry: network={} hostname={} ip={}", network.getId(),
                hostname, nic.getIPv4Address());
        String extensionIp = ensureExtensionIp(network);
        List<String> args = new ArrayList<>();
        args.add("--network-id");   args.add(String.valueOf(network.getId()));
        args.add("--ip");           args.add(safeStr(nic.getIPv4Address()));
        args.add("--hostname");     args.add(safeStr(hostname));
        args.add("--extension-ip"); args.add(safeStr(extensionIp));
        args.addAll(getVpcIdArgs(network));
        return executeScript(network, "add-dns-entry", args.toArray(new String[0]));
    }

    @Override
    public boolean configDnsSupportForSubnet(Network network, NicProfile nic, VirtualMachineProfile vm,
            DeployDestination dest, ReservationContext context)
            throws ConcurrentOperationException, InsufficientCapacityException, ResourceUnavailableException {
        if (!canHandle(network, Service.Dns)) {
            return false;
        }
        logger.debug("configDnsSupportForSubnet: network={}", network.getId());
        String extensionIp = ensureExtensionIp(network);
        List<String> args = new ArrayList<>();
        args.add("--network-id");   args.add(String.valueOf(network.getId()));
        args.add("--gateway");      args.add(safeStr(network.getGateway()));
        args.add("--cidr");         args.add(safeStr(network.getCidr()));
        args.add("--dns");          args.add(safeStr(getNetworkDns(network)));
        args.add("--vlan");         args.add(safeStr(getVlanId(network)));
        args.add("--domain");       args.add(safeStr(network.getNetworkDomain()));
        args.add("--extension-ip"); args.add(safeStr(extensionIp));
        args.addAll(getVpcIdArgs(network));
        return executeScript(network, "config-dns-subnet", args.toArray(new String[0]));
    }

    @Override
    public boolean removeDnsSupportForSubnet(Network network) throws ResourceUnavailableException {
        if (!canHandle(network, Service.Dns)) {
            return false;
        }
        logger.debug("removeDnsSupportForSubnet: network={}", network.getId());
        String extensionIp = ensureExtensionIp(network);
        List<String> args = new ArrayList<>();
        args.add("--network-id");   args.add(String.valueOf(network.getId()));
        args.add("--extension-ip"); args.add(safeStr(extensionIp));
        args.addAll(getVpcIdArgs(network));
        return executeScript(network, "remove-dns-subnet", args.toArray(new String[0]));
    }

    // ---- UserDataServiceProvider ----

    @Override
    public boolean addPasswordAndUserdata(Network network, NicProfile nic, VirtualMachineProfile profile,
            DeployDestination dest, ReservationContext context)
            throws ConcurrentOperationException, InsufficientCapacityException, ResourceUnavailableException {
        if (!canHandle(network, Service.UserData)) {
            return false;
        }

        VirtualMachine vm = profile.getVirtualMachine();

        // SSH public key from VM instance details
        String sshPublicKey = null;
        try {
            VMInstanceDetailVO sshKeyDetail = vmInstanceDetailsDao.findDetail(profile.getId(), VmDetailConstants.SSH_PUBLIC_KEY);
            if (sshKeyDetail != null) {
                sshPublicKey = sshKeyDetail.getValue();
            }
        } catch (Exception e) {
            logger.debug("Could not fetch SSH public key for VM {}: {}", profile.getId(), e.getMessage());
        }

        // Service offering display name
        String serviceOfferingName = "";
        try {
            serviceOfferingName = profile.getServiceOffering().getDisplayText();
        } catch (Exception e) {
            logger.debug("Could not fetch service offering for VM {}: {}", profile.getId(), e.getMessage());
        }

        // Is Windows guest?
        boolean isWindows = false;
        try {
            isWindows = guestOSCategoryDao
                    .findById(guestOSDao.findById(vm.getGuestOSId()).getCategoryId())
                    .getName().equalsIgnoreCase("Windows");
        } catch (Exception e) {
            logger.debug("Could not determine OS type for VM {}: {}", profile.getId(), e.getMessage());
        }

        // Hypervisor hostname – prefer dest host, fall back to current host
        String destHostname = null;
        try {
            if (dest != null && dest.getHost() != null) {
                destHostname = VirtualMachineManager.getHypervisorHostname(dest.getHost().getName());
            } else if (vm.getHostId() != null) {
                destHostname = VirtualMachineManager.getHypervisorHostname(
                        hostDao.findById(vm.getHostId()).getName());
            }
        } catch (Exception e) {
            logger.debug("Could not resolve hypervisor hostname for VM {}: {}", profile.getId(), e.getMessage());
        }

        // Password from the VM profile parameter (set by UserVmManager before deployment)
        String password = (String) profile.getParameter(VirtualMachineProfile.Param.VmPassword);

        // Use this NIC's IP — the metadata server in each namespace identifies requesters
        // by REMOTE_ADDR, which will be the VM's IP on THIS network (not necessarily the
        // default NIC IP), so we always key metadata by the NIC's IP on this network.
        String nicIpAddress = nic.getIPv4Address();

        logger.debug("addPasswordAndUserdata: network={} ip={} hasPassword={} hasSshKey={}",
                network.getId(), nicIpAddress,
                password != null && !password.isEmpty(),
                sshPublicKey != null && !sshPublicKey.isEmpty());

        final UserVmVO userVm = userVmDao.findById(vm.getId());
        if (userVm == null) {
            throw new CloudRuntimeException("Could not find UserVmVO for VM " + vm.getId());
        }

        // Generate the full metadata set (userdata, meta-data/*, password) in one go
        List<String[]> vmData = networkModel.generateVmData(
                userVm.getUserData(),
                userVm.getUserDataDetails(),
                serviceOfferingName,
                vm.getDataCenterId(),
                profile.getInstanceName(),
                profile.getHostName(),
                profile.getId(),
                profile.getUuid(),
                nicIpAddress,
                sshPublicKey,
                password,
                isWindows,
                destHostname);

        if (vmData == null || vmData.isEmpty()) {
            logger.debug("addPasswordAndUserdata: no VM data generated for network={} ip={}", network.getId(), nicIpAddress);
            return true;
        }

        // Serialise vmData as JSON array.
        // For the userdata entry CloudStack stores user-data base64-encoded; decode it so the
        // wrapper writes the actual bytes. All other fields are plain strings. In both cases we
        // then re-encode with Base64 so the single --vm-data argument is shell-safe.
        StringBuilder json = new StringBuilder("[");
        boolean first = true;
        for (String[] entry : vmData) {
            String dir     = entry[NetworkModel.CONFIGDATA_DIR];
            String file    = entry[NetworkModel.CONFIGDATA_FILE];
            String content = entry.length > NetworkModel.CONFIGDATA_CONTENT
                    ? entry[NetworkModel.CONFIGDATA_CONTENT] : null;
            if (content == null) content = "";

            byte[] contentBytes;
            if (NetworkModel.USERDATA_DIR.equals(dir) && NetworkModel.USERDATA_FILE.equals(file)) {
                // user-data is stored as base64 in CloudStack DB; decode it for the wrapper
                try {
                    contentBytes = Base64.getDecoder().decode(content);
                } catch (Exception e) {
                    contentBytes = content.getBytes(StandardCharsets.UTF_8);
                }
            } else {
                contentBytes = content.getBytes(StandardCharsets.UTF_8);
            }

            if (!first) json.append(",");
            first = false;
            json.append("{\"dir\":\"").append(jsonEscape(dir))
                .append("\",\"file\":\"").append(jsonEscape(file))
                .append("\",\"content\":\"")
                .append(Base64.getEncoder().encodeToString(contentBytes))
                .append("\"}");
        }
        json.append("]");

        // Wrap the entire JSON as base64 to avoid any shell quoting / escaping issues
        String vmDataArg = Base64.getEncoder().encodeToString(
                json.toString().getBytes(StandardCharsets.UTF_8));

        List<String> args = new ArrayList<>();
        args.add("--network-id");   args.add(String.valueOf(network.getId()));
        args.add("--ip");           args.add(safeStr(nicIpAddress));
        args.add("--gateway");      args.add(safeStr(nic.getIPv4Gateway()));
        args.add("--extension-ip"); args.add(safeStr(ensureExtensionIp(network)));
        args.addAll(getVpcIdArgs(network));
        return executeScriptWithFilePayload(network, "save-vm-data", "--vm-data-file",
                vmDataArg, args.toArray(new String[0]));
    }

    @Override
    public boolean savePassword(Network network, NicProfile nic, VirtualMachineProfile vm)
            throws ResourceUnavailableException {
        if (!canHandle(network, Service.UserData)) {
            return false;
        }
        String password = (String) vm.getParameter(VirtualMachineProfile.Param.VmPassword);
        if (password == null || password.isEmpty()) {
            return true;
        }
        logger.debug("savePassword: network={} ip={}", network.getId(), nic.getIPv4Address());
        String extensionIp = ensureExtensionIp(network);
        List<String> args = new ArrayList<>();
        args.add("--network-id");   args.add(String.valueOf(network.getId()));
        args.add("--ip");           args.add(safeStr(nic.getIPv4Address()));
        args.add("--gateway");      args.add(safeStr(nic.getIPv4Gateway()));
        args.add("--password");     args.add(password);
        args.add("--extension-ip"); args.add(safeStr(extensionIp));
        args.addAll(getVpcIdArgs(network));
        return executeScript(network, "save-password", args.toArray(new String[0]));
    }

    @Override
    public boolean saveUserData(Network network, NicProfile nic, VirtualMachineProfile vm)
            throws ResourceUnavailableException {
        if (!canHandle(network, Service.UserData)) {
            return false;
        }
        String userData = null;
        if (vm.getVirtualMachine() instanceof UserVm) {
            userData = ((UserVm) vm.getVirtualMachine()).getUserData();
        }
        if (userData == null || userData.isEmpty()) {
            return true;
        }
        logger.debug("saveUserData: network={} ip={}", network.getId(), nic.getIPv4Address());
        // userData is stored as base64; pass it directly so the script can decode it
        String extensionIp = ensureExtensionIp(network);
        List<String> args = new ArrayList<>();
        args.add("--network-id");   args.add(String.valueOf(network.getId()));
        args.add("--ip");           args.add(safeStr(nic.getIPv4Address()));
        args.add("--gateway");      args.add(safeStr(nic.getIPv4Gateway()));
        args.add("--userdata");     args.add(userData);
        args.add("--extension-ip"); args.add(safeStr(extensionIp));
        args.addAll(getVpcIdArgs(network));
        return executeScript(network, "save-userdata", args.toArray(new String[0]));
    }

    @Override
    public boolean saveSSHKey(Network network, NicProfile nic, VirtualMachineProfile vm,
            String sshPublicKey) throws ResourceUnavailableException {
        if (!canHandle(network, Service.UserData)) {
            return false;
        }
        if (sshPublicKey == null || sshPublicKey.isEmpty()) {
            return true;
        }
        logger.debug("saveSSHKey: network={} ip={}", network.getId(), nic.getIPv4Address());
        // Encode SSH key as base64 to safely pass via CLI
        String sshKeyBase64 = Base64.getEncoder().encodeToString(sshPublicKey.getBytes(java.nio.charset.StandardCharsets.UTF_8));
        String extensionIp = ensureExtensionIp(network);
        List<String> args = new ArrayList<>();
        args.add("--network-id");   args.add(String.valueOf(network.getId()));
        args.add("--ip");           args.add(safeStr(nic.getIPv4Address()));
        args.add("--gateway");      args.add(safeStr(nic.getIPv4Gateway()));
        args.add("--sshkey");       args.add(sshKeyBase64);
        args.add("--extension-ip"); args.add(safeStr(extensionIp));
        args.addAll(getVpcIdArgs(network));
        return executeScript(network, "save-sshkey", args.toArray(new String[0]));
    }

    @Override
    public boolean saveHypervisorHostname(NicProfile nic, Network network, VirtualMachineProfile vm,
            DeployDestination dest) throws ResourceUnavailableException {
        if (!canHandle(network, Service.UserData)) {
            return false;
        }
        String hostname = dest != null && dest.getHost() != null ? dest.getHost().getName() : null;
        if (hostname == null || hostname.isEmpty()) {
            return true;
        }
        logger.debug("saveHypervisorHostname: network={} ip={} host={}", network.getId(),
                nic.getIPv4Address(), hostname);
        String extensionIp = ensureExtensionIp(network);
        List<String> args = new ArrayList<>();
        args.add("--network-id");          args.add(String.valueOf(network.getId()));
        args.add("--ip");                  args.add(safeStr(nic.getIPv4Address()));
        args.add("--gateway");             args.add(safeStr(nic.getIPv4Gateway()));
        args.add("--hypervisor-hostname"); args.add(hostname);
        args.add("--extension-ip");        args.add(safeStr(extensionIp));
        args.addAll(getVpcIdArgs(network));
        return executeScript(network, "save-hypervisor-hostname", args.toArray(new String[0]));
    }

    // ---- LoadBalancingServiceProvider ----

    @Override
    public boolean applyLBRules(Network network, List<LoadBalancingRule> rules)
            throws ResourceUnavailableException {
        if (rules == null || rules.isEmpty()) {
            return true;
        }
        if (!canHandle(network, Service.Lb)) {
            return false;
        }
        logger.info("Applying {} LB rules for network {}", rules.size(), network.getId());
        String vlanId = getVlanId(network);
        List<String> vpcArgs = getVpcIdArgs(network);

        // Serialise all rules as a JSON array and pass as a single --lb-rules argument
        StringBuilder json = new StringBuilder("[");
        boolean firstRule = true;
        for (LoadBalancingRule rule : rules) {
            if (!firstRule) json.append(",");
            firstRule = false;
            boolean revoke = rule.getState() == FirewallRule.State.Revoke;
            json.append("{");
            json.append("\"id\":").append(rule.getId()).append(",");
            json.append("\"name\":\"").append(jsonEscape(rule.getName())).append("\",");
            json.append("\"publicIp\":\"").append(jsonEscape(rule.getSourceIp() != null ? rule.getSourceIp().addr() : "")).append("\",");
            json.append("\"publicPort\":").append(rule.getSourcePortStart()).append(",");
            json.append("\"privatePort\":").append(rule.getDefaultPortStart()).append(",");
            json.append("\"protocol\":\"").append(jsonEscape(safeStr(rule.getProtocol()))).append("\",");
            json.append("\"algorithm\":\"").append(jsonEscape(safeStr(rule.getAlgorithm()))).append("\",");
            json.append("\"revoke\":").append(revoke).append(",");
            json.append("\"backends\":[");
            if (rule.getDestinations() != null) {
                boolean firstDest = true;
                for (LoadBalancingRule.LbDestination dest : rule.getDestinations()) {
                    if (!firstDest) json.append(",");
                    firstDest = false;
                    json.append("{");
                    json.append("\"ip\":\"").append(jsonEscape(dest.getIpAddress())).append("\",");
                    json.append("\"port\":").append(dest.getDestinationPortStart()).append(",");
                    json.append("\"revoked\":").append(dest.isRevoked());
                    json.append("}");
                }
            }
            json.append("]");
            json.append("}");
        }
        json.append("]");

        List<String> args = new ArrayList<>();
        args.add("--network-id"); args.add(String.valueOf(network.getId()));
        args.add("--vlan");       args.add(safeStr(vlanId));
        args.add("--lb-rules");   args.add(json.toString());
        args.addAll(vpcArgs);
        boolean result = executeScript(network, "apply-lb-rules", args.toArray(new String[0]));
        if (!result) {
            throw new ResourceUnavailableException("Failed to apply LB rules for network " + network.getId(),
                    Network.class, network.getId());
        }
        return true;
    }

    @Override
    public boolean validateLBRule(Network network, LoadBalancingRule rule) {
        // Delegate validation to the external script; accept by default
        return true;
    }

    @Override
    public List<LoadBalancerTO> updateHealthChecks(Network network,
                                                   List<LoadBalancingRule> lbrules) {
        // Health-check state updates are not implemented via this path
        return new ArrayList<>();
    }

    @Override
    public boolean handlesOnlyRulesInTransitionState() {
        return false;
    }

    /** Escapes a string for embedding in a JSON string literal. */
    private static String jsonEscape(String s) {
        if (s == null) return "";
        return s.replace("\\", "\\\\").replace("\"", "\\\"").replace("\n", "\\n").replace("\r", "\\r");
    }

    @Override
    public IpDeployer getIpDeployer(Network network) {
        // This element itself implements IpDeployer; return this instance.
        return this;
    }

    @Override
    public boolean rollingRestartSupported() {
        return false;
    }

    /**
     * Applies all active firewall rules for a network to the external network device.
     *
     * <p>Three categories of rules are handled:</p>
     * <ol>
     *   <li><b>Egress rules</b> ({@link FirewallRule.TrafficType#Egress}) — control outbound
     *       traffic from guest VMs.  The network offering's {@code egressDefaultPolicy} flag
     *       is consulted:
     *       <ul>
     *         <li>{@code true}  (ALLOW by default) — each egress rule becomes a DROP rule;
     *             a catch-all ACCEPT is appended at the end.</li>
     *         <li>{@code false} (DENY by default) — each egress rule becomes an ACCEPT rule;
     *             a catch-all DROP is appended at the end.</li>
     *       </ul>
     *   </li>
     *   <li><b>Ingress rules</b> ({@link FirewallRule.TrafficType#Ingress}) on public IPs
     *       (static NAT, port-forwarding, LB, …) — control inbound access to a specific
     *       public IP.  The wrapper script uses {@code conntrack --ctorigdst} to match the
     *       original pre-DNAT destination, so no private-IP lookup is required and all
     *       DNAT-based services (static-NAT, port-forwarding, LB) are handled uniformly.</li>
     *   <li><b>Default egress policy</b> — always conveyed via the JSON payload so the
     *       script can enforce it even when the explicit rule list is empty.</li>
     * </ol>
     *
     * <p><b>Full-state rebuild semantics:</b>
     * {@code applyFWRules} is called with a <em>narrow</em> scope — the firewall manager
     * passes only the rules for one public IP ({@code applyIngressFirewallRules}) or only
     * the egress rules ({@code applyEgressFirewallRules}) per call.  The script, however,
     * rebuilds the entire firewall chain from scratch each time it runs.  To avoid wiping
     * the rules for other IPs on every call, this method ignores the {@code rules} parameter
     * and instead queries the database for <em>all</em> active (non-revoked, non-System)
     * {@link FirewallRule.Purpose#Firewall} rules for the network.</p>
     *
     * <p>Script command: {@code apply-fw-rules}</p>
     */
    @Override
    public boolean applyFWRules(Network network, List<? extends FirewallRule> rules)
            throws ResourceUnavailableException {
        if (!canHandle(network, Service.Firewall)) {
            return false;
        }

        // Determine default egress policy from the network offering.
        // true  = ALLOW (default permissive; explicit rules are deny-rules)
        // false = DENY  (default restrictive; explicit rules are allow-rules)
        NetworkOfferingVO offering = networkOfferingDao.findById(network.getNetworkOfferingId());
        boolean defaultEgressAllow = offering == null || offering.isEgressDefaultPolicy();

        // Load ALL active (non-revoked) firewall rules for this network from the DB.
        // applyFWRules is called in a narrow scope (only one public IP's ingress rules, or
        // only egress rules per call), but the script does a full rebuild of the firewall
        // chain.  Querying the DB ensures every call produces a complete, correct chain.
        List<FirewallRuleVO> allRules = firewallRulesDao.listByNetworkAndPurposeAndNotRevoked(
                network.getId(), FirewallRule.Purpose.Firewall);
        // Skip System-type rules — the default egress policy is already conveyed by
        // "default_egress_allow".  System rules are transient (not stored in DB), but
        // guard here anyway in case of future changes.
        allRules = allRules.stream()
                .filter(r -> !FirewallRule.FirewallRuleType.System.equals(r.getType()))
                .collect(Collectors.toList());

        for (FirewallRuleVO r : allRules) {
            firewallRulesDao.loadSourceCidrs(r);
            firewallRulesDao.loadDestinationCidrs(r);
        }

        logger.info("applyFWRules: network={} activeRules={} defaultEgressAllow={}",
                network.getId(), allRules.size(), defaultEgressAllow);

        // Build JSON payload: { "default_egress_allow": <bool>, "cidr": "...", "rules": [...] }
        StringBuilder json = new StringBuilder();
        json.append("{\"default_egress_allow\":").append(defaultEgressAllow).append(",");
        json.append("\"cidr\":\"").append(jsonEscape(safeStr(network.getCidr()))).append("\",");
        json.append("\"rules\":[");

        boolean first = true;
        for (FirewallRuleVO rule : allRules) {
            if (!first) json.append(",");
            first = false;

            boolean isEgress = FirewallRule.TrafficType.Egress.equals(rule.getTrafficType());

            json.append("{");
            json.append("\"id\":").append(rule.getId()).append(",");
            json.append("\"type\":\"").append(isEgress ? "egress" : "ingress").append("\",");
            json.append("\"protocol\":\"").append(jsonEscape(safeStr(rule.getProtocol()))).append("\",");
            if (rule.getSourcePortStart() != null) {
                json.append("\"portStart\":").append(rule.getSourcePortStart()).append(",");
            }
            if (rule.getSourcePortEnd() != null) {
                json.append("\"portEnd\":").append(rule.getSourcePortEnd()).append(",");
            }
            if (rule.getIcmpType() != null) {
                json.append("\"icmpType\":").append(rule.getIcmpType()).append(",");
            }
            if (rule.getIcmpCode() != null) {
                json.append("\"icmpCode\":").append(rule.getIcmpCode()).append(",");
            }
            // For ingress rules include the public IP the rule is associated with.
            if (!isEgress) {
                json.append("\"publicIp\":\"")
                    .append(jsonEscape(getIpAddress(rule.getSourceIpAddressId())))
                    .append("\",");
            }
            // sourceCidrs: for ingress = allowed external source IPs;
            //              for egress  = allowed VM source IP ranges
            json.append("\"sourceCidrs\":[");
            List<String> sourceCidrs = rule.getSourceCidrList();
            if (sourceCidrs != null && !sourceCidrs.isEmpty()) {
                boolean firstCidr = true;
                for (String cidr : sourceCidrs) {
                    if (!firstCidr) json.append(",");
                    firstCidr = false;
                    json.append("\"").append(jsonEscape(cidr)).append("\"");
                }
            }
            json.append("]");
            // destCidrs: optional destination CIDR filter (meaningful for egress rules)
            List<String> destCidrs = rule.getDestinationCidrList();
            json.append(",\"destCidrs\":[");
            if (destCidrs != null && !destCidrs.isEmpty()) {
                boolean firstCidr = true;
                for (String cidr : destCidrs) {
                    if (!firstCidr) json.append(",");
                    firstCidr = false;
                    json.append("\"").append(jsonEscape(cidr)).append("\"");
                }
            }
            json.append("]");
            json.append("}");
        }
        json.append("]}");

        String rulesBase64 = Base64.getEncoder().encodeToString(
                json.toString().getBytes(StandardCharsets.UTF_8));

        List<String> args = new ArrayList<>();
        args.add("--network-id");  args.add(String.valueOf(network.getId()));
        args.add("--vlan");        args.add(safeStr(getVlanId(network)));
        args.add("--gateway");     args.add(safeStr(network.getGateway()));
        args.add("--cidr");        args.add(safeStr(network.getCidr()));
        args.addAll(getVpcIdArgs(network));

        boolean result = executeScriptWithFilePayload(network, "apply-fw-rules", "--fw-rules-file",
                rulesBase64, args.toArray(new String[0]));
        if (!result) {
            throw new ResourceUnavailableException(
                    "Failed to apply firewall rules for network " + network.getId(),
                    Network.class, network.getId());
        }
        return true;
    }

    // ---- AggregatedCommandExecutor ----

    /**
     * Called at the start of a network-restart cycle (before rules are re-programmed).
     * We have nothing to "start" here — the batch restore is driven by
     * {@link #completeAggregatedExecution}.
     */
    @Override
    public boolean prepareAggregatedExecution(Network network, DeployDestination dest)
            throws ResourceUnavailableException {
        if (!canHandle(network, null)) {
            return true;
        }
        logger.debug("prepareAggregatedExecution: network={}", network.getId());
        return true;
    }

    /**
     * Called after all firewall/NAT/LB rules have been re-applied during a network restart.
     *
     * <p>Queries all active User-VM NICs on this network from the database, builds a single
     * batch JSON payload containing DHCP/DNS/metadata entries for every VM, and sends it to
     * the wrapper script as a single {@code restore-network} call.  This avoids N script
     * invocations (one per VM) and instead performs the full restore in one shot.</p>
     */
    @Override
    public boolean completeAggregatedExecution(Network network, DeployDestination dest)
            throws ResourceUnavailableException {
        if (!canHandle(network, null)) {
            return true;
        }

        logger.info("completeAggregatedExecution: restoring all VM network data for network={}", network.getId());

        boolean dhcpEnabled     = networkModel.areServicesSupportedInNetwork(network.getId(), Service.Dhcp)
                && networkModel.isProviderSupportServiceInNetwork(network.getId(), Service.Dhcp, getProvider());
        boolean dnsEnabled      = networkModel.areServicesSupportedInNetwork(network.getId(), Service.Dns)
                && networkModel.isProviderSupportServiceInNetwork(network.getId(), Service.Dns, getProvider());
        boolean userdataEnabled = networkModel.areServicesSupportedInNetwork(network.getId(), Service.UserData)
                && networkModel.isProviderSupportServiceInNetwork(network.getId(), Service.UserData, getProvider());

        if (!dhcpEnabled && !dnsEnabled && !userdataEnabled) {
            logger.debug("completeAggregatedExecution: no DHCP/DNS/UserData service for network={}, skipping", network.getId());
            return true;
        }

        // Query all active User-VM NICs on this network
        List<NicVO> nics = nicDao.listByNetworkIdAndType(network.getId(), VirtualMachine.Type.User);
        if (nics == null || nics.isEmpty()) {
            logger.debug("completeAggregatedExecution: no user VM NICs on network={}, skipping", network.getId());
            return true;
        }

        logger.info("completeAggregatedExecution: building batch restore for {} VMs on network={}",
                nics.size(), network.getId());

        String restoreDataBase64 = buildRestoreNetworkData(network, nics, dhcpEnabled, dnsEnabled, userdataEnabled);

        String extensionIp = ensureExtensionIp(network);
        List<String> args = new ArrayList<>();
        args.add("--network-id");    args.add(String.valueOf(network.getId()));
        args.add("--gateway");       args.add(safeStr(network.getGateway()));
        args.add("--cidr");          args.add(safeStr(network.getCidr()));
        args.add("--vlan");          args.add(safeStr(getVlanId(network)));
        args.add("--extension-ip");  args.add(safeStr(extensionIp));
        args.add("--dns");           args.add(safeStr(getNetworkDns(network)));
        args.add("--domain");        args.add(safeStr(network.getNetworkDomain()));
        args.addAll(getVpcIdArgs(network));

        return executeScriptWithFilePayload(network, "restore-network", "--restore-data-file",
                restoreDataBase64, args.toArray(new String[0]));
    }

    /**
     * Called in the {@code finally} block of the network-restart cycle to clean up any
     * temporary state created by {@link #prepareAggregatedExecution}.
     * Nothing to clean up here.
     */
    @Override
    public boolean cleanupAggregatedExecution(Network network, DeployDestination dest)
            throws ResourceUnavailableException {
        return true;
    }

    /**
     * Builds the base64-encoded JSON payload for {@code restore-network}.
     *
     * <p>The JSON structure is:</p>
     * <pre>
     * {
     *   "dhcp_enabled": true,
     *   "dns_enabled":  true,
     *   "userdata_enabled": true,
     *   "vms": [
     *     {
     *       "ip":          "10.0.0.10",
     *       "mac":         "02:00:00:00:00:01",
     *       "hostname":    "vm-1",
     *       "default_nic": true,
     *       "vm_data": [
     *         { "dir": "userdata", "file": "user-data", "content": "<base64>" },
     *         { "dir": "meta-data", "file": "instance-id", "content": "<base64>" },
     *         ...
     *       ]
     *     },
     *     ...
     *   ]
     * }
     * </pre>
     *
     * <p>Each {@code vm_data} entry has its {@code content} base64-encoded (the same
     * encoding used by the per-VM {@code save-vm-data} command), so the wrapper script
     * can handle both paths with the same decoder.</p>
     */
    private String buildRestoreNetworkData(Network network, List<NicVO> nics,
            boolean dhcpEnabled, boolean dnsEnabled, boolean userdataEnabled) {

        // Precompute service-offering display text keyed by offering ID to avoid repeated DB hits
        Map<Long, String> offeringNameCache = new HashMap<>();

        StringBuilder json = new StringBuilder("{");
        json.append("\"dhcp_enabled\":").append(dhcpEnabled).append(",");
        json.append("\"dns_enabled\":").append(dnsEnabled).append(",");
        json.append("\"userdata_enabled\":").append(userdataEnabled).append(",");
        json.append("\"vms\":[");

        boolean firstVm = true;
        for (NicVO nic : nics) {
            if (nic.getState() != Nic.State.Reserved && nic.getState() != Nic.State.Allocated) {
                continue;
            }
            if (nic.getIPv4Address() == null || nic.getMacAddress() == null) {
                continue;
            }

            Long instanceId = nic.getInstanceId();
            if (instanceId == null) {
                continue;
            }

            UserVmVO userVm = userVmDao.findById(instanceId);
            if (userVm == null) {
                continue;
            }

            // Per-VM data array (only if UserData service is enabled)
            List<String[]> vmData = null;
            if (userdataEnabled) {
                try {
                    // Service offering display text
                    String offeringName = offeringNameCache.computeIfAbsent(userVm.getServiceOfferingId(), id -> {
                        try {
                            ServiceOfferingVO so = serviceOfferingDao.findById(id);
                            return so != null ? so.getDisplayText() : "";
                        } catch (Exception e) {
                            return "";
                        }
                    });

                    // SSH public key
                    String sshPublicKey = null;
                    try {
                        VMInstanceDetailVO sshKeyDetail = vmInstanceDetailsDao.findDetail(instanceId, VmDetailConstants.SSH_PUBLIC_KEY);
                        if (sshKeyDetail != null) {
                            sshPublicKey = sshKeyDetail.getValue();
                        }
                    } catch (Exception e) {
                        logger.debug("Could not fetch SSH key for VM {}: {}", instanceId, e.getMessage());
                    }

                    // Is Windows?
                    boolean isWindows = false;
                    try {
                        isWindows = guestOSCategoryDao
                                .findById(guestOSDao.findById(userVm.getGuestOSId()).getCategoryId())
                                .getName().equalsIgnoreCase("Windows");
                    } catch (Exception ignored) { }

                    // Hypervisor hostname from current host
                    String destHostname = null;
                    try {
                        if (userVm.getHostId() != null) {
                            destHostname = VirtualMachineManager.getHypervisorHostname(
                                    hostDao.findById(userVm.getHostId()).getName());
                        }
                    } catch (Exception ignored) { }

                    vmData = networkModel.generateVmData(
                            userVm.getUserData(),
                            userVm.getUserDataDetails(),
                            offeringName,
                            userVm.getDataCenterId(),
                            userVm.getInstanceName(),
                            userVm.getHostName(),
                            userVm.getId(),
                            userVm.getUuid(),
                            nic.getIPv4Address(),
                            sshPublicKey,
                            null,   // password — not re-issued on restore
                            isWindows,
                            destHostname);
                } catch (Exception e) {
                    logger.warn("Could not generate vmData for VM {} on network {}: {}", instanceId, network.getId(), e.getMessage());
                }
            }

            // Build VM JSON entry
            if (!firstVm) json.append(",");
            firstVm = false;

            json.append("{");
            json.append("\"ip\":\"").append(jsonEscape(nic.getIPv4Address())).append("\",");
            json.append("\"mac\":\"").append(jsonEscape(nic.getMacAddress())).append("\",");
            json.append("\"hostname\":\"").append(jsonEscape(safeStr(userVm.getHostName()))).append("\",");
            json.append("\"default_nic\":").append(nic.isDefaultNic()).append(",");
            json.append("\"vm_data\":[");

            if (vmData != null && !vmData.isEmpty()) {
                boolean firstEntry = true;
                for (String[] entry : vmData) {
                    String dir     = entry[NetworkModel.CONFIGDATA_DIR];
                    String file    = entry[NetworkModel.CONFIGDATA_FILE];
                    String content = entry.length > NetworkModel.CONFIGDATA_CONTENT
                            ? entry[NetworkModel.CONFIGDATA_CONTENT] : null;
                    if (content == null) content = "";

                    byte[] contentBytes;
                    if (NetworkModel.USERDATA_DIR.equals(dir) && NetworkModel.USERDATA_FILE.equals(file)) {
                        try {
                            contentBytes = Base64.getDecoder().decode(content);
                        } catch (Exception e) {
                            contentBytes = content.getBytes(StandardCharsets.UTF_8);
                        }
                    } else {
                        contentBytes = content.getBytes(StandardCharsets.UTF_8);
                    }

                    if (!firstEntry) json.append(",");
                    firstEntry = false;
                    json.append("{\"dir\":\"").append(jsonEscape(dir))
                        .append("\",\"file\":\"").append(jsonEscape(file))
                        .append("\",\"content\":\"")
                        .append(Base64.getEncoder().encodeToString(contentBytes))
                        .append("\"}");
                }
            }

            json.append("]"); // vm_data
            json.append("}"); // vm object
        }

        json.append("]"); // vms
        json.append("}"); // root

        return Base64.getEncoder().encodeToString(json.toString().getBytes(StandardCharsets.UTF_8));
    }

    // ---- VpcProvider ----

    /**
     * Finds the extension + physical-network pair for the given VPC by scanning the
     * physical networks in the VPC's zone for a registered NetworkOrchestrator extension.
     * Returns {@code null} when no suitable extension is found.
     */
    protected Pair<Long, Extension> resolveExtensionForVpc(Vpc vpc) {
        List<PhysicalNetworkVO> physNetworks = physicalNetworkDao.listByZone(vpc.getZoneId());
        if (physNetworks == null || physNetworks.isEmpty()) {
            return null;
        }
        for (PhysicalNetworkVO pn : physNetworks) {
            Extension ext;
            if (providerName != null && !providerName.isBlank()) {
                ext = extensionHelper.getExtensionForPhysicalNetworkAndProvider(pn.getId(), providerName);
            } else {
                ext = extensionHelper.getExtensionForPhysicalNetwork(pn.getId());
            }
            if (ext != null) {
                return new Pair<>(pn.getId(), ext);
            }
        }
        return null;
    }

    /**
     * Resolves the script file for a VPC-level operation (no network object required).
     */
    protected File resolveScriptFileForVpc(Long physicalNetworkId, Extension extension) {
        if (physicalNetworkId == null) {
            throw new CloudRuntimeException("No physical network ID for VPC extension");
        }
        if (extension == null) {
            throw new CloudRuntimeException("No extension found for physical network " + physicalNetworkId);
        }
        if (!Extension.Type.NetworkOrchestrator.equals(extension.getType())) {
            throw new CloudRuntimeException("Extension " + extension.getName() + " is not of type NetworkOrchestrator");
        }
        if (!Extension.State.Enabled.equals(extension.getState())) {
            throw new CloudRuntimeException("Extension " + extension.getName() + " is not enabled");
        }
        if (!extension.isPathReady()) {
            throw new CloudRuntimeException("Extension " + extension.getName() + " path is not ready");
        }
        String extensionPath = extensionHelper.getExtensionScriptPath(extension);
        if (extensionPath == null) {
            throw new CloudRuntimeException("Could not resolve path for extension " + extension.getName());
        }
        File extensionDir = new File(extensionPath);
        File namedScript = new File(extensionDir, extension.getName() + ".sh");
        if (namedScript.exists() && namedScript.canExecute()) {
            return namedScript;
        }
        if (extensionDir.isFile() && extensionDir.canExecute()) {
            return extensionDir;
        }
        throw new CloudRuntimeException(
                "No executable script found in extension path " + extensionPath
                + ". Expected '" + extension.getName() + ".sh'.");
    }

    /**
     * Calls {@code ensure-network-device} with VPC-level args (no {@code --network-id}).
     * The returned JSON is persisted in {@code vpc_details} under key
     * {@value #NETWORK_DETAIL_EXTENSION_DETAILS}.  VPC tier networks then inherit
     * these details via {@link #ensureExtensionDetails(Network)}.
     */
    protected void ensureExtensionDetails(Vpc vpc) {
        Map<String, String> stored = vpcDetailsDao.listDetailsKeyPairs(vpc.getId());
        String currentDetails = stored != null
                ? stored.getOrDefault(NETWORK_DETAIL_EXTENSION_DETAILS, "{}") : "{}";

        logger.info("Ensuring extension device for VPC {} (current={})", vpc.getId(), currentDetails);

        Pair<Long, Extension> physNetAndExt = resolveExtensionForVpc(vpc);
        if (physNetAndExt == null) {
            logger.warn("ensureExtensionDetails(vpc): no extension found for VPC {} zone {}",
                    vpc.getId(), vpc.getZoneId());
            return;
        }
        Long physicalNetworkId = physNetAndExt.first();
        Extension extension = physNetAndExt.second();
        File scriptFile = resolveScriptFileForVpc(physicalNetworkId, extension);
        String physicalNetworkDetailsJson = buildPhysicalNetworkDetailsJson(physicalNetworkId, extension);

        List<String> cmdLine = new ArrayList<>();
        cmdLine.add(scriptFile.getAbsolutePath());
        cmdLine.add("ensure-network-device");
        cmdLine.add("--vpc-id");
        cmdLine.add(String.valueOf(vpc.getId()));
        cmdLine.add("--zone-id");
        cmdLine.add(String.valueOf(vpc.getZoneId()));
        cmdLine.add("--current-details");
        cmdLine.add(currentDetails);
        cmdLine.add(ARG_PHYSICAL_NETWORK_EXTENSION_DETAILS);
        cmdLine.add(physicalNetworkDetailsJson);
        cmdLine.add(ARG_NETWORK_EXTENSION_DETAILS);
        cmdLine.add(currentDetails);

        try {
            ProcessBuilder pb = new ProcessBuilder(cmdLine);
            pb.redirectErrorStream(true);
            Process process = pb.start();
            String output = new String(process.getInputStream().readAllBytes()).trim();
            int exitCode = process.waitFor();

            logger.debug("Ensuring VPC network device script: {}", String.join(" ", cmdLine));

            if (exitCode != 0) {
                logger.warn("ensure-network-device exited {} for VPC {} — keeping current details",
                        exitCode, vpc.getId());
                if ("{}".equals(currentDetails)) {
                    vpcDetailsDao.addDetail(vpc.getId(), NETWORK_DETAIL_EXTENSION_DETAILS, "{}", false);
                }
                return;
            }
            if (output.isEmpty()) {
                output = "{}".equals(currentDetails) ? "{}" : currentDetails;
            }
            if (!output.equals(currentDetails)) {
                logger.info("VPC extension device updated for VPC {}: {}", vpc.getId(), output);
                vpcDetailsDao.addDetail(vpc.getId(), NETWORK_DETAIL_EXTENSION_DETAILS, output, false);
            } else {
                logger.debug("VPC extension device unchanged for VPC {}: {}", vpc.getId(), output);
            }
        } catch (Exception e) {
            logger.warn("Failed ensure-network-device for VPC {}: {}", vpc.getId(), e.getMessage());
            if ("{}".equals(currentDetails)) {
                vpcDetailsDao.addDetail(vpc.getId(), NETWORK_DETAIL_EXTENSION_DETAILS, "{}", false);
            }
        }
    }

    /**
     * Returns the per-VPC extension-details JSON from {@code vpc_details}
     * (returns {@code {}} if not yet set).
     */
    private String getVpcExtensionDetailsJson(long vpcId) {
        Map<String, String> vpcDetails = vpcDetailsDao.listDetailsKeyPairs(vpcId);
        return vpcDetails != null
                ? vpcDetails.getOrDefault(NETWORK_DETAIL_EXTENSION_DETAILS, "{}") : "{}";
    }

    /**
     * Executes the extension script for a VPC-level command (no tier network required).
     * Uses VPC-level details from {@code vpc_details}.
     */
    protected boolean executeVpcScript(Vpc vpc, String command, String... args) {
        Pair<Long, Extension> physNetAndExt = resolveExtensionForVpc(vpc);
        if (physNetAndExt == null) {
            logger.warn("executeVpcScript: no extension found for VPC {} zone {}", vpc.getId(), vpc.getZoneId());
            return false;
        }
        Long physicalNetworkId = physNetAndExt.first();
        Extension extension = physNetAndExt.second();
        File scriptFile = resolveScriptFileForVpc(physicalNetworkId, extension);

        String physicalNetworkDetailsJson = buildPhysicalNetworkDetailsJson(physicalNetworkId, extension);
        String vpcExtDetailsJson = getVpcExtensionDetailsJson(vpc.getId());

        logger.debug("Physical network details JSON: {}", physicalNetworkDetailsJson);
        logger.debug("VPC extension details JSON: {}", vpcExtDetailsJson);

        List<String> cmdLine = new ArrayList<>();
        cmdLine.add(scriptFile.getAbsolutePath());
        cmdLine.add(command);
        cmdLine.addAll(Arrays.asList(args));
        cmdLine.add(ARG_PHYSICAL_NETWORK_EXTENSION_DETAILS);
        cmdLine.add(physicalNetworkDetailsJson);
        cmdLine.add(ARG_NETWORK_EXTENSION_DETAILS);
        cmdLine.add(vpcExtDetailsJson);

        logger.debug("Executing VPC extension script: {}", String.join(" ", cmdLine));

        try {
            ProcessBuilder pb = new ProcessBuilder(cmdLine);
            pb.redirectErrorStream(true);
            Process process = pb.start();
            byte[] output = process.getInputStream().readAllBytes();
            int exitCode = process.waitFor();

            String outputStr = new String(output).trim();
            if (!outputStr.isEmpty()) {
                logger.debug("Script output: {}", outputStr);
            }
            if (exitCode != 0) {
                logger.error("VPC extension script {} failed with exit code {}: {}", command, exitCode, outputStr);
                return false;
            }
            return true;
        } catch (Exception e) {
            logger.error("Failed to execute VPC extension script {}: {}", command, e.getMessage(), e);
            throw new CloudRuntimeException("Failed to execute VPC extension script: " + command, e);
        }
    }

    protected PublicIpAddress getVpcSourceNatIp(long vpcId) {
        final List<IPAddressVO> ips = ipAddressDao.listByAssociatedVpc(vpcId, true);
        if (ips == null || ips.isEmpty()) {
            return null;
        }
        IPAddressVO selected = null;
        for (final IPAddressVO ip : ips) {
            if (ip.getState() != IpAddress.State.Releasing) {
                selected = ip;
                break;
            }
        }
        if (selected == null) {
            selected = ips.get(0);
        }

        final VlanVO vlan = vlanDao.findById(selected.getVlanId());
        if (vlan == null) {
            logger.warn("No VLAN found for VPC source NAT IP {} (vpc={})", selected.getAddress(), vpcId);
            return null;
        }
        return PublicIp.createFromAddrAndVlan(selected, vlan);
    }

    /**
     * Implements the VPC by:
     * <ol>
     *   <li>Calling {@link #ensureExtensionDetails(Vpc)} to select a host and
     *       save the VPC-level details (does not use any anchor tier network).</li>
     *   <li>Calling the script's {@code implement-vpc} command to create the VPC
     *       namespace and VPC-level networking state.</li>
     *   <li>Applying VPC source NAT if a source-NAT IP already exists (the script's
     *       {@code assign-ip} sets up the public veth + SNAT rule for the VPC CIDR).</li>
     * </ol>
     */
    @Override
    public boolean implementVpc(Vpc vpc, DeployDestination dest, ReservationContext context)
            throws ConcurrentOperationException, ResourceUnavailableException, InsufficientCapacityException {

        // Step 1: Ensure a VPC extension device is selected and details saved at VPC level.
        ensureExtensionDetails(vpc);

        // Step 2: Create the VPC namespace (no anchor tier network needed).
        List<String> implArgs = new ArrayList<>();
        implArgs.add("--vpc-id");  implArgs.add(String.valueOf(vpc.getId()));
        implArgs.add("--cidr");    implArgs.add(safeStr(vpc.getCidr()));

        // Include source NAT IP if already allocated, so the script can set up the
        // VPC-level SNAT rule for the entire VPC CIDR.
        final PublicIpAddress sourceNatIp = getVpcSourceNatIp(vpc.getId());
        if (sourceNatIp != null) {
            implArgs.add("--public-ip");      implArgs.add(safeStr(sourceNatIp.getAddress().addr()));
            implArgs.add("--public-vlan");    implArgs.add(safeStr(getPublicVlanTag(sourceNatIp.getId())));
            implArgs.add("--public-gateway"); implArgs.add(safeStr(sourceNatIp.getGateway()));
            implArgs.add("--public-cidr");    implArgs.add(safeStr(getPublicCidr(sourceNatIp.getId())));
            implArgs.add("--source-nat");     implArgs.add("true");
        }

        if (!executeVpcScript(vpc, "implement-vpc", implArgs.toArray(new String[0]))) {
            return false;
        }

        return true;
    }

    /**
     * Shuts down the VPC by:
     * <ol>
     *   <li>Calling {@code destroy-network} for each extension-backed VPC tier (removes
     *       tier resources but preserves the shared VPC namespace).</li>
     *   <li>Calling {@code shutdown-vpc} to remove the VPC namespace and state after
     *       all tiers have been cleaned up.</li>
     * </ol>
     */
    @Override
    public boolean shutdownVpc(Vpc vpc, ReservationContext context)
            throws ConcurrentOperationException, ResourceUnavailableException {
        final List<? extends Network> networks = networkModel.listNetworksByVpc(vpc.getId());

        boolean result = true;
        if (networks != null) {
            for (final Network network : networks) {
                if (!canHandle(network, null)) {
                    continue;
                }

                final List<String> args = new ArrayList<>();
                args.add("--network-id"); args.add(String.valueOf(network.getId()));
                args.add("--vlan");       args.add(safeStr(getVlanId(network)));
                args.addAll(getVpcIdArgs(network));

                final boolean tierResult = executeScript(network, "destroy-network", args.toArray(new String[0]));
                result = result && tierResult;
            }
        }

        // Remove the VPC namespace and VPC-level details regardless of tier result.
        List<String> vpcArgs = new ArrayList<>();
        vpcArgs.add("--vpc-id"); vpcArgs.add(String.valueOf(vpc.getId()));
        boolean vpcResult = executeVpcScript(vpc, "shutdown-vpc", vpcArgs.toArray(new String[0]));
        if (vpcResult) {
            try {
                vpcDetailsDao.removeDetail(vpc.getId(), NETWORK_DETAIL_EXTENSION_DETAILS);
            } catch (Exception e) {
                logger.warn("Failed to remove VPC extension details for VPC {}: {}", vpc.getId(), e.getMessage());
            }
        }
        result = result && vpcResult;

        return result;
    }

    @Override
    public boolean createPrivateGateway(PrivateGateway gateway)
            throws ConcurrentOperationException, ResourceUnavailableException {
        throw new UnsupportedOperationException("Private gateways are not supported by the network extension element.");
    }

    /** Private gateways are not supported by the network extension element. */
    @Override
    public boolean deletePrivateGateway(PrivateGateway gateway)
            throws ConcurrentOperationException, ResourceUnavailableException {
        throw new UnsupportedOperationException("Private gateways are not supported by the network extension element.");
    }

    /** Static routes are not supported by the network extension element. */
    @Override
    public boolean applyStaticRoutes(Vpc vpc, List<StaticRouteProfile> routes)
            throws ResourceUnavailableException {
        throw new UnsupportedOperationException("Static routes are not supported by the network extension element.");
    }

    /** ACL items on private gateways are not supported by the network extension element. */
    @Override
    public boolean applyACLItemsToPrivateGw(PrivateGateway gateway, List<? extends NetworkACLItem> rules)
            throws ResourceUnavailableException {
        throw new UnsupportedOperationException("ACL items on private gateways are not supported by the network extension element.");
    }

    @Override
    public boolean updateVpcSourceNatIp(Vpc vpc, IpAddress address) {
        if (vpc == null || address == null || address.getAddress() == null) {
            logger.warn("updateVpcSourceNatIp: invalid input (vpc={}, address={})", vpc, address);
            return false;
        }

        final List<String> args = new ArrayList<>();
        final VlanVO vlan = vlanDao.findById(address.getVlanId());
        args.add("--vpc-id");         args.add(String.valueOf(vpc.getId()));
        args.add("--cidr");           args.add(safeStr(vpc.getCidr()));
        args.add("--public-ip");      args.add(safeStr(address.getAddress().addr()));
        args.add("--public-vlan");    args.add(safeStr(getPublicVlanTag(address.getId())));
        args.add("--public-gateway"); args.add(vlan != null ? safeStr(vlan.getVlanGateway()) : "");
        args.add("--public-cidr");    args.add(safeStr(getPublicCidr(address.getId())));
        args.add("--source-nat");     args.add("true");

        final boolean result = executeVpcScript(vpc, "update-vpc-source-nat-ip", args.toArray(new String[0]));
        if (!result) {
            logger.warn("updateVpcSourceNatIp: failed to update source NAT IP for VPC {} to {}",
                    vpc.getId(), address.getAddress().addr());
        }
        return result;
    }

    /**
     * Applies VPC network ACL rules for a VPC tier network via the script's
     * {@code apply-network-acl} command.  Rules are serialised as a Base64-encoded
     * JSON array and passed via a temporary payload file.
     *
     * <p>Script command: {@code apply-network-acl}</p>
     */
    @Override
    public boolean applyNetworkACLs(Network config, List<? extends NetworkACLItem> rules)
            throws ResourceUnavailableException {
        if (!canHandle(config, Service.NetworkACL)) {
            return true;
        }

        // Rebuild the ACL chain from all non-revoked rules.
        List<? extends NetworkACLItem> activeRules = rules == null ? List.of() :
                rules.stream()
                     .filter(r -> r.getState() != NetworkACLItem.State.Revoke)
                     .collect(Collectors.toList());

        logger.info("applyNetworkACLs: network={} activeRules={}", config.getId(), activeRules.size());

        String aclRulesBase64 = buildAclRulesBase64(activeRules);

        List<String> args = new ArrayList<>();
        args.add("--network-id"); args.add(String.valueOf(config.getId()));
        args.add("--vlan");       args.add(safeStr(getVlanId(config)));
        args.add("--gateway");    args.add(safeStr(config.getGateway()));
        args.add("--cidr");       args.add(safeStr(config.getCidr()));
        args.addAll(getVpcIdArgs(config));

        boolean result = executeScriptWithFilePayload(config, "apply-network-acl",
                "--acl-rules-file", aclRulesBase64, args.toArray(new String[0]));
        if (!result) {
            throw new ResourceUnavailableException(
                    "Failed to apply network ACL rules for network " + config.getId(),
                    Network.class, config.getId());
        }
        return true;
    }

    /**
     * Re-applies ACL rules for all extension-backed networks in a VPC after a rule reorder.
     * Calls {@code apply-network-acl} for each affected network with the full ACL item list.
     */
    @Override
    public boolean reorderAclRules(Vpc vpc, List<? extends Network> networks,
            List<? extends NetworkACLItem> networkACLItems) {
        if (networks == null || networks.isEmpty()) {
            return true;
        }

        List<? extends NetworkACLItem> activeRules = networkACLItems == null ? List.of() :
                networkACLItems.stream()
                               .filter(r -> r.getState() != NetworkACLItem.State.Revoke)
                               .collect(Collectors.toList());

        boolean result = true;
        for (Network network : networks) {
            if (!canHandle(network, Service.NetworkACL)) {
                continue;
            }
            try {
                String aclRulesBase64 = buildAclRulesBase64(activeRules);

                List<String> args = new ArrayList<>();
                args.add("--network-id"); args.add(String.valueOf(network.getId()));
                args.add("--vlan");       args.add(safeStr(getVlanId(network)));
                args.add("--gateway");    args.add(safeStr(network.getGateway()));
                args.add("--cidr");       args.add(safeStr(network.getCidr()));
                args.addAll(getVpcIdArgs(network));

                boolean r = executeScriptWithFilePayload(network, "apply-network-acl",
                        "--acl-rules-file", aclRulesBase64, args.toArray(new String[0]));
                result = result && r;
            } catch (Exception e) {
                logger.warn("reorderAclRules: failed for network {}: {}", network.getId(), e.getMessage());
                result = false;
            }
        }
        return result;
    }

    /**
     * Serialises a list of {@link NetworkACLItem}s to a Base64-encoded JSON array
     * suitable for passing to the {@code apply-network-acl} script command.
     * Rules are sorted by their number (priority order).
     */
    private String buildAclRulesBase64(List<? extends NetworkACLItem> rules) {
        StringBuilder json = new StringBuilder("[");
        boolean first = true;
        List<? extends NetworkACLItem> sorted = rules.stream()
                .sorted(java.util.Comparator.comparingInt(NetworkACLItem::getNumber))
                .collect(Collectors.toList());
        for (NetworkACLItem rule : sorted) {
            if (!first) json.append(",");
            first = false;
            json.append("{");
            json.append("\"number\":").append(rule.getNumber()).append(",");
            json.append("\"action\":\"").append(rule.getAction().name().toLowerCase()).append("\",");
            json.append("\"trafficType\":\"").append(rule.getTrafficType().name().toLowerCase()).append("\",");
            json.append("\"protocol\":\"").append(jsonEscape(safeStr(rule.getProtocol()))).append("\"");
            if (rule.getSourcePortStart() != null) {
                json.append(",\"portStart\":").append(rule.getSourcePortStart());
            }
            if (rule.getSourcePortEnd() != null) {
                json.append(",\"portEnd\":").append(rule.getSourcePortEnd());
            }
            if (rule.getIcmpType() != null) {
                json.append(",\"icmpType\":").append(rule.getIcmpType());
            }
            if (rule.getIcmpCode() != null) {
                json.append(",\"icmpCode\":").append(rule.getIcmpCode());
            }
            json.append(",\"sourceCidrs\":[");
            List<String> sourceCidrs = rule.getSourceCidrList();
            if (sourceCidrs != null && !sourceCidrs.isEmpty()) {
                boolean firstCidr = true;
                for (String cidr : sourceCidrs) {
                    if (!firstCidr) json.append(",");
                    firstCidr = false;
                    json.append("\"").append(jsonEscape(cidr)).append("\"");
                }
            }
            json.append("]");
            json.append("}");
        }
        json.append("]");
        return Base64.getEncoder().encodeToString(
                json.toString().getBytes(StandardCharsets.UTF_8));
    }
}
