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

import com.cloud.utils.exception.CloudRuntimeException;
import org.apache.commons.httpclient.contrib.ssl.EasySSLProtocolSocketFactory;
import org.apache.commons.httpclient.protocol.Protocol;
import org.apache.log4j.Logger;
import org.springframework.http.*;
import org.springframework.web.client.RestTemplate;

import java.net.URI;
import java.net.URISyntaxException;

public class UcsHttpClient {
    private static final Logger logger = Logger.getLogger(UcsHttpClient.class);
    //private static HttpClient client;
    private static Protocol ucsHttpsProtocol = new org.apache.commons.httpclient.protocol.Protocol("https", new EasySSLProtocolSocketFactory(), 443);
    private final String url;
    private static RestTemplate template;

    static {
        //client = new HttpClient();
        template = new RestTemplate();
    }

    public UcsHttpClient(String ip) {
        url = String.format("http://%s/nuova", ip);
        Protocol.registerProtocol("https", ucsHttpsProtocol);
    }


    private String call(URI uri, String body) {
        HttpHeaders requestHeaders = new HttpHeaders();
        requestHeaders.setContentType(MediaType.APPLICATION_XML);
        requestHeaders.setContentLength(body.length());
        HttpEntity<String> req = new HttpEntity<String>(body, requestHeaders);
        if (!body.contains("aaaLogin")) {
            logger.debug(String.format("UCS call: %s", body));
        }
        ResponseEntity<String> rsp = template.exchange(uri, HttpMethod.POST, req, String.class);
        if (rsp.getStatusCode() == org.springframework.http.HttpStatus.OK) {
            return rsp.getBody();
        } else if (rsp.getStatusCode() == org.springframework.http.HttpStatus.FOUND) {
            // Handle HTTPS redirect
            // Ideal way might be to configure from add manager API
            // for using either HTTP / HTTPS
            // Allow only one level of redirect
            java.net.URI location = rsp.getHeaders().getLocation();
            if (location == null) {
                throw new CloudRuntimeException("Call failed: Bad redirect from UCS Manager");
            }
            //call(location, body);
            rsp = template.exchange(location, HttpMethod.POST, req, String.class);
        }
        if (rsp.getStatusCode() != org.springframework.http.HttpStatus.OK) {
            String err = String.format("http status: %s, response body:%s", rsp.getStatusCode().toString(), rsp.getBody());
            throw new CloudRuntimeException(String.format("UCS API call failed, details: %s\n", err));
        }

        if (rsp.getBody().contains("errorCode")) {
            String err = String.format("ucs call failed:\nsubmitted doc:%s\nresponse:%s\n", body, rsp.getBody());
            throw new CloudRuntimeException(err);
        }
        return rsp.getBody();
    }

    public String call(String body) {
        try {
            return call(new URI(url), body);
        } catch (URISyntaxException e) {
            throw new CloudRuntimeException(e);
        }
        /*
        PostMethod post = new PostMethod(url);
        post.setRequestEntity(new StringRequestEntity(xml));
        post.setRequestHeader("Content-type", "text/xml");
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
                }
                else {
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
        */
    }
}
