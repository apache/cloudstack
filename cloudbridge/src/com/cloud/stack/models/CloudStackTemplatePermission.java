/*
 * Copyright (C) 2011 Citrix Systems, Inc.  All rights reserved.
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *      http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package com.cloud.stack.models;

import java.util.List;

import com.google.gson.annotations.SerializedName;

/**
 * @author slriv
 *
 */
public class CloudStackTemplatePermission {
	@SerializedName(ApiConstants.ID)
	private Long id;
	@SerializedName(ApiConstants.IS_PUBLIC)
	private Boolean isPublic;
	@SerializedName(ApiConstants.DOMAIN_ID)
	private Long domainId;
	@SerializedName(ApiConstants.ACCOUNT)
	private List<CloudStackAccount> accounts;
	

	/**
	 * 
	 */
	public CloudStackTemplatePermission() {
	}


	/**
	 * @return the id
	 */
	public Long getId() {
		return id;
	}


	/**
	 * @return the isPublic
	 */
	public Boolean getIsPublic() {
		return isPublic;
	}


	/**
	 * @return the domainId
	 */
	public Long getDomainId() {
		return domainId;
	}


	/**
	 * @return the accounts
	 */
	public List<CloudStackAccount> getAccounts() {
		return accounts;
	}

}
