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

import com.cloud.network.rules.FirewallRule;
import com.cloud.serializer.Param;
import com.google.gson.annotations.SerializedName;
import org.apache.cloudstack.api.BaseResponse;
import org.apache.cloudstack.api.EntityReference;

@EntityReference(value = FirewallRule.class)
public class TlsDataResponse extends BaseResponse {
    @SerializedName("crt")
    @Param(description = "crt")
    private String crt;

    @SerializedName("key")
    @Param(description = "key")
    private String key;

    @SerializedName("chain")
    @Param(description = "chain")
    private String chain;

    public String getCrt() {
        return crt;
    }

    public void setCrt(final String crt) {
        this.crt = crt;
    }

    public String getKey() {
        return key;
    }

    public void setKey(final String key) {
        this.key = key;
    }

    public String getChain() {
        return chain;
    }

    public void setChain(final String chain) {
        this.chain = chain;
    }
}
