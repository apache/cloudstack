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

package com.cloud.agent.api;

import java.io.File;
import java.util.UUID;

import com.cloud.storage.StoragePoolVO;

public class DeleteStoragePoolCommand extends Command {
	
	StoragePoolVO pool;
	public static final String LOCAL_PATH_PREFIX="/mnt/";
	String localPath;

	
	public DeleteStoragePoolCommand() {
		
	}
    
    public DeleteStoragePoolCommand(StoragePoolVO pool, String localPath) {
    	this.pool = pool;
    	this.localPath = localPath;
    }
    
    public DeleteStoragePoolCommand(StoragePoolVO pool) {
		this(pool, LOCAL_PATH_PREFIX + File.separator + UUID.nameUUIDFromBytes((pool.getHostAddress() + pool.getPath()).getBytes()));
	}

    public StoragePoolVO getPool() {
        return pool;
    }

    public void setPool(StoragePoolVO pool) {
        this.pool = pool;
    }
    
	@Override
    public boolean executeInSequence() {
        return false;
    }

	public String getLocalPath() {
		return localPath;
	}
	
}
