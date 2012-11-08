package org.apache.cloudstack.storage.datastore.driver;

import org.apache.cloudstack.engine.cloud.entity.api.VolumeEntity;
import org.apache.cloudstack.engine.subsystem.api.storage.EndPoint;


public interface PrimaryDataStoreDriver {
	boolean createVolume(VolumeEntity vol);
	boolean deleteVolume(VolumeEntity vo);
	String grantAccess(VolumeEntity vol,  EndPoint ep);
	boolean revokeAccess(VolumeEntity vol, EndPoint ep);
}
