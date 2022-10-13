package org.apache.cloudstack.network.tungsten.service;

import net.juniper.contrail.api.ApiPropertyBase;
import net.juniper.contrail.api.ObjectReference;
import net.juniper.contrail.api.types.InstanceIp;
import net.juniper.contrail.api.types.VirtualMachine;
import net.juniper.contrail.api.types.VirtualMachineInterface;
import net.juniper.contrail.api.types.VirtualNetwork;
import org.apache.cloudstack.network.tungsten.api.response.TungstenInstanceIpResponse;
import org.apache.cloudstack.network.tungsten.api.response.TungstenNetworkResponse;
import org.apache.cloudstack.network.tungsten.api.response.TungstenVirtualMachineResponse;
import org.apache.cloudstack.network.tungsten.api.response.TungstenVmInterfaceResponse;

public class TungstenResponseHelper {

    public static TungstenNetworkResponse createTungstenNetworkResponse(VirtualNetwork virtualNetwork){
        TungstenNetworkResponse tungstenNetworkResponse = new TungstenNetworkResponse();
        tungstenNetworkResponse.setName(virtualNetwork.getName());
        tungstenNetworkResponse.setParentUuid(virtualNetwork.getParentUuid());
        tungstenNetworkResponse.setUuid(virtualNetwork.getUuid());
        tungstenNetworkResponse.setFqName(virtualNetwork.getQualifiedName());
        tungstenNetworkResponse.setObjectName("tungstenVirtualNetwork");
        return tungstenNetworkResponse;
    }

    public static TungstenVirtualMachineResponse createTungstenVirtualMachineResponse(VirtualMachine virtualMachine){
        TungstenVirtualMachineResponse tungstenVirtualMachineResponse = new TungstenVirtualMachineResponse();
        tungstenVirtualMachineResponse.setName(virtualMachine.getName());
        tungstenVirtualMachineResponse.setUuid(virtualMachine.getUuid());

        if(virtualMachine.getVirtualMachineInterfaceBackRefs() != null){
            for(ObjectReference<ApiPropertyBase> item : virtualMachine.getVirtualMachineInterfaceBackRefs()){
                tungstenVirtualMachineResponse.getVmInterfacesUuid().add(item.getUuid());
            }
        }

        tungstenVirtualMachineResponse.setObjectName("tungstenVirtualMachine");
        return tungstenVirtualMachineResponse;
    }

    public static TungstenVmInterfaceResponse createTungstenVmInterfaceResponse(VirtualMachineInterface virtualMachineInterface){
        TungstenVmInterfaceResponse tungstenVmInterfaceResponse = new TungstenVmInterfaceResponse();
        tungstenVmInterfaceResponse.setName(virtualMachineInterface.getName());
        tungstenVmInterfaceResponse.setUuid(virtualMachineInterface.getUuid());
        tungstenVmInterfaceResponse.setParentUuid(virtualMachineInterface.getParentUuid());

        if(virtualMachineInterface.getVirtualNetwork() != null) {
            for(ObjectReference<ApiPropertyBase> item : virtualMachineInterface.getVirtualNetwork()){
                tungstenVmInterfaceResponse.getVirtualNetworksUuid().add(item.getUuid());
            }
        }

        if(virtualMachineInterface.getVirtualMachine() != null){
            for(ObjectReference<ApiPropertyBase> item : virtualMachineInterface.getVirtualMachine()){
                tungstenVmInterfaceResponse.getVirtualMachinesUuid().add(item.getUuid());
            }
        }
        tungstenVmInterfaceResponse.setObjectName("tungstenVirtualMachineInterface");
        return tungstenVmInterfaceResponse;
    }

    public static TungstenInstanceIpResponse createTungstenInstanceIpResponse(InstanceIp instanceIp){
        TungstenInstanceIpResponse tungstenInstanceIpResponse = new TungstenInstanceIpResponse();
        tungstenInstanceIpResponse.setName(instanceIp.getName());
        tungstenInstanceIpResponse.setObjectName("tungstenInstanceIp");
        return tungstenInstanceIpResponse;
    }
}
