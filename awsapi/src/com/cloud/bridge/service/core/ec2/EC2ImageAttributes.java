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

import java.util.List;

public class EC2ImageAttributes {

    private String imageId;
    private String description;
    private boolean isPublic;
    private List<String> accountNamesWithLaunchPermission;
    private String domainId;

    public enum ImageAttribute {
        description, launchPermission, kernel, ramdisk, productCodes, blockDeviceMapping
    };

    public EC2ImageAttributes() {
        imageId = null;
        description = null;
        isPublic = false;
        accountNamesWithLaunchPermission = null;
        domainId = null;
    }

    public void setImageId(String imageId) {
        this.imageId = imageId;
    }

    public String getImageId() {
        return this.imageId;
    }

    public void setDescription(String description) {
        this.description = description;
    }

    public String getDescription() {
        return this.description;
    }

    public void setIsPublic(boolean isPublic) {
        this.isPublic = isPublic;
    }

    public boolean getIsPublic() {
        return this.isPublic;
    }

    public String getDomainId() {
        return domainId;
    }

    public void setDomainId(String domainId) {
        this.domainId = domainId;
    }

    public void setAccountNamesWithLaunchPermission(List<String> accountNamesWithLaunchPermission) {
        this.accountNamesWithLaunchPermission = accountNamesWithLaunchPermission;
    }

    public List<String> getAccountNamesWithLaunchPermission() {
        return accountNamesWithLaunchPermission;
    }

}
