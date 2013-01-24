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
@Table(name = "volume_reservation")
public class VolumeReservationVO implements InternalIdentity{

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "id")
    private long id;

    @Column(name = "vm_reservation_id")
    private Long vmReservationId;
    
    @Column(name = "vm_id")
    private long vmId;
    
    @Column(name="volume_id")
    private long volumeId;

    @Column(name="pool_id")
    private long poolId;
    
    // VolumeId -> poolId 
    @Transient
    Map<String, String> volumeReservationMap;
    
    /**
     * There should never be a public constructor for this class. Since it's
     * only here to define the table for the DAO class.
     */
    protected VolumeReservationVO() {
    }

    public VolumeReservationVO(long vmId, long volumeId, long poolId, Long vmReservationId) {
        this.vmId = vmId;
        this.volumeId = volumeId;
        this.poolId = poolId;
        this.vmReservationId = vmReservationId;
    }
    
    
    public long getId() {
        return id;
    }

    public long getVmId() {
        return vmId;
    }

    public Long geVmReservationId() {
        return vmReservationId;
    }
    
    public long getVolumeId() {
        return volumeId;
    }

    public Long getPoolId() {
        return poolId;
    }


    public Map<String,String> getVolumeReservation(){
        return volumeReservationMap;
    }

}
