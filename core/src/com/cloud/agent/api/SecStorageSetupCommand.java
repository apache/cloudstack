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

public class SecStorageSetupCommand extends Command {
	private Long dcId;
	String [] allowedInternalSites = new String[0];
	String copyUserName;
	String copyPassword;
	
	public SecStorageSetupCommand() {
		super();
	}
	
	public SecStorageSetupCommand(Long dcId) {
		super();
		this.dcId = dcId;
	}
	
	public Long getDataCenterId() {
		return dcId;
	}
	
	@Override
	public boolean executeInSequence() {
		return true;
	}

	public String[] getAllowedInternalSites() {
		return allowedInternalSites;
	}

	public void setAllowedInternalSites(String[] allowedInternalSites) {
		this.allowedInternalSites = allowedInternalSites;
	}

	public String getCopyUserName() {
		return copyUserName;
	}

	public void setCopyUserName(String copyUserName) {
		this.copyUserName = copyUserName;
	}

	public String getCopyPassword() {
		return copyPassword;
	}

	public void setCopyPassword(String copyPassword) {
		this.copyPassword = copyPassword;
	}

}
