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

package org.apache.cloudstack.mom.webhook.vo;


import java.util.Date;

import javax.persistence.Column;
import javax.persistence.Entity;
import javax.persistence.GeneratedValue;
import javax.persistence.GenerationType;
import javax.persistence.Id;
import javax.persistence.Table;
import javax.persistence.Temporal;
import javax.persistence.TemporalType;

import org.apache.cloudstack.api.Identity;
import org.apache.cloudstack.api.InternalIdentity;
import org.apache.cloudstack.utils.reflectiontostringbuilderutils.ReflectionToStringBuilderUtils;

import com.cloud.api.query.vo.BaseViewVO;

@Entity
@Table(name = "webhook_delivery_view")
public class WebhookDeliveryJoinVO extends BaseViewVO implements InternalIdentity, Identity {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "id")
    private long id;

    @Column(name = "uuid")
    private String uuid;

    @Column(name = "event_id")
    private long eventId;

    @Column(name = "event_uuid")
    private String eventUuid;

    @Column(name = "event_type")
    private String eventType;

    @Column(name = "webhook_id")
    private long webhookId;

    @Column(name = "webhook_uuid")
    private String webhookUuId;

    @Column(name = "webhook_name")
    private String webhookName;

    @Column(name = "mshost_id")
    private long managementServerId;

    @Column(name = "mshost_uuid")
    private String managementServerUuId;

    @Column(name = "mshost_msid")
    private long managementServerMsId;

    @Column(name = "mshost_name")
    private String managementServerName;

    @Column(name = "headers", length = 65535)
    private String headers;

    @Column(name = "payload", length = 65535)
    private String payload;

    @Column(name = "success")
    private boolean success;

    @Column(name = "response", length = 65535)
    private String response;

    @Column(name = "start_time")
    @Temporal(value = TemporalType.TIMESTAMP)
    private Date startTime;

    @Column(name = "end_time")
    @Temporal(value = TemporalType.TIMESTAMP)
    private Date endTime;

    @Override
    public long getId() {
        return 0;
    }

    @Override
    public String getUuid() {
        return uuid;
    }

    public long getEventId() {
        return eventId;
    }

    public String getEventUuid() {
        return eventUuid;
    }

    public String getEventType() {
        return eventType;
    }

    public long getWebhookId() {
        return webhookId;
    }

    public String getWebhookUuId() {
        return webhookUuId;
    }

    public String getWebhookName() {
        return webhookName;
    }

    public long getManagementServerId() {
        return managementServerId;
    }

    public String getManagementServerUuId() {
        return managementServerUuId;
    }

    public long getManagementServerMsId() {
        return managementServerMsId;
    }

    public String getManagementServerName() {
        return managementServerName;
    }

    public String getHeaders() {
        return headers;
    }

    public String getPayload() {
        return payload;
    }

    public boolean isSuccess() {
        return success;
    }

    public String getResponse() {
        return response;
    }

    public Date getStartTime() {
        return startTime;
    }

    public Date getEndTime() {
        return endTime;
    }

    @Override
    public String toString() {
        return String.format("WebhookDelivery [%s]", ReflectionToStringBuilderUtils.reflectOnlySelectedFields(
                this, "id", "uuid", "webhookId", "startTime", "success"));
    }

    public WebhookDeliveryJoinVO() {
    }
}
