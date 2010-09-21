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

import com.google.gson.annotations.SerializedName;

public class PortForwardingServiceRuleResponse extends BaseResponse {
    @SerializedName("id")
    private long ruleId;

    @SerializedName("publicport")
    private String publicPort;

    @SerializedName("privateport")
    private String privatePort;

    @SerializedName("protocol")
    private String protocol;

    @SerializedName("portforwardingserviceid")
    private Long portForwardingServiceId;

    @SerializedName("jobid")
    private Long jobId;

    @SerializedName("jobstatus")
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
