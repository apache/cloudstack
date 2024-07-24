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
package org.apache.cloudstack.api.response;

import org.apache.cloudstack.management.ManagementServerHost;
import com.cloud.serializer.Param;
import com.google.gson.annotations.SerializedName;
import org.apache.cloudstack.api.ApiConstants;
import org.apache.cloudstack.api.BaseResponse;
import org.apache.cloudstack.api.EntityReference;
import org.apache.cloudstack.management.ManagementServerHost.State;

import java.util.Date;

@EntityReference(value = ManagementServerHost.class)
public class ManagementServerResponse extends BaseResponse {
    @SerializedName(ApiConstants.ID)
    @Param(description = "the ID of the management server")
    private String id;

    @SerializedName(ApiConstants.NAME)
    @Param(description = "the name of the management server")
    private String name;

    @SerializedName(ApiConstants.STATE)
    @Param(description = "the state of the management server")
    private State state;

    @SerializedName(ApiConstants.VERSION)
    @Param(description = "the version of the management server")
    private String version;

    @SerializedName(ApiConstants.JAVA_DISTRIBUTION)
    @Param(description = "the java distribution name running the management server process")
    private String javaDistribution;

    @SerializedName(ApiConstants.JAVA_VERSION)
    @Param(description = "the version of the java distribution running the management server process")
    private String javaVersion;

    @SerializedName(ApiConstants.OS_DISTRIBUTION)
    @Param(description = "the name of the OS distribution running on the management server")
    private String osDistribution;

    @SerializedName(ApiConstants.LAST_SERVER_START)
    @Param(description = "the last time this Management Server was started")
    private Date lastServerStart;

    @SerializedName(ApiConstants.LAST_SERVER_STOP)
    @Param(description = "the last time this Management Server was stopped")
    private Date lastServerStop;

    @SerializedName(ApiConstants.LAST_BOOT)
    @Param(description = "the last time the host on which this Management Server runs was booted")
    private Date lastBoot;

    @SerializedName(ApiConstants.KERNEL_VERSION)
    @Param(description = "the running OS kernel version for this Management Server")
    private String kernelVersion;

    @SerializedName(ApiConstants.SERVICE_IP)
    @Param(description = "the IP Address for this Management Server")
    private String serviceIp;

    public String getId() {
        return this.id;
    }

    public String getName() {
        return this.name;
    }

    public State getState() {
        return state;
    }

    public String getVersion() {
        return version;
    }

    public String getJavaVersion() {
        return javaVersion;
    }

    public String getJavaDistribution() {
        return javaDistribution;
    }

    public String getOsDistribution() {
        return osDistribution;
    }

    public Date getLastServerStart() {
        return lastServerStart;
    }

    public Date getLastServerStop() {
        return lastServerStop;
    }

    public Date getLastBoot() {
        return lastBoot;
    }

    public String getServiceIp() {
        return serviceIp;
    }

    public void setId(String id) {
        this.id = id;
    }

    public void setName(String name) {
        this.name = name;
    }

    public void setState(State state) {
        this.state = state;
    }

    public void setVersion(String version) {
        this.version = version;
    }

    public void setJavaDistribution(String javaDistribution) {
        this.javaDistribution = javaDistribution;
    }

    public void setJavaVersion(String javaVersion) {
        this.javaVersion = javaVersion;
    }

    public void setOsDistribution(String osDistribution) {
        this.osDistribution = osDistribution;
    }

    public void setLastServerStart(Date lastServerStart) {
        this.lastServerStart = lastServerStart;
    }

    public void setLastServerStop(Date lastServerStop) {
        this.lastServerStop = lastServerStop;
    }

    public void setLastBoot(Date lastBoot) {
        this.lastBoot = lastBoot;
    }

    public void setKernelVersion(String kernelVersion) {
        this.kernelVersion = kernelVersion;
    }

    public void setServiceIp(String serviceIp) {
        this.serviceIp = serviceIp;
    }
}
