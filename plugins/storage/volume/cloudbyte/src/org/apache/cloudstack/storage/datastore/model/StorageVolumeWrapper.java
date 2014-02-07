package org.apache.cloudstack.storage.datastore.model;

import com.google.gson.annotations.SerializedName;

public class StorageVolumeWrapper {

	@SerializedName("storage")
	private StorageVolume storageVolume;
	
	public StorageVolume getStorageVolume()
	{
		return storageVolume;
	}
		
}
