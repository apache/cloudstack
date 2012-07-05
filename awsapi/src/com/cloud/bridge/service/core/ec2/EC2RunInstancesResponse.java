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
package com.cloud.bridge.service.core.ec2;

import java.util.ArrayList;
import java.util.List;

import com.cloud.bridge.service.core.ec2.EC2Instance;

public class EC2RunInstancesResponse {

	private List<EC2Instance> instanceSet = new ArrayList<EC2Instance>();    

	public EC2RunInstancesResponse() {
	}
	
	public void addInstance( EC2Instance param ) {
		instanceSet.add( param );
	}
	
	public EC2Instance[] getInstanceSet() {
		return instanceSet.toArray(new EC2Instance[0]);
	}
}
