//
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
//

package com.cloud.utils.rest;

import java.io.IOException;

import org.apache.commons.codec.binary.Base64;
import org.apache.commons.httpclient.HttpClient;
import org.apache.commons.httpclient.HttpException;
import org.apache.commons.httpclient.HttpMethodBase;

/**
 * Base 64 encoded authorization strategy. This implementation as opposed to
 * {@link RESTValidationStrategy} doesn't do a login after auth error, but instead
 * includes the encoded credentials in each request, instead of a cookie.
 */
public class BasicEncodedRESTValidationStrategy extends RESTValidationStrategy {

    public BasicEncodedRESTValidationStrategy(final String host, final String adminuser, final String adminpass) {
        super();
        this.host = host;
        user = adminuser;
        password = adminpass;
    }

    public BasicEncodedRESTValidationStrategy() {
    }

    @Override
    public void executeMethod(final HttpMethodBase method, final HttpClient client,
            final String protocol)
                    throws CloudstackRESTException, HttpException, IOException {
        if (host == null || host.isEmpty() || user == null || user.isEmpty() || password == null || password.isEmpty()) {
            throw new CloudstackRESTException("Hostname/credentials are null or empty");
        }

        final String encodedCredentials = encodeCredentials();
        method.setRequestHeader("Authorization", "Basic " + encodedCredentials);
        client.executeMethod(method);
    }

    private String encodeCredentials() {
        final String authString = user + ":" + password;
        final byte[] authEncBytes = Base64.encodeBase64(authString.getBytes());
        final String authStringEnc = new String(authEncBytes);
        return authStringEnc;
    }

}
