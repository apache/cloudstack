package org.apache.cloudstack.storage.datastore.driver;

import org.apache.cloudstack.storage.EndPoint;
import org.apache.cloudstack.storage.volume.Volume;

public interface PrimaryDataStoreDriver {
	boolean createVolume(Volume vol);
	boolean deleteVolume(Volume vo);
	String grantAccess(Volume vol,  EndPoint ep);
	boolean revokeAccess(Volume vol, EndPoint ep);
}
