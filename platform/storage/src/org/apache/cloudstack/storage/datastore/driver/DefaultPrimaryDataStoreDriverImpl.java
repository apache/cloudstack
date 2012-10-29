package org.apache.cloudstack.storage.datastore.driver;

import org.apache.cloudstack.storage.EndPoint;
import org.apache.cloudstack.storage.volume.Volume;
import org.springframework.stereotype.Component;

@Component
public class DefaultPrimaryDataStoreDriverImpl implements
		PrimaryDataStoreDriver {

	@Override
	public boolean createVolume(Volume vol) {
		// TODO Auto-generated method stub
		return false;
	}

	@Override
	public boolean deleteVolume(Volume vo) {
		// TODO Auto-generated method stub
		return false;
	}

	@Override
	public String grantAccess(Volume vol, EndPoint ep) {
		// TODO Auto-generated method stub
		return null;
	}

	@Override
	public boolean revokeAccess(Volume vol, EndPoint ep) {
		// TODO Auto-generated method stub
		return false;
	}

}
