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

import com.cloud.utils.IdentityProxy;
import com.cloud.serializer.Param;
import com.google.gson.annotations.SerializedName;

public class ExceptionResponse extends BaseResponse {
	@SerializedName("uuid") @Param(description="uuid associated with this error")
	private IdentityProxy id;
	
    @SerializedName("errorcode") @Param(description="numeric code associated with this error")
    private Integer errorCode;

    @SerializedName("errortext") @Param(description="the text associated with this error")
    private String errorText;

    public Integer getErrorCode() {
        return errorCode;
    }

    public void setErrorCode(Integer errorCode) {
        this.errorCode = errorCode;
    }

    public String getErrorText() {
        return errorText;
    }

    public void setErrorText(String errorText) {
        this.errorText = errorText;
    }
    
    public void setProxyObject(String table_name, Long id) {
		this.id = new IdentityProxy();
		this.id.setTableName(table_name);
		this.id.setValue(id);
		return;
	}
	
	public IdentityProxy getProxyObject() {
		return id;
	}
}
