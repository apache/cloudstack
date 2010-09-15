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

import com.cloud.api.ApiDBUtils;
import com.cloud.api.BaseListCmd;
import com.cloud.api.Implementation;
import com.cloud.api.Parameter;
import com.cloud.api.response.SecurityGroupResponse;
import com.cloud.network.SecurityGroupVO;
import com.cloud.serializer.SerializerHelper;
import com.cloud.user.Account;

@Implementation(method="searchForSecurityGroupsByVM")
public class ListPortForwardingServicesByVmCmd extends BaseListCmd {
    public static final Logger s_logger = Logger.getLogger(ListPortForwardingServicesByVmCmd.class.getName());

    private static final String s_name = "listportforwardingservicesbyvmresponse";

    /////////////////////////////////////////////////////
    //////////////// API parameters /////////////////////
    /////////////////////////////////////////////////////

    @Parameter(name="account", type=CommandType.STRING)
    private String accountName;

    @Parameter(name="domainid", type=CommandType.LONG)
    private Long domainId;

    @Parameter(name="ipaddress", type=CommandType.STRING)
    private String ipAddress;

    @Parameter(name="virtualmachineid", type=CommandType.LONG)
    private Long virtualMachineId;


    /////////////////////////////////////////////////////
    /////////////////// Accessors ///////////////////////
    /////////////////////////////////////////////////////

    public String getAccountName() {
        return accountName;
    }

    public Long getDomainId() {
        return domainId;
    }

    public String getIpAddress() {
        return ipAddress;
    }

    public Long getVirtualMachineId() {
        return virtualMachineId;
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
        Map<String, List<SecurityGroupVO>> portForwardingServices = (Map<String, List<SecurityGroupVO>>)getResponseObject();

        List<SecurityGroupResponse> response = new ArrayList<SecurityGroupResponse>();
        for (String addr : portForwardingServices.keySet()) {
            List<SecurityGroupVO> appliedGroup = portForwardingServices.get(addr);
            for (SecurityGroupVO group : appliedGroup) {
                SecurityGroupResponse pfsData = new SecurityGroupResponse();
                pfsData.setId(group.getId());
                pfsData.setName(group.getName());
                pfsData.setDescription(group.getDescription());
                pfsData.setIpAddress(addr);

                Account accountTemp = ApiDBUtils.findAccountById(group.getAccountId());
                if (accountTemp != null) {
                    pfsData.setAccountName(accountTemp.getAccountName());
                    pfsData.setDomainId(accountTemp.getDomainId());
                    pfsData.setDomainName(ApiDBUtils.findDomainById(accountTemp.getDomainId()).getName());
                }

                response.add(pfsData);
            }
        }

        return SerializerHelper.toSerializedString(response);
    }
}
