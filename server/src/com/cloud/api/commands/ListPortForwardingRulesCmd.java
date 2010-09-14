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
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import org.apache.log4j.Logger;

import com.cloud.api.ApiDBUtils;
import com.cloud.api.BaseCmd.Manager;
import com.cloud.api.BaseListCmd;
import com.cloud.api.Implementation;
import com.cloud.api.Parameter;
import com.cloud.api.response.FirewallRuleResponse;
import com.cloud.network.FirewallRuleVO;
import com.cloud.network.IPAddressVO;
import com.cloud.serializer.SerializerHelper;
import com.cloud.server.Criteria;
import com.cloud.vm.UserVmVO;

@Implementation(method="listPortForwardingRules", manager=Manager.NetworkManager)
public class ListPortForwardingRulesCmd extends BaseListCmd {
    public static final Logger s_logger = Logger.getLogger(ListPortForwardingRulesCmd.class.getName());

    private static final String s_name = "listportforwardingrulesresponse";

    /////////////////////////////////////////////////////
    //////////////// API parameters /////////////////////
    /////////////////////////////////////////////////////

    @Parameter(name="ipaddress", type=CommandType.STRING, required=true)
    private String ipAddress;


    /////////////////////////////////////////////////////
    /////////////////// Accessors ///////////////////////
    /////////////////////////////////////////////////////

    public String getIpAddress() {
        return ipAddress;
    }

    /////////////////////////////////////////////////////
    /////////////// API Implementation///////////////////
    /////////////////////////////////////////////////////

    @Override
    public String getName() {
        return s_name;
    }

    @Override @SuppressWarnings("unchecked")
    public String getResponse() {
        List<FirewallRuleVO> firewallRules = (List<FirewallRuleVO>)getResponseObject();
        Map<String, UserVmVO> userVmCache = new HashMap<String, UserVmVO>();
        IPAddressVO ipAddr = ApiDBUtils.findIpAddressById(ipAddress);

        List<FirewallRuleResponse> response = new ArrayList<FirewallRuleResponse>();
        for (FirewallRuleVO fwRule : firewallRules) {
            FirewallRuleResponse ruleData = new FirewallRuleResponse();

            ruleData.setId(fwRule.getId());
            ruleData.setPublicPort(fwRule.getPublicPort());
            ruleData.setPrivatePort(fwRule.getPrivatePort());
            ruleData.setProtocol(fwRule.getProtocol());

            UserVmVO userVM = userVmCache.get(fwRule.getPrivateIpAddress());
            if (userVM == null) {
                Criteria c = new Criteria();
                c.addCriteria(Criteria.ACCOUNTID, new Object[] {ipAddr.getAccountId()});
                c.addCriteria(Criteria.DATACENTERID, ipAddr.getDataCenterId());
                c.addCriteria(Criteria.IPADDRESS, fwRule.getPrivateIpAddress());
                List<UserVmVO> userVMs = ApiDBUtils.searchForUserVMs(c);

                if ((userVMs != null) && (userVMs.size() > 0)) {
                    userVM = userVMs.get(0);
                    userVmCache.put(fwRule.getPrivateIpAddress(), userVM);
                }
            }

            if (userVM != null) {
                ruleData.setVirtualMachineId(userVM.getId());
                ruleData.setVirtualMachineName(userVM.getName());
            }

            response.add(ruleData);
        }

        return SerializerHelper.toSerializedString(response);
    }
}
