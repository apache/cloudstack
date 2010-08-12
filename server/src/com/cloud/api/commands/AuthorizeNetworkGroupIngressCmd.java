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
import java.util.Collection;
import java.util.HashMap;
import java.util.Iterator;
import java.util.List;
import java.util.Map;

import org.apache.log4j.Logger;

import com.cloud.api.BaseCmd;
import com.cloud.api.ServerApiException;
import com.cloud.network.security.NetworkGroupVO;
import com.cloud.user.Account;
import com.cloud.utils.Pair;
import com.cloud.utils.net.NetUtils;

public class AuthorizeNetworkGroupIngressCmd extends BaseCmd {
	public static final Logger s_logger = Logger.getLogger(AuthorizeNetworkGroupIngressCmd.class.getName());

    private static final String s_name = "authorizenetworkgroupingress";
    private static final List<Pair<Enum, Boolean>> s_properties = new ArrayList<Pair<Enum, Boolean>>();

    static {
        s_properties.add(new Pair<Enum, Boolean>(BaseCmd.Properties.ACCOUNT_OBJ, Boolean.FALSE));
        s_properties.add(new Pair<Enum, Boolean>(BaseCmd.Properties.ACCOUNT, Boolean.FALSE));
        s_properties.add(new Pair<Enum, Boolean>(BaseCmd.Properties.DOMAIN_ID, Boolean.FALSE));
        s_properties.add(new Pair<Enum, Boolean>(BaseCmd.Properties.USER_ID, Boolean.FALSE));
        s_properties.add(new Pair<Enum, Boolean>(BaseCmd.Properties.PROTOCOL, Boolean.FALSE));
        s_properties.add(new Pair<Enum, Boolean>(BaseCmd.Properties.START_PORT, Boolean.FALSE));
        s_properties.add(new Pair<Enum, Boolean>(BaseCmd.Properties.END_PORT, Boolean.FALSE));
        s_properties.add(new Pair<Enum, Boolean>(BaseCmd.Properties.ICMP_TYPE, Boolean.FALSE));
        s_properties.add(new Pair<Enum, Boolean>(BaseCmd.Properties.ICMP_CODE, Boolean.FALSE));
        s_properties.add(new Pair<Enum, Boolean>(BaseCmd.Properties.NETWORK_GROUP_NAME, Boolean.TRUE));
        s_properties.add(new Pair<Enum, Boolean>(BaseCmd.Properties.CIDR_LIST, Boolean.FALSE));
        s_properties.add(new Pair<Enum, Boolean>(BaseCmd.Properties.USER_NETWORK_GROUP_LIST, Boolean.FALSE));
    }

    public String getName() {
        return s_name;
    }
    
    public static String getResultObjectName() {
    	return "networkgroup";
    }
    
    public List<Pair<Enum, Boolean>> getProperties() {
        return s_properties;
    }

    @Override
    public List<Pair<String, Object>> execute(Map<String, Object> params) {
        Account account = (Account)params.get(BaseCmd.Properties.ACCOUNT_OBJ.getName());
        String accountName = (String)params.get(BaseCmd.Properties.ACCOUNT.getName());
        Long domainId = (Long)params.get(BaseCmd.Properties.DOMAIN_ID.getName());
        Long userId = (Long)params.get(BaseCmd.Properties.USER_ID.getName());
        Integer startPort = (Integer)params.get(BaseCmd.Properties.START_PORT.getName());
        Integer endPort = (Integer)params.get(BaseCmd.Properties.END_PORT.getName());
        Integer icmpType = (Integer)params.get(BaseCmd.Properties.ICMP_TYPE.getName());
        Integer icmpCode = (Integer)params.get(BaseCmd.Properties.ICMP_CODE.getName());
        String protocol = (String)params.get(BaseCmd.Properties.PROTOCOL.getName());
        String networkGroup = (String)params.get(BaseCmd.Properties.NETWORK_GROUP_NAME.getName());
        String cidrList = (String)params.get(BaseCmd.Properties.CIDR_LIST.getName());
        Map groupList = (Map)params.get(BaseCmd.Properties.USER_NETWORK_GROUP_LIST.getName());

        Long accountId = null;
        Integer startPortOrType = null;
        Integer endPortOrCode = null;

        if (protocol == null) {
        	protocol = "all";
        }
        //FIXME: for exceptions below, add new enums to BaseCmd.PARAM_ to reflect the error condition more precisely
        if (!NetUtils.isValidNetworkGroupProto(protocol)) {
        	s_logger.debug("Invalid protocol specified " + protocol);
        	 throw new ServerApiException(BaseCmd.NET_INVALID_PARAM_ERROR, "Invalid protocol " + protocol);
        }
        if ("icmp".equalsIgnoreCase(protocol) ) {
            if ((icmpType == null) || (icmpCode == null)) {
                throw new ServerApiException(BaseCmd.NET_INVALID_PARAM_ERROR, "Invalid ICMP type/code specified, icmpType = " + icmpType + ", icmpCode = " + icmpCode);
            }
        	if (icmpType == -1 && icmpCode != -1) {
        		throw new ServerApiException(BaseCmd.NET_INVALID_PARAM_ERROR, "Invalid icmp type range" );
        	} 
        	if (icmpCode > 255) {
        		throw new ServerApiException(BaseCmd.NET_INVALID_PARAM_ERROR, "Invalid icmp code " );
        	}
        	startPortOrType = icmpType;
        	endPortOrCode= icmpCode;
        } else if (protocol.equals("all")) {
        	if ((startPort != null) || (endPort != null)) {
                throw new ServerApiException(BaseCmd.NET_INVALID_PARAM_ERROR, "Cannot specify startPort or endPort without specifying protocol");
            }
        	startPortOrType = 0;
        	endPortOrCode = 0;
        } else {
            if ((startPort == null) || (endPort == null)) {
                throw new ServerApiException(BaseCmd.NET_INVALID_PARAM_ERROR, "Invalid port range specified, startPort = " + startPort + ", endPort = " + endPort);
            }
            if (startPort == 0 && endPort == 0) {
                endPort = 65535;
            }
            if (startPort > endPort) {
                s_logger.debug("Invalid port range specified: " + startPort + ":" + endPort);
                throw new ServerApiException(BaseCmd.NET_INVALID_PARAM_ERROR, "Invalid port range " );
            }
            if (startPort > 65535 || endPort > 65535 || startPort < -1 || endPort < -1) {
                s_logger.debug("Invalid port numbers specified: " + startPort + ":" + endPort);
                throw new ServerApiException(BaseCmd.NET_INVALID_PARAM_ERROR, "Invalid port numbers " );
            }
            
        	if (startPort < 0 || endPort < 0) {
        		throw new ServerApiException(BaseCmd.NET_INVALID_PARAM_ERROR, "Invalid port range " );
        	}
            startPortOrType = startPort;
            endPortOrCode= endPort;
        }
        
        protocol = protocol.toLowerCase();

        if ((account == null) || isAdmin(account.getType())) {
            if ((accountName != null) && (domainId != null)) {
                // if it's an admin account, do a quick permission check
                if ((account != null) && !getManagementServer().isChildDomain(account.getDomainId(), domainId)) {
                    if (s_logger.isDebugEnabled()) {
                        s_logger.debug("Unable to find rules for network security group id = " + networkGroup + ", permission denied.");
                    }
                    throw new ServerApiException(BaseCmd.ACCOUNT_ERROR, "Unable to find rules for network security group id = " + networkGroup + ", permission denied.");
                }

                Account groupOwner = getManagementServer().findActiveAccount(accountName, domainId);
                if (groupOwner == null) {
                    throw new ServerApiException(BaseCmd.PARAM_ERROR, "Unable to find account " + accountName + " in domain " + domainId);
                }
                accountId = groupOwner.getId();
            } else {
                if (account != null) {
                    accountId = account.getId();
                    domainId = account.getDomainId();
                }
            }
        } else {
            if (account != null) {
                accountId = account.getId();
                domainId = account.getDomainId();
            }
        }

        if (accountId == null) {
            throw new ServerApiException(BaseCmd.PARAM_ERROR, "Unable to find account for network security group " + networkGroup + "; failed to authorize ingress.");
        }

        NetworkGroupVO sg = getManagementServer().findNetworkGroupByName(accountId, networkGroup);
        if (sg == null) {
            if (s_logger.isDebugEnabled()) {
                s_logger.debug("Unable to find network security group with id " + networkGroup);
            }
            throw new ServerApiException(BaseCmd.PARAM_ERROR, "Unable to find network security group with id " + networkGroup);
        }

        if (cidrList == null && groupList == null) {
            if (s_logger.isDebugEnabled()) {
                s_logger.debug("At least one cidr or at least one security group needs to be specified");
            }
        	throw new ServerApiException(BaseCmd.PARAM_ERROR, "At least one cidr or at least one security group needs to be specified");
        }

        List<String> authorizedCidrs = new ArrayList<String>();
        if (cidrList != null) {
        	if (protocol.equals("all")) {
                throw new ServerApiException(BaseCmd.NET_INVALID_PARAM_ERROR, "Cannot authorize cidrs without specifying protocol and ports.");	
        	}
        	String [] cidrs = cidrList.split(",");
        	for (String cidr: cidrs) {
        		if (!NetUtils.isValidCIDR(cidr)) {
                    s_logger.debug( "Invalid cidr (" + cidr + ") given, unable to authorize ingress.");	
                    throw new ServerApiException(BaseCmd.NET_INVALID_PARAM_ERROR, "Invalid cidr (" + cidr + ") given, unable to authorize ingress.");	
        		}
        		authorizedCidrs.add(cidr);
        	}
        }
        
        List<NetworkGroupVO> authorizedGroups = new ArrayList<NetworkGroupVO> ();
        if (groupList != null) {
            Collection userGroupCollection = groupList.values();
            Iterator iter = userGroupCollection.iterator();
            while (iter.hasNext()) {
                HashMap userGroup = (HashMap)iter.next();
        		String group = (String)userGroup.get("group");
        		String authorizedAccountName = (String)userGroup.get("account");
        		if ((group == null) || (authorizedAccountName == null)) {
        			 throw new ServerApiException(BaseCmd.PARAM_ERROR, "Invalid user group specified, fields 'group' and 'account' cannot be null, please specify groups in the form:  userGroupList[0].group=XXX&userGroupList[0].account=YYY");
        		}

        		Account authorizedAccount = getManagementServer().findActiveAccount(authorizedAccountName, domainId);
        		if (authorizedAccount == null) {
        		    if (s_logger.isDebugEnabled()) {
        		        s_logger.debug("Nonexistent account: " + authorizedAccountName + ", domainid: " + domainId + " when trying to authorize ingress for " + networkGroup + ":" + protocol + ":" + startPortOrType + ":" + endPortOrCode);
        		    }
        		    throw new ServerApiException(BaseCmd.PARAM_ERROR, "Nonexistent account: " + authorizedAccountName + " when trying to authorize ingress for " + networkGroup + ":" + protocol + ":" + startPortOrType + ":" + endPortOrCode);
        		}

        		NetworkGroupVO groupVO = getManagementServer().findNetworkGroupByName(authorizedAccount.getId(), group);
        		if (groupVO == null) {
        		    if (s_logger.isDebugEnabled()) {
        		        s_logger.debug("Nonexistent group " + group + " for account " + authorizedAccountName + "/" + domainId);
        		    }
        		    throw new ServerApiException(BaseCmd.NET_INVALID_PARAM_ERROR, "Invalid group (" + group + ") given, unable to authorize ingress.");
        		}
        		authorizedGroups.add(groupVO);
        	}
        }

        // If command is executed via 8096 port, set userId to the id of System account (1)
        if (userId == null) {
            userId = Long.valueOf(1);
        }

        long jobId = getManagementServer().authorizeNetworkGroupIngressAsync(accountId, networkGroup, protocol, startPortOrType, endPortOrCode, authorizedCidrs.toArray(new String[authorizedCidrs.size()]), authorizedGroups);
        //long ruleId = 0;
        
        if (jobId == 0) {
            s_logger.warn("Unable to schedule async-job for AuthorizeNetworkGroupIngressCmd command");
        } else {
            if (s_logger.isDebugEnabled())
                s_logger.debug("AuthorizeNetworkGroupIngressCmd command has been accepted, job id: " + jobId);

            // many rules can be created as a result, so returning ruleId may not be correct here
            //ruleId = waitInstanceCreation(jobId);
        }

        List<Pair<String, Object>> returnValues = new ArrayList<Pair<String, Object>>();
        returnValues.add(new Pair<String, Object>(BaseCmd.Properties.JOB_ID.getName(), Long.valueOf(jobId)));
        //returnValues.add(new Pair<String, Object>(BaseCmd.Properties.RULE_ID.getName(), Long.valueOf(ruleId))); 
        return returnValues;
    }

    /*
	protected long getInstanceIdFromJobSuccessResult(String result) {
		CreateOrUpdateRuleResultObject resultObject = (CreateOrUpdateRuleResultObject)SerializerHelper.fromSerializedString(result);
		if(resultObject != null) {
			return resultObject.getRuleId();
		}

		return 0;
	}
	*/
}
