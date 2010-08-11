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

package com.cloud.agent.api.storage;

import com.cloud.agent.api.Command;
import com.cloud.storage.StoragePoolVO;

public class CopyVolumeCommand extends Command {

	long volumeId;
	String volumePath;	
	StoragePoolVO pool;
	String secondaryStorageURL;
	boolean toSecondaryStorage;
	
	public CopyVolumeCommand() {		
	}
	
	public CopyVolumeCommand(long volumeId, String volumePath, StoragePoolVO pool, String secondaryStorageURL, boolean toSecondaryStorage) {
		this.volumeId = volumeId;
		this.volumePath = volumePath;				
		this.pool = pool;
		this.secondaryStorageURL = secondaryStorageURL;
		this.toSecondaryStorage = toSecondaryStorage;
	}
	
	@Override
    public boolean executeInSequence() {
        return true;
    }
	
	public String getVolumePath() {
		return volumePath;
	}
	
	public long getVolumeId() {
		return volumeId;
	}
	
	public StoragePoolVO getPool() {
		return pool;
	}
	
	public String getSecondaryStorageURL() {
		return secondaryStorageURL;
	}
	
	public boolean toSecondaryStorage() {
		return toSecondaryStorage;
	}
}
