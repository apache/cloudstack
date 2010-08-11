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

import java.util.ArrayList;
import java.util.List;

import com.cloud.storage.Volume;
import com.cloud.storage.VolumeVO;


/**
 * @author chiradeep
 *
 */
public class MirrorAnswer extends Answer {
    public enum MirrorState {
    	NOT_MIRRORED,
    	ACTIVE,
    	DEGRADED,
    	FAILED
	}
    
    public enum DiskState {
    	ACTIVE,
    	REBUILD,
    	FAILED
	}
    
    public class MirrorInfo {
    	public Volume.VolumeType volType;
    	public VolumeVO vol1;
    	public VolumeVO vol2;
    	public DiskState disk1State;
    	public DiskState disk2State;
    	public MirrorState mirrorState;
    	public int rebuildPct;
    }

	String vmName;
	//List<MirrorInfo> mirrorInfo = new ArrayList<MirrorInfo>();
	String error;

    
    protected MirrorAnswer() {
    }


	public MirrorAnswer(MirrorCommand cmd, String vmName, String err) {
		super(cmd, false, err);
		this.vmName = vmName;
	}

	public MirrorAnswer(MirrorCommand cmd, String vmName){
		super(cmd, true, null);
		this.vmName = vmName;
	}

	public String getVmName() {
		return vmName;
	}


	public void setVmName(String vmName) {
		this.vmName = vmName;
	}

    

}
