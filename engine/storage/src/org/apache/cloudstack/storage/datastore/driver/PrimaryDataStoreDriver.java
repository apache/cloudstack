package org.apache.cloudstack.storage.datastore.driver;

import org.apache.cloudstack.engine.subsystem.api.storage.EndPoint;
import org.apache.cloudstack.storage.volume.TemplateOnPrimaryDataStoreInfo;
import org.apache.cloudstack.storage.volume.VolumeObject;


public interface PrimaryDataStoreDriver {
	boolean createVolume(VolumeObject vol);
	boolean createVolumeFromBaseImage(VolumeObject volume, TemplateOnPrimaryDataStoreInfo template);
	boolean deleteVolume(VolumeObject vo);
	String grantAccess(VolumeObject vol,  EndPoint ep);
	boolean revokeAccess(VolumeObject vol, EndPoint ep);
}
