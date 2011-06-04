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

package com.cloud.cluster.agentlb;

import java.util.Date;

import javax.persistence.Column;
import javax.persistence.Entity;
import javax.persistence.Id;
import javax.persistence.Inheritance;
import javax.persistence.InheritanceType;
import javax.persistence.Table;

import com.cloud.utils.db.GenericDao;

@Entity
@Table(name = "op_host_transfer")
@Inheritance(strategy = InheritanceType.TABLE_PER_CLASS)
public class HostTransferMapVO {

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
    
    @Column(name=GenericDao.CREATED_COLUMN)
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

    public long getId() {
        return id;
    }
    
    public Date getCreated() {
        return created;
    }

}
