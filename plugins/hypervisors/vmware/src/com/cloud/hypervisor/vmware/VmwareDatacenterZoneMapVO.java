//Licensed to the Apache Software Foundation (ASF) under one
//or more contributor license agreements.  See the NOTICE file
//distributed with this work for additional information
//regarding copyright ownership.  The ASF licenses this file
//to you under the Apache License, Version 2.0 (the
//"License"); you may not use this file except in compliance
//with the License.  You may obtain a copy of the License at
//
//http://www.apache.org/licenses/LICENSE-2.0
//
//Unless required by applicable law or agreed to in writing,
//software distributed under the License is distributed on an
//"AS IS" BASIS, WITHOUT WARRANTIES OR CONDITIONS OF ANY
//KIND, either express or implied.  See the License for the
//specific language governing permissions and limitations
//under the License.

package com.cloud.hypervisor.vmware;

import javax.persistence.Column;
import javax.persistence.Entity;
import javax.persistence.GeneratedValue;
import javax.persistence.GenerationType;
import javax.persistence.Id;
import javax.persistence.Table;


//NOTE: This particular table is totally internal to the CS MS.
//Do not ever include a uuid/guid field in this table. We just
//need it map zone ids with VMware datacenter Ids.

@Entity
@Table(name="vmware_data_center_zone_map")
public class VmwareDatacenterZoneMapVO implements VmwareDatacenterZoneMap {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name="id")
    private long id;

    @Column(name="zone_id")
    private long zoneId;

    @Column(name="vmware_data_center_id")
    private long vmwareDcId;

    public VmwareDatacenterZoneMapVO(long zoneId, long vmwareDcId) {
        this.zoneId = zoneId;
        this.vmwareDcId = vmwareDcId;
    }

    public VmwareDatacenterZoneMapVO() {
        // Do nothing.
    }

    @Override
    public long getId() {
        return id;
    }

    @Override
    public long getZoneId() {
        return zoneId;
    }

    @Override
    public long getVmwareDcId() {
        return vmwareDcId;
    }

     public void setZoneId(long zoneId) {
        this.zoneId = zoneId;
    }

    public void setVmwareDcId(long vmwareDcId) {
        this.vmwareDcId = vmwareDcId;
    }
}
