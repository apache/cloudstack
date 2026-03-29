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
package org.apache.cloudstack.api.command.user.network;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;

import org.apache.cloudstack.api.APICommand;
import org.apache.cloudstack.api.ApiConstants;
import org.apache.cloudstack.api.ApiErrorCode;
import org.apache.cloudstack.api.BaseAsyncCmd;
import org.apache.cloudstack.api.Parameter;
import org.apache.cloudstack.api.ServerApiException;
import org.apache.cloudstack.api.response.ListResponse;
import org.apache.cloudstack.api.response.NetworkACLItemResponse;
import org.apache.cloudstack.api.response.NetworkACLResponse;
import org.apache.cloudstack.context.CallContext;
import org.apache.commons.collections.MapUtils;

import com.cloud.event.EventTypes;
import com.cloud.exception.ResourceUnavailableException;
import com.cloud.network.vpc.NetworkACLItem;
import com.cloud.user.Account;

@APICommand(name = "importNetworkACL", description = "Imports Network ACL rules.",
        responseObject = NetworkACLItemResponse.class,
        requestHasSensitiveInfo = false, responseHasSensitiveInfo = false,
        since = "4.22.1")
public class ImportNetworkACLCmd extends BaseAsyncCmd {

    // ///////////////////////////////////////////////////
    // ////////////// API parameters /////////////////////
    // ///////////////////////////////////////////////////

    @Parameter(
            name = ApiConstants.ACL_ID,
            type = CommandType.UUID,
            entityType = NetworkACLResponse.class,
            required = true,
            description = "The ID of the Network ACL to which the rules will be imported"
    )
    private Long aclId;

    @Parameter(name = ApiConstants.RULES, type = CommandType.MAP, required = true,
            description = "Rules param list, id and protocol are must. Invalid rules will be discarded. Example: " +
                    "rules[0].id=101&rules[0].protocol=tcp&rules[0].traffictype=ingress&rules[0].state=active&rules[0].cidrlist=192.168.1.0/24" +
                    "&rules[0].tags=web&rules[0].aclid=acl-001&rules[0].aclname=web-acl&rules[0].number=1&rules[0].action=allow&rules[0].fordisplay=true" +
                    "&rules[0].description=allow%20web%20traffic&rules[1].id=102&rules[1].protocol=udp&rules[1].traffictype=egress&rules[1].state=enabled" +
                    "&rules[1].cidrlist=10.0.0.0/8&rules[1].tags=db&rules[1].aclid=acl-002&rules[1].aclname=db-acl&rules[1].number=2&rules[1].action=deny" +
                    "&rules[1].fordisplay=false&rules[1].description=deny%20database%20traffic")
    private Map rules;


    // ///////////////////////////////////////////////////
    // ///////////////// Accessors ///////////////////////
    // ///////////////////////////////////////////////////

    // Returns map, corresponds to a rule with the details in the keys:
    // id, protocol, startport, endport, traffictype, state, cidrlist, tags, aclid, aclname, number, action, fordisplay, description
    public Map getRules() {
        return rules;
    }

    public Long getAclId() {
        return aclId;
    }

    // ///////////////////////////////////////////////////
    // ///////////// API Implementation///////////////////
    // ///////////////////////////////////////////////////


    @Override
    public void execute() throws ResourceUnavailableException {
        validateParams();
        List<NetworkACLItem> importedRules = _networkACLService.importNetworkACLRules(this);
        ListResponse<NetworkACLItemResponse> response = new ListResponse<>();
        List<NetworkACLItemResponse> aclResponse = new ArrayList<>();
        for (NetworkACLItem acl : importedRules) {
            NetworkACLItemResponse ruleData = _responseGenerator.createNetworkACLItemResponse(acl);
            aclResponse.add(ruleData);
        }
        response.setResponses(aclResponse, importedRules.size());
        response.setResponseName(getCommandName());
        setResponseObject(response);
    }

    @Override
    public long getEntityOwnerId() {
        Account account = CallContext.current().getCallingAccount();
        if (account != null) {
            return account.getId();
        }
        return Account.ACCOUNT_ID_SYSTEM;
    }

    @Override
    public String getEventType() {
        return EventTypes.EVENT_NETWORK_ACL_IMPORT;
    }

    @Override
    public String getEventDescription() {
        return "Importing ACL rules for ACL ID: " + getAclId();
    }


    private void validateParams() {
        if(MapUtils.isEmpty(rules)) {
            throw new ServerApiException(ApiErrorCode.PARAM_ERROR, "Rules parameter is empty or null");
        }

        if (getAclId() == null || _networkACLService.getNetworkACL(getAclId()) == null) {
            throw new ServerApiException(ApiErrorCode.PARAM_ERROR, "Unable to find Network ACL with provided ACL ID");
        }
    }
}
