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

import com.cloud.utils.StringUtils;
import com.cloud.utils.TungstenUtils;
import com.cloud.utils.exception.CloudRuntimeException;
import net.juniper.tungsten.api.ApiConnector;
import net.juniper.tungsten.api.ApiObjectBase;
import net.juniper.tungsten.api.ApiPropertyBase;
import net.juniper.tungsten.api.ObjectReference;
import net.juniper.tungsten.api.Status;
import net.juniper.tungsten.api.types.AccessControlList;
import net.juniper.tungsten.api.types.AclRuleType;
import net.juniper.tungsten.api.types.ActionListType;
import net.juniper.tungsten.api.types.AddressGroup;
import net.juniper.tungsten.api.types.AddressType;
import net.juniper.tungsten.api.types.ApplicationPolicySet;
import net.juniper.tungsten.api.types.ConfigRoot;
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
import net.juniper.tungsten.api.types.MatchConditionType;
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
import org.apache.log4j.Logger;

import java.io.IOException;
import java.net.HttpURLConnection;
import java.net.URL;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.concurrent.TimeUnit;

public class TungstenApi {

    private static final Logger S_LOGGER = Logger.getLogger(TungstenApi.class);
    private Status.ErrorHandler errorHandler = new Status.ErrorHandler() {
        @Override
        public void handle(final String s) {
            S_LOGGER.error(s);
        }
    };

    public static final String TUNGSTEN_DEFAULT_DOMAIN = "default-domain";
    public static final String TUNGSTEN_DEFAULT_PROJECT = "default-project";
    public static final String TUNGSTEN_DEFAULT_IPAM = "default-network-ipam";
    public static final String TUNGSTEN_DEFAULT_POLICY_MANAGEMENT = "default-policy-management";
    public static final String TUNGSTEN_GLOBAL_SYSTEM_CONFIG = "default-global-system-config";
    public static final String TUNGSTEN_GLOBAL_VROUTER_CONFIG = "default-global-vrouter-config";

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
            return (VirtualMachine) apiConnector.findByFQN(VirtualMachine.class, getFqnName(virtualMachine));
        } catch (IOException e) {
            S_LOGGER.error("Unable to create Tungsten-Fabric vm " + vmUuid, e);
            return null;
        }
    }

    public VirtualMachineInterface createTungstenVmInterface(String nicUuid, String nicName, String mac,
        String virtualNetworkUuid, String virtualMachineUuid, String projectUuid) {
        VirtualNetwork virtualNetwork = null;
        VirtualMachine virtualMachine = null;
        Project project = null;

        try {
            virtualNetwork = (VirtualNetwork) apiConnector.findById(VirtualNetwork.class, virtualNetworkUuid);
            virtualMachine = (VirtualMachine) apiConnector.findById(VirtualMachine.class, virtualMachineUuid);
            project = (Project) apiConnector.findById(Project.class, projectUuid);
        } catch (IOException e) {
            S_LOGGER.error("Failed getting the resources needed for virtual machine interface creation from Tungsten-Fabric");
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
            Status status = apiConnector.create(virtualMachineInterface);
            status.ifFailure(errorHandler);
            return (VirtualMachineInterface) apiConnector.findById(VirtualMachineInterface.class,
                virtualMachineInterface.getUuid());
        } catch (IOException e) {
            S_LOGGER.error("Failed creating virtual machine interface in Tungsten-Fabric");
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
            S_LOGGER.error("Failed getting the resources needed for virtual machine interface creation from Tungsten-Fabric");
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
            S_LOGGER.error("Failed creating instance ip in Tungsten-Fabric");
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
            S_LOGGER.error("Failed getting the resources needed for virtual machine interface creation from Tungsten-Fabric");
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
            S_LOGGER.error("Failed creating instance ip in Tungsten-Fabric");
            return null;
        }
    }

    public boolean deleteTungstenNetwork(VirtualNetwork network) {
        try {
            Status status = apiConnector.delete(network);
            status.ifFailure(errorHandler);
            return status.isSuccess();
        } catch (IOException e) {
            S_LOGGER.error("Failed deleting the network from Tungsten-Fabric");
            return false;
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
            S_LOGGER.error("Failed deleting the virtual machine interface from Tungsten-Fabric");
            return false;
        }
    }

    public boolean deleteTungstenVm(VirtualMachine virtualMachine) {
        try {
            if (virtualMachine != null) {
                Status status = apiConnector.delete(virtualMachine);
                status.ifFailure(errorHandler);
                return status.isSuccess();
            }
            return true;
        } catch (IOException e) {
            S_LOGGER.error("Failed deleting the virtual machine from Tungsten-Fabric");
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

    public String getFqnName(ApiObjectBase obj) {
        StringBuilder sb = new StringBuilder();
        for (String item : obj.getQualifiedName()) {
            sb.append(item);
            sb.append(":");
        }
        sb.deleteCharAt(sb.toString().length() - 1);
        return sb.toString();
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
            if (fqn == null) {
                return apiConnector.findByFQN(Project.class, TUNGSTEN_DEFAULT_DOMAIN + ":" + TUNGSTEN_DEFAULT_PROJECT);
            } else {
                return apiConnector.findByFQN(Project.class, fqn);
            }
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
                List<String> names = new ArrayList(parent);
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
            VirtualMachineInterface virtualMachineInterface = new VirtualMachineInterface();
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
            //virtualMachineInterface.setDeviceOwner("CS:LOADBALANCER");
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

    public boolean deleteTungstenLogicalRouter(LogicalRouter logicalRouter) {
        try {
            Status status = apiConnector.delete(logicalRouter);
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
                if (status.isSuccess()) {
                    return apiConnector.findById(FloatingIpPool.class, floatingIpPool.getUuid());
                } else {
                    return null;
                }
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
                if (status.isSuccess()) {
                    return apiConnector.findById(FloatingIp.class, floatingIp.getUuid());
                } else {
                    return null;
                }
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
        try {
            // wait for service instance created
            try {
                TimeUnit.SECONDS.sleep(1);
            } catch (InterruptedException e) {
                S_LOGGER.error("can not delay for service instance create");
            }

            Project project = (Project) apiConnector.findById(Project.class, projectUuid);
            List<InstanceIp> instanceIps = (List<InstanceIp>) apiConnector.list(InstanceIp.class, null);
            if (instanceIps != null) {
                for (InstanceIp instanceIp : instanceIps) {
                    if (instanceIp.getQualifiedName()
                        .get(0)
                        .startsWith(
                            TungstenUtils.getSnatNetworkStartName(project.getQualifiedName(), logicalRouterUuid))
                        && instanceIp.getQualifiedName().get(0).endsWith(TungstenUtils.getSnatNetworkEndName())) {
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

    public boolean deleteTungstenFloatingIpPool(FloatingIpPool floatingIpPool) {
        try {
            if (floatingIpPool != null) {
                Status status = apiConnector.delete(floatingIpPool);
                status.ifFailure(errorHandler);
                return status.isSuccess();
            }
            return true;
        } catch (IOException e) {
            return false;
        }
    }

    public boolean deleteTungstenFloatingIp(FloatingIp floatingIp) {
        try {
            if (floatingIp != null) {
                Status status = apiConnector.delete(floatingIp);
                status.ifFailure(errorHandler);
                return status.isSuccess();
            }
            return true;
        } catch (IOException e) {
            return false;
        }
    }

    public boolean deleteTungstenNetworkPolicy(NetworkPolicy networkPolicy) {
        try {
            if (networkPolicy != null) {
                Status status = apiConnector.delete(networkPolicy);
                status.ifFailure(errorHandler);
                return status.isSuccess();
            }
            return true;
        } catch (IOException e) {
            return false;
        }
    }

    public boolean deleteTungstenLoadBalancer(Loadbalancer loadbalancer) {
        try {
            if (loadbalancer != null) {
                Status status = apiConnector.delete(loadbalancer);
                status.ifFailure(errorHandler);
                return status.isSuccess();
            }
            return true;
        } catch (IOException e) {
            return false;
        }
    }

    public boolean deleteTungstenLoadBalancerHealthMonitor(LoadbalancerHealthmonitor loadbalancerHealthmonitor) {
        try {
            if (loadbalancerHealthmonitor != null) {
                Status status = apiConnector.delete(loadbalancerHealthmonitor);
                status.ifFailure(errorHandler);
                return status.isSuccess();
            }
            return true;
        } catch (IOException e) {
            return false;
        }
    }

    public boolean deleteTungstenLoadBalancerMember(LoadbalancerMember loadbalancerMember) {
        try {
            if (loadbalancerMember != null) {
                Status status = apiConnector.delete(loadbalancerMember);
                status.ifFailure(errorHandler);
                return status.isSuccess();
            }
            return true;
        } catch (IOException e) {
            return false;
        }
    }

    public boolean deleteTungstenLoadBalancerPool(LoadbalancerPool loadbalancerPool) {
        try {
            if (loadbalancerPool != null) {
                Status status = apiConnector.delete(loadbalancerPool);
                status.ifFailure(errorHandler);
                return status.isSuccess();
            }
            return true;
        } catch (IOException e) {
            return false;
        }
    }

    public boolean deleteTungstenLoadBalancerListener(LoadbalancerListener loadbalancerListener) {
        try {
            if (loadbalancerListener != null) {
                Status status = apiConnector.delete(loadbalancerListener);
                status.ifFailure(errorHandler);
                return status.isSuccess();
            }
            return true;
        } catch (IOException e) {
            return false;
        }
    }

    public ApiObjectBase createTungstenNetworkPolicy(String name, String projectUuid,
        List<TungstenRule> tungstenRuleList) {
        try {
            Project project = (Project) getTungstenObject(Project.class, projectUuid);
            NetworkPolicy networkPolicy = (NetworkPolicy) apiConnector.find(NetworkPolicy.class, project, name);
            if (networkPolicy == null) {
                PolicyEntriesType policyEntriesType = new PolicyEntriesType();

                getPolicyEntriesType(tungstenRuleList, policyEntriesType);

                networkPolicy = new NetworkPolicy();
                networkPolicy.setName(name);
                networkPolicy.setParent(project);
                networkPolicy.setEntries(policyEntriesType);

                Status status = apiConnector.create(networkPolicy);
                status.ifFailure(errorHandler);
                if (status.isSuccess()) {
                    return apiConnector.findById(NetworkPolicy.class, networkPolicy.getUuid());
                } else {
                    return null;
                }
            } else {
                PolicyEntriesType policyEntriesType = networkPolicy.getEntries();
                if (policyEntriesType == null) {
                    policyEntriesType = new PolicyEntriesType();
                    networkPolicy.setEntries(policyEntriesType);
                }

                getPolicyEntriesType(tungstenRuleList, policyEntriesType);

                Status status = apiConnector.update(networkPolicy);
                status.ifFailure(errorHandler);
                if (status.isSuccess()) {
                    return apiConnector.findById(NetworkPolicy.class, networkPolicy.getUuid());
                } else {
                    return null;
                }
            }
        } catch (IOException e) {
            return null;
        }
    }

    public ApiObjectBase applyTungstenNetworkPolicy(String projectUuid, String networkPolicyName, String networkUuid,
        int majorSequence, int minorSequence) {
        try {
            Project project = (Project) getTungstenObject(Project.class, projectUuid);
            NetworkPolicy networkPolicy = (NetworkPolicy) apiConnector.find(NetworkPolicy.class, project,
                networkPolicyName);
            VirtualNetwork network = (VirtualNetwork) apiConnector.findById(VirtualNetwork.class, networkUuid);

            List<ObjectReference<VirtualNetworkPolicyType>> objectReferenceList = network.getNetworkPolicy();
            if (objectReferenceList != null) {
                for (ObjectReference<VirtualNetworkPolicyType> objectReference : objectReferenceList) {
                    if (objectReference.getUuid().equals(networkPolicy.getUuid())) {
                        return networkPolicy;
                    }
                }
            }

            if (networkPolicy == null || network == null) {
                return null;
            }

            network.addNetworkPolicy(networkPolicy,
                new VirtualNetworkPolicyType(new SequenceType(majorSequence, minorSequence)));
            network.setPerms2(null);
            Status status = apiConnector.update(network);
            status.ifFailure(errorHandler);
            return apiConnector.find(NetworkPolicy.class, project, networkPolicyName);
        } catch (IOException e) {
            return null;
        }
    }

    public ApiObjectBase applyTungstenNetworkPolicy(String policyUuid, String networkUuid, int majorSequence,
        int minorSequence) {
        try {
            NetworkPolicy networkPolicy = (NetworkPolicy) apiConnector.findById(NetworkPolicy.class, policyUuid);
            VirtualNetwork network = (VirtualNetwork) apiConnector.findById(VirtualNetwork.class, networkUuid);

            List<ObjectReference<VirtualNetworkPolicyType>> objectReferenceList = network.getNetworkPolicy();
            if (objectReferenceList != null) {
                for (ObjectReference<VirtualNetworkPolicyType> objectReference : objectReferenceList) {
                    if (objectReference.getUuid().equals(networkPolicy.getUuid())) {
                        return networkPolicy;
                    }
                }
            }

            if (networkPolicy == null || network == null) {
                return null;
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
                apiConnector.create(tungstenDefaultProject);
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
        Domain domain = (Domain) apiConnector.findByFQN(Domain.class, TUNGSTEN_DEFAULT_DOMAIN);
        return domain;
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
            if (monitorType == "HTTP") {
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

    public boolean removeTungstenNetworkPolicy(String projectUuid, String networkUuid, String networkPolicyName) {
        try {
            Project project = (Project) getTungstenObject(Project.class, projectUuid);
            VirtualNetwork virtualNetwork = (VirtualNetwork) apiConnector.findById(VirtualNetwork.class, networkUuid);
            NetworkPolicy networkPolicy = (NetworkPolicy) apiConnector.find(NetworkPolicy.class, project,
                networkPolicyName);
            virtualNetwork.removeNetworkPolicy(networkPolicy,
                new VirtualNetworkPolicyType(new SequenceType(0, 0), null));
            Status status = apiConnector.update(virtualNetwork);
            status.ifFailure(errorHandler);
            return status.isSuccess();
        } catch (IOException e) {
            return false;
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
            VirtualNetwork virtualNetwork = (VirtualNetwork) apiConnector.findById(VirtualNetwork.class,
                publicNetworkUuid);
            VirtualMachineInterface virtualMachineInterface = (VirtualMachineInterface) apiConnector.findById(
                VirtualMachineInterface.class, vmiUuid);
            FloatingIpPool floatingIpPool = (FloatingIpPool) apiConnector.find(FloatingIpPool.class, virtualNetwork,
                floatingIpPoolName);
            FloatingIp floatingIp = (FloatingIp) apiConnector.find(FloatingIp.class, floatingIpPool, floatingIpName);
            PortMappings portMappings = floatingIp.getPortMappings();
            if (isAdd) {
                if (portMappings == null) {
                    portMappings = new PortMappings();
                }

                portMappings.addPortMappings(protocol, publicPort, privatePort);
                floatingIp.setPortMappings(portMappings);
                floatingIp.addVirtualMachineInterface(virtualMachineInterface);
                floatingIp.setPortMappingsEnable(true);
            } else {
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
                    || floatingIp.getVirtualMachineInterface().size() == 0) {
                    floatingIp.setPortMappingsEnable(false);
                }
            }
            Status status = apiConnector.update(floatingIp);
            status.ifFailure(errorHandler);
            return status.isSuccess();
        } catch (IOException e) {
            return false;
        }
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
                return false;
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

    public ApiObjectBase createTungstenTag(final String uuid, final String tagType, final String tagValue) {
        try {
            Tag tag = new Tag();
            tag.setUuid(uuid);
            tag.setName(tagType + "=" + tagValue);
            tag.setTypeName(tagType);
            tag.setValue(tagValue);
            tag.setParent(new ConfigRoot());
            Status status = apiConnector.create(tag);
            status.ifFailure(errorHandler);
            return apiConnector.findById(Tag.class, tag.getUuid());
        } catch (IOException e) {
            return null;
        }
    }

    public ApiObjectBase createTungstenApplicationPolicySet(String uuid, String name) {
        try {
            PolicyManagement policyManagement = (PolicyManagement) apiConnector.find(PolicyManagement.class,
                new ConfigRoot(), TUNGSTEN_DEFAULT_POLICY_MANAGEMENT);
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

    public ApiObjectBase createTungstenFirewallPolicy(String uuid, String name) {
        try {
            PolicyManagement policyManagement = (PolicyManagement) apiConnector.find(PolicyManagement.class,
                new ConfigRoot(), TUNGSTEN_DEFAULT_POLICY_MANAGEMENT);
            if (policyManagement == null) {
                return null;
            }

            FirewallPolicy firewallPolicy = new FirewallPolicy();
            firewallPolicy.setUuid(uuid);
            firewallPolicy.setName(name);
            firewallPolicy.setParent(policyManagement);
            Status status = apiConnector.create(firewallPolicy);
            status.ifFailure(errorHandler);
            return apiConnector.findById(FirewallPolicy.class, firewallPolicy.getUuid());
        } catch (IOException e) {
            return null;
        }
    }

    public ApiObjectBase createTungstenFirewallRule(String uuid, String name, String action, String serviceGroupUuid,
        String srcTagUuid, String srcAddressGroupUuid, String direction, String destTagUuid,
        String destAddressGroupUuid, String tagTypeUuid) {
        try {
            PolicyManagement policyManagement = (PolicyManagement) apiConnector.find(PolicyManagement.class,
                new ConfigRoot(), TUNGSTEN_DEFAULT_POLICY_MANAGEMENT);
            if (policyManagement == null) {
                return null;
            }

            ServiceGroup serviceGroup = (ServiceGroup) apiConnector.findById(ServiceGroup.class, serviceGroupUuid);
            AddressGroup srcAddressGroup = (AddressGroup) apiConnector.findById(AddressGroup.class,
                srcAddressGroupUuid);
            AddressGroup destAddressGroup = (AddressGroup) apiConnector.findById(AddressGroup.class,
                destAddressGroupUuid);
            Tag srcTag = (Tag) apiConnector.findById(Tag.class, srcTagUuid);
            Tag destTag = (Tag) apiConnector.findById(Tag.class, destTagUuid);
            TagType tagType = (TagType) apiConnector.findById(TagType.class, tagTypeUuid);
            if (serviceGroup == null) {
                return null;
            }

            if (srcAddressGroup == null && srcTag == null) {
                return null;
            }

            if (destAddressGroup == null && destTag == null) {
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
                srcFirewallRuleEndpointType.addTags(srcTag.getName());
            } else {
                String srcAddressGroupName = StringUtils.join(srcAddressGroup.getQualifiedName(), ":");
                srcFirewallRuleEndpointType.setAddressGroup(srcAddressGroupName);
            }

            FirewallRuleEndpointType destFirewallRuleEndpointType = new FirewallRuleEndpointType();
            if (destTag != null) {
                destFirewallRuleEndpointType.addTags(destTag.getName());
            } else {
                String destAddressGroupName = StringUtils.join(destAddressGroup.getQualifiedName(), ":");
                destFirewallRuleEndpointType.setAddressGroup(destAddressGroupName);
            }

            firewallRule.setEndpoint1(srcFirewallRuleEndpointType);
            firewallRule.setDirection(direction);
            firewallRule.setEndpoint2(destFirewallRuleEndpointType);
            FirewallRuleMatchTagsType firewallRuleMatchTagsType = new FirewallRuleMatchTagsType();
            if (tagType != null) {
                firewallRuleMatchTagsType.addTag(tagType.getName());
                firewallRule.setMatchTags(firewallRuleMatchTagsType);
            }

            Status status = apiConnector.create(firewallRule);
            status.ifFailure(errorHandler);
            return apiConnector.findById(FirewallRule.class, firewallRule.getUuid());
        } catch (IOException e) {
            return null;
        }
    }

    public ApiObjectBase createTungstenServiceGroup(String uuid, String name, String protocol, int startPort,
        int endPort) {
        try {
            PolicyManagement policyManagement = (PolicyManagement) apiConnector.find(PolicyManagement.class,
                new ConfigRoot(), TUNGSTEN_DEFAULT_POLICY_MANAGEMENT);
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
            PolicyManagement policyManagement = (PolicyManagement) apiConnector.find(PolicyManagement.class,
                new ConfigRoot(), TUNGSTEN_DEFAULT_POLICY_MANAGEMENT);
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

    public ApiObjectBase removeTungstenTag(List<String> networkUuids, List<String> vmUuids, List<String> nicUuids,
        String policyUuid, String tagUuid) {
        try {
            Tag tag = (Tag) getTungstenObject(Tag.class, tagUuid);
            if (tag == null) {
                return null;
            }

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

            if (nicUuids != null) {
                for (String nicUuid : nicUuids) {
                    VirtualMachineInterface virtualMachineInterface = (VirtualMachineInterface) getTungstenObject(
                        VirtualMachineInterface.class, nicUuid);
                    if (tag != null && virtualMachineInterface != null) {
                        virtualMachineInterface.removeTag(tag);
                        Status status = apiConnector.update(virtualMachineInterface);
                        status.ifFailure(errorHandler);
                    }
                }
            }

            if (policyUuid != null) {
                NetworkPolicy networkPolicy = (NetworkPolicy) getTungstenObject(NetworkPolicy.class, policyUuid);
                if (tag != null && networkPolicy != null) {
                    networkPolicy.removeTag(tag);
                    Status status = apiConnector.update(networkPolicy);
                    status.ifFailure(errorHandler);
                }
            }

            return apiConnector.findById(Tag.class, tagUuid);
        } catch (IOException e) {
            return null;
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

    public ApiObjectBase addTungstenFirewallPolicy(final String applicationPolicySetUuid,
        final String firewallPolicyUuid, final int sequence, final String tagUuid) {
        try {
            ApplicationPolicySet applicationPolicySet = (ApplicationPolicySet) apiConnector.findById(
                ApplicationPolicySet.class, applicationPolicySetUuid);
            FirewallPolicy firewallPolicy = (FirewallPolicy) apiConnector.findById(FirewallPolicy.class,
                firewallPolicyUuid);
            Tag tag = (Tag) apiConnector.findById(Tag.class, tagUuid);
            if (applicationPolicySet == null || firewallPolicy == null || tag == null) {
                return null;
            }

            applicationPolicySet.setFirewallPolicy(firewallPolicy, new FirewallSequence(String.valueOf(sequence)));
            applicationPolicySet.setTag(tag);

            Status status = apiConnector.update(applicationPolicySet);
            status.ifFailure(errorHandler);
            return apiConnector.findById(ApplicationPolicySet.class, applicationPolicySetUuid);
        } catch (IOException e) {
            return null;
        }
    }

    public ApiObjectBase addTungstenFirewallRule(final String firewallPolicyUuid, final String firewallRuleUuid,
        final int sequence) {
        try {
            FirewallPolicy firewallPolicy = (FirewallPolicy) apiConnector.findById(FirewallPolicy.class,
                firewallPolicyUuid);
            FirewallRule firewallRule = (FirewallRule) apiConnector.findById(FirewallRule.class, firewallRuleUuid);
            if (firewallPolicy == null || firewallRule == null) {
                return null;
            }
            firewallPolicy.addFirewallRule(firewallRule, new FirewallSequence(String.valueOf(sequence)));
            Status status = apiConnector.update(firewallPolicy);
            status.ifFailure(errorHandler);
            return apiConnector.findById(FirewallPolicy.class, firewallPolicyUuid);
        } catch (IOException e) {
            return null;
        }
    }

    public ApiObjectBase listTungstenPolicyRule(final String policyUuid) {
        try {
            NetworkPolicy networkPolicy = (NetworkPolicy) apiConnector.findById(NetworkPolicy.class, policyUuid);
            return networkPolicy;
        } catch (IOException e) {
            return null;
        }
    }

    public List<? extends ApiObjectBase> listTungstenAddressPolicy(String projectFqn, String policyName) {
        Project project = (Project) getTungstenProjectByFqn(projectFqn);
        List<NetworkPolicy> networkPolicyList = new ArrayList<>();
        NetworkPolicy networkPolicy = (NetworkPolicy) getTungstenObjectByName(NetworkPolicy.class,
            project.getQualifiedName(), policyName);

        if (networkPolicy != null) {
            networkPolicyList.add(networkPolicy);
        }

        return networkPolicyList;
    }

    public List<? extends ApiObjectBase> listTungstenPolicy(String projectFqn, String policyUuid) {
        Project project = (Project) getTungstenProjectByFqn(projectFqn);
        return getTungstenListObject(NetworkPolicy.class, project, policyUuid);
    }

    public List<? extends ApiObjectBase> listTungstenNetwork(String projectFqn, String networkUuid) {
        Project project = (Project) getTungstenProjectByFqn(projectFqn);
        return getTungstenListObject(VirtualNetwork.class, project, networkUuid);
    }

    public List<? extends ApiObjectBase> listTungstenVm(String projectFqn, String vmUuid) {
        Project project = (Project) getTungstenProjectByFqn(projectFqn);
        return getTungstenListObject(VirtualMachine.class, project, vmUuid);
    }

    public List<? extends ApiObjectBase> listTungstenNic(String projectFqn, String nicUuid) {
        Project project = (Project) getTungstenProjectByFqn(projectFqn);
        return getTungstenListObject(VirtualMachineInterface.class, project, nicUuid);
    }

    public List<? extends ApiObjectBase> listTungstenTag(String networkUuid, String vmUuid, String nicUuid,
        String policyUuid, String tagUuid) {
        try {
            List<Tag> tagList = new ArrayList<>();

            if (networkUuid != null) {
                VirtualNetwork virtualNetwork = (VirtualNetwork) apiConnector.findById(VirtualNetwork.class,
                    networkUuid);
                tagList = (List<Tag>) getTungstenListObject(Tag.class, virtualNetwork.getTag());
            } else if (vmUuid != null) {
                VirtualMachine virtualMachine = (VirtualMachine) apiConnector.findById(VirtualMachine.class, vmUuid);
                tagList = (List<Tag>) getTungstenListObject(Tag.class, virtualMachine.getTag());
            } else if (nicUuid != null) {
                VirtualMachineInterface virtualMachineInterface = (VirtualMachineInterface) apiConnector.findById(
                    VirtualMachineInterface.class, nicUuid);
                tagList = (List<Tag>) getTungstenListObject(Tag.class, virtualMachineInterface.getTag());
            } else if (policyUuid != null) {
                NetworkPolicy networkPolicy = (NetworkPolicy) apiConnector.findById(NetworkPolicy.class, policyUuid);
                tagList = (List<Tag>) getTungstenListObject(Tag.class, networkPolicy.getTag());
            } else {
                tagList = (List<Tag>) getTungstenListObject(Tag.class);
            }

            return getObjectList(tagList, tagUuid);
        } catch (IOException e) {
            return new ArrayList<>();
        }
    }

    public List<? extends ApiObjectBase> listTungstenTagType(String tagTypeUuid) {
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
            return tagTypeList;
        } catch (IOException e) {
            return new ArrayList<>();
        }
    }

    public List<? extends ApiObjectBase> listTungstenNetworkPolicy(String networkUuid, String policyUuid) {
        try {
            VirtualNetwork virtualNetwork = (VirtualNetwork) apiConnector.findById(VirtualNetwork.class, networkUuid);
            List<NetworkPolicy> networkPolicyList = new ArrayList<>();
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

    public List<? extends ApiObjectBase> listTungstenApplicationPolicySet(String applicationPolicySetUuid) {
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
            return applicationPolicySetList;
        } catch (IOException e) {
            return new ArrayList<>();
        }
    }

    public List<? extends ApiObjectBase> listTungstenFirewallPolicy(String applicationPolicySetUuid,
        String firewallPolicyUuid) {
        try {
            List<FirewallPolicy> firewallPolicyList = new ArrayList<>();
            if (applicationPolicySetUuid != null) {
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
            } else {
                List<FirewallPolicy> firewallPolicies = (List<FirewallPolicy>) apiConnector.list(FirewallPolicy.class,
                    null);
                if (firewallPolicies != null) {
                    for (FirewallPolicy firewallPolicy : firewallPolicies) {
                        if (firewallPolicyUuid != null) {
                            if (firewallPolicy.getUuid().equals(firewallPolicyUuid)) {
                                firewallPolicyList.add(
                                    (FirewallPolicy) apiConnector.findById(FirewallPolicy.class, firewallPolicyUuid));
                            }
                        } else {
                            String parentName = firewallPolicy.getQualifiedName().get(0);
                            if (parentName.equals(TUNGSTEN_DEFAULT_POLICY_MANAGEMENT)) {
                                firewallPolicyList.add((FirewallPolicy) apiConnector.findById(FirewallPolicy.class,
                                    firewallPolicy.getUuid()));
                            }
                        }
                    }
                }
            }
            return firewallPolicyList;
        } catch (IOException e) {
            return new ArrayList<>();
        }
    }

    public List<? extends ApiObjectBase> listTungstenFirewallRule(String firewallPolicyUuid, String firewallRuleUuid) {
        try {
            List<FirewallRule> firewallRuleList = new ArrayList<>();
            if (firewallPolicyUuid != null) {
                FirewallPolicy firewallPolicy = (FirewallPolicy) apiConnector.findById(FirewallPolicy.class,
                    firewallPolicyUuid);
                List<ObjectReference<FirewallSequence>> objectReferenceList = firewallPolicy.getFirewallRule();
                if (objectReferenceList != null) {
                    for (ObjectReference<FirewallSequence> objectReference : objectReferenceList) {
                        FirewallRule firewallRule = (FirewallRule) apiConnector.findById(FirewallPolicy.class,
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
            } else {
                List<FirewallRule> firewallRules = (List<FirewallRule>) apiConnector.list(FirewallRule.class, null);
                if (firewallRules != null) {
                    for (FirewallRule firewallRule : firewallRules) {
                        if (firewallRuleUuid != null) {
                            if (firewallRule.getUuid().equals(firewallRuleUuid)) {
                                firewallRuleList.add(
                                    (FirewallRule) apiConnector.findById(FirewallRule.class, firewallRuleUuid));
                            }
                        } else {
                            String parentName = firewallRule.getQualifiedName().get(0);
                            if (parentName.equals(TUNGSTEN_DEFAULT_POLICY_MANAGEMENT)) {
                                firewallRuleList.add(
                                    (FirewallRule) apiConnector.findById(FirewallRule.class, firewallRule.getUuid()));
                            }
                        }
                    }
                }
            }
            return firewallRuleList;
        } catch (IOException e) {
            return new ArrayList<>();
        }
    }

    public List<? extends ApiObjectBase> listTungstenServiceGroup(String serviceGroupUuid) {
        try {
            List<ServiceGroup> serviceGroupList = new ArrayList<>();
            List<ServiceGroup> serviceGroups = (List<ServiceGroup>) apiConnector.list(ServiceGroup.class, null);
            if (serviceGroups != null) {
                for (ServiceGroup serviceGroup : serviceGroups) {
                    if (serviceGroupUuid != null) {
                        if (serviceGroup.getUuid().equals(serviceGroupUuid)) {
                            serviceGroupList.add(
                                (ServiceGroup) apiConnector.findById(ServiceGroup.class, serviceGroupUuid));
                        }
                    } else {
                        String parentName = serviceGroup.getQualifiedName().get(0);
                        if (parentName.equals(TUNGSTEN_DEFAULT_POLICY_MANAGEMENT)) {
                            serviceGroupList.add(
                                (ServiceGroup) apiConnector.findById(ServiceGroup.class, serviceGroup.getUuid()));
                        }
                    }
                }
            }
            return serviceGroupList;
        } catch (IOException e) {
            return new ArrayList<>();
        }
    }

    public List<? extends ApiObjectBase> listTungstenAddressGroup(String addressGroupUuid) {
        try {
            List<AddressGroup> addressGroupList = new ArrayList<>();
            List<AddressGroup> addressGroups = (List<AddressGroup>) apiConnector.list(AddressGroup.class, null);
            if (addressGroups != null) {
                for (AddressGroup addressGroup : addressGroups) {
                    if (addressGroupUuid != null) {
                        if (addressGroup.getUuid().equals(addressGroupUuid)) {
                            addressGroupList.add(
                                (AddressGroup) apiConnector.findById(AddressGroup.class, addressGroupUuid));
                        }
                    } else {
                        String parentName = addressGroup.getQualifiedName().get(0);
                        if (parentName.equals(TUNGSTEN_DEFAULT_POLICY_MANAGEMENT)) {
                            addressGroupList.add(
                                (AddressGroup) apiConnector.findById(AddressGroup.class, addressGroup.getUuid()));
                        }
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

    public ApiObjectBase removeTungstenFirewallPolicy(String applicationPolicySetUuid, String firewallPolicyUuid) {
        try {
            ApplicationPolicySet applicationPolicySet = (ApplicationPolicySet) apiConnector.findById(
                ApplicationPolicySet.class, applicationPolicySetUuid);
            FirewallPolicy firewallPolicy = (FirewallPolicy) apiConnector.findById(FirewallPolicy.class,
                firewallPolicyUuid);
            if (applicationPolicySet == null || firewallPolicy == null) {
                return null;
            }
            applicationPolicySet.removeFirewallPolicy(firewallPolicy, new FirewallSequence());
            applicationPolicySet.clearTag();
            Status status = apiConnector.update(applicationPolicySet);
            status.ifFailure(errorHandler);
            return apiConnector.findById(ApplicationPolicySet.class, applicationPolicySetUuid);
        } catch (IOException e) {
            return null;
        }
    }

    public ApiObjectBase removeTungstenFirewallRule(String firewallPolicyUuid, String firewallRuleUuid) {
        try {
            FirewallPolicy firewallPolicy = (FirewallPolicy) apiConnector.findById(FirewallPolicy.class,
                firewallPolicyUuid);
            FirewallRule firewallRule = (FirewallRule) apiConnector.findById(FirewallRule.class, firewallRuleUuid);
            if (firewallPolicy == null || firewallRule == null) {
                return null;
            }
            firewallPolicy.removeFirewallRule(firewallRule, new FirewallSequence());
            Status status = apiConnector.update(firewallPolicy);
            status.ifFailure(errorHandler);
            return apiConnector.findById(FirewallPolicy.class, firewallPolicyUuid);
        } catch (IOException e) {
            return null;
        }
    }

    public ApiObjectBase updateTungstenVrouterConfig(String forwardingMode) {
        try {
            GlobalSystemConfig globalSystemConfig = (GlobalSystemConfig) apiConnector.find(GlobalSystemConfig.class,
                null, TUNGSTEN_GLOBAL_SYSTEM_CONFIG);
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

    public List<? extends ApiObjectBase> getTungstenListObject(Class<? extends ApiObjectBase> cls, ApiObjectBase parent,
        String uuid) {
        try {
            List resultList = new ArrayList();
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

        if (gateway != null) {
            ipamSubnetType.setDefaultGateway(gateway);
        }

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
            String srcNetwork = tungstenRule.getSrcNetwork();
            String dstNetwork = tungstenRule.getDstNetwork();

            if (srcNetwork == null || srcNetwork.isEmpty() || srcNetwork.isBlank()) {
                srcNetwork = TungstenUtils.ANY;
            }

            if (dstNetwork == null || dstNetwork.isEmpty() || dstNetwork.isBlank()) {
                dstNetwork = TungstenUtils.ANY;
            }

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

    public String getSubnetUuid(String networkUuid) {
        try {
            VirtualNetwork network = (VirtualNetwork) apiConnector.findById(VirtualNetwork.class, networkUuid);
            if (network != null) {
                List<ObjectReference<VnSubnetsType>> listIpam = network.getNetworkIpam();
                if (listIpam != null) {
                    for (ObjectReference<VnSubnetsType> objectReference : listIpam) {
                        VnSubnetsType vnSubnetsType = objectReference.getAttr();
                        List<IpamSubnetType> ipamSubnetTypeList = vnSubnetsType.getIpamSubnets();
                        for (IpamSubnetType ipamSubnetType : ipamSubnetTypeList) {
                            return ipamSubnetType.getSubnetUuid();
                        }
                    }
                }
                return null;
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

    public boolean deleteTungstenSecurityGroup(String tungstenSecurityGroupUuid) {
        try {
            SecurityGroup securityGroup = (SecurityGroup) getTungstenObject(SecurityGroup.class,
                tungstenSecurityGroupUuid);
            if (securityGroup != null) {
                Status status = apiConnector.delete(SecurityGroup.class, tungstenSecurityGroupUuid);
                status.ifFailure(errorHandler);
                return status.isSuccess();
            }
            return true;
        } catch (IOException e) {
            return false;
        }
    }

    public boolean addTungstenSecurityGroupRule(String tungstenSecurityGroupUuid, String securityGroupRuleUuid,
        String securityGroupRuleType, int startPort, int endPort, String cidr, String ipPrefix, int ipPrefixLen,
        String protocol) {
        try {
            SecurityGroup securityGroup = (SecurityGroup) getTungstenObject(SecurityGroup.class,
                tungstenSecurityGroupUuid);
            if (securityGroup == null) {
                return false;
            }
            for (ObjectReference<ApiPropertyBase> accessControl : securityGroup.getAccessControlLists()) {
                AccessControlList accessControlList = (AccessControlList) getTungstenObject(AccessControlList.class,
                    accessControl.getUuid());
                if (accessControlList.getName().equals(TungstenUtils.getTungstenAccessControl(securityGroupRuleType))) {
                    AclRuleType aclRuleType = createAclRuleType(securityGroupRuleUuid, securityGroupRuleType, startPort,
                        endPort, cidr, ipPrefix, ipPrefixLen, protocol);
                    PolicyRuleType policyRuleType = createPolicyRuleType(aclRuleType.getMatchCondition(),
                        securityGroupRuleUuid);
                    accessControlList.getEntries().addAclRule(aclRuleType);
                    if (securityGroup.getEntries() == null) {
                        securityGroup.setEntries(new PolicyEntriesType(new ArrayList<>(Arrays.asList(policyRuleType))));
                    } else {
                        securityGroup.getEntries().addPolicyRule(policyRuleType);
                    }
                    Status accessControlListStatus = apiConnector.update(accessControlList);
                    if (!accessControlListStatus.isSuccess()) {
                        return false;
                    }
                    Status securityGroupStatus = apiConnector.update(securityGroup);
                    return securityGroupStatus.isSuccess();
                }
            }

        } catch (IOException e) {
            return false;
        }
        return false;
    }

    public boolean removeTungstenSecurityGroupRule(String tungstenSecurityGroupUuid, String securityGroupRuleUuid,
        String securityGroupRuleType) {
        try {
            SecurityGroup securityGroup = (SecurityGroup) getTungstenObject(SecurityGroup.class,
                tungstenSecurityGroupUuid);
            if (securityGroup == null) {
                return false;
            }
            for (ObjectReference<ApiPropertyBase> accessControl : securityGroup.getAccessControlLists()) {
                AccessControlList accessControlList = (AccessControlList) getTungstenObject(AccessControlList.class,
                    accessControl.getUuid());
                if (accessControlList.getName().equals(TungstenUtils.getTungstenAccessControl(securityGroupRuleType))) {
                    List<AclRuleType> existingAclList = accessControlList.getEntries().getAclRule();
                    accessControlList.getEntries().clearAclRule();
                    for (AclRuleType aclRuleType : existingAclList) {
                        if (!aclRuleType.getRuleUuid().equals(securityGroupRuleUuid)) {
                            accessControlList.getEntries().addAclRule(aclRuleType);
                        }
                    }
                    Status status = apiConnector.update(accessControlList);
                    return status.isSuccess();
                }
            }
        } catch (IOException e) {
            return false;
        }
        return true;
    }

    public boolean removeTungstenPolicyRule(String tungstenSecurityGroupUuid, String securityGroupRuleUuid) {
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

    public AclRuleType createAclRuleType(String securityGroupRuleUuid, String securityGroupRuleType, int startPort,
        int endPort, String cidr, String ipPrefix, int ipPrefixLen, String protocol) {
        String tungstenProtocol = TungstenUtils.getTungstenProtocol(protocol, cidr);
        String tungstenEthertType = TungstenUtils.getEthertTypeFromCidr(cidr);
        AddressType addressType = new AddressType(new SubnetType(ipPrefix, ipPrefixLen), null, null, null);

        MatchConditionType matchConditionType = new MatchConditionType();
        matchConditionType.setSrcPort(new PortType(0, 65535));
        matchConditionType.setDstPort(new PortType(startPort, endPort));
        if (securityGroupRuleType.equals(TungstenUtils.INGRESS_RULE)) {
            matchConditionType.setSrcAddress(addressType);
            matchConditionType.setDstAddress(new AddressType(null, null, "local", null));
        } else if (securityGroupRuleType.equals(TungstenUtils.EGRESS_RULE)) {
            matchConditionType.setDstAddress(addressType);
            matchConditionType.setSrcAddress(new AddressType(null, null, "local", null));
        }
        matchConditionType.setProtocol(tungstenProtocol);
        matchConditionType.setEthertype(tungstenEthertType);
        AclRuleType aclRuleType = new AclRuleType(matchConditionType);
        aclRuleType.setDirection(TungstenUtils.ONE_WAY_DIRECTION);
        aclRuleType.setRuleUuid(securityGroupRuleUuid);
        return aclRuleType;
    }

    public PolicyRuleType createPolicyRuleType(MatchConditionType matchConditionType, String securityGroupRuleUuid) {
        PolicyRuleType policyRuleType = new PolicyRuleType();
        policyRuleType.setDirection(TungstenUtils.ONE_WAY_DIRECTION);
        policyRuleType.setProtocol(matchConditionType.getProtocol());
        policyRuleType.setEthertype(matchConditionType.getEthertype());
        policyRuleType.setRuleUuid(securityGroupRuleUuid);
        policyRuleType.addSrcPorts(matchConditionType.getSrcPort());
        policyRuleType.addDstPorts(matchConditionType.getDstPort());
        policyRuleType.addDstAddresses(matchConditionType.getDstAddress());
        policyRuleType.addSrcAddresses(matchConditionType.getSrcAddress());
        return policyRuleType;
    }

    public boolean addInstanceToSecurityGroup(String vmUuid, List<String> securityGroupUuidList) {
        try {
            VirtualMachine vm = (VirtualMachine) apiConnector.findById(VirtualMachine.class, vmUuid);
            if (vm == null) {
                return false;
            }

            VirtualMachineInterface vmi = null;
            for (ObjectReference obj : vm.getVirtualMachineInterfaceBackRefs()) {
                VirtualMachineInterface tungstenVmi = (VirtualMachineInterface) apiConnector.findById(
                    VirtualMachineInterface.class, obj.getUuid());
                if (tungstenVmi.getName().startsWith("vmi")) {
                    vmi = tungstenVmi;
                    break;
                }
            }

            if (vmi == null) {
                return false;
            }
            for (String securityGroupUuid : securityGroupUuidList) {
                SecurityGroup tungstenSecurityGroup = (SecurityGroup) apiConnector.findById(SecurityGroup.class,
                    securityGroupUuid);
                if (tungstenSecurityGroup != null) {
                    vmi.addSecurityGroup(tungstenSecurityGroup);
                    apiConnector.update(vmi);
                }
            }
            return true;
        } catch (IOException e) {
            return false;
        }
    }

    private List<? extends ApiObjectBase> getTungstenListObject(Class<? extends ApiObjectBase> cls) {
        try {
            List resultList = new ArrayList();
            List<? extends ApiObjectBase> list = apiConnector.list(cls, null);
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

    private List<? extends ApiObjectBase> getTungstenListObject(Class<? extends ApiObjectBase> cls,
        List<ObjectReference<ApiPropertyBase>> objectReferenceList) {
        try {
            if (objectReferenceList == null) {
                return new ArrayList<>();
            }

            List resultList = new ArrayList();
            for (ObjectReference<ApiPropertyBase> object : objectReferenceList) {
                resultList.add(apiConnector.findById(cls, object.getUuid()));
            }

            return resultList;
        } catch (IOException e) {
            return new ArrayList<>();
        }
    }

    private List<? extends ApiObjectBase> getObjectList(List<? extends ApiObjectBase> list, String uuid) {
        if (uuid == null)
            return list;

        for (ApiObjectBase apiObjectBase : list) {
            if (apiObjectBase.getUuid().equals(uuid)) {
                return Arrays.asList(apiObjectBase);
            }
        }

        return new ArrayList<>();
    }
}
