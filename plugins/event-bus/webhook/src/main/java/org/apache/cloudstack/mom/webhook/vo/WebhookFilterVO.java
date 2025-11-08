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
import javax.persistence.EnumType;
import javax.persistence.Enumerated;
import javax.persistence.GeneratedValue;
import javax.persistence.GenerationType;
import javax.persistence.Id;
import javax.persistence.Table;

import org.apache.cloudstack.mom.webhook.WebhookFilter;
import org.apache.cloudstack.utils.reflectiontostringbuilderutils.ReflectionToStringBuilderUtils;

import com.cloud.utils.db.GenericDao;

@Entity
@Table(name = "webhook_filter")
public class WebhookFilterVO implements WebhookFilter {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "id", updatable = false, nullable = false)
    private Long id;

    @Column(name = "uuid")
    private String uuid;

    @Column(name = "webhook_id", nullable = false)
    private Long webhookId;

    @Column(name = "type", length = 20)
    @Enumerated(value = EnumType.STRING)
    private Type type;

    @Column(name = "mode", length = 20)
    @Enumerated(value = EnumType.STRING)
    private Mode mode;

    @Column(name = "match_type", length = 20)
    @Enumerated(value = EnumType.STRING)
    private MatchType matchType;

    @Column(name = "value", nullable = false, length = 128)
    private String value;

    @Column(name = GenericDao.CREATED_COLUMN)
    private Date created;

    public WebhookFilterVO() {
        this.uuid = UUID.randomUUID().toString();
    }

    public WebhookFilterVO(Long webhookId, Type type, Mode mode, MatchType matchType, String value) {
        this.uuid = UUID.randomUUID().toString();
        this.webhookId = webhookId;
        this.type = type;
        this.mode = mode;
        this.matchType = matchType;
        this.value = value;
    }

    @Override
    public long getId() {
        return id;
    }

    public void setId(Long id) {
        this.id = id;
    }

    @Override
    public String getUuid() {
        return uuid;
    }

    @Override
    public long getWebhookId() {
        return webhookId;
    }

    public void setWebhookId(Long webhookId) {
        this.webhookId = webhookId;
    }

    @Override
    public Type getType() {
        return type;
    }

    public void setType(Type type) {
        this.type = type;
    }

    @Override
    public Mode getMode() {
        return mode;
    }

    public void setMode(Mode mode) {
        this.mode = mode;
    }

    @Override
    public MatchType getMatchType() {
        return matchType;
    }

    public void setMatchType(MatchType matchType) {
        this.matchType = matchType;
    }

    @Override
    public String getValue() {
        return value;
    }

    public void setValue(String value) {
        this.value = value;
    }

    @Override
    public Date getCreated() {
        return created;
    }

    public void setCreated(Date created) {
        this.created = created;
    }

    @Override
    public String toString() {
        return String.format("WebhookFilter %s", ReflectionToStringBuilderUtils.reflectOnlySelectedFields(
                this, "id", "uuid", "webhook_id", "type", "mode", "match_type", "value"));
    }
}
