package com.cloud.hypervisor.kvm.resource.wrapper;

import com.cloud.agent.api.Answer;
import com.cloud.hypervisor.kvm.resource.LibvirtComputingResource;
import com.cloud.resource.CommandWrapper;
import com.cloud.resource.ResourceWrapper;
import org.apache.cloudstack.network.tungsten.agent.api.AddTungstenVirtualGatewayCommand;
import org.apache.cloudstack.network.tungsten.agent.api.TungstenAnswer;

import java.io.IOException;

@ResourceWrapper(handles = AddTungstenVirtualGatewayCommand.class)
public class LibvirtAddTungstenVirtualGatewayCommandWrapper
    extends CommandWrapper<AddTungstenVirtualGatewayCommand, Answer, LibvirtComputingResource> {
    @Override
    public Answer execute(final AddTungstenVirtualGatewayCommand command,
        final LibvirtComputingResource serverResource) {
        boolean result = serverResource.addTungstenVirtualGateway(command.getInf(), command.getSubnet(),
            command.getRoute(), command.getVrf(), command.getNetnsName(), command.getGateway());
        if (result) {
            return new TungstenAnswer(command, true, null);
        } else {
            return new TungstenAnswer(command, new IOException());
        }
    }
}
