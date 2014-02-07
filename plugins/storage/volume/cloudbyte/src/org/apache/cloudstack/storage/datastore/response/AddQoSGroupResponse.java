package org.apache.cloudstack.storage.datastore.response;


import org.apache.cloudstack.storage.datastore.model.QoSGroup;

import com.google.gson.annotations.SerializedName;

public class AddQoSGroupResponse {

	@SerializedName("addqosgroupresponse")
	private QoSGroup qosGroup;
	
	
	public QoSGroup getQoSGroup() {
		return qosGroup;
	}
}
