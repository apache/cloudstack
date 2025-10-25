package org.apache.cloudstack.api.command.user.vnf;

import org.apache.cloudstack.api.*;
import org.apache.cloudstack.api.response.*;
import javax.inject.Inject;

@APICommand(name="getVnfNetworkStatus",
        description="Return broker/VNF/dictionary status for a VNF network",
        responseObject=org.apache.cloudstack.api.response.SuccessResponse.class)
public class GetVnfNetworkStatusCmd extends BaseCmd {
    @Parameter(name="networkid", type=CommandType.UUID, entityType=NetworkResponse.class, required=true) private Long networkId;
    @Inject private org.apache.cloudstack.vnf.VnfNetworkService vnfSvc;
    @Override public void execute() {
        setResponseObject(vnfSvc.getStatus(networkId));
    }
    @Override public String getCommandName(){ return "getvnfnetworkstatusresponse"; }
}
