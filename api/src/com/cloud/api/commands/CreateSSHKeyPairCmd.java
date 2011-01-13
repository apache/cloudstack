package com.cloud.api.commands;

import org.apache.log4j.Logger;

import com.cloud.api.ApiConstants;
import com.cloud.api.BaseCmd;
import com.cloud.api.Implementation;
import com.cloud.api.Parameter;
import com.cloud.api.response.SSHKeyPairResponse;
import com.cloud.user.SSHKeyPair;

@Implementation(description="Create a new keypair and returns the private key", responseObject=SSHKeyPairResponse.class) 
public class CreateSSHKeyPairCmd extends BaseCmd {
    public static final Logger s_logger = Logger.getLogger(CreateSSHKeyPairCmd.class.getName());
    private static final String s_name = "createkeypairresponse";
    
    
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
		SSHKeyPair r = _mgr.createSSHKeyPair(this);
		SSHKeyPairResponse response = new SSHKeyPairResponse(r.getName(), r.getFingerprint(), r.getPrivateKey());
		response.setResponseName(getCommandName());
		response.setObjectName("keypair");
		this.setResponseObject(response);
	}

	@Override
	public String getCommandName() {
		return s_name;
	}
}
