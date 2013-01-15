package org.apache.cloudstack.storage.image.store.lifecycle;

import java.util.Map;

import javax.inject.Inject;

import org.apache.cloudstack.engine.subsystem.api.storage.ClusterScope;
import org.apache.cloudstack.engine.subsystem.api.storage.DataStore;
import org.apache.cloudstack.engine.subsystem.api.storage.ZoneScope;
import org.apache.cloudstack.storage.image.db.ImageDataStoreDao;

public class DefaultImageDataStoreLifeCycle implements ImageDataStoreLifeCycle {
    @Inject
	protected ImageDataStoreDao imageStoreDao;
	
	public DefaultImageDataStoreLifeCycle() {
	}


    @Override
    public boolean initialize(DataStore store, Map<String, String> dsInfos) {
        // TODO Auto-generated method stub
        return false;
    }


    @Override
    public boolean attachCluster(DataStore store, ClusterScope scope) {
        // TODO Auto-generated method stub
        return false;
    }


    @Override
    public boolean attachZone(DataStore dataStore, ZoneScope scope) {
        // TODO Auto-generated method stub
        return false;
    }


    @Override
    public boolean dettach() {
        // TODO Auto-generated method stub
        return false;
    }


    @Override
    public boolean unmanaged() {
        // TODO Auto-generated method stub
        return false;
    }


    @Override
    public boolean maintain() {
        // TODO Auto-generated method stub
        return false;
    }


    @Override
    public boolean cancelMaintain() {
        // TODO Auto-generated method stub
        return false;
    }


    @Override
    public boolean deleteDataStore() {
        // TODO Auto-generated method stub
        return false;
    }

}
