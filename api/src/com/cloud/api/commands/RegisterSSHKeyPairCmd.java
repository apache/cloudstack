package com.cloud.api.commands;

import org.apache.log4j.Logger;

import com.cloud.api.ApiConstants;
import com.cloud.api.BaseCmd;
import com.cloud.api.Implementation;
import com.cloud.api.Parameter;
import com.cloud.api.response.SSHKeyPairResponse;
import com.cloud.user.SSHKeyPair;

@Implementation(description="Register a public key in a keypair under a certain name", responseObject=SSHKeyPairResponse.class) 
public class RegisterSSHKeyPairCmd extends BaseCmd {
    public static final Logger s_logger = Logger.getLogger(RegisterSSHKeyPairCmd.class.getName());
    private static final String s_name = "registerkeypairresponse";
	
   
    /////////////////////////////////////////////////////
    //////////////// API parameters /////////////////////
    /////////////////////////////////////////////////////
	
	@Parameter(name=ApiConstants.NAME, type=CommandType.STRING, required=true, description="Name of the keypair") 
	private String name;
	
    @Parameter(name="publickey", type=CommandType.STRING, required=true, description="Public key material of the keypair") 
    private String publicKey;

    
    /////////////////////////////////////////////////////
    /////////////////// Accessors ///////////////////////
    ///////////////////////////////////////////////////// 
    
	public String getName() {
		return name;
	}

	public String getPublicKey() {
		return publicKey;
	}

    
    /////////////////////////////////////////////////////
    /////////////// API Implementation///////////////////
	/////////////////////////////////////////////////////
	
	@Override
	public void execute() {	
		SSHKeyPair result = _mgr.registerSSHKeyPair(this);
        SSHKeyPairResponse response = new SSHKeyPairResponse(result.getName(), result.getFingerprint());
        response.setResponseName(getCommandName());
		response.setObjectName("keypair");
        this.setResponseObject(response);
	}

	@Override
	public String getCommandName() {
		return s_name;
	} 
    
}
