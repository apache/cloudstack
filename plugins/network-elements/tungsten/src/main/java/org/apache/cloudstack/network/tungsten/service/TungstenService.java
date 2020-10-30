package org.apache.cloudstack.network.tungsten.service;

import com.cloud.network.Network;
import com.cloud.vm.NicProfile;
import net.juniper.tungsten.api.ApiObjectBase;
import net.juniper.tungsten.api.types.InstanceIp;
import net.juniper.tungsten.api.types.NetworkIpam;
import net.juniper.tungsten.api.types.Project;
import net.juniper.tungsten.api.types.VirtualMachine;
import net.juniper.tungsten.api.types.VirtualMachineInterface;
import net.juniper.tungsten.api.types.VirtualNetwork;
import net.juniper.tungsten.api.types.VnSubnetsType;
import org.apache.cloudstack.network.tungsten.vrouter.Port;
import org.apache.cloudstack.network.tungsten.vrouter.VRouterApiConnector;

import java.io.IOException;
import java.util.List;

public interface TungstenService {

    public static final String TUNGSTEN_DEFAULT_DOMAIN = "default-domain";
    public static final String TUNGSTEN_DEFAULT_PROJECT = "default-project";
    public static final String TUNGSTEN_DEFAULT_IPAM = "default-network-ipam";

    void init(long zoneId);

    VRouterApiConnector get_vrouterApi();

    VirtualMachine createVmInTungsten(String vmUuid, String vmName);

    VirtualNetwork deleteNetworkFromTungsten(String tungstenNetworkUuid) throws IOException;

    VirtualNetwork getVirtualNetworkFromTungsten(String virtualNetworkUuid) throws IOException;

    Project getTungstenNetworkProject(long accountId, long domainId) throws IOException;

    InstanceIp createInstanceIpInTungsten(VirtualNetwork vn, VirtualMachineInterface vmi, NicProfile nic);

    VirtualMachineInterface createVmInterfaceInTungsten(NicProfile nicProfile, VirtualNetwork vn, VirtualMachine vm,
                                                        Project project);

    void addTungstenVrouterPort(Port port) throws IOException;

    void deleteTungstenVrouterPort(String portUuid) throws IOException;

    ApiObjectBase getObject(Class<? extends ApiObjectBase> obj, String uuid) throws IOException;

    NetworkIpam getDefaultProjectNetworkIpam(Project project) throws IOException;

    VnSubnetsType getVnSubnetsType(String ipAllocPoolStart, String ipAllocPoolEnd, String subnetIpPrefix,
                                   int subnetIpPrefixLength, String defaultGateway, boolean isDhcpEnabled, List<String> dnsNameservers,
                                   boolean isIpAddrFromStart);

    VirtualNetwork createTungstenVirtualNetwork(Network network, Project project, NetworkIpam networkIpam,
                                                VnSubnetsType subnet);

    void deleteObject(Class<? extends ApiObjectBase> obj, String uuid) throws IOException;
}
