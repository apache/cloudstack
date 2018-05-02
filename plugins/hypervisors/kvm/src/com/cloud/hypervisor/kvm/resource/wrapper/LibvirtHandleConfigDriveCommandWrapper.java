package com.cloud.hypervisor.kvm.resource.wrapper;

import com.cloud.agent.api.Answer;
import com.cloud.agent.api.HandleConfigDriveIsoCommand;
import com.cloud.hypervisor.kvm.resource.LibvirtComputingResource;
import com.cloud.resource.CommandWrapper;
import com.cloud.resource.ResourceWrapper;

@ResourceWrapper(handles = HandleConfigDriveIsoCommand.class)
public class LibvirtHandleConfigDriveCommandWrapper  extends CommandWrapper<HandleConfigDriveIsoCommand, Answer, LibvirtComputingResource>
{
    @Override public Answer execute(HandleConfigDriveIsoCommand command, LibvirtComputingResource serverResource) {
        return serverResource.getConfigDriveFactory().executeRequest(command);
    }
}
