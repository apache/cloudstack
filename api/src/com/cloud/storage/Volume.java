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

import java.util.Date;

import com.cloud.domain.PartOf;
import com.cloud.template.BasedOn;
import com.cloud.user.OwnedBy;


public interface Volume extends PartOf, OwnedBy, BasedOn {
	enum VolumeType {UNKNOWN, ROOT, SWAP, DATADISK, ISO};
	
	enum MirrorState {NOT_MIRRORED, ACTIVE, DEFUNCT};
	
	enum State {
	    Allocated,
	    Creating,
	    Created,
	    Corrupted,
	    ToBeDestroyed,
	    Expunging,
	    Destroyed
	}
	
	enum SourceType {
		Snapshot,DiskOffering,Template,Blank
	}
	
	long getId();
	/**
     * @return the volume name
     */
    String getName();
    
    /**
     * @return total size of the partition
     */
    long getSize();
    
    void setSize(long size);
    
    /**
     * @return the vm instance id
     */
    Long getInstanceId();
    
    /**
     * @return the folder of the volume
     */
    String getFolder();
    
    /**
     * @return the path created.
     */
    String getPath();
    
    Long getPodId();
    
    long getDataCenterId();
    
    VolumeType getVolumeType();
    
    Storage.StorageResourceType getStorageResourceType();
    
	Long getPoolId();
	
	State getState();
	
	SourceType getSourceType();
	
	void setSourceType(SourceType sourceType);

	void setSourceId(Long sourceId);

	Long getSourceId();

	Date getAttached();

	void setAttached(Date attached);
}
