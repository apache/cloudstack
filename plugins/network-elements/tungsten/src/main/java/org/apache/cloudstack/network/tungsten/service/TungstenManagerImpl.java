package org.apache.cloudstack.network.tungsten.service;

import com.cloud.utils.component.ManagerBase;
import com.google.common.collect.Lists;
import net.juniper.contrail.api.ApiObjectBase;
import net.juniper.contrail.api.types.InstanceIp;
import net.juniper.contrail.api.types.VirtualMachine;
import net.juniper.contrail.api.types.VirtualMachineInterface;
import net.juniper.contrail.api.types.VirtualNetwork;
import net.juniper.contrail.api.types.VirtualRouter;
import org.apache.cloudstack.api.response.ListResponse;
import org.apache.cloudstack.api.response.SuccessResponse;
import org.apache.cloudstack.framework.config.ConfigKey;
import org.apache.cloudstack.framework.config.Configurable;
import org.apache.cloudstack.network.tungsten.api.command.AddVRouterPortCmd;
import org.apache.cloudstack.network.tungsten.api.command.CreateTungstenInstanceIpCmd;
import org.apache.cloudstack.network.tungsten.api.command.CreateTungstenNetworkCmd;
import org.apache.cloudstack.network.tungsten.api.command.CreateTungstenProviderCmd;
import org.apache.cloudstack.network.tungsten.api.command.CreateTungstenVirtualMachineCmd;
import org.apache.cloudstack.network.tungsten.api.command.CreateTungstenVmInterfaceCmd;
import org.apache.cloudstack.network.tungsten.api.command.DeleteTungstenNetworkCmd;
import org.apache.cloudstack.network.tungsten.api.command.DeleteTungstenProviderCmd;
import org.apache.cloudstack.network.tungsten.api.command.DeleteVRouterPortCmd;
import org.apache.cloudstack.network.tungsten.api.command.ListTungstenNetworkCmd;
import org.apache.cloudstack.network.tungsten.api.command.ListTungstenProvidersCmd;
import org.apache.cloudstack.network.tungsten.api.command.ListTungstenVirtualMachineCmd;
import org.apache.cloudstack.network.tungsten.api.command.ListTungstenVirtualRouterCmd;
import org.apache.cloudstack.network.tungsten.api.command.ListTungstenVmInterfaceCmd;
import org.apache.cloudstack.network.tungsten.api.response.TungstenNetworkResponse;
import org.apache.cloudstack.network.tungsten.api.response.TungstenVirtualMachineResponse;
import org.apache.cloudstack.network.tungsten.api.response.TungstenVirtualRouterResponse;
import org.apache.cloudstack.network.tungsten.api.response.TungstenVmInterfaceResponse;
import org.apache.cloudstack.network.tungsten.vrouter.Port;
import org.apache.log4j.Logger;
import org.springframework.stereotype.Component;

import javax.inject.Inject;
import java.io.IOException;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

@Component
public class TungstenManagerImpl extends ManagerBase implements TungstenManager, Configurable {

    private static final Logger s_logger = Logger.getLogger(TungstenManager.class);

    @Inject
    TungstenService tungstenService;

    @Override
    public List<Class<?>> getCommands() {
        return Lists.<Class<?>>newArrayList(ListTungstenNetworkCmd.class,
                CreateTungstenNetworkCmd.class, DeleteTungstenNetworkCmd.class,
                CreateTungstenVmInterfaceCmd.class, CreateTungstenVirtualMachineCmd.class,
                CreateTungstenInstanceIpCmd.class, ListTungstenVirtualMachineCmd.class,
                ListTungstenVmInterfaceCmd.class, AddVRouterPortCmd.class, DeleteVRouterPortCmd.class,
                ListTungstenVirtualRouterCmd.class, CreateTungstenProviderCmd.class,
                DeleteTungstenProviderCmd.class, ListTungstenProvidersCmd.class
        );
    }

    @Override
    public ListResponse<TungstenNetworkResponse> getNetworks(ListTungstenNetworkCmd cmd) throws IOException {
        List<VirtualNetwork> networks;
        ListResponse<TungstenNetworkResponse> response = new ListResponse<>();
        List<TungstenNetworkResponse> tungstenNetworkResponses = new ArrayList<>();

        if (cmd.getNetworkUUID() != null)
            networks = Arrays.asList(tungstenService.getVirtualNetworkFromTungsten(cmd.getNetworkUUID()));
        else
            networks = (List<VirtualNetwork>) tungstenService.get_api().list(VirtualNetwork.class, null);

        if (networks != null && !networks.isEmpty()) {
            for (VirtualNetwork virtualNetwork : networks) {
                TungstenNetworkResponse tungstenNetworkResponse = TungstenResponseHelper.createTungstenNetworkResponse(virtualNetwork);
                tungstenNetworkResponses.add(tungstenNetworkResponse);
            }
        }
        response.setResponses(tungstenNetworkResponses);
        return response;
    }

    @Override
    public ListResponse<TungstenVirtualRouterResponse> getVirtualRouters(ListTungstenVirtualRouterCmd cmd) throws IOException {
        List<VirtualRouter> virtualRouters;
        ListResponse<TungstenVirtualRouterResponse> response = new ListResponse<>();
        List<TungstenVirtualRouterResponse> tungstenVirtualRouterResponses = new ArrayList<>();

        if (cmd.getVirtualRouterUuid() != null)
            virtualRouters = Arrays.asList((VirtualRouter) getTungstenObjectByUUID(VirtualRouter.class, cmd.getVirtualRouterUuid()));
        else
            virtualRouters = (List<VirtualRouter>) tungstenService.get_api().list(VirtualRouter.class, null);

        if (virtualRouters != null & !virtualRouters.isEmpty()) {
            for (VirtualRouter virtualRouter : virtualRouters) {
                TungstenVirtualRouterResponse tungstenVirtualRouterResponse = TungstenResponseHelper.createTungstenVirtualRouterResponse(virtualRouter);
                tungstenVirtualRouterResponses.add(tungstenVirtualRouterResponse);
            }
        }
        response.setResponses(tungstenVirtualRouterResponses);
        return response;
    }

    @Override
    public ListResponse<TungstenVmInterfaceResponse> getVmInterfaces(ListTungstenVmInterfaceCmd cmd) throws IOException {
        List<VirtualMachineInterface> vmInterfaces;
        ListResponse<TungstenVmInterfaceResponse> response = new ListResponse<>();
        List<TungstenVmInterfaceResponse> tungstenVmInterfaceResponses = new ArrayList<>();

        if (cmd.getVmInterfaceUUID() != null)
            vmInterfaces = Arrays.asList((VirtualMachineInterface) getTungstenObjectByUUID(VirtualMachineInterface.class, cmd.getVmInterfaceUUID()));
        else
            vmInterfaces = (List<VirtualMachineInterface>) tungstenService.get_api().list(VirtualMachineInterface.class, null);

        if (vmInterfaces != null && !vmInterfaces.isEmpty()) {
            for (VirtualMachineInterface vmInterface : vmInterfaces) {
                TungstenVmInterfaceResponse tungstenVmInterfaceResponse = TungstenResponseHelper.createTungstenVmInterfaceResponse(vmInterface);
                tungstenVmInterfaceResponses.add(tungstenVmInterfaceResponse);
            }
        }
        response.setResponses(tungstenVmInterfaceResponses);
        return response;
    }

    public ListResponse<TungstenVirtualMachineResponse> getVirtualMachines(ListTungstenVirtualMachineCmd cmd) throws IOException {
        List<VirtualMachine> virtualMachines;
        ListResponse<TungstenVirtualMachineResponse> response = new ListResponse<>();
        List<TungstenVirtualMachineResponse> tungstenVirtualMachineResponses = new ArrayList<>();

        if (cmd.getVirtualMachineUUID() != null)
            virtualMachines = Arrays.asList((VirtualMachine) tungstenService.get_api().findById(VirtualMachine.class, cmd.getVirtualMachineUUID()));
        else
            virtualMachines = (List<VirtualMachine>) tungstenService.get_api().list(VirtualMachine.class, null);

        if (virtualMachines != null && !virtualMachines.isEmpty()) {
            for (VirtualMachine virtualMachine : virtualMachines) {
                TungstenVirtualMachineResponse tungstenVirtualMachineResponse = TungstenResponseHelper.createTungstenVirtualMachineResponse(virtualMachine);
                tungstenVirtualMachineResponses.add(tungstenVirtualMachineResponse);
            }
        }
        response.setResponses(tungstenVirtualMachineResponses);
        return response;
    }

    @Override
    public VirtualNetwork createTungstenNetwork(CreateTungstenNetworkCmd cmd) {
        return tungstenService.createNetworkInTungsten(cmd.getTungstenNetworkUuid(), cmd.getName(), null, cmd.getNetworkIpamUUID(), cmd.getIpAllocPoolStart(),
                cmd.getIpAllocPoolEnd(), cmd.getSubnetIpPrefix(), cmd.getSubnetIpPrefixLength(), cmd.getDefaultGateway(), cmd.isEnableDHCP(),
                cmd.getDnsNameservers(), cmd.isAddrFromStart());
    }

    @Override
    public VirtualMachine createTungstenVirtualMachine(CreateTungstenVirtualMachineCmd cmd) {
        return tungstenService.createVmInTungsten(cmd.getTungstenVmUuid(), cmd.getName());
    }

    @Override
    public InstanceIp createInstanceIp(CreateTungstenInstanceIpCmd cmd) {
        return tungstenService.createInstanceIpInTungsten(cmd.getName(), cmd.getTungstenVmInterfaceUuid(),
                cmd.getTungstenNetworkUuid(), cmd.getTungstenInstanceIpAddress());
    }

    @Override
    public VirtualMachineInterface createTungstenVirtualMachineInterface(CreateTungstenVmInterfaceCmd cmd) {
        return tungstenService.createVmInterfaceInTungsten(null, cmd.getName(), cmd.getTungstenProjectUuid(), cmd.getTungstenNetworkUuid(),
                cmd.getTungstenVirtualMachineUuid(), cmd.getTungstenSecurityGroupUuid(),
                cmd.getTungstenVmInterfaceMacAddresses());
    }

    @Override
    public ApiObjectBase getTungstenObjectByUUID(Class<? extends ApiObjectBase> cls, String uuid) throws IOException {
        if (uuid != null)
            return tungstenService.get_api().findById(cls, uuid);
        else
            return null;
    }

    @Override
    public VirtualNetwork deleteTungstenNetwork(DeleteTungstenNetworkCmd cmd) throws IOException {
        return tungstenService.deleteNetworkFromTungsten(cmd.getTungstenNetworkUuid());
    }

    @Override
    public SuccessResponse addVRouterPort(final AddVRouterPortCmd cmd) throws IOException {
        Port port = new Port();
        port.setId(cmd.getTungstenVmInterfaceUuid());
        port.setInstanceId(cmd.getTungstenVirtualMachineUuid());
        port.setIpAddress(cmd.getTungstenInstanceIpAddress());
        port.setMacAddress(cmd.getTungstenVmInterfaceMacAddress());
        port.setTapInterfaceName(getTapName(cmd.getTungstenVmInterfaceMacAddress()));
        port.setVmProjectId(String.valueOf(cmd.getProjectId()));
        port.setDisplayName(cmd.getTungstenVmName());
        port.setVnId(cmd.getTungstenVnUuid());
        SuccessResponse successResponse = new SuccessResponse(cmd.getCommandName());
        if (tungstenService.get_vrouterApi().addPort(port)) {
            successResponse.setSuccess(true);
            successResponse.setDisplayText("Success to add vrouter port");
        } else {
            successResponse.setSuccess(false);
            successResponse.setDisplayText("Fail to add vrouter port");
        }
        return successResponse;
    }

    @Override
    public SuccessResponse deleteVRouterPort(final DeleteVRouterPortCmd cmd) throws IOException {
        String portId = cmd.getTungstenVmInterfaceUuid();
        SuccessResponse successResponse = new SuccessResponse(cmd.getCommandName());
        if (tungstenService.get_vrouterApi().deletePort(portId)) {
            successResponse.setSuccess(true);
            successResponse.setDisplayText("Success to delete vrouter port");
        } else {
            successResponse.setSuccess(false);
            successResponse.setDisplayText("Fail to delete vrouter port");
        }
        return successResponse;
    }

    @Override
    public String getConfigComponentName() {
        return TungstenManager.class.getSimpleName();
    }

    @Override
    public ConfigKey<?>[] getConfigKeys() {
        return new ConfigKey[0];
    }

    private String getTapName(final String macAddress) {
        return "tap" + macAddress.replace(":", "");
    }
}
