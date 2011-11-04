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

import java.util.ArrayList;
import java.util.List;

import org.apache.log4j.Logger;

import com.cloud.api.ApiConstants;
import com.cloud.api.BaseListCmd;
import com.cloud.api.Implementation;
import com.cloud.api.Parameter;
import com.cloud.api.response.ListResponse;
import com.cloud.api.response.SSHKeyPairResponse;
import com.cloud.user.SSHKeyPair;

@Implementation(description="List registered keypairs", responseObject=SSHKeyPairResponse.class) 
public class ListSSHKeyPairsCmd extends BaseListCmd {
    public static final Logger s_logger = Logger.getLogger(ListSSHKeyPairsCmd.class.getName());
    private static final String s_name = "listsshkeypairsresponse";
    
    
    /////////////////////////////////////////////////////
    //////////////// API parameters /////////////////////
    /////////////////////////////////////////////////////
	
	@Parameter(name=ApiConstants.NAME, type=CommandType.STRING, description="A key pair name to look for") 
	private String name;
	
    @Parameter(name="fingerprint", type=CommandType.STRING, description="A public key fingerprint to look for") 
    private String fingerprint;

    
    /////////////////////////////////////////////////////
    /////////////////// Accessors ///////////////////////
    ///////////////////////////////////////////////////// 
    
	public String getName() {
		return name;
	}
	
	public String getFingerprint() {
		return fingerprint;
	}
    
    
    /////////////////////////////////////////////////////
    /////////////// API Implementation///////////////////
    /////////////////////////////////////////////////////
    
	@Override
	public void execute() {
		List<? extends SSHKeyPair> resultList = _mgr.listSSHKeyPairs(this);
		List<SSHKeyPairResponse> responses = new ArrayList<SSHKeyPairResponse>();
		for (SSHKeyPair result : resultList) {
			SSHKeyPairResponse r = new SSHKeyPairResponse(result.getName(), result.getFingerprint());
			r.setObjectName("keypair");
			responses.add(r);
		}
		
        ListResponse<SSHKeyPairResponse> response = new ListResponse<SSHKeyPairResponse>();
        response.setResponses(responses);
        response.setResponseName(getCommandName());
        this.setResponseObject(response);
	}

	@Override
	public String getCommandName() {
		return s_name;
	}

}
