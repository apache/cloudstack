package org.apache.cloudstack.storage.datastore.response;


import org.apache.cloudstack.storage.datastore.model.JobId;

import com.google.gson.annotations.SerializedName;

public class DeleteVolumeResponse {

	@SerializedName("deleteFileSystemResponse")
	private JobId jobId;
	
	public String getJobId(){		
		return jobId.getJobid();
	}
	
	
}
