package org.apache.cloudstack.storage.datastore.model;


import com.google.gson.annotations.SerializedName;

public class VolumeProperties {

	@SerializedName("id")
	private String id;
	
	@SerializedName("name")
	private String name;
	
	public String getid() {
		return id;
	}
	
	public String getName() {
		return name;
	}
	
	
}
