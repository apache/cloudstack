package org.apache.cloudstack.storage.datastore;

import org.apache.cloudstack.engine.subsystem.api.storage.DataObject;
import org.apache.cloudstack.engine.subsystem.api.storage.DataObjectType;
import org.apache.cloudstack.engine.subsystem.api.storage.DataStore;
import org.apache.cloudstack.engine.subsystem.api.storage.DataStoreRole;
import org.apache.cloudstack.engine.subsystem.api.storage.VolumeInfo;
import org.apache.cloudstack.storage.db.ObjectInDataStoreVO;
import org.apache.cloudstack.storage.image.TemplateInfo;
import org.apache.cloudstack.storage.snapshot.SnapshotInfo;
import org.apache.cloudstack.storage.volume.ObjectInDataStoreStateMachine;
import org.apache.cloudstack.storage.volume.ObjectInDataStoreStateMachine.Event;

import com.cloud.utils.fsm.NoTransitionException;

public interface ObjectInDataStoreManager {
    public TemplateInfo create(TemplateInfo template, DataStore dataStore);
    public VolumeInfo create(VolumeInfo volume, DataStore dataStore);
    public SnapshotInfo create(SnapshotInfo snapshot, DataStore dataStore);
    public ObjectInDataStoreVO findObject(long objectId, DataObjectType type,
            long dataStoreId, DataStoreRole role);
    public boolean update(DataObject vo, Event event) throws NoTransitionException;
}
