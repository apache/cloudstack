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
import net.juniper.tungsten.api.ApiPropertyBase;
import net.juniper.tungsten.api.ObjectReference;
import net.juniper.tungsten.api.types.InterfaceRouteTable;
import org.apache.cloudstack.api.ApiConstants;
import org.apache.cloudstack.api.BaseResponse;

import java.util.ArrayList;
import java.util.List;

public class TungstenFabricInterfaceRouteTableResponse extends BaseResponse {

    @SerializedName(ApiConstants.UUID)
    @Param(description = "Tungsten-Fabric network route table uuid")
    private String uuid;

    @SerializedName(ApiConstants.NAME)
    @Param(description = "Tungsten-Fabric network route table name")
    private String name;

    @SerializedName(ApiConstants.TUNGSTEN_VMS)
    @Param(description = "Tungsten-Fabric vms name")
    private List<TungstenFabricVmResponse> vms;

    @SerializedName(ApiConstants.ZONE_ID)
    @Param(description = "Tungsten-Fabric provider zone id")
    private long zoneId;

    @SerializedName(ApiConstants.ZONE_NAME)
    @Param(description = "Tungsten-Fabric provider zone name")
    private String zoneName;

    public TungstenFabricInterfaceRouteTableResponse(InterfaceRouteTable interfaceRouteTable, DataCenter zone) {
        this.uuid = interfaceRouteTable.getUuid();
        this.name = interfaceRouteTable.getName();
        List<TungstenFabricVmResponse> vms = new ArrayList<>();
        List<ObjectReference<ApiPropertyBase>> objectReferenceList = interfaceRouteTable.getVirtualMachineInterfaceBackRefs();
        if (objectReferenceList != null) {
            for (ObjectReference<ApiPropertyBase> objectReference : objectReferenceList) {
                TungstenFabricVmResponse tungstenFabricVmResponse = new TungstenFabricVmResponse(objectReference.getUuid(),
                        objectReference.getReferredName().get(objectReference.getReferredName().size() - 1));
                vms.add(tungstenFabricVmResponse);
            }
        }
        this.vms = vms;
        this.zoneId = zone.getId();
        this.zoneName = zone.getName();
        this.setObjectName("interfaceroutetable");
    }

    public String getUuid() {
        return uuid;
    }

    public void setUuid(String uuid) {
        this.uuid = uuid;
    }

    public String getName() {
        return name;
    }

    public void setName(String name) {
        this.name = name;
    }

    public List<TungstenFabricVmResponse> getVms() {
        return vms;
    }

    public void setVms(List<TungstenFabricVmResponse> vms) {
        this.vms = vms;
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
