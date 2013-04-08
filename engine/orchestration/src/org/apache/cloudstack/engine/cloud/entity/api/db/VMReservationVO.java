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
package org.apache.cloudstack.engine.cloud.entity.api.db;

import java.util.Date;
import java.util.Map;
import java.util.UUID;

import javax.persistence.Column;
import javax.persistence.Entity;
import javax.persistence.GeneratedValue;
import javax.persistence.GenerationType;
import javax.persistence.Id;
import javax.persistence.Table;
import javax.persistence.Transient;

import org.apache.cloudstack.api.Identity;
import org.apache.cloudstack.api.InternalIdentity;

import com.cloud.utils.db.GenericDao;

@Entity
@Table(name = "vm_reservation")
public class VMReservationVO implements Identity, InternalIdentity{

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "id")
    private long id;

    @Column(name = "vm_id")
    private long vmId;

    @Column(name="uuid")
    private String uuid;

    @Column(name="data_center_id")
    private long dataCenterId;

    @Column(name="pod_id")
    private long podId;

    @Column(name="cluster_id")
    private long clusterId;

    @Column(name="host_id")
    private long hostId;

    @Column(name=GenericDao.CREATED_COLUMN)
    private Date created;

    @Column(name=GenericDao.REMOVED_COLUMN)
    private Date removed;

    // VolumeId -> poolId
    @Transient
    Map<Long,Long> volumeReservationMap;

    /**
     * There should never be a public constructor for this class. Since it's
     * only here to define the table for the DAO class.
     */
    protected VMReservationVO() {
    }

    public VMReservationVO(long vmId, long dataCenterId, long podId, long clusterId, long hostId) {
        this.vmId = vmId;
        this.dataCenterId = dataCenterId;
        this.podId = podId;
        this.clusterId = clusterId;
        this.hostId = hostId;
        this.uuid = UUID.randomUUID().toString();
    }


    public long getId() {
        return id;
    }

    public long getVmId() {
        return vmId;
    }

    @Override
    public String getUuid() {
        return uuid;
    }

    public long getDataCenterId() {
        return dataCenterId;
    }

    public Long getPodId() {
        return podId;
    }

    public Long getClusterId() {
        return clusterId;
    }

    public Long getHostId() {
        return hostId;
    }

    public Map<Long,Long> getVolumeReservation(){
        return volumeReservationMap;
    }

    public void setVolumeReservation(Map<Long,Long> volumeReservationMap){
        this.volumeReservationMap = volumeReservationMap;
    }

}
