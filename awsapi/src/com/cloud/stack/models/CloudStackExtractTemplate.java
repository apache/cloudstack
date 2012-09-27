// Licensed to the Apache Software Foundation (ASF) under one
// or more contributor license agreements.  See the NOTICE file
// distributed with this work for additional information
// regarding copyright ownership.  The ASF licenses this file
// to you under the Apache License, Version 2.0 (the
// "License"); you may not use this file except in compliance
// with the License.  You may obtain a copy of the License at
//
//   http://www.apache.org/licenses/LICENSE-2.0
//
// Unless required by applicable law or agreed to in writing,
// software distributed under the License is distributed on an
// "AS IS" BASIS, WITHOUT WARRANTIES OR CONDITIONS OF ANY
// KIND, either express or implied.  See the License for the
// specific language governing permissions and limitations
// under the License.

package com.cloud.stack.models;

import com.google.gson.annotations.SerializedName;

public class CloudStackExtractTemplate {
	@SerializedName(ApiConstants.ID)
	Long id;
	@SerializedName(ApiConstants.ACCOUNT_ID)
	Long accountId;
	@SerializedName(ApiConstants.CREATED)
	String created;
	@SerializedName(ApiConstants.EXTRACT_ID)
	Long extractId;
	@SerializedName(ApiConstants.EXTRACT_MODE)
	String extractMode;
	@SerializedName(ApiConstants.NAME)
	String name;
	@SerializedName(ApiConstants.STATE)
	String state;
	@SerializedName(ApiConstants.STATUS)
	String status;
	@SerializedName(ApiConstants.STORAGE_TYPE)
	String storageType;
	@SerializedName(ApiConstants.UPLOAD_PERCENTAGE)
	String uploadPercentage;
	@SerializedName(ApiConstants.URL)
	String url;
	@SerializedName(ApiConstants.ZONE_ID)
	Long zoneId;
	@SerializedName(ApiConstants.ZONE_NAME)
	String zoneName;
	
	/**
	 * 
	 */
	public CloudStackExtractTemplate() {
		// TODO Auto-generated constructor stub
	}

	/**
	 * @return the id
	 */
	public Long getId() {
		return id;
	}

	/**
	 * @return the accountId
	 */
	public Long getAccountId() {
		return accountId;
	}

	/**
	 * @return the created
	 */
	public String getCreated() {
		return created;
	}

	/**
	 * @return the extractId
	 */
	public Long getExtractId() {
		return extractId;
	}

	/**
	 * @return the extractMode
	 */
	public String getExtractMode() {
		return extractMode;
	}

	/**
	 * @return the name
	 */
	public String getName() {
		return name;
	}

	/**
	 * @return the state
	 */
	public String getState() {
		return state;
	}

	/**
	 * @return the status
	 */
	public String getStatus() {
		return status;
	}

	/**
	 * @return the storageType
	 */
	public String getStorageType() {
		return storageType;
	}

	/**
	 * @return the uploadPercentage
	 */
	public String getUploadPercentage() {
		return uploadPercentage;
	}

	/**
	 * @return the url
	 */
	public String getUrl() {
		return url;
	}

	/**
	 * @return the zoneId
	 */
	public Long getZoneId() {
		return zoneId;
	}

	/**
	 * @return the zoneName
	 */
	public String getZoneName() {
		return zoneName;
	}
}
