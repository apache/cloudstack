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

import com.cloud.serializer.Param;
import com.google.gson.annotations.SerializedName;
import org.apache.cloudstack.api.ApiConstants;

import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

@SuppressWarnings("unused")
public class VnfTemplateResponse extends TemplateResponse {

    @SerializedName(ApiConstants.VNF_NICS)
    @Param(description = "NICs of the VNF template")
    private List<VnfNicResponse> vnfNics;

    @SerializedName(ApiConstants.VNF_DETAILS)
    @Param(description = "VNF details")
    private Map<String, String> vnfDetails;

    public void addVnfNic(VnfNicResponse vnfNic) {
        if (this.vnfNics == null) {
            this.vnfNics = new ArrayList<>();
        }
        this.vnfNics.add(vnfNic);
    }

    public void addVnfDetail(String key, String value) {
        if (this.vnfDetails == null) {
            this.vnfDetails = new LinkedHashMap<>();
        }
        this.vnfDetails.put(key,value);
    }

    public List<VnfNicResponse> getVnfNics() {
        return vnfNics;
    }

    public Map<String, String> getVnfDetails() {
        return vnfDetails;
    }
}
