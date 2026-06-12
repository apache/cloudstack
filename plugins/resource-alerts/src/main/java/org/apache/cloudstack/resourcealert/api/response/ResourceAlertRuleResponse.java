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
import org.apache.cloudstack.resourcealert.ResourceAlertRule;

import com.cloud.serializer.Param;
import com.google.gson.annotations.SerializedName;

@EntityReference(value = {ResourceAlertRule.class})
public class ResourceAlertRuleResponse extends BaseResponse {

    @SerializedName(ApiConstants.ID)
    @Param(description = "the ID of the alert rule")
    private String id;

    @SerializedName(ApiConstants.NAME)
    @Param(description = "the name of the alert rule")
    private String name;

    @SerializedName("resourcetype")
    @Param(description = "the type of resource this rule monitors")
    private String resourceType;

    @SerializedName("resourceid")
    @Param(description = "the specific resource ID; absent for generic rules")
    private String resourceId;

    @SerializedName("metric")
    @Param(description = "the metric being monitored")
    private String metric;

    @SerializedName("condition")
    @Param(description = "the comparison operator (GT, GTE, LT, LTE, EQ)")
    private String condition;

    @SerializedName("threshold")
    @Param(description = "the threshold value that triggers this rule")
    private double threshold;

    @SerializedName("severity")
    @Param(description = "the severity of the alert (CRITICAL, HIGH, MEDIUM, LOW)")
    private String severity;

    @SerializedName(ApiConstants.MESSAGE)
    @Param(description = "the message sent with the alert")
    private String message;

    @SerializedName("email")
    @Param(description = "whether email notification is enabled for this rule")
    private boolean email;

    @SerializedName("resetinterval")
    @Param(description = "minimum seconds between repeat firings of this rule")
    private int resetInterval;

    @SerializedName(ApiConstants.ACCOUNT)
    @Param(description = "the account that owns this rule")
    private String accountName;

    @SerializedName(ApiConstants.DOMAIN_ID)
    @Param(description = "the ID of the domain this rule belongs to")
    private String domainId;

    @SerializedName(ApiConstants.DOMAIN)
    @Param(description = "the name of the domain this rule belongs to")
    private String domainName;

    @SerializedName(ApiConstants.CREATED)
    @Param(description = "the date this rule was created")
    private Date created;

    public void setId(String id) { this.id = id; }
    public void setName(String name) { this.name = name; }
    public void setResourceType(String resourceType) { this.resourceType = resourceType; }
    public void setResourceId(String resourceId) { this.resourceId = resourceId; }
    public void setMetric(String metric) { this.metric = metric; }
    public void setCondition(String condition) { this.condition = condition; }
    public void setThreshold(double threshold) { this.threshold = threshold; }
    public void setSeverity(String severity) { this.severity = severity; }
    public void setMessage(String message) { this.message = message; }
    public void setEmail(boolean email) { this.email = email; }
    public void setResetInterval(int resetInterval) { this.resetInterval = resetInterval; }
    public void setAccountName(String accountName) { this.accountName = accountName; }
    public void setDomainId(String domainId) { this.domainId = domainId; }
    public void setDomainName(String domainName) { this.domainName = domainName; }
    public void setCreated(Date created) { this.created = created; }
}
