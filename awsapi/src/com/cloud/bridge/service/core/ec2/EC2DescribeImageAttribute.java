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



import com.cloud.bridge.service.core.ec2.EC2ImageAttributes.ImageAttribute;

public class EC2DescribeImageAttribute {

	private String imageId        = null;     //AMI id
	private ImageAttribute attribute = null; 
	
	public EC2DescribeImageAttribute() {
	}

    public void setAttribute(ImageAttribute attribute) {
        this.attribute = attribute;
    }

    public ImageAttribute getAttribute() {
        return attribute;
    }

    public void setImageId(String imageId) {
        this.imageId = imageId;
    }

    public String getImageId() {
        return imageId;
    }
	
}
