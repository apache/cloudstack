package com.cloud.hypervisor.kvm.resource.wrapper;

import com.cloud.agent.api.Answer;
import com.cloud.hypervisor.kvm.resource.LibvirtComputingResource;
import com.cloud.resource.CommandWrapper;
import com.cloud.resource.ResourceWrapper;
import org.apache.cloudstack.network.tungsten.agent.api.UpdateTungstenLoadbalancerSslCommand;

import java.io.IOException;

@ResourceWrapper(handles = UpdateTungstenLoadbalancerSslCommand.class)
public class LibvirtUpdateTungstenLoadbalancerSslCommandWrapper
    extends CommandWrapper<UpdateTungstenLoadbalancerSslCommand, Answer, LibvirtComputingResource> {
    @Override
    public Answer execute(final UpdateTungstenLoadbalancerSslCommand command,
        final LibvirtComputingResource serverResource) {
        boolean result = serverResource.updateTungstenLoadbalancerSsl(command.getLbUuid(), command.getSslCertName(),
            command.getCertificateKey(), command.getPrivateKey(), command.getPrivateIp(), command.getPort());

        if (result) {
            return new Answer(command, true, null);
        } else {
            return new Answer(command, new IOException());
        }
    }
}
