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


import javax.naming.NamingException;

import org.apache.log4j.Logger;

import com.cloud.api.ApiConstants;
import com.cloud.api.BaseCmd;
import com.cloud.api.Implementation;
import com.cloud.api.Parameter;
import com.cloud.api.ServerApiException;
import com.cloud.api.response.LDAPConfigResponse;
import com.cloud.exception.ConcurrentOperationException;
import com.cloud.exception.InsufficientCapacityException;
import com.cloud.exception.ResourceAllocationException;
import com.cloud.exception.ResourceUnavailableException;
import com.cloud.user.Account;

@Implementation(description="Configure the LDAP context for this site.", responseObject=LDAPConfigResponse.class)
public class LDAPConfigCmd extends BaseCmd  {
    public static final Logger s_logger = Logger.getLogger(LDAPConfigCmd.class.getName());

    private static final String s_name = "ldapconfigresponse";

    /////////////////////////////////////////////////////
    //////////////// API parameters /////////////////////
    /////////////////////////////////////////////////////

    @Parameter(name=ApiConstants.HOST_NAME, type=CommandType.STRING, required=true, description="Hostname or ip address of the ldap server eg: my.ldap.com")
    private String hostname;
    
    @Parameter(name=ApiConstants.PORT, type=CommandType.INTEGER, description="Specify the LDAP port if required, default is 389.")
    private Integer port=0;
    
    @Parameter(name=ApiConstants.USE_SSL, type=CommandType.BOOLEAN, description="Check “Use SSL” if the external LDAP server is configured for LDAP over SSL.")
    private Boolean useSSL;

    @Parameter(name=ApiConstants.SEARCH_BASE, type=CommandType.STRING, required=true, description="The search base defines the starting point for the search in the directory tree Example:  dc=cloud,dc=com.")
    private String searchBase;

    @Parameter(name=ApiConstants.QUERY_FILTER, type=CommandType.STRING, required=true, description="You specify a query filter here, which narrows down the users, who can be part of this domain.")
    private String queryFilter;

    @Parameter(name=ApiConstants.BIND_DN, type=CommandType.STRING, required=true, description="Specify the distinguished name of a user with the search permission on the directory.")
    private String bindDN;
    
    @Parameter(name=ApiConstants.BIND_PASSWORD, type=CommandType.STRING, required=true, description="Enter the password.")
    private String bindPassword;
    

    /////////////////////////////////////////////////////
    /////////////////// Accessors ///////////////////////
    /////////////////////////////////////////////////////

    public String getBindPassword() {
        return bindPassword;
    }

    public String getBindDN() {
        return bindDN;
    }

    public String getQueryFilter() {
        return queryFilter;
    }

    public String getSearchBase() {
        return searchBase;
    }

    public Boolean getUseSSL() {
        return useSSL == null ? Boolean.FALSE : Boolean.TRUE;
    }

    public String getHostname() {
        return hostname;
    }

    public Integer getPort() {
        return port <= 0 ? 389 : port;
    }

    /////////////////////////////////////////////////////
    /////////////// API Implementation///////////////////
    /////////////////////////////////////////////////////


    @Override
    public void execute() throws ResourceUnavailableException,
            InsufficientCapacityException, ServerApiException,
            ConcurrentOperationException, ResourceAllocationException {
          try {
              boolean result = _configService.updateLDAP(this);
              if (result){
                  LDAPConfigResponse lr = _responseGenerator.createLDAPConfigResponse(getHostname(), getPort(), getUseSSL(), getQueryFilter(), getSearchBase(), getBindDN());
                  lr.setResponseName(getCommandName());
                  this.setResponseObject(lr);
              }
          }
          catch (NamingException ne){
              ne.printStackTrace();
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
