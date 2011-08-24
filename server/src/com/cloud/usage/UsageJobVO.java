/**
 * *  Copyright (C) 2011 Citrix Systems, Inc.  All rights reserved
*
 *
 * This software is licensed under the GNU General Public License v3 or later.
 *
 * It is free software: you can redistribute it and/or modify
 * it under the terms of the GNU General Public License as published by
 * the Free Software Foundation, either version 3 of the License, or any later version.
 * This program is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 * GNU General Public License for more details.
 *
 * You should have received a copy of the GNU General Public License
 * along with this program.  If not, see <http://www.gnu.org/licenses/>.
 *
 */

package com.cloud.usage;

import java.util.Date;

import javax.persistence.Column;
import javax.persistence.Entity;
import javax.persistence.GeneratedValue;
import javax.persistence.GenerationType;
import javax.persistence.Id;
import javax.persistence.Table;
import javax.persistence.Temporal;
import javax.persistence.TemporalType;

@Entity
@Table(name="usage_job")
public class UsageJobVO {

    public static final int JOB_TYPE_RECURRING = 0;
    public static final int JOB_TYPE_SINGLE = 1;

    public static final int JOB_NOT_SCHEDULED = 0;
    public static final int JOB_SCHEDULED = 1;

    @Id
    @GeneratedValue(strategy=GenerationType.IDENTITY)
    @Column(name="id")
    private Long id;

    @Column(name="host")
    private String host;

    @Column(name="pid")
    private Integer pid;

    @Column(name="job_type")
    private int jobType;

    @Column(name="scheduled")
    private int scheduled;

    @Column(name="start_millis")
    private long startMillis;

    @Column(name="end_millis")
    private long endMillis;

    @Column(name="exec_time")
    private long execTime;

    @Temporal(TemporalType.TIMESTAMP)
    @Column(name="start_date")
    private Date startDate;

    @Temporal(TemporalType.TIMESTAMP)
    @Column(name="end_date")
    private Date endDate;

    @Column(name="success")
    private Boolean success;

    @Temporal(TemporalType.TIMESTAMP)
    @Column(name="heartbeat")
    private Date heartbeat;

    public UsageJobVO() {}

    public Long getId() {
        return id;
    }

    public void setId(Long id) {
        this.id = id;
    }

    public String getHost() {
        return host;
    }

    public void setHost(String host) {
        this.host = host;
    }

    public Integer getPid() {
        return pid;
    }

    public void setPid(Integer pid) {
        this.pid = pid;
    }

    public int getJobType() {
        return jobType;
    }

    public void setJobType(int jobType) {
        this.jobType = jobType;
    }

    public int getScheduled() {
        return scheduled;
    }

    public void setScheduled(int scheduled) {
        this.scheduled = scheduled;
    }

    public long getStartMillis() {
        return startMillis;
    }

    public void setStartMillis(long startMillis) {
        this.startMillis = startMillis;
    }

    public long getEndMillis() {
        return endMillis;
    }

    public void setEndMillis(long endMillis) {
        this.endMillis = endMillis;
    }

    public long getExecTime() {
        return execTime;
    }

    public void setExecTime(long execTime) {
        this.execTime = execTime;
    }

    public Date getStartDate() {
        return startDate;
    }

    public void setStartDate(Date startDate) {
        this.startDate = startDate;
    }

    public Date getEndDate() {
        return endDate;
    }

    public void setEndDate(Date endDate) {
        this.endDate = endDate;
    }

    public Boolean getSuccess() {
        return success;
    }

    public void setSuccess(Boolean success) {
        this.success = success;
    }

    public Date getHeartbeat() {
        return heartbeat;
    }

    public void setHeartbeat(Date heartbeat) {
        this.heartbeat = heartbeat;
    }
}
