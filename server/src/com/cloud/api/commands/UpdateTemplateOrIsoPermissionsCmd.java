package com.cloud.api.commands;

import java.util.List;

import org.apache.log4j.Logger;

import com.cloud.api.BaseCmd;
import com.cloud.api.BaseCmd.Manager;
import com.cloud.api.Implementation;
import com.cloud.api.Parameter;

@Implementation(method="updateTemplatePermissions", manager=Manager.ManagementServer)
public abstract class UpdateTemplateOrIsoPermissionsCmd extends BaseCmd {
	public Logger s_logger = getLogger();
    protected String s_name = getResponseName();


    
    /////////////////////////////////////////////////////
    //////////////// API parameters /////////////////////
    /////////////////////////////////////////////////////

    @Parameter(name="accounts", type=CommandType.LIST, collectionType=CommandType.STRING)
    private List<String> accountNames;

    @Parameter(name="id", type=CommandType.LONG, required=true)
    private Long id;

    @Parameter(name="isfeatured", type=CommandType.BOOLEAN)
    private Boolean featured;

    @Parameter(name="ispublic", type=CommandType.BOOLEAN)
    private Boolean isPublic;

    @Parameter(name="op", type=CommandType.STRING)
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
    
    @Override
    public String getResponse() 
    {
    	return null;//return the response here
    }
}
