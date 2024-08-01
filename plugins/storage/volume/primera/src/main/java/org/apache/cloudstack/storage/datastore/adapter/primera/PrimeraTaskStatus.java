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
package org.apache.cloudstack.storage.datastore.adapter.primera;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.fasterxml.jackson.annotation.JsonInclude;

@JsonIgnoreProperties(ignoreUnknown = true)
@JsonInclude(JsonInclude.Include.NON_NULL)
public class PrimeraTaskStatus {
    private Integer id;
    private Integer type;
    private String name;
    private Integer status;
    private Integer completedPhases;
    private Integer totalPhases;
    private Integer completedSteps;
    private Integer totalSteps;
    private String startTime;
    private String finishTime;
    private Integer priority;
    private String user;
    private String detailedStatus;
    public static final Integer STATUS_DONE = 1;
    public static final Integer STATUS_ACTIVE = 2;
    public static final Integer STATUS_CANCELLED = 3;
    public static final Integer STATUS_FAILED = 4;

    public boolean isFinished() {
        if (status != STATUS_ACTIVE) {
            return true;
        }
        return false;
    }

    public boolean isSuccess() {
        if (status == STATUS_DONE) {
            return true;
        }
        return false;
    }

    public String getStatusName() {
        if (status == PrimeraTaskStatus.STATUS_DONE) {
            return "DONE";
        } else if (status == PrimeraTaskStatus.STATUS_ACTIVE) {
            return "ACTIVE";
        } else if (status == PrimeraTaskStatus.STATUS_CANCELLED) {
            return "CANCELLED";
        } else if (status == PrimeraTaskStatus.STATUS_FAILED) {
            return "FAILED";
        } else {
            return "UNKNOWN";
        }
    }

    public Integer getId() {
        return id;
    }

    public void setId(Integer id) {
        this.id = id;
    }

    public Integer getType() {
        return type;
    }

    public void setType(Integer type) {
        this.type = type;
    }

    public String getName() {
        return name;
    }

    public void setName(String name) {
        this.name = name;
    }

    public Integer getStatus() {
        return status;
    }

    public void setStatus(Integer status) {
        this.status = status;
    }

    public Integer getCompletedPhases() {
        return completedPhases;
    }

    public void setCompletedPhases(Integer completedPhases) {
        this.completedPhases = completedPhases;
    }

    public Integer getTotalPhases() {
        return totalPhases;
    }

    public void setTotalPhases(Integer totalPhases) {
        this.totalPhases = totalPhases;
    }

    public Integer getCompletedSteps() {
        return completedSteps;
    }

    public void setCompletedSteps(Integer completedSteps) {
        this.completedSteps = completedSteps;
    }

    public Integer getTotalSteps() {
        return totalSteps;
    }

    public void setTotalSteps(Integer totalSteps) {
        this.totalSteps = totalSteps;
    }

    public String getStartTime() {
        return startTime;
    }

    public void setStartTime(String startTime) {
        this.startTime = startTime;
    }

    public String getFinishTime() {
        return finishTime;
    }

    public void setFinishTime(String finishTime) {
        this.finishTime = finishTime;
    }

    public Integer getPriority() {
        return priority;
    }

    public void setPriority(Integer priority) {
        this.priority = priority;
    }

    public String getUser() {
        return user;
    }

    public void setUser(String user) {
        this.user = user;
    }

    public String getDetailedStatus() {
        return detailedStatus;
    }

    public void setDetailedStatus(String detailedStatus) {
        this.detailedStatus = detailedStatus;
    }
}
