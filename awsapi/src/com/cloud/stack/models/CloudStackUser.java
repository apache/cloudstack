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
public class CloudStackUser {

	@SerializedName(ApiConstants.ID)
	private String id;
	@SerializedName(ApiConstants.ACCOUNT)
	private String account;
	@SerializedName(ApiConstants.ACCOUNT_TYPE)
	private String accountType;
	@SerializedName(ApiConstants.API_KEY)
	private String apikey;
	@SerializedName(ApiConstants.CREATED)
	private String created;
	@SerializedName(ApiConstants.DOMAIN)
	private String domain;
	@SerializedName(ApiConstants.DOMAIN_ID)
	private String domainId;
	@SerializedName(ApiConstants.EMAIL)
	private String email;
	@SerializedName(ApiConstants.FIRSTNAME)
	private String firstname;
	@SerializedName(ApiConstants.LASTNAME)
	private String lastname;
	@SerializedName(ApiConstants.SECRET_KEY)
	private String secretkey;
	@SerializedName(ApiConstants.STATE)
	private String state;
	@SerializedName(ApiConstants.TIMEZONE)
	private String timeZone;
	@SerializedName(ApiConstants.USERNAME)
	private String username;
	
	/**
	 * 
	 */
	public CloudStackUser() {
	}

	/**
	 * @return the id
	 */
	public String getId() {
		return id;
	}

	/**
	 * @return the account
	 */
	public String getAccount() {
		return account;
	}

	/**
	 * @return the accountType
	 */
	public String getAccountType() {
		return accountType;
	}

	/**
	 * @return the apikey
	 */
	public String getApikey() {
		return apikey;
	}

	/**
	 * @return the created
	 */
	public String getCreated() {
		return created;
	}

	/**
	 * @return the domain
	 */
	public String getDomain() {
		return domain;
	}

	/**
	 * @return the domainId
	 */
	public String getDomainId() {
		return domainId;
	}

	/**
	 * @return the email
	 */
	public String getEmail() {
		return email;
	}

	/**
	 * @return the firstname
	 */
	public String getFirstname() {
		return firstname;
	}

	/**
	 * @return the lastname
	 */
	public String getLastname() {
		return lastname;
	}

	/**
	 * @return the secretkey
	 */
	public String getSecretkey() {
		return secretkey;
	}

	/**
	 * @return the state
	 */
	public String getState() {
		return state;
	}

	/**
	 * @return the timeZone
	 */
	public String getTimeZone() {
		return timeZone;
	}

	/**
	 * @return the username
	 */
	public String getUsername() {
		return username;
	}

}
