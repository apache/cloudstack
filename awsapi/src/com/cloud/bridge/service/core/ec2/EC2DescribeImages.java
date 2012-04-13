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

public class EC2DescribeImages {

	private List<String> executableBySet = new ArrayList<String>();;    // a list of strings identifying users
	private List<String> imageSet        = new ArrayList<String>();     // a list of AMI id's
	private List<String> ownersSet       = new ArrayList<String>();     // a list of AMI owner id's
	       
	public EC2DescribeImages() {
	}

	public void addExecutableBySet( String param ) {
		executableBySet.add( param );
	}
	
	public String[] getExcutableBySet() {
		return executableBySet.toArray(new String[0]);
	}
	
	public void addImageSet( String param ) {
		imageSet.add( param );
	}
	
	public String[] getImageSet() {
		return imageSet.toArray(new String[0]);
	}
	
	public void addOwnersSet( String param ) {
		ownersSet.add( param );
	}
	
	public String[] getOwnersSet() {
		return ownersSet.toArray(new String[0]);
	}
}
