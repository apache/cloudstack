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

import com.cloud.utils.NumbersUtil;

/**
* LegacyZoneVO contains id of CloudStack zone containing clusters from multiple VMware vCetners and/or VMware Datacenters.
*/

@Entity
@Table(name="legacy_zones")
public class LegacyZoneVO implements LegacyZone {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "id")
    private long id;

    @Column(name = "zone_id")
    private long zoneId;

    @Override
    public long getId() {
        return id;
    }

    @Override
    public long getZoneId() {
        return zoneId;
    }

    @Override
    public int hashCode() {
        return NumbersUtil.hash(id);
    }

    @Override
    public boolean equals(Object obj) {
        if (obj instanceof LegacyZoneVO) {
            return ((LegacyZoneVO)obj).getId() == this.getId();
        } else {
            return false;
        }
    }

    public LegacyZoneVO() {
    }

    public LegacyZoneVO(long zoneId) {
        this.id = zoneId;
    }

    public LegacyZoneVO(long id, long zoneId) {
        this.id = id;
        this.zoneId = zoneId;
    }

}
