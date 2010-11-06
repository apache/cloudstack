package com.cloud.api.response;

import com.cloud.serializer.Param;
import com.google.gson.annotations.SerializedName;

public class SuccessResponse extends BaseResponse {
	 @SerializedName("success") @Param(description="true if operation is executed successfully")
	 private Boolean success = true;
	 
     @SerializedName("displaytext") @Param(description="any text associated with the success or failure")
     private String displayText;
     
	 public Boolean getSuccess() {
	 	return success;
	 }

	public void setSuccess(Boolean success) {
		this.success = success;
	}

	public String getDisplayText() {
	    return displayText;
	}

    public void setDisplayText(String displayText) {
        this.displayText = displayText;
    }
    
    public SuccessResponse(String responseName) {
    	super.setResponseName(responseName);
    }
}
