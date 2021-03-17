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

package org.apache.cloudstack.storage.datastore.api;

import com.google.common.base.Strings;

public class StoragePoolStatistics {
    String maxCapacityInKb; // total capacity
    String spareCapacityInKb; // spare capacity, space not used for volumes creation/allocation
    String netCapacityInUseInKb; // user data capacity in use
    String netUnusedCapacityInKb; // capacity available for volume creation (volume space to write)

    public Long getMaxCapacityInKb() {
        if (Strings.isNullOrEmpty(maxCapacityInKb)) {
            return Long.valueOf(0);
        }
        return Long.valueOf(maxCapacityInKb);
    }

    public void setMaxCapacityInKb(String maxCapacityInKb) {
        this.maxCapacityInKb = maxCapacityInKb;
    }

    public Long getSpareCapacityInKb() {
        if (Strings.isNullOrEmpty(spareCapacityInKb)) {
            return Long.valueOf(0);
        }
        return Long.valueOf(spareCapacityInKb);
    }

    public void setSpareCapacityInKb(String spareCapacityInKb) {
        this.spareCapacityInKb = spareCapacityInKb;
    }

    public Long getNetCapacityInUseInKb() {
        if (Strings.isNullOrEmpty(netCapacityInUseInKb)) {
            return Long.valueOf(0);
        }
        return Long.valueOf(netCapacityInUseInKb);
    }

    public void setNetCapacityInUseInKb(String netCapacityInUseInKb) {
        this.netCapacityInUseInKb = netCapacityInUseInKb;
    }

    public Long getNetUnusedCapacityInKb() {
        if (Strings.isNullOrEmpty(netUnusedCapacityInKb)) {
            return Long.valueOf(0);
        }
        return Long.valueOf(netUnusedCapacityInKb);
    }

    public Long getNetUnusedCapacityInBytes() {
        return (getNetUnusedCapacityInKb() * 1024);
    }

    public void setNetUnusedCapacityInKb(String netUnusedCapacityInKb) {
        this.netUnusedCapacityInKb = netUnusedCapacityInKb;
    }

    public Long getNetMaxCapacityInBytes() {
        // total usable capacity = ("maxCapacityInKb" - "spareCapacityInKb") / 2
        Long netMaxCapacityInKb = getMaxCapacityInKb() - getSpareCapacityInKb();
        return ((netMaxCapacityInKb / 2) * 1024);
    }

    public Long getNetUsedCapacityInBytes() {
        return (getNetMaxCapacityInBytes() - getNetUnusedCapacityInBytes());
    }
}
