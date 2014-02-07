package org.apache.cloudstack.storage.datastore.model;

import com.google.gson.annotations.SerializedName;

public class QoSGroupWrapper {

	@SerializedName("qosgroup")
	private QoSGroup qoSGroup;
	
	public QoSGroup getQosGroup() {
		
		return qoSGroup;
	}
	
	
}
