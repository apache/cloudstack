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
package org.apache.cloudstack.network.tungsten.service;

import com.cloud.utils.Pair;
import com.cloud.utils.TungstenUtils;
import com.cloud.utils.exception.CloudRuntimeException;
import com.cloud.utils.net.NetUtils;
import net.juniper.tungsten.api.ApiConnector;
import net.juniper.tungsten.api.ApiObjectBase;
import net.juniper.tungsten.api.ApiPropertyBase;
import net.juniper.tungsten.api.ObjectReference;
import net.juniper.tungsten.api.Status;
import net.juniper.tungsten.api.types.ActionListType;
import net.juniper.tungsten.api.types.AddressGroup;
import net.juniper.tungsten.api.types.AddressType;
import net.juniper.tungsten.api.types.ApplicationPolicySet;
import net.juniper.tungsten.api.types.ConfigRoot;
import net.juniper.tungsten.api.types.DhcpOptionType;
import net.juniper.tungsten.api.types.DhcpOptionsListType;
import net.juniper.tungsten.api.types.Domain;
import net.juniper.tungsten.api.types.FatFlowProtocols;
import net.juniper.tungsten.api.types.FirewallPolicy;
import net.juniper.tungsten.api.types.FirewallRule;
import net.juniper.tungsten.api.types.FirewallRuleEndpointType;
import net.juniper.tungsten.api.types.FirewallRuleMatchTagsType;
import net.juniper.tungsten.api.types.FirewallSequence;
import net.juniper.tungsten.api.types.FirewallServiceGroupType;
import net.juniper.tungsten.api.types.FirewallServiceType;
import net.juniper.tungsten.api.types.FloatingIp;
import net.juniper.tungsten.api.types.FloatingIpPool;
import net.juniper.tungsten.api.types.GlobalSystemConfig;
import net.juniper.tungsten.api.types.GlobalVrouterConfig;
import net.juniper.tungsten.api.types.InstanceIp;
import net.juniper.tungsten.api.types.IpamSubnetType;
import net.juniper.tungsten.api.types.KeyValuePairs;
import net.juniper.tungsten.api.types.Loadbalancer;
import net.juniper.tungsten.api.types.LoadbalancerHealthmonitor;
import net.juniper.tungsten.api.types.LoadbalancerHealthmonitorType;
import net.juniper.tungsten.api.types.LoadbalancerListener;
import net.juniper.tungsten.api.types.LoadbalancerListenerType;
import net.juniper.tungsten.api.types.LoadbalancerMember;
import net.juniper.tungsten.api.types.LoadbalancerMemberType;
import net.juniper.tungsten.api.types.LoadbalancerPool;
import net.juniper.tungsten.api.types.LoadbalancerPoolType;
import net.juniper.tungsten.api.types.LoadbalancerType;
import net.juniper.tungsten.api.types.LogicalRouter;
import net.juniper.tungsten.api.types.MacAddressesType;
import net.juniper.tungsten.api.types.NetworkIpam;
import net.juniper.tungsten.api.types.NetworkPolicy;
import net.juniper.tungsten.api.types.PolicyEntriesType;
import net.juniper.tungsten.api.types.PolicyManagement;
import net.juniper.tungsten.api.types.PolicyRuleType;
import net.juniper.tungsten.api.types.PortMap;
import net.juniper.tungsten.api.types.PortMappings;
import net.juniper.tungsten.api.types.PortType;
import net.juniper.tungsten.api.types.Project;
import net.juniper.tungsten.api.types.SecurityGroup;
import net.juniper.tungsten.api.types.SequenceType;
import net.juniper.tungsten.api.types.ServiceGroup;
import net.juniper.tungsten.api.types.SubnetListType;
import net.juniper.tungsten.api.types.SubnetType;
import net.juniper.tungsten.api.types.Tag;
import net.juniper.tungsten.api.types.TagType;
import net.juniper.tungsten.api.types.VirtualMachine;
import net.juniper.tungsten.api.types.VirtualMachineInterface;
import net.juniper.tungsten.api.types.VirtualNetwork;
import net.juniper.tungsten.api.types.VirtualNetworkPolicyType;
import net.juniper.tungsten.api.types.VnSubnetsType;
import org.apache.cloudstack.api.ApiErrorCode;
import org.apache.cloudstack.api.ServerApiException;
import org.apache.cloudstack.network.tungsten.model.TungstenLoadBalancerMember;
import org.apache.cloudstack.network.tungsten.model.TungstenRule;
import org.apache.commons.lang3.StringUtils;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

import java.io.IOException;
import java.net.HttpURLConnection;
import java.net.URL;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.Objects;
import java.util.concurrent.TimeUnit;

public class TungstenApi {

    protected Logger logger = LogManager.getLogger(getClass());
    private final Status.ErrorHandler errorHandler = logger::error;

    public static final String TUNGSTEN_DEFAULT_DOMAIN = "default-domain";
    public static final String TUNGSTEN_DEFAULT_PROJECT = "admin";
    public static final String TUNGSTEN_DEFAULT_IPAM = "default-network-ipam";
    public static final String TUNGSTEN_DEFAULT_POLICY_MANAGEMENT = "default-policy-management";
    public static final String TUNGSTEN_GLOBAL_SYSTEM_CONFIG = "default-global-system-config";
    public static final String TUNGSTEN_GLOBAL_VROUTER_CONFIG = "default-global-vrouter-config";
    public static final String TUNGSTEN_LOCAL_SECURITY_GROUP = "local";
    public static final String TUNGSTEN_DEFAULT = "default";

    private String hostname;
    private String port;
    private ApiConnector apiConnector;

    public String getHostname() {
        return hostname;
    }

    public void setHostname(String hostname) {
        this.hostname = hostname;
    }

    public String getPort() {
        return port;
    }

    public void setPort(String port) {
        this.port = port;
    }

    public ApiConnector getApiConnector() {
        return apiConnector;
    }

    public void setApiConnector(ApiConnector apiConnector) {
        this.apiConnector = apiConnector;
    }

    public void checkTungstenProviderConnection() {
        try {
            URL url = new URL("http://" + hostname + ":" + port);
            HttpURLConnection huc = (HttpURLConnection) url.openConnection();

            if (huc.getResponseCode() != 200) {
                throw new ServerApiException(ApiErrorCode.RESOURCE_UNAVAILABLE_ERROR,
                    "There is not a Tungsten-Fabric provider using hostname: " + hostname + " and port: " + port);
            }
        } catch (IOException e) {
            throw new ServerApiException(ApiErrorCode.RESOURCE_UNAVAILABLE_ERROR,
                "There is not a Tungsten-Fabric provider using hostname: " + hostname + " and port: " + port);
        }
    }

    public VirtualNetwork createTungstenNetwork(String uuid, String name, String displayName, String parent,
        boolean routerExternal, boolean shared, String ipPrefix, int ipPrefixLen, String gateway, boolean dhcpEnable,
        String dnsServer, String allocationStart, String allocationEnd, boolean ipFromStart,
        boolean isManagementNetwork, String subnetName) {
        try {
            VirtualNetwork virtualNetwork = (VirtualNetwork) apiConnector.findById(VirtualNetwork.class, uuid);
            if (virtualNetwork != null)
                return virtualNetwork;
            Project project = (Project) apiConnector.findById(Project.class, parent);
            virtualNetwork = new VirtualNetwork();
            if (subnetName != null) {
                NetworkIpam networkIpam = getDefaultProjectNetworkIpam(project);
                VnSubnetsType vnSubnetsType = new VnSubnetsType();
                IpamSubnetType ipamSubnetType = getIpamSubnetType(ipPrefix, ipPrefixLen, gateway, dhcpEnable,
                    ipFromStart, allocationStart, allocationEnd, subnetName, dnsServer);
                vnSubnetsType.addIpamSubnets(ipamSubnetType);
                virtualNetwork.addNetworkIpam(networkIpam, vnSubnetsType);
            }

            if (uuid != null) {
                virtualNetwork.setUuid(uuid);
            }

            virtualNetwork.setName(name);
            virtualNetwork.setDisplayName(displayName);
            virtualNetwork.setParent(project);
            virtualNetwork.setRouterExternal(routerExternal);
            virtualNetwork.setIsShared(shared);

            if (isManagementNetwork) {
                VirtualNetwork fabricNetwork = (VirtualNetwork) apiConnector.findByFQN(VirtualNetwork.class,
                    TungstenUtils.FABRIC_NETWORK_FQN);
                if (fabricNetwork != null) {
                    virtualNetwork.setVirtualNetwork(fabricNetwork);
                }
            }

            Status status = apiConnector.create(virtualNetwork);
            status.ifFailure(errorHandler);
            return (VirtualNetwork) apiConnector.findById(VirtualNetwork.class, virtualNetwork.getUuid());
        } catch (IOException e) {
            return null;
        }
    }

    public VirtualMachine createTungstenVirtualMachine(String vmUuid, String vmName) {
        try {
            VirtualMachine virtualMachine = new VirtualMachine();
            virtualMachine.setName(vmName);
            virtualMachine.setUuid(vmUuid);
            Status status = apiConnector.create(virtualMachine);
            status.ifFailure(errorHandler);
            return (VirtualMachine) apiConnector.findById(VirtualMachine.class, virtualMachine.getUuid());
        } catch (IOException e) {
            logger.error("Unable to create Tungsten-Fabric vm " + vmUuid, e);
            return null;
        }
    }

    public VirtualMachineInterface createTungstenVmInterface(String nicUuid, String nicName, String mac,
        String virtualNetworkUuid, String virtualMachineUuid, String projectUuid, String gateway, boolean defaultNic) {
        VirtualNetwork virtualNetwork = null;
        VirtualMachine virtualMachine = null;
        Project project = null;

        try {
            virtualNetwork = (VirtualNetwork) apiConnector.findById(VirtualNetwork.class, virtualNetworkUuid);
            virtualMachine = (VirtualMachine) apiConnector.findById(VirtualMachine.class, virtualMachineUuid);
            project = (Project) apiConnector.findById(Project.class, projectUuid);
        } catch (IOException e) {
            logger.error("Failed getting the resources needed for virtual machine interface creation from Tungsten-Fabric");
        }

        VirtualMachineInterface virtualMachineInterface = new VirtualMachineInterface();
        try {
            virtualMachineInterface.setUuid(nicUuid);
            virtualMachineInterface.setName(nicName);
            virtualMachineInterface.setVirtualNetwork(virtualNetwork);
            virtualMachineInterface.setVirtualMachine(virtualMachine);
            virtualMachineInterface.setParent(project);
            virtualMachineInterface.setPortSecurityEnabled(false);
            MacAddressesType macAddressesType = new MacAddressesType();
            macAddressesType.addMacAddress(mac);
            virtualMachineInterface.setMacAddresses(macAddressesType);
            if (defaultNic) {
                DhcpOptionsListType dhcpOptionsListType = new DhcpOptionsListType();
                dhcpOptionsListType.addDhcpOption(new DhcpOptionType("3", gateway));
                virtualMachineInterface.setDhcpOptionList(dhcpOptionsListType);
            }
            Status status = apiConnector.create(virtualMachineInterface);
            status.ifFailure(errorHandler);
            return (VirtualMachineInterface) apiConnector.findById(VirtualMachineInterface.class,
                virtualMachineInterface.getUuid());
        } catch (IOException e) {
            logger.error("Failed creating virtual machine interface in Tungsten-Fabric");
            return null;
        }
    }

    public InstanceIp createTungstenInstanceIp(String instanceIpName, String ip, String virtualNetworkUuid,
        String vmInterfaceUuid) {
        VirtualNetwork virtualNetwork;
        VirtualMachineInterface virtualMachineInterface;

        try {
            virtualNetwork = (VirtualNetwork) apiConnector.findById(VirtualNetwork.class, virtualNetworkUuid);
            virtualMachineInterface = (VirtualMachineInterface) apiConnector.findById(VirtualMachineInterface.class,
                vmInterfaceUuid);
        } catch (IOException e) {
            logger.error("Failed getting the resources needed for instance ip creation from Tungsten-Fabric");
            return null;
        }

        try {
            InstanceIp instanceIp = new InstanceIp();
            instanceIp.setName(instanceIpName);
            instanceIp.setVirtualNetwork(virtualNetwork);
            instanceIp.setVirtualMachineInterface(virtualMachineInterface);
            instanceIp.setAddress(ip);
            Status status = apiConnector.create(instanceIp);
            status.ifFailure(errorHandler);
            return (InstanceIp) apiConnector.findById(InstanceIp.class, instanceIp.getUuid());
        } catch (IOException e) {
            logger.error("Failed creating instance ip in Tungsten-Fabric");
            return null;
        }
    }

    public InstanceIp createTungstenInstanceIp(String instanceIpName, String ip, String virtualNetworkUuid,
        String vmInterfaceUuid, String subnetUuid) {
        VirtualNetwork virtualNetwork;
        VirtualMachineInterface virtualMachineInterface;

        try {
            virtualNetwork = (VirtualNetwork) apiConnector.findById(VirtualNetwork.class, virtualNetworkUuid);
            virtualMachineInterface = (VirtualMachineInterface) apiConnector.findById(VirtualMachineInterface.class,
                vmInterfaceUuid);
        } catch (IOException e) {
            logger.error("Failed getting the resources needed for instance ip creation with subnet from Tungsten-Fabric");
            return null;
        }

        try {
            InstanceIp instanceIp = new InstanceIp();
            instanceIp.setName(instanceIpName);
            instanceIp.setVirtualNetwork(virtualNetwork);
            instanceIp.setVirtualMachineInterface(virtualMachineInterface);
            instanceIp.setAddress(ip);
            instanceIp.setSubnetUuid(subnetUuid);
            Status status = apiConnector.create(instanceIp);
            status.ifFailure(errorHandler);
            return (InstanceIp) apiConnector.findById(InstanceIp.class, instanceIp.getUuid());
        } catch (IOException e) {
            logger.error("Failed creating instance ip in Tungsten-Fabric");
            return null;
        }
    }

    public boolean deleteTungstenVmInterface(VirtualMachineInterface vmi) {
        try {
            List<ObjectReference<ApiPropertyBase>> instanceIpORs = vmi.getInstanceIpBackRefs();
            if (instanceIpORs != null) {
                for (ObjectReference<ApiPropertyBase> instanceIpOR : instanceIpORs) {
                    Status status = apiConnector.delete(InstanceIp.class, instanceIpOR.getUuid());
                    status.ifFailure(errorHandler);
                }
            }
            Status status = apiConnector.delete(vmi);
            status.ifFailure(errorHandler);
            return status.isSuccess();
        } catch (IOException e) {
            logger.error("Failed deleting the virtual machine interface from Tungsten-Fabric");
            return false;
        }
    }

    public NetworkIpam getDefaultProjectNetworkIpam(Project project) {
        try {
            List<String> names = new ArrayList<>();
            Domain domain = (Domain) apiConnector.findById(Domain.class, project.getParentUuid());
            names.add(domain.getName());
            names.add(project.getName());
            names.add(TUNGSTEN_DEFAULT_IPAM);
            String ipamUuid = apiConnector.findByName(NetworkIpam.class, names);
            if (ipamUuid == null) {
                NetworkIpam defaultIpam = new NetworkIpam();
                defaultIpam.setName(TUNGSTEN_DEFAULT_IPAM);
                defaultIpam.setParent(project);
                Status status = apiConnector.create(defaultIpam);
                status.ifFailure(errorHandler);
                ipamUuid = defaultIpam.getUuid();
            }
            return (NetworkIpam) apiConnector.findById(NetworkIpam.class, ipamUuid);
        } catch (IOException ex) {
            return null;
        }
    }

    public ApiObjectBase getTungstenObject(Class<? extends ApiObjectBase> aClass, String uuid) {
        try {
            return apiConnector.findById(aClass, uuid);
        } catch (IOException ex) {
            return null;
        }
    }

    public ApiObjectBase getTungstenProjectByFqn(String fqn) {
        try {
            return apiConnector.findByFQN(Project.class, Objects.requireNonNullElse(fqn, TUNGSTEN_DEFAULT_DOMAIN + ":" + TUNGSTEN_DEFAULT_PROJECT));
        } catch (IOException ex) {
            return null;
        }
    }

    public ApiObjectBase getTungstenObjectByName(Class<? extends ApiObjectBase> aClass, List<String> parent,
        String name) {
        try {
            if (parent == null) {
                List<String> names = new ArrayList<>();
                names.add(name);
                String uuid = apiConnector.findByName(aClass, names);
                return apiConnector.findById(aClass, uuid);
            } else {
                List<String> names = new ArrayList<>(parent);
                names.add(name);
                String uuid = apiConnector.findByName(aClass, names);
                return apiConnector.findById(aClass, uuid);
            }
        } catch (IOException ex) {
            return null;
        }
    }

    public ApiObjectBase createTungstenLogicalRouter(String name, String parentUuid, String pubNetworkUuid) {
        try {
            Project project = (Project) apiConnector.findById(Project.class, parentUuid);
            VirtualNetwork virtualNetwork = (VirtualNetwork) apiConnector.findById(VirtualNetwork.class,
                pubNetworkUuid);
            LogicalRouter logicalRouter = (LogicalRouter) apiConnector.find(LogicalRouter.class, project, name);
            if (logicalRouter == null) {
                logicalRouter = new LogicalRouter();
                logicalRouter.setName(name);
                logicalRouter.setParent(project);
                logicalRouter.setVirtualNetwork(virtualNetwork, null);
                Status status = apiConnector.create(logicalRouter);
                status.ifFailure(errorHandler);
                if (status.isSuccess()) {
                    return apiConnector.findById(LogicalRouter.class, logicalRouter.getUuid());
                } else {
                    return null;
                }
            } else {
                return logicalRouter;
            }
        } catch (IOException ex) {
            return null;
        }
    }

    public ApiObjectBase createTungstenGatewayVmi(String name, String projectUuid, String vnUuid) {
        try {
            Project project = (Project) apiConnector.findById(Project.class, projectUuid);
            VirtualNetwork virtualNetwork = (VirtualNetwork) apiConnector.findById(VirtualNetwork.class, vnUuid);
            VirtualMachineInterface virtualMachineInterface = (VirtualMachineInterface) apiConnector.find(
                VirtualMachineInterface.class, project, name);

            if (virtualMachineInterface != null) {
                logger.error("interface " + name + " is existed");
                return null;
            }

            virtualMachineInterface = new VirtualMachineInterface();
            virtualMachineInterface.setName(name);
            virtualMachineInterface.setParent(project);
            virtualMachineInterface.setVirtualNetwork(virtualNetwork);
            virtualMachineInterface.setPortSecurityEnabled(false);
            Status status = apiConnector.create(virtualMachineInterface);
            status.ifFailure(errorHandler);
            return apiConnector.findById(VirtualMachineInterface.class, virtualMachineInterface.getUuid());
        } catch (IOException ex) {
            return null;
        }
    }

    public ApiObjectBase createTungstenLbVmi(String name, String projectUuid, String vnUuid) {
        try {
            Project project = (Project) apiConnector.findById(Project.class, projectUuid);
            VirtualNetwork virtualNetwork = (VirtualNetwork) apiConnector.findById(VirtualNetwork.class, vnUuid);
            VirtualMachineInterface virtualMachineInterface = new VirtualMachineInterface();
            virtualMachineInterface.setName(name);
            virtualMachineInterface.setParent(project);
            virtualMachineInterface.setVirtualNetwork(virtualNetwork);
            //add this when tungsten support cloudstack
            virtualMachineInterface.setDeviceOwner("CS:LOADBALANCER");
            virtualMachineInterface.setPortSecurityEnabled(false);
            Status status = apiConnector.create(virtualMachineInterface);
            status.ifFailure(errorHandler);
            return apiConnector.findById(VirtualMachineInterface.class, virtualMachineInterface.getUuid());
        } catch (IOException ex) {
            return null;
        }
    }

    public boolean updateTungstenObject(ApiObjectBase apiObjectBase) {
        try {
            Status status = apiConnector.update(apiObjectBase);
            status.ifFailure(errorHandler);
            return status.isSuccess();
        } catch (IOException ex) {
            return false;
        }
    }

    public ApiObjectBase createTungstenFloatingIpPool(String networkUuid, String fipName) {
        try {
            VirtualNetwork virtualNetwork = (VirtualNetwork) apiConnector.findById(VirtualNetwork.class, networkUuid);
            FloatingIpPool floatingIpPool = (FloatingIpPool) apiConnector.find(FloatingIpPool.class, virtualNetwork,
                fipName);
            if (floatingIpPool == null) {
                floatingIpPool = new FloatingIpPool();
                floatingIpPool.setName(fipName);
                floatingIpPool.setParent(virtualNetwork);
                Status status = apiConnector.create(floatingIpPool);
                status.ifFailure(errorHandler);
                return apiConnector.findById(FloatingIpPool.class, floatingIpPool.getUuid());
            } else {
                return floatingIpPool;
            }
        } catch (IOException e) {
            return null;
        }
    }

    public ApiObjectBase createTungstenFloatingIp(String projectUuid, String networkUuid, String fipName, String name,
        String publicIp) {
        try {
            Project project = (Project) apiConnector.findById(Project.class, projectUuid);
            VirtualNetwork virtualNetwork = (VirtualNetwork) apiConnector.findById(VirtualNetwork.class, networkUuid);
            FloatingIpPool fip = (FloatingIpPool) apiConnector.find(FloatingIpPool.class, virtualNetwork, fipName);
            FloatingIp floatingIp = (FloatingIp) apiConnector.find(FloatingIp.class, fip, name);
            if (floatingIp == null) {
                floatingIp = new FloatingIp();
                floatingIp.setName(name);
                floatingIp.setParent(fip);
                floatingIp.setProject(project);
                floatingIp.setAddress(publicIp);
                Status status = apiConnector.create(floatingIp);
                status.ifFailure(errorHandler);
                return apiConnector.findById(FloatingIp.class, floatingIp.getUuid());
            } else {
                return floatingIp;
            }
        } catch (IOException e) {
            return null;
        }
    }

    public boolean assignTungstenFloatingIp(String networkUuid, String vmiUuid, String fipName, String name,
        String privateIp) {
        try {
            VirtualNetwork virtualNetwork = (VirtualNetwork) apiConnector.findById(VirtualNetwork.class, networkUuid);
            FloatingIpPool fip = (FloatingIpPool) apiConnector.find(FloatingIpPool.class, virtualNetwork, fipName);
            VirtualMachineInterface vmi = (VirtualMachineInterface) apiConnector.findById(VirtualMachineInterface.class,
                vmiUuid);
            FloatingIp floatingIp = (FloatingIp) apiConnector.find(FloatingIp.class, fip, name);
            floatingIp.setVirtualMachineInterface(vmi);
            floatingIp.setFixedIpAddress(privateIp);
            Status status = apiConnector.update(floatingIp);
            status.ifFailure(errorHandler);
            return status.isSuccess();
        } catch (IOException e) {
            return false;
        }
    }

    public boolean releaseTungstenFloatingIp(String networkUuid, String fipName, String name) {
        try {
            VirtualNetwork virtualNetwork = (VirtualNetwork) apiConnector.findById(VirtualNetwork.class, networkUuid);
            FloatingIpPool fip = (FloatingIpPool) apiConnector.find(FloatingIpPool.class, virtualNetwork, fipName);
            FloatingIp floatingIp = (FloatingIp) apiConnector.find(FloatingIp.class, fip, name);
            if (floatingIp == null) {
                return true;
            }
            floatingIp.clearVirtualMachineInterface();
            floatingIp.setFixedIpAddress(null);
            Status status = apiConnector.update(floatingIp);
            status.ifFailure(errorHandler);
            return status.isSuccess();
        } catch (IOException e) {
            return false;
        }
    }

    public String getTungstenNatIp(String projectUuid, String logicalRouterUuid) {
        // wait for service instance created
        try {
            TimeUnit.SECONDS.sleep(1);
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
            logger.error("can not delay for service instance create");
        }

        try {
            Project project = (Project) apiConnector.findById(Project.class, projectUuid);
            List<InstanceIp> instanceIps = (List<InstanceIp>) apiConnector.list(InstanceIp.class, null);
            if (instanceIps != null) {
                for (InstanceIp instanceIp : instanceIps) {
                    if (instanceIp.getQualifiedName()
                        .get(0)
                        .startsWith(
                            TungstenUtils.getSnatNetworkStartName(project.getQualifiedName(), logicalRouterUuid))
                        && instanceIp.getQualifiedName().get(0).endsWith(TungstenUtils.SNAT_NETWORK_END_NAME)) {
                        InstanceIp natInstanceIp = (InstanceIp) apiConnector.findById(InstanceIp.class,
                            instanceIp.getUuid());
                        if (natInstanceIp != null) {
                            return natInstanceIp.getAddress();
                        }
                    }
                }
            }
            return null;
        } catch (IOException e) {
            return null;
        }
    }

    public ApiObjectBase createOrUpdateTungstenNetworkPolicy(String name, String projectUuid,
        List<TungstenRule> tungstenRuleList) {
        try {
            Project project = (Project) getTungstenObject(Project.class, projectUuid);
            NetworkPolicy networkPolicy = (NetworkPolicy) apiConnector.find(NetworkPolicy.class, project, name);
            PolicyEntriesType policyEntriesType;
            if (networkPolicy == null) {
                policyEntriesType = new PolicyEntriesType();

                getPolicyEntriesType(tungstenRuleList, policyEntriesType);

                networkPolicy = new NetworkPolicy();
                networkPolicy.setName(name);
                networkPolicy.setParent(project);
                networkPolicy.setEntries(policyEntriesType);

                Status status = apiConnector.create(networkPolicy);
                status.ifFailure(errorHandler);
            } else {
                policyEntriesType = networkPolicy.getEntries();
                if (policyEntriesType == null) {
                    policyEntriesType = new PolicyEntriesType();
                    networkPolicy.setEntries(policyEntriesType);
                }

                getPolicyEntriesType(tungstenRuleList, policyEntriesType);

                Status status = apiConnector.update(networkPolicy);
                status.ifFailure(errorHandler);
            }
            return apiConnector.findById(NetworkPolicy.class, networkPolicy.getUuid());
        } catch (IOException e) {
            return null;
        }
    }

    public ApiObjectBase applyTungstenNetworkPolicy(String policyUuid, String networkUuid, int majorSequence,
        int minorSequence) {
        try {
            NetworkPolicy networkPolicy = (NetworkPolicy) apiConnector.findById(NetworkPolicy.class, policyUuid);
            VirtualNetwork network = (VirtualNetwork) apiConnector.findById(VirtualNetwork.class, networkUuid);

            if (networkPolicy == null || network == null) {
                return null;
            }

            List<ObjectReference<VirtualNetworkPolicyType>> objectReferenceList = network.getNetworkPolicy();
            if (objectReferenceList != null) {
                for (ObjectReference<VirtualNetworkPolicyType> objectReference : objectReferenceList) {
                    if (objectReference.getUuid().equals(networkPolicy.getUuid())) {
                        return networkPolicy;
                    }
                }
            }

            network.addNetworkPolicy(networkPolicy,
                new VirtualNetworkPolicyType(new SequenceType(majorSequence, minorSequence)));
            network.setPerms2(null);
            Status status = apiConnector.update(network);
            status.ifFailure(errorHandler);
            return apiConnector.findById(NetworkPolicy.class, policyUuid);
        } catch (IOException e) {
            return null;
        }
    }

    public ApiObjectBase getTungstenFabricNetwork() {
        try {
            return apiConnector.findByFQN(VirtualNetwork.class, TungstenUtils.FABRIC_NETWORK_FQN);
        } catch (IOException e) {
            return null;
        }
    }

    public ApiObjectBase createTungstenDomain(String domainName, String domainUuid) {
        try {
            Domain domain = (Domain) apiConnector.findById(Domain.class, domainUuid);
            if (domain != null)
                return domain;
            //create tungsten domain
            Domain tungstenDomain = new Domain();
            tungstenDomain.setDisplayName(domainName);
            tungstenDomain.setName(domainName);
            tungstenDomain.setUuid(domainUuid);
            Status status = apiConnector.create(tungstenDomain);
            status.ifFailure(errorHandler);
            if (status.isSuccess()) {
                // create default project in tungsten for this newly created domain
                Project tungstenDefaultProject = new Project();
                tungstenDefaultProject.setDisplayName(TUNGSTEN_DEFAULT_PROJECT);
                tungstenDefaultProject.setName(TUNGSTEN_DEFAULT_PROJECT);
                tungstenDefaultProject.setParent(tungstenDomain);
                Status defaultProjectStatus = apiConnector.create(tungstenDefaultProject);
                defaultProjectStatus.ifFailure(errorHandler);
            }
            return getTungstenObject(Domain.class, tungstenDomain.getUuid());
        } catch (IOException e) {
            throw new CloudRuntimeException("Failed creating domain resource in Tungsten-Fabric.");
        }
    }

    public ApiObjectBase createTungstenProject(String projectName, String projectUuid, String domainUuid,
        String domainName) {
        try {
            Project project = (Project) getTungstenObject(Project.class, projectUuid);
            if (project != null)
                return project;
            //Create tungsten project
            Project tungstenProject = new Project();
            tungstenProject.setDisplayName(projectName);
            tungstenProject.setName(projectName);
            tungstenProject.setUuid(projectUuid);
            Domain tungstenDomain;

            if (domainUuid == null && domainName == null)
                tungstenDomain = getDefaultTungstenDomain();
            else {
                tungstenDomain = (Domain) getTungstenObject(Domain.class, domainUuid);
                if (tungstenDomain == null)
                    tungstenDomain = (Domain) createTungstenDomain(domainName, domainUuid);
            }
            tungstenProject.setParent(tungstenDomain);
            apiConnector.create(tungstenProject);
            return getTungstenObject(Project.class, tungstenProject.getUuid());
        } catch (IOException e) {
            throw new CloudRuntimeException("Failed creating project resource in Tungsten-Fabric.");
        }
    }

    public boolean deleteTungstenDomain(String domainUuid) {
        try {
            Domain domain = (Domain) getTungstenObject(Domain.class, domainUuid);
            //delete the projects of this domain
            for (ObjectReference<ApiPropertyBase> project : domain.getProjects()) {
                apiConnector.delete(Project.class, project.getUuid());
            }
            Status status = apiConnector.delete(Domain.class, domainUuid);
            status.ifFailure(errorHandler);
            return status.isSuccess();
        } catch (IOException e) {
            return false;
        }
    }

    public boolean deleteTungstenProject(String projectUuid) {
        try {
            Project project = (Project) getTungstenObject(Project.class, projectUuid);
            if (project != null) {
                Status status = apiConnector.delete(Project.class, projectUuid);
                status.ifFailure(errorHandler);
                return status.isSuccess();
            }
            return true;
        } catch (IOException e) {
            return false;
        }
    }

    public Domain getDefaultTungstenDomain() throws IOException {
        return (Domain) apiConnector.findByFQN(Domain.class, TUNGSTEN_DEFAULT_DOMAIN);
    }

    public ApiObjectBase createTungstenLoadbalancer(String projectUuid, String lbName, String vmiUuid,
        String subnetUuid, String privateIp) {
        try {
            Project project = (Project) getTungstenObject(Project.class, projectUuid);
            VirtualMachineInterface virtualMachineInterface = (VirtualMachineInterface) apiConnector.findById(
                VirtualMachineInterface.class, vmiUuid);
            LoadbalancerType loadbalancerType = new LoadbalancerType();
            loadbalancerType.setVipSubnetId(subnetUuid);
            loadbalancerType.setVipAddress(privateIp);
            loadbalancerType.setAdminState(true);
            loadbalancerType.setOperatingStatus("ONLINE");
            loadbalancerType.setProvisioningStatus("ACTIVE");

            Loadbalancer loadbalancer = new Loadbalancer();
            loadbalancer.setName(lbName);
            loadbalancer.setParent(project);
            loadbalancer.setProperties(loadbalancerType);
            loadbalancer.setProvider("opencontrail");
            loadbalancer.setVirtualMachineInterface(virtualMachineInterface);
            Status status = apiConnector.create(loadbalancer);
            status.ifFailure(errorHandler);
            if (status.isSuccess()) {
                return apiConnector.findById(Loadbalancer.class, loadbalancer.getUuid());
            } else {
                return null;
            }
        } catch (IOException e) {
            return null;
        }
    }

    public ApiObjectBase createTungstenLoadbalancerListener(String projectUuid, String loadBalancerUuid, String name,
        String protocol, int port) {
        try {
            Project project = (Project) getTungstenObject(Project.class, projectUuid);
            Loadbalancer loadbalancer = (Loadbalancer) apiConnector.findById(Loadbalancer.class, loadBalancerUuid);
            LoadbalancerListenerType loadbalancerListenerType = new LoadbalancerListenerType();
            loadbalancerListenerType.setConnectionLimit(-1);
            loadbalancerListenerType.setAdminState(true);
            loadbalancerListenerType.setProtocol(protocol);
            loadbalancerListenerType.setProtocolPort(port);

            LoadbalancerListener loadbalancerListener = new LoadbalancerListener();
            loadbalancerListener.setName(name);
            loadbalancerListener.setParent(project);
            loadbalancerListener.setLoadbalancer(loadbalancer);
            loadbalancerListener.setProperties(loadbalancerListenerType);
            Status status = apiConnector.create(loadbalancerListener);
            status.ifFailure(errorHandler);
            if (status.isSuccess()) {
                return apiConnector.findById(LoadbalancerListener.class, loadbalancerListener.getUuid());
            } else {
                return null;
            }
        } catch (IOException e) {
            return null;
        }
    }

    public ApiObjectBase createTungstenLoadbalancerHealthMonitor(String projectUuid, String name, String monitorType,
        int maxRetries, int delay, int timeout, String httpMethod, String urlPath, String expectedCode) {
        try {
            Project project = (Project) getTungstenObject(Project.class, projectUuid);
            LoadbalancerHealthmonitorType loadbalancerHealthmonitorType = new LoadbalancerHealthmonitorType();
            loadbalancerHealthmonitorType.setMonitorType(monitorType);
            loadbalancerHealthmonitorType.setMaxRetries(maxRetries);
            loadbalancerHealthmonitorType.setDelay(delay);
            loadbalancerHealthmonitorType.setAdminState(true);
            loadbalancerHealthmonitorType.setTimeout(timeout);
            if (monitorType.equals("HTTP")) {
                loadbalancerHealthmonitorType.setHttpMethod(httpMethod);
                loadbalancerHealthmonitorType.setUrlPath(urlPath);
                loadbalancerHealthmonitorType.setExpectedCodes(expectedCode);
            }

            LoadbalancerHealthmonitor loadbalancerHealthmonitor = new LoadbalancerHealthmonitor();
            loadbalancerHealthmonitor.setName(name);
            loadbalancerHealthmonitor.setParent(project);
            loadbalancerHealthmonitor.setProperties(loadbalancerHealthmonitorType);
            Status status = apiConnector.create(loadbalancerHealthmonitor);
            status.ifFailure(errorHandler);
            if (status.isSuccess()) {
                return apiConnector.findById(LoadbalancerHealthmonitor.class, loadbalancerHealthmonitor.getUuid());
            } else {
                return null;
            }
        } catch (IOException e) {
            return null;
        }
    }

    public ApiObjectBase createTungstenLoadbalancerPool(String projectUuid, String loadbalancerlistenerUuid,
        String loadbalancerHealthmonitorUuid, String name, String method, String protocol) {
        try {
            Project project = (Project) getTungstenObject(Project.class, projectUuid);
            LoadbalancerListener loadbalancerListener = (LoadbalancerListener) apiConnector.findById(
                LoadbalancerListener.class, loadbalancerlistenerUuid);
            LoadbalancerHealthmonitor loadbalancerHealthmonitor = (LoadbalancerHealthmonitor) apiConnector.findById(
                LoadbalancerHealthmonitor.class, loadbalancerHealthmonitorUuid);
            LoadbalancerPoolType loadbalancerPoolType = new LoadbalancerPoolType();
            loadbalancerPoolType.setLoadbalancerMethod(method);
            loadbalancerPoolType.setProtocol(protocol);
            loadbalancerPoolType.setAdminState(true);

            LoadbalancerPool loadbalancerPool = new LoadbalancerPool();
            loadbalancerPool.setName(name);
            loadbalancerPool.setParent(project);
            loadbalancerPool.setLoadbalancerListener(loadbalancerListener);
            loadbalancerPool.setLoadbalancerHealthmonitor(loadbalancerHealthmonitor);
            loadbalancerPool.setProperties(loadbalancerPoolType);
            Status status = apiConnector.create(loadbalancerPool);
            status.ifFailure(errorHandler);
            if (status.isSuccess()) {
                return apiConnector.findById(LoadbalancerPool.class, loadbalancerPool.getUuid());
            } else {
                return null;
            }
        } catch (IOException e) {
            return null;
        }
    }

    public ApiObjectBase createTungstenLoadbalancerMember(String loadbalancerPoolUuid, String name, String address,
        String subnetUuid, int port, int weight) {
        try {
            LoadbalancerPool loadbalancerPool = (LoadbalancerPool) apiConnector.findById(LoadbalancerPool.class,
                loadbalancerPoolUuid);
            LoadbalancerMemberType loadbalancerMemberType = new LoadbalancerMemberType();
            loadbalancerMemberType.setAddress(address);
            loadbalancerMemberType.setAdminState(true);
            loadbalancerMemberType.setProtocolPort(port);
            loadbalancerMemberType.setSubnetId(subnetUuid);
            loadbalancerMemberType.setWeight(weight);

            LoadbalancerMember loadbalancerMember = new LoadbalancerMember();
            loadbalancerMember.setName(name);
            loadbalancerMember.setParent(loadbalancerPool);
            loadbalancerMember.setProperties(loadbalancerMemberType);
            Status status = apiConnector.create(loadbalancerMember);
            status.ifFailure(errorHandler);
            if (status.isSuccess()) {
                return apiConnector.findById(LoadbalancerMember.class, loadbalancerMember.getUuid());
            } else {
                return null;
            }
        } catch (IOException e) {
            return null;
        }
    }

    public boolean updateLoadBalancerMember(String projectUuid, String lbPoolName,
        List<TungstenLoadBalancerMember> listTungstenLoadBalancerMember, String subnetUuid) {
        try {
            Project project = (Project) getTungstenObject(Project.class, projectUuid);
            LoadbalancerPool loadbalancerPool = (LoadbalancerPool) apiConnector.find(LoadbalancerPool.class, project,
                lbPoolName);
            List<ObjectReference<ApiPropertyBase>> listMember = loadbalancerPool.getLoadbalancerMembers();

            if (listMember != null) {
                for (ObjectReference<ApiPropertyBase> member : listMember) {
                    Status status = apiConnector.delete(LoadbalancerMember.class, member.getUuid());
                    status.ifFailure(errorHandler);
                    if (!status.isSuccess()) {
                        return false;
                    }
                }
            }

            for (TungstenLoadBalancerMember tungstenLoadBalancerMember : listTungstenLoadBalancerMember) {
                LoadbalancerMemberType loadbalancerMemberType = new LoadbalancerMemberType();
                loadbalancerMemberType.setAddress(tungstenLoadBalancerMember.getIpAddress());
                loadbalancerMemberType.setProtocolPort(tungstenLoadBalancerMember.getPort());
                loadbalancerMemberType.setSubnetId(subnetUuid);
                loadbalancerMemberType.setAdminState(true);
                loadbalancerMemberType.setWeight(tungstenLoadBalancerMember.getWeight());
                LoadbalancerMember loadbalancerMember = new LoadbalancerMember();
                loadbalancerMember.setName(tungstenLoadBalancerMember.getName());
                loadbalancerMember.setParent(loadbalancerPool);
                loadbalancerMember.setProperties(loadbalancerMemberType);
                Status status = apiConnector.create(loadbalancerMember);
                status.ifFailure(errorHandler);
                if (!status.isSuccess()) {
                    return false;
                }
            }
            return true;
        } catch (IOException e) {
            return false;
        }
    }

    public boolean updateLoadBalancerPool(String projectUuid, String lbPoolName, String lbMethod,
        String lbSessionPersistence, String lbPersistenceCookieName, String lbProtocol, boolean statEnable,
        String statsPort, String statsUri, String statsAuth) {
        try {
            Project project = (Project) getTungstenObject(Project.class, projectUuid);
            LoadbalancerPool loadbalancerPool = (LoadbalancerPool) apiConnector.find(LoadbalancerPool.class, project,
                lbPoolName);
            LoadbalancerPoolType loadbalancerPoolType = loadbalancerPool.getProperties();
            if (lbMethod != null) {
                loadbalancerPoolType.setLoadbalancerMethod(lbMethod);
            }
            if (lbSessionPersistence != null) {
                loadbalancerPoolType.setSessionPersistence(lbSessionPersistence);
            }
            if (lbPersistenceCookieName != null) {
                loadbalancerPoolType.setPersistenceCookieName(lbPersistenceCookieName);
            }
            if (lbProtocol != null) {
                loadbalancerPoolType.setProtocol(lbProtocol);
            }

            if (statEnable) {
                KeyValuePairs keyValuePairs = new KeyValuePairs();
                keyValuePairs.addKeyValuePair("stats_enable", "enable");
                keyValuePairs.addKeyValuePair("stats_port", statsPort);
                keyValuePairs.addKeyValuePair("stats_realm", "Haproxy Statistics");
                keyValuePairs.addKeyValuePair("stats_uri", statsUri);
                keyValuePairs.addKeyValuePair("stats_auth", statsAuth);
                loadbalancerPool.setCustomAttributes(keyValuePairs);
            }

            Status status = apiConnector.update(loadbalancerPool);
            status.ifFailure(errorHandler);
            return status.isSuccess();
        } catch (IOException e) {
            return false;
        }
    }

    public boolean updateLoadBalancerListener(String projectUuid, String listenerName, String protocol, int port,
        String url) {
        try {
            Project project = (Project) getTungstenObject(Project.class, projectUuid);
            LoadbalancerListener loadbalancerListener = (LoadbalancerListener) apiConnector.find(
                LoadbalancerListener.class, project, listenerName);
            LoadbalancerListenerType loadbalancerListenerType = loadbalancerListener.getProperties();
            loadbalancerListenerType.setProtocolPort(port);
            loadbalancerListenerType.setProtocol(protocol);
            loadbalancerListenerType.setDefaultTlsContainer(url);
            Status status = apiConnector.update(loadbalancerListener);
            status.ifFailure(errorHandler);
            return status.isSuccess();
        } catch (IOException e) {
            return false;
        }
    }

    public boolean updateLoadBalancerHealthMonitor(String projectUuid, String healthMonitorName, String type,
        int retry, int timeout, int interval, String httpMethod, String expectedCode, String urlPath) {
        try {
            Project project = (Project) getTungstenObject(Project.class, projectUuid);
            LoadbalancerHealthmonitor loadbalancerHealthmonitor = (LoadbalancerHealthmonitor) apiConnector.find(
                LoadbalancerHealthmonitor.class, project, healthMonitorName);
            LoadbalancerHealthmonitorType loadbalancerHealthmonitorType = new LoadbalancerHealthmonitorType();
            loadbalancerHealthmonitorType.setMonitorType(type);
            loadbalancerHealthmonitorType.setMaxRetries(retry);
            loadbalancerHealthmonitorType.setTimeout(timeout);
            loadbalancerHealthmonitorType.setDelay(interval);
            loadbalancerHealthmonitorType.setHttpMethod(httpMethod);
            loadbalancerHealthmonitorType.setExpectedCodes(expectedCode);
            loadbalancerHealthmonitorType.setUrlPath(urlPath);
            loadbalancerHealthmonitorType.setAdminState(true);
            loadbalancerHealthmonitor.setProperties(loadbalancerHealthmonitorType);
            Status status = apiConnector.update(loadbalancerHealthmonitor);
            status.ifFailure(errorHandler);
            return status.isSuccess();
        } catch (IOException e) {
            return false;
        }
    }

    public boolean updateLBServiceInstanceFatFlow(String publicNetworkUuid, String floatingIpPoolName,
        String floatingIpName) {
        boolean result = true;
        try {
            VirtualNetwork virtualNetwork = (VirtualNetwork) apiConnector.findById(VirtualNetwork.class,
                publicNetworkUuid);
            FloatingIpPool floatingIpPool = (FloatingIpPool) apiConnector.find(FloatingIpPool.class, virtualNetwork,
                floatingIpPoolName);
            FloatingIp floatingIp = (FloatingIp) apiConnector.find(FloatingIp.class, floatingIpPool, floatingIpName);
            List<ObjectReference<ApiPropertyBase>> listRefVmi = floatingIp.getVirtualMachineInterface();
            for (ObjectReference<ApiPropertyBase> refVmi : listRefVmi) {
                if (refVmi.getReferredName().get(refVmi.getReferredName().size() - 1).contains("right__1")) {
                    String siUuid = refVmi.getUuid();
                    VirtualMachineInterface vmi = (VirtualMachineInterface) apiConnector.findById(
                        VirtualMachineInterface.class, siUuid);
                    FatFlowProtocols fatFlowProtocols = vmi.getFatFlowProtocols();
                    if (fatFlowProtocols != null) {
                        fatFlowProtocols.clearFatFlowProtocol();
                        Status status = apiConnector.update(vmi);
                        status.ifFailure(errorHandler);
                        result = result && status.isSuccess();
                    }
                }
            }

            return result;
        } catch (IOException e) {
            return false;
        }
    }

    public boolean applyTungstenPortForwarding(boolean isAdd, String publicNetworkUuid, String floatingIpPoolName,
                                               String floatingIpName, String vmiUuid, String protocol, int publicPort, int privatePort) {
        try {
            FloatingIp floatingIp;
            VirtualNetwork virtualNetwork = (VirtualNetwork) apiConnector.findById(VirtualNetwork.class,
                    publicNetworkUuid);
            VirtualMachineInterface virtualMachineInterface = (VirtualMachineInterface) apiConnector.findById(
                    VirtualMachineInterface.class, vmiUuid);
            FloatingIpPool floatingIpPool = (FloatingIpPool) apiConnector.find(FloatingIpPool.class, virtualNetwork,
                    floatingIpPoolName);
            if (isAdd) {
                floatingIp = addFloatingIp(virtualMachineInterface, floatingIpPool, floatingIpName
                        , protocol, publicPort, privatePort);
            } else {
                floatingIp = removeFloatingIp(virtualMachineInterface, floatingIpPool, floatingIpName
                        , protocol, publicPort, privatePort);
            }

            Status status = apiConnector.update(floatingIp);
            status.ifFailure(errorHandler);
            return status.isSuccess();
        } catch (IOException e) {
            return false;
        }
    }

    private FloatingIp addFloatingIp(VirtualMachineInterface virtualMachineInterface, FloatingIpPool floatingIpPool,
                                     String floatingIpName, String protocol, int publicPort, int privatePort) throws IOException {
        FloatingIp floatingIp = (FloatingIp) apiConnector.find(FloatingIp.class, floatingIpPool, floatingIpName);
        PortMappings portMappings = floatingIp.getPortMappings();
        if (portMappings == null) {
            portMappings = new PortMappings();
        }

        portMappings.addPortMappings(protocol, publicPort, privatePort);
        floatingIp.setPortMappings(portMappings);
        floatingIp.addVirtualMachineInterface(virtualMachineInterface);
        floatingIp.setPortMappingsEnable(true);
        return floatingIp;
    }

    private FloatingIp removeFloatingIp(VirtualMachineInterface virtualMachineInterface,
     FloatingIpPool floatingIpPool, String floatingIpName, String protocol, int publicPort, int privatePort) throws IOException {
        FloatingIp floatingIp = (FloatingIp) apiConnector.find(FloatingIp.class, floatingIpPool, floatingIpName);
        PortMappings portMappings = floatingIp.getPortMappings();
        if (portMappings != null) {
            List<PortMap> portMapList = portMappings.getPortMappings();
            List<PortMap> removePortMapList = new ArrayList<>();
            for (PortMap portMap : portMapList) {
                if (portMap.getProtocol().equals(protocol) && portMap.getSrcPort() == publicPort
                        && portMap.getDstPort() == privatePort) {
                    removePortMapList.add(portMap);
                }
            }
            portMapList.removeAll(removePortMapList);
        }

        floatingIp.removeVirtualMachineInterface(virtualMachineInterface);

        if (floatingIp.getVirtualMachineInterface() == null
                || floatingIp.getVirtualMachineInterface().isEmpty()) {
            floatingIp.setPortMappingsEnable(false);
        }
        return floatingIp;
    }

    public boolean addTungstenNetworkSubnetCommand(String networkUuid, String ipPrefix, int ipPrefixLen, String gateway,
        boolean dhcpEnable, String dnsServer, String allocationStart, String allocationEnd, boolean ipFromStart,
        String subnetName) {
        try {
            VirtualNetwork virtualNetwork = (VirtualNetwork) apiConnector.findById(VirtualNetwork.class, networkUuid);
            if (virtualNetwork == null) {
                return false;
            }
            Project project = (Project) apiConnector.findById(Project.class, virtualNetwork.getParentUuid());
            NetworkIpam networkIpam = getDefaultProjectNetworkIpam(project);

            if (networkIpam == null) {
                return false;
            }

            IpamSubnetType ipamSubnetType = getIpamSubnetType(ipPrefix, ipPrefixLen, gateway, dhcpEnable, ipFromStart,
                allocationStart, allocationEnd, subnetName, dnsServer);
            List<ObjectReference<VnSubnetsType>> objectReferenceList = virtualNetwork.getNetworkIpam();

            if (objectReferenceList != null && objectReferenceList.size() == 1) {
                VnSubnetsType vnSubnetsType = objectReferenceList.get(0).getAttr();
                vnSubnetsType.addIpamSubnets(ipamSubnetType);
            } else {
                VnSubnetsType vnSubnetsType = new VnSubnetsType();
                vnSubnetsType.addIpamSubnets(ipamSubnetType);
                virtualNetwork.addNetworkIpam(networkIpam, vnSubnetsType);
            }

            virtualNetwork.setPerms2(null);
            Status status = apiConnector.update(virtualNetwork);
            status.ifFailure(errorHandler);
            return status.isSuccess();
        } catch (IOException e) {
            return false;
        }
    }

    public boolean removeTungstenNetworkSubnetCommand(String networkUuid, String subnetName) {
        try {
            boolean clear = false;
            VirtualNetwork virtualNetwork = (VirtualNetwork) apiConnector.findById(VirtualNetwork.class, networkUuid);
            if (virtualNetwork == null) {
                return true;
            }

            List<ObjectReference<VnSubnetsType>> objectReferenceList = virtualNetwork.getNetworkIpam();
            if (objectReferenceList == null) {
                return true;
            }

            for (ObjectReference<VnSubnetsType> vnSubnetsTypeObjectReference : objectReferenceList) {
                VnSubnetsType vnSubnetsType = vnSubnetsTypeObjectReference.getAttr();
                List<IpamSubnetType> ipamSubnetTypeList = vnSubnetsType.getIpamSubnets();

                List<IpamSubnetType> removeIpamSubnetTypelist = new ArrayList<>();
                for (IpamSubnetType ipamSubnetType : ipamSubnetTypeList) {
                    if (ipamSubnetType.getSubnetName().equals(subnetName)) {
                        removeIpamSubnetTypelist.add(ipamSubnetType);
                    }
                }

                if (ipamSubnetTypeList.size() != removeIpamSubnetTypelist.size()) {
                    ipamSubnetTypeList.removeAll(removeIpamSubnetTypelist);
                } else {
                    clear = true;
                }
            }

            if (clear) {
                virtualNetwork.clearNetworkIpam();
            }

            virtualNetwork.setPerms2(null);

            Status status = apiConnector.update(virtualNetwork);
            status.ifFailure(errorHandler);
            return status.isSuccess();
        } catch (IOException e) {
            return false;
        }
    }

    public ApiObjectBase createTungstenTagType(String uuid, String name) {
        try {
            TagType tagType = new TagType();
            tagType.setUuid(uuid);
            tagType.setName(name);
            Status status = apiConnector.create(tagType);
            status.ifFailure(errorHandler);
            return apiConnector.findById(TagType.class, tagType.getUuid());
        } catch (IOException e) {
            return null;
        }
    }

    public ApiObjectBase createTungstenTag(final String uuid, final String tagType, final String tagValue, final String tagId) {
        try {
            Tag tag = new Tag();
            tag.setUuid(uuid);
            tag.setName(tagType + "=" + tagValue);
            tag.setTypeName(tagType);
            tag.setValue(tagValue);
            tag.setParent(new ConfigRoot());
            if (tagId != null) {
                tag.setId(tagId);
            }
            Status status = apiConnector.create(tag);
            status.ifFailure(errorHandler);
            return apiConnector.findById(Tag.class, tag.getUuid());
        } catch (IOException e) {
            return null;
        }
    }

    public ApiObjectBase createTungstenApplicationPolicySet(String uuid, String name) {
        try {
            String policyManagementUuid = apiConnector.findByName(PolicyManagement.class, new ConfigRoot(), TUNGSTEN_DEFAULT_POLICY_MANAGEMENT);
            PolicyManagement policyManagement = (PolicyManagement) apiConnector.findById(PolicyManagement.class, policyManagementUuid);
            if (policyManagement == null) {
                return null;
            }

            ApplicationPolicySet applicationPolicySet = new ApplicationPolicySet();
            applicationPolicySet.setUuid(uuid);
            applicationPolicySet.setName(name);
            applicationPolicySet.setParent(policyManagement);
            Status status = apiConnector.create(applicationPolicySet);
            status.ifFailure(errorHandler);
            return apiConnector.findById(ApplicationPolicySet.class, applicationPolicySet.getUuid());
        } catch (IOException e) {
            return null;
        }
    }

    public ApiObjectBase createTungstenFirewallPolicy(String uuid, String applicationPolicySetUuid, String name, int sequence) {
        try {
            String policyManagementUuid = apiConnector.findByName(PolicyManagement.class, new ConfigRoot(), TUNGSTEN_DEFAULT_POLICY_MANAGEMENT);
            PolicyManagement policyManagement = (PolicyManagement) apiConnector.findById(PolicyManagement.class, policyManagementUuid);
            if (policyManagement == null) {
                return null;
            }

            ApplicationPolicySet applicationPolicySet = (ApplicationPolicySet) apiConnector.findById(ApplicationPolicySet.class, applicationPolicySetUuid);
            List<ObjectReference<ApiPropertyBase>> objectReferenceList = applicationPolicySet.getTag();
            FirewallPolicy firewallPolicy = new FirewallPolicy();
            firewallPolicy.setUuid(uuid);
            firewallPolicy.setName(name);
            firewallPolicy.setParent(policyManagement);
            if (objectReferenceList != null && !objectReferenceList.isEmpty()) {
                for (ObjectReference<ApiPropertyBase> objectReference : objectReferenceList) {
                    Tag tag = (Tag) apiConnector.findById(Tag.class, objectReference.getUuid());
                    firewallPolicy.setTag(tag);
                }
            }
            Status status = apiConnector.create(firewallPolicy);
            status.ifFailure(errorHandler);

            if (status.isSuccess()) {
                applicationPolicySet.addFirewallPolicy(firewallPolicy, new FirewallSequence(String.valueOf(sequence)));
                Status update = apiConnector.update(applicationPolicySet);
                update.ifFailure(errorHandler);
                if (update.isSuccess()) {
                    return apiConnector.findById(FirewallPolicy.class, firewallPolicy.getUuid());
                } else {
                    apiConnector.delete(firewallPolicy);
                }
            }
            return null;
        } catch (IOException e) {
            return null;
        }
    }

    public ApiObjectBase createTungstenFirewallRule(String uuid, String firewallPolicyUuid, String name, String action, String serviceGroupUuid,
        String srcTagUuid, String srcAddressGroupUuid, String srcNetworkUuid, String direction, String destTagUuid,
        String destAddressGroupUuid, String destNetworkUuid, String tagTypeUuid, int sequence) {
        try {
            String policyManagementUuid = apiConnector.findByName(PolicyManagement.class, new ConfigRoot(), TUNGSTEN_DEFAULT_POLICY_MANAGEMENT);
            PolicyManagement policyManagement = (PolicyManagement) apiConnector.findById(PolicyManagement.class, policyManagementUuid);
            if (policyManagement == null) {
                return null;
            }
            FirewallPolicy firewallPolicy = (FirewallPolicy) apiConnector.findById(FirewallPolicy.class, firewallPolicyUuid);
            if (firewallPolicy == null) {
                return null;
            }
            ServiceGroup serviceGroup = (ServiceGroup) apiConnector.findById(ServiceGroup.class, serviceGroupUuid);
            AddressGroup srcAddressGroup = (AddressGroup) apiConnector.findById(AddressGroup.class,
                srcAddressGroupUuid);
            AddressGroup destAddressGroup = (AddressGroup) apiConnector.findById(AddressGroup.class,
                destAddressGroupUuid);
            VirtualNetwork srcNetwork = (VirtualNetwork) apiConnector.findById(VirtualNetwork.class, srcNetworkUuid);
            VirtualNetwork destNetwork = (VirtualNetwork) apiConnector.findById(VirtualNetwork.class, destNetworkUuid);
            Tag srcTag = (Tag) apiConnector.findById(Tag.class, srcTagUuid);
            Tag destTag = (Tag) apiConnector.findById(Tag.class, destTagUuid);
            TagType tagType = (TagType) apiConnector.findById(TagType.class, tagTypeUuid);
            if (serviceGroup == null) {
                return null;
            }

            if (srcAddressGroup == null && srcTag == null && srcNetwork == null) {
                return null;
            }

            if (destAddressGroup == null && destTag == null && destNetwork == null) {
                return null;
            }

            FirewallRule firewallRule = new FirewallRule();
            firewallRule.setUuid(uuid);
            firewallRule.setName(name);
            firewallRule.setParent(policyManagement);
            firewallRule.setActionList(new ActionListType(action));
            firewallRule.setServiceGroup(serviceGroup);
            FirewallRuleEndpointType srcFirewallRuleEndpointType = new FirewallRuleEndpointType();
            if (srcTag != null) {
                srcFirewallRuleEndpointType.addTags("global:" + srcTag.getName());
                srcFirewallRuleEndpointType.addTagIds(Integer.decode(srcTag.getId()));
            }

            if (srcAddressGroup != null) {
                String srcAddressGroupName = StringUtils.join(srcAddressGroup.getQualifiedName(), ":");
                srcFirewallRuleEndpointType.setAddressGroup(srcAddressGroupName);
            }

            if (srcNetwork != null) {
                srcFirewallRuleEndpointType.setVirtualNetwork(StringUtils.join(srcNetwork.getQualifiedName(), ":"));
            }

            FirewallRuleEndpointType destFirewallRuleEndpointType = new FirewallRuleEndpointType();
            if (destTag != null) {
                destFirewallRuleEndpointType.addTags("global:" + destTag.getName());
                destFirewallRuleEndpointType.addTagIds(Integer.decode(destTag.getId()));
            }

            if (destAddressGroup != null) {
                String destAddressGroupName = StringUtils.join(destAddressGroup.getQualifiedName(), ":");
                destFirewallRuleEndpointType.setAddressGroup(destAddressGroupName);
            }

            if (destNetwork != null) {
                destFirewallRuleEndpointType.setVirtualNetwork(StringUtils.join(destNetwork.getQualifiedName(), ":"));
            }

            firewallRule.setEndpoint1(srcFirewallRuleEndpointType);
            firewallRule.setDirection(direction);
            firewallRule.setEndpoint2(destFirewallRuleEndpointType);
            FirewallRuleMatchTagsType firewallRuleMatchTagsType = new FirewallRuleMatchTagsType();
            if (tagType != null) {
                firewallRuleMatchTagsType.addTag(tagType.getName());
                firewallRule.setMatchTags(firewallRuleMatchTagsType);
            }

            return createFirewallRule(firewallPolicy, firewallRule, sequence);
        } catch (IOException e) {
            return null;
        }
    }

    private ApiObjectBase createFirewallRule(FirewallPolicy firewallPolicy, FirewallRule firewallRule, int sequence) throws IOException {
        Status status = apiConnector.create(firewallRule);
        status.ifFailure(errorHandler);
        if (status.isSuccess()) {
            firewallPolicy.addFirewallRule(firewallRule, new FirewallSequence(String.valueOf(sequence)));
            Status updated = apiConnector.update(firewallPolicy);
            updated.ifFailure(errorHandler);
            if (updated.isSuccess()) {
                return apiConnector.findById(FirewallRule.class, firewallRule.getUuid());
            } else {
                apiConnector.delete(firewallRule);
            }
        }
        return null;
    }

    public ApiObjectBase createTungstenServiceGroup(String uuid, String name, String protocol, int startPort,
        int endPort) {
        try {
            String policyManagementUuid = apiConnector.findByName(PolicyManagement.class, new ConfigRoot(), TUNGSTEN_DEFAULT_POLICY_MANAGEMENT);
            PolicyManagement policyManagement = (PolicyManagement) apiConnector.findById(PolicyManagement.class, policyManagementUuid);
            if (policyManagement == null) {
                return null;
            }

            ServiceGroup serviceGroup = new ServiceGroup();
            serviceGroup.setUuid(uuid);
            serviceGroup.setName(name);
            serviceGroup.setParent(policyManagement);
            FirewallServiceType firewallServiceType = new FirewallServiceType();
            firewallServiceType.setProtocol(protocol);
            firewallServiceType.setSrcPorts(new PortType(0));
            firewallServiceType.setDstPorts(new PortType(startPort, endPort));
            FirewallServiceGroupType firewallServiceGroupType = new FirewallServiceGroupType();
            firewallServiceGroupType.addFirewallService(firewallServiceType);
            serviceGroup.setFirewallServiceList(firewallServiceGroupType);
            Status status = apiConnector.create(serviceGroup);
            status.ifFailure(errorHandler);
            return apiConnector.findById(ServiceGroup.class, serviceGroup.getUuid());
        } catch (IOException e) {
            return null;
        }
    }

    public ApiObjectBase createTungstenAddressGroup(String uuid, String name, String ipPrefix, int ipPrefixLen) {
        try {
            String policyManagementUuid = apiConnector.findByName(PolicyManagement.class, new ConfigRoot(), TUNGSTEN_DEFAULT_POLICY_MANAGEMENT);
            PolicyManagement policyManagement = (PolicyManagement) apiConnector.findById(PolicyManagement.class, policyManagementUuid);
            if (policyManagement == null) {
                return null;
            }

            AddressGroup addressGroup = new AddressGroup();
            addressGroup.setUuid(uuid);
            addressGroup.setName(name);
            SubnetListType subnetListType = new SubnetListType();
            subnetListType.addSubnet(ipPrefix, ipPrefixLen);
            addressGroup.setPrefix(subnetListType);
            addressGroup.setParent(policyManagement);
            Status status = apiConnector.create(addressGroup);
            status.ifFailure(errorHandler);
            return apiConnector.findById(AddressGroup.class, addressGroup.getUuid());
        } catch (IOException e) {
            return null;
        }
    }

    public boolean applyTungstenNetworkTag(List<String> networkUuids, String tagUuid) {
        try {
            boolean result = true;
            for (String networkUuid : networkUuids) {
                Tag tag = (Tag) apiConnector.findById(Tag.class, tagUuid);
                VirtualNetwork virtualNetwork = (VirtualNetwork) apiConnector.findById(VirtualNetwork.class,
                    networkUuid);
                virtualNetwork.addTag(tag);
                Status status = apiConnector.update(virtualNetwork);
                status.ifFailure(errorHandler);
                result = result && status.isSuccess();
            }
            return result;
        } catch (IOException e) {
            return false;
        }
    }

    public boolean applyTungstenVmTag(List<String> vmUuids, String tagUuid) {
        try {
            boolean result = true;
            Tag tag = (Tag) apiConnector.findById(Tag.class, tagUuid);
            for (String vmUuid : vmUuids) {
                VirtualMachine virtualMachine = (VirtualMachine) apiConnector.findById(VirtualMachine.class, vmUuid);
                virtualMachine.addTag(tag);
                Status status = apiConnector.update(virtualMachine);
                status.ifFailure(errorHandler);
                result = result && status.isSuccess();
            }
            return result;
        } catch (IOException e) {
            return false;
        }
    }

    public boolean applyTungstenNicTag(List<String> nicUuids, String tagUuid) {
        try {
            boolean result = true;
            Tag tag = (Tag) apiConnector.findById(Tag.class, tagUuid);
            for (String nicUuid : nicUuids) {
                VirtualMachineInterface virtualMachineInterface = (VirtualMachineInterface) apiConnector.findById(
                    VirtualMachineInterface.class, nicUuid);
                virtualMachineInterface.addTag(tag);
                Status status = apiConnector.update(virtualMachineInterface);
                status.ifFailure(errorHandler);
                result = result && status.isSuccess();
            }
            return result;
        } catch (IOException e) {
            return false;
        }
    }

    public boolean applyTungstenPolicyTag(String policyUuid, String tagUuid) {
        try {
            Tag tag = (Tag) apiConnector.findById(Tag.class, tagUuid);
            NetworkPolicy networkPolicy = (NetworkPolicy) apiConnector.findById(NetworkPolicy.class, policyUuid);
            networkPolicy.addTag(tag);
            Status status = apiConnector.update(networkPolicy);
            status.ifFailure(errorHandler);
            return status.isSuccess();
        } catch (IOException e) {
            return false;
        }
    }

    public boolean applyTungstenApplicationPolicySetTag(String applicationPolicySetUuid, String tagUuid) {
        try {
            Tag tag = (Tag) apiConnector.findById(Tag.class, tagUuid);
            ApplicationPolicySet applicationPolicySet = (ApplicationPolicySet) apiConnector.findById(ApplicationPolicySet.class, applicationPolicySetUuid);
            applicationPolicySet.addTag(tag);
            Status status = apiConnector.update(applicationPolicySet);
            status.ifFailure(errorHandler);

            List<ObjectReference<FirewallSequence>> firewallPolicyList = applicationPolicySet.getFirewallPolicy();
            if (firewallPolicyList != null && !firewallPolicyList.isEmpty()) {
                for(ObjectReference<FirewallSequence> objectReference : firewallPolicyList) {
                    FirewallPolicy firewallPolicy = (FirewallPolicy) apiConnector.findById(FirewallPolicy.class, objectReference.getUuid());
                    firewallPolicy.setTag(tag);
                    Status updateFirewallPolicyStatus = apiConnector.update(firewallPolicy);
                    updateFirewallPolicyStatus.ifFailure(errorHandler);
                }
            }

            return status.isSuccess();
        } catch (IOException e) {
            return false;
        }
    }

    public ApiObjectBase removeTungstenTag(List<String> networkUuids, List<String> vmUuids, List<String> nicUuids,
        String policyUuid, String applicationPolicySetUuid, String tagUuid) {
        try {
            Tag tag = (Tag) getTungstenObject(Tag.class, tagUuid);
            if (tag == null) {
                return null;
            }

            removeTungstenNetworkTag(tag, networkUuids);
            removeTungstenVmTag(tag, vmUuids);
            removeTungstenNicTag(tag, nicUuids);
            removeTungstenNetworkPolicyTag(tag, policyUuid);
            removeTungstenApplicationPolicySetTag(tag, applicationPolicySetUuid);

            return apiConnector.findById(Tag.class, tagUuid);
        } catch (IOException e) {
            return null;
        }
    }

    private void removeTungstenNetworkTag(Tag tag, List<String> networkUuids) throws IOException {
        if (networkUuids != null) {
            for (String networkUuid : networkUuids) {
                VirtualNetwork virtualNetwork = (VirtualNetwork) getTungstenObject(VirtualNetwork.class,
                        networkUuid);
                if (virtualNetwork != null) {
                    virtualNetwork.removeTag(tag);
                    Status status = apiConnector.update(virtualNetwork);
                    status.ifFailure(errorHandler);
                }
            }
        }
    }

    private void removeTungstenVmTag(Tag tag, List<String> vmUuids) throws IOException {
        if (vmUuids != null) {
            for (String vmUuid : vmUuids) {
                VirtualMachine virtualMachine = (VirtualMachine) getTungstenObject(VirtualMachine.class, vmUuid);
                if (virtualMachine != null) {
                    virtualMachine.removeTag(tag);
                    Status status = apiConnector.update(virtualMachine);
                    status.ifFailure(errorHandler);
                }
            }
        }
    }

    private void removeTungstenNicTag(Tag tag, List<String> nicUuids) throws IOException {
        if (nicUuids != null) {
            for (String nicUuid : nicUuids) {
                VirtualMachineInterface virtualMachineInterface = (VirtualMachineInterface) getTungstenObject(
                        VirtualMachineInterface.class, nicUuid);
                if (virtualMachineInterface != null) {
                    virtualMachineInterface.removeTag(tag);
                    Status status = apiConnector.update(virtualMachineInterface);
                    status.ifFailure(errorHandler);
                }
            }
        }
    }

    private void removeTungstenNetworkPolicyTag(Tag tag, String policyUuid) throws IOException {
        if (policyUuid != null) {
            NetworkPolicy networkPolicy = (NetworkPolicy) getTungstenObject(NetworkPolicy.class, policyUuid);
            if (networkPolicy != null) {
                networkPolicy.removeTag(tag);
                Status status = apiConnector.update(networkPolicy);
                status.ifFailure(errorHandler);
            }
        }
    }

    private void removeTungstenApplicationPolicySetTag(Tag tag, String applicationPolicySetUuid) throws IOException {
        if (applicationPolicySetUuid != null) {
            ApplicationPolicySet applicationPolicySet = (ApplicationPolicySet) getTungstenObject(ApplicationPolicySet.class, applicationPolicySetUuid);
            if (applicationPolicySet != null) {
                applicationPolicySet.removeTag(tag);
                Status status = apiConnector.update(applicationPolicySet);
                status.ifFailure(errorHandler);
            }
        }
    }

    public ApiObjectBase removeTungstenPolicy(String networkUuid, String policyUuid) {
        try {
            NetworkPolicy networkPolicy = (NetworkPolicy) getTungstenObject(NetworkPolicy.class, policyUuid);
            VirtualNetwork virtualNetwork = (VirtualNetwork) getTungstenObject(VirtualNetwork.class, networkUuid);
            if (networkPolicy != null && virtualNetwork != null) {
                virtualNetwork.removeNetworkPolicy(networkPolicy, new VirtualNetworkPolicyType());
                Status status = apiConnector.update(virtualNetwork);
                status.ifFailure(errorHandler);
                return apiConnector.findById(NetworkPolicy.class, policyUuid);
            }
            return null;
        } catch (IOException e) {
            return null;
        }
    }

    public ApiObjectBase createTungstenPolicy(final String uuid, final String name, final String projectUuid) {
        try {
            Project project = (Project) getTungstenObject(Project.class, projectUuid);
            NetworkPolicy networkPolicy = new NetworkPolicy();
            networkPolicy.setUuid(uuid);
            networkPolicy.setName(name);
            networkPolicy.setParent(project);
            Status status = apiConnector.create(networkPolicy);
            status.ifFailure(errorHandler);
            return apiConnector.findById(NetworkPolicy.class, networkPolicy.getUuid());
        } catch (IOException e) {
            return null;
        }
    }

    public ApiObjectBase addTungstenPolicyRule(final String uuid, final String policyUuid, final String action,
        final String protocol, final String direction, final String srcNetwork, final String srcIpPrefix,
        final int srcIpPrefixLen, final int srcStartPort, final int srcEndPort, final String destNetwork,
        final String destIpPrefix, final int destIpPrefixLen, final int destStartPort, final int destEndPort) {
        try {
            NetworkPolicy networkPolicy = (NetworkPolicy) apiConnector.findById(NetworkPolicy.class, policyUuid);
            PolicyEntriesType policyEntriesType = networkPolicy.getEntries();
            if (policyEntriesType == null) {
                policyEntriesType = new PolicyEntriesType();
                networkPolicy.setEntries(policyEntriesType);
            }

            PolicyRuleType policyRuleType = new PolicyRuleType();
            policyRuleType.setActionList(new ActionListType(action));
            policyRuleType.setProtocol(protocol);
            policyRuleType.setRuleUuid(uuid);
            policyRuleType.setDirection(direction);

            AddressType srcAddressType = new AddressType();
            if (srcNetwork != null) {
                srcAddressType.setVirtualNetwork(srcNetwork);
            }

            if (srcIpPrefix != null) {
                srcAddressType.addSubnet(new SubnetType(srcIpPrefix, srcIpPrefixLen));
            }

            AddressType dstAddressType = new AddressType();
            if (destNetwork != null) {
                dstAddressType.setVirtualNetwork(destNetwork);
            }

            if (destIpPrefix != null) {
                dstAddressType.addSubnet(new SubnetType(destIpPrefix, destIpPrefixLen));
            }
            policyRuleType.addSrcAddresses(srcAddressType);
            policyRuleType.addDstAddresses(dstAddressType);
            policyRuleType.addSrcPorts(srcStartPort, srcEndPort);
            policyRuleType.addDstPorts(destStartPort, destEndPort);

            policyEntriesType.addPolicyRule(policyRuleType);
            Status status = apiConnector.update(networkPolicy);
            status.ifFailure(errorHandler);
            return apiConnector.findById(NetworkPolicy.class, policyUuid);
        } catch (IOException e) {
            return null;
        }
    }

    public List<ApiObjectBase> listTungstenAddressPolicy(String projectUuid, String policyName) {
        Project project = (Project) getTungstenObject(Project.class, projectUuid);
        List<ApiObjectBase> networkPolicyList = new ArrayList<>();
        NetworkPolicy networkPolicy = (NetworkPolicy) getTungstenObjectByName(NetworkPolicy.class,
            project.getQualifiedName(), policyName);

        if (networkPolicy != null) {
            networkPolicyList.add(networkPolicy);
        }

        return networkPolicyList;
    }

    public List<ApiObjectBase> listTungstenPolicy(String projectUuid, String policyUuid) {
        Project project = (Project) getTungstenObject(Project.class, projectUuid);
        return getTungstenListObject(NetworkPolicy.class, project, policyUuid);
    }

    public List<ApiObjectBase> listTungstenNetwork(String projectUuid, String networkUuid) {
        Project project = (Project) getTungstenObject(Project.class, projectUuid);
        return getTungstenListObject(VirtualNetwork.class, project, networkUuid);
    }

    public List<ApiObjectBase> listTungstenVm(String projectUuid, String vmUuid) {
        Project project = (Project) getTungstenObject(Project.class, projectUuid);
        return getTungstenListObject(VirtualMachine.class, project, vmUuid);
    }

    public List<ApiObjectBase> listTungstenNic(String projectUuid, String nicUuid) {
        Project project = (Project) getTungstenObject(Project.class, projectUuid);
        return getTungstenListObject(VirtualMachineInterface.class, project, nicUuid);
    }

    public List<ApiObjectBase> listTungstenTag(String networkUuid, String vmUuid, String nicUuid,
        String policyUuid, String applicationPolicySetUuid, String tagUuid) {
        try {
            List<ApiObjectBase> tagList;

            if (networkUuid != null) {
                VirtualNetwork virtualNetwork = (VirtualNetwork) apiConnector.findById(VirtualNetwork.class,
                    networkUuid);
                tagList = getTungstenListTag(virtualNetwork.getTag());
            } else if (vmUuid != null) {
                VirtualMachine virtualMachine = (VirtualMachine) apiConnector.findById(VirtualMachine.class, vmUuid);
                tagList = getTungstenListTag(virtualMachine.getTag());
            } else if (nicUuid != null) {
                VirtualMachineInterface virtualMachineInterface = (VirtualMachineInterface) apiConnector.findById(
                    VirtualMachineInterface.class, nicUuid);
                tagList = getTungstenListTag(virtualMachineInterface.getTag());
            } else if (policyUuid != null) {
                NetworkPolicy networkPolicy = (NetworkPolicy) apiConnector.findById(NetworkPolicy.class, policyUuid);
                tagList = getTungstenListTag(networkPolicy.getTag());
            } else if (applicationPolicySetUuid != null) {
                ApplicationPolicySet applicationPolicySet = (ApplicationPolicySet) apiConnector.findById(ApplicationPolicySet.class, applicationPolicySetUuid);
                tagList = getTungstenListTag(applicationPolicySet.getTag());
            } else {
                tagList = getTungstenListTag();
            }

            return getObjectList(filterSystemTag(tagList), tagUuid);
        } catch (IOException e) {
            return new ArrayList<>();
        }
    }

    public List<ApiObjectBase> listTungstenTagType(String tagTypeUuid) {
        try {
            List<TagType> tagTypeList = new ArrayList<>();
            if (tagTypeUuid != null) {
                TagType tagType = (TagType) apiConnector.findById(TagType.class, tagTypeUuid);
                if (tagType != null) {
                    tagTypeList.add(tagType);
                }
            } else {
                List<TagType> list = (List<TagType>) apiConnector.list(TagType.class, null);
                if (list != null) {
                    for (TagType tagType : list) {
                        tagTypeList.add((TagType) apiConnector.findById(TagType.class, tagType.getUuid()));
                    }
                }
            }
            return filterSystemTagType(tagTypeList);
        } catch (IOException e) {
            return new ArrayList<>();
        }
    }

    public List<ApiObjectBase> listTungstenNetworkPolicy(String networkUuid, String policyUuid) {
        try {
            VirtualNetwork virtualNetwork = (VirtualNetwork) apiConnector.findById(VirtualNetwork.class, networkUuid);
            List<ApiObjectBase> networkPolicyList = new ArrayList<>();
            if (virtualNetwork == null)
                return networkPolicyList;

            List<ObjectReference<VirtualNetworkPolicyType>> objectReferenceList = virtualNetwork.getNetworkPolicy();
            if (objectReferenceList != null) {
                for (ObjectReference<VirtualNetworkPolicyType> objectReference : objectReferenceList) {
                    if (policyUuid == null) {
                        NetworkPolicy networkPolicy = (NetworkPolicy) apiConnector.findById(NetworkPolicy.class,
                            objectReference.getUuid());
                        networkPolicyList.add(networkPolicy);
                    } else {
                        if (objectReference.getUuid().equals(policyUuid)) {
                            NetworkPolicy networkPolicy = (NetworkPolicy) apiConnector.findById(NetworkPolicy.class,
                                objectReference.getUuid());
                            networkPolicyList.add(networkPolicy);
                        }
                    }
                }
            }

            return networkPolicyList;
        } catch (IOException e) {
            return new ArrayList<>();
        }
    }

    public List<ApiObjectBase> listTungstenApplicationPolicySet(String applicationPolicySetUuid) {
        try {
            List<ApplicationPolicySet> applicationPolicySetList = new ArrayList<>();
            if (applicationPolicySetUuid != null) {
                ApplicationPolicySet applicationPolicySet = (ApplicationPolicySet) apiConnector.findById(
                    ApplicationPolicySet.class, applicationPolicySetUuid);
                if (applicationPolicySet != null) {
                    applicationPolicySetList.add(applicationPolicySet);
                }
            } else {
                List<ApplicationPolicySet> list = (List<ApplicationPolicySet>) apiConnector.list(
                    ApplicationPolicySet.class, null);
                if (list != null) {
                    for (ApplicationPolicySet applicationPolicySet : list) {
                        List<String> qualifiedName = applicationPolicySet.getQualifiedName();
                        if (qualifiedName.get(0).equals(TUNGSTEN_DEFAULT_POLICY_MANAGEMENT)) {
                            applicationPolicySetList.add(
                                (ApplicationPolicySet) apiConnector.findById(ApplicationPolicySet.class,
                                    applicationPolicySet.getUuid()));
                        }
                    }
                }
            }
            return filterSystemApplicationPolicySet(applicationPolicySetList);
        } catch (IOException e) {
            return new ArrayList<>();
        }
    }

    public List<ApiObjectBase> listTungstenFirewallPolicy(String applicationPolicySetUuid,
        String firewallPolicyUuid) {
        try {
            if (applicationPolicySetUuid != null) {
                return listTungstenFirewallPolicyWithUuid(applicationPolicySetUuid, firewallPolicyUuid);
            } else {
                return listTungstenFirewallPolicyWithoutUuid(firewallPolicyUuid);
            }
        } catch (IOException e) {
            return new ArrayList<>();
        }
    }

    private List<ApiObjectBase> listTungstenFirewallPolicyWithUuid(String applicationPolicySetUuid, String firewallPolicyUuid) throws IOException {
        List<ApiObjectBase> firewallPolicyList = new ArrayList<>();
        ApplicationPolicySet applicationPolicySet = (ApplicationPolicySet) apiConnector.findById(
                ApplicationPolicySet.class, applicationPolicySetUuid);
        List<ObjectReference<FirewallSequence>> objectReferenceList = applicationPolicySet.getFirewallPolicy();
        if (objectReferenceList != null) {
            for (ObjectReference<FirewallSequence> objectReference : objectReferenceList) {
                FirewallPolicy firewallPolicy = (FirewallPolicy) apiConnector.findById(FirewallPolicy.class,
                        objectReference.getUuid());
                if (firewallPolicyUuid != null) {
                    if (objectReference.getUuid().equals(firewallPolicyUuid)) {
                        firewallPolicyList.add(firewallPolicy);
                    }
                } else {
                    firewallPolicyList.add(firewallPolicy);
                }
            }
        }
        return firewallPolicyList;
    }

    private List<ApiObjectBase> listTungstenFirewallPolicyWithoutUuid(String firewallPolicyUuid) throws IOException {
        List<ApiObjectBase> firewallPolicyList = new ArrayList<>();
        List<FirewallPolicy> firewallPolicies = (List<FirewallPolicy>) apiConnector.list(FirewallPolicy.class,
                null);
        if (firewallPolicies != null) {
            for (FirewallPolicy firewallPolicy : firewallPolicies) {
                if (firewallPolicyUuid != null) {
                    if (firewallPolicy.getUuid().equals(firewallPolicyUuid)) {
                        firewallPolicyList.add(apiConnector.findById(FirewallPolicy.class, firewallPolicyUuid));
                    }
                } else {
                    String parentName = firewallPolicy.getQualifiedName().get(0);
                    if (parentName.equals(TUNGSTEN_DEFAULT_POLICY_MANAGEMENT)) {
                        firewallPolicyList.add(apiConnector.findById(FirewallPolicy.class, firewallPolicy.getUuid()));
                    }
                }
            }
        }
        return firewallPolicyList;
    }

    public List<ApiObjectBase> listTungstenFirewallRule(String firewallPolicyUuid, String firewallRuleUuid) {
        try {
            if (firewallPolicyUuid != null) {
                return listTungstenFirewallRuleWithUuid(firewallPolicyUuid, firewallRuleUuid);
            } else {
                return listTungstenFirewallRuleWithoutUuid(firewallRuleUuid);
            }
        } catch (IOException e) {
            return new ArrayList<>();
        }
    }

    private List<ApiObjectBase> listTungstenFirewallRuleWithUuid(String firewallPolicyUuid, String firewallRuleUuid) throws IOException {
        List<ApiObjectBase> firewallRuleList = new ArrayList<>();
        FirewallPolicy firewallPolicy = (FirewallPolicy) apiConnector.findById(FirewallPolicy.class,
                firewallPolicyUuid);
        List<ObjectReference<FirewallSequence>> objectReferenceList = firewallPolicy.getFirewallRule();
        if (objectReferenceList != null) {
            for (ObjectReference<FirewallSequence> objectReference : objectReferenceList) {
                FirewallRule firewallRule = (FirewallRule) apiConnector.findById(FirewallRule.class,
                        objectReference.getUuid());
                if (firewallRuleUuid != null) {
                    if (objectReference.getUuid().equals(firewallRuleUuid)) {
                        firewallRuleList.add(firewallRule);
                    }
                } else {
                    firewallRuleList.add(firewallRule);
                }
            }
        }
        return firewallRuleList;
    }

    private List<ApiObjectBase> listTungstenFirewallRuleWithoutUuid(String firewallRuleUuid) throws IOException {
        List<ApiObjectBase> firewallRuleList = new ArrayList<>();
        List<FirewallRule> firewallRules = (List<FirewallRule>) apiConnector.list(FirewallRule.class, null);
        if (firewallRules != null) {
            for (FirewallRule firewallRule : firewallRules) {
                if (firewallRuleUuid != null) {
                    if (firewallRule.getUuid().equals(firewallRuleUuid)) {
                        firewallRuleList.add(apiConnector.findById(FirewallRule.class, firewallRuleUuid));
                    }
                } else {
                    String parentName = firewallRule.getQualifiedName().get(0);
                    if (parentName.equals(TUNGSTEN_DEFAULT_POLICY_MANAGEMENT)) {
                        firewallRuleList.add(apiConnector.findById(FirewallRule.class, firewallRule.getUuid()));
                    }
                }
            }
        }
        return firewallRuleList;
    }

    public List<ApiObjectBase> listTungstenServiceGroup(String serviceGroupUuid) {
        try {
            List<ApiObjectBase> serviceGroupList = new ArrayList<>();
            List<ServiceGroup> serviceGroups = (List<ServiceGroup>) apiConnector.list(ServiceGroup.class, null);

            if (serviceGroups == null) {
                return new ArrayList<>();
            }

            for (ServiceGroup serviceGroup : serviceGroups) {
                if (serviceGroupUuid != null) {
                    if (serviceGroup.getUuid().equals(serviceGroupUuid)) {
                        serviceGroupList.add(apiConnector.findById(ServiceGroup.class, serviceGroupUuid));
                    }
                } else {
                    String parentName = serviceGroup.getQualifiedName().get(0);
                    if (parentName.equals(TUNGSTEN_DEFAULT_POLICY_MANAGEMENT)) {
                        serviceGroupList.add(apiConnector.findById(ServiceGroup.class, serviceGroup.getUuid()));
                    }
                }
            }
            return serviceGroupList;
        } catch (IOException e) {
            return new ArrayList<>();
        }
    }

    public List<ApiObjectBase> listTungstenAddressGroup(String addressGroupUuid) {
        try {
            List<ApiObjectBase> addressGroupList = new ArrayList<>();
            List<AddressGroup> addressGroups = (List<AddressGroup>) apiConnector.list(AddressGroup.class, null);
            if (addressGroups == null) {
                return new ArrayList<>();
            }

            for (AddressGroup addressGroup : addressGroups) {
                if (addressGroupUuid != null) {
                    if (addressGroup.getUuid().equals(addressGroupUuid)) {
                        addressGroupList.add(apiConnector.findById(AddressGroup.class, addressGroupUuid));
                    }
                } else {
                    String parentName = addressGroup.getQualifiedName().get(0);
                    if (parentName.equals(TUNGSTEN_DEFAULT_POLICY_MANAGEMENT)) {
                        addressGroupList.add(apiConnector.findById(AddressGroup.class, addressGroup.getUuid()));
                    }
                }
            }
            return addressGroupList;
        } catch (IOException e) {
            return new ArrayList<>();
        }
    }

    public ApiObjectBase removeTungstenNetworkPolicyRule(String policyUuid, String ruleUuid) {
        try {
            NetworkPolicy networkPolicy = (NetworkPolicy) apiConnector.findById(NetworkPolicy.class, policyUuid);
            PolicyEntriesType policyEntriesType = networkPolicy.getEntries();
            List<PolicyRuleType> policyRuleTypeList = policyEntriesType.getPolicyRule();
            PolicyRuleType removePolicyRuleType = null;
            for (PolicyRuleType policyRuleType : policyRuleTypeList) {
                if (policyRuleType.getRuleUuid().equals(ruleUuid)) {
                    removePolicyRuleType = policyRuleType;
                }
            }
            policyRuleTypeList.remove(removePolicyRuleType);
            Status status = apiConnector.update(networkPolicy);
            status.ifFailure(errorHandler);
            return apiConnector.findById(NetworkPolicy.class, policyUuid);
        } catch (IOException e) {
            return null;
        }
    }

    public ApiObjectBase updateTungstenVrouterConfig(String forwardingMode) {
        try {
            String globalVrouterConfigUuid = apiConnector.findByName(GlobalSystemConfig.class, new ConfigRoot(), TUNGSTEN_GLOBAL_SYSTEM_CONFIG);
            GlobalSystemConfig globalSystemConfig = (GlobalSystemConfig) apiConnector.findById(GlobalSystemConfig.class, globalVrouterConfigUuid);
            GlobalVrouterConfig globalVrouterConfig = (GlobalVrouterConfig) apiConnector.find(GlobalVrouterConfig.class,
                globalSystemConfig, TUNGSTEN_GLOBAL_VROUTER_CONFIG);
            if (globalVrouterConfig == null) {
                return null;
            }
            globalVrouterConfig.setForwardingMode(forwardingMode);
            Status status = apiConnector.update(globalVrouterConfig);
            status.ifFailure(errorHandler);
            return apiConnector.findById(GlobalVrouterConfig.class, globalVrouterConfig.getUuid());
        } catch (IOException e) {
            return null;
        }
    }

    public boolean deleteTungstenObject(ApiObjectBase apiObjectBase) {
        try {
            Status status = apiConnector.delete(apiObjectBase);
            status.ifFailure(errorHandler);
            return status.isSuccess();
        } catch (IOException e) {
            return false;
        }
    }

    public boolean deleteTungstenObject(Class<? extends ApiObjectBase> cls, String uuid) {
        try {
            Status status = apiConnector.delete(cls, uuid);
            status.ifFailure(errorHandler);
            return status.isSuccess();
        } catch (IOException e) {
            return false;
        }
    }

    public List<ApiObjectBase> getTungstenListObject(Class<? extends ApiObjectBase> cls, ApiObjectBase parent,
        String uuid) {
        try {
            List<ApiObjectBase> resultList = new ArrayList<>();
            if (uuid != null) {
                resultList.add(apiConnector.findById(cls, uuid));
            } else {
                List<? extends ApiObjectBase> list = apiConnector.list(cls, parent.getQualifiedName());
                if (list != null) {
                    for (ApiObjectBase object : list) {
                        resultList.add(apiConnector.findById(object.getClass(), object.getUuid()));
                    }
                }
            }
            return resultList;
        } catch (IOException e) {
            return new ArrayList<>();
        }
    }

    private IpamSubnetType getIpamSubnetType(String ipPrefix, int ipPrefixLen, String gateway, boolean dhcpEnable,
        boolean ipFromStart, String allocationStart, String allocationEnd, String subnetName, String dnsServer) {
        IpamSubnetType ipamSubnetType = new IpamSubnetType();
        ipamSubnetType.setSubnetName(subnetName);
        ipamSubnetType.setSubnet(new SubnetType(ipPrefix, ipPrefixLen));
        ipamSubnetType.setDefaultGateway(gateway != null ? gateway : TungstenUtils.ALL_IP4_PREFIX);
        ipamSubnetType.setEnableDhcp(dhcpEnable);
        ipamSubnetType.setAddrFromStart(ipFromStart);
        ipamSubnetType.setDnsServerAddress(dnsServer);

        if (allocationStart != null && allocationEnd != null) {
            ipamSubnetType.addAllocationPools(allocationStart, allocationEnd, false);
        }

        return ipamSubnetType;
    }

    private PolicyRuleType getPolicyRuleType(TungstenRule tungstenRule) {
        PolicyRuleType policyRuleType = new PolicyRuleType();
        if (tungstenRule.getUuid() != null) {
            policyRuleType.setRuleUuid(tungstenRule.getUuid());
        }
        policyRuleType.setActionList(new ActionListType(tungstenRule.getAction()));
        policyRuleType.setDirection(tungstenRule.getDirection());
        policyRuleType.setProtocol(tungstenRule.getProtocol());
        return policyRuleType;
    }

    private void getPolicyEntriesType(List<TungstenRule> tungstenRuleList, PolicyEntriesType policyEntriesType) {
        for (TungstenRule tungstenRule : tungstenRuleList) {
            PolicyRuleType policyRuleType = getPolicyRuleType(tungstenRule);
            AddressType srcAddressType = new AddressType();
            AddressType dstAddressType = new AddressType();
            String srcIpPrefix = tungstenRule.getSrcIpPrefix();
            int srcIpPrefixLen = tungstenRule.getSrcIpPrefixLen();
            String dstIpPrefix = tungstenRule.getDstIpPrefix();
            int dstIpPrefixLen = tungstenRule.getDstIpPrefixLen();
            String srcNetwork = getSrcNetwork(tungstenRule);
            String dstNetwork = getDstNetwork(tungstenRule);

            if (srcIpPrefix == null || srcIpPrefix.isEmpty() || srcIpPrefix.isBlank()) {
                srcIpPrefix = TungstenUtils.ALL_IP4_PREFIX;
                srcIpPrefixLen = 0;
            }

            if (dstIpPrefix == null || dstIpPrefix.isEmpty() || dstIpPrefix.isBlank()) {
                dstIpPrefix = TungstenUtils.ALL_IP4_PREFIX;
                dstIpPrefixLen = 0;
            }

            if (!srcNetwork.equals(TungstenUtils.ANY)) {
                srcAddressType.setVirtualNetwork(srcNetwork);
            } else {
                srcAddressType.setSubnet(new SubnetType(srcIpPrefix, srcIpPrefixLen));
            }

            if (!dstNetwork.equals(TungstenUtils.ANY)) {
                dstAddressType.setVirtualNetwork(dstNetwork);
            } else {
                dstAddressType.setSubnet(new SubnetType(dstIpPrefix, dstIpPrefixLen));
            }

            policyRuleType.addSrcAddresses(srcAddressType);
            policyRuleType.addDstAddresses(dstAddressType);
            policyRuleType.addSrcPorts(tungstenRule.getSrcStartPort(), tungstenRule.getSrcEndPort());
            policyRuleType.addDstPorts(tungstenRule.getDstStartPort(), tungstenRule.getDstEndPort());
            policyEntriesType.addPolicyRule(policyRuleType);
        }
    }

    private String getSrcNetwork(TungstenRule tungstenRule) {
        String srcNetwork = tungstenRule.getSrcNetwork();
        if (srcNetwork == null || srcNetwork.isEmpty() || srcNetwork.isBlank()) {
            return TungstenUtils.ANY;
        } else {
            return srcNetwork;
        }
    }

    private String getDstNetwork(TungstenRule tungstenRule) {
        String dstNetwork = tungstenRule.getDstNetwork();
        if (dstNetwork == null || dstNetwork.isEmpty() || dstNetwork.isBlank()) {
            return TungstenUtils.ANY;
        } else {
            return dstNetwork;
        }
    }

    public String getSubnetUuid(String networkUuid) {
        try {
            String subnetUuid = null;
            VirtualNetwork network = (VirtualNetwork) apiConnector.findById(VirtualNetwork.class, networkUuid);
            if (network != null) {
                List<ObjectReference<VnSubnetsType>> listIpam = network.getNetworkIpam();
                if (listIpam != null) {
                    for (ObjectReference<VnSubnetsType> objectReference : listIpam) {
                        VnSubnetsType vnSubnetsType = objectReference.getAttr();
                        List<IpamSubnetType> ipamSubnetTypeList = vnSubnetsType.getIpamSubnets();
                        for (IpamSubnetType ipamSubnetType : ipamSubnetTypeList) {
                            subnetUuid = ipamSubnetType.getSubnetUuid();
                        }
                    }
                }
                return subnetUuid;
            }
            return null;
        } catch (IOException e) {
            return null;
        }
    }

    public ApiObjectBase createTungstenSecurityGroup(String securityGroupUuid, String securityGroupName,
        String securityGroupDescription, String projectFqn) {
        try {
            SecurityGroup tungstenSecurityGroup = (SecurityGroup) apiConnector.findById(SecurityGroup.class,
                securityGroupUuid);
            if (tungstenSecurityGroup != null) {
                return tungstenSecurityGroup;
            }
            Project project = (Project) getTungstenProjectByFqn(projectFqn);
            tungstenSecurityGroup = new SecurityGroup();
            tungstenSecurityGroup.setUuid(securityGroupUuid);
            tungstenSecurityGroup.setName(securityGroupName);
            tungstenSecurityGroup.setDisplayName(securityGroupDescription);
            tungstenSecurityGroup.setParent(project);
            Status status = apiConnector.create(tungstenSecurityGroup);
            status.ifFailure(errorHandler);
            if (status.isSuccess()) {
                return apiConnector.findById(SecurityGroup.class, tungstenSecurityGroup.getUuid());
            } else {
                return null;
            }
        } catch (IOException e) {
            return null;
        }
    }

    public boolean addTungstenSecurityGroupRule(String tungstenSecurityGroupUuid, String securityGroupRuleUuid,
        String securityGroupRuleType, int startPort, int endPort, String target, String etherType, String protocol) {
        try {
            SecurityGroup securityGroup = (SecurityGroup) getTungstenObject(SecurityGroup.class,
                tungstenSecurityGroupUuid);
            if (securityGroup == null) {
                return false;
            }

            PolicyEntriesType policyEntriesType = securityGroup.getEntries();
            if (policyEntriesType == null) {
                policyEntriesType = new PolicyEntriesType();
                securityGroup.setEntries(policyEntriesType);
            }

            PolicyRuleType policyRuleType = createPolicyRuleType(securityGroupRuleUuid, securityGroupRuleType,
                startPort, endPort, target, etherType, protocol);

            policyEntriesType.addPolicyRule(policyRuleType);
            Status status = apiConnector.update(securityGroup);
            status.ifFailure(errorHandler);
            return status.isSuccess();
        } catch (IOException e) {
            return false;
        }
    }

    public boolean removeTungstenSecurityGroupRule(String tungstenSecurityGroupUuid, String securityGroupRuleUuid) {
        try {
            SecurityGroup securityGroup = (SecurityGroup) getTungstenObject(SecurityGroup.class,
                tungstenSecurityGroupUuid);
            if (securityGroup == null) {
                return false;
            }
            List<PolicyRuleType> existingPolicyRules = securityGroup.getEntries().getPolicyRule();
            securityGroup.getEntries().clearPolicyRule();
            for (PolicyRuleType policyRule : existingPolicyRules) {
                if (!policyRule.getRuleUuid().equals(securityGroupRuleUuid)) {
                    securityGroup.getEntries().addPolicyRule(policyRule);
                }
            }
            Status status = apiConnector.update(securityGroup);
            return status.isSuccess();
        } catch (IOException e) {
            return false;
        }
    }

    private PolicyRuleType createPolicyRuleType(String securityGroupRuleUuid, String securityGroupRuleType,
        int startPort, int endPort, String target, String etherType, String protocol) {
        AddressType addressType;
        String tungstenProtocol;
        String tungstenEthertType;
        if (NetUtils.isValidIp4Cidr(target) || NetUtils.isValidIp6Cidr(target)) {
            Pair<String, Integer> pair = NetUtils.getCidr(target);
            addressType = new AddressType(new SubnetType(pair.first(), pair.second()));
            tungstenProtocol = TungstenUtils.getTungstenProtocol(protocol, target);
            tungstenEthertType = TungstenUtils.getEthertTypeFromCidr(target);
        } else {
            addressType = new AddressType(null, null, target);
            tungstenProtocol = etherType.equals(TungstenUtils.IPV4) ?
                TungstenUtils.getTungstenProtocol(protocol, NetUtils.ALL_IP4_CIDRS) :
                TungstenUtils.getTungstenProtocol(protocol, NetUtils.ALL_IP6_CIDRS);
            tungstenEthertType = etherType;
        }

        PolicyRuleType policyRuleType = new PolicyRuleType();
        policyRuleType.setDirection(TungstenUtils.ONE_WAY_DIRECTION);
        policyRuleType.setProtocol(tungstenProtocol);
        policyRuleType.setEthertype(tungstenEthertType);
        policyRuleType.setRuleUuid(securityGroupRuleUuid);

        if (securityGroupRuleType.equals(TungstenUtils.INGRESS_RULE)) {
            policyRuleType.addSrcAddresses(addressType);
            policyRuleType.addSrcPorts(new PortType(0, 65535));
            policyRuleType.addDstAddresses(new AddressType(null, null, TUNGSTEN_LOCAL_SECURITY_GROUP, null));
            policyRuleType.addDstPorts(new PortType(startPort, endPort));
        } else if (securityGroupRuleType.equals(TungstenUtils.EGRESS_RULE)) {
            policyRuleType.addSrcPorts(new PortType(0, 65535));
            policyRuleType.addSrcAddresses(new AddressType(null, null, TUNGSTEN_LOCAL_SECURITY_GROUP, null));
            policyRuleType.addDstAddresses(addressType);
            policyRuleType.addDstPorts(new PortType(startPort, endPort));

        }

        return policyRuleType;
    }

    public boolean addInstanceToSecurityGroup(String nicUuid, List<String> securityGroupUuidList) {
        try {
            VirtualMachineInterface vmi = (VirtualMachineInterface) apiConnector.findById(VirtualMachineInterface.class,
                nicUuid);
            if (vmi == null) {
                return false;
            }

            for (String securityGroupUuid : securityGroupUuidList) {
                SecurityGroup tungstenSecurityGroup = (SecurityGroup) apiConnector.findById(SecurityGroup.class,
                    securityGroupUuid);
                if (tungstenSecurityGroup != null) {
                    vmi.addSecurityGroup(tungstenSecurityGroup);
                }
            }

            vmi.setPortSecurityEnabled(true);
            Status status = apiConnector.update(vmi);
            status.ifFailure(errorHandler);
            return status.isSuccess();
        } catch (IOException e) {
            return false;
        }
    }

    public boolean removeInstanceFromSecurityGroup(String nicUuid, List<String> securityGroupUuidList) {
        try {
            VirtualMachineInterface vmi = (VirtualMachineInterface) apiConnector.findById(VirtualMachineInterface.class,
                nicUuid);
            if (vmi == null) {
                return true;
            }

            for (String securityGroupUuid : securityGroupUuidList) {
                SecurityGroup tungstenSecurityGroup = (SecurityGroup) apiConnector.findById(SecurityGroup.class,
                    securityGroupUuid);
                if (tungstenSecurityGroup != null) {
                    vmi.removeSecurityGroup(tungstenSecurityGroup);
                }
            }

            vmi.setPortSecurityEnabled(false);
            Status status = apiConnector.update(vmi);
            status.ifFailure(errorHandler);
            return status.isSuccess();
        } catch (IOException e) {
            return false;
        }
    }

    public boolean addSecondaryIpAddress(String networkUuid, String nicUuid, String iiName, String address) {
        try {
            VirtualNetwork virtualNetwork = (VirtualNetwork) apiConnector.findById(VirtualNetwork.class, networkUuid);
            VirtualMachineInterface virtualMachineInterface = (VirtualMachineInterface) apiConnector.findById(
                VirtualMachineInterface.class, nicUuid);
            InstanceIp instanceIp = new InstanceIp();
            instanceIp.setName(iiName);
            instanceIp.setVirtualNetwork(virtualNetwork);
            instanceIp.setVirtualMachineInterface(virtualMachineInterface);
            instanceIp.setAddress(address);
            if (NetUtils.isValidIp6(address)) {
                instanceIp.setFamily("v6");
            }
            instanceIp.setSecondary(true);
            Status status = apiConnector.create(instanceIp);
            status.ifFailure(errorHandler);
            return status.isSuccess();
        } catch (IOException e) {
            return false;
        }
    }

    public boolean removeSecondaryIpAddress(String iiName) {
        try {
            String instanceIpUuid = apiConnector.findByName(InstanceIp.class, null, iiName);
            InstanceIp instanceIp = (InstanceIp) apiConnector.findById(InstanceIp.class, instanceIpUuid);
            if (instanceIp != null) {
                Status status = apiConnector.delete(instanceIp);
                status.ifFailure(errorHandler);
                return status.isSuccess();
            } else {
                return true;
            }
        } catch (IOException e) {
            return false;
        }
    }

    public String getTungstenNetworkDns(String uuid, String subnetName) {
        try {
            VirtualNetwork virtualNetwork = (VirtualNetwork) apiConnector.findById(VirtualNetwork.class, uuid);
            if (virtualNetwork == null) {
                return null;
            }

            List<ObjectReference<VnSubnetsType>> objectReferenceList = virtualNetwork.getNetworkIpam();
            if (objectReferenceList == null) {
                return null;
            }

            for (ObjectReference<VnSubnetsType> objectReference : objectReferenceList) {
                VnSubnetsType vnSubnetsType = objectReference.getAttr();
                if (vnSubnetsType == null) {
                    return null;
                }

                List<IpamSubnetType> ipamSubnetTypeList = vnSubnetsType.getIpamSubnets();
                if (ipamSubnetTypeList == null) {
                    return null;
                }

                for (IpamSubnetType ipamSubnetType : ipamSubnetTypeList) {
                    if (ipamSubnetType.getSubnetName().equals(subnetName)) {
                        return ipamSubnetType.getDnsServerAddress();
                    }
                }
            }

            return null;
        } catch (IOException e) {
            return null;
        }
    }

    public boolean updateTungstenDefaultSecurityGroup(String projectFqn) {
        try {
            Project project = (Project) getTungstenProjectByFqn(projectFqn);
            SecurityGroup securityGroup = (SecurityGroup) apiConnector.find(SecurityGroup.class, project, TUNGSTEN_DEFAULT);
            if (securityGroup == null) {
                return true;
            }

            PolicyEntriesType policyEntriesType = securityGroup.getEntries();
            if (policyEntriesType == null) {
                return false;
            }

            List<PolicyRuleType> policyRuleTypeList = policyEntriesType.getPolicyRule();
            if (policyRuleTypeList == null || policyRuleTypeList.size() != 4) {
                return true;
            }

            updatePolicyRule(policyRuleTypeList);

            Status status = apiConnector.update(securityGroup);
            status.ifFailure(errorHandler);
            return status.isSuccess();
        } catch (IOException e) {
            return false;
        }
    }

    private void updatePolicyRule(List<PolicyRuleType> policyRuleTypeList) {
        for (PolicyRuleType policyRuleType : policyRuleTypeList) {
            List<AddressType> addressTypeList = policyRuleType.getSrcAddresses();
            if (addressTypeList.size() != 1) {
                return;
            }

            AddressType addressType = addressTypeList.get(0);
            if (addressType == null || addressType.getSecurityGroup() == null) {
                return;
            }

            if (!addressType.getSecurityGroup().equals(TUNGSTEN_LOCAL_SECURITY_GROUP)) {
                if (policyRuleType.getEthertype().equals(TungstenUtils.IPV4)) {
                    addressType.setSecurityGroup(null);
                    addressType.setSubnet(new SubnetType(TungstenUtils.ALL_IP4_PREFIX, 0));
                }

                if (policyRuleType.getEthertype().equals(TungstenUtils.IPV6)) {
                    addressType.setSecurityGroup(null);
                    addressType.setSubnet(new SubnetType(TungstenUtils.ALL_IP6_PREFIX, 0));
                }
            }
        }
    }

    public ApiObjectBase createRoutingLogicalRouter(String projectUuid, String uuid, String name) {
        try {
            Project project = (Project) getTungstenObject(Project.class, projectUuid);
            LogicalRouter logicalRouter = new LogicalRouter();
            logicalRouter.setParent(project);
            logicalRouter.setName(TungstenUtils.ROUTINGLR_NAME + name);
            logicalRouter.setDisplayName(name);
            logicalRouter.setUuid(uuid);
            Status status = apiConnector.create(logicalRouter);
            status.ifFailure(errorHandler);
            return apiConnector.findById(LogicalRouter.class, logicalRouter.getUuid());
        } catch (IOException e) {
            return null;
        }
    }

    public List<ApiObjectBase> listRoutingLogicalRouter(String logicalRouterUuid) {
        try {
            List<LogicalRouter> logicalRouterList = new ArrayList<>();
            List<LogicalRouter> logicalRouters = (List<LogicalRouter>) apiConnector.list(LogicalRouter.class, null);
            if (logicalRouters != null) {
                for (LogicalRouter logicalRouter : logicalRouters) {
                    if (logicalRouterUuid != null) {
                        if (logicalRouter.getUuid().equals(logicalRouterUuid)) {
                            logicalRouterList.add(
                                (LogicalRouter) apiConnector.findById(LogicalRouter.class, logicalRouterUuid));
                        }
                    } else {
                        logicalRouterList.add(
                            (LogicalRouter) apiConnector.findById(LogicalRouter.class, logicalRouter.getUuid()));
                    }
                }
            }
            return filterSystemLogicalRouter(logicalRouterList);
        } catch (IOException e) {
            return new ArrayList<>();
        }
    }

    public ApiObjectBase addNetworkGatewayToLogicalRouter(String networkUuid, String logicalRouterUuid, String ipAddress) {
        try {
            LogicalRouter logicalRouter = (LogicalRouter) apiConnector.findById(LogicalRouter.class, logicalRouterUuid);
            VirtualNetwork virtualNetwork = (VirtualNetwork) apiConnector.findById(VirtualNetwork.class, networkUuid);
            Project project = (Project) apiConnector.findById(Project.class, virtualNetwork.getParentUuid());
            VirtualMachineInterface vmi = (VirtualMachineInterface) createTungstenGatewayVmi(
                TungstenUtils.getRoutingGatewayVmiName(logicalRouter.getName(), virtualNetwork.getName()),
                project.getUuid(), virtualNetwork.getUuid());
            createTungstenInstanceIp(
                TungstenUtils.getRoutingGatewayIiName(logicalRouter.getName(), virtualNetwork.getName()), ipAddress,
                virtualNetwork.getUuid(), vmi.getUuid());
            logicalRouter.addVirtualMachineInterface(vmi);
            Status status = apiConnector.update(logicalRouter);
            status.ifFailure(errorHandler);
            return apiConnector.findById(LogicalRouter.class, logicalRouterUuid);
        } catch (IOException e) {
            return null;
        }
    }

    public ApiObjectBase removeNetworkGatewayFromLogicalRouter(String networkUuid, String logicalRouterUuid) {
        try {
            LogicalRouter logicalRouter = (LogicalRouter) apiConnector.findById(LogicalRouter.class, logicalRouterUuid);
            VirtualNetwork virtualNetwork = (VirtualNetwork) apiConnector.findById(VirtualNetwork.class, networkUuid);
            Project project = (Project) apiConnector.findById(Project.class, virtualNetwork.getParentUuid());
            VirtualMachineInterface vmi = (VirtualMachineInterface) apiConnector.find(VirtualMachineInterface.class, project,
                TungstenUtils.getRoutingGatewayVmiName(logicalRouter.getName(), virtualNetwork.getName()));
            logicalRouter.removeVirtualMachineInterface(vmi);
            Status status = apiConnector.update(logicalRouter);
            status.ifFailure(errorHandler);
            deleteTungstenVmInterface(vmi);
            return apiConnector.findById(LogicalRouter.class, logicalRouterUuid);
        } catch (IOException e) {
            return null;
        }
    }

    public List<VirtualNetwork> getBackRefFromVirtualNetwork(Class<VirtualNetwork> aClass, List<ObjectReference<ApiPropertyBase>> objectReferenceList) {
        List<VirtualNetwork> apiObjectBaseList = new ArrayList<>();
        if (objectReferenceList == null) {
            return apiObjectBaseList;
        }
        try {
            for (ObjectReference<ApiPropertyBase> objectReference : objectReferenceList) {
                VirtualNetwork apiObjectBase = (VirtualNetwork) apiConnector.findById(aClass, objectReference.getUuid());
                apiObjectBaseList.add(apiObjectBase);
            }

            return apiObjectBaseList;
        } catch (IOException e) {
            return apiObjectBaseList;
        }
    }

    public List<VirtualMachine> getBackRefFromVirtualMachine(Class<VirtualMachine> aClass, List<ObjectReference<ApiPropertyBase>> objectReferenceList) {
        List<VirtualMachine> apiObjectBaseList = new ArrayList<>();
        if (objectReferenceList == null) {
            return apiObjectBaseList;
        }
        try {
            for (ObjectReference<ApiPropertyBase> objectReference : objectReferenceList) {
                VirtualMachine apiObjectBase = (VirtualMachine) apiConnector.findById(aClass, objectReference.getUuid());
                apiObjectBaseList.add(apiObjectBase);
            }

            return apiObjectBaseList;
        } catch (IOException e) {
            return apiObjectBaseList;
        }
    }

    public List<VirtualMachineInterface> getBackRefFromVirtualMachineInterface(Class<VirtualMachineInterface> aClass, List<ObjectReference<ApiPropertyBase>> objectReferenceList) {
        List<VirtualMachineInterface> apiObjectBaseList = new ArrayList<>();
        if (objectReferenceList == null) {
            return apiObjectBaseList;
        }
        try {
            for (ObjectReference<ApiPropertyBase> objectReference : objectReferenceList) {
                VirtualMachineInterface apiObjectBase = (VirtualMachineInterface) apiConnector.findById(aClass, objectReference.getUuid());
                apiObjectBaseList.add(apiObjectBase);
            }

            return apiObjectBaseList;
        } catch (IOException e) {
            return apiObjectBaseList;
        }
    }

    public List<NetworkPolicy> getBackRefFromNetworkPolicy(Class<NetworkPolicy> aClass, List<ObjectReference<ApiPropertyBase>> objectReferenceList) {
        List<NetworkPolicy> apiObjectBaseList = new ArrayList<>();
        if (objectReferenceList == null) {
            return apiObjectBaseList;
        }
        try {
            for (ObjectReference<ApiPropertyBase> objectReference : objectReferenceList) {
                NetworkPolicy apiObjectBase = (NetworkPolicy) apiConnector.findById(aClass, objectReference.getUuid());
                apiObjectBaseList.add(apiObjectBase);
            }

            return apiObjectBaseList;
        } catch (IOException e) {
            return apiObjectBaseList;
        }
    }

    public List<ApplicationPolicySet> getBackRefFromApplicationPolicySet(Class<ApplicationPolicySet> aClass, List<ObjectReference<ApiPropertyBase>> objectReferenceList) {
        List<ApplicationPolicySet> apiObjectBaseList = new ArrayList<>();
        if (objectReferenceList == null) {
            return apiObjectBaseList;
        }
        try {
            for (ObjectReference<ApiPropertyBase> objectReference : objectReferenceList) {
                ApplicationPolicySet apiObjectBase = (ApplicationPolicySet) apiConnector.findById(aClass, objectReference.getUuid());
                apiObjectBaseList.add(apiObjectBase);
            }

            return apiObjectBaseList;
        } catch (IOException e) {
            return apiObjectBaseList;
        }
    }

    public List<VirtualNetwork> getNetworksFromNetworkPolicy(NetworkPolicy networkPolicy) {
        List<VirtualNetwork> virtualNetworkList = new ArrayList<>();
        try {
            List<ObjectReference<VirtualNetworkPolicyType>> vnList = networkPolicy.getVirtualNetworkBackRefs();
            if (vnList == null) {
                return virtualNetworkList;
            }

            for(ObjectReference<VirtualNetworkPolicyType> vn : vnList) {
                VirtualNetwork virtualNetwork = (VirtualNetwork) apiConnector.findById(VirtualNetwork.class, vn.getUuid());
                virtualNetworkList.add(virtualNetwork);
            }

            return virtualNetworkList;
        } catch (IOException e) {
            return virtualNetworkList;
        }
    }

    public List<VirtualNetwork> listConnectedNetworkFromLogicalRouter(LogicalRouter logicalRouter) {
        List<VirtualNetwork> virtualNetworkList = new ArrayList<>();
        try {
            List<ObjectReference<ApiPropertyBase>> vmiList = logicalRouter.getVirtualMachineInterface();
            if (vmiList == null) {
                return virtualNetworkList;
            }

            for (ObjectReference<ApiPropertyBase> vmi : vmiList) {
                VirtualMachineInterface virtualMachineInterface = (VirtualMachineInterface) apiConnector.findById(
                    VirtualMachineInterface.class, vmi.getUuid());
                List<ObjectReference<ApiPropertyBase>> networkList = virtualMachineInterface.getVirtualNetwork();
                for(ObjectReference<ApiPropertyBase> network : networkList) {
                    VirtualNetwork virtualNetwork = (VirtualNetwork) apiConnector.findById(VirtualNetwork.class, network.getUuid());
                    virtualNetworkList.add(virtualNetwork);
                }
            }
            return virtualNetworkList;
        } catch (IOException e) {
            return virtualNetworkList;
        }
    }

    public Project createDefaultTungstenProject() {
        try {
            Domain domain = (Domain) apiConnector.findByFQN(Domain.class, TUNGSTEN_DEFAULT_DOMAIN);
            List<String> fqdnName = Arrays.asList(TUNGSTEN_DEFAULT_DOMAIN, TUNGSTEN_DEFAULT_PROJECT);
            String uuid = apiConnector.findByName(Project.class, fqdnName);
            if (uuid == null) {
                Project project = new Project();
                project.setParent(domain);
                project.setName(TUNGSTEN_DEFAULT_PROJECT);
                project.setDisplayName(TUNGSTEN_DEFAULT_PROJECT);
                Status status = apiConnector.create(project);
                status.ifFailure(errorHandler);
                return (Project) apiConnector.findById(Project.class, project.getUuid());
            } else {
                return (Project) apiConnector.findById(Project.class, uuid);
            }
        } catch (IOException e) {
            return null;
        }
    }

    private List<ApiObjectBase> getTungstenListTag() {
        try {
            List<ApiObjectBase> resultList = new ArrayList<>();
            List<? extends ApiObjectBase> list = apiConnector.list(Tag.class, null);
            if (list != null) {
                for (ApiObjectBase object : list) {
                    resultList.add(apiConnector.findById(object.getClass(), object.getUuid()));
                }
            }
            return resultList;
        } catch (IOException e) {
            return new ArrayList<>();
        }
    }

    private List<ApiObjectBase> getTungstenListTag(List<ObjectReference<ApiPropertyBase>> objectReferenceList) {
        try {
            if (objectReferenceList == null) {
                return new ArrayList<>();
            }

            List<ApiObjectBase> resultList = new ArrayList<>();
            for (ObjectReference<ApiPropertyBase> object : objectReferenceList) {
                resultList.add(apiConnector.findById(Tag.class, object.getUuid()));
            }

            return resultList;
        } catch (IOException e) {
            return new ArrayList<>();
        }
    }

    private List<ApiObjectBase> getObjectList(List<? extends ApiObjectBase> list, String uuid) {
        if (uuid == null)
            return (List<ApiObjectBase>) list;

        for (ApiObjectBase apiObjectBase : list) {
            if (apiObjectBase.getUuid().equals(uuid)) {
                return Arrays.asList(apiObjectBase);
            }
        }

        return new ArrayList<>();
    }

    private List<ApiObjectBase> filterSystemTag(List<ApiObjectBase> tagList) {
        List<ApiObjectBase> result = new ArrayList<>();
        for(ApiObjectBase tag : tagList) {
            String[] tagTypeList = StringUtils.split(tag.getName(), "=");
            if (tagTypeList.length == 2 && !tagTypeList[1].startsWith("fabric")) {
                result.add(tag);
            }
        }
        return result;
    }

    private List<ApiObjectBase> filterSystemTagType(List<TagType> tagTypeList) {
        List<ApiObjectBase> result = new ArrayList<>();
        for(TagType tagType : tagTypeList) {
            if (!tagType.getName().startsWith("neutron")) {
                result.add(tagType);
            }
        }
        return result;
    }

    private List<ApiObjectBase> filterSystemApplicationPolicySet(List<ApplicationPolicySet> applicationPolicySetList) {
        List<ApiObjectBase> result = new ArrayList<>();
        for(ApplicationPolicySet applicationPolicySet : applicationPolicySetList) {
            if (!applicationPolicySet.getName().startsWith(TUNGSTEN_DEFAULT)) {
                result.add(applicationPolicySet);
            }
        }
        return result;
    }

    private List<ApiObjectBase> filterSystemLogicalRouter(List<LogicalRouter> logicalRouterList) {
        List<ApiObjectBase> result = new ArrayList<>();
        for(LogicalRouter logicalRouter : logicalRouterList) {
            if (logicalRouter.getName().startsWith(TungstenUtils.ROUTINGLR_NAME)) {
                result.add(logicalRouter);
            }
        }
        return result;
    }
}
