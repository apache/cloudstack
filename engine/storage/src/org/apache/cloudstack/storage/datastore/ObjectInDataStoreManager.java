package org.apache.cloudstack.storage.datastore;

import org.apache.cloudstack.engine.subsystem.api.storage.VolumeInfo;
import org.apache.cloudstack.storage.db.ObjectInDataStoreVO;
import org.apache.cloudstack.storage.image.TemplateInfo;
import org.apache.cloudstack.storage.snapshot.SnapshotInfo;
import org.apache.cloudstack.storage.volume.ObjectInDataStoreStateMachine;

public interface ObjectInDataStoreManager {
    public TemplateInfo create(TemplateInfo template, DataStore dataStore);
    public ObjectInDataStoreVO create(VolumeInfo volume, DataStore dataStore);
    public ObjectInDataStoreVO create(SnapshotInfo snapshot, DataStore dataStore);
    public TemplateInfo findTemplate(TemplateInfo template, DataStore dataStore);
    public VolumeInfo findVolume(VolumeInfo volume, DataStore dataStore);
    public SnapshotInfo findSnapshot(SnapshotInfo snapshot, DataStore dataStore);
    public boolean update(TemplateInfo vo, ObjectInDataStoreStateMachine.Event event);
}
