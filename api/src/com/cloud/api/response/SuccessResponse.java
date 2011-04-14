/**
 *  Copyright (C) 2010 Cloud.com, Inc.  All rights reserved.
 * 
 * This software is licensed under the GNU General Public License v3 or later.
 * 
 * It is free software: you can redistribute it and/or modify
 * it under the terms of the GNU General Public License as published by
 * the Free Software Foundation, either version 3 of the License, or any later version.
 * This program is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 * GNU General Public License for more details.
 * 
 * You should have received a copy of the GNU General Public License
 * along with this program.  If not, see <http://www.gnu.org/licenses/>.
 * 
 */

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
    
    public SuccessResponse() {}
    
    public SuccessResponse(String responseName) {
    	super.setResponseName(responseName);
    }
}
