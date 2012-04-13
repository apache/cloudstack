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

public class EC2DescribeVolumes {

	private List<String> volumeSet = new ArrayList<String>();    // a list of strings identifying volume ids
	private EC2VolumeFilterSet vfs = null;

	public EC2DescribeVolumes() {
	}

	public void addVolumeId( String param ) {
		volumeSet.add( param );
	}
	
	public String[] getVolumeSet() {
		return volumeSet.toArray(new String[0]);
	}
	
	public EC2VolumeFilterSet getFilterSet() {
		return vfs;
	}
	
	public void setFilterSet( EC2VolumeFilterSet param ) {
		vfs = param;
	}
}
