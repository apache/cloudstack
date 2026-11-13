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
package org.apache.cloudstack.storage.datastore.util;

import com.amazonaws.SignableRequest;
import com.amazonaws.auth.AWS4Signer;
import com.amazonaws.auth.AWSCredentials;
import com.amazonaws.auth.SignerFactory;
import com.amazonaws.http.HttpMethodName;

/**
 * SigV4 signer for the IAM API of Ceph RGW.
 *
 * The AWS SDK signs IAM (query protocol) requests before it attaches the form body and its
 * {@code Content-Type} header, so that header is sent unsigned. RGW from Tentacle (v20) on
 * rejects a request whose supplied {@code Content-Type} is not among the signed headers
 * ("'content-type' supplied but not in CanonicalHeaders"); Squid tolerated it. Adding the
 * header the SDK will send anyway, before signing, makes it part of the canonical request.
 */
public class RgwIamSigner extends AWS4Signer {

    public static final String NAME = "RgwIamSigner";
    static final String FORM_CONTENT_TYPE = "application/x-www-form-urlencoded; charset=utf-8";

    private static volatile boolean registered;

    public static String register() {
        if (!registered) {
            synchronized (RgwIamSigner.class) {
                if (!registered) {
                    SignerFactory.registerSigner(NAME, RgwIamSigner.class);
                    registered = true;
                }
            }
        }
        return NAME;
    }

    @Override
    public void sign(SignableRequest<?> request, AWSCredentials credentials) {
        if (request.getHttpMethod() == HttpMethodName.POST && !request.getHeaders().containsKey("Content-Type")) {
            request.addHeader("Content-Type", FORM_CONTENT_TYPE);
        }
        super.sign(request, credentials);
    }
}
