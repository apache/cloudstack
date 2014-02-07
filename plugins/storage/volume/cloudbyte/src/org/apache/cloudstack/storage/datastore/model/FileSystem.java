package org.apache.cloudstack.storage.datastore.model;

import java.util.HashMap;

import com.google.gson.annotations.SerializedName;

public class FileSystem {

	@SerializedName("id")
	private String uuid;
	
	@SerializedName("name")
	private String name;
	
	@SerializedName("timestamp")
	private String timestamp;
	
	@SerializedName("iqnname")
	private String iqnname;
	
	@SerializedName("filesystemproperties")
	private HashMap<String, String>[] filesystemproperties; 
	
	public String getUuid() {
		return uuid;
	}
	
	public String getName() {
		return name;
	}
	
	public String getIqn() {
		return iqnname;
	}
	
	public String getTimestamp() {
		return timestamp;
	}
	
}
