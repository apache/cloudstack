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

import com.cloud.api.BaseAsyncCmd;
import com.cloud.api.Implementation;
import com.cloud.api.Parameter;
import com.cloud.api.BaseCmd.Manager;

@Implementation(method="deleteUser", manager=Manager.ManagementServer)
public class DeleteUserCmd extends BaseAsyncCmd {
	public static final Logger s_logger = Logger.getLogger(DeleteUserCmd.class.getName());
	private static final String s_name = "deleteuserresponse";

    /////////////////////////////////////////////////////
    //////////////// API parameters /////////////////////
    /////////////////////////////////////////////////////

    @Parameter(name="id", type=CommandType.LONG, required=true)
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

	public static String getStaticName() {
		return s_name;
	}
	
	public String getName() {
        return s_name;
    }

//    @Override
//    public List<Pair<String, Object>> execute(Map<String, Object> params) {
//        Long userId = (Long)params.get(BaseCmd.Properties.ID.getName());
//        
//        //Verify that the user exists in the system
//        User user = getManagementServer().getUser(userId.longValue());
//        if (user == null) {
//            throw new ServerApiException(BaseCmd.PARAM_ERROR, "unable to find user " + userId);
//        }
//        
//        // If the user is a System user, return an error.  We do not allow this
//        Account account = getManagementServer().findAccountById(user.getAccountId());
//        if ((account != null) && (account.getId() == Account.ACCOUNT_ID_SYSTEM)) {
//        	throw new ServerApiException(BaseCmd.ACCOUNT_ERROR, "user id : " + userId + " is a system account, delete is not allowed");
//        }
//        
//        long jobId = getManagementServer().deleteUserAsync(userId.longValue());
//        List<Pair<String, Object>> returnValues = new ArrayList<Pair<String, Object>>();
//        returnValues.add(new Pair<String, Object>(BaseCmd.Properties.JOB_ID.getName(), Long.valueOf(jobId))); 
//        return returnValues;
//    }


	@Override
	public String getResponse() {
		// TODO Auto-generated method stub
		return null;
	}
}
