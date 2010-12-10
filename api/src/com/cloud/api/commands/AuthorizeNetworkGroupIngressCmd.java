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

import java.util.Collection;
import java.util.HashMap;
import java.util.Iterator;
import java.util.List;
import java.util.Map;

import org.apache.log4j.Logger;

import com.cloud.api.ApiConstants;
import com.cloud.api.BaseAsyncCmd;
import com.cloud.api.BaseCmd;
import com.cloud.api.Implementation;
import com.cloud.api.Parameter;
import com.cloud.api.ServerApiException;
import com.cloud.api.response.IngressRuleResponse;
import com.cloud.api.response.NetworkGroupResponse;
import com.cloud.event.EventTypes;
import com.cloud.network.security.IngressRule;
import com.cloud.user.Account;
import com.cloud.user.UserContext;
import com.cloud.utils.StringUtils;

//FIXME - add description
@Implementation(responseObject=IngressRuleResponse.class) @SuppressWarnings("rawtypes")
public class AuthorizeNetworkGroupIngressCmd extends BaseAsyncCmd {
	public static final Logger s_logger = Logger.getLogger(AuthorizeNetworkGroupIngressCmd.class.getName());

    private static final String s_name = "authorizenetworkgroupingress";

    /////////////////////////////////////////////////////
    //////////////// API parameters /////////////////////
    /////////////////////////////////////////////////////

    @Parameter(name=ApiConstants.PROTOCOL, type=CommandType.STRING, description="TCP is default. UDP is the other supported protocol")
    private String protocol;

    //FIXME - add description
    @Parameter(name=ApiConstants.START_PORT, type=CommandType.INTEGER)
    private Integer startPort;

    //FIXME - add description
    @Parameter(name=ApiConstants.END_PORT, type=CommandType.INTEGER)
    private Integer endPort;

    //FIXME - add description
    @Parameter(name=ApiConstants.ICMP_TYPE, type=CommandType.INTEGER)
    private Integer icmpType;

    //FIXME - add description
    @Parameter(name=ApiConstants.ICMP_CODE, type=CommandType.INTEGER)
    private Integer icmpCode;

    //FIXME - add description
    @Parameter(name=ApiConstants.NETWORK_GROUP_NAME, type=CommandType.STRING, required=true)
    private String networkGroupName;

    //FIXME - add description
    @Parameter(name=ApiConstants.CIDR_LIST, type=CommandType.LIST, collectionType=CommandType.STRING)
    private List cidrList;

    //FIXME - add description
    @Parameter(name=ApiConstants.USER_NETWORK_GROUP_LIST, type=CommandType.MAP)
    private Map userNetworkGroupList;

    //FIXME - add description
    @Parameter(name=ApiConstants.ACCOUNT, type=CommandType.STRING)
    private String accountName;

    //FIXME - add description
    @Parameter(name=ApiConstants.DOMAIN_ID, type=CommandType.LONG)
    private Long domainId;


    /////////////////////////////////////////////////////
    /////////////////// Accessors ///////////////////////
    /////////////////////////////////////////////////////

    public String getAccountName() {
        return accountName;
    }

    public List getCidrList() {
        return cidrList;
    }

    public Long getDomainId() {
        return domainId;
    }

    public Integer getEndPort() {
        return endPort;
    }

    public Integer getIcmpCode() {
        return icmpCode;
    }

    public Integer getIcmpType() {
        return icmpType;
    }

    public String getNetworkGroupName() {
        return networkGroupName;
    }

    public String getProtocol() {
        if (protocol == null) {
            return "all";
        }
        return protocol;
    }

    public Integer getStartPort() {
        return startPort;
    }

    public Map getUserNetworkGroupList() {
        return userNetworkGroupList;
    }


    /////////////////////////////////////////////////////
    /////////////// API Implementation///////////////////
    /////////////////////////////////////////////////////

    @Override
    public String getCommandName() {
        return s_name;
    }

    public static String getResultObjectName() {
    	return "networkgroup";
    }

    @Override
    public long getEntityOwnerId() {
        Account account = UserContext.current().getAccount();
        if ((account == null) || isAdmin(account.getType())) {
            if ((domainId != null) && (accountName != null)) {
                Account userAccount = _responseGenerator.findAccountByNameDomain(accountName, domainId);
                if (userAccount != null) {
                    return userAccount.getId();
                }
            }
        }

        if (account != null) {
            return account.getId();
        }

        return Account.ACCOUNT_ID_SYSTEM; // no account info given, parent this command to SYSTEM so ERROR events are tracked
    }

    @Override
    public String getEventType() {
        return EventTypes.EVENT_NETWORK_GROUP_AUTHORIZE_INGRESS;
    }

    @Override
    public String getEventDescription() {
        StringBuilder sb = new StringBuilder();
        if (getUserNetworkGroupList() != null) {
            sb.append("group list(group/account): ");
            Collection userGroupCollection = getUserNetworkGroupList().values();
            Iterator iter = userGroupCollection.iterator();

            HashMap userGroup = (HashMap)iter.next();
            String group = (String)userGroup.get("group");
            String authorizedAccountName = (String)userGroup.get("account");
            sb.append(group + "/" + authorizedAccountName);

            while (iter.hasNext()) {
                userGroup = (HashMap)iter.next();
                group = (String)userGroup.get("group");
                authorizedAccountName = (String)userGroup.get("account");
                sb.append(", " + group + "/" + authorizedAccountName);
            }
        } else if (getCidrList() != null) {
            sb.append("cidr list: ");
            sb.append(StringUtils.join(getCidrList(), ", "));
        } else {
            sb.append("<error:  no ingress parameters>");
        }

        return  "authorizing ingress to group: " + getNetworkGroupName() + " to " + sb.toString();
    }
	
    @Override
    public void execute(){
        List<? extends IngressRule> ingressRules = _networkGroupMgr.authorizeNetworkGroupIngress(this);
        if (ingressRules != null && ! ingressRules.isEmpty()) {
        NetworkGroupResponse response = _responseGenerator.createNetworkGroupResponseFromIngressRule(ingressRules);
            this.setResponseObject(response);
        } else {
            throw new ServerApiException(BaseCmd.INTERNAL_ERROR, "Failed to authorize network group ingress rule(s)");
        }
        
    }
}
