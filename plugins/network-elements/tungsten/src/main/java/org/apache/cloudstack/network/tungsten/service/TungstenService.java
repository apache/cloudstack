package org.apache.cloudstack.network.tungsten.service;

import com.cloud.network.Network;
import com.cloud.user.Account;
import com.cloud.vm.NicProfile;
import net.juniper.contrail.api.ApiConnector;
import net.juniper.contrail.api.ApiObjectBase;
import net.juniper.contrail.api.types.InstanceIp;
import net.juniper.contrail.api.types.NetworkIpam;
import net.juniper.contrail.api.types.Project;
import net.juniper.contrail.api.types.VirtualMachine;
import net.juniper.contrail.api.types.VirtualMachineInterface;
import net.juniper.contrail.api.types.VirtualNetwork;
import net.juniper.contrail.api.types.VnSubnetsType;
import org.apache.cloudstack.network.tungsten.vrouter.Port;
import org.apache.cloudstack.network.tungsten.vrouter.VRouterApiConnector;

import java.io.IOException;
import java.util.List;

public interface TungstenService {

    public static final String TUNGSTEN_DEFAULT_DOMAIN = "default-domain";
    public static final String TUNGSTEN_DEFAULT_PROJECT = "default-project";
    public static final String TUNGSTEN_DEFAULT_IPAM = "default-network-ipam";

    void init();

    ApiConnector get_api();

    VRouterApiConnector get_vrouterApi();

    VirtualNetwork createNetworkInTungsten(String networkUuid, String networkName, String projectUuid,
        String networkIpamUuid, String ipAllocPoolStart, String ipAllocPoolEnd, String subnetIpPrefix,
        int subnetIpPrefixLength, String defaultGateway, boolean isDhcpEnabled, List<String> dnsNameservers,
        boolean isIpAddrFromStart);

    VirtualMachine createVmInTungsten(String vmUuid, String vmName);

    InstanceIp createInstanceIpInTungsten(String instanceIpName, String tungstenVmInterfaceUuid,
        String tungstenNetworkUuid, String tungstenInstanceIpAddress);

    VirtualMachineInterface createVmInterfaceInTungsten(String vmiUuid, String vmInterfaceName,
        String tungstenProjectUuid, String tungstenNetworkUuid, String tungstenVmUuid, String tungstenSecurityGroupUuid,
        List<String> tungstenVmInterfaceMacAddresses);

    VirtualNetwork deleteNetworkFromTungsten(String tungstenNetworkUuid) throws IOException;

    VirtualNetwork getVirtualNetworkFromTungsten(String virtualNetworkUuid) throws IOException;

    void expugeVmFromTungsten(String vmUuid) throws IOException;

    Project getTungstenNetworkProject(Account owner) throws IOException;

    void addTungstenVrouterPort(Port port) throws IOException;

    void deleteTungstenVrouterPort(String portUuid) throws IOException;

    InstanceIp createInstanceIpInTungsten(VirtualNetwork vn, VirtualMachineInterface vmi, NicProfile nic);

    VirtualMachineInterface createVmInterfaceInTungsten(NicProfile nicProfile, VirtualNetwork vn, VirtualMachine vm,
        Project project);

    ApiObjectBase getObject(Class<? extends ApiObjectBase> obj, String uuid) throws IOException;

    NetworkIpam getDefaultProjectNetworkIpam(Project project) throws IOException;

    VnSubnetsType getVnSubnetsType(String ipAllocPoolStart, String ipAllocPoolEnd, String subnetIpPrefix,
        int subnetIpPrefixLength, String defaultGateway, boolean isDhcpEnabled, List<String> dnsNameservers,
        boolean isIpAddrFromStart);

    VirtualNetwork createTungstenVirtualNetwork(Network network, Project project, NetworkIpam networkIpam,
        VnSubnetsType subnet);

    void deleteObject(Class<? extends ApiObjectBase> obj, String uuid) throws IOException;
}
