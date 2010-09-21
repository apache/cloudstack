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

import com.google.gson.annotations.SerializedName;

public class ServiceOfferingResponse extends BaseResponse {
    @SerializedName("id")
    private Long id;

    @SerializedName("name")
    private String name;

    @SerializedName("displaytext")
    private String displayText;

    @SerializedName("cpunumber")
    private int cpuNumber;

    @SerializedName("cpuspeed")
    private int cpuSpeed;

    @SerializedName("memory")
    private int memory;

    @SerializedName("created")
    private Date created;

    @SerializedName("storagetype")
    private String storageType;

    @SerializedName("offerha")
    private Boolean offerHa;

    @SerializedName("usevirtualnetwork")
    private Boolean useVirtualNetwork;

    @SerializedName("tags")
    private String tags;

    public Long getId() {
        return id;
    }

    public void setId(Long id) {
        this.id = id;
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

    public int getCpuNumber() {
        return cpuNumber;
    }

    public void setCpuNumber(int cpuNumber) {
        this.cpuNumber = cpuNumber;
    }

    public int getCpuSpeed() {
        return cpuSpeed;
    }

    public void setCpuSpeed(int cpuSpeed) {
        this.cpuSpeed = cpuSpeed;
    }

    public int getMemory() {
        return memory;
    }

    public void setMemory(int memory) {
        this.memory = memory;
    }

    public Date getCreated() {
        return created;
    }

    public void setCreated(Date created) {
        this.created = created;
    }

    public String getStorageType() {
        return storageType;
    }

    public void setStorageType(String storageType) {
        this.storageType = storageType;
    }

    public Boolean getOfferHa() {
        return offerHa;
    }

    public void setOfferHa(Boolean offerHa) {
        this.offerHa = offerHa;
    }

    public Boolean getUseVirtualNetwork() {
        return useVirtualNetwork;
    }

    public void setUseVirtualNetwork(Boolean useVirtualNetwork) {
        this.useVirtualNetwork = useVirtualNetwork;
    }

    public String getTags() {
        return tags;
    }

    public void setTags(String tags) {
        this.tags = tags;
    }
}
