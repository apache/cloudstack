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

import org.apache.cloudstack.api.ApiConstants;
import com.cloud.serializer.Param;
import com.google.gson.annotations.SerializedName;
import org.apache.cloudstack.api.BaseResponse;

public class SSHKeyPairResponse extends BaseResponse {

    @SerializedName(ApiConstants.NAME) @Param(description="Name of the keypair")
    private String name;

    @SerializedName("fingerprint") @Param(description="Fingerprint of the public key")
    private String fingerprint;

    @SerializedName("privatekey") @Param(description="Private key")
    private String privateKey;

    public SSHKeyPairResponse() {}

    public SSHKeyPairResponse(String name, String fingerprint) {
        this(name, fingerprint, null);
    }

    public SSHKeyPairResponse(String name, String fingerprint, String privateKey) {
        this.name = name;
        this.fingerprint = fingerprint;
        this.privateKey = privateKey;
    }

    public String getName() {
        return name;
    }

    public void setName(String name) {
        this.name = name;
    }

    public String getFingerprint() {
        return fingerprint;
    }

    public void setFingerprint(String fingerprint) {
        this.fingerprint = fingerprint;
    }

    public String getPrivateKey() {
        return privateKey;
    }

    public void setPrivateKey(String privateKey) {
        this.privateKey = privateKey;
    }

}
