package com.cloud.network.guru;

import com.cloud.network.Network;
import com.cloud.user.Account;

import java.io.IOException;
import java.util.List;

public interface NetworkGuruTungsten {
    void createNetworkInTungsten(Network networkVO, Account owner) throws IOException;
    String createVirtualMachineInTungsten(String virtualMachineUuid, String virtualMachineName);
    String createVmInterfaceInTungsten(String vmInterfaceName, String tungstenProjectUuid, String tungstenNetworkUuid, String tungstenVirtualMachineUuid, String tungstenSecurityGroupUuid, List<String> tungstenVmInterfaceMacAddresses);
    void createTungstenInstanceIp(String instanceIpName, String tungstenVmInterfaceUuid, String tungstenNetworkUuid, String tungstenInstanceIpAddress);
}
