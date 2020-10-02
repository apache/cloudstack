package org.apache.cloudstack.network.tungsten.service;

import com.cloud.utils.component.ManagerBase;
import com.google.common.collect.Lists;
import org.apache.cloudstack.api.response.SuccessResponse;
import org.apache.cloudstack.framework.config.ConfigKey;
import org.apache.cloudstack.framework.config.Configurable;
import org.apache.cloudstack.network.tungsten.api.command.AddVRouterPortCmd;
import org.apache.cloudstack.network.tungsten.api.command.CreateTungstenProviderCmd;
import org.apache.cloudstack.network.tungsten.api.command.DeleteTungstenProviderCmd;
import org.apache.cloudstack.network.tungsten.api.command.DeleteVRouterPortCmd;
import org.apache.cloudstack.network.tungsten.api.command.ListTungstenProvidersCmd;
import org.apache.cloudstack.network.tungsten.vrouter.Port;
import org.apache.log4j.Logger;
import org.springframework.stereotype.Component;

import javax.inject.Inject;
import java.io.IOException;
import java.util.List;

@Component
public class TungstenManagerImpl extends ManagerBase implements TungstenManager, Configurable {

    private static final Logger s_logger = Logger.getLogger(TungstenManager.class);

    @Inject
    TungstenService tungstenService;

    @Override
    public List<Class<?>> getCommands() {
        return Lists.<Class<?>>newArrayList(
                AddVRouterPortCmd.class, DeleteVRouterPortCmd.class,
                CreateTungstenProviderCmd.class, DeleteTungstenProviderCmd.class,
                ListTungstenProvidersCmd.class
        );
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
