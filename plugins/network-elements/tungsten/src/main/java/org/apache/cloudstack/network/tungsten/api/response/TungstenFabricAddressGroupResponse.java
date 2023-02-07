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
package org.apache.cloudstack.network.tungsten.api.response;

import com.cloud.dc.DataCenter;
import com.cloud.serializer.Param;
import com.google.gson.annotations.SerializedName;
import net.juniper.tungsten.api.types.AddressGroup;
import org.apache.cloudstack.api.ApiConstants;
import org.apache.cloudstack.api.BaseResponse;

public class TungstenFabricAddressGroupResponse extends BaseResponse {
    @SerializedName(ApiConstants.UUID)
    @Param(description = "Tungsten-Fabric address group uuid")
    private String uuid;

    @SerializedName(ApiConstants.NAME)
    @Param(description = "Tungsten-Fabric address group name")
    private String name;

    @SerializedName(ApiConstants.IP_PREFIX)
    @Param(description = "Tungsten-Fabric address group ip prefix")
    private String ipPrefix;

    @SerializedName(ApiConstants.IP_PREFIX_LEN)
    @Param(description = "Tungsten-Fabric address group ip prefix length")
    private int ipPrefixLen;

    @SerializedName(ApiConstants.ZONE_ID)
    @Param(description = "Tungsten-Fabric provider zone id")
    private long zoneId;

    @SerializedName(ApiConstants.ZONE_NAME)
    @Param(description = "Tungsten-Fabric provider zone name")
    private String zoneName;

    public TungstenFabricAddressGroupResponse(String uuid, String name, DataCenter zone) {
        this.uuid = uuid;
        this.name = name;
        this.zoneId = zone.getId();
        this.zoneName = zone.getName();
        this.setObjectName("addressgroup");
    }

    public TungstenFabricAddressGroupResponse(AddressGroup addressGroup, DataCenter zone) {
        this.uuid = addressGroup.getUuid();
        this.name = addressGroup.getName();
        this.ipPrefix = addressGroup.getPrefix().getSubnet().get(0).getIpPrefix();
        this.ipPrefixLen = addressGroup.getPrefix().getSubnet().get(0).getIpPrefixLen();
        this.zoneId = zone.getId();
        this.zoneName = zone.getName();
        this.setObjectName("addressgroup");
    }

    public String getUuid() {
        return uuid;
    }

    public void setUuid(final String uuid) {
        this.uuid = uuid;
    }

    public String getName() {
        return name;
    }

    public void setName(final String name) {
        this.name = name;
    }

    public String getIpPrefix() {
        return ipPrefix;
    }

    public void setIpPrefix(final String ipPrefix) {
        this.ipPrefix = ipPrefix;
    }

    public int getIpPrefixLen() {
        return ipPrefixLen;
    }

    public void setIpPrefixLen(final int ipPrefixLen) {
        this.ipPrefixLen = ipPrefixLen;
    }

    public long getZoneId() {
        return zoneId;
    }

    public void setZoneId(final long zoneId) {
        this.zoneId = zoneId;
    }

    public String getZoneName() {
        return zoneName;
    }

    public void setZoneName(final String zoneName) {
        this.zoneName = zoneName;
    }
}
