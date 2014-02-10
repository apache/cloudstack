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
package com.cloud.cluster.agentlb;

import java.util.Date;

import javax.persistence.Column;
import javax.persistence.Entity;
import javax.persistence.Id;
import javax.persistence.Inheritance;
import javax.persistence.InheritanceType;
import javax.persistence.Table;

import org.apache.cloudstack.api.InternalIdentity;

import com.cloud.utils.db.GenericDao;

@Entity
@Table(name = "op_host_transfer")
@Inheritance(strategy = InheritanceType.TABLE_PER_CLASS)
public class HostTransferMapVO implements InternalIdentity {

    public enum HostTransferState {
        TransferRequested, TransferStarted;
    }

    @Id
    @Column(name = "id")
    private long id;

    @Column(name = "initial_mgmt_server_id")
    private long initialOwner;

    @Column(name = "future_mgmt_server_id")
    private long futureOwner;

    @Column(name = "state")
    private HostTransferState state;

    @Column(name = GenericDao.CREATED_COLUMN)
    private Date created;

    public HostTransferMapVO(long hostId, long initialOwner, long futureOwner) {
        this.id = hostId;
        this.initialOwner = initialOwner;
        this.futureOwner = futureOwner;
        this.state = HostTransferState.TransferRequested;
    }

    protected HostTransferMapVO() {
    }

    public long getInitialOwner() {
        return initialOwner;
    }

    public long getFutureOwner() {
        return futureOwner;
    }

    public HostTransferState getState() {
        return state;
    }

    public void setInitialOwner(long initialOwner) {
        this.initialOwner = initialOwner;
    }

    public void setFutureOwner(long futureOwner) {
        this.futureOwner = futureOwner;
    }

    public void setState(HostTransferState state) {
        this.state = state;
    }

    @Override
    public long getId() {
        return id;
    }

    public Date getCreated() {
        return created;
    }

}
