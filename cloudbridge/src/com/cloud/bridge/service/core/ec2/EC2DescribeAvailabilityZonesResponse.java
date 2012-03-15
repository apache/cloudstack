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
package com.cloud.bridge.service.core.ec2;

import java.util.ArrayList;
import java.util.List;

public class EC2DescribeAvailabilityZonesResponse {

	private List<String> zoneIds   = new ArrayList<String>();    
	private List<String> zoneNames = new ArrayList<String>();    

	public EC2DescribeAvailabilityZonesResponse() {
	}
	
    public void addZone(String id, String name) {
        zoneIds.add(id);
        zoneNames.add(name);
	}
	
	/**
	 * The Amazon API only cares about the names of zones not their ID value.
	 * 
	 * @return an array containing a set of zone names
	 */
	public String[] getZoneSet() {
		return zoneNames.toArray(new String[0]);
	}
	
    public String getZoneIdAt(int index) {
        if (zoneIds.isEmpty() || index >= zoneIds.size()) {
            return null;
        }
        return zoneIds.get(index);
	}
}
