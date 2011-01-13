package com.cloud.api.commands;

import org.apache.log4j.Logger;

import com.cloud.api.ApiConstants;
import com.cloud.api.BaseCmd;
import com.cloud.api.Implementation;
import com.cloud.api.Parameter;
import com.cloud.api.response.SuccessResponse;

@Implementation(description="Deletes a keypair by name", responseObject=SuccessResponse.class) 
public class DeleteSSHKeyPairCmd extends BaseCmd {
    public static final Logger s_logger = Logger.getLogger(CreateSSHKeyPairCmd.class.getName());
    private static final String s_name = "deletekeypairresponse";
    
    
    /////////////////////////////////////////////////////
    //////////////// API parameters /////////////////////
    /////////////////////////////////////////////////////
    
	@Parameter(name=ApiConstants.NAME, type=CommandType.STRING, required=true, description="Name of the keypair") 
	private String name;
	
    
    /////////////////////////////////////////////////////
    /////////////////// Accessors ///////////////////////
    ///////////////////////////////////////////////////// 
    
	public String getName() {
		return name;
	}
	
	
    /////////////////////////////////////////////////////
    /////////////// API Implementation///////////////////
	/////////////////////////////////////////////////////

	@Override
	public void execute() {
		boolean result = _mgr.deleteSSHKeyPair(this);
		SuccessResponse response = new SuccessResponse(getCommandName());
		response.setSuccess(result);
		this.setResponseObject(response);
	}

	@Override
	public String getCommandName() {
		return s_name;
	}
}
