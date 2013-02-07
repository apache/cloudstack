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
package com.cloud.ucs.manager;

import org.apache.commons.httpclient.HttpClient;
import org.apache.commons.httpclient.methods.PostMethod;
import org.apache.commons.httpclient.methods.StringRequestEntity;

import com.cloud.utils.exception.CloudRuntimeException;

public class UcsHttpClient {
    private static HttpClient client = new HttpClient();
    private String url;

    public UcsHttpClient(String ip) {
        this.url = String.format("http://%s/nuova", ip);
    }

    public String call(String xml) {
        PostMethod post = new PostMethod(url);
        post.setRequestEntity(new StringRequestEntity(xml));
        post.setRequestHeader("Content-type", "text/xml");
        try {
            int result = client.executeMethod(post);
            if (result != 200) {
               throw new CloudRuntimeException("Call failed: " + post.getResponseBodyAsString()); 
            }
            return post.getResponseBodyAsString();
        } catch (Exception e) {
            throw new CloudRuntimeException(e.getMessage(), e);
        } finally {
            post.releaseConnection();
        }
    }
}
