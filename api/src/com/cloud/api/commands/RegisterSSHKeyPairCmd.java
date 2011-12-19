/**
 *  Copyright (C) 2010 Cloud.com, Inc.  All rights reserved.
 * 
 * This software is licensed under the GNU General Public License v3 or later.
 * 
 * It is free software: you can redistribute it and/or modify
 * it under the terms of the GNU General Public License as published by
 * the Free Software Foundation, either version 3 of the License, or any later version.
 * This program is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 * GNU General Public License for more details.
 * 
 * You should have received a copy of the GNU General Public License
 * along with this program.  If not, see <http://www.gnu.org/licenses/>.
 * 
 */

package com.cloud.api.commands;

import org.apache.log4j.Logger;

import com.cloud.api.ApiConstants;
import com.cloud.api.BaseCmd;
import com.cloud.api.IdentityMapper;
import com.cloud.api.Implementation;
import com.cloud.api.Parameter;
import com.cloud.api.response.SSHKeyPairResponse;
import com.cloud.user.SSHKeyPair;
import com.cloud.user.UserContext;

@Implementation(description="Register a public key in a keypair under a certain name", responseObject=SSHKeyPairResponse.class) 
public class RegisterSSHKeyPairCmd extends BaseCmd {
    public static final Logger s_logger = Logger.getLogger(RegisterSSHKeyPairCmd.class.getName());
    private static final String s_name = "registersshkeypairresponse";
	
   
    /////////////////////////////////////////////////////
    //////////////// API parameters /////////////////////
    /////////////////////////////////////////////////////
	
	@Parameter(name=ApiConstants.NAME, type=CommandType.STRING, required=true, description="Name of the keypair") 
	private String name;
	
    @Parameter(name="publickey", type=CommandType.STRING, required=true, description="Public key material of the keypair") 
    private String publicKey;

    //Owner information
    @Parameter(name=ApiConstants.ACCOUNT, type=CommandType.STRING, description="an optional account for the ssh key. Must be used with domainId.")
    private String accountName;
    
    @IdentityMapper(entityTableName="domain")
    @Parameter(name=ApiConstants.DOMAIN_ID, type=CommandType.LONG, description="an optional domainId for the ssh key. If the account parameter is used, domainId must also be used.")
    private Long domainId;
    
    @IdentityMapper(entityTableName="projects")
    @Parameter(name=ApiConstants.PROJECT_ID, type=CommandType.LONG, description="an optional project for the ssh key")
    private Long projectId;
    
    /////////////////////////////////////////////////////
    /////////////////// Accessors ///////////////////////
    ///////////////////////////////////////////////////// 
    
	public String getName() {
		return name;
	}

	public String getPublicKey() {
		return publicKey;
	}

	public String getAccountName() {
	    return accountName;
	}
	    
    public Long getDomainId() {
        return domainId;
    }
    
    public Long getProjectId() {
        return projectId;
    }
    
    /////////////////////////////////////////////////////
    /////////////// API Implementation///////////////////
	/////////////////////////////////////////////////////
	
    @Override
    public long getEntityOwnerId() {
        Long accountId = getAccountId(accountName, domainId, projectId, true);
        if (accountId == null) {
            return UserContext.current().getCaller().getId();
        }
        
        return accountId;
    }	
	
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
