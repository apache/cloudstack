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
package org.apache.cloudstack.api.command.user.job;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Date;
import java.util.List;
import java.util.Locale;

import com.cloud.exception.InvalidParameterValueException;
import com.cloud.utils.StringUtils;

import org.apache.cloudstack.api.APICommand;
import org.apache.cloudstack.api.ApiArgValidator;
import org.apache.cloudstack.api.ApiConstants;
import org.apache.cloudstack.api.BaseListAccountResourcesCmd;
import org.apache.cloudstack.api.Parameter;
import org.apache.cloudstack.api.response.AsyncJobResponse;
import org.apache.cloudstack.api.response.ListResponse;
import org.apache.cloudstack.api.response.ManagementServerResponse;
import org.apache.cloudstack.jobs.JobInfo;

@APICommand(name = "listAsyncJobs", description = "Lists asynchronous jobs for the Account.", responseObject = AsyncJobResponse.class,
        requestHasSensitiveInfo = false, responseHasSensitiveInfo = false)
public class ListAsyncJobsCmd extends BaseListAccountResourcesCmd {

    /////////////////////////////////////////////////////
    //////////////// API parameters /////////////////////
    /////////////////////////////////////////////////////

    @Parameter(name = ApiConstants.START_DATE, type = CommandType.DATE, description = "The start date from which the async jobs should be listed. Only jobs created on or after this date will be included. (use format \"yyyy-MM-dd'T'HH:mm:ss'+'SSSS\")")
    private Date startDate;

    @Parameter(name = ApiConstants.END_DATE, type = CommandType.DATE, description = "The end date up to which the async jobs should be listed. Only jobs created on or before this date will be included. (use format \"yyyy-MM-dd'T'HH:mm:ss'+'SSSS\")")
    private Date endDate;

    @Parameter(name = ApiConstants.MANAGEMENT_SERVER_ID, type = CommandType.UUID, entityType = ManagementServerResponse.class, description = "The id of the management server", since="4.19")
    private Long managementServerId;

    @Parameter(name = ApiConstants.RESOURCE_ID, validations = {ApiArgValidator.UuidString}, type = CommandType.STRING, description = "the ID of the resource associated with the job", since="4.22.1")
    private String resourceId;

    @Parameter(name = ApiConstants.RESOURCE_TYPE, type = CommandType.STRING, description = "the type of the resource associated with the job", since="4.22.1")
    private String resourceType;

    @Parameter(name = ApiConstants.JOB_STATUS, type = CommandType.LIST, collectionType = CommandType.STRING, description = "Comma-separated list of job statuses to list the async jobs by. " +
            "Accepts names (IN_PROGRESS, SUCCEEDED, FAILED, CANCELLED) or ordinals (0, 1, 2, 3). Only pending jobs are listed by default.", since = "24.0")
    private List<String> jobStatuses;

    @Parameter(name = ApiConstants.DURATION, type = CommandType.INTEGER, description = "the duration in hours to list the async jobs started or completed within that period up to now.", since = "24.0")
    private Integer duration;

    /////////////////////////////////////////////////////
    /////////////////// Accessors ///////////////////////
    /////////////////////////////////////////////////////

    public Date getStartDate() {
        return startDate;
    }

    public Date getEndDate() {
        return endDate;
    }

    public Long getManagementServerId() {
        return managementServerId;
    }

    public String getResourceId() {
        return resourceId;
    }

    public String getResourceType() {
        return resourceType;
    }

    public List<Long> getJobStatuses() {
        if (jobStatuses == null) {
            return null;
        }

        if (jobStatuses.isEmpty()) {
            throw new InvalidParameterValueException("Empty job status");
        }

        List<Long> statuses = new ArrayList<>(jobStatuses.size());
        for (String status : jobStatuses) {
            statuses.add((long)parseJobStatus(status).value());
        }
        return statuses;
    }

    private JobInfo.Status parseJobStatus(String status) {
        if (StringUtils.isBlank(status)) {
            throw new InvalidParameterValueException("Empty job status");
        }

        String value = status.trim();
        try {
            return JobInfo.Status.fromValue(Integer.parseInt(value));
        } catch (NumberFormatException e) {
            // not an ordinal, fall through and try the name
        } catch (IllegalArgumentException e) {
            throw new InvalidParameterValueException(String.format("Invalid job status: %s. Valid values are %s or their ordinals",
                    status, Arrays.toString(JobInfo.Status.values())));
        }

        try {
            return JobInfo.Status.valueOf(value.toUpperCase(Locale.ROOT));
        } catch (IllegalArgumentException e) {
            throw new InvalidParameterValueException(String.format("Invalid job status: %s. Valid values are %s or their ordinals",
                    status, Arrays.toString(JobInfo.Status.values())));
        }
    }

    public Integer getDuration() {
        return duration;
    }

    /////////////////////////////////////////////////////
    /////////////// API Implementation///////////////////
    /////////////////////////////////////////////////////
    @Override
    public void execute() {
        ListResponse<AsyncJobResponse> response = _queryService.searchForAsyncJobs(this);
        response.setResponseName(getCommandName());
        this.setResponseObject(response);
    }
}
