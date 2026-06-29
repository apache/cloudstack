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

package org.apache.cloudstack.resourcealert.api.response;

import java.util.Date;

import org.apache.cloudstack.api.ApiConstants;
import org.apache.cloudstack.api.BaseResponse;
import org.apache.cloudstack.api.EntityReference;
import org.apache.cloudstack.resourcealert.ResourceAlert;

import com.cloud.serializer.Param;
import com.google.gson.annotations.SerializedName;

@EntityReference(value = {ResourceAlert.class})
public class ResourceAlertResponse extends BaseResponse {

    @SerializedName(ApiConstants.ID)
    @Param(description = "the ID of the fired alert")
    private String id;

    @SerializedName("alertruleid")
    @Param(description = "the ID of the rule that triggered this alert")
    private String alertRuleId;

    @SerializedName("resourceid")
    @Param(description = "the ID of the resource that triggered this alert")
    private String resourceId;

    @SerializedName("metrictype")
    @Param(description = "the metric that crossed the threshold")
    private String metricType;

    @SerializedName("metricvalue")
    @Param(description = "the observed metric value at the time of firing")
    private double metricValue;

    @SerializedName("severity")
    @Param(description = "the severity of the alert")
    private String severity;

    @SerializedName(ApiConstants.MESSAGE)
    @Param(description = "the alert message")
    private String message;

    @SerializedName("alerttimestamp")
    @Param(description = "the time the alert was fired")
    private Date alertTimestamp;

    public void setId(String id) { this.id = id; }
    public void setAlertRuleId(String alertRuleId) { this.alertRuleId = alertRuleId; }
    public void setResourceId(String resourceId) { this.resourceId = resourceId; }
    public void setMetricType(String metricType) { this.metricType = metricType; }
    public void setMetricValue(double metricValue) { this.metricValue = metricValue; }
    public void setSeverity(String severity) { this.severity = severity; }
    public void setMessage(String message) { this.message = message; }
    public void setAlertTimestamp(Date alertTimestamp) { this.alertTimestamp = alertTimestamp; }
}
