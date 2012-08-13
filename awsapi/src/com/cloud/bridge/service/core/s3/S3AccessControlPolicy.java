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
package com.cloud.bridge.service.core.s3;

public class S3AccessControlPolicy  extends S3Response {
	protected S3CanonicalUser owner;
	protected S3Grant[] grants;
	
	public S3AccessControlPolicy() {
		super();
	}

	public S3CanonicalUser getOwner() {
		return owner;
	}

	public void setOwner(S3CanonicalUser owner) {
		this.owner = owner;
	}

	public S3Grant[] getGrants() {
		return grants;
	}

	public void setGrants(S3Grant[] grants) {
		this.grants = grants;
	}
}
