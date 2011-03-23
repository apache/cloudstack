package com.cloud.api.commands;

import org.apache.log4j.Logger;

import com.cloud.api.ApiConstants;
import com.cloud.api.BaseCmd;
import com.cloud.api.Implementation;
import com.cloud.api.Parameter;
import com.cloud.api.response.SSHKeyPairResponse;
import com.cloud.user.Account;
import com.cloud.user.SSHKeyPair;
import com.cloud.user.UserContext;

@Implementation(description="Create a new keypair and returns the private key", responseObject=SSHKeyPairResponse.class, includeInApiDoc=false) 
public class CreateSSHKeyPairCmd extends BaseCmd {
    public static final Logger s_logger = Logger.getLogger(CreateSSHKeyPairCmd.class.getName());
    private static final String s_name = "createkeypairresponse";
    
    
    /////////////////////////////////////////////////////
    //////////////// API parameters /////////////////////
    /////////////////////////////////////////////////////
    
	@Parameter(name=ApiConstants.NAME, type=CommandType.STRING, required=true, description="Name of the keypair") 
	private String name;
	
    //Owner information
    @Parameter(name=ApiConstants.ACCOUNT, type=CommandType.STRING, description="an optional account for the ssh key. Must be used with domainId.")
    private String accountName;
    
    @Parameter(name=ApiConstants.DOMAIN_ID, type=CommandType.LONG, description="an optional domainId for the ssh key. If the account parameter is used, domainId must also be used.")
    private Long domainId;
	
    
    /////////////////////////////////////////////////////
    /////////////////// Accessors ///////////////////////
    /////////////////////////////////////////////////////
    
	public String getName() {
		return name;
	}
	
    public String getAccountName() {
        return accountName;
    }
    
    public Long getDomainId() {
        return domainId;
    }

	
    /////////////////////////////////////////////////////
    /////////////// API Implementation///////////////////
	/////////////////////////////////////////////////////
    @Override
    public long getEntityOwnerId() {
        Account account = UserContext.current().getCaller();

        if (account != null) {
            return account.getId();
        }

        return Account.ACCOUNT_ID_SYSTEM; // no account info given, parent this command to SYSTEM so ERROR events are tracked
    }

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
