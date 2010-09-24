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

import org.apache.log4j.Logger;

import com.cloud.api.ApiDBUtils;
import com.cloud.api.BaseListCmd;
import com.cloud.api.Implementation;
import com.cloud.api.Parameter;
import com.cloud.api.ResponseObject;
import com.cloud.api.response.ListResponse;
import com.cloud.api.response.SystemVmResponse;
import com.cloud.async.AsyncJobVO;
import com.cloud.vm.ConsoleProxyVO;
import com.cloud.vm.SecondaryStorageVmVO;
import com.cloud.vm.SystemVm;
import com.cloud.vm.VMInstanceVO;

@Implementation(method="searchForSystemVms")
public class ListSystemVMsCmd extends BaseListCmd {
    public static final Logger s_logger = Logger.getLogger(ListSystemVMsCmd.class.getName());

    private static final String s_name = "listsystemvmsresponse";

    /////////////////////////////////////////////////////
    //////////////// API parameters /////////////////////
    /////////////////////////////////////////////////////

    @Parameter(name="hostid", type=CommandType.LONG)
    private Long hostId;

    @Parameter(name="id", type=CommandType.LONG)
    private Long id;

    @Parameter(name="name", type=CommandType.STRING)
    private String systemVmName;

    @Parameter(name="podid", type=CommandType.LONG)
    private Long podId;

    @Parameter(name="state", type=CommandType.STRING)
    private String state;

    @Parameter(name="systemvmtype", type=CommandType.STRING)
    private String systemVmType;

    @Parameter(name="zoneid", type=CommandType.LONG)
    private Long zoneId;

    /////////////////////////////////////////////////////
    /////////////////// Accessors ///////////////////////
    /////////////////////////////////////////////////////

    public Long getHostId() {
        return hostId;
    }

    public Long getId() {
        return id;
    }

    public String getSystemVmName() {
        return systemVmName;
    }

    public Long getPodId() {
        return podId;
    }

    public String getState() {
        return state;
    }

    public String getSystemVmType() {
        return systemVmType;
    }

    public Long getZoneId() {
        return zoneId;
    }

    /////////////////////////////////////////////////////
    /////////////// API Implementation///////////////////
    /////////////////////////////////////////////////////

    @Override
    public String getName() {
        return s_name;
    }
    
    @Override @SuppressWarnings("unchecked")
    public ResponseObject getResponse() {
        List<? extends VMInstanceVO> systemVMs = (List<? extends VMInstanceVO>)getResponseObject();

        ListResponse response = new ListResponse();
        List<SystemVmResponse> vmResponses = new ArrayList<SystemVmResponse>();
        for (VMInstanceVO systemVM : systemVMs) {
            SystemVmResponse vmResponse = new SystemVmResponse();
            if (systemVM instanceof SystemVm) {
                SystemVm vm = (SystemVm)systemVM;

                vmResponse.setId(vm.getId());
                vmResponse.setSystemVmType(vm.getType().toString().toLowerCase());

                String instanceType = "console_proxy";
                if (systemVM instanceof SecondaryStorageVmVO) {
                    instanceType = "sec_storage_vm"; // FIXME:  this should be a constant so that the async jobs get updated with the correct instance type, they are using
                                                     //         different instance types at the moment
                }

                AsyncJobVO asyncJob = ApiDBUtils.findInstancePendingAsyncJob(instanceType, vm.getId());
                if (asyncJob != null) {
                    vmResponse.setJobId(asyncJob.getId());
                    vmResponse.setJobStatus(asyncJob.getStatus());
                } 

                vmResponse.setZoneId(vm.getDataCenterId());
                vmResponse.setZoneName(ApiDBUtils.findZoneById(vm.getDataCenterId()).getName());
                vmResponse.setDns1(vm.getDns1());
                vmResponse.setDns2(vm.getDns2());
                vmResponse.setNetworkDomain(vm.getDomain());
                vmResponse.setGateway(vm.getGateway());
                vmResponse.setName(vm.getName());
                vmResponse.setPodId(vm.getPodId());
                if (vm.getHostId() != null) {
                    vmResponse.setHostId(vm.getHostId());
                    vmResponse.setHostName(ApiDBUtils.findHostById(vm.getHostId()).getName());
                }
                vmResponse.setPrivateIp(vm.getPrivateIpAddress());
                vmResponse.setPrivateMacAddress(vm.getPrivateMacAddress());
                vmResponse.setPrivateNetmask(vm.getPrivateNetmask());
                vmResponse.setPublicIp(vm.getPublicIpAddress());
                vmResponse.setPublicMacAddress(vm.getPublicMacAddress());
                vmResponse.setPublicNetmask(vm.getPublicNetmask());
                vmResponse.setTemplateId(vm.getTemplateId());
                vmResponse.setCreated(vm.getCreated());
                if (vm.getState() != null) {
                    vmResponse.setState(vm.getState().toString());
                }
            }

            // for console proxies, add the active sessions
            if (systemVM instanceof ConsoleProxyVO) {
                ConsoleProxyVO proxy = (ConsoleProxyVO)systemVM;
                vmResponse.setActiveViewerSessions(proxy.getActiveSession());
            }

            vmResponse.setResponseName("systemvm");
            vmResponses.add(vmResponse);
        }

        response.setResponses(vmResponses);
        response.setResponseName(getName());
        return response;
    }
}
