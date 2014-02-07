package org.apache.cloudstack.storage.datastore.response;


import org.apache.cloudstack.storage.datastore.model.Capabilities;

import com.google.gson.annotations.SerializedName;

public class ListCapabilitiesResponse {

	@SerializedName("listcapabilitiesresponse")
	private Capabilities capabilities;
	
	public Capabilities getCapabilities() {
		return capabilities;
	}
}
