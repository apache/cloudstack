/**
 * *  Copyright (C) 2011 Citrix Systems, Inc.  All rights reserved
*
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
import java.util.Calendar;
import java.util.Date;
import java.util.List;
import java.util.TimeZone;

import org.apache.log4j.Logger;

import com.cloud.api.ApiConstants;
import com.cloud.api.ApiDBUtils;
import com.cloud.api.BaseListCmd;
import com.cloud.api.IdentityMapper;
import com.cloud.api.Implementation;
import com.cloud.api.Parameter;
import com.cloud.api.response.ListResponse;
import com.cloud.projects.Project;
import com.cloud.server.ManagementServerExt;
import com.cloud.server.api.response.UsageRecordResponse;
import com.cloud.usage.UsageTypes;
import com.cloud.usage.UsageVO;
import com.cloud.user.Account;

@Implementation(description="Lists usage records for accounts", responseObject=UsageRecordResponse.class)
public class GetUsageRecordsCmd extends BaseListCmd {
    public static final Logger s_logger = Logger.getLogger(GetUsageRecordsCmd.class.getName());

    private static final String s_name = "listusagerecordsresponse";

    /////////////////////////////////////////////////////
    //////////////// API parameters /////////////////////
    /////////////////////////////////////////////////////

    @Parameter(name=ApiConstants.ACCOUNT, type=CommandType.STRING, description="List usage records for the specified user.")
    private String accountName;

    @IdentityMapper(entityTableName="domain")
    @Parameter(name=ApiConstants.DOMAIN_ID, type=CommandType.LONG, description="List usage records for the specified domain.")
    private Long domainId;

    @Parameter(name=ApiConstants.END_DATE, type=CommandType.DATE, required=true, description="End date range for usage record query. Use yyyy-MM-dd as the date format, e.g. startDate=2009-06-03.")
    private Date endDate;

    @Parameter(name=ApiConstants.START_DATE, type=CommandType.DATE, required=true, description="Start date range for usage record query. Use yyyy-MM-dd as the date format, e.g. startDate=2009-06-01.")
    private Date startDate;
    
    @IdentityMapper(entityTableName="account")
    @Parameter(name=ApiConstants.ACCOUNT_ID, type=CommandType.LONG, description="List usage records for the specified account")
    private Long accountId;
    
    @IdentityMapper(entityTableName="projects")
    @Parameter(name=ApiConstants.PROJECT_ID, type=CommandType.LONG, description="List usage records for specified project")
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
                    usageRecResponse.setProjectId(project.getId());
                    usageRecResponse.setProjectName(project.getName());
                } else {
                    usageRecResponse.setAccountId(account.getId());
                    usageRecResponse.setAccountName(account.getAccountName());
                }
             
                usageRecResponse.setDomainId(usageRecord.getDomainId());
                
                usageRecResponse.setZoneId(usageRecord.getZoneId());
                usageRecResponse.setDescription(usageRecord.getDescription());
                usageRecResponse.setUsage(usageRecord.getUsageDisplay());
                usageRecResponse.setUsageType(usageRecord.getUsageType());
                usageRecResponse.setVirtualMachineId(usageRecord.getVmInstanceId());
                usageRecResponse.setVmName(usageRecord.getVmName());
                usageRecResponse.setServiceOfferingId(usageRecord.getOfferingId());
                usageRecResponse.setTemplateId(usageRecord.getTemplateId());
                usageRecResponse.setUsageId(usageRecord.getUsageId());
                if(usageRecord.getUsageType() == UsageTypes.IP_ADDRESS){
                    usageRecResponse.setSourceNat((usageRecord.getType().equals("SourceNat"))?true:false);
                } else {
                    usageRecResponse.setType(usageRecord.getType());
                }
                usageRecResponse.setSize(usageRecord.getSize());

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
