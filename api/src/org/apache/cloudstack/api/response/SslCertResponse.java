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

import java.util.List;

import com.google.gson.annotations.SerializedName;

import org.apache.cloudstack.api.ApiConstants;
import org.apache.cloudstack.api.BaseResponse;
import org.apache.cloudstack.api.EntityReference;

import com.cloud.network.lb.SslCert;
import com.cloud.serializer.Param;

//import org.apache.cloudstack.api.EntityReference;

@EntityReference(value = SslCert.class)
public class SslCertResponse extends BaseResponse {

    @SerializedName(ApiConstants.ID)
    @Param(description = "SSL certificate ID")
    private String id;

    @SerializedName(ApiConstants.CERTIFICATE)
    @Param(description = "certificate")
    private String certificate;

    @SerializedName(ApiConstants.PRIVATE_KEY)
    @Param(description = "private key")
    private String privatekey;

    @SerializedName(ApiConstants.ACCOUNT)
    @Param(description = "account for the certificate")
    private String accountName;

    @SerializedName(ApiConstants.CERTIFICATE_CHAIN)
    @Param(description = "certificate chain")
    private String certchain;

    @SerializedName(ApiConstants.CERTIFICATE_FINGERPRINT)
    @Param(description = "certificate fingerprint")
    private String fingerprint;

    @SerializedName(ApiConstants.LOAD_BALANCER_RULE_LIST)
    @Param(description = "List of loabalancers this certificate is bound to")
    List<String> lbIds;

    public SslCertResponse() {
    }

    public void setId(String id) {
        this.id = id;
    }

    public void setCertificate(String cert) {
        this.certificate = cert;
    }

    public void setPrivatekey(String key) {
        this.privatekey = key;
    }

    public void setAccountName(String accountName) {
        this.accountName = accountName;
    }

    public void setCertchain(String chain) {
        this.certchain = chain;
    }

    public void setFingerprint(String fingerprint) {
        this.fingerprint = fingerprint;
    }

    public void setLbIds(List<String> lbIds) {
        this.lbIds = lbIds;
    }
}
