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
import org.apache.cloudstack.engine.subsystem.api.storage.DataTO;
import org.apache.cloudstack.engine.subsystem.api.storage.ObjectInDataStoreStateMachine;
import org.apache.cloudstack.engine.subsystem.api.storage.SnapshotInfo;
import org.apache.cloudstack.engine.subsystem.api.storage.VolumeDataFactory;
import org.apache.cloudstack.engine.subsystem.api.storage.VolumeInfo;
import org.apache.cloudstack.engine.subsystem.api.storage.disktype.DiskFormat;
import org.apache.cloudstack.storage.command.CreateObjectAnswer;
import org.apache.cloudstack.storage.datastore.ObjectInDataStoreManager;
import org.apache.cloudstack.storage.datastore.db.SnapshotDataStoreDao;
import org.apache.cloudstack.storage.datastore.db.SnapshotDataStoreVO;
import org.apache.cloudstack.storage.to.SnapshotObjectTO;
import org.apache.log4j.Logger;

import com.cloud.agent.api.Answer;
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
    @Inject
    SnapshotDataStoreDao snapshotStore;
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
		return this.ojbectInStoreMgr.findObject(this, getDataStore()).getInstallPath();
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
		SnapshotDataStoreVO snapshotStoreVO = this.snapshotStore.findBySnapshot(this.getId(), this.getDataStore().getRole());
		return snapshotStoreVO.getParentSnapshotId();
	}

	public SnapshotVO getSnapshotVO(){
	    return this.snapshot;
	}

    @Override
    public DataTO getTO() {
        // TODO Auto-generated method stub
        return null;
    }

    @Override
    public void processEvent(ObjectInDataStoreStateMachine.Event event, Answer answer) {
    	SnapshotDataStoreVO snapshotStore = this.snapshotStore.findByStoreSnapshot(this.getDataStore().getRole(), 
    		   this.getDataStore().getId(), this.getId());
    	if (answer instanceof CreateObjectAnswer) {
    		SnapshotObjectTO snapshotTO = (SnapshotObjectTO)((CreateObjectAnswer) answer).getData();
    		snapshotStore.setInstallPath(snapshotTO.getPath());
    		this.snapshotStore.update(snapshotStore.getId(), snapshotStore);
    	} else {
    		throw new CloudRuntimeException("Unknown answer: " + answer.getClass());
    	}
    	this.processEvent(event);
    }

    @Override
    public ObjectInDataStoreStateMachine.State getStatus() {
       return this.ojbectInStoreMgr.findObject(this, store).getObjectInStoreState();
    }

	@Override
	public void addPayload(Object data) {
		// TODO Auto-generated method stub
		
	}
    
}
