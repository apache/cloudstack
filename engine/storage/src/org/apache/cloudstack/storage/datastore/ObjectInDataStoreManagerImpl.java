package org.apache.cloudstack.storage.datastore;

import org.apache.cloudstack.engine.subsystem.api.storage.VolumeInfo;
import org.apache.cloudstack.storage.db.ObjectInDataStoreDao;
import org.apache.cloudstack.storage.db.ObjectInDataStoreVO;
import org.apache.cloudstack.storage.image.TemplateInfo;
import org.apache.cloudstack.storage.snapshot.SnapshotInfo;
import org.apache.cloudstack.storage.volume.ObjectInDataStoreStateMachine.Event;
import org.springframework.stereotype.Component;



@Component
public  class ObjectInDataStoreManagerImpl implements ObjectInDataStoreManager {
    @Inject
    ObjectInDataStoreDao objectDataStoreDao;
    @Override
    public TemplateInfo create(TemplateInfo template, DataStore dataStore) {
        ObjectInDataStoreVO vo = new ObjectInDataStoreVO();
        vo.setDataStoreId(dataStore.getId());
        vo.setDataStoreType(dataStore.getRole());
        vo.setObjectId(template.getId());
        vo.setObjectType("template");
        vo = objectDataStoreDao.persist(vo);
        TemplateInDataStore tmpl = new TemplateInDataStore(template, dataStore, vo);
        return tmpl;
    }

    @Override
    public ObjectInDataStoreVO create(VolumeInfo volume, DataStore dataStore) {
        // TODO Auto-generated method stub
        return null;
    }

    @Override
    public ObjectInDataStoreVO create(SnapshotInfo snapshot, DataStore dataStore) {
        // TODO Auto-generated method stub
        return null;
    }

    @Override
    public TemplateInfo findTemplate(TemplateInfo template, DataStore dataStore) {
        // TODO Auto-generated method stub
        return null;
    }

    @Override
    public VolumeInfo findVolume(VolumeInfo volume, DataStore dataStore) {
        // TODO Auto-generated method stub
        return null;
    }

    @Override
    public SnapshotInfo findSnapshot(SnapshotInfo snapshot, DataStore dataStore) {
        // TODO Auto-generated method stub
        return null;
    }

    @Override
    public boolean update(TemplateInfo vo, Event event) {
        // TODO Auto-generated method stub
        return false;
    }

}
