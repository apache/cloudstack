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
package com.cloud.ha;

import java.util.Date;

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

import org.apache.cloudstack.api.InternalIdentity;

import com.cloud.ha.HighAvailabilityManager.Step;
import com.cloud.ha.HighAvailabilityManager.WorkType;
import com.cloud.utils.db.GenericDao;
import com.cloud.vm.VirtualMachine;
import com.cloud.vm.VirtualMachine.State;

@Entity
@Table(name = "op_ha_work")
public class HaWorkVO implements InternalIdentity {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "id")
    private Long id;

    @Column(name = "instance_id", updatable = false, nullable = false)
    private long instanceId;    // vm_instance id

    @Column(name = "mgmt_server_id", nullable = true)
    private Long serverId;

    @Column(name = GenericDao.CREATED_COLUMN)
    private Date created;

    @Column(name = "state", nullable = false)
    @Enumerated(value = EnumType.STRING)
    private State previousState;

    @Column(name = "host_id", nullable = false)
    private long hostId;

    @Column(name = "taken", nullable = true)
    @Temporal(value = TemporalType.TIMESTAMP)
    private Date dateTaken;

    @Column(name = "time_to_try", nullable = true)
    private long timeToTry;

    @Column(name = "type", updatable = false, nullable = false)
    @Enumerated(value = EnumType.STRING)
    private WorkType workType;

    @Column(name = "updated")
    private long updateTime;

    @Column(name = "step", nullable = false)
    @Enumerated(value = EnumType.STRING)
    private HighAvailabilityManager.Step step;

    @Column(name = "vm_type", updatable = false, nullable = false)
    @Enumerated(value = EnumType.STRING)
    private VirtualMachine.Type type;

    @Column(name = "tried")
    int timesTried;

    protected HaWorkVO() {
    }

    @Override
    public long getId() {
        return id;
    }

    public long getInstanceId() {
        return instanceId;
    }

    public WorkType getWorkType() {
        return workType;
    }

    public void setStep(final HighAvailabilityManager.Step step) {
        this.step = step;
    }

    public Long getServerId() {
        return serverId;
    }

    public VirtualMachine.Type getType() {
        return type;
    }

    public void setServerId(final Long serverId) {
        this.serverId = serverId;
    }

    public Date getCreated() {
        return created;
    }

    public void setHostId(final long hostId) {
        this.hostId = hostId;
    }

    public HighAvailabilityManager.Step getStep() {
        return step;
    }

    public State getPreviousState() {
        return previousState;
    }

    public Date getDateTaken() {
        return dateTaken;
    }

    public long getHostId() {
        return hostId;
    }

    public void setDateTaken(final Date taken) {
        this.dateTaken = taken;
    }

    public void setTimesTried(final int time) {
        timesTried = time;
    }

    public boolean canScheduleNew(final long interval) {
        return (timeToTry + interval) < (System.currentTimeMillis() >> 10);
    }

    public int getTimesTried() {
        return timesTried;
    }

    public long getUpdateTime() {
        return updateTime;
    }

    public void setUpdateTime(long time) {
        updateTime = time;
    }

    public long getTimeToTry() {
        return timeToTry;
    }

    public void setTimeToTry(final long timeToTry) {
        this.timeToTry = timeToTry;
    }

    public void setPreviousState(State state) {
        this.previousState = state;
    }

    public HaWorkVO(final long instanceId, final VirtualMachine.Type type, final WorkType workType, final Step step, final long hostId, final State previousState,
            final int timesTried, final long updated) {
        this.workType = workType;
        this.type = type;
        this.instanceId = instanceId;
        this.serverId = null;
        this.hostId = hostId;
        this.previousState = previousState;
        this.dateTaken = null;
        this.timesTried = timesTried;
        this.step = step;
        this.timeToTry = System.currentTimeMillis() >> 10;
        this.updateTime = updated;
    }

    @Override
    public String toString() {
        return new StringBuilder("HAWork[").append(id)
            .append("-")
            .append(workType)
            .append("-")
            .append(instanceId)
            .append("-")
            .append(previousState)
            .append("-")
            .append(step)
            .append("]")
            .toString();
    }
}
