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
import java.net.URI;
import java.nio.file.Files;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;

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
import com.cloud.network.IpAddressManager;
import com.cloud.network.IpAddress;
import com.cloud.network.Network;
import com.cloud.network.Network.Capability;
import com.cloud.network.Network.Provider;
import com.cloud.network.Network.Service;
import com.cloud.network.NetworkModel;
import com.cloud.network.Networks;
import com.cloud.network.dao.NetworkDetailVO;
import com.cloud.network.dao.NetworkDao;
import com.cloud.network.dao.NetworkVO;
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
import com.cloud.utils.net.NetUtils;
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
import com.google.gson.JsonArray;
import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import com.google.gson.JsonParser;

import org.apache.cloudstack.api.ApiConstants;
import org.apache.cloudstack.engine.orchestration.service.NetworkOrchestrationService;
import org.apache.cloudstack.extension.Extension;
import org.apache.cloudstack.extension.ExtensionHelper;
import org.apache.cloudstack.extension.NetworkCustomActionProvider;
import org.apache.cloudstack.framework.extensions.dao.ExtensionDetailsDao;
import org.apache.cloudstack.resourcedetail.dao.VpcDetailsDao;
import org.apache.commons.collections.CollectionUtils;
import org.apache.commons.collections.MapUtils;
import org.apache.commons.lang3.EnumUtils;
import org.apache.commons.lang3.StringUtils;

import java.nio.charset.StandardCharsets;
import java.util.Base64;
import java.util.stream.Collectors;


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
    private ExtensionDetailsDao extensionDetailsDao;
    @Inject
    private NetworkDao networkDao;
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

    public static final String ARG_PHYSICAL_NETWORK_EXTENSION_DETAILS = "physical-network-extension-details";
    public static final String ARG_NETWORK_EXTENSION_DETAILS = "network-extension-details";
    public static final String ARG_PAYLOAD = "payload";
    public static final String ARG_ACTION_PARAMS = "action-params";

    public static final int DEFAULT_SCRIPT_TIMEOUT_SECONDS = 60;

    public static final int EXIT_CODE_SUCCESS = 0;
    public static final int EXIT_CODE_FAILURE = -1;

    // ---- Script command names ----

    public static final String CMD_IMPLEMENT_NETWORK = "implement-network";
    public static final String CMD_SHUTDOWN_NETWORK = "shutdown-network";
    public static final String CMD_DESTROY_NETWORK = "destroy-network";
    public static final String CMD_ENSURE_NETWORK_DEVICE = "ensure-network-device";
    public static final String CMD_ASSIGN_IP = "assign-ip";
    public static final String CMD_RELEASE_IP = "release-ip";
    public static final String CMD_ADD_STATIC_NAT = "add-static-nat";
    public static final String CMD_DELETE_STATIC_NAT = "delete-static-nat";
    public static final String CMD_ADD_PORT_FORWARD = "add-port-forward";
    public static final String CMD_DELETE_PORT_FORWARD = "delete-port-forward";
    public static final String CMD_ADD_DHCP_ENTRY = "add-dhcp-entry";
    public static final String CMD_CONFIG_DHCP_SUBNET = "config-dhcp-subnet";
    public static final String CMD_REMOVE_DHCP_SUBNET = "remove-dhcp-subnet";
    public static final String CMD_REMOVE_DHCP_ENTRY = "remove-dhcp-entry";
    public static final String CMD_ADD_DNS_ENTRY = "add-dns-entry";
    public static final String CMD_REMOVE_DNS_ENTRY = "remove-dns-entry";
    public static final String CMD_CONFIG_DNS_SUBNET = "config-dns-subnet";
    public static final String CMD_REMOVE_DNS_SUBNET = "remove-dns-subnet";
    public static final String CMD_SAVE_VM_DATA = "save-vm-data";
    public static final String CMD_SAVE_PASSWORD = "save-password";
    public static final String CMD_SAVE_USERDATA = "save-userdata";
    public static final String CMD_SAVE_SSHKEY = "save-sshkey";
    public static final String CMD_SAVE_HYPERVISOR_HOSTNAME = "save-hypervisor-hostname";
    public static final String CMD_APPLY_LB_RULES = "apply-lb-rules";
    public static final String CMD_APPLY_FW_RULES = "apply-fw-rules";
    public static final String CMD_RESTORE_NETWORK = "restore-network";
    public static final String CMD_IMPLEMENT_VPC = "implement-vpc";
    public static final String CMD_SHUTDOWN_VPC = "shutdown-vpc";
    public static final String CMD_UPDATE_VPC_SOURCE_NAT_IP = "update-vpc-source-nat-ip";
    public static final String CMD_APPLY_NETWORK_ACL = "apply-network-acl";
    public static final String CMD_CUSTOM_ACTION = "custom-action";
    public static final String CMD_PREPARE_NIC = "prepare-nic";
    public static final String CMD_RELEASE_NIC = "release-nic";

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
        copy.extensionDetailsDao            = this.extensionDetailsDao;
        copy.networkDao                     = this.networkDao;
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
            // in the extension's "network.service.capabilities" detail.  The ExtensionHelper
            // exposes a helper that loads the Service→Capability map from the DB.
            if (StringUtils.isNotBlank(providerName)) {
                Map<Service, Map<Capability, String>> caps = extensionHelper.getNetworkCapabilitiesForProvider(null, providerName);
                if (MapUtils.isNotEmpty(caps)) {
                    return caps;
                }
            }
        } catch (Exception e) {
            logger.warn("Failed to load network service capabilities from extension details for provider '{}': {}", providerName, e.getMessage());
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
            logger.warn("Network {} has no physical network — cannot resolve extension", network);
            return null;
        }
        Extension ext = extensionHelper.getExtensionForPhysicalNetworkAndProvider(physicalNetworkId, providerName);
        if (ext != null) {
            return ext;
        }
        logger.warn("No extension found for scoped provider '{}' on physical network {}", providerName, physicalNetworkId);
        return null;
    }

    protected boolean canHandle(Network network, Service service) {
        Long physicalNetworkId = network.getPhysicalNetworkId();
        if (physicalNetworkId == null) {
            return false;
        }
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
        logger.info("Implementing network extension for network {} (VLAN {})", network, network.getBroadcastUri());

        // Step 1: Ensure a network device is selected and its details stored.
        ensureExtensionDetails(network);

        // Step 2: Create the network on the device.
        JsonObject implementPayload = new JsonObject();
        addNetworkToPayload(implementPayload, network);
        addExtensionIpToPayload(implementPayload, network);

        Pair<Integer, String> result = executeScriptAndReturnOutput(network, CMD_IMPLEMENT_NETWORK, implementPayload);

        if (result.first() != EXIT_CODE_SUCCESS) {
            return false;
        }

        // Update the network properties from the output
        applyNetworkUpdateFromScriptOutput(network, result.second());

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
                logger.warn("Failed to configure source NAT IP for network {}: {}", network, e.getMessage());
            }
        }

        return true;
    }

    @Override
    public boolean prepare(Network network, NicProfile nic, VirtualMachineProfile vm,
            DeployDestination dest, ReservationContext context)
            throws ConcurrentOperationException, ResourceUnavailableException, InsufficientCapacityException {

        if (!canHandle(network, null)) {
            return false;
        }

        if (!networkModel.isProviderEnabledInPhysicalNetwork(networkModel.getPhysicalNetworkId(network), getProvider().getName())) {
            return false;
        }

        // Sync nic with network
        applyNicUpdateFromNetwork(network, nic.getId());

        // Build payload for prepare-nic script command
        try {
            JsonObject payload = new JsonObject();
            addNetworkToPayload(payload, network);
            addNicToPayload(payload, nic);
            if (vm != null) {
                payload.addProperty("hostname", safeStr(vm.getHostName()));
            }
            addExtensionIpToPayload(payload, network);

            logger.debug("Preparing NIC via extension script: network={} nicMac={} nicIp={}", network, nic.getMacAddress(), nic.getIPv4Address());

            return executeScript(network, CMD_PREPARE_NIC, payload);
        } catch (Exception e) {
            logger.warn("prepare: failed to prepare NIC for network {}: {}", network, e.getMessage());
            return false;
        }
    }

    private void applyNicUpdateFromNetwork(Network network, Long nicId) {
        if (nicId == null) {
            return;
        }
        NicVO nicVo = nicDao.findById(nicId);
        if (nicVo == null) {
            return;
        }
        if (network.getBroadcastUri() != null) {
            nicVo.setBroadcastUri(network.getBroadcastUri());
            nicVo.setIsolationUri(network.getBroadcastUri());
            nicDao.update(nicId, nicVo);
        }
    }

    @Override
    public boolean release(Network network, NicProfile nic, VirtualMachineProfile vm,
            ReservationContext context) throws ConcurrentOperationException, ResourceUnavailableException {
        if (!canHandle(network, null)) {
            return true;
        }

        try {
            JsonObject payload = new JsonObject();
            addNetworkToPayload(payload, network);
            addNicToPayload(payload, nic);
            addExtensionIpToPayload(payload, network);

            logger.debug("Releasing NIC via extension script: network={} nicMac={} nicIp={}", network, nic != null ? nic.getMacAddress() : null, nic != null ? nic.getIPv4Address() : null);

            return executeScript(network, CMD_RELEASE_NIC, payload);
        } catch (Exception e) {
            logger.warn("release: failed to release NIC for network {}: {}", network, e.getMessage());
            return false;
        }
    }

    @Override
    public boolean shutdown(Network network, ReservationContext context, boolean cleanup)
            throws ConcurrentOperationException, ResourceUnavailableException {
        logger.info("Shutting down network extension for network {}", network);
        JsonObject payload = new JsonObject();
        addNetworkToPayload(payload, network);
        boolean result = executeScript(network, CMD_SHUTDOWN_NETWORK, payload);
        if (result) {
            // Remove stored per-network extension details (e.g. namespace). For VPC-backed networks
            // the namespace is named cs-vpc-<vpcId>, stored in the extension details. Removing the
            // stored details ensures the namespace is deleted/forgotten on shutdown.
            try {
                networkDetailsDao.removeDetail(network.getId(), NETWORK_DETAIL_EXTENSION_DETAILS);
            } catch (Exception e) {
                logger.warn("Failed to remove network extension details for network {}: {}", network, e.getMessage());
            }
        }
        return result;
    }

    @Override
    public boolean destroy(Network network, ReservationContext context)
            throws ConcurrentOperationException, ResourceUnavailableException {
        logger.info("Destroying network extension for network {}", network);
        JsonObject payload = new JsonObject();
        addNetworkToPayload(payload, network);
        // For both isolated and VPC tier networks, use destroy-network.
        // For VPC tiers, the script preserves the shared namespace;
        // the VPC namespace is removed only when shutdownVpc() calls shutdown-vpc.
        boolean result = executeScript(network, CMD_DESTROY_NETWORK, payload);
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
        if (CollectionUtils.isEmpty(placeholderNics)) {
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
                if (StringUtils.isNotBlank(ip)) {
                    logger.debug("Cleaning up PlaceHolder IP {} on network {}", ip, network);
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
                        network, placeholderNic, e.getMessage());
            }

            try {
                nicDao.remove(placeholderNic.getId());
            } catch (Exception e) {
                logger.warn("Failed to remove placeholder nic {} for network {}: {}",
                        placeholderNic, network, e.getMessage());
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

        logger.info("Ensuring network device for network {} (current={})", network, currentDetails);

        Extension extension = resolveExtension(network);
        File scriptFile = resolveScriptFile(network, extension);

        JsonObject argsPayload = new JsonObject();
        addNetworkToPayload(argsPayload, network);
        argsPayload.addProperty("current_details", currentDetails);
        JsonObject payload = buildNetworkScriptPayload(network, argsPayload, extension);

        try {
            Pair<Integer, String> result = executeScriptWithFilePayload(scriptFile,
                    CMD_ENSURE_NETWORK_DEVICE, payload, "Network extension");
            String output = result.second() != null ? result.second() : "";

            if (result.first() != EXIT_CODE_SUCCESS) {
                logger.warn("ensure-network-device exited {} for network {} — keeping current details",
                        -1, network);
                if ("{}".equals(currentDetails)) {
                    networkDetailsDao.addDetail(network.getId(), NETWORK_DETAIL_EXTENSION_DETAILS, "{}", false);
                }
                return;
            }
            if (output.isEmpty()) {
                output = "{}".equals(currentDetails) ? "{}" : currentDetails;
            }
            if (!output.equals(currentDetails)) {
                logger.info("Network device updated for network {}: {}", network, output);
                networkDetailsDao.addDetail(network.getId(), NETWORK_DETAIL_EXTENSION_DETAILS, output, false);
            } else {
                logger.debug("Network device unchanged for network {}: {}", network, output);
            }
        } catch (Exception e) {
            logger.warn("Failed ensure-network-device for network {}: {}", network, e.getMessage());
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
                    logger.warn("Failed to acquire extension IP for network {}: {}", network, e.getMessage());
                }
        }
        return null;
    }

    // ---- IpDeployer ----

    @Override
    public boolean applyIps(Network network, List<? extends PublicIpAddress> ipAddress, Set<Service> services)
            throws ResourceUnavailableException {
        if (CollectionUtils.isEmpty(ipAddress)) {
            return true;
        }
        logger.info("Applying {} IPs for network {}", ipAddress.size(), network);

        for (PublicIpAddress ip : ipAddress) {
            boolean isSourceNat = ip.isSourceNat();
            boolean isRevoke = ip.getState() == IpAddress.State.Releasing;
            String action = isRevoke ? CMD_RELEASE_IP : CMD_ASSIGN_IP;

            JsonObject payload = new JsonObject();
            addNetworkToPayload(payload, network);
            addPublicIpToPayload(payload, ip.getId(), isSourceNat);

             boolean result = executeScript(network, action, payload);
             if (!result) {
                 throw new ResourceUnavailableException(
                         "Failed to " + action + " for IP " + ip.getAddress().addr(),
                         Network.class, network.getId());
             }
         }
         return true;
     }

    // ---- StaticNatServiceProvider ----

    @Override
    public boolean applyStaticNats(Network config, List<? extends StaticNat> rules)
            throws ResourceUnavailableException {
        if (CollectionUtils.isEmpty(rules)) {
            return true;
        }
        if (!canHandle(config, Service.StaticNat)) {
            return false;
        }
        logger.info("Applying {} static NAT rules for network {}", rules.size(), config);
        for (StaticNat rule : rules) {
            String action = rule.isForRevoke() ? CMD_DELETE_STATIC_NAT : CMD_ADD_STATIC_NAT;
            JsonObject payload = new JsonObject();
            addNetworkToPayload(payload, config);
            addPublicIpToPayload(payload, rule.getSourceIpAddressId(), false);
            payload.addProperty("private_ip", safeStr(rule.getDestIpAddress()));
            boolean result = executeScript(config, action, payload);
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
        if (CollectionUtils.isEmpty(rules)) {
            return true;
        }
        if (!canHandle(network, Service.PortForwarding)) {
            return false;
        }
        logger.info("Applying {} port forwarding rules for network {}", rules.size(), network);
        for (PortForwardingRule rule : rules) {
            boolean isRevoke = rule.getState() == FirewallRule.State.Revoke;
            String action = isRevoke ? CMD_DELETE_PORT_FORWARD : CMD_ADD_PORT_FORWARD;
            String publicPort  = PortForwardingServiceProvider.getPublicPortRange(rule);
            String privatePort = PortForwardingServiceProvider.getPrivatePFPortRange(rule);
            JsonObject payload = new JsonObject();
            addNetworkToPayload(payload, network);
            addPublicIpToPayload(payload, rule.getSourceIpAddressId(), false);
            payload.addProperty("public_port", safeStr(publicPort));
            payload.addProperty("private_ip", safeStr(rule.getDestinationIpAddress() != null
                    ? rule.getDestinationIpAddress().addr() : null));
            payload.addProperty("private_port", safeStr(privatePort));
            payload.addProperty("protocol", safeStr(rule.getProtocol()));
            boolean result = executeScript(network, action, payload);
            if (!result) {
                throw new ResourceUnavailableException("Failed to " + action + " for port forwarding rule",
                        Network.class, network.getId());
            }
        }
        return true;
    }

    // ---- Script execution ----

    protected boolean executeScript(Network network, String command, JsonObject argsPayload) {
        return executeScriptAndReturnOutput(network, command, argsPayload).first() == 0;
    }

    protected Pair<Integer, String> executeScriptAndReturnOutput(Network network, String command, JsonObject argsPayload) {
        ensureExtensionDetails(network);
        Extension extension = resolveExtension(network);
        JsonObject payload = buildNetworkScriptPayload(network, argsPayload, extension);
        return executeScriptWithFilePayload(network, command, payload);
    }

    private JsonObject parseJsonOutput(String outputStr) {
        if (StringUtils.isBlank(outputStr)) {
            return null;
        }
        try {
            JsonElement parsed = JsonParser.parseString(outputStr);
            if (!parsed.isJsonObject()) {
                logger.debug("Ignoring non-object script output: {}", outputStr);
                return null;
            }
            return parsed.getAsJsonObject();
        } catch (Exception e) {
            logger.debug("Ignoring non-JSON script output: {}", outputStr);
            return null;
        }
    }

    private String getJsonString(JsonObject jsonObject, String keyPath) {
        if (jsonObject == null || StringUtils.isBlank(keyPath)) {
            return null;
        }
        JsonElement value = jsonObject.has(keyPath) ? jsonObject.get(keyPath) : null;
        if (value == null) {
            JsonElement current = jsonObject;
            String[] parts = keyPath.split("\\.");
            for (String part : parts) {
                if (current == null || !current.isJsonObject()) {
                    current = null;
                    break;
                }
                JsonObject currentObj = current.getAsJsonObject();
                if (!currentObj.has(part)) {
                    current = null;
                    break;
                }
                current = currentObj.get(part);
            }
            value = current;
        }
        if (value == null || value.isJsonNull()) {
            return null;
        }
        return value.getAsString();
    }

    private void applyNetworkUpdateFromScriptOutput(Network network, String outputStr) {
        if (!ExtensionHelper.NETWORK_EXTENSION_GURU_NAME.equals(network.getGuruName())) {
            logger.debug("Network {} is not managed by NetworkExtensionGuru, skipping script output processing", network.getId());
            return;
        }

        JsonObject outputJson = parseJsonOutput(outputStr);

        String networkBroadcastUri = getJsonString(outputJson, "network.broadcast_uri");
        String networkBroadcastDomainType = getJsonString(outputJson, "network.broadcast_domain_type");
        if (networkBroadcastUri == null && networkBroadcastDomainType == null) {
            return;
        }

        try {
            NetworkVO networkVo = networkDao.findById(network.getId());
            if (networkVo == null) {
                return;
            }

            boolean changed = false;
            if (networkBroadcastDomainType != null) {
                Networks.BroadcastDomainType domainType = EnumUtils.getEnumIgnoreCase(Networks.BroadcastDomainType.class, networkBroadcastDomainType);
                if (domainType != null) {
                    networkVo.setBroadcastDomainType(domainType);
                    changed = true;
                } else {
                    logger.warn("Ignoring unknown broadcast domain type '{}' for network {}",
                            networkBroadcastDomainType, network);
                }
            }

            if (networkBroadcastUri != null) {
                networkVo.setBroadcastUri(URI.create(networkBroadcastUri));
                changed = true;
            }

            if (changed) {
                networkDao.update(networkVo.getId(), networkVo);
                for (NicVO nicVO : nicDao.listByNetworkId(networkVo.getId())) {
                    applyNicUpdateFromNetwork(networkVo, nicVO.getId());
                }
            }
        } catch (Exception e) {
            logger.warn("Failed to update network {} from script output: {}", network, e.getMessage());
        }
    }

    protected Pair<Integer, String> executeScriptWithFilePayload(Network network, String command, JsonObject payload) {
        Extension extension = resolveExtension(network);
        File scriptFile = resolveScriptFile(network, extension);
        return executeScriptWithFilePayload(scriptFile, command, payload, "Network extension");
    }

    private Pair<Integer, String> executeScriptWithFilePayload(File scriptFile, String command,
            JsonObject payload, String logPrefix) {
        File payloadFile = null;
        try {
            payloadFile = File.createTempFile("cs-extnet-" + command + "-", ".payload");
            String payloadJson = payload != null ? new Gson().toJson(payload) : "{}";
            logger.debug("Writing payload to payload file {}", payloadFile);
            Files.writeString(payloadFile.toPath(), payloadJson, StandardCharsets.UTF_8);

            List<String> cmdLine = new ArrayList<>();
            cmdLine.add(scriptFile.getAbsolutePath());
            cmdLine.add(command);
            cmdLine.add(payloadFile.getAbsolutePath());
            cmdLine.add(String.valueOf(DEFAULT_SCRIPT_TIMEOUT_SECONDS));

            logger.debug("Executing {} script: {}", logPrefix, String.join(" ", cmdLine));

            ProcessBuilder processBuilder = new ProcessBuilder(cmdLine);
            processBuilder.redirectErrorStream(true);
            Process process = processBuilder.start();
            byte[] output = process.getInputStream().readAllBytes();
            int exitCode = process.waitFor();

            String outputStr = new String(output).trim();
            if (!outputStr.isEmpty()) {
                logger.debug("Script output: {}", outputStr);
            }

            if (exitCode != EXIT_CODE_SUCCESS) {
                logger.error("{} script {} failed with exit code {}: {}", logPrefix, command, exitCode, outputStr);
                return new Pair<>(exitCode, outputStr);
            }

            JsonObject outputJson = parseJsonOutput(outputStr);
            String status = outputJson != null ? getJsonString(outputJson, "status") : null;
            if (StringUtils.isNotBlank(status) && !"success".equalsIgnoreCase(status)) {
                logger.error("{} script {} returned non-success status '{}': {}", logPrefix, command, status, outputStr);
                return new Pair<>(EXIT_CODE_FAILURE, outputStr);
            }

            return new Pair<>(EXIT_CODE_SUCCESS, outputStr);
        } catch (Exception e) {
            throw new CloudRuntimeException(
                    String.format("Failed preparing payload file for command %s", command), e);
        } finally {
            if (payloadFile != null && payloadFile.exists() && !payloadFile.delete()) {
                payloadFile.deleteOnExit();
            }
        }
    }

    private JsonObject buildNetworkScriptPayload(Network network, JsonObject argsPayload, Extension extension) {
        JsonObject payload = new JsonObject();
        payload.add(ARG_PHYSICAL_NETWORK_EXTENSION_DETAILS,
                buildPhysicalNetworkExtensionDetailsPayload(network.getPhysicalNetworkId(), extension));
        payload.add(ARG_NETWORK_EXTENSION_DETAILS, buildNetworkExtensionDetailsPayload(network));
        payload.add(ARG_PAYLOAD, argsPayload != null ? argsPayload : new JsonObject());
        return payload;
    }

    /**
     * Adds all standard network-level fields to the payload.
     * Always-present: network_id, vlan, zone_id.
     * Conditional (only when non-blank): gateway, cidr, vpc_id, and the two
     * IPv6 network fields (network_ip6_gateway, network_ip6_cidr).
     */
    private void addNetworkToPayload(JsonObject payload, Network network) {
        if (payload == null || network == null) {
            return;
        }
        payload.addProperty("network_id", String.valueOf(network.getId()));
        payload.addProperty("vlan", safeStr(getVlanId(network)));
        payload.addProperty("zone_id", String.valueOf(network.getDataCenterId()));
        if (network.getGuestType() != null) {
            payload.addProperty("guest_type", network.getGuestType().toString().toLowerCase());
        }
        if (StringUtils.isNotBlank(network.getGateway())) {
            payload.addProperty("gateway", safeStr(network.getGateway()));
        }
        if (StringUtils.isNotBlank(network.getCidr())) {
            payload.addProperty("cidr", safeStr(network.getCidr()));
        }
        if (network.getVpcId() != null) {
            payload.addProperty("vpc_id", String.valueOf(network.getVpcId()));
        }
        if (StringUtils.isNotBlank(network.getIp6Gateway())) {
            payload.addProperty("network_ip6_gateway", safeStr(network.getIp6Gateway()));
        }
        if (StringUtils.isNotBlank(network.getIp6Cidr())) {
            payload.addProperty("network_ip6_cidr", safeStr(network.getIp6Cidr()));
        }
    }

    /**
     * Adds all available NIC fields to the payload.
     * Fields are added only when non-blank / non-null so the JSON stays clean.
     * Covers: nic_id, nic_uuid, mac, ip, gateway (IPv4), netmask, default_nic,
     * device_id, and the three IPv6 NIC fields.
     */
    private void addNicToPayload(JsonObject payload, NicProfile nic) {
        if (payload == null || nic == null) {
            return;
        }
        payload.addProperty("nic_id", String.valueOf(nic.getId()));
        if (StringUtils.isNotBlank(nic.getUuid())) {
            payload.addProperty("nic_uuid", nic.getUuid());
        }
        payload.addProperty("mac", safeStr(nic.getMacAddress()));
        payload.addProperty("ip", safeStr(nic.getIPv4Address()));
        if (StringUtils.isNotBlank(nic.getIPv4Gateway())) {
            payload.addProperty("gateway", safeStr(nic.getIPv4Gateway()));
        }
        if (StringUtils.isNotBlank(nic.getIPv4Netmask())) {
            payload.addProperty("netmask", safeStr(nic.getIPv4Netmask()));
        }
        payload.addProperty("default_nic", String.valueOf(nic.isDefaultNic()));
        if (nic.getDeviceId() != null) {
            payload.addProperty("device_id", String.valueOf(nic.getDeviceId()));
        }
        if (StringUtils.isNotBlank(nic.getIPv6Address())) {
            payload.addProperty("ip6_address", safeStr(nic.getIPv6Address()));
        }
        if (StringUtils.isNotBlank(nic.getIPv6Gateway())) {
            payload.addProperty("ip6_gateway", safeStr(nic.getIPv6Gateway()));
        }
        if (StringUtils.isNotBlank(nic.getIPv6Cidr())) {
            payload.addProperty("ip6_cidr", safeStr(nic.getIPv6Cidr()));
        }
    }

    /**
     * Adds the network DNS fields to the payload: {@code dns} (comma-separated
     * resolver list) and {@code domain} (network domain suffix).
     */
    private void addNetworkDnsToPayload(JsonObject payload, Network network) {
        if (payload == null || network == null) {
            return;
        }
        payload.addProperty("dns", safeStr(getNetworkDns(network)));
        payload.addProperty("domain", safeStr(network.getNetworkDomain()));
    }

    /**
     * Resolves the extension IP for {@code network} via {@link #ensureExtensionIp}
     * and adds it to the payload as {@code extension_ip}.
     */
    private void addExtensionIpToPayload(JsonObject payload, Network network) {
        if (payload == null || network == null) {
            return;
        }
        String extensionIp = ensureExtensionIp(network);
        if (StringUtils.isNotBlank(extensionIp)) {
            payload.addProperty("extension_ip", extensionIp);
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
     * Builds the physical-network extension details as a {@link JsonObject}.
     * Includes all {@code extension_resource_map_details} for the extension on the
     * physical network, enriched with physical-network metadata fields.
     */
    private JsonObject buildPhysicalNetworkExtensionDetailsPayload(Long physicalNetworkId, Extension extension) {
        Map<String, String> map = buildPhysicalNetworkDetailsMap(physicalNetworkId, extension);
        JsonObject obj = new JsonObject();
        for (Map.Entry<String, String> entry : map.entrySet()) {
            if (entry.getValue() != null) {
                obj.addProperty(entry.getKey(), entry.getValue());
            }
        }
        return obj;
    }

    /**
     * Returns the per-network extension-details JSON blob (the value stored under
     * {@code NETWORK_DETAIL_EXTENSION_DETAILS} in {@code network_details} or
     * {@code vpc_details}) as a {@link JsonObject}.
     * Returns an empty object when no blob has been stored yet.
     */
    private JsonObject buildNetworkExtensionDetailsPayload(Network network) {
        String json;
        if (network.getVpcId() != null) {
            Map<String, String> vpcDetails = vpcDetailsDao.listDetailsKeyPairs(network.getVpcId());
            json = vpcDetails != null ? vpcDetails.getOrDefault(NETWORK_DETAIL_EXTENSION_DETAILS, "{}") : "{}";
        } else {
            Map<String, String> networkDetails = networkDetailsDao.listDetailsKeyPairs(network.getId());
            json = networkDetails != null ? networkDetails.getOrDefault(NETWORK_DETAIL_EXTENSION_DETAILS, "{}") : "{}";
        }
        return parseJsonObjectOrEmpty(json);
    }

    /**
     * Returns the VPC-level extension-details JSON blob (stored under
     * {@code NETWORK_DETAIL_EXTENSION_DETAILS} in {@code vpc_details}) as a
     * {@link JsonObject}.  Returns an empty object when no blob has been stored.
     */
    private JsonObject buildVpcExtensionDetailsPayload(long vpcId) {
        Map<String, String> vpcDetails = vpcDetailsDao.listDetailsKeyPairs(vpcId);
        String json = vpcDetails != null ? vpcDetails.getOrDefault(NETWORK_DETAIL_EXTENSION_DETAILS, "{}") : "{}";
        return parseJsonObjectOrEmpty(json);
    }

    /**
     * Builds the custom-action parameters as a {@link JsonObject}.
     * Returns an empty object for {@code null} or empty parameter maps.
     */
    private JsonObject buildActionParamsPayload(Map<String, Object> parameters) {
        JsonObject obj = new JsonObject();
        if (MapUtils.isEmpty(parameters)) {
            return obj;
        }
        for (Map.Entry<String, Object> entry : parameters.entrySet()) {
            obj.addProperty(entry.getKey(),
                    entry.getValue() != null ? entry.getValue().toString() : "");
        }
        return obj;
    }

    /**
     * Parses a JSON string into a {@link JsonObject}.
     * Returns an empty {@link JsonObject} when the input is {@code null}, blank,
     * or not a valid JSON object.
     */
    private JsonObject parseJsonObjectOrEmpty(String json) {
        if (StringUtils.isBlank(json)) {
            return new JsonObject();
        }
        try {
            JsonElement element = JsonParser.parseString(json);
            return element.isJsonObject() ? element.getAsJsonObject() : new JsonObject();
        } catch (Exception e) {
            throw new CloudRuntimeException("Failed to parse JSON: " + json, e);
        }
    }

    // ---- Custom action ----

    @Override
    public boolean canHandleCustomAction(Network network) {
        return canHandle(network, Service.CustomAction);
    }

    /**
     * Runs a custom action on the external network device.
     * The custom action payload is written to a temporary file and passed to the
     * extension script via {@link #executeScriptWithFilePayload(File, String, JsonObject, String)}.
     */
    @Override
    public String runCustomAction(Network network, String actionName, Map<String, Object> parameters) {
        Extension extension = resolveExtension(network);
        File scriptFile = resolveScriptFile(network, extension);

        JsonObject payload = buildCustomActionPayload(network, extension, actionName, parameters);

        logger.info("Running custom action '{}' on network {} (extension: {}, params: {} key(s))",
                actionName, network, extension != null ? extension.getName() : "unknown",
                parameters != null ? parameters.size() : 0);

        try {
            Pair<Integer, String> result = executeScriptWithFilePayload(scriptFile, CMD_CUSTOM_ACTION, payload,
                    "Network extension");
            String outputStr = result.second() != null ? result.second().trim() : "";

            if (result.first() != EXIT_CODE_SUCCESS) {
                logger.error("Custom action '{}' on network {} failed: {}", actionName, network, outputStr);
                return null;
            }
            logger.info("Custom action '{}' on network {} completed successfully", actionName, network);
            return outputStr.isEmpty() ? "OK" : outputStr;
        } catch (Exception e) {
            logger.error("Failed to execute custom action '{}' on network {}: {}", actionName, network, e.getMessage());
            throw new CloudRuntimeException(String.format("Failed to execute custom action %s on network %s", actionName, network.getUuid()), e);
        }
    }

    @Override
    public boolean canHandleVpcCustomAction(Vpc vpc) {
        return resolveExtensionForVpc(vpc) != null;
    }

    /**
     * Runs a custom action on the external network device for a VPC.
     * The custom action payload is written to a temporary file and passed to the
     * extension script via {@link #executeScriptWithFilePayload(File, String, JsonObject, String)}.
     */
    @Override
    public String runCustomAction(Vpc vpc, String actionName, Map<String, Object> parameters) {
        Pair<Long, Extension> physNetAndExt = resolveExtensionForVpc(vpc);
        if (physNetAndExt == null) {
            throw new CloudRuntimeException("No extension found for VPC " + vpc.getUuid());
        }
        Long physicalNetworkId = physNetAndExt.first();
        Extension extension = physNetAndExt.second();
        File scriptFile = resolveScriptFileForVpc(physicalNetworkId, extension);

        JsonObject payload = buildCustomActionPayload(vpc, physicalNetworkId, extension, actionName, parameters);

        logger.info("Running custom action '{}' on VPC {} (extension: {}, params: {} key(s))",
                actionName, vpc, extension != null ? extension.getName() : "unknown",
                parameters != null ? parameters.size() : 0);

        try {
            Pair<Integer, String> result = executeScriptWithFilePayload(scriptFile, CMD_CUSTOM_ACTION, payload,
                    "VPC extension");
            String outputStr = result.second() != null ? result.second().trim() : "";

            if (result.first() != EXIT_CODE_SUCCESS) {
                logger.error("VPC custom action '{}' on VPC {} failed: {}", actionName, vpc, outputStr);
                return null;
            }
            logger.info("VPC custom action '{}' on VPC {} completed successfully", actionName, vpc);
            return outputStr.isEmpty() ? "OK" : outputStr;
        } catch (Exception e) {
            logger.error("Failed to execute VPC custom action '{}' on VPC {}: {}", actionName, vpc, e.getMessage());
            throw new CloudRuntimeException(String.format("Failed to execute VPC custom action %s on VPC %s ", actionName, vpc.getUuid()), e);
        }
    }

    private JsonObject buildCustomActionPayload(Network network, Extension extension, String actionName,
            Map<String, Object> parameters) {
        JsonObject payload = new JsonObject();
        addNetworkToPayload(payload, network);
        payload.addProperty("action", actionName);
        payload.add(ARG_ACTION_PARAMS, buildActionParamsPayload(parameters));
        payload.add(ARG_PHYSICAL_NETWORK_EXTENSION_DETAILS,
                buildPhysicalNetworkExtensionDetailsPayload(network.getPhysicalNetworkId(), extension));
        payload.add(ARG_NETWORK_EXTENSION_DETAILS,
                buildNetworkExtensionDetailsPayload(network));
        return payload;
    }

    private JsonObject buildCustomActionPayload(Vpc vpc, Long physicalNetworkId, Extension extension,
            String actionName, Map<String, Object> parameters) {
        JsonObject payload = new JsonObject();
        payload.addProperty("vpc_id", String.valueOf(vpc.getId()));
        payload.addProperty("action", actionName);
        payload.add(ARG_ACTION_PARAMS, buildActionParamsPayload(parameters));
        payload.add(ARG_PHYSICAL_NETWORK_EXTENSION_DETAILS,
                buildPhysicalNetworkExtensionDetailsPayload(physicalNetworkId, extension));
        payload.add(ARG_NETWORK_EXTENSION_DETAILS,
                buildVpcExtensionDetailsPayload(vpc.getId()));
        return payload;
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
            throw new CloudRuntimeException("Network " + network.getUuid() + " has no physical network");
        }
        if (extension == null) {
            throw new CloudRuntimeException(
                    "No NetworkOrchestrator extension found for network " + network.getUuid()
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

        File extensionFile = new File(extensionPath);
        if (extensionFile.isFile() && extensionFile.canExecute()) {
            return extensionFile;
        }

        throw new CloudRuntimeException(
                "No executable script found in extension path " + extensionPath + " inside the extension directory.");
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

    /**
     * Adds all standard public-IP fields to the payload.
     * Makes exactly two DB calls: one for the {@link IpAddress} and one for
     * its {@link VlanVO}. All five fields are then derived from those two
     * objects in memory — no further DB calls are made.
     * Fields: {@code public_ip}, {@code public_vlan}, {@code public_gateway},
     * {@code public_cidr}, {@code source_nat}.
     */
    private void addPublicIpToPayload(JsonObject payload, Long ipAddressId, boolean sourceNat) {
        if (payload == null || ipAddressId == null) {
            return;
        }
        IpAddress ip = networkModel.getIp(ipAddressId);
        if (ip == null || ip.getAddress() == null) {
            return;
        }
        VlanVO vlan = vlanDao.findById(ip.getVlanId());
        payload.addProperty("public_ip", safeStr(ip.getAddress().addr()));
        payload.addProperty("public_vlan", vlan != null ? safeStr(vlan.getVlanTag()) : "");
        payload.addProperty("public_gateway", vlan != null ? safeStr(vlan.getVlanGateway()) : "");
        payload.addProperty("public_cidr", vlan != null
                ? StringUtils.defaultString(NetUtils.ipAndNetMaskToCidr(ip.getAddress().addr(), vlan.getVlanNetmask())) : "");
        payload.addProperty("source_nat", String.valueOf(sourceNat));
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
        logger.debug("addDhcpEntry: network={} mac={} ip={} ipv6={}", network,
                nic.getMacAddress(), nic.getIPv4Address(), nic.getIPv6Address());
        JsonObject payload = new JsonObject();
        addNetworkToPayload(payload, network);
        addNicToPayload(payload, nic);
        payload.addProperty("hostname", safeStr(vm.getHostName()));
        addNetworkDnsToPayload(payload, network);
        addExtensionIpToPayload(payload, network);
        return executeScript(network, CMD_ADD_DHCP_ENTRY, payload);
    }

    @Override
    public boolean configDhcpSupportForSubnet(Network network, NicProfile nic, VirtualMachineProfile vm,
            DeployDestination dest, ReservationContext context)
            throws ConcurrentOperationException, InsufficientCapacityException, ResourceUnavailableException {
        if (!canHandle(network, Service.Dhcp)) {
            return false;
        }
        logger.debug("configDhcpSupportForSubnet: network={}", network);
        JsonObject payload = new JsonObject();
        addNetworkToPayload(payload, network);
        addNetworkDnsToPayload(payload, network);
        addExtensionIpToPayload(payload, network);
        addNicToPayload(payload, nic);
        return executeScript(network, CMD_CONFIG_DHCP_SUBNET, payload);
    }

    @Override
    public boolean removeDhcpSupportForSubnet(Network network) throws ResourceUnavailableException {
        if (!canHandle(network, Service.Dhcp)) {
            return false;
        }
        logger.debug("removeDhcpSupportForSubnet: network={}", network);
        JsonObject payload = new JsonObject();
        addNetworkToPayload(payload, network);
        addExtensionIpToPayload(payload, network);
        return executeScript(network, CMD_REMOVE_DHCP_SUBNET, payload);
    }

    @Override
    public boolean setExtraDhcpOptions(Network network, long nicId, Map<Integer, String> dhcpOptions) {
        return false;
    }

    @Override
    public boolean removeDhcpEntry(Network network, NicProfile nic, VirtualMachineProfile vmProfile)
            throws ResourceUnavailableException {
        if (!canHandle(network, Service.Dhcp)) {
            return false;
        }
        logger.debug("removeDhcpEntry: network={} mac={} ip={} ipv6={}", network,
                nic.getMacAddress(), nic.getIPv4Address(), nic.getIPv6Address());
        JsonObject payload = new JsonObject();
        addNetworkToPayload(payload, network);
        addNicToPayload(payload, nic);
        addExtensionIpToPayload(payload, network);
        return executeScript(network, CMD_REMOVE_DHCP_ENTRY, payload);
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
        logger.debug("addDnsEntry: network={} hostname={} ip={} ipv6={}", network,
                hostname, nic.getIPv4Address(), nic.getIPv6Address());
        JsonObject payload = new JsonObject();
        addNetworkToPayload(payload, network);
        addNetworkDnsToPayload(payload, network);
        addNicToPayload(payload, nic);
        payload.addProperty("hostname", safeStr(hostname));
        addExtensionIpToPayload(payload, network);
        return executeScript(network, CMD_ADD_DNS_ENTRY, payload);
    }

    @Override
    public boolean removeDnsEntry(Network network, NicProfile nic, VirtualMachineProfile vmProfile) throws ResourceUnavailableException {
        if (!canHandle(network, Service.Dns)) {
            return false;
        }
        logger.debug("removeDnsEntry: network={} mac={} ip={} ipv6={}", network,
                nic.getMacAddress(), nic.getIPv4Address(), nic.getIPv6Address());
        JsonObject payload = new JsonObject();
        addNetworkToPayload(payload, network);
        addNetworkDnsToPayload(payload, network);
        addNicToPayload(payload, nic);
        addExtensionIpToPayload(payload, network);
        return executeScript(network, CMD_REMOVE_DNS_ENTRY, payload);
    }

    @Override
    public boolean configDnsSupportForSubnet(Network network, NicProfile nic, VirtualMachineProfile vm,
            DeployDestination dest, ReservationContext context)
            throws ConcurrentOperationException, InsufficientCapacityException, ResourceUnavailableException {
        if (!canHandle(network, Service.Dns)) {
            return false;
        }
        logger.debug("configDnsSupportForSubnet: network={}", network);
        JsonObject payload = new JsonObject();
        addNetworkToPayload(payload, network);
        addNetworkDnsToPayload(payload, network);
        addExtensionIpToPayload(payload, network);
        addNicToPayload(payload, nic);
        return executeScript(network, CMD_CONFIG_DNS_SUBNET, payload);
    }

    @Override
    public boolean removeDnsSupportForSubnet(Network network) throws ResourceUnavailableException {
        if (!canHandle(network, Service.Dns)) {
            return false;
        }
        logger.debug("removeDnsSupportForSubnet: network={}", network);
        JsonObject payload = new JsonObject();
        addNetworkToPayload(payload, network);
        addNetworkDnsToPayload(payload, network);
        addExtensionIpToPayload(payload, network);
        return executeScript(network, CMD_REMOVE_DNS_SUBNET, payload);
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
            logger.debug("Could not fetch SSH public key for VM {}: {}", profile, e.getMessage());
        }

        // Service offering display name
        String serviceOfferingName = "";
        try {
            serviceOfferingName = profile.getServiceOffering().getDisplayText();
        } catch (Exception e) {
            logger.debug("Could not fetch service offering for VM {}: {}", profile, e.getMessage());
        }

        // Is Windows guest?
        boolean isWindows = false;
        try {
            isWindows = guestOSCategoryDao
                    .findById(guestOSDao.findById(vm.getGuestOSId()).getCategoryId())
                    .getName().equalsIgnoreCase("Windows");
        } catch (Exception e) {
            logger.debug("Could not determine OS type for VM {}: {}", profile, e.getMessage());
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
            logger.debug("Could not resolve hypervisor hostname for VM {}: {}", profile, e.getMessage());
        }

        // Password from the VM profile parameter (set by UserVmManager before deployment)
        String password = (String) profile.getParameter(VirtualMachineProfile.Param.VmPassword);

        // Use this NIC's IP — the metadata server in each namespace identifies requesters
        // by REMOTE_ADDR, which will be the VM's IP on THIS network (not necessarily the
        // default NIC IP), so we always key metadata by the NIC's IP on this network.
        String nicIpAddress = nic.getIPv4Address();

        logger.debug("addPasswordAndUserdata: network={} ip={} ipv6={} hasPassword={} hasSshKey={}",
                network.getId(), nicIpAddress, nic.getIPv6Address(),
                StringUtils.isNotEmpty(password),
                StringUtils.isNotEmpty(sshPublicKey));

        final UserVmVO userVm = userVmDao.findById(vm.getId());
        if (userVm == null) {
            throw new CloudRuntimeException("Could not find UserVmVO for VM " + vm.getUuid());
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

        if (CollectionUtils.isEmpty(vmData)) {
            logger.debug("addPasswordAndUserdata: no VM data generated for network={} ip={} ipv6={}", network, nicIpAddress, nic.getIPv6Address());
            return true;
        }

        JsonArray vmDataArray = new JsonArray();
        for (String[] entry : vmData) {
            String dir     = entry[NetworkModel.CONFIGDATA_DIR];
            String file    = entry[NetworkModel.CONFIGDATA_FILE];
            String content = entry.length > NetworkModel.CONFIGDATA_CONTENT
                    ? entry[NetworkModel.CONFIGDATA_CONTENT] : null;
            if (content == null) content = "";

            String contentStr;
            if (NetworkModel.USERDATA_DIR.equals(dir) && NetworkModel.USERDATA_FILE.equals(file)) {
                try {
                    contentStr = new String(Base64.getDecoder().decode(content), StandardCharsets.UTF_8);
                } catch (Exception e) {
                    contentStr = content;
                }
            } else {
                contentStr = content;
            }

            JsonObject entryObj = new JsonObject();
            entryObj.addProperty("dir", dir);
            entryObj.addProperty("file", file);
            entryObj.addProperty("content", contentStr);
            vmDataArray.add(entryObj);
        }

        JsonObject payload = new JsonObject();
        addNetworkToPayload(payload, network);
        addNicToPayload(payload, nic);
        payload.addProperty("ip", safeStr(nicIpAddress));
        addExtensionIpToPayload(payload, network);
        payload.add("vm_data", vmDataArray);

        return executeScript(network, CMD_SAVE_VM_DATA, payload);
    }

    @Override
    public boolean savePassword(Network network, NicProfile nic, VirtualMachineProfile vm)
            throws ResourceUnavailableException {
        if (!canHandle(network, Service.UserData)) {
            return false;
        }
        String password = (String) vm.getParameter(VirtualMachineProfile.Param.VmPassword);
        if (StringUtils.isEmpty(password)) {
            return true;
        }
        logger.debug("savePassword: network={} ip={} ipv6={}", network, nic.getIPv4Address(), nic.getIPv6Address());
        JsonObject payload = new JsonObject();
        addNetworkToPayload(payload, network);
        addNicToPayload(payload, nic);
        payload.addProperty("password", password);
        addExtensionIpToPayload(payload, network);
        return executeScript(network, CMD_SAVE_PASSWORD, payload);
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
        if (StringUtils.isEmpty(userData)) {
            return true;
        }
        logger.debug("saveUserData: network={} ip={} ipv6={}", network, nic.getIPv4Address(), nic.getIPv6Address());
        String userDataDecoded;
        try {
            userDataDecoded = new String(Base64.getDecoder().decode(userData), StandardCharsets.UTF_8);
        } catch (Exception e) {
            userDataDecoded = userData;
        }
        JsonObject payload = new JsonObject();
        addNetworkToPayload(payload, network);
        addNicToPayload(payload, nic);
        payload.addProperty("userdata", userDataDecoded);
        addExtensionIpToPayload(payload, network);
        return executeScript(network, CMD_SAVE_USERDATA, payload);
    }

    @Override
    public boolean saveSSHKey(Network network, NicProfile nic, VirtualMachineProfile vm,
            String sshPublicKey) throws ResourceUnavailableException {
        if (!canHandle(network, Service.UserData)) {
            return false;
        }
        if (StringUtils.isEmpty(sshPublicKey)) {
            return true;
        }
        logger.debug("saveSSHKey: network={} ip={} ipv6={}", network, nic.getIPv4Address(), nic.getIPv6Address());
        JsonObject payload = new JsonObject();
        addNetworkToPayload(payload, network);
        addNicToPayload(payload, nic);
        payload.addProperty("sshkey", sshPublicKey);
        addExtensionIpToPayload(payload, network);
        return executeScript(network, CMD_SAVE_SSHKEY, payload);
    }

    @Override
    public boolean saveHypervisorHostname(NicProfile nic, Network network, VirtualMachineProfile vm,
            DeployDestination dest) throws ResourceUnavailableException {
        if (!canHandle(network, Service.UserData)) {
            return false;
        }
        String hostname = dest != null && dest.getHost() != null ? dest.getHost().getName() : null;
        if (StringUtils.isBlank(hostname)) {
            return true;
        }
        logger.debug("saveHypervisorHostname: network={} ip={} ipv6={} host={}", network,
                nic.getIPv4Address(), nic.getIPv6Address(), hostname);
        JsonObject payload = new JsonObject();
        addNetworkToPayload(payload, network);
        addNicToPayload(payload, nic);
        payload.addProperty("hypervisor_hostname", hostname);
        addExtensionIpToPayload(payload, network);
        return executeScript(network, CMD_SAVE_HYPERVISOR_HOSTNAME, payload);
    }

    // ---- LoadBalancingServiceProvider ----

    @Override
    public boolean applyLBRules(Network network, List<LoadBalancingRule> rules)
            throws ResourceUnavailableException {
        if (CollectionUtils.isEmpty(rules)) {
            return true;
        }
        if (!canHandle(network, Service.Lb)) {
            return false;
        }
        logger.info("Applying {} LB rules for network {}", rules.size(), network);

        JsonArray lbRulesArray = new JsonArray();
        for (LoadBalancingRule rule : rules) {
            boolean revoke = rule.getState() == FirewallRule.State.Revoke;
            JsonObject ruleObj = new JsonObject();
            ruleObj.addProperty("id", rule.getId());
            ruleObj.addProperty("name", rule.getName());
            ruleObj.addProperty("publicIp", rule.getSourceIp() != null ? rule.getSourceIp().addr() : "");
            ruleObj.addProperty("publicPort", rule.getSourcePortStart());
            ruleObj.addProperty("privatePort", rule.getDefaultPortStart());
            ruleObj.addProperty("protocol", safeStr(rule.getProtocol()));
            ruleObj.addProperty("algorithm", safeStr(rule.getAlgorithm()));
            ruleObj.addProperty("revoke", revoke);
            JsonArray backendsArray = buildLBRuleBackendArray(rule);
            ruleObj.add("backends", backendsArray);
            lbRulesArray.add(ruleObj);
        }

        JsonObject payload = new JsonObject();
        addNetworkToPayload(payload, network);
        payload.add("lb_rules", lbRulesArray);
        boolean result = executeScript(network, CMD_APPLY_LB_RULES, payload);
        if (!result) {
            throw new ResourceUnavailableException("Failed to apply LB rules for network " + network.getUuid(),
                    Network.class, network.getId());
        }
        return true;
    }

    private static JsonArray buildLBRuleBackendArray(LoadBalancingRule rule) {
        JsonArray backendsArray = new JsonArray();
        if (rule.getDestinations() != null) {
            for (LoadBalancingRule.LbDestination dest : rule.getDestinations()) {
                JsonObject destObj = new JsonObject();
                destObj.addProperty("ip", dest.getIpAddress());
                destObj.addProperty("port", dest.getDestinationPortStart());
                destObj.addProperty("revoked", dest.isRevoked());
                backendsArray.add(destObj);
            }
        }
        return backendsArray;
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
                network, allRules.size(), defaultEgressAllow);

        JsonObject fwRules = new JsonObject();
        fwRules.addProperty("default_egress_allow", defaultEgressAllow);
        fwRules.addProperty("cidr", safeStr(network.getCidr()));
        JsonArray rulesArray = new JsonArray();
        for (FirewallRuleVO rule : allRules) {
            boolean isEgress = FirewallRule.TrafficType.Egress.equals(rule.getTrafficType());
            JsonObject ruleObj = new JsonObject();
            ruleObj.addProperty("id", rule.getId());
            ruleObj.addProperty("type", isEgress ? "egress" : "ingress");
            ruleObj.addProperty("protocol", safeStr(rule.getProtocol()));
            if (rule.getSourcePortStart() != null) ruleObj.addProperty("portStart", rule.getSourcePortStart());
            if (rule.getSourcePortEnd() != null) ruleObj.addProperty("portEnd", rule.getSourcePortEnd());
            if (rule.getIcmpType() != null) ruleObj.addProperty("icmpType", rule.getIcmpType());
            if (rule.getIcmpCode() != null) ruleObj.addProperty("icmpCode", rule.getIcmpCode());
            // For ingress rules include the public IP the rule is associated with.
            if (!isEgress) ruleObj.addProperty("publicIp", getIpAddress(rule.getSourceIpAddressId()));
            // sourceCidrs: for ingress = allowed external source IPs;
            //              for egress  = allowed VM source IP ranges
            JsonArray sourceCidrsArray = new JsonArray();
            List<String> sourceCidrs = rule.getSourceCidrList();
            if (CollectionUtils.isNotEmpty(sourceCidrs)) {
                for (String cidr : sourceCidrs) sourceCidrsArray.add(cidr);
            }
            ruleObj.add("sourceCidrs", sourceCidrsArray);
            // destCidrs: optional destination CIDR filter (meaningful for egress rules)
            JsonArray destCidrsArray = new JsonArray();
            List<String> destCidrs = rule.getDestinationCidrList();
            if (CollectionUtils.isNotEmpty(destCidrs)) {
                for (String cidr : destCidrs) destCidrsArray.add(cidr);
            }
            ruleObj.add("destCidrs", destCidrsArray);
            rulesArray.add(ruleObj);
        }
        fwRules.add("rules", rulesArray);

        JsonObject payload = new JsonObject();
        addNetworkToPayload(payload, network);
        payload.add("fw_rules", fwRules);

        boolean result = executeScript(network, CMD_APPLY_FW_RULES, payload);
        if (!result) {
            throw new ResourceUnavailableException(
                    "Failed to apply firewall rules for network " + network,
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
        logger.debug("prepareAggregatedExecution: network={} dest={}", network, dest);
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

        logger.debug("completeAggregatedExecution: restoring all VM network data for network={} dest={}", network, dest);

        boolean dhcpEnabled     = networkModel.areServicesSupportedInNetwork(network.getId(), Service.Dhcp)
                && networkModel.isProviderSupportServiceInNetwork(network.getId(), Service.Dhcp, getProvider());
        boolean dnsEnabled      = networkModel.areServicesSupportedInNetwork(network.getId(), Service.Dns)
                && networkModel.isProviderSupportServiceInNetwork(network.getId(), Service.Dns, getProvider());
        boolean userdataEnabled = networkModel.areServicesSupportedInNetwork(network.getId(), Service.UserData)
                && networkModel.isProviderSupportServiceInNetwork(network.getId(), Service.UserData, getProvider());

        if (!dhcpEnabled && !dnsEnabled && !userdataEnabled) {
            logger.debug("completeAggregatedExecution: no DHCP/DNS/UserData service for network={}, skipping", network);
            return true;
        }

        // Query all active User-VM NICs on this network
        List<NicVO> nics = nicDao.listByNetworkIdAndType(network.getId(), VirtualMachine.Type.User);
        if (CollectionUtils.isEmpty(nics)) {
            logger.debug("completeAggregatedExecution: no user VM NICs on network={}, skipping", network);
            return true;
        }

        logger.info("completeAggregatedExecution: building batch restore for {} VMs on network={}",
                nics.size(), network);

        JsonObject restoreData = buildRestoreNetworkData(network, nics, dhcpEnabled, dnsEnabled, userdataEnabled);

        JsonObject payload = new JsonObject();
        addNetworkToPayload(payload, network);
        addExtensionIpToPayload(payload, network);
        addNetworkDnsToPayload(payload, network);
        payload.add("restore_data", restoreData);

        return executeScript(network, CMD_RESTORE_NETWORK, payload);
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
     *         { "dir": "userdata", "file": "user-data", "content": "<plain text>" },
     *         { "dir": "meta-data", "file": "instance-id", "content": "<plain text>" },
     *         ...
     *       ]
     *     },
     *     ...
     *   ]
     * }
     * </pre>
     *
     * <p>Each {@code vm_data} entry has its {@code content} as a plain UTF-8 string,
     * matching the encoding used by the per-VM {@code save-vm-data} command.</p>
     */
    private JsonObject buildRestoreNetworkData(Network network, List<NicVO> nics,
            boolean dhcpEnabled, boolean dnsEnabled, boolean userdataEnabled) {

        Map<Long, String> offeringNameCache = new HashMap<>();

        JsonObject root = new JsonObject();
        root.addProperty("dhcp_enabled", dhcpEnabled);
        root.addProperty("dns_enabled", dnsEnabled);
        root.addProperty("userdata_enabled", userdataEnabled);
        JsonArray vmsArray = new JsonArray();

        for (NicVO nic : nics) {
            if (nic.getState() != Nic.State.Reserved && nic.getState() != Nic.State.Allocated) {
                continue;
            }
            if (nic.getIPv4Address() == null || nic.getMacAddress() == null) {
                continue;
            }

            long instanceId = nic.getInstanceId();

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
                        logger.debug("Could not fetch SSH key for VM {}: {}", userVm, e.getMessage());
                    }

                    // Is Windows?
                    boolean isWindows = false;
                    try {
                        isWindows = guestOSCategoryDao
                                .findByIdIncludingRemoved(guestOSDao.findByIdIncludingRemoved(userVm.getGuestOSId()).getCategoryId())
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
                    logger.warn("Could not generate vmData for VM {} on network {}: {}", userVm, network, e.getMessage());
                }
            }

            JsonObject vmObj = new JsonObject();
            addNetworkDnsToPayload(vmObj, network);
            vmObj.addProperty("ip", nic.getIPv4Address());
            vmObj.addProperty("ip6_address", safeStr(nic.getIPv6Address()));
            vmObj.addProperty("mac", nic.getMacAddress());
            vmObj.addProperty("nic_uuid", safeStr(nic.getUuid()));
            vmObj.addProperty("hostname", safeStr(userVm.getHostName()));
            vmObj.addProperty("device_id", String.valueOf(nic.getDeviceId()));
            vmObj.addProperty("default_nic", nic.isDefaultNic());

            JsonArray vmDataArray = new JsonArray();
            if (CollectionUtils.isNotEmpty(vmData)) {
                for (String[] entry : vmData) {
                    String dir     = entry[NetworkModel.CONFIGDATA_DIR];
                    String file    = entry[NetworkModel.CONFIGDATA_FILE];
                    String content = entry.length > NetworkModel.CONFIGDATA_CONTENT
                            ? entry[NetworkModel.CONFIGDATA_CONTENT] : null;
                    if (content == null) content = "";

                    String contentStr;
                    if (NetworkModel.USERDATA_DIR.equals(dir) && NetworkModel.USERDATA_FILE.equals(file)) {
                        try {
                            contentStr = new String(Base64.getDecoder().decode(content), StandardCharsets.UTF_8);
                        } catch (Exception e) {
                            contentStr = content;
                        }
                    } else {
                        contentStr = content;
                    }

                    JsonObject entryObj = new JsonObject();
                    entryObj.addProperty("dir", dir);
                    entryObj.addProperty("file", file);
                    entryObj.addProperty("content", contentStr);
                    vmDataArray.add(entryObj);
                }
            }
            vmObj.add("vm_data", vmDataArray);
            vmsArray.add(vmObj);
        }

        root.add("vms", vmsArray);
        return root;
    }

    // ---- VpcProvider ----

    /**
     * Finds the extension + physical-network pair for the given VPC by scanning the
     * physical networks in the VPC's zone for a registered NetworkOrchestrator extension.
     * Returns {@code null} when no suitable extension is found.
     */
    protected Pair<Long, Extension> resolveExtensionForVpc(Vpc vpc) {
        List<PhysicalNetworkVO> physNetworks;
        List<NetworkVO> networks = networkDao.listByVpc(vpc.getId());
        if (CollectionUtils.isNotEmpty(networks)) {
            physNetworks = new ArrayList<>();
            for (NetworkVO network : networks) {
                PhysicalNetworkVO pn = physicalNetworkDao.findById(network.getPhysicalNetworkId());
                if (pn != null && !physNetworks.contains(pn)) {
                    physNetworks.add(pn);
                }
            }
        } else {
            physNetworks = physicalNetworkDao.listByZoneAndTrafficType(vpc.getZoneId(), Networks.TrafficType.Guest);
            if (CollectionUtils.isEmpty(physNetworks)) {
                return null;
            }
        }
        for (PhysicalNetworkVO pn : physNetworks) {
            Extension ext = extensionHelper.getExtensionForPhysicalNetworkAndProvider(pn.getId(), providerName);
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
        File extensionFile = new File(extensionPath);
        if (extensionFile.isFile() && extensionFile.canExecute()) {
            return extensionFile;
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

        logger.info("Ensuring extension device for VPC {} (current={})", vpc, currentDetails);

        Pair<Long, Extension> physNetAndExt = resolveExtensionForVpc(vpc);
        if (physNetAndExt == null) {
            logger.warn("ensureExtensionDetails(vpc): no extension found for VPC {} zone {}",
                    vpc, vpc.getZoneId());
            return;
        }
        JsonObject argsPayload = new JsonObject();
        argsPayload.addProperty("vpc_id", String.valueOf(vpc.getId()));
        argsPayload.addProperty("zone_id", String.valueOf(vpc.getZoneId()));
        argsPayload.addProperty("current_details", currentDetails);

        try {
            Pair<Integer, String> result = executeVpcScriptAndReturnOutput(vpc, CMD_ENSURE_NETWORK_DEVICE, argsPayload);
            String output = result.second() != null ? result.second() : "";

            if (result.first() != EXIT_CODE_SUCCESS) {
                logger.warn("ensure-network-device exited {} for VPC {} — keeping current details",
                        -1, vpc);
                if ("{}".equals(currentDetails)) {
                    vpcDetailsDao.addDetail(vpc.getId(), NETWORK_DETAIL_EXTENSION_DETAILS, "{}", false);
                }
                return;
            }
            if (output.isEmpty()) {
                output = "{}".equals(currentDetails) ? "{}" : currentDetails;
            }
            if (!output.equals(currentDetails)) {
                logger.info("VPC extension device updated for VPC {}: {}", vpc, output);
                vpcDetailsDao.addDetail(vpc.getId(), NETWORK_DETAIL_EXTENSION_DETAILS, output, false);
            } else {
                logger.debug("VPC extension device unchanged for VPC {}: {}", vpc, output);
            }
        } catch (Exception e) {
            logger.warn("Failed ensure-network-device for VPC {}: {}", vpc, e.getMessage());
            if ("{}".equals(currentDetails)) {
                vpcDetailsDao.addDetail(vpc.getId(), NETWORK_DETAIL_EXTENSION_DETAILS, "{}", false);
            }
        }
    }

    /**
     * Executes the extension script for a VPC-level command (no tier network required).
     * Uses VPC-level details from {@code vpc_details}.
     */
    protected boolean executeVpcScript(Vpc vpc, String command, JsonObject argsPayload) {
        return executeVpcScriptAndReturnOutput(vpc, command, argsPayload).first() == EXIT_CODE_SUCCESS;
    }

    protected Pair<Integer, String> executeVpcScriptAndReturnOutput(Vpc vpc, String command, JsonObject argsPayload) {
        Pair<Long, Extension> physNetAndExt = resolveExtensionForVpc(vpc);
        if (physNetAndExt == null) {
            logger.warn("executeVpcScript: no extension found for VPC {} zone {}", vpc, vpc.getZoneId());
            return new Pair<>(EXIT_CODE_FAILURE, "No extension found for VPC " + vpc.getUuid());
        }
        Long physicalNetworkId = physNetAndExt.first();
        Extension extension = physNetAndExt.second();
        File scriptFile = resolveScriptFileForVpc(physicalNetworkId, extension);
        try {
            JsonObject payload = buildVpcScriptPayload(vpc, argsPayload, physicalNetworkId, extension);
            return executeScriptWithFilePayload(scriptFile, command, payload, "VPC extension");
        } catch (Exception e) {
            logger.error("Failed to execute VPC extension script {}: {}", command, e.getMessage(), e);
            throw new CloudRuntimeException("Failed to execute VPC extension script: " + command, e);
        }
    }

    private JsonObject buildVpcScriptPayload(Vpc vpc, JsonObject argsPayload, Long physicalNetworkId, Extension extension) {
        JsonObject payload = new JsonObject();
        payload.add(ARG_PHYSICAL_NETWORK_EXTENSION_DETAILS,
                buildPhysicalNetworkExtensionDetailsPayload(physicalNetworkId, extension));
        payload.add(ARG_NETWORK_EXTENSION_DETAILS, buildVpcExtensionDetailsPayload(vpc.getId()));
        payload.add(ARG_PAYLOAD, argsPayload != null ? argsPayload : new JsonObject());
        return payload;
    }

    protected PublicIpAddress getVpcSourceNatIp(long vpcId) {
        final List<IPAddressVO> ips = ipAddressDao.listByAssociatedVpc(vpcId, true);
        if (CollectionUtils.isEmpty(ips)) {
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
        JsonObject implPayload = new JsonObject();
        implPayload.addProperty("vpc_id", String.valueOf(vpc.getId()));
        implPayload.addProperty("vpc_cidr", safeStr(vpc.getCidr()));

        // Include source NAT IP if already allocated, so the script can set up the
        // VPC-level SNAT rule for the entire VPC CIDR.
        final PublicIpAddress sourceNatIp = getVpcSourceNatIp(vpc.getId());
        if (sourceNatIp != null) {
            addPublicIpToPayload(implPayload, sourceNatIp.getId(), true);
        }

        return executeVpcScript(vpc, CMD_IMPLEMENT_VPC, implPayload);
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

                final JsonObject payload = new JsonObject();
                addNetworkToPayload(payload, network);

                final boolean tierResult = executeScript(network, CMD_DESTROY_NETWORK, payload);
                result = result && tierResult;
            }
        }

        // Remove the VPC namespace and VPC-level details regardless of tier result.
        JsonObject vpcPayload = new JsonObject();
        vpcPayload.addProperty("vpc_id", String.valueOf(vpc.getId()));
        boolean vpcResult = executeVpcScript(vpc, CMD_SHUTDOWN_VPC, vpcPayload);
        if (vpcResult) {
            try {
                vpcDetailsDao.removeDetail(vpc.getId(), NETWORK_DETAIL_EXTENSION_DETAILS);
            } catch (Exception e) {
                logger.warn("Failed to remove VPC extension details for VPC {}: {}", vpc, e.getMessage());
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

        final JsonObject payload = new JsonObject();
        payload.addProperty("vpc_id", String.valueOf(vpc.getId()));
        payload.addProperty("vpc_cidr", safeStr(vpc.getCidr()));
        addPublicIpToPayload(payload, address.getId(), true);

        final boolean result = executeVpcScript(vpc, CMD_UPDATE_VPC_SOURCE_NAT_IP, payload);
        if (!result) {
            logger.warn("updateVpcSourceNatIp: failed to update source NAT IP for VPC {} to {}",
                    vpc, address.getAddress().addr());
        }
        return result;
    }

    /**
     * Applies VPC network ACL rules for a VPC tier network via the script's
     * {@code apply-network-acl} command.  Rules are serialised as a JSON array
     * and passed via a temporary payload file.
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

        logger.info("applyNetworkACLs: network={} activeRules={}", config, activeRules.size());

        JsonArray aclRules = buildAclRulesArray(activeRules);

        JsonObject payload = new JsonObject();
        addNetworkToPayload(payload, config);
        payload.add("acl_rules", aclRules);

        boolean result = executeScript(config, CMD_APPLY_NETWORK_ACL, payload);
        if (!result) {
            throw new ResourceUnavailableException(
                    "Failed to apply network ACL rules for network " + config.getUuid(),
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
        if (CollectionUtils.isEmpty(networks)) {
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
                JsonArray aclRules = buildAclRulesArray(activeRules);

                JsonObject payload = new JsonObject();
                addNetworkToPayload(payload, network);
                payload.add("acl_rules", aclRules);

                boolean r = executeScript(network, CMD_APPLY_NETWORK_ACL, payload);
                result = result && r;
            } catch (Exception e) {
                logger.warn("reorderAclRules: failed for network {}: {}", network, e.getMessage());
                result = false;
            }
        }
        return result;
    }

    /**
     * Serialises a list of {@link NetworkACLItem}s to a {@link JsonArray}
     * suitable for passing to the {@code apply-network-acl} script command.
     * Rules are sorted by their number (priority order).
     */
    private JsonArray buildAclRulesArray(List<? extends NetworkACLItem> rules) {
        JsonArray array = new JsonArray();
        List<? extends NetworkACLItem> sorted = rules.stream()
                .sorted(java.util.Comparator.comparingInt(NetworkACLItem::getNumber))
                .collect(Collectors.toList());
        for (NetworkACLItem rule : sorted) {
            JsonObject ruleObj = buildAclRuleObject(rule);
            JsonArray sourceCidrsArray = new JsonArray();
            List<String> sourceCidrs = rule.getSourceCidrList();
            if (CollectionUtils.isNotEmpty(sourceCidrs)) {
                for (String cidr : sourceCidrs) sourceCidrsArray.add(cidr);
            }
            ruleObj.add("sourceCidrs", sourceCidrsArray);
            array.add(ruleObj);
        }
        return array;
    }

    private JsonObject buildAclRuleObject(NetworkACLItem rule) {
        JsonObject ruleObj = new JsonObject();
        ruleObj.addProperty("number", rule.getNumber());
        ruleObj.addProperty("action", rule.getAction().name().toLowerCase());
        ruleObj.addProperty("trafficType", rule.getTrafficType().name().toLowerCase());
        ruleObj.addProperty("protocol", safeStr(rule.getProtocol()));
        if (rule.getSourcePortStart() != null) ruleObj.addProperty("portStart", rule.getSourcePortStart());
        if (rule.getSourcePortEnd() != null) ruleObj.addProperty("portEnd", rule.getSourcePortEnd());
        if (rule.getIcmpType() != null) ruleObj.addProperty("icmpType", rule.getIcmpType());
        if (rule.getIcmpCode() != null) ruleObj.addProperty("icmpCode", rule.getIcmpCode());
        return ruleObj;
    }
}
