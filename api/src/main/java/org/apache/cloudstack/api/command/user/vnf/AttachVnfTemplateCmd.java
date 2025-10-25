package org.apache.cloudstack.api.command.user.vnf;

import org.apache.cloudstack.api.*;
import org.apache.cloudstack.api.response.*;
import javax.inject.Inject;

@APICommand(name="attachVnfTemplate", responseObject=SuccessResponse.class,
        description="Bind an existing VM as the VNF for the network")
public class AttachVnfTemplateCmd extends BaseAsyncCmd {
    @Parameter(name="networkid", type=CommandType.UUID, entityType=NetworkResponse.class, required=true) private Long networkId;
    @Parameter(name="vmid", type=CommandType.UUID, entityType=UserVmResponse.class, required=true) private Long vmId;

    @Inject private org.apache.cloudstack.vnf.VnfNetworkService vnfSvc;
    @Override public void execute() {
        vnfSvc.attachVnfVm(networkId, vmId, getEntityOwnerId());
        setResponseObject(new SuccessResponse(getCommandName()));
    }
    @Override public String getCommandName(){return "attachvnftemplateresponse";}
    @Override public long getEntityOwnerId(){return CallContext.current().getCallingAccountId();}
}
