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
package com.cloud.vm;

import java.util.Date;

import javax.persistence.Column;
import javax.persistence.Entity;
import javax.persistence.Id;
import javax.persistence.Table;
import javax.persistence.Temporal;
import javax.persistence.TemporalType;

import com.cloud.utils.db.GenericDao;

@Entity
@Table(name="op_it_work")
public class ItWorkVO {
    enum Type {
        Start,
        Cleanup;
    }
    
    enum ResourceType {
        Volume,
        Nic
    }
    
    enum Step {
        Prepare,
        Start,
        Started,
    }
    
    @Id
    @Column(name="id")
    String id;
    
    @Column(name=GenericDao.CREATED_COLUMN)
    Date created;
    
    @Column(name="mgmt_server_id")
    long managementServerId;
    
    @Column(name="type")
    Type type;
    
    @Column(name="thread")
    String threadName;
    
    @Column(name="state")
    Step step;
    
    @Column(name="cancel_taken")
    @Temporal(value=TemporalType.TIMESTAMP)
    Date taken;
    
    @Column(name="instance_id")
    long instanceId;
    
    public long getInstanceId() {
        return instanceId;
    }

    @Column(name="resource_id")
    long resourceId;
    
    @Column(name="resource_type")
    ResourceType resourceType;
    
    
    public long getResourceId() {
        return resourceId;
    }

    public void setResourceId(long resourceId) {
        this.resourceId = resourceId;
    }

    public ResourceType getResourceType() {
        return resourceType;
    }

    public void setResourceType(ResourceType resourceType) {
        this.resourceType = resourceType;
    }

    protected ItWorkVO() {
    }
    
    protected ItWorkVO(String id, long managementServerId, Type type, long instanceId) {
        this.id = id;
        this.managementServerId = managementServerId;
        this.type = type;
        this.threadName = Thread.currentThread().getName();
        this.step = Step.Prepare;
        this.instanceId = instanceId;
        this.resourceType = null;
    }

    public String getId() {
        return id;
    }

    public Date getCreated() {
        return created;
    }

    public long getManagementServerId() {
        return managementServerId;
    }
    
    public Type getType() {
        return type;
    }
    
    public void setType(Type type) {
        this.type = type;
    }
    
    public String getThreadName() {
        return threadName;
    }
    
    public Step getStep() {
        return step;
    }
    
    public void setStep(Step state) {
        this.step = state;
    }
    
    public Date getTaken() {
        return taken;
    }
}
