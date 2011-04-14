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

import com.cloud.storage.Upload;

public class DeleteEntityDownloadURLCommand extends AbstractDownloadCommand {
    
    private String path;
    private String extractUrl; 
    private Upload.Type type;

    public DeleteEntityDownloadURLCommand(String path, Upload.Type type, String url) {
        super();
        this.path = path;
        this.type = type;
        this.extractUrl = url;
    }

    public DeleteEntityDownloadURLCommand() {
        super();
    }

    public String getPath() {
        return path;
    }

    public void setPath(String path) {
        this.path = path;
    }

    public Upload.Type getType() {
        return type;
    }

    public void setType(Upload.Type type) {
        this.type = type;
    }

	public String getExtractUrl() {
		return extractUrl;
	}

	public void setExtractUrl(String extractUrl) {
		this.extractUrl = extractUrl;
	}

}
