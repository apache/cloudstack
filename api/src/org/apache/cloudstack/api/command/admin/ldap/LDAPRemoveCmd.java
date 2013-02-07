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


import org.apache.cloudstack.api.APICommand;
import org.apache.cloudstack.api.BaseCmd;
import org.apache.cloudstack.api.response.LDAPConfigResponse;
import org.apache.cloudstack.api.response.LDAPRemoveResponse;
import org.apache.log4j.Logger;

import com.cloud.user.Account;

@APICommand(name = "ldapRemove", description="Remove the LDAP context for this site.", responseObject=LDAPConfigResponse.class, since="3.0.1")
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
