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
package com.cloud.api.query.vo;

import com.cloud.utils.db.GenericDao;
import org.apache.cloudstack.management.ManagementServerHost;
import org.apache.cloudstack.management.ManagementServerHost.State;

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
import java.util.Date;

/**
 * The Value object for api response utility view for management server queries
 */
@Entity
@Table(name = "mshost_view")
public class ManagementServerJoinVO {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "id")
    private long id;

    @Column(name = "uuid")
    private String uuid;

    @Column(name = "msid", updatable = true, nullable = false)
    private long msid;

    @Column(name = "runid", updatable = true, nullable = false)
    private long runid;

    @Column(name = "name", updatable = true, nullable = true)
    private String name;

    @Column(name = "state", updatable = true, nullable = false)
    @Enumerated(value = EnumType.STRING)
    private ManagementServerHost.State state;

    @Column(name = "version", updatable = true, nullable = true)
    private String version;

    @Column(name = "service_ip", updatable = true, nullable = false)
    private String serviceIP;

    @Column(name = "service_port", updatable = true, nullable = false)
    private int servicePort;

    @Temporal(TemporalType.TIMESTAMP)
    @Column(name = "last_update", updatable = true, nullable = true)
    private Date lastUpdateTime;

    @Column(name = GenericDao.REMOVED_COLUMN)
    private Date removed;

    @Column(name = "alert_count", updatable = true, nullable = false)
    private int alertCount;

    @Temporal(TemporalType.TIMESTAMP)
    @Column(name="last_jvm_start")
    private Date lastJvmStart;

    @Temporal(TemporalType.TIMESTAMP)
    @Column(name="last_jvm_stop")
    private Date lastJvmStop;

    @Temporal(TemporalType.TIMESTAMP)
    @Column(name="last_system_boot")
    private Date lastSystemBoot;

    @Column(name="os_distribution")
    private String osDistribution;

    @Column(name="java_name")
    private String javaName;

    @Column(name="java_version")
    private String javaVersion;

    public long getId() {
        return id;
    }

    public String getUuid() {
        return uuid;
    }

    public long getMsid() {
        return msid;
    }

    public long getRunid() {
        return runid;
    }

    public String getName() {
        return name;
    }

    public State getState() {
        return state;
    }

    public String getVersion() {
        return version;
    }

    public String getServiceIP() {
        return serviceIP;
    }

    public int getServicePort() {
        return servicePort;
    }

    public Date getLastUpdateTime() {
        return lastUpdateTime;
    }

    public Date getRemoved() {
        return removed;
    }

    public int getAlertCount() {
        return alertCount;
    }

    public Date getLastJvmStart() {
        return lastJvmStart;
    }

    public Date getLastJvmStop() {
        return lastJvmStop;
    }

    public Date getLastSystemBoot() {
        return lastSystemBoot;
    }

    public String getOsDistribution() {
        return osDistribution;
    }

    public String getJavaName() {
        return javaName;
    }

    public String getJavaVersion() {
        return javaVersion;
    }
}
