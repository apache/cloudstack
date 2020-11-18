package com.cloud.hypervisor.kvm.resource.wrapper;

import com.cloud.agent.api.Answer;
import org.apache.cloudstack.network.tungsten.agent.api.SetupTungstenVRouterCommand;
import com.cloud.hypervisor.kvm.resource.LibvirtComputingResource;
import com.cloud.resource.CommandWrapper;
import com.cloud.resource.ResourceWrapper;

import java.io.IOException;

@ResourceWrapper(handles = SetupTungstenVRouterCommand.class)
    public class LibvirtSetupTungstenVRouterCommandWrapper extends
    CommandWrapper<SetupTungstenVRouterCommand, Answer, LibvirtComputingResource> {
    @Override
    public Answer execute(final SetupTungstenVRouterCommand command, final LibvirtComputingResource serverResource) {
            boolean result = serverResource.setupTungstenVRouter(command.getInf(), command.getSubnet(),
                command.getRoute(), command.getVrf(), command.getGateway());
            if (result) {
                return new Answer(command, true, null);
            } else {
                return new Answer(command, new IOException());
            }
    }
}
