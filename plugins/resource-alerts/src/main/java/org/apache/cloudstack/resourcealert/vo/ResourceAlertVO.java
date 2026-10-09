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

import org.apache.cloudstack.resourcealert.AlertSeverity;
import org.apache.cloudstack.resourcealert.ResourceAlert;

@Entity
@Table(name = "resource_alerts")
public class ResourceAlertVO implements ResourceAlert {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "id")
    private long id;

    @Column(name = "uuid")
    private String uuid;

    @Column(name = "alert_rule_id")
    private long alertRuleId;

    @Column(name = "resource_id")
    private Long resourceId;

    @Column(name = "metric_type")
    private String metricType;

    @Column(name = "metric_value")
    private double metricValue;

    @Column(name = "severity")
    @Enumerated(value = EnumType.STRING)
    private AlertSeverity severity;

    @Column(name = "message", length = 4096)
    private String message;

    @Column(name = "alert_timestamp")
    @Temporal(value = TemporalType.TIMESTAMP)
    private Date alertTimestamp;

    public ResourceAlertVO() {
        this.uuid = UUID.randomUUID().toString();
    }

    public ResourceAlertVO(long alertRuleId, Long resourceId, String metricType,
            double metricValue, AlertSeverity severity, String message, Date alertTimestamp) {
        this.uuid = UUID.randomUUID().toString();
        this.alertRuleId = alertRuleId;
        this.resourceId = resourceId;
        this.metricType = metricType;
        this.metricValue = metricValue;
        this.severity = severity;
        this.message = message;
        this.alertTimestamp = alertTimestamp;
    }

    @Override public long getId() { return id; }
    @Override public String getUuid() { return uuid; }
    @Override public long getAlertRuleId() { return alertRuleId; }
    @Override public Long getResourceId() { return resourceId; }
    @Override public String getMetricType() { return metricType; }
    @Override public double getMetricValue() { return metricValue; }
    @Override public AlertSeverity getSeverity() { return severity; }
    @Override public String getMessage() { return message; }
    @Override public Date getAlertTimestamp() { return alertTimestamp; }
}
