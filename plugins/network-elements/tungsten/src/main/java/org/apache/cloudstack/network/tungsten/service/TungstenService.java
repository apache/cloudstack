package org.apache.cloudstack.network.tungsten.service;

import net.juniper.contrail.api.ApiConnector;
import net.juniper.contrail.api.types.InstanceIp;
import net.juniper.contrail.api.types.VirtualMachine;
import net.juniper.contrail.api.types.VirtualMachineInterface;
import net.juniper.contrail.api.types.VirtualNetwork;
import org.apache.cloudstack.network.tungsten.vrouter.VRouterApiConnector;

import java.io.IOException;
import java.util.List;

public interface TungstenService {

    ApiConnector get_api();

    VRouterApiConnector get_vrouterApi();

    VirtualNetwork createNetworkInTungsten(String networkUuid, String networkName, String networkIpamUuid, String ipAllocPoolStart,
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
}
