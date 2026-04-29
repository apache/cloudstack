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
package com.cloud.ucs.manager;

import org.apache.commons.httpclient.Header;
import org.apache.commons.httpclient.HttpClient;
import org.apache.commons.httpclient.URI;
import org.apache.commons.httpclient.contrib.ssl.EasySSLProtocolSocketFactory;
import org.apache.commons.httpclient.methods.PostMethod;
import org.apache.commons.httpclient.methods.StringRequestEntity;
import org.apache.commons.httpclient.protocol.Protocol;

import com.cloud.utils.exception.CloudRuntimeException;

public class UcsHttpClient {
    private static HttpClient client = new HttpClient();
    private static Protocol ucsHttpsProtocol = new org.apache.commons.httpclient.protocol.Protocol("https", new EasySSLProtocolSocketFactory(), 443);
    private final String url;

    public UcsHttpClient(String ip) {
        url = String.format("http://%s/nuova", ip);
        Protocol.registerProtocol("https", ucsHttpsProtocol);
    }

    public String call(String xml) {
        PostMethod post = new PostMethod(url);
        post.setRequestEntity(new StringRequestEntity(xml));
        post.setRequestHeader("content-type", "text/xml");
        //post.setFollowRedirects(true);
        try {
            int result = client.executeMethod(post);
            if (result == 302) {
                // Handle HTTPS redirect
                // Ideal way might be to configure from add manager API
                // for using either HTTP / HTTPS
                // Allow only one level of redirect
                String redirectLocation;
                Header locationHeader = post.getResponseHeader("location");
                if (locationHeader != null) {
                    redirectLocation = locationHeader.getValue();
                } else {
                    throw new CloudRuntimeException("Call failed: Bad redirect from UCS Manager");
                }
                post.setURI(new URI(redirectLocation));
                result = client.executeMethod(post);
            }
            // Check for errors
            if (result != 200) {
                throw new CloudRuntimeException("Call failed: " + post.getResponseBodyAsString());
            }
            String res = post.getResponseBodyAsString();
            if (res.contains("errorCode")) {
                String err = String.format("ucs call failed:\nsubmitted doc:%s\nresponse:%s\n", xml, res);
                throw new CloudRuntimeException(err);
            }
            return res;
        } catch (Exception e) {
            throw new CloudRuntimeException(e.getMessage(), e);
        } finally {
            post.releaseConnection();
        }
    }
}
