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
package com.cloud.storage.upload.params;

import com.cloud.hypervisor.Hypervisor;

import java.util.Map;

public interface UploadParams {
    boolean isIso();
    long getUserId();
    String getName();
    String getDisplayText();
    Integer getBits();
    boolean isPasswordEnabled();
    boolean requiresHVM();
    String getUrl();
    boolean isPublic();
    boolean isFeatured();
    boolean isExtractable();
    String getFormat();
    Long getGuestOSId();
    Long getZoneId();
    Hypervisor.HypervisorType getHypervisorType();
    String getChecksum();
    boolean isBootable();
    String getTemplateTag();
    long getTemplateOwnerId();
    Map getDetails();
    boolean isSshKeyEnabled();
    String getImageStoreUuid();
    boolean isDynamicallyScalable();
    boolean isRoutingType();
    boolean isDirectDownload();
    boolean isDeployAsIs();
}
