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

public class EC2DescribeKeyPairsResponse {
	
	protected List<EC2SSHKeyPair> keyPairSet = new ArrayList<EC2SSHKeyPair>();    

	/**
	 * 
	 */
	public EC2DescribeKeyPairsResponse() {
	}
	
	public void reset() {
		keyPairSet.clear();
	}
	
	public void addKeyPair( EC2SSHKeyPair param ) {
		keyPairSet.add( param );
	}
	
	public EC2SSHKeyPair[] getKeyPairSet() {
		return keyPairSet.toArray(new EC2SSHKeyPair[0]);
	}

}
