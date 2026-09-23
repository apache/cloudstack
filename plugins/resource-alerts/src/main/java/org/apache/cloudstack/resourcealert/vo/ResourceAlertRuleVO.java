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

package org.apache.cloudstack.resourcealert.vo;

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
import javax.persistence.Temporal;
import javax.persistence.TemporalType;

import org.apache.cloudstack.resourcealert.AlertCondition;
import org.apache.cloudstack.resourcealert.AlertSeverity;
import org.apache.cloudstack.resourcealert.ResourceAlertRule;

import com.cloud.utils.db.GenericDao;

@Entity
@Table(name = "resource_alert_rules")
public class ResourceAlertRuleVO implements ResourceAlertRule {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "id")
    private long id;

    @Column(name = "uuid")
    private String uuid;

    @Column(name = "name")
    private String name;

    @Column(name = "resource_type")
    @Enumerated(value = EnumType.STRING)
    private ResourceType resourceType;

    @Column(name = "resource_id")
    private Long resourceId;

    @Column(name = "account_id")
    private long accountId;

    @Column(name = "domain_id")
    private long domainId;

    @Column(name = "metric")
    private String metric;

    @Column(name = "condition_operator")
    @Enumerated(value = EnumType.STRING)
    private AlertCondition condition;

    @Column(name = "threshold")
    private double threshold;

    @Column(name = "severity")
    @Enumerated(value = EnumType.STRING)
    private AlertSeverity severity;

    @Column(name = "message", length = 4096)
    private String message;

    @Column(name = "email")
    private boolean email;

    @Column(name = "reset_interval")
    private int resetInterval;

    @Column(name = GenericDao.CREATED_COLUMN)
    private Date created;

    @Column(name = "updated")
    @Temporal(value = TemporalType.TIMESTAMP)
    private Date updated;

    @Column(name = GenericDao.REMOVED_COLUMN)
    private Date removed;

    public ResourceAlertRuleVO() {
        this.uuid = UUID.randomUUID().toString();
    }

    public ResourceAlertRuleVO(String name, ResourceType resourceType, Long resourceId,
            long accountId, long domainId, String metric, AlertCondition condition,
            double threshold, AlertSeverity severity, String message, boolean email, int resetInterval) {
        this.uuid = UUID.randomUUID().toString();
        this.name = name;
        this.resourceType = resourceType;
        this.resourceId = resourceId;
        this.accountId = accountId;
        this.domainId = domainId;
        this.metric = metric;
        this.condition = condition;
        this.threshold = threshold;
        this.severity = severity;
        this.message = message;
        this.email = email;
        this.resetInterval = resetInterval;
    }

    @Override public long getId() { return id; }
    @Override public String getUuid() { return uuid; }
    @Override public String getName() { return name; }
    @Override public ResourceType getResourceType() { return resourceType; }
    @Override public Long getResourceId() { return resourceId; }
    @Override public long getAccountId() { return accountId; }
    @Override public long getDomainId() { return domainId; }
    @Override public String getMetric() { return metric; }
    @Override public AlertCondition getCondition() { return condition; }
    @Override public double getThreshold() { return threshold; }
    @Override public AlertSeverity getSeverity() { return severity; }
    @Override public String getMessage() { return message; }
    @Override public boolean isEmail() { return email; }
    @Override public int getResetInterval() { return resetInterval; }
    @Override public Date getCreated() { return created; }

    @Override
    public Class<?> getEntityType() {
        return ResourceAlertRule.class;
    }

    public Date getRemoved() { return removed; }
    public Date getUpdated() { return updated; }

    public void setName(String name) { this.name = name; }
    public void setCondition(AlertCondition condition) { this.condition = condition; }
    public void setThreshold(double threshold) { this.threshold = threshold; }
    public void setSeverity(AlertSeverity severity) { this.severity = severity; }
    public void setMessage(String message) { this.message = message; }
    public void setEmail(boolean email) { this.email = email; }
    public void setResetInterval(int resetInterval) { this.resetInterval = resetInterval; }
    public void setUpdated(Date updated) { this.updated = updated; }
    public void setRemoved(Date removed) { this.removed = removed; }
}
