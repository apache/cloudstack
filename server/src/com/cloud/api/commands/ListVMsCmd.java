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

import java.text.DecimalFormat;
import java.util.ArrayList;
import java.util.List;

import org.apache.log4j.Logger;

import com.cloud.api.ApiDBUtils;
import com.cloud.api.BaseListCmd;
import com.cloud.api.Implementation;
import com.cloud.api.Parameter;
import com.cloud.api.response.UserVmResponse;
import com.cloud.async.AsyncJobVO;
import com.cloud.offering.ServiceOffering;
import com.cloud.serializer.SerializerHelper;
import com.cloud.storage.VMTemplateVO;
import com.cloud.user.Account;
import com.cloud.user.UserContext;
import com.cloud.vm.UserVmVO;
import com.cloud.vm.VmStats;

@Implementation(method="searchForUserVMs")
public class ListVMsCmd extends BaseListCmd {
    public static final Logger s_logger = Logger.getLogger(ListVMsCmd.class.getName());

    private static final String s_name = "listvirtualmachinesresponse";

    /////////////////////////////////////////////////////
    //////////////// API parameters /////////////////////
    /////////////////////////////////////////////////////

    @Parameter(name="account", type=CommandType.STRING)
    private String accountName;

    @Parameter(name="domainid", type=CommandType.LONG)
    private Long domainId;

    @Parameter(name="hostid", type=CommandType.LONG)
    private Long hostId;

    @Parameter(name="id", type=CommandType.LONG)
    private Long id;

    @Parameter(name="name", type=CommandType.STRING)
    private String instanceName;

    @Parameter(name="podid", type=CommandType.LONG)
    private Long podId;

    @Parameter(name="state", type=CommandType.STRING)
    private String state;

    @Parameter(name="zoneid", type=CommandType.LONG)
    private Long zoneId;

    /////////////////////////////////////////////////////
    /////////////////// Accessors ///////////////////////
    /////////////////////////////////////////////////////

    public String getAccountName() {
        return accountName;
    }

    public Long getDomainId() {
        return domainId;
    }

    public Long getHostId() {
        return hostId;
    }

    public Long getId() {
        return id;
    }

    public String getInstanceName() {
        return instanceName;
    }

    public Long getPodId() {
        return podId;
    }

    public String getState() {
        return state;
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
	public String getResponse() {
        List<UserVmVO> userVms = (List<UserVmVO>)getResponseObject();

        List<UserVmResponse> response = new ArrayList<UserVmResponse>();
        for (UserVmVO userVm : userVms) {
            UserVmResponse userVmResponse = new UserVmResponse();
            userVmResponse.setId(userVm.getId());
            AsyncJobVO asyncJob = ApiDBUtils.findInstancePendingAsyncJob("vm_instance", userVm.getId());
            if (asyncJob != null) {
                userVmResponse.setJobId(asyncJob.getId());
                userVmResponse.setJobStatus(asyncJob.getStatus());
            } 

            userVmResponse.setName(userVm.getName());
            userVmResponse.setCreated(userVm.getCreated());
            userVmResponse.setPrivateIp(userVm.getPrivateIpAddress());
            if (userVm.getState() != null) {
                userVmResponse.setState(userVm.getState().toString());
            }

            Account acct = ApiDBUtils.findAccountById(Long.valueOf(userVm.getAccountId()));
            if (acct != null) {
                userVmResponse.setAccountName(acct.getAccountName());
                userVmResponse.setDomainId(acct.getDomainId());
                userVmResponse.setDomainName(ApiDBUtils.findDomainById(acct.getDomainId()).getName());
            }

            userVmResponse.setHaEnable(userVm.isHaEnabled());
            
            if (userVm.getDisplayName() != null) {
                userVmResponse.setDisplayName(userVm.getDisplayName());
            } else {
                userVmResponse.setDisplayName(userVm.getName());
            }

            userVmResponse.setGroup(userVm.getGroup());

            // Data Center Info
            userVmResponse.setZoneId(userVm.getDataCenterId());
            userVmResponse.setZoneName(ApiDBUtils.findZoneById(userVm.getDataCenterId()).getName());

            Account account = (Account)UserContext.current().getAccountObject();
            //if user is an admin, display host id
            if (((account == null) || isAdmin(account.getType())) && (userVm.getHostId() != null)) {
                userVmResponse.setHostId(userVm.getHostId());
                userVmResponse.setHostName(ApiDBUtils.findHostById(userVm.getHostId()).getName());
            }

            // Template Info
            VMTemplateVO template = ApiDBUtils.findTemplateById(userVm.getTemplateId());
            if (template != null) {
                userVmResponse.setTemplateId(userVm.getTemplateId());
                userVmResponse.setTemplateName(template.getName());
                userVmResponse.setTemplateDisplayText(template.getDisplayText());
                userVmResponse.setPasswordEnabled(template.getEnablePassword());
            } else {
                userVmResponse.setTemplateId(-1L);
                userVmResponse.setTemplateName("ISO Boot");
                userVmResponse.setTemplateDisplayText("ISO Boot");
                userVmResponse.setPasswordEnabled(false);
            }

            // ISO Info
            if (userVm.getIsoId() != null) {
                VMTemplateVO iso = ApiDBUtils.findTemplateById(userVm.getIsoId().longValue());
                if (iso != null) {
                    userVmResponse.setIsoId(userVm.getIsoId());
                    userVmResponse.setIsoName(iso.getName());
                }
            }

            // Service Offering Info
            ServiceOffering offering = ApiDBUtils.findServiceOfferingById(userVm.getServiceOfferingId());
            userVmResponse.setServiceOfferingId(userVm.getServiceOfferingId());
            userVmResponse.setServiceOfferingName(offering.getName());
            userVmResponse.setCpuNumber(offering.getCpu());
            userVmResponse.setCpuSpeed(offering.getSpeed());
            userVmResponse.setMemory(offering.getRamSize());
            
            //stats calculation
            DecimalFormat decimalFormat = new DecimalFormat("#.##");
            String cpuUsed = null;
            VmStats vmStats = ApiDBUtils.getVmStatistics(userVm.getId());
            if (vmStats != null) {
                float cpuUtil = (float) vmStats.getCPUUtilization();
                cpuUsed = decimalFormat.format(cpuUtil) + "%";
                userVmResponse.setCpuUsed(cpuUsed);

                long networkKbRead = (long)vmStats.getNetworkReadKBs();
                userVmResponse.setNetworkKbsRead(networkKbRead);
                
                long networkKbWrite = (long)vmStats.getNetworkWriteKBs();
                userVmResponse.setNetworkKbsWrite(networkKbWrite);
            }
            
            userVmResponse.setOsTypeId(userVm.getGuestOSId());

            //network groups
            userVmResponse.setNetworkGroupList(ApiDBUtils.getNetworkGroupsNamesForVm(userVm.getId()));

            response.add(userVmResponse);
        }

        return SerializerHelper.toSerializedString(response);
    }
}
