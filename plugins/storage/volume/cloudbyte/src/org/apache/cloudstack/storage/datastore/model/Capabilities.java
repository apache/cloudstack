package org.apache.cloudstack.storage.datastore.model;

import java.util.HashMap;

import com.google.gson.annotations.SerializedName;

public class Capabilities {

	@SerializedName("capability")
	private HashMap<String, String> capabilites;
	
	public String getVersion() {
		return capabilites.get("cloudByteVersion");
	}
}
