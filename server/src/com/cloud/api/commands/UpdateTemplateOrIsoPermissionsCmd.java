package com.cloud.api.commands;

import java.util.List;

import org.apache.log4j.Logger;

import com.cloud.api.BaseCmd;
import com.cloud.api.BaseCmd.Manager;
import com.cloud.api.Implementation;
import com.cloud.api.Parameter;
import com.cloud.api.response.SuccessResponse;

@Implementation(method="updateTemplatePermissions", manager=Manager.ManagementServer)
public abstract class UpdateTemplateOrIsoPermissionsCmd extends BaseCmd {
	public Logger s_logger = getLogger();
    protected String s_name = getResponseName();


    
    /////////////////////////////////////////////////////
    //////////////// API parameters /////////////////////
    /////////////////////////////////////////////////////

    @Parameter(name="accounts", type=CommandType.LIST, collectionType=CommandType.STRING, description="a comma delimited list of accounts")
    private List<String> accountNames;

    @Parameter(name="id", type=CommandType.LONG, required=true, description="the template ID")
    private Long id;

    @Parameter(name="isfeatured", type=CommandType.BOOLEAN, description="true for featured templates/isos, false otherwise")
    private Boolean featured;

    @Parameter(name="ispublic", type=CommandType.BOOLEAN, description="true for public templates/isos, false for private templates/isos")
    private Boolean isPublic;

    @Parameter(name="op", type=CommandType.STRING, description="permission operator (add, remove, reset)")
    private String operation;

    /////////////////////////////////////////////////////
    /////////////////// Accessors ///////////////////////
    /////////////////////////////////////////////////////

    public List<String> getAccountNames() {
        return accountNames;
    }

    public Long getId() {
        return id;
    }

    public Boolean isFeatured() {
        return featured;
    }

    public Boolean isPublic() {
        return isPublic;
    }

    public String getOperation() {
        return operation;
    }

    /////////////////////////////////////////////////////
    /////////////// API Implementation///////////////////
    /////////////////////////////////////////////////////

    @Override
    public String getName() {
        return s_name;
    }    
    
    protected String getResponseName() {
    	return "updatetemplateorisopermissionsresponse";
    }
    
    protected Logger getLogger() {
    	return Logger.getLogger(UpdateTemplateOrIsoPermissionsCmd.class.getName());    
    }
    
    @Override @SuppressWarnings("unchecked")
    public SuccessResponse getResponse() {
        Boolean success = (Boolean)getResponseObject();
        SuccessResponse response = new SuccessResponse();
        response.setSuccess(success);
        response.setResponseName(getResponseName());
    	return response;
    }
}
