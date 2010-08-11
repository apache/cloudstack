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

package com.cloud.storage;

import javax.persistence.Column;
import javax.persistence.Entity;
import javax.persistence.Table;

@Entity
@Table(name="snapshot_policy_ref")
public class SnapshotPolicyRefVO {
	
    @Column(name="snap_id")
    long snapshotId;
    
    @Column(name="volume_id")
    long volumeId;
    
    @Column(name="policy_id")
    long policyId;
    
    public SnapshotPolicyRefVO() { }

    public SnapshotPolicyRefVO(long snapshotId, long volumeId, long policyId) {
        this.snapshotId = snapshotId;
        this.volumeId = volumeId;
        this.policyId = policyId;
    }
    
    public long getSnapshotId() {
        return snapshotId;
    }
    
    public long getVolumeId() {
        return snapshotId;
    }
    
    public long getPolicyId() {
        return policyId;
    }
}
