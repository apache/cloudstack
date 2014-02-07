package org.apache.cloudstack.storage.datastore.model;

import com.google.gson.annotations.SerializedName;

public class Tsm {

	@SerializedName("id")
	private String uuid;
	
	@SerializedName("name")
	private String name;
	
	@SerializedName("datasetid")
	private String datasetid;
	
	@SerializedName("volumes")
	private VolumeProperties[] volumeProperties;
	
	public String getUuid() {
		return uuid;
	}
	
	public String getName() {
		return name;
	}
	
	public String getDatasetid() {
		return datasetid;
	}
	
    public boolean checkvolume() {
		
		if(volumeProperties != null){
			return true;
		}
		else{
			return false;
		}
		
	}
	
	public VolumeProperties getVolumeProperties(int i) {
		return volumeProperties[i];
	}
	
}
