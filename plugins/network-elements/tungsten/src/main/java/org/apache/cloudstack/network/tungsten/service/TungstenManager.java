package org.apache.cloudstack.network.tungsten.service;

import com.cloud.utils.component.PluggableService;
import net.juniper.contrail.api.ApiObjectBase;
import net.juniper.contrail.api.types.InstanceIp;
import net.juniper.contrail.api.types.VirtualMachine;
import net.juniper.contrail.api.types.VirtualMachineInterface;
import net.juniper.contrail.api.types.VirtualNetwork;
import org.apache.cloudstack.api.response.ListResponse;
import org.apache.cloudstack.api.response.SuccessResponse;
import org.apache.cloudstack.network.tungsten.api.command.AddVRouterPortCmd;
import org.apache.cloudstack.network.tungsten.api.command.CreateTungstenInstanceIpCmd;
import org.apache.cloudstack.network.tungsten.api.command.CreateTungstenNetworkCmd;
import org.apache.cloudstack.network.tungsten.api.command.CreateTungstenVirtualMachineCmd;
import org.apache.cloudstack.network.tungsten.api.command.CreateTungstenVmInterfaceCmd;
import org.apache.cloudstack.network.tungsten.api.command.DeleteTungstenNetworkCmd;
import org.apache.cloudstack.network.tungsten.api.command.DeleteVRouterPortCmd;
import org.apache.cloudstack.network.tungsten.api.command.ListTungstenNetworkCmd;
import org.apache.cloudstack.network.tungsten.api.command.ListTungstenVirtualMachineCmd;
import org.apache.cloudstack.network.tungsten.api.command.ListTungstenVirtualRouterCmd;
import org.apache.cloudstack.network.tungsten.api.command.ListTungstenVmInterfaceCmd;
import org.apache.cloudstack.network.tungsten.api.response.TungstenNetworkResponse;
import org.apache.cloudstack.network.tungsten.api.response.TungstenVirtualMachineResponse;
import org.apache.cloudstack.network.tungsten.api.response.TungstenVirtualRouterResponse;
import org.apache.cloudstack.network.tungsten.api.response.TungstenVmInterfaceResponse;

import java.io.IOException;

public interface TungstenManager extends PluggableService {
  ListResponse<TungstenNetworkResponse> getNetworks(ListTungstenNetworkCmd cmd) throws IOException;

  ListResponse<TungstenVmInterfaceResponse> getVmInterfaces(ListTungstenVmInterfaceCmd cmd)
          throws IOException;

  ListResponse<TungstenVirtualMachineResponse> getVirtualMachines(ListTungstenVirtualMachineCmd cmd)
          throws IOException;

  VirtualNetwork createTungstenNetwork(CreateTungstenNetworkCmd cmd);

  VirtualMachine createTungstenVirtualMachine(CreateTungstenVirtualMachineCmd cmd);

  VirtualMachineInterface createTungstenVirtualMachineInterface(CreateTungstenVmInterfaceCmd cmd);

  InstanceIp createInstanceIp(CreateTungstenInstanceIpCmd cmd);

  VirtualNetwork deleteTungstenNetwork(DeleteTungstenNetworkCmd cmd) throws IOException;

  ApiObjectBase getTungstenObjectByUUID(Class<? extends ApiObjectBase> cls, String uuid)
          throws IOException;

  SuccessResponse addVRouterPort(AddVRouterPortCmd cmd) throws IOException;

  SuccessResponse deleteVRouterPort(DeleteVRouterPortCmd cmd) throws IOException;

  ListResponse<TungstenVirtualRouterResponse> getVirtualRouters(ListTungstenVirtualRouterCmd cmd) throws IOException;
}
