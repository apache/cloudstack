package com.cloud.network.guru;

import com.cloud.network.Network;

import java.util.List;

public interface NetworkGuruTungsten {
    Network createNetworkInTungsten(Network networkVO);
    String createVirtualMachineInTungsten(String virtualMachineUuid, String virtualMachineName);
    String createVmInterfaceInTungsten(String vmInterfaceName, String tungstenProjectUuid, String tungstenNetworkUuid, String tungstenVirtualMachineUuid, String tungstenSecurityGroupUuid, List<String> tungstenVmInterfaceMacAddresses);
    void createTungstenInstanceIp(String instanceIpName, String tungstenVmInterfaceUuid, String tungstenNetworkUuid, String tungstenInstanceIpAddress);
}
