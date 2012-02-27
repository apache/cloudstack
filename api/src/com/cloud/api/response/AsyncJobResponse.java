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
package com.cloud.api.response;

import java.util.Date;

import org.apache.log4j.Logger;

import com.cloud.api.ApiConstants;
import com.cloud.api.IdentityProxy;
import com.cloud.api.ResponseObject;
import com.cloud.async.AsyncJob;
import com.cloud.serializer.Param;
import com.google.gson.annotations.SerializedName;

@SuppressWarnings("unused")
public class AsyncJobResponse extends BaseResponse {
    private static final Logger s_logger = Logger.getLogger(AsyncJobResponse.class.getName());

    @SerializedName("accountid") @Param(description="the account that executed the async command")
    private IdentityProxy accountId = new IdentityProxy("account");

    @SerializedName(ApiConstants.USER_ID) @Param(description="the user that executed the async command")
    private IdentityProxy userId = new IdentityProxy("user");

    @SerializedName("cmd") @Param(description="the async command executed")
    private String cmd;

    @SerializedName("jobstatus") @Param(description="the current job status-should be 0 for PENDING")
    private Integer jobStatus;

    @SerializedName("jobprocstatus") @Param(description="the progress information of the PENDING job")
    private Integer jobProcStatus;

    @SerializedName("jobresultcode") @Param(description="the result code for the job")
    private Integer jobResultCode;

    @SerializedName("jobresulttype") @Param(description="the result type")
    private String jobResultType;

    @SerializedName("jobresult") @Param(description="the result reason")
    private ResponseObject jobResult;
 
    @SerializedName("jobinstancetype") @Param(description="the instance/entity object related to the job")
    private String jobInstanceType;

    @SerializedName("jobinstanceid") @Param(description="the unique ID of the instance/entity object related to the job")
    private IdentityProxy jobInstanceId = new IdentityProxy();

    @SerializedName(ApiConstants.CREATED) @Param(description="	the created date of the job")
    private Date created;

    public void setAccountId(Long accountId) {
        this.accountId.setValue(accountId);
    }

    public void setUserId(Long userId) {
        this.userId.setValue(userId);
    }

    public void setCmd(String cmd) {
        this.cmd = cmd;
    }

    public void setJobStatus(Integer jobStatus) {
        this.jobStatus = jobStatus;
    }

    public void setJobProcStatus(Integer jobProcStatus) {
        this.jobProcStatus = jobProcStatus;
    }

    public void setJobResultCode(Integer jobResultCode) {
        this.jobResultCode = jobResultCode;
    }

    public void setJobResultType(String jobResultType) {
        this.jobResultType = jobResultType;
    }

    public void setJobResult(ResponseObject jobResult) {
        this.jobResult = jobResult;
    }

    public void setJobInstanceType(String jobInstanceType) {
        this.jobInstanceType = jobInstanceType;

        if(jobInstanceType != null) {
        	if(jobInstanceType.equalsIgnoreCase(AsyncJob.Type.Volume.toString())) {
        		this.jobInstanceId.setTableName("volumes");
        	} else if (jobInstanceType.equalsIgnoreCase(AsyncJob.Type.Template.toString())) {
        		this.jobInstanceId.setTableName("vm_template");
        	} else if (jobInstanceType.equalsIgnoreCase(AsyncJob.Type.Iso.toString())) {
        		this.jobInstanceId.setTableName("vm_template");
        	} else if (jobInstanceType.equalsIgnoreCase(AsyncJob.Type.VirtualMachine.toString()) || jobInstanceType.equalsIgnoreCase(AsyncJob.Type.ConsoleProxy.toString()) || jobInstanceType.equalsIgnoreCase(AsyncJob.Type.SystemVm.toString()) || jobInstanceType.equalsIgnoreCase(AsyncJob.Type.DomainRouter.toString()) ) {
        		this.jobInstanceId.setTableName("vm_instance");
        	} else if (jobInstanceType.equalsIgnoreCase(AsyncJob.Type.Snapshot.toString())) {
        		this.jobInstanceId.setTableName("snapshots");
        	} else if (jobInstanceType.equalsIgnoreCase(AsyncJob.Type.Host.toString())) {
        		this.jobInstanceId.setTableName("host");
        	} else if (jobInstanceType.equalsIgnoreCase(AsyncJob.Type.StoragePool.toString())) {
        		this.jobInstanceId.setTableName("storage_pool");
        	} else if (jobInstanceType.equalsIgnoreCase(AsyncJob.Type.IpAddress.toString())) {
        		this.jobInstanceId.setTableName("user_ip_address");
        	} else if (jobInstanceType.equalsIgnoreCase(AsyncJob.Type.SecurityGroup.toString())) {
        		this.jobInstanceId.setTableName("security_group");
        	} else if (jobInstanceType.equalsIgnoreCase(AsyncJob.Type.PhysicalNetwork.toString())) {
        		this.jobInstanceId.setTableName("physical_network");
            } else if (jobInstanceType.equalsIgnoreCase(AsyncJob.Type.TrafficType.toString())) {
                this.jobInstanceId.setTableName("physical_network_traffic_types");
            } else if (jobInstanceType.equalsIgnoreCase(AsyncJob.Type.PhysicalNetworkServiceProvider.toString())) {
                this.jobInstanceId.setTableName("physical_network_service_providers");
        	} else if (jobInstanceType.equalsIgnoreCase(AsyncJob.Type.FirewallRule.toString())) {
        	    this.jobInstanceId.setTableName("firewall_rules");
        	} else if (jobInstanceType.equalsIgnoreCase(AsyncJob.Type.Account.toString())) {
                this.jobInstanceId.setTableName("account");
            } else if (jobInstanceType.equalsIgnoreCase(AsyncJob.Type.User.toString())) {
                this.jobInstanceId.setTableName("user");
            } else if (!jobInstanceType.equalsIgnoreCase(AsyncJob.Type.None.toString())){
                s_logger.warn("Failed to get async job instanceId for job instance type " + jobInstanceType);
        		// TODO : when we hit here, we need to add instanceType -> UUID entity table mapping
        		assert(false);
        	}
        }
    }

    public void setJobInstanceId(Long jobInstanceId) {
    	this.jobInstanceId.setValue(jobInstanceId);
    }

    public void setCreated(Date created) {
        this.created = created;
    }
}
