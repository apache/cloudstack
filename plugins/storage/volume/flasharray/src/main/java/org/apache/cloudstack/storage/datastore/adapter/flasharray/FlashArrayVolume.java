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
package org.apache.cloudstack.storage.datastore.adapter.flasharray;

import org.apache.cloudstack.storage.datastore.adapter.ProviderSnapshot;

import com.fasterxml.jackson.annotation.JsonIgnore;
import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.fasterxml.jackson.annotation.JsonInclude;
import com.fasterxml.jackson.annotation.JsonProperty;

@JsonIgnoreProperties(ignoreUnknown = true)
@JsonInclude(JsonInclude.Include.NON_NULL)
public class FlashArrayVolume implements ProviderSnapshot {
    public static final String PURE_OUI = "24a9370";

    @JsonProperty("destroyed")
    private Boolean destroyed;
    /** The virtual size requested for this volume */
    @JsonProperty("provisioned")
    private Long allocatedSizeBytes;
    @JsonIgnore
    private String id;
    @JsonIgnore // we don't use the Cloudstack user name at all
    private String name;
    @JsonIgnore
    private String shortExternalName;
    @JsonProperty("pod")
    private FlashArrayVolumePod pod;
    @JsonProperty("priority")
    private Integer priority;
    @JsonProperty("promotion_status")
    private String promotionStatus;
    @JsonProperty("subtype")
    private String subtype;
    @JsonProperty("space")
    private FlashArrayVolumeSpace space;
    @JsonProperty("source")
    private FlashArrayVolumeSource source;
    @JsonProperty("serial")
    private String serial;
    @JsonProperty("name")
    private String externalName;
    @JsonProperty("id")
    private String externalUuid;
    @JsonIgnore
    private AddressType addressType;
    @JsonIgnore
    private String connectionId;

    public FlashArrayVolume() {
        this.addressType = AddressType.FIBERWWN;
    }

    @Override
    public Boolean isDestroyed() {
        return destroyed;
    }
    @Override
    @JsonIgnore
    public String getId() {
        return id;
    }
    @Override
    @JsonIgnore
    public String getName() {
        return name;
    }
    @JsonIgnore
    public String getPodName() {
        if (pod != null) {
            return pod.name;
        } else {
            return null;
        }
    }
    @Override
    @JsonIgnore
    public Integer getPriority() {
        return priority;
    }
    @Override
    @JsonIgnore
    public String getState() {
        return null;
    }
    @Override
    @JsonIgnore
    public AddressType getAddressType() {
        return addressType;
    }
    @Override
    @JsonIgnore
    public String getAddress() {
        if (serial == null) return null;
        return ("6" + PURE_OUI + serial).toLowerCase();
    }
    @Override
    public String getExternalConnectionId() {
        return connectionId;
    }

    @JsonIgnore
    public void setExternalConnectionId(String externalConnectionId) {
        this.connectionId = externalConnectionId;
    }

    @Override
    public void setId(String id) {
        this.id = id;
    }
    @Override
    public void setName(String name) {
        this.name = name;
    }
    public void setPodName(String podname) {
        FlashArrayVolumePod pod = new FlashArrayVolumePod();
        pod.name = podname;
        this.pod = pod;
    }
    @Override
    public void setPriority(Integer priority) {
        this.priority = priority;
    }
    @Override
    public void setAddressType(AddressType addressType) {
        this.addressType = addressType;
    }
    @Override
    @JsonIgnore
    public Long getAllocatedSizeInBytes() {
        return this.allocatedSizeBytes;
    }
    public void setAllocatedSizeBytes(Long size) {
        this.allocatedSizeBytes = size;
    }
    @Override
    @JsonIgnore
    public Long getUsedBytes() {
        if (space != null) {
            return space.getVirtual();
        } else {
            return null;
        }
    }

    public void setDestroyed(Boolean destroyed) {
        this.destroyed = destroyed;
    }
    public FlashArrayVolumeSource getSource() {
        return source;
    }
    public void setSource(FlashArrayVolumeSource source) {
        this.source = source;
    }
    @Override
    public String getExternalUuid() {
        return externalUuid;
    }
    @Override
    public String getExternalName() {
        return externalName;
    }

    public void setExternalUuid(String uuid) {
        this.externalUuid = uuid;
    }

    public void setExternalName(String name) {
        this.externalName = name;
    }
    @Override
    public Boolean canAttachDirectly() {
        return false;
    }
    public String getConnectionId() {
        return connectionId;
    }
    public void setConnectionId(String connectionId) {
        this.connectionId = connectionId;
    }

    public Boolean getDestroyed() {
        return destroyed;
    }

    public Long getAllocatedSizeBytes() {
        return allocatedSizeBytes;
    }

    public String getShortExternalName() {
        return shortExternalName;
    }

    public void setShortExternalName(String shortExternalName) {
        this.shortExternalName = shortExternalName;
    }

    public FlashArrayVolumePod getPod() {
        return pod;
    }

    public void setPod(FlashArrayVolumePod pod) {
        this.pod = pod;
    }

    public String getPromotionStatus() {
        return promotionStatus;
    }

    public void setPromotionStatus(String promotionStatus) {
        this.promotionStatus = promotionStatus;
    }

    public String getSubtype() {
        return subtype;
    }

    public void setSubtype(String subtype) {
        this.subtype = subtype;
    }

    public FlashArrayVolumeSpace getSpace() {
        return space;
    }

    public void setSpace(FlashArrayVolumeSpace space) {
        this.space = space;
    }

    public String getSerial() {
        return serial;
    }

    public void setSerial(String serial) {
        this.serial = serial;
    }

}
