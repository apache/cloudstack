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

public class CloudStackKeyPair {
	@SerializedName(ApiConstants.FINGERPRINT)
	private String fingerprint;
	@SerializedName(ApiConstants.NAME)
	private String name;
	@SerializedName(ApiConstants.PRIVATE_KEY)
	private String privatekey;


	/**
	 *  Empty Constructor...
	 */
	public CloudStackKeyPair() {
		
	}

	/**
	 * @return the fingerprint
	 */
	public String getFingerprint() {
		return fingerprint;
	}

	/**
	 * @return the name
	 */
	public String getName() {
		return name;
	}

	/**
	 * @return the privatekey
	 */
	public String getPrivatekey() {
		return privatekey;
	}

}
