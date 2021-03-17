package com.cloud.hypervisor.kvm.resource.wrapper;

import com.cloud.agent.api.Answer;
import com.cloud.hypervisor.kvm.resource.LibvirtComputingResource;
import com.cloud.resource.CommandWrapper;
import com.cloud.resource.ResourceWrapper;
import org.apache.cloudstack.network.tungsten.agent.api.UpdateTungstenLoadbalancerStatsCommand;

import java.io.IOException;

@ResourceWrapper(handles = UpdateTungstenLoadbalancerStatsCommand.class)
public class LibvirtUpdateTungstenLoadbalancerStatsCommandWrapper
    extends CommandWrapper<UpdateTungstenLoadbalancerStatsCommand, Answer, LibvirtComputingResource> {
    @Override
    public Answer execute(final UpdateTungstenLoadbalancerStatsCommand command,
        final LibvirtComputingResource serverResource) {
        boolean result = serverResource.updateTungstenLoadbalancerStats(command.getLbUuid(), command.getLbStatsPort(),
            command.getLbStatsUri(), command.getLbStatsAuth());

        if (result) {
            return new Answer(command, true, null);
        } else {
            return new Answer(command, new IOException());
        }
    }
}
