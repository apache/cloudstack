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

package org.apache.cloudstack.vmmigration;

import com.cloud.event.VmMigrationEvent;
import com.cloud.utils.db.GenericDao;

import javax.persistence.Column;
import javax.persistence.Entity;
import javax.persistence.EnumType;
import javax.persistence.Enumerated;
import javax.persistence.GeneratedValue;
import javax.persistence.GenerationType;
import javax.persistence.Id;
import javax.persistence.Table;
import java.util.Date;

@Entity
@Table(name = "vm_migration")
public class VmMigrationVO implements VmMigrationEvent {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "id")
    private long id;

    @Enumerated(value = EnumType.STRING)
    @Column(name = "state")
    private State state;

    @Column(name = "instance_name")
    private String instanceName;

    @Column(name = "instance_id")
    private Long instanceId;

    @Column(name = "description", length = 1024)
    private String description;

    @Column(name = "vm_type")
    private String vmType;

    @Column(name = "source_host")
    private String sourceHost;

    @Column(name = "destination_host")
    private String destinationHost;

    @Column(name = GenericDao.CREATED_COLUMN)
    private Date created;

    @Column(name = "user_name")
    private String userName;

    @Column(name = "account_id")
    private long accountId;

    @Column(name = "domain_id")
    private long domainId;

    public VmMigrationVO() {}

    public VmMigrationVO(VmMigrationEvent.State state, String instanceName, Long instanceId, String description,
                         String vmType, String sourceHostName, String destinationHostName, Date created,
                         String userName, long accountId, long domainId) {
        this.state = state;
        this.instanceId = instanceId;
        this.description = description;
        this.vmType = vmType;
        this.sourceHost = sourceHostName;
        this.destinationHost = destinationHostName;
        this.created = created;
        this.userName = userName;
        this.accountId = accountId;
        this.domainId = domainId;
        this.instanceName = instanceName;
    }

    public long getId() {
        return id;
    }

    public void setId(long id) {
        this.id = id;
    }

    @Override
    public State getState() {
        return state;
    }

    public VmMigrationVO setState(State state) {
        this.state = state;
        return this;
    }

    @Override
    public String getDescription() {
        return description;
    }

    public VmMigrationVO setDescription(String description) {
        this.description = description;
        return this;
    }

    public String getSourceHost() {
        return sourceHost;
    }

    public VmMigrationVO setSourceHost(String sourceHost) {
        this.sourceHost = sourceHost;
        return this;
    }

    public String getDestinationHost() {
        return destinationHost;
    }

    public VmMigrationVO setDestinationHost(String destinationHost) {
        this.destinationHost = destinationHost;
        return this;
    }

    @Override
    public Date getCreateDate() {
        return created;
    }

    public VmMigrationVO setCreated(Date createDate) {
        this.created = createDate;
        return this;
    }

    public String getUserName() {
        return userName;
    }

    public VmMigrationVO setUserName(String userId) {
        this.userName = userId;
        return this;
    }

    public long getAccountId() {
        return accountId;
    }

    public VmMigrationVO setAccountId(long accountId) {
        this.accountId = accountId;
        return this;
    }

    public long getDomainId() {
        return domainId;
    }

    public VmMigrationVO setDomainId(long domainId) {
        this.domainId = domainId;
        return this;
    }

    @Override
    public String getVmType() {
        return vmType;
    }

    public VmMigrationVO setVmType(String vmtype) {
        this.vmType = vmtype;
        return this;
    }

    public String getInstanceName() {
        return instanceName;
    }

    public VmMigrationVO setInstanceName(String vmName) {
        this.instanceName = vmName;
        return this;
    }

    public Long getInstanceId() {
        return instanceId;
    }

    public VmMigrationVO setInstanceId(Long instanceId) {
        this.instanceId = instanceId;
        return this;
    }
}
