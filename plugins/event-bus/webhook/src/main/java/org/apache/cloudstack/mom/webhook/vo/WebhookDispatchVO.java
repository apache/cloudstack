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
import java.util.UUID;

import javax.persistence.Column;
import javax.persistence.Entity;
import javax.persistence.GeneratedValue;
import javax.persistence.GenerationType;
import javax.persistence.Id;
import javax.persistence.Table;
import javax.persistence.Temporal;
import javax.persistence.TemporalType;

import org.apache.cloudstack.mom.webhook.WebhookDispatch;
import org.apache.cloudstack.mom.webhook.WebhookRule;
import org.apache.cloudstack.utils.reflectiontostringbuilderutils.ReflectionToStringBuilderUtils;

@Entity
@Table(name = "webhook_dispatch")
public class WebhookDispatchVO implements WebhookDispatch {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "id")
    private long id;

    @Column(name = "uuid")
    private String uuid;

    @Column(name = "event_id")
    private long eventId;

    @Column(name = "webhook_id")
    private long webhookRuleId;

    @Column(name = "mshost_msid")
    private long mangementServerId;

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
        return id;
    }

    @Override
    public String getUuid() {
        return uuid;
    }

    @Override
    public long getEventId() {
        return eventId;
    }

    @Override
    public long getWebhookRuleId() {
        return webhookRuleId;
    }

    @Override
    public long getManagementServerId() {
        return mangementServerId;
    }

    @Override
    public String getPayload() {
        return payload;
    }

    @Override
    public boolean isSuccess() {
        return success;
    }

    @Override
    public String getResponse() {
        return response;
    }

    @Override
    public Date getStartTime() {
        return startTime;
    }

    @Override
    public Date getEndTime() {
        return endTime;
    }

    @Override
    public String toString() {
        return String.format("WebhookDispatch [%s]", ReflectionToStringBuilderUtils.reflectOnlySelectedFields(this, "id", "uuid", "webhookRuleId", "endTime", "success"));
    }

    public WebhookDispatchVO() {
        this.uuid = UUID.randomUUID().toString();
    }

    public WebhookDispatchVO(long eventId, long webhookRuleId, long managementServerId, String payload, boolean success,
                             String response, Date startTime, Date endTime) {
        this.uuid = UUID.randomUUID().toString();
        this.eventId = eventId;
        this.webhookRuleId = webhookRuleId;
        this.mangementServerId = managementServerId;
        this.payload = payload;
        this.success = success;
        this.response = response;
        this.startTime = startTime;
        this.endTime = endTime;
    }



    /*
     * For creating a dummy object for testing dispatch
     */
    public WebhookDispatchVO(long managementServerId, String payload, boolean success,
                             String response, Date startTime, Date endTime) {
        this.id = WebhookRule.ID_DUMMY_RULE;
        this.uuid = UUID.randomUUID().toString();
        this.eventId = WebhookRule.ID_DUMMY_RULE;
        this.webhookRuleId = WebhookRule.ID_DUMMY_RULE;
        this.mangementServerId = managementServerId;
        this.payload = payload;
        this.success = success;
        this.response = response;
        this.startTime = startTime;
        this.endTime = endTime;
    }
}
