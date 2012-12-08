package org.apache.cloudstack.storage.datastore.driver;

import java.util.Map;

import org.apache.cloudstack.engine.subsystem.api.storage.EndPoint;
import org.apache.cloudstack.storage.datastore.PrimaryDataStore;
import org.apache.cloudstack.storage.volume.TemplateOnPrimaryDataStoreInfo;
import org.apache.cloudstack.storage.volume.VolumeObject;

public class SolidfirePrimaryDataStoreDriver implements PrimaryDataStoreDriver {

	@Override
	public boolean createVolume(VolumeObject vol) {
		// TODO Auto-generated method stub
		return false;
	}

	@Override
	public boolean createVolumeFromBaseImage(VolumeObject volume,
			TemplateOnPrimaryDataStoreInfo template) {
		// TODO Auto-generated method stub
		return false;
	}

	@Override
	public boolean deleteVolume(VolumeObject vo) {
		// TODO Auto-generated method stub
		return false;
	}

	@Override
	public String grantAccess(VolumeObject vol, EndPoint ep) {
		// TODO Auto-generated method stub
		return null;
	}

	@Override
	public boolean revokeAccess(VolumeObject vol, EndPoint ep) {
		// TODO Auto-generated method stub
		return false;
	}

    @Override
    public long getCapacity() {
        // TODO Auto-generated method stub
        return 0;
    }

    @Override
    public long getAvailableCapacity() {
        // TODO Auto-generated method stub
        return 0;
    }

    @Override
    public boolean initialize(Map<String, String> params) {
        // TODO Auto-generated method stub
        return false;
    }

    @Override
    public boolean grantAccess(EndPoint ep) {
        // TODO Auto-generated method stub
        return false;
    }

    @Override
    public boolean revokeAccess(EndPoint ep) {
        // TODO Auto-generated method stub
        return false;
    }

    @Override
    public void setDataStore(PrimaryDataStore dataStore) {
        // TODO Auto-generated method stub
        
    }

}
