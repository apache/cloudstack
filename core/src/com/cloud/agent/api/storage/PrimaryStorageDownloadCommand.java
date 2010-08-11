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

import com.cloud.storage.Storage.ImageFormat;


/**
 * @author Anthony
 *
 */
public class PrimaryStorageDownloadCommand extends AbstractDownloadCommand {
	String localPath;
	String poolUuid;
	long poolId;

	protected PrimaryStorageDownloadCommand() {
	}

    public PrimaryStorageDownloadCommand(String name, String url, ImageFormat format, long accountId, long poolId, String poolUuid) {
        super(name, url, format, accountId);
        this.poolId = poolId;
        this.poolUuid = poolUuid;
    }
    
    public String getPoolUuid() {
        return poolUuid;
    }
    
    public long getPoolId() {
        return poolId;
    }
    
    public void setLocalPath(String path) {
    	this.localPath = path;
    }
    
    public String getLocalPath() {
    	return localPath;
    }
    
    @Override
    public boolean executeInSequence() {
        return true;
    }
}
