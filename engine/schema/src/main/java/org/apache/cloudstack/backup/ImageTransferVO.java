//Licensed to the Apache Software Foundation (ASF) under one
//or more contributor license agreements.  See the NOTICE file
//distributed with this work for additional information
//regarding copyright ownership.  The ASF licenses this file
//to you under the Apache License, Version 2.0 (the
//"License"); you may not use this file except in compliance
//the License.  You may obtain a copy of the License at
//
//http://www.apache.org/licenses/LICENSE-2.0
//
//Unless required by applicable law or agreed to in writing,
//software distributed under the License is distributed on an
//"AS IS" BASIS, WITHOUT WARRANTIES OR CONDITIONS OF ANY
//KIND, either express or implied.  See the License for the
//specific language governing permissions and limitations
//under the License.

package org.apache.cloudstack.backup;

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

@Entity
@Table(name = "image_transfer")
public class ImageTransferVO implements ImageTransfer {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "id")
    private long id;

    @Column(name = "uuid")
    private String uuid;

    @Column(name = "backup_id")
    private long backupId;

    @Column(name = "disk_id")
    private long diskId;

    @Column(name = "host_id")
    private long hostId;

    @Column(name = "nbd_port")
    private int nbdPort;

    @Column(name = "transfer_url")
    private String transferUrl;

    @Enumerated(value = EnumType.STRING)
    @Column(name = "phase")
    private Phase phase;

    @Enumerated(value = EnumType.STRING)
    @Column(name = "direction")
    private Direction direction;

    @Column(name = "signed_ticket_id")
    private String signedTicketId;

    @Column(name = "account_id")
    Long accountId;

    @Column(name = "domain_id")
    Long domainId;

    @Column(name = "data_center_id")
    Long dataCenterId;

    @Column(name = "created")
    @Temporal(value = TemporalType.TIMESTAMP)
    private Date created;

    @Column(name = "updated")
    @Temporal(value = TemporalType.TIMESTAMP)
    private Date updated;

    @Column(name = "removed")
    @Temporal(value = TemporalType.TIMESTAMP)
    private Date removed;

    public ImageTransferVO() {
    }

    public ImageTransferVO(String uuid, Long backupId, long diskId, long hostId, int nbdPort, Phase phase, Direction direction, Long accountId, Long domainId, Long dataCenterId) {
        this.uuid = uuid;
        this.backupId = backupId;
        this.diskId = diskId;
        this.hostId = hostId;
        this.nbdPort = nbdPort;
        this.phase = phase;
        this.direction = direction;
        this.accountId = accountId;
        this.domainId = domainId;
        this.dataCenterId = dataCenterId;
        this.created = new Date();
    }

    @Override
    public long getId() {
        return id;
    }

    @Override
    public String getUuid() {
        return uuid;
    }

    @Override
    public long getBackupId() {
        return backupId;
    }

    public void setBackupId(long backupId) {
        this.backupId = backupId;
    }

    @Override
    public long getDiskId() {
        return diskId;
    }

    public void setDiskId(long diskId) {
        this.diskId = diskId;
    }

    @Override
    public long getHostId() {
        return hostId;
    }

    public void setHostId(long hostId) {
        this.hostId = hostId;
    }

    @Override
    public int getNbdPort() {
        return nbdPort;
    }

    public void setNbdPort(int nbdPort) {
        this.nbdPort = nbdPort;
    }

    @Override
    public String getTransferUrl() {
        return transferUrl;
    }

    public void setTransferUrl(String transferUrl) {
        this.transferUrl = transferUrl;
    }

    @Override
    public Phase getPhase() {
        return phase;
    }

    public void setPhase(Phase phase) {
        this.phase = phase;
        this.updated = new Date();
    }

    @Override
    public Direction getDirection() {
        return direction;
    }

    public void setDirection(Direction direction) {
        this.direction = direction;
    }

    @Override
    public String getSignedTicketId() {
        return signedTicketId;
    }

    public void setSignedTicketId(String signedTicketId) {
        this.signedTicketId = signedTicketId;
    }

    @Override
    public Class<?> getEntityType() {
        return ImageTransfer.class;
    }

    @Override
    public String getName() {
        return null;
    }

    @Override
    public long getDomainId() {
        return domainId;
    }

    @Override
    public long getAccountId() {
        return accountId;
    }

    @Override
    public long getDataCenterId() {
        return dataCenterId;
    }

    public Date getCreated() {
        return created;
    }

    public Date getUpdated() {
        return updated;
    }
}
