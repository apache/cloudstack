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

import java.util.ArrayList;
import java.util.List;

/**
 * An S3AccessControlList is simply a holder of grants depicted as instances of S3Grant.
 */
public class S3AccessControlList {
	private List<S3Grant> list = new ArrayList<S3Grant>();
	
	public S3AccessControlList() {
	}
	
	public S3Grant[] getGrants() {
		return list.toArray(new S3Grant[0]);
	}
	
	public void addGrant(S3Grant grant) {
		list.add(grant);
	}
	
	public int size() {
		return list.size();
	}
}
