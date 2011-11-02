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

import com.cloud.api.ApiConstants;
import com.cloud.api.IdentityProxy;
import com.cloud.serializer.Param;
import com.google.gson.annotations.SerializedName;

public class CloudIdentifierResponse extends BaseResponse {
	
    @SerializedName(ApiConstants.USER_ID) @Param(description="the user ID for the cloud identifier")
    private IdentityProxy userId = new IdentityProxy("user");
    
    @SerializedName("cloudidentifier") @Param(description="the cloud identifier")
    private String cloudIdentifier;
    
    @SerializedName("signature") @Param(description="the signed response for the cloud identifier")
    private String signature;
    
    public Long getUserId() {
        return userId.getValue();
    }

    public void setUserId(Long userId) {
        this.userId.setValue(userId);
    }
    
    public String getCloudIdentifier() {
        return cloudIdentifier;
    }

    public void setCloudIdentifier(String cloudIdentifier) {
        this.cloudIdentifier = cloudIdentifier;
    }
    
    public String getSignature() {
        return signature;
    }

    public void setSignature(String signature) {
        this.signature = signature;
    }

}
