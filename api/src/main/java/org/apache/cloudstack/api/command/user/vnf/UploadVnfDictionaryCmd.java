package org.apache.cloudstack.api.command.user.vnf;

import org.apache.cloudstack.api.*;
import org.apache.cloudstack.api.response.*;
import javax.inject.Inject;

@APICommand(name="uploadVnfDictionary", responseObject=SuccessResponse.class,
        description="Upload/replace a dictionary YAML for a VNF network")
public class UploadVnfDictionaryCmd extends BaseAsyncCmd {
    @Parameter(name="networkid", type=CommandType.UUID, entityType=NetworkResponse.class, required=true) private Long networkId;
    @Parameter(name="name", type=CommandType.STRING) private String name;
    @Parameter(name="yaml", type=CommandType.STRING, required=true) private String yaml;

    @Inject private org.apache.cloudstack.vnf.VnfNetworkService vnfSvc;

    @Override public void execute() {
        vnfSvc.uploadDictionary(networkId, name, yaml, getEntityOwnerId());
        setResponseObject(new SuccessResponse(getCommandName()));
    }
    @Override public String getCommandName() { return "uploadvnfdictionaryresponse"; }
    @Override public long getEntityOwnerId() { return CallContext.current().getCallingAccountId(); }
}
