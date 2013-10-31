// Licensed to the Apache Software Foundation (ASF) under one
// or more contributor license agreements.  See the NOTICE file
// distributed with this work for additional information
// regarding copyright ownership.  The ASF licenses this file
// to you under the Apache License, Version 2.0 (the
// "License"); you may not use this file except in compliance
// with the License.  You may obtain a copy of the License at
//
//   http://www.apache.org/licenses/LICENSE-2.0
//
// Unless required by applicable law or agreed to in writing,
// software distributed under the License is distributed on an
// "AS IS" BASIS, WITHOUT WARRANTIES OR CONDITIONS OF ANY
// KIND, either express or implied.  See the License for the 
// specific language governing permissions and limitations
// under the License.
package com.cloud.agent.api;

import java.util.List;

import org.apache.cloudstack.storage.to.VolumeObjectTO;

import com.cloud.vm.snapshot.VMSnapshot;

public class VMSnapshotTO {
	private Long id;
    private String snapshotName;
    private VMSnapshot.Type type;
    private Long createTime;
    private Boolean current;
    private String description;
    private VMSnapshotTO parent;
    private List<VolumeObjectTO> volumes;
    
    public Long getId() {
		return id;
	}
	public void setId(Long id) {
		this.id = id;
	}
	public VMSnapshotTO(Long id, String snapshotName, 
	        VMSnapshot.Type type, Long createTime, 
			String description, Boolean current, VMSnapshotTO parent) {
		super();
		this.id = id;
		this.snapshotName = snapshotName;
		this.type = type;
		this.createTime = createTime;
		this.current = current;
		this.description = description;
		this.parent = parent;
	}
	public VMSnapshotTO() {
	    
	}
	public String getDescription() {
		return description;
	}
	public void setDescription(String description) {
		this.description = description;
	}
	public Boolean getCurrent() {
        return current;
    }
    public void setCurrent(Boolean current) {
        this.current = current;
    }
    public Long getCreateTime() {
        return createTime;
    }
    public void setCreateTime(Long createTime) {
        this.createTime = createTime;
    }
 
    public VMSnapshot.Type getType() {
        return type;
    }
    public void setType(VMSnapshot.Type type) {
        this.type = type;
    }

    public String getSnapshotName() {
        return snapshotName;
    }
    public void setSnapshotName(String snapshotName) {
        this.snapshotName = snapshotName;
    }
    public VMSnapshotTO getParent() {
        return parent;
    }
    public void setParent(VMSnapshotTO parent) {
        this.parent = parent;
    }

    public List<VolumeObjectTO> getVolumes() {
        return this.volumes;
    }

    public void setVolumes(List<VolumeObjectTO> volumes) {
        this.volumes = volumes;
    }
}
