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

import com.cloud.serializer.Param;
import com.google.gson.annotations.SerializedName;

public class PortForwardingServiceRuleResponse extends BaseResponse {
    @SerializedName("id") @Param(description="the ID of the port forwarding service rule")
    private long ruleId;

    @SerializedName("publicport") @Param(description="the public port of the port forwarding service rule")
    private String publicPort;

    @SerializedName("privateport") @Param(description="the private port of the port forwarding service rule")
    private String privatePort;

    @SerializedName("protocol") @Param(description="the protocol (TCP/UDP) of the port forwarding service rule")
    private String protocol;

    @SerializedName("portforwardingserviceid") @Param(description="the id of port forwarding service where the rule belongs to")
    private Long portForwardingServiceId;

    @SerializedName("jobid") @Param(description="the job ID associated with the port forwarding rule. This is only displayed if the rule listed is part of a currently running asynchronous job.")
    private Long jobId;

    @SerializedName("jobstatus") @Param(description="the job status associated with the port forwarding rule.  This is only displayed if the rule listed is part of a currently running asynchronous job.")
    private Integer jobStatus;

    public Long getPortForwardingServiceId() {
        return portForwardingServiceId;
    }

    public void setPortForwardingServiceId(Long portForwardingServiceId) {
        this.portForwardingServiceId = portForwardingServiceId;
    }

    public long getRuleId() {
        return ruleId;
    }

    public void setRuleId(long ruleId) {
        this.ruleId = ruleId;
    }

    public String getPublicPort() {
        return publicPort;
    }

    public void setPublicPort(String publicPort) {
        this.publicPort = publicPort;
    }

    public String getPrivatePort() {
        return privatePort;
    }

    public void setPrivatePort(String privatePort) {
        this.privatePort = privatePort;
    }

    public String getProtocol() {
        return protocol;
    }

    public void setProtocol(String protocol) {
        this.protocol = protocol;
    }

    public Long getJobId() {
        return jobId;
    }

    public void setJobId(Long jobId) {
        this.jobId = jobId;
    }

    public Integer getJobStatus() {
        return jobStatus;
    }

    public void setJobStatus(Integer jobStatus) {
        this.jobStatus = jobStatus;
    }
}
