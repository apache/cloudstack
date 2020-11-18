package org.apache.cloudstack.network.tungsten.service;

import net.juniper.tungsten.api.ApiConnector;
import net.juniper.tungsten.api.ApiObjectBase;
import net.juniper.tungsten.api.ApiPropertyBase;
import net.juniper.tungsten.api.ObjectReference;
import net.juniper.tungsten.api.Status;
import net.juniper.tungsten.api.types.Domain;
import net.juniper.tungsten.api.types.FloatingIp;
import net.juniper.tungsten.api.types.FloatingIpPool;
import net.juniper.tungsten.api.types.InstanceIp;
import net.juniper.tungsten.api.types.IpamSubnetType;
import net.juniper.tungsten.api.types.LogicalRouter;
import net.juniper.tungsten.api.types.MacAddressesType;
import net.juniper.tungsten.api.types.NetworkIpam;
import net.juniper.tungsten.api.types.Project;
import net.juniper.tungsten.api.types.SubnetType;
import net.juniper.tungsten.api.types.VirtualMachine;
import net.juniper.tungsten.api.types.VirtualMachineInterface;
import net.juniper.tungsten.api.types.VirtualNetwork;
import net.juniper.tungsten.api.types.VnSubnetsType;
import org.apache.cloudstack.api.ApiErrorCode;
import org.apache.cloudstack.api.ServerApiException;
import org.apache.log4j.Logger;

import java.io.IOException;
import java.net.HttpURLConnection;
import java.net.URL;
import java.util.ArrayList;
import java.util.List;

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

    private String hostname;
    private String port;
    private String zoneId;
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

    public String getZoneId() {
        return zoneId;
    }

    public void setZoneId(final String zoneId) {
        this.zoneId = zoneId;
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
                    "There is not a tungsten provider using hostname: " + hostname + " and port: " + port);
            }
        } catch (IOException e) {
            throw new ServerApiException(ApiErrorCode.RESOURCE_UNAVAILABLE_ERROR,
                "There is not a tungsten provider using hostname: " + hostname + " and port: " + port);
        }
    }

    public VirtualNetwork createTungstenNetwork(String uuid, String name, String parent, boolean routerExternal,
        boolean shared, String ipPrefix, int ipPrefixLen, String gateway, boolean dhcpEnable, List<String> dnsServers,
        String allocationStart, String allocationEnd, boolean ipFromStart) {
        try {
            Project project = (Project) apiConnector.findById(Project.class, parent);
            NetworkIpam networkIpam = getDefaultProjectNetworkIpam(project);
            IpamSubnetType ipamSubnetType = new IpamSubnetType();
            ipamSubnetType.setSubnet(new SubnetType(ipPrefix, ipPrefixLen));
            ipamSubnetType.setDefaultGateway(gateway);
            ipamSubnetType.setEnableDhcp(dhcpEnable);
            ipamSubnetType.setAddrFromStart(ipFromStart);

            if (allocationStart != null && allocationEnd != null) {
                ipamSubnetType.addAllocationPools(allocationStart, allocationEnd, false);
            }

            VnSubnetsType vnSubnetsType = new VnSubnetsType();
            vnSubnetsType.addIpamSubnets(ipamSubnetType);
            VirtualNetwork virtualNetwork = new VirtualNetwork();
            if (uuid != null) {
                virtualNetwork.setUuid(uuid);
            }
            virtualNetwork.setName(name);
            virtualNetwork.addNetworkIpam(networkIpam, vnSubnetsType);
            virtualNetwork.setParent(project);
            virtualNetwork.setRouterExternal(routerExternal);
            virtualNetwork.setIsShared(shared);
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
            S_LOGGER.error("Unable to create tungsten vm " + vmUuid, e);
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
            S_LOGGER.error("Failed getting the resources needed for virtual machine interface creation from tungsten");
        }

        VirtualMachineInterface virtualMachineInterface = new VirtualMachineInterface();
        try {
            virtualMachineInterface.setUuid(nicUuid);
            virtualMachineInterface.setName(nicName);
            virtualMachineInterface.setVirtualNetwork(virtualNetwork);
            virtualMachineInterface.setVirtualMachine(virtualMachine);
            virtualMachineInterface.setParent(project);
            MacAddressesType macAddressesType = new MacAddressesType();
            macAddressesType.addMacAddress(mac);
            virtualMachineInterface.setMacAddresses(macAddressesType);
            Status status = apiConnector.create(virtualMachineInterface);
            status.ifFailure(errorHandler);
            return (VirtualMachineInterface) apiConnector.findById(VirtualMachineInterface.class,
                virtualMachineInterface.getUuid());
        } catch (IOException e) {
            S_LOGGER.error("Failed creating virtual machine interface in tungsten");
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
            S_LOGGER.error("Failed getting the resources needed for virtual machine interface creation from tungsten");
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
            S_LOGGER.error("Failed creating instance ip in tungsten");
            return null;
        }
    }

    public boolean deleteTungstenNetwork(VirtualNetwork network) {
        try {
            Status status = apiConnector.delete(network);
            status.ifFailure(errorHandler);
            return status.isSuccess();
        } catch (IOException e) {
            S_LOGGER.error("Failed deleting the network from tungsten");
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
            S_LOGGER.error("Failed deleting the virtual machine interface from tungsten");
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
            S_LOGGER.error("Failed deleting the virtual machine from tungsten");
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

    public ApiObjectBase getTungstenNetworkProject(String uuid) {
        try {
            if (uuid == null) {
                return apiConnector.findByFQN(Project.class, TUNGSTEN_DEFAULT_DOMAIN + ":" + TUNGSTEN_DEFAULT_PROJECT);
            } else {
                return apiConnector.findById(Project.class, uuid);
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
            Status status = apiConnector.create(virtualMachineInterface);
            status.ifFailure(errorHandler);
            return apiConnector.findById(VirtualMachineInterface.class, virtualMachineInterface.getUuid());
        } catch (IOException ex) {
            return null;
        }
    }

    public ApiObjectBase createTungstenGatewayInstanceIp(String name, String vnUuid, String vmiUuid, String address) {
        try {
            VirtualNetwork virtualNetwork = (VirtualNetwork) apiConnector.findById(VirtualNetwork.class, vnUuid);
            VirtualMachineInterface virtualMachineInterface = (VirtualMachineInterface) apiConnector.findById(
                VirtualMachineInterface.class, vmiUuid);
            InstanceIp instanceIp = new InstanceIp();
            instanceIp.setName(name);
            instanceIp.setVirtualNetwork(virtualNetwork);
            instanceIp.setVirtualMachineInterface(virtualMachineInterface);
            instanceIp.setAddress(address);
            Status status = apiConnector.create(instanceIp);
            status.ifFailure(errorHandler);
            return apiConnector.findById(InstanceIp.class, instanceIp.getUuid());
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

    public ApiObjectBase createTungstenFloatingIp(String projectUuid, String networkUuid, String vmiUuid,
        String fipName, String name, String publicIp, String privateIp) {
        try {
            Project project = (Project) apiConnector.findById(Project.class, projectUuid);
            VirtualNetwork virtualNetwork = (VirtualNetwork) apiConnector.findById(VirtualNetwork.class, networkUuid);
            FloatingIpPool fip = (FloatingIpPool) apiConnector.find(FloatingIpPool.class, virtualNetwork, fipName);
            VirtualMachineInterface vmi = (VirtualMachineInterface) apiConnector.findById(VirtualMachineInterface.class,
                vmiUuid);
            FloatingIp floatingIp = new FloatingIp();
            floatingIp.setName(name);
            floatingIp.setParent(fip);
            floatingIp.setProject(project);
            floatingIp.setVirtualMachineInterface(vmi);
            floatingIp.setAddress(publicIp);
            floatingIp.setFixedIpAddress(privateIp);
            Status status = apiConnector.create(floatingIp);
            status.ifFailure(errorHandler);
            if (status.isSuccess()) {
                return apiConnector.findById(FloatingIp.class, floatingIp.getUuid());
            } else {
                return null;
            }
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
}
