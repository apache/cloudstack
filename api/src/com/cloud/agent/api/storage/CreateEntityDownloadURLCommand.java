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

public class CreateEntityDownloadURLCommand extends AbstractDownloadCommand {

    public CreateEntityDownloadURLCommand(String installPath, String uuid) {
        super();
        this.installPath = installPath;
        this.extractLinkUUID = uuid;
    }

    public CreateEntityDownloadURLCommand() {
    }

    private String installPath;
    private String extractLinkUUID;
    
    @Override
    public boolean executeInSequence() {
        return false;
    }

    public String getInstallPath() {
        return installPath;
    }

    public void setInstallPath(String installPath) {
        this.installPath = installPath;
    }

	public String getExtractLinkUUID() {
		return extractLinkUUID;
	}

	public void setExtractLinkUUID(String extractLinkUUID) {
		this.extractLinkUUID = extractLinkUUID;
	}

}
