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

public abstract class UploadParamsBase implements UploadParams {

    private boolean isIso;
    private long userId;
    private String name;
    private String displayText;
    private Integer bits;
    private Boolean passwordEnabled;
    private Boolean requiresHVM;
    private Boolean isPublic;
    private Boolean featured;
    private Boolean isExtractable;
    private String format;
    private Long guestOSId;
    private Long zoneId;
    private Hypervisor.HypervisorType hypervisorType;
    private String checksum;
    private Boolean bootable;
    private String templateTag;
    private long templateOwnerId;
    private Map details;
    private Boolean sshkeyEnabled;
    private Boolean isDynamicallyScalable;
    private Boolean isRoutingType;

    UploadParamsBase(long userId, String name, String displayText,
                               Integer bits, Boolean passwordEnabled, Boolean requiresHVM,
                               Boolean isPublic, Boolean featured,
                               Boolean isExtractable, String format, Long guestOSId,
                               Long zoneId, Hypervisor.HypervisorType hypervisorType, String checksum,
                               String templateTag, long templateOwnerId,
                               Map details, Boolean sshkeyEnabled,
                               Boolean isDynamicallyScalable, Boolean isRoutingType) {
        this.userId = userId;
        this.name = name;
        this.displayText = displayText;
        this.bits = bits;
        this.passwordEnabled = passwordEnabled;
        this.requiresHVM = requiresHVM;
        this.isPublic = isPublic;
        this.featured = featured;
        this.isExtractable = isExtractable;
        this.format = format;
        this.guestOSId = guestOSId;
        this.zoneId = zoneId;
        this.hypervisorType = hypervisorType;
        this.checksum = checksum;
        this.templateTag = templateTag;
        this.templateOwnerId = templateOwnerId;
        this.details = details;
        this.sshkeyEnabled = sshkeyEnabled;
        this.isDynamicallyScalable = isDynamicallyScalable;
        this.isRoutingType = isRoutingType;
    }

    UploadParamsBase(long userId, String name, String displayText, Boolean isPublic, Boolean isFeatured,
                               Boolean isExtractable, Long osTypeId, Long zoneId, Boolean bootable, long ownerId) {
        this.userId = userId;
        this.name = name;
        this.displayText = displayText;
        this.isPublic = isPublic;
        this.featured = isFeatured;
        this.isExtractable = isExtractable;
        this.guestOSId = osTypeId;
        this.zoneId = zoneId;
        this.bootable = bootable;
        this.templateOwnerId = ownerId;
    }

    @Override
    public boolean isIso() {
        return isIso;
    }

    @Override
    public long getUserId() {
        return userId;
    }

    @Override
    public String getName() {
        return name;
    }

    @Override
    public String getDisplayText() {
        return displayText;
    }

    @Override
    public Integer getBits() {
        return bits;
    }

    @Override
    public Boolean isPasswordEnabled() {
        return passwordEnabled;
    }

    @Override
    public Boolean requiresHVM() {
        return requiresHVM;
    }

    @Override
    public String getUrl() {
        return null;
    }

    @Override
    public Boolean isPublic() {
        return isPublic;
    }

    @Override
    public Boolean isFeatured() {
        return featured;
    }

    @Override
    public Boolean isExtractable() {
        return isExtractable;
    }

    @Override
    public String getFormat() {
        return format;
    }

    @Override
    public Long getGuestOSId() {
        return guestOSId;
    }

    @Override
    public Long getZoneId() {
        return zoneId;
    }

    @Override
    public Hypervisor.HypervisorType getHypervisorType() {
        return hypervisorType;
    }

    @Override
    public String getChecksum() {
        return checksum;
    }

    @Override
    public Boolean isBootable() {
        return bootable;
    }

    @Override
    public String getTemplateTag() {
        return templateTag;
    }

    @Override
    public long getTemplateOwnerId() {
        return templateOwnerId;
    }

    @Override
    public Map getDetails() {
        return details;
    }

    @Override
    public Boolean isSshKeyEnabled() {
        return sshkeyEnabled;
    }

    @Override
    public String getImageStoreUuid() {
        return null;
    }

    @Override
    public Boolean isDynamicallyScalable() {
        return isDynamicallyScalable;
    }

    @Override
    public Boolean isRoutingType() {
        return isRoutingType;
    }

    @Override
    public boolean isDirectDownload() {
        return false;
    }

    void setIso(boolean iso) {
        isIso = iso;
    }

    void setBootable(Boolean bootable) {
        this.bootable = bootable;
    }

    void setBits(Integer bits) {
        this.bits = bits;
    }

    void setFormat(String format) {
        this.format = format;
    }

    void setRequiresHVM(Boolean requiresHVM) {
        this.requiresHVM = requiresHVM;
    }

    void setHypervisorType(Hypervisor.HypervisorType hypervisorType) {
        this.hypervisorType = hypervisorType;
    }
}
