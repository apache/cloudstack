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

public abstract class AbstractDownloadCommand extends StorageCommand {

    private String url;
    private ImageFormat format;
    private long accountId;
    private String name;
    
    protected AbstractDownloadCommand() {
    }
    
    protected AbstractDownloadCommand(String name, String url, ImageFormat format, long accountId) {
        this.url = url;
        this.format = format;
        this.accountId = accountId;
        this.name = name;
    }
    
    protected AbstractDownloadCommand(AbstractDownloadCommand that) {
        this(that.name, that.url, that.format, that.accountId);
    }
    
    public String getUrl() {
        return url;
    }
    
    public String getName() {
        return name;
    }
    
    public ImageFormat getFormat() {
        return format;
    }
    
    public long getAccountId() {
        return accountId;
    }
    
    @Override
    public boolean executeInSequence() {
        return true;
    }

	public void setUrl(String url) {
		this.url = url;
	}

}
