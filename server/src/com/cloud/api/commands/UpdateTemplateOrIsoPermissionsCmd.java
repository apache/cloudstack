package com.cloud.api.commands;

import java.util.List;

import org.apache.log4j.Logger;

import com.cloud.api.ApiConstants;
import com.cloud.api.BaseCmd;
import com.cloud.api.Implementation;
import com.cloud.api.Parameter;
import com.cloud.api.ServerApiException;
import com.cloud.exception.ConcurrentOperationException;
import com.cloud.exception.InsufficientAddressCapacityException;
import com.cloud.exception.InsufficientCapacityException;
import com.cloud.exception.InvalidParameterValueException;
import com.cloud.exception.PermissionDeniedException;
import com.cloud.exception.ResourceUnavailableException;

@Implementation()
public abstract class UpdateTemplateOrIsoPermissionsCmd extends BaseCmd {
	public Logger s_logger = getLogger();
    protected String s_name = getResponseName();


    
    /////////////////////////////////////////////////////
    //////////////// API parameters /////////////////////
    /////////////////////////////////////////////////////

    @Parameter(name=ApiConstants.ACCOUNTS, type=CommandType.LIST, collectionType=CommandType.STRING, description="a comma delimited list of accounts")
    private List<String> accountNames;

    @Parameter(name=ApiConstants.ID, type=CommandType.LONG, required=true, description="the template ID")
    private Long id;

    @Parameter(name=ApiConstants.IS_FEATURED, type=CommandType.BOOLEAN, description="true for featured templates/isos, false otherwise")
    private Boolean featured;

    @Parameter(name=ApiConstants.IS_PUBLIC, type=CommandType.BOOLEAN, description="true for public templates/isos, false for private templates/isos")
    private Boolean isPublic;

    @Parameter(name=ApiConstants.OP, type=CommandType.STRING, description="permission operator (add, remove, reset)")
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
    public void execute() throws ServerApiException, InvalidParameterValueException, PermissionDeniedException, InsufficientAddressCapacityException, InsufficientCapacityException, ConcurrentOperationException, ResourceUnavailableException{
        //method is implemented in updateTemplate/updateIsoPermissions
    }
}
