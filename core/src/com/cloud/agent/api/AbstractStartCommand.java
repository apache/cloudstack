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

import java.util.List;

import com.cloud.storage.VolumeVO;
import com.cloud.storage.VirtualMachineTemplate.BootloaderType;

public abstract class AbstractStartCommand extends Command {
	
	protected String vmName;
	protected String storageHosts[] = new String[2];
	protected List<VolumeVO> volumes;
	protected boolean mirroredVols = false;
	protected BootloaderType bootloader = BootloaderType.PyGrub;

	public AbstractStartCommand(String vmName, String storageHost, List<VolumeVO> vols) {
	    this(vmName, new String[] {storageHost}, vols, false);
	}
	
	public AbstractStartCommand(String vmName, String[] storageHosts, List<VolumeVO> volumes, boolean mirroredVols) {
		super();
		this.vmName = vmName;
		this.storageHosts = storageHosts;
		this.volumes = volumes;
		this.mirroredVols = mirroredVols;
	}

	public BootloaderType getBootloader() {
		return bootloader;
	}

	public void setBootloader(BootloaderType bootloader) {
		this.bootloader = bootloader;
	}

	protected AbstractStartCommand() {
		super();
	}

	public List<VolumeVO> getVolumes() {
		return volumes;
	}

	public String getVmName() {
	    return vmName;
	}

	public String getStorageHost() {
		return storageHosts[0];
	}

	public boolean isMirroredVols() {
		return mirroredVols;
	}

	public void setMirroredVols(boolean mirroredVols) {
		this.mirroredVols = mirroredVols;
	}
	
	public String [] getStorageHosts() {
		return storageHosts;
	}

}