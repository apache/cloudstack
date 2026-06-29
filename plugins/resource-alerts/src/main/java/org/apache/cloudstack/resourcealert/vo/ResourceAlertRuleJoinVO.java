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

import javax.persistence.Column;
import javax.persistence.Entity;
import javax.persistence.EnumType;
import javax.persistence.Enumerated;
import javax.persistence.Id;
import javax.persistence.Table;
import javax.persistence.Temporal;
import javax.persistence.TemporalType;

import org.apache.cloudstack.resourcealert.AlertCondition;
import org.apache.cloudstack.resourcealert.AlertSeverity;
import org.apache.cloudstack.resourcealert.ResourceAlertRule;

import com.cloud.user.Account;

@Entity
@Table(name = "resource_alert_rule_view")
public class ResourceAlertRuleJoinVO {

    @Id
    @Column(name = "id", updatable = false, nullable = false)
    private long id;

    @Column(name = "uuid")
    private String uuid;

    @Column(name = "name")
    private String name;

    @Column(name = "resource_type")
    @Enumerated(value = EnumType.STRING)
    private ResourceAlertRule.ResourceType resourceType;

    @Column(name = "resource_id")
    private Long resourceId;

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

    @Column(name = "created")
    private Date created;

    @Column(name = "updated")
    @Temporal(value = TemporalType.TIMESTAMP)
    private Date updated;

    @Column(name = "removed")
    private Date removed;

    @Column(name = "account_id")
    private long accountId;

    @Column(name = "account_uuid")
    private String accountUuid;

    @Column(name = "account_name")
    private String accountName;

    @Column(name = "account_type")
    @Enumerated(value = EnumType.STRING)
    private Account.Type accountType;

    @Column(name = "domain_id")
    private long domainId;

    @Column(name = "domain_uuid")
    private String domainUuid;

    @Column(name = "domain_name")
    private String domainName;

    @Column(name = "domain_path")
    private String domainPath;

    public ResourceAlertRuleJoinVO() {}

    public long getId() { return id; }
    public String getUuid() { return uuid; }
    public String getName() { return name; }
    public ResourceAlertRule.ResourceType getResourceType() { return resourceType; }
    public Long getResourceId() { return resourceId; }
    public String getMetric() { return metric; }
    public AlertCondition getCondition() { return condition; }
    public double getThreshold() { return threshold; }
    public AlertSeverity getSeverity() { return severity; }
    public String getMessage() { return message; }
    public boolean isEmail() { return email; }
    public int getResetInterval() { return resetInterval; }
    public Date getCreated() { return created; }
    public Date getUpdated() { return updated; }
    public Date getRemoved() { return removed; }
    public long getAccountId() { return accountId; }
    public String getAccountUuid() { return accountUuid; }
    public String getAccountName() { return accountName; }
    public Account.Type getAccountType() { return accountType; }
    public long getDomainId() { return domainId; }
    public String getDomainUuid() { return domainUuid; }
    public String getDomainName() { return domainName; }
    public String getDomainPath() { return domainPath; }
}
