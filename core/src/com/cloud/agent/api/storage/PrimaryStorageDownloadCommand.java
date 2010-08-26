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
	
	//
	// Temporary hacking to make vmware work quickly, expose NFS raw information to allow
	// agent do quick copy over NFS.
	//
	// provide storage URL (it contains all information to help agent resource to mount the
	// storage if needed, example of such URL may be as following
	// 		nfs://192.168.10.231/export/home/kelven/vmware-test/secondary
	String secondaryStorageUrl;
	String primaryStorageUrl;

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
    
    public void setSecondaryStorageUrl(String url) {
    	secondaryStorageUrl = url;
    }
    
    public String getSecondaryStorageUrl() {
    	return secondaryStorageUrl;
    }
    
    public void setPrimaryStorageUrl(String url) {
    	primaryStorageUrl = url;
    }
    
    public String getPrimaryStorageUrl() {
    	return primaryStorageUrl;
    }
    
    @Override
    public boolean executeInSequence() {
        return true;
    }
}
