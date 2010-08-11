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

public  class MirrorCommand extends Command {

	protected String vmName;
	protected String removeHost;
	protected String addHost;
	protected List<VolumeVO> removeVols;
	protected List<VolumeVO> addVols;


	public MirrorCommand(String vmName, String removeHost, String addHost,
			List<VolumeVO> removeVols, List<VolumeVO> addVols) {
		super();
		this.vmName = vmName;
		this.removeHost = removeHost;
		this.addHost = addHost;
		this.removeVols = removeVols;
		this.addVols = addVols;
	}

	protected MirrorCommand() {
		//satisfies gson
	}
	
	public String getVmName() {
		return vmName;
	}


	public void setVmName(String vmName) {
		this.vmName = vmName;
	}


	public String getRemoveHost() {
		return removeHost;
	}


	public void setRemoveHost(String removeHost) {
		this.removeHost = removeHost;
	}


	public String getAddHost() {
		return addHost;
	}


	public void setAddHost(String addHost) {
		this.addHost = addHost;
	}


	public List<VolumeVO> getRemoveVols() {
		return removeVols;
	}


	public void setRemoveVols(List<VolumeVO> removeVols) {
		this.removeVols = removeVols;
	}


	public List<VolumeVO> getAddVols() {
		return addVols;
	}


	public void setAddVols(List<VolumeVO> addVols) {
		this.addVols = addVols;
	}


	@Override
	public boolean executeInSequence() {
		return true;
	}

	
}