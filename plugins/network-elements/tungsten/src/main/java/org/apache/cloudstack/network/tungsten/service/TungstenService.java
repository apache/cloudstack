package org.apache.cloudstack.network.tungsten.service;

import com.cloud.user.Account;
import net.juniper.contrail.api.ApiConnector;
import net.juniper.contrail.api.types.InstanceIp;
import net.juniper.contrail.api.types.Project;
import net.juniper.contrail.api.types.VirtualMachine;
import net.juniper.contrail.api.types.VirtualMachineInterface;
import net.juniper.contrail.api.types.VirtualNetwork;
import org.apache.cloudstack.network.tungsten.vrouter.VRouterApiConnector;

import javax.naming.ConfigurationException;
import java.io.IOException;
import java.util.List;

public interface TungstenService {

    public static final String TUNGSTEN_DEFAULT_DOMAIN = "default-domain";
    public static final String TUNGSTEN_DEFAULT_PROJECT = "default-project";

    void init() throws ConfigurationException;

    ApiConnector get_api();

    VRouterApiConnector get_vrouterApi();

    VirtualNetwork createNetworkInTungsten(String networkUuid, String networkName, String projectUuid, String networkIpamUuid, String ipAllocPoolStart,
                                           String ipAllocPoolEnd, String subnetIpPrefix, int subnetIpPrefixLength, String defaultGateway,
                                           boolean isDhcpEnabled, List<String> dnsNameservers, boolean isIpAddrFromStart);

    VirtualMachine createVmInTungsten(String vmUuid, String vmName);

    InstanceIp createInstanceIpInTungsten(String instanceIpName, String tungstenVmInterfaceUuid, String tungstenNetworkUuid,
                                          String tungstenInstanceIpAddress);

    VirtualMachineInterface createVmInterfaceInTungsten(String vmInterfaceName, String tungstenProjectUuid, String tungstenNetworkUuid,
                                                        String tungstenVmUuid, String tungstenSecurityGroupUuid, List<String> tungstenVmInterfaceMacAddresses);

    VirtualNetwork deleteNetworkFromTungsten(String tungstenNetworkUuid) throws IOException;

    VirtualNetwork getVirtualNetworkFromTungsten(String virtualNetworkUuid) throws IOException;

    void expugeVmFromTungsten(String vmUuid) throws IOException;

    Project getTungstenNetworkProject(Account owner) throws IOException;

}
