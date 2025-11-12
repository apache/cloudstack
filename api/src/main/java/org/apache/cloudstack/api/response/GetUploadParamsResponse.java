/*
 * Licensed to the Apache Software Foundation (ASF) under one
 * or more contributor license agreements.  See the NOTICE file
 * distributed with this work for additional information
 * regarding copyright ownership.  The ASF licenses this file
 * to you under the Apache License, Version 2.0 (the
 * "License"); you may not use this file except in compliance
 * with the License.  You may obtain a copy of the License at
 *
 *   http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing,
 * software distributed under the License is distributed on an
 * "AS IS" BASIS, WITHOUT WARRANTIES OR CONDITIONS OF ANY
 * KIND, either express or implied.  See the License for the
 * specific language governing permissions and limitations
 * under the License.
 */
package org.apache.cloudstack.api.response;

import java.net.URL;
import java.util.UUID;

import org.apache.cloudstack.api.ApiConstants;
import org.apache.cloudstack.api.BaseResponse;

import com.cloud.serializer.Param;
import com.google.gson.annotations.SerializedName;

public class GetUploadParamsResponse extends BaseResponse {

    @SerializedName(ApiConstants.ID)
    @Param(description = "the template/volume ID")
    private UUID id;

    @SerializedName(ApiConstants.POST_URL)
    @Param(description = "POST url to upload the file to")
    private URL postURL;

    @SerializedName(ApiConstants.METADATA)
    @Param(description = "encrypted data to be sent in the POST request.")
    private String metadata;

    @SerializedName(ApiConstants.EXPIRES)
    @Param(description = "the timestamp after which the signature expires")
    private String expires;

    @SerializedName(ApiConstants.SIGNATURE)
    @Param(description = "signature to be sent in the POST request.")
    private String signature;

    public GetUploadParamsResponse(UUID id, URL postURL, String metadata, String expires, String signature) {
        this.id = id;
        this.postURL = postURL;
        this.metadata = metadata;
        this.expires = expires;
        this.signature = signature;
        setObjectName("getuploadparams");
    }

    public GetUploadParamsResponse() {
        setObjectName("getuploadparams");
    }

    public UUID getId() {
        return id;
    }

    public void setId(UUID id) {
        this.id = id;
    }

    public void setPostURL(URL postURL) {
        this.postURL = postURL;
    }

    public void setMetadata(String metadata) {
        this.metadata = metadata;
    }

    public void setTimeout(String expires) {
        this.expires = expires;
    }

    public void setSignature(String signature) {
        this.signature = signature;
    }
}
