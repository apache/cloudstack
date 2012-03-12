/**
 *  Copyright (C) 2011 Citrix.com, Inc.  All rights reserved.
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


import javax.naming.NamingException;

import org.apache.log4j.Logger;

import com.cloud.api.ApiConstants;
import com.cloud.api.BaseCmd;
import com.cloud.api.Implementation;
import com.cloud.api.Parameter;
import com.cloud.api.ServerApiException;
import com.cloud.api.response.BaseResponse;
import com.cloud.api.response.LDAPConfigResponse;
import com.cloud.api.response.LDAPRemoveResponse;
import com.cloud.exception.ConcurrentOperationException;
import com.cloud.exception.InsufficientCapacityException;
import com.cloud.exception.ResourceAllocationException;
import com.cloud.exception.ResourceUnavailableException;
import com.cloud.user.Account;

@Implementation(description="Remove the LDAP context for this site.", responseObject=LDAPConfigResponse.class, since="3.0.0")
public class LDAPRemoveCmd extends BaseCmd  {
    public static final Logger s_logger = Logger.getLogger(LDAPRemoveCmd.class.getName());

    private static final String s_name = "ldapremoveresponse";

    /////////////////////////////////////////////////////
    //////////////// API parameters /////////////////////
    /////////////////////////////////////////////////////

   
    /////////////////////////////////////////////////////
    /////////////////// Accessors ///////////////////////
    /////////////////////////////////////////////////////

   
    /////////////////////////////////////////////////////
    /////////////// API Implementation///////////////////
    /////////////////////////////////////////////////////


    @Override
    public void execute(){
          boolean result = _configService.removeLDAP(this);
          if (result){
        	  LDAPRemoveResponse lr = new LDAPRemoveResponse();
              lr.setObjectName("ldapremove");
              lr.setResponseName(getCommandName());
              this.setResponseObject(lr);             
          }
    }

    @Override
    public String getCommandName() {
        return s_name;
    }
    
    @Override
    public long getEntityOwnerId() {
        return Account.ACCOUNT_ID_SYSTEM;
    }


}
