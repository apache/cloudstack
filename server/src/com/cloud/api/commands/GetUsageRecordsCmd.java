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
package com.cloud.api.commands;

import java.text.DecimalFormat;
import java.util.ArrayList;
import java.util.Calendar;
import java.util.Date;
import java.util.List;
import java.util.TimeZone;

import org.apache.cloudstack.api.response.AccountResponse;
import org.apache.cloudstack.api.response.DomainResponse;
import org.apache.cloudstack.api.response.ProjectResponse;
import org.apache.log4j.Logger;

import org.apache.cloudstack.api.ApiConstants;
import com.cloud.api.ApiDBUtils;
import com.cloud.dc.DataCenter;
import com.cloud.domain.Domain;

import org.apache.cloudstack.api.BaseListCmd;
import org.apache.cloudstack.api.APICommand;
import org.apache.cloudstack.api.Parameter;
import org.apache.cloudstack.api.response.ListResponse;
import com.cloud.projects.Project;
import com.cloud.server.ManagementServerExt;
import com.cloud.storage.VMTemplateVO;

import org.apache.cloudstack.api.response.UsageRecordResponse;

import com.cloud.usage.UsageTypes;
import com.cloud.usage.UsageVO;
import com.cloud.user.Account;
import com.cloud.uuididentity.dao.IdentityDao;
import com.cloud.uuididentity.dao.IdentityDaoImpl;
import com.cloud.vm.VMInstanceVO;

@APICommand(name = "listUsageRecords", description="Lists usage records for accounts", responseObject=UsageRecordResponse.class)
public class GetUsageRecordsCmd extends BaseListCmd {
    public static final Logger s_logger = Logger.getLogger(GetUsageRecordsCmd.class.getName());

    private static final String s_name = "listusagerecordsresponse";

    /////////////////////////////////////////////////////
    //////////////// API parameters /////////////////////
    /////////////////////////////////////////////////////

    @Parameter(name=ApiConstants.ACCOUNT, type=CommandType.STRING, description="List usage records for the specified user.")
    private String accountName;

    @Parameter(name=ApiConstants.DOMAIN_ID, type=CommandType.UUID, entityType = DomainResponse.class,
            description="List usage records for the specified domain.")
    private Long domainId;

    @Parameter(name=ApiConstants.END_DATE, type=CommandType.DATE, required=true, description="End date range for usage record query. Use yyyy-MM-dd as the date format, e.g. startDate=2009-06-03.")
    private Date endDate;

    @Parameter(name=ApiConstants.START_DATE, type=CommandType.DATE, required=true, description="Start date range for usage record query. Use yyyy-MM-dd as the date format, e.g. startDate=2009-06-01.")
    private Date startDate;

    @Parameter(name=ApiConstants.ACCOUNT_ID, type=CommandType.UUID, entityType = AccountResponse.class,
            description="List usage records for the specified account")
    private Long accountId;

    @Parameter(name=ApiConstants.PROJECT_ID, type=CommandType.UUID, entityType = ProjectResponse.class,
            description="List usage records for specified project")
    private Long projectId;

    @Parameter(name=ApiConstants.TYPE, type=CommandType.LONG, description="List usage records for the specified usage type")
    private Long usageType;

    /////////////////////////////////////////////////////
    /////////////////// Accessors ///////////////////////
    /////////////////////////////////////////////////////

    public String getAccountName() {
        return accountName;
    }

    public Long getDomainId() {
        return domainId;
    }

    public Date getEndDate() {
        return endDate;
    }

    public Date getStartDate() {
        return startDate;
    }

    public Long getAccountId() {
        return accountId;
    }

    public Long getUsageType() {
        return usageType;
    }

    public Long getProjectId() {
        return projectId;
    }

    /////////////////////////////////////////////////////
    ///////////////  Misc parameters  ///////////////////
    /////////////////////////////////////////////////////

    private TimeZone usageTimezone;

    public TimeZone getUsageTimezone() {
        return usageTimezone;
    }

    public void setUsageTimezone(TimeZone tz) {
        this.usageTimezone = tz;
    }

    /////////////////////////////////////////////////////
    /////////////// API Implementation///////////////////
    /////////////////////////////////////////////////////

    @Override
    public String getCommandName() {
        return s_name;
    }

    public String getDateStringInternal(Date inputDate) {
        if (inputDate == null) return null;

        TimeZone tz = getUsageTimezone();
        Calendar cal = Calendar.getInstance(tz);
        cal.setTime(inputDate);

        StringBuffer sb = new StringBuffer();
        sb.append(cal.get(Calendar.YEAR)+"-");

        int month = cal.get(Calendar.MONTH) + 1;
        if (month < 10) {
            sb.append("0" + month + "-");
        } else {
            sb.append(month+"-");
        }

        int day = cal.get(Calendar.DAY_OF_MONTH);
        if (day < 10) {
            sb.append("0" + day);
        } else {
            sb.append(""+day);
        }

        sb.append("'T'");

        int hour = cal.get(Calendar.HOUR_OF_DAY);
        if (hour < 10) {
            sb.append("0" + hour + ":");
        } else {
            sb.append(hour+":");
        }

        int minute = cal.get(Calendar.MINUTE);
        if (minute < 10) {
            sb.append("0" + minute + ":");
        } else {
            sb.append(minute+":");
        }

        int seconds = cal.get(Calendar.SECOND);
        if (seconds < 10) {
            sb.append("0" + seconds);
        } else {
            sb.append(""+seconds);
        }

        double offset = cal.get(Calendar.ZONE_OFFSET);
        if (tz.inDaylightTime(inputDate)) {
            offset += (1.0*tz.getDSTSavings()); // add the timezone's DST value (typically 1 hour expressed in milliseconds)
        }

        offset = offset / (1000d*60d*60d);
        int hourOffset = (int)offset;
        double decimalVal = Math.abs(offset) - Math.abs(hourOffset);
        int minuteOffset = (int)(decimalVal * 60);

        if (hourOffset < 0) {
            if (hourOffset > -10) {
                sb.append("-0"+Math.abs(hourOffset));
            } else {
                sb.append("-"+Math.abs(hourOffset));
            }
        } else {
            if (hourOffset < 10) {
                sb.append("+0" + hourOffset);
            } else {
                sb.append("+" + hourOffset);
            }
        }

        sb.append(":");

        if (minuteOffset == 0) {
            sb.append("00");
        } else if (minuteOffset < 10) {
            sb.append("0" + minuteOffset);
        } else {
            sb.append("" + minuteOffset);
        }

        return sb.toString();
    }

    @Override
    public void execute(){
        ManagementServerExt _mgrExt = (ManagementServerExt)_mgr;
        List<UsageVO> usageRecords = _mgrExt.getUsageRecords(this);
        IdentityDao identityDao = new IdentityDaoImpl();
        ListResponse<UsageRecordResponse> response = new ListResponse<UsageRecordResponse>();
        List<UsageRecordResponse> usageResponses = new ArrayList<UsageRecordResponse>();
        for (Object usageRecordGeneric : usageRecords) {
            UsageRecordResponse usageRecResponse = new UsageRecordResponse();
            if (usageRecordGeneric instanceof UsageVO) {
                UsageVO usageRecord = (UsageVO)usageRecordGeneric;

                Account account = ApiDBUtils.findAccountByIdIncludingRemoved(usageRecord.getAccountId());
                if (account.getType() == Account.ACCOUNT_TYPE_PROJECT) {
                    //find the project
                    Project project = ApiDBUtils.findProjectByProjectAccountId(account.getId());
                    usageRecResponse.setProjectId(project.getUuid());
                    usageRecResponse.setProjectName(project.getName());
                } else {
                    usageRecResponse.setAccountId(account.getUuid());
                    usageRecResponse.setAccountName(account.getAccountName());
                }

                Domain domain = ApiDBUtils.findDomainById(usageRecord.getDomainId());
                if (domain != null) {
                    usageRecResponse.setDomainId(domain.getUuid());
                }

                if (usageRecord.getZoneId() != null) {
                    DataCenter zone = ApiDBUtils.findZoneById(usageRecord.getZoneId());
                    if (zone != null) {
                        usageRecResponse.setZoneId(zone.getUuid());
                    }
                }
                usageRecResponse.setDescription(usageRecord.getDescription());
                usageRecResponse.setUsage(usageRecord.getUsageDisplay());
                usageRecResponse.setUsageType(usageRecord.getUsageType());
                if (usageRecord.getVmInstanceId() != null) {
                    VMInstanceVO vm = ApiDBUtils.findVMInstanceById(usageRecord.getVmInstanceId());
                    if (vm != null) {
                        usageRecResponse.setVirtualMachineId(vm.getUuid());
                    }
                }
                usageRecResponse.setVmName(usageRecord.getVmName());
                if (usageRecord.getTemplateId() != null) {
                    VMTemplateVO template = ApiDBUtils.findTemplateById(usageRecord.getTemplateId());
                    if (template != null) {
                        usageRecResponse.setTemplateId(template.getUuid());
                    }
                }

                if(usageRecord.getUsageType() == UsageTypes.RUNNING_VM || usageRecord.getUsageType() == UsageTypes.ALLOCATED_VM){
                	//Service Offering Id
                	usageRecResponse.setOfferingId(identityDao.getIdentityUuid("disk_offering", usageRecord.getOfferingId().toString()));
                	//VM Instance ID
                	usageRecResponse.setUsageId(identityDao.getIdentityUuid("vm_instance", usageRecord.getUsageId().toString()));
                	//Hypervisor Type
                	usageRecResponse.setType(usageRecord.getType());

                } else if(usageRecord.getUsageType() == UsageTypes.IP_ADDRESS){
                	//isSourceNAT
                    usageRecResponse.setSourceNat((usageRecord.getType().equals("SourceNat"))?true:false);
                    //isSystem
                    usageRecResponse.setSystem((usageRecord.getSize() == 1)?true:false);
                    //IP Address ID
                    usageRecResponse.setUsageId(identityDao.getIdentityUuid("user_ip_address", usageRecord.getUsageId().toString()));

                } else if(usageRecord.getUsageType() == UsageTypes.NETWORK_BYTES_SENT || usageRecord.getUsageType() == UsageTypes.NETWORK_BYTES_RECEIVED){
                	//Device Type
                	usageRecResponse.setType(usageRecord.getType());
                	if(usageRecord.getType().equals("DomainRouter")){
                        //Domain Router Id
                        usageRecResponse.setUsageId(identityDao.getIdentityUuid("vm_instance", usageRecord.getUsageId().toString()));
                	} else {
                		//External Device Host Id
                		usageRecResponse.setUsageId(identityDao.getIdentityUuid("host", usageRecord.getUsageId().toString()));
                	}
                    //Network ID
                    usageRecResponse.setNetworkId(identityDao.getIdentityUuid("networks", usageRecord.getNetworkId().toString()));

                } else if(usageRecord.getUsageType() == UsageTypes.VOLUME){
                    //Volume ID
                    usageRecResponse.setUsageId(identityDao.getIdentityUuid("volumes", usageRecord.getUsageId().toString()));
                    //Volume Size
                    usageRecResponse.setSize(usageRecord.getSize());
                	//Disk Offering Id
                    if(usageRecord.getOfferingId() != null){
                    	usageRecResponse.setOfferingId(identityDao.getIdentityUuid("disk_offering", usageRecord.getOfferingId().toString()));
                    }

                } else if(usageRecord.getUsageType() == UsageTypes.TEMPLATE || usageRecord.getUsageType() == UsageTypes.ISO){
                    //Template/ISO ID
                    usageRecResponse.setUsageId(identityDao.getIdentityUuid("vm_template", usageRecord.getUsageId().toString()));
                    //Template/ISO Size
                    usageRecResponse.setSize(usageRecord.getSize());

                } else if(usageRecord.getUsageType() == UsageTypes.SNAPSHOT){
                    //Snapshot ID
                    usageRecResponse.setUsageId(identityDao.getIdentityUuid("snapshots", usageRecord.getUsageId().toString()));
                    //Snapshot Size
                    usageRecResponse.setSize(usageRecord.getSize());

                } else if(usageRecord.getUsageType() == UsageTypes.LOAD_BALANCER_POLICY){
                    //Load Balancer Policy ID
                    usageRecResponse.setUsageId(usageRecord.getUsageId().toString());

                } else if(usageRecord.getUsageType() == UsageTypes.PORT_FORWARDING_RULE){
                    //Port Forwarding Rule ID
                    usageRecResponse.setUsageId(usageRecord.getUsageId().toString());

                } else if(usageRecord.getUsageType() == UsageTypes.NETWORK_OFFERING){
                	//Network Offering Id
                	usageRecResponse.setOfferingId(identityDao.getIdentityUuid("network_offerings", usageRecord.getOfferingId().toString()));
                	//is Default
                	usageRecResponse.setDefault((usageRecord.getUsageId() == 1)? true:false);

                } else if(usageRecord.getUsageType() == UsageTypes.VPN_USERS){
                    //VPN User ID
                    usageRecResponse.setUsageId(usageRecord.getUsageId().toString());

                } else if(usageRecord.getUsageType() == UsageTypes.SECURITY_GROUP){
                	//Security Group Id
                	usageRecResponse.setUsageId(identityDao.getIdentityUuid("security_group", usageRecord.getUsageId().toString()));
                }

                if (usageRecord.getRawUsage() != null) {
                    DecimalFormat decimalFormat = new DecimalFormat("###########.######");
                    usageRecResponse.setRawUsage(decimalFormat.format(usageRecord.getRawUsage()));
                }

                if (usageRecord.getStartDate() != null) {
                    usageRecResponse.setStartDate(getDateStringInternal(usageRecord.getStartDate()));
                }
                if (usageRecord.getEndDate() != null) {
                    usageRecResponse.setEndDate(getDateStringInternal(usageRecord.getEndDate()));
                }
            }

            usageRecResponse.setObjectName("usagerecord");
            usageResponses.add(usageRecResponse);
        }

        response.setResponses(usageResponses);
        response.setResponseName(getCommandName());
        this.setResponseObject(response);
    }
}
