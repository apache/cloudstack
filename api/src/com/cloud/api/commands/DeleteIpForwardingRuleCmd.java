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
import com.cloud.api.BaseAsyncCmd;
import com.cloud.api.BaseCmd;
import com.cloud.api.IdentityMapper;
import com.cloud.api.Implementation;
import com.cloud.api.Parameter;
import com.cloud.api.ServerApiException;
import com.cloud.api.response.SuccessResponse;
import com.cloud.async.AsyncJob;
import com.cloud.event.EventTypes;
import com.cloud.exception.InvalidParameterValueException;
import com.cloud.network.rules.FirewallRule;
import com.cloud.user.UserContext;

@Implementation(description="Deletes an ip forwarding rule", responseObject=SuccessResponse.class)
public class DeleteIpForwardingRuleCmd extends BaseAsyncCmd {
    public static final Logger s_logger = Logger.getLogger(DeleteIpForwardingRuleCmd.class.getName());

    private static final String s_name = "deleteipforwardingruleresponse";

    /////////////////////////////////////////////////////
    //////////////// API parameters /////////////////////
    /////////////////////////////////////////////////////

    @IdentityMapper(entityTableName="firewall_rules")
    @Parameter(name=ApiConstants.ID, type=CommandType.LONG, required=true, description="the id of the forwarding rule")
    private Long id;

    
    // unexposed parameter needed for events logging
    @IdentityMapper(entityTableName="account")
    @Parameter(name=ApiConstants.ACCOUNT_ID, type=CommandType.LONG, expose=false)
    private Long ownerId;
    /////////////////////////////////////////////////////
    /////////////////// Accessors ///////////////////////
    /////////////////////////////////////////////////////

    public Long getId() {
        return id;
    }

    /////////////////////////////////////////////////////
    /////////////// API Implementation///////////////////
    /////////////////////////////////////////////////////

    @Override
    public String getCommandName() {
        return s_name;
    }

    @Override
    public void execute(){
        UserContext.current().setEventDetails("Rule Id: "+id);
    	boolean result = _firewallService.revokeRelatedFirewallRule(id, true);
	    result = result && _rulesService.revokeStaticNatRule(id, true);	

        if (result) {
            SuccessResponse response = new SuccessResponse(getCommandName());
            this.setResponseObject(response);
        } else {
            throw new ServerApiException(BaseCmd.INTERNAL_ERROR, "Failed to delete ip forwarding rule");
        }
    }

    @Override
    public long getEntityOwnerId() {
        if (ownerId == null) {
            FirewallRule rule = _entityMgr.findById(FirewallRule.class, id);
            if (rule == null) {
                throw new InvalidParameterValueException("Unable to find static nat rule by id: " + id);
            } else {
                ownerId = rule.getAccountId();
            }
        }
        return ownerId;
    }

    @Override
    public String getEventType() {
        return EventTypes.EVENT_NET_RULE_DELETE;
    }

    @Override
    public String getEventDescription() {
        return  ("Deleting an ipforwarding 1:1 NAT rule id:"+id);
    }
    
    @Override
    public String getSyncObjType() {
        return BaseAsyncCmd.networkSyncObject;
    }

    @Override
    public Long getSyncObjId() {
        return _rulesService.getFirewallRule(id).getNetworkId();
    }
    
    @Override
    public AsyncJob.Type getInstanceType() {
        return AsyncJob.Type.FirewallRule;
    }

}
