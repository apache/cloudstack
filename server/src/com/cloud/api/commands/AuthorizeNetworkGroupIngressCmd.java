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
import java.util.Map;

import org.apache.log4j.Logger;

import com.cloud.api.BaseAsyncCmd;
import com.cloud.api.BaseCmd.Manager;
import com.cloud.api.Implementation;
import com.cloud.api.Parameter;
import com.cloud.api.ResponseObject;
import com.cloud.api.response.IngressRuleResponse;
import com.cloud.api.response.ListResponse;
import com.cloud.network.security.IngressRuleVO;

@Implementation(method="authorizeNetworkGroupIngress", manager=Manager.NetworkGroupManager) @SuppressWarnings("rawtypes")
public class AuthorizeNetworkGroupIngressCmd extends BaseAsyncCmd {
	public static final Logger s_logger = Logger.getLogger(AuthorizeNetworkGroupIngressCmd.class.getName());

    private static final String s_name = "authorizenetworkgroupingress";

    /////////////////////////////////////////////////////
    //////////////// API parameters /////////////////////
    /////////////////////////////////////////////////////

    @Parameter(name="protocol", type=CommandType.STRING)
    private String protocol;

    @Parameter(name="startport", type=CommandType.INTEGER)
    private Integer startPort;

    @Parameter(name="endport", type=CommandType.INTEGER)
    private Integer endPort;

    @Parameter(name="icmptype", type=CommandType.INTEGER)
    private Integer icmpType;

    @Parameter(name="icmpcode", type=CommandType.INTEGER)
    private Integer icmpCode;

    @Parameter(name="networkgroupname", type=CommandType.STRING, required=true)
    private String networkGroupName;

    @Parameter(name="cidrlist", type=CommandType.LIST, collectionType=CommandType.STRING)
    private List<String> cidrList;

    @Parameter(name="usernetworkgrouplist", type=CommandType.MAP)
    private Map userNetworkGroupList;

    @Parameter(name="account", type=CommandType.STRING)
    private String accountName;

    @Parameter(name="domainid", type=CommandType.LONG)
    private Long domainId;


    /////////////////////////////////////////////////////
    /////////////////// Accessors ///////////////////////
    /////////////////////////////////////////////////////

    public String getAccountName() {
        return accountName;
    }

    public List<String> getCidrList() {
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
    public String getName() {
        return s_name;
    }

    public static String getResultObjectName() {
    	return "networkgroup";
    }

	@Override @SuppressWarnings("unchecked")
	public ResponseObject getResponse() {
	    List<IngressRuleVO> ingressRules = (List<IngressRuleVO>)getResponseObject();

	    ListResponse response = new ListResponse();
        if ((ingressRules != null) && !ingressRules.isEmpty()) {
            List<IngressRuleResponse> responses = new ArrayList<IngressRuleResponse>();
            for (IngressRuleVO ingressRule : ingressRules) {
                IngressRuleResponse ingressData = new IngressRuleResponse();

                ingressData.setRuleId(ingressRule.getId());
                ingressData.setProtocol(ingressRule.getProtocol());
                if ("icmp".equalsIgnoreCase(ingressRule.getProtocol())) {
                    ingressData.setIcmpType(ingressRule.getStartPort());
                    ingressData.setIcmpCode(ingressRule.getEndPort());
                } else {
                    ingressData.setStartPort(ingressRule.getStartPort());
                    ingressData.setEndPort(ingressRule.getEndPort());
                }

                if (ingressRule.getAllowedNetworkGroup() != null) {
                    ingressData.setNetworkGroupName(ingressRule.getAllowedNetworkGroup());
                    ingressData.setAccountName(ingressRule.getAllowedNetGrpAcct());
                } else {
                    ingressData.setCidr(ingressRule.getAllowedSourceIpCidr());
                }

                ingressData.setResponseName("ingressrule");
                responses.add(ingressData);
            }
            response.setResponses(responses);
        }

        response.setResponseName(getName());
		return response;
	}
}
