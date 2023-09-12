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
package org.apache.cloudstack.network.tungsten.api.response;

import com.cloud.dc.DataCenter;
import com.cloud.serializer.Param;
import com.google.gson.annotations.SerializedName;
import org.apache.cloudstack.api.ApiConstants;
import org.apache.cloudstack.api.BaseResponse;
import org.apache.cloudstack.network.tungsten.dao.TungstenFabricLBHealthMonitorVO;

public class TungstenFabricLBHealthMonitorResponse extends BaseResponse {
    @SerializedName("lbruleid")
    @Param(description = "the LB rule ID")
    private String lbRuleId;

    @SerializedName("id")
    @Param(description = "the health monitor ID")
    private long id;

    @SerializedName("uuid")
    @Param(description = "the health monitor UUID")
    private String uuid;

    @SerializedName("type")
    @Param(description = "the health monitor type")
    private String type;

    @SerializedName("retry")
    @Param(description = "the health monitor retry")
    private int retry;

    @SerializedName("timeout")
    @Param(description = "the health monitor timeout")
    private int timeout;

    @SerializedName("interval")
    @Param(description = "the health monitor interval")
    private int interval;

    @SerializedName("httpmethod")
    @Param(description = "the health monitor http method")
    private String httpMethod;

    @SerializedName("expectedcode")
    @Param(description = "the health monitor expected code")
    private String expectedCode;

    @SerializedName("urlpath")
    @Param(description = "the health monitor url path")
    private String urlPath;

    @SerializedName(ApiConstants.ZONE_ID)
    @Param(description = "Tungsten-Fabric provider zone id")
    private long zoneId;

    @SerializedName(ApiConstants.ZONE_NAME)
    @Param(description = "Tungsten-Fabric provider zone name")
    private String zoneName;

    public TungstenFabricLBHealthMonitorResponse(TungstenFabricLBHealthMonitorVO tungstenFabricLBHealthMonitorVO, DataCenter zone) {
        this.id = tungstenFabricLBHealthMonitorVO.getId();
        this.uuid = tungstenFabricLBHealthMonitorVO.getUuid();
        this.type = tungstenFabricLBHealthMonitorVO.getType();
        this.retry = tungstenFabricLBHealthMonitorVO.getRetry();
        this.timeout = tungstenFabricLBHealthMonitorVO.getTimeout();
        this.interval = tungstenFabricLBHealthMonitorVO.getInterval();
        this.httpMethod = tungstenFabricLBHealthMonitorVO.getHttpMethod();
        this.expectedCode = tungstenFabricLBHealthMonitorVO.getExpectedCode();
        this.urlPath = tungstenFabricLBHealthMonitorVO.getUrlPath();
        this.zoneId = zone.getId();
        this.zoneName = zone.getName();
        this.setObjectName("healthmonitor");
    }

    public String getLbRuleId() {
        return lbRuleId;
    }

    public void setLbRuleId(final String lbRuleId) {
        this.lbRuleId = lbRuleId;
    }

    public long getId() {
        return id;
    }

    public void setId(final long id) {
        this.id = id;
    }

    public String getUuid() {
        return uuid;
    }

    public void setUuid(final String uuid) {
        this.uuid = uuid;
    }

    public String getType() {
        return type;
    }

    public void setType(final String type) {
        this.type = type;
    }

    public String getHttpMethod() {
        return httpMethod;
    }

    public void setHttpMethod(final String httpMethod) {
        this.httpMethod = httpMethod;
    }

    public int getRetry() {
        return retry;
    }

    public void setRetry(final int retry) {
        this.retry = retry;
    }

    public int getTimeout() {
        return timeout;
    }

    public void setTimeout(final int timeout) {
        this.timeout = timeout;
    }

    public int getInterval() {
        return interval;
    }

    public void setInterval(final int interval) {
        this.interval = interval;
    }

    public String getExpectedCode() {
        return expectedCode;
    }

    public void setExpectedCode(final String expectedCode) {
        this.expectedCode = expectedCode;
    }

    public String getUrlPath() {
        return urlPath;
    }

    public void setUrlPath(final String urlPath) {
        this.urlPath = urlPath;
    }

    public long getZoneId() {
        return zoneId;
    }

    public void setZoneId(final long zoneId) {
        this.zoneId = zoneId;
    }

    public String getZoneName() {
        return zoneName;
    }

    public void setZoneName(final String zoneName) {
        this.zoneName = zoneName;
    }
}
