package org.apache.cloudstack.api.command.user.vnf;

import org.apache.cloudstack.api.*;
import org.apache.cloudstack.api.response.*;
import javax.inject.Inject;

@APICommand(name = "createVnfNetwork",
        description = "Create a VNF network (deploy VR broker + VNF VM)",
        responseObject = CreateNetworkResponse.class)
public class CreateVnfNetworkCmd extends BaseAsyncCreateCmd {
    @Parameter(name="name", type=CommandType.STRING, required=true) private String name;
    @Parameter(name="displaytext", type=CommandType.STRING) private String displayText;
    @Parameter(name="zoneid", type=CommandType.UUID, entityType=ZoneResponse.class, required=true) private Long zoneId;
    @Parameter(name="vnftemplateid", type=CommandType.UUID, entityType=TemplateResponse.class, required=true) private Long vnfTemplateId;
    @Parameter(name="servicehelpers", type=CommandType.STRING) private String serviceHelpers;
    @Parameter(name="dictionaryyaml", type=CommandType.STRING) private String dictionaryYaml;

    @Inject private org.apache.cloudstack.vnf.VnfNetworkService vnfSvc;

    @Override public void execute() {
        CreateNetworkResponse resp = vnfSvc.createVnfNetwork(this);
        setResponseObject(resp); resp.setResponseName(getCommandName());
    }
    @Override public String getCommandName() { return "createvnfnetworkresponse"; }
    @Override public long getEntityOwnerId() { return CallContext.current().getCallingAccountId(); }

    // getters...
    public String getName(){return name;} public String getDisplayText(){return displayText;}
    public Long getZoneId(){return zoneId;} public Long getVnfTemplateId(){return vnfTemplateId;}
    public String getServiceHelpers(){return serviceHelpers;} public String getDictionaryYaml(){return dictionaryYaml;}
}
