/**
 *  Copyright (C) 2010 Cloud.com, Inc.  All rights reserved.
 * 
 * This software is licensed under the GNU General Public License v3 or later.
 * 
 * It is free software: you can redistribute it and/or modify
 * it under the terms of the GNU General Public License as published by
 * the Free Software Foundation, either version 3 of the License, or any later version.
 * This program is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 * GNU General Public License for more details.
 * 
 * You should have received a copy of the GNU General Public License
 * along with this program.  If not, see <http://www.gnu.org/licenses/>.
 * 
 */
package com.cloud.api.response;

import java.util.Date;

import com.cloud.api.ApiConstants;
import com.cloud.utils.IdentityProxy;
import com.cloud.serializer.Param;
import com.google.gson.annotations.SerializedName;

public class DiskOfferingResponse extends BaseResponse {
    @SerializedName(ApiConstants.ID) @Param(description="unique ID of the disk offering")
    private IdentityProxy id = new IdentityProxy("disk_offering");

    @SerializedName(ApiConstants.DOMAIN_ID) @Param(description="the domain ID this disk offering belongs to. Ignore this information as it is not currently applicable.")
    private IdentityProxy domainId = new IdentityProxy("domain");

    @SerializedName(ApiConstants.DOMAIN) @Param(description="the domain name this disk offering belongs to. Ignore this information as it is not currently applicable.")
    private String domain;

    @SerializedName(ApiConstants.NAME) @Param(description="the name of the disk offering")
    private String name;

    @SerializedName(ApiConstants.DISPLAY_TEXT) @Param(description="an alternate display text of the disk offering.")
    private String displayText;

    @SerializedName(ApiConstants.DISK_SIZE) @Param(description="the size of the disk offering in GB")
    private Long diskSize;

    @SerializedName(ApiConstants.CREATED) @Param(description="the date this disk offering was created")
    private Date created;

    @SerializedName("iscustomized") @Param(description="true if disk offering uses custom size, false otherwise")
    private Boolean customized;
    
    @SerializedName(ApiConstants.TAGS) @Param(description="the tags for the disk offering")
    private String tags;

    public Long getId() {
        return id.getValue();
    }

    public void setId(Long id) {
        this.id.setValue(id);
    }

    public Long getDomainId() {
        return domainId.getValue();
    }

    public void setDomainId(Long domainId) {
        this.domainId.setValue(domainId);
    }

    public String getDomain() {
        return domain;
    }

    public void setDomain(String domain) {
        this.domain = domain;
    }

    public String getName() {
        return name;
    }

    public void setName(String name) {
        this.name = name;
    }

    public String getDisplayText() {
        return displayText;
    }

    public void setDisplayText(String displayText) {
        this.displayText = displayText;
    }

    public Long getDiskSize() {
        return diskSize;
    }

    public void setDiskSize(Long diskSize) {
        this.diskSize = diskSize;
    }

    public Date getCreated() {
        return created;
    }

    public void setCreated(Date created) {
        this.created = created;
    }

    public String getTags() {
        return tags;
    }

    public void setTags(String tags) {
        this.tags = tags;
    }

    public Boolean isCustomized() {
        return customized;
    }

    public void setCustomized(Boolean customized) {
        this.customized = customized;
    }

}
