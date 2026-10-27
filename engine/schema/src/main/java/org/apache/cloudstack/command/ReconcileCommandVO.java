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

package org.apache.cloudstack.command;

import java.util.Date;

import javax.persistence.Column;
import javax.persistence.Entity;
import javax.persistence.GeneratedValue;
import javax.persistence.GenerationType;
import javax.persistence.Id;
import javax.persistence.Table;
import javax.persistence.Temporal;
import javax.persistence.TemporalType;

import com.cloud.agent.api.Command;
import com.cloud.utils.db.GenericDao;

import org.apache.cloudstack.acl.InfrastructureEntity;
import org.apache.cloudstack.api.ApiCommandResourceType;
import org.apache.cloudstack.api.InternalIdentity;

@Entity
@Table(name = "reconcile_commands")
public class ReconcileCommandVO implements InfrastructureEntity, InternalIdentity {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "id")
    private long id;

    @Column(name = "management_server_id")
    private long managementServerId;

    @Column(name = "host_id")
    private long hostId;

    @Column(name = "request_sequence")
    private long requestSequence;

    @Column(name = "resource_id")
    private long resourceId;

    @Column(name = "resource_type")
    private ApiCommandResourceType resourceType;

    @Column(name = "state_by_management")
    private Command.State stateByManagement;

    @Column(name = "state_by_agent")
    private Command.State stateByAgent;

    @Column(name = "command_name")
    private String commandName;

    @Column(name = "command_info", length = 65535)
    private String commandInfo;

    @Column(name = "answer_name")
    private String answerName;

    @Column(name = "answer_info", length = 65535)
    private String answerInfo;

    @Column(name = GenericDao.CREATED_COLUMN)
    private Date created;

    @Column(name= GenericDao.REMOVED_COLUMN)
    private Date removed;

    @Temporal(TemporalType.TIMESTAMP)
    @Column(name= "updated")
    private Date updated;

    @Column(name= "retry_count")
    private Long retryCount = 0L;

    @Override
    public long getId() {
        return id;
    }

    public long getManagementServerId() {
        return managementServerId;
    }

    public void setManagementServerId(long managementServerId) {
        this.managementServerId = managementServerId;
    }

    public long getHostId() {
        return hostId;
    }

    public void setHostId(long hostId) {
        this.hostId = hostId;
    }

    public long getRequestSequence() {
        return requestSequence;
    }

    public void setRequestSequence(long requestSequence) {
        this.requestSequence = requestSequence;
    }

    public long getResourceId() {
        return resourceId;
    }

    public void setResourceId(long resourceId) {
        this.resourceId = resourceId;
    }

    public ApiCommandResourceType getResourceType() {
        return resourceType;
    }

    public void setResourceType(ApiCommandResourceType resourceType) {
        this.resourceType = resourceType;
    }

    public Command.State getStateByManagement() {
        return stateByManagement;
    }

    public void setStateByManagement(Command.State stateByManagement) {
        this.stateByManagement = stateByManagement;
    }

    public Command.State getStateByAgent() {
        return stateByAgent;
    }

    public void setStateByAgent(Command.State stateByAgent) {
        this.stateByAgent = stateByAgent;
    }

    public String getCommandName() {
        return commandName;
    }

    public void setCommandName(String commandName) {
        this.commandName = commandName;
    }

    public String getCommandInfo() {
        return commandInfo;
    }

    public void setCommandInfo(String commandInfo) {
        this.commandInfo = commandInfo;
    }

    public Date getCreated() {
        return created;
    }

    public void setCreated(Date created) {
        this.created = created;
    }

    public String getAnswerName() {
        return answerName;
    }

    public void setAnswerName(String answerName) {
        this.answerName = answerName;
    }

    public String getAnswerInfo() {
        return answerInfo;
    }

    public void setAnswerInfo(String answerInfo) {
        this.answerInfo = answerInfo;
    }

    public Date getRemoved() {
        return removed;
    }

    public void setRemoved(Date removed) {
        this.removed = removed;
    }

    public Date getUpdated() {
        return updated;
    }

    public void setUpdated(Date updated) {
        this.updated = updated;
    }

    public Long getRetryCount() {
        return retryCount;
    }

    public void setRetryCount(Long retryCount) {
        this.retryCount = retryCount;
    }
}
