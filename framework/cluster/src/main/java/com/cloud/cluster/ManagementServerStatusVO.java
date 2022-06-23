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
package com.cloud.cluster;

import com.cloud.utils.db.GenericDao;
import org.apache.cloudstack.management.ManagementServerStatus;
import org.apache.commons.lang3.builder.ReflectionToStringBuilder;
import org.apache.commons.lang3.builder.ToStringStyle;

import javax.persistence.Column;
import javax.persistence.Entity;
import javax.persistence.GeneratedValue;
import javax.persistence.GenerationType;
import javax.persistence.Id;
import javax.persistence.Table;
import javax.persistence.Temporal;
import javax.persistence.TemporalType;
import java.util.Date;

@Entity
@Table(name = "mshost_status")
public class ManagementServerStatusVO implements ManagementServerStatus {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "id")
    private long id;

    @Column(name = "ms_id", nullable = false)
    private String msId;

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

    @Temporal(TemporalType.TIMESTAMP)
    @Column(name = "updated")
    private Date updated;

    @Column(name = GenericDao.CREATED_COLUMN)
    private Date created;

    @Column(name = GenericDao.REMOVED_COLUMN)
    private Date removed;


    public ManagementServerStatusVO() {
    }

    @Override
    public long getId() {
        return id;
    }

    public void setId(long id) {
        this.id = id;
    }

    @Override
    public String getMsId() {
        return msId;
    }

    public void setMsId(String msId) {
        this.msId = msId;
    }

    @Override
    public Date getLastJvmStart() {
        return lastJvmStart;
    }

    public void setLastJvmStart(Date lastJvmStart) {
        this.lastJvmStart = lastJvmStart;
    }

    @Override
    public Date getLastJvmStop() {
        return lastJvmStop;
    }

    public void setLastJvmStop(Date lastJvmStop) {
        this.lastJvmStop = lastJvmStop;
    }

    @Override
    public Date getLastSystemBoot() {
        return lastSystemBoot;
    }

    public void setLastSystemBoot(Date lastSystemBoot) {
        this.lastSystemBoot = lastSystemBoot;
    }

    @Override
    public String getOsDistribution() {
        return osDistribution;
    }

    public void setOsDistribution(String osDistribution) {
        this.osDistribution = osDistribution;
    }

    @Override
    public String getJavaName() {
        return javaName;
    }

    public void setJavaName(String javaName) {
        this.javaName = javaName;
    }

    @Override
    public String getJavaVersion() {
        return javaVersion;
    }

    public void setJavaVersion(String javaVersion) {
        this.javaVersion = javaVersion;
    }

    @Override
    public Date getUpdated() {
        return updated;
    }

    public void setUpdated(Date updated) {
        this.updated = updated;
    }

    @Override
    public Date getCreated() {
        return created;
    }

    public void setCreated(Date created) {
        this.created = created;
    }

    @Override
    public Date getRemoved() {
        return removed;
    }

    public void setRemoved(Date removedTime) {
        removed = removedTime;
    }

    @Override
    public String toString() {
        return new ReflectionToStringBuilder(this, ToStringStyle.SIMPLE_STYLE).toString();
    }
}
