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

package com.cloud.storage.secondary;

import com.cloud.utils.events.EventArgs;
import com.cloud.vm.SecondaryStorageVmVO;

public class SecStorageVmAlertEventArgs extends EventArgs {
	
	private static final long serialVersionUID = 23773987551479885L;
	
	public static final int SSVM_CREATED = 1;
	public static final int SSVM_UP = 2; 
	public static final int SSVM_DOWN = 3; 
	public static final int SSVM_CREATE_FAILURE = 4;
	public static final int SSVM_START_FAILURE = 5;
	public static final int SSVM_FIREWALL_ALERT = 6;
	public static final int SSVM_STORAGE_ALERT = 7;
	public static final int SSVM_REBOOTED = 8;
	
	public static final String ALERT_SUBJECT = "ssvm-alert";

	
	private int type;
	private long zoneId;
	private long ssVmId;
	private SecondaryStorageVmVO ssVm;
	private String message;
	
	public SecStorageVmAlertEventArgs(int type, long zoneId, 
		long ssVmId, SecondaryStorageVmVO ssVm, String message) {
		
		super(ALERT_SUBJECT);
		this.type = type;
		this.zoneId = zoneId;
		this.ssVmId = ssVmId;
		this.ssVm = ssVm;
		this.message = message;
	}
	
	public int getType() {
		return type;
	}

	public long getZoneId() {
		return zoneId;
	}

	public long getSecStorageVmId() {
		return ssVmId;
	}

	public SecondaryStorageVmVO getSecStorageVm() {
		return ssVm;
	}

	public String getMessage() {
		return message;
	}
}
