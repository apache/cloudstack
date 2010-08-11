/**
 *  Copyright (C) 2010 Cloud.com, Inc.  All rights reserved.
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

import com.cloud.ha.HighAvailabilityManager.Step;
import com.cloud.utils.db.GenericDao;
import com.cloud.vm.State;
import com.cloud.vm.VirtualMachine;

@Entity
@Table(name="op_ha_work")
public class WorkVO {
    public enum WorkType {
        Migration,
        Stop,
        CheckStop,
        Destroy,
        HA;
    }
    
    @Id
    @GeneratedValue(strategy=GenerationType.IDENTITY)
    @Column(name="id")
    private Long id;
    
    @Column(name="instance_id", updatable=false, nullable=false)
    private long instanceId;    // vm_instance id
    
    @Column(name="mgmt_server_id", nullable=true)
    private Long serverId;
    
    @Column(name=GenericDao.CREATED_COLUMN)
    private Date created;
    
    @Column(name="state", nullable=false)
    @Enumerated(value=EnumType.STRING)
    private State previousState;
    
    @Column(name="host_id", nullable=false)
    private long hostId;
    
    @Column(name="taken", nullable=true)
    @Temporal(value=TemporalType.TIMESTAMP)
    private Date dateTaken;
    
    @Column(name="time_to_try", nullable=true)
    private long timeToTry;
    
    @Column(name="type", updatable = false, nullable=false)
    @Enumerated(value=EnumType.STRING)
    private WorkType workType;
    
    @Column(name="updated")
    private long updateTime;
    
    @Column(name="step", nullable = false)
    @Enumerated(value=EnumType.STRING)
    private HighAvailabilityManager.Step step;
    
    @Column(name="vm_type", updatable = false, nullable=false)
    @Enumerated(value=EnumType.STRING)
    private VirtualMachine.Type type;
    
    @Column(name="tried")
    int timesTried;
    
    protected WorkVO() {
    }
    
    public Long getId() {
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
    
    public WorkVO(final long instanceId, final VirtualMachine.Type type, final WorkType workType, final Step step, final long hostId, final State previousState, final int timesTried, final long updated) {
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
    	return new StringBuilder("[HA-Work:id=").append(id).append(":type=").append(workType.toString()).append(":vm=").append(instanceId).append(":state=").append(previousState.toString()).append("]").toString();
    }
}
