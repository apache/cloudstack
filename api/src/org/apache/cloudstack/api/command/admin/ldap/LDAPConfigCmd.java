// Licensed to the Apache Software Foundation (ASF) under one
// or more contributor license agreements.  See the NOTICE file
// distributed with this work for additional information
// regarding copyright ownership.  The ASF licenses this file
// to you under the Apache License, Version 2.0 (the
// "License"); you may not use this file except in compliance
// with the License.  You may obtain a copy of the License at
//
//   http://www.apache.org/licenses/LICENSE-2.0
//
// Unless required by applicable law or agreed to in writing,
// software distributed under the License is distributed on an
// "AS IS" BASIS, WITHOUT WARRANTIES OR CONDITIONS OF ANY
// KIND, either express or implied.  See the License for the
// specific language governing permissions and limitations
// under the License.
package org.apache.cloudstack.api.command.admin.ldap;


import javax.naming.NamingException;

import org.apache.cloudstack.api.APICommand;
import org.apache.cloudstack.api.ApiConstants;
import org.apache.cloudstack.api.BaseCmd;
import org.apache.cloudstack.api.Parameter;
import org.apache.cloudstack.api.ServerApiException;
import org.apache.cloudstack.api.response.LDAPConfigResponse;
import org.apache.log4j.Logger;

import com.cloud.exception.ConcurrentOperationException;
import com.cloud.exception.InsufficientCapacityException;
import com.cloud.exception.ResourceAllocationException;
import com.cloud.exception.ResourceUnavailableException;
import com.cloud.exception.InvalidParameterValueException;
import com.cloud.user.Account;

@APICommand(name = "ldapConfig", description="Configure the LDAP context for this site.", responseObject=LDAPConfigResponse.class, since="3.0.0")
public class LDAPConfigCmd extends BaseCmd  {
    public static final Logger s_logger = Logger.getLogger(LDAPConfigCmd.class.getName());

    private static final String s_name = "ldapconfigresponse";

    /////////////////////////////////////////////////////
    //////////////// API parameters /////////////////////
    /////////////////////////////////////////////////////

    @Parameter(name=ApiConstants.LIST_ALL, type=CommandType.STRING,  description="Hostname or ip address of the ldap server eg: my.ldap.com")
    private String listall;

    @Parameter(name=ApiConstants.HOST_NAME, type=CommandType.STRING,  description="Hostname or ip address of the ldap server eg: my.ldap.com")
    private String hostname;

    @Parameter(name=ApiConstants.PORT, type=CommandType.INTEGER, description="Specify the LDAP port if required, default is 389.")
    private Integer port=0;

    @Parameter(name=ApiConstants.USE_SSL, type=CommandType.BOOLEAN, description="Check Use SSL if the external LDAP server is configured for LDAP over SSL.")
    private Boolean useSSL;

    @Parameter(name=ApiConstants.SEARCH_BASE, type=CommandType.STRING,  description="The search base defines the starting point for the search in the directory tree Example:  dc=cloud,dc=com.")
    private String searchBase;

    @Parameter(name=ApiConstants.QUERY_FILTER, type=CommandType.STRING,  description="You specify a query filter here, which narrows down the users, who can be part of this domain.")
    private String queryFilter;

    @Parameter(name=ApiConstants.BIND_DN, type=CommandType.STRING, description="Specify the distinguished name of a user with the search permission on the directory.")
    private String bindDN;

    @Parameter(name=ApiConstants.BIND_PASSWORD, type=CommandType.STRING, description="Enter the password.")
    private String bindPassword;

    @Parameter(name=ApiConstants.TRUST_STORE, type=CommandType.STRING, description="Enter the path to trust certificates store.")
    private String trustStore;

    @Parameter(name=ApiConstants.TRUST_STORE_PASSWORD, type=CommandType.STRING, description="Enter the password for trust store.")
    private String trustStorePassword;

    /////////////////////////////////////////////////////
    /////////////////// Accessors ///////////////////////
    /////////////////////////////////////////////////////

    public String getListAll() {
        return listall == null ? "false" : listall;
    }

    public String getBindPassword() {
        return bindPassword;
    }

    public String getBindDN() {
        return bindDN;
    }

    public void setBindDN(String bdn) {
        this.bindDN=bdn;
    }

    public String getQueryFilter() {
        return queryFilter;
    }

    public void setQueryFilter(String queryFilter) {
        this.queryFilter=queryFilter;
    }
    public String getSearchBase() {
        return searchBase;
    }

    public void setSearchBase(String searchBase) {
        this.searchBase=searchBase;
    }

    public Boolean getUseSSL() {
        return useSSL == null ? Boolean.FALSE : useSSL;
    }

    public void setUseSSL(Boolean useSSL) {
        this.useSSL=useSSL;
    }

    public String getHostname() {
        return hostname;
    }

    public void setHostname(String hostname) {
        this.hostname=hostname;
    }

    public Integer getPort() {
        return port <= 0 ? 389 : port;
    }

    public void setPort(Integer port) {
        this.port=port;
    }

    public String getTrustStore() {
        return trustStore;
    }

    public void setTrustStore(String trustStore) {
        this.trustStore=trustStore;
    }

    public String getTrustStorePassword() {
        return trustStorePassword;
    }


    /////////////////////////////////////////////////////
    /////////////// API Implementation///////////////////
    /////////////////////////////////////////////////////


    @Override
    public void execute() throws ResourceUnavailableException,
            InsufficientCapacityException, ServerApiException,
            ConcurrentOperationException, ResourceAllocationException {
          try {
              if ("true".equalsIgnoreCase(getListAll())){
                  // return the existing conf
                  LDAPConfigCmd cmd = _configService.listLDAPConfig(this);
                  LDAPConfigResponse lr = _responseGenerator.createLDAPConfigResponse(cmd.getHostname(), cmd.getPort(), cmd.getUseSSL(),
                          cmd.getQueryFilter(), cmd.getSearchBase(), cmd.getBindDN());
                  lr.setResponseName(getCommandName());
                  this.setResponseObject(lr);
              }
              else if (getHostname()==null || getSearchBase() == null || getQueryFilter() == null) {
                  throw new InvalidParameterValueException("You need to provide hostname, serachbase and queryfilter to configure your LDAP server");
              }
              else {
                  boolean result = _configService.updateLDAP(this);
                  if (result){
                      LDAPConfigResponse lr = _responseGenerator.createLDAPConfigResponse(getHostname(), getPort(), getUseSSL(), getQueryFilter(), getSearchBase(), getBindDN());
                      lr.setResponseName(getCommandName());
                      this.setResponseObject(lr);
                  }
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
