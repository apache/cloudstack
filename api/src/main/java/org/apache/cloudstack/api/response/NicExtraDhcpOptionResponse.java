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
package org.apache.cloudstack.api.response;


import com.google.gson.annotations.SerializedName;
import org.apache.cloudstack.api.ApiConstants;
import org.apache.cloudstack.api.BaseResponse;
import org.apache.cloudstack.api.EntityReference;

import com.cloud.serializer.Param;

@EntityReference(value = NicExtraDhcpOptionResponse.class)
public class NicExtraDhcpOptionResponse extends BaseResponse {

    @SerializedName(ApiConstants.ID)
    @Param(description = "the ID of the extra dhcp option")
    private String id;

    @SerializedName(ApiConstants.NIC_ID)
    @Param(description = "the ID of the nic")
    private String nicId;

    @SerializedName(ApiConstants.EXTRA_DHCP_OPTION_NAME)
    @Param(description = "the name of the extra DHCP option")
    private String codeName;

    @SerializedName(ApiConstants.EXTRA_DHCP_OPTION_CODE)
    @Param(description = "the extra DHCP option code")
    private int code;

    @SerializedName(ApiConstants.EXTRA_DHCP_OPTION_VALUE)
    @Param(description = "the extra DHCP option value")
    private String value;

    public NicExtraDhcpOptionResponse() {
        super();
    }

    public NicExtraDhcpOptionResponse(String codeName, int code, String value) {
        this.codeName = codeName;
        this.code = code;
        this.value = value;
    }

    public String getId() {
        return id;
    }

    public void setId(String id) {
        this.id = id;
    }

    public String getNicId() {
        return nicId;
    }

    public void setNicId(String nicId) {
        this.nicId = nicId;
    }

    public String getCodeName() {
        return codeName;
    }

    public void setCodeName(String codeName) {
        this.codeName = codeName;
    }

    public int getCode() {
        return code;
    }

    public void setCode(int code) {
        this.code = code;
    }

    public String getValue() {
        return value;
    }

    public void setValue(String value) {
        this.value = value;
    }
}
