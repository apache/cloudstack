/*
 * Licensed to the Apache Software Foundation (ASF) under one
 * or more contributor license agreements.  See the NOTICE file
 * distributed with this work for additional information
 * regarding copyright ownership.  The ASF licenses this file
 * to you under the Apache License, Version 2.0 (the
 * "License"); you may not use this file except in compliance
 * with the License.  You may obtain a copy of the License at
 *
 *   http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing,
 * software distributed under the License is distributed on an
 * "AS IS" BASIS, WITHOUT WARRANTIES OR CONDITIONS OF ANY
 * KIND, either express or implied.  See the License for the
 * specific language governing permissions and limitations
 * under the License.
 */
package org.apache.cloudstack.storage.snapshot;

import java.util.Date;

import javax.inject.Inject;

import org.apache.cloudstack.engine.subsystem.api.storage.DataObjectType;
import org.apache.cloudstack.engine.subsystem.api.storage.DataStore;
import org.apache.cloudstack.engine.subsystem.api.storage.ObjectInDataStoreStateMachine;
import org.apache.cloudstack.engine.subsystem.api.storage.SnapshotInfo;
import org.apache.cloudstack.engine.subsystem.api.storage.VolumeDataFactory;
import org.apache.cloudstack.engine.subsystem.api.storage.VolumeInfo;
import org.apache.cloudstack.engine.subsystem.api.storage.disktype.DiskFormat;
import org.apache.cloudstack.storage.datastore.ObjectInDataStoreManager;
import org.apache.log4j.Logger;

import com.cloud.hypervisor.Hypervisor.HypervisorType;
import com.cloud.storage.Snapshot;
import com.cloud.storage.SnapshotVO;
import com.cloud.storage.dao.SnapshotDao;
import com.cloud.storage.dao.VolumeDao;
import com.cloud.utils.component.ComponentContext;
import com.cloud.utils.exception.CloudRuntimeException;
import com.cloud.utils.fsm.NoTransitionException;

public class SnapshotObject implements SnapshotInfo {
	private static final Logger s_logger = Logger.getLogger(SnapshotObject.class);
    private SnapshotVO snapshot;
    private DataStore store;
    @Inject
    protected SnapshotDao snapshotDao; 
    @Inject
    protected VolumeDao volumeDao;
    @Inject protected VolumeDataFactory volFactory;
    @Inject protected SnapshotStateMachineManager stateMachineMgr;
    @Inject
    ObjectInDataStoreManager ojbectInStoreMgr;
    public SnapshotObject() {
      
    }
    
    protected void configure(SnapshotVO snapshot, DataStore store) {
    	this.snapshot = snapshot;
    	this.store = store;
    }
    
    public static SnapshotObject getSnapshotObject(SnapshotVO snapshot, DataStore store) {
    	SnapshotObject snapObj = ComponentContext.inject(SnapshotObject.class);
    	snapObj.configure(snapshot, store);
    	return snapObj;
    }

    public DataStore getStore() {
        return this.store;
    }

    @Override
    public SnapshotInfo getParent() {
        // TODO Auto-generated method stub
        return null;
    }

    @Override
    public SnapshotInfo getChild() {
        // TODO Auto-generated method stub
        return null;
    }

    @Override
    public VolumeInfo getBaseVolume() {
        return volFactory.getVolume(this.snapshot.getVolumeId());
    }

    @Override
    public long getId() {
       return this.snapshot.getId();
    }

    @Override
    public String getUri() {
        return this.snapshot.getUuid();
    }

    @Override
    public DataStore getDataStore() {
        return this.store;
    }

    @Override
    public Long getSize() {
    	return this.getSize();
    }

    @Override
    public DataObjectType getType() {
    	return DataObjectType.SNAPSHOT;
    }

    @Override
    public DiskFormat getFormat() {
        return null;
    }

    @Override
    public String getUuid() {
        return this.snapshot.getUuid();
    }

	@Override
	public void processEvent(
			ObjectInDataStoreStateMachine.Event event) {
		try {
			ojbectInStoreMgr.update(this, event);
		} catch (Exception e) {
			s_logger.debug("Failed to update state:" + e.toString());
			throw new CloudRuntimeException("Failed to update state: " + e.toString());
		}
	}

	@Override
	public long getAccountId() {
		return this.snapshot.getAccountId();
	}

	@Override
	public long getVolumeId() {
		return this.snapshot.getVolumeId();
	}

	@Override
	public String getPath() {
		return this.snapshot.getPath();
	}
	
	public void setPath(String path) {
		this.snapshot.setPath(path);
	}

	@Override
	public String getName() {
		return this.snapshot.getName();
	}

	@Override
	public Date getCreated() {
		return this.snapshot.getCreated();
	}

	@Override
	public Type getRecurringType() {
		return this.snapshot.getRecurringType();
	}

	@Override
	public State getState() {
		return this.snapshot.getState();
	}

	@Override
	public HypervisorType getHypervisorType() {
		return this.snapshot.getHypervisorType();
	}

	@Override
	public boolean isRecursive() {
		return this.snapshot.isRecursive();
	}

	@Override
	public short getsnapshotType() {
		return this.snapshot.getsnapshotType();
	}

	@Override
	public long getDomainId() {
		return this.snapshot.getDomainId();
	}
	
	public void setPrevSnapshotId(Long id) {
		this.snapshot.setPrevSnapshotId(id);
	}

	@Override
	public Long getDataCenterId() {
		return this.snapshot.getDataCenterId();
	}
	
	public void processEvent(Snapshot.Event event)
			throws NoTransitionException {
		stateMachineMgr.processEvent(this.snapshot, event);
	}

	@Override
	public Long getPrevSnapshotId() {
		return this.snapshot.getPrevSnapshotId();
	}
	
	public void setBackupSnapshotId(String id) {
		this.snapshot.setBackupSnapshotId(id);
	}
	
	public String getBackupSnapshotId() {
		return this.snapshot.getBackupSnapshotId();
	}
}
