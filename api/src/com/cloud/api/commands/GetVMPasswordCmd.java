package com.cloud.api.commands;

import java.security.InvalidParameterException;

import org.apache.log4j.Logger;

import com.cloud.api.ApiConstants;
import com.cloud.api.BaseCmd;
import com.cloud.api.Implementation;
import com.cloud.api.Parameter;
import com.cloud.api.response.GetVMPasswordResponse;

@Implementation(responseObject=GetVMPasswordResponse.class, description="Returns an encrypted password for the VM")
public class GetVMPasswordCmd extends BaseCmd {
    public static final Logger s_logger = Logger.getLogger(GetVMPasswordCmd.class.getName());
    private static final String s_name = "getvmpasswordresponse";

    
    /////////////////////////////////////////////////////
    //////////////// API parameters /////////////////////
    /////////////////////////////////////////////////////

    @Parameter(name=ApiConstants.ID, type=CommandType.LONG, required=true, description="The ID of the virtual machine")
    private Long id;
    
    
    /////////////////////////////////////////////////////
    /////////////////// Accessors ///////////////////////
    /////////////////////////////////////////////////////

    public Long getId() {
        return id;
    }

    
    /////////////////////////////////////////////////////
    /////////////// API Implementation///////////////////
    /////////////////////////////////////////////////////
    
	@Override
	public void execute() {
		String passwd = _mgr.getVMPassword(this);
		if (passwd == null || passwd.equals("")) 
			throw new InvalidParameterException("No password for VM with id '" + getId() + "' found.");
		
		this.setResponseObject(new GetVMPasswordResponse(getCommandName(), passwd));
	}

	@Override
	public String getCommandName() {
		return s_name;
	}

}
