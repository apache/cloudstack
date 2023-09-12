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
package org.apache.cloudstack.storage.resource;

import java.util.Map;

import org.junit.Assert;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.InjectMocks;
import org.mockito.Mockito;
import org.mockito.Spy;
import org.mockito.junit.MockitoJUnitRunner;

import io.netty.handler.codec.http.DefaultHttpHeaders;
import io.netty.handler.codec.http.HttpHeaders;

@RunWith(MockitoJUnitRunner.class)
public class HttpUploadServerHandlerTest {

    @Spy
    @InjectMocks
    HttpUploadServerHandler httpUploadServerHandler = new HttpUploadServerHandler(Mockito.mock(NfsSecondaryStorageResource.class));

    private void runGetUploadRequestUsefulHeadersTestForHost(String hostHeaderKey, String hostHeaderValue) {
        HttpHeaders request = new DefaultHttpHeaders();
        request.add(hostHeaderKey, hostHeaderValue);
        Map<HttpUploadServerHandler.UploadHeader, String> headers = httpUploadServerHandler.getUploadRequestUsefulHeaders(request);
        Assert.assertEquals(hostHeaderValue, headers.get(HttpUploadServerHandler.UploadHeader.HOST));
    }

    @Test
    public void testGetUploadRequestUsefulHeadersHeaderKeyDifferentCase() {
        String host = "SomeHost";
        runGetUploadRequestUsefulHeadersTestForHost(HttpUploadServerHandler.UploadHeader.HOST.getName(), host);
        runGetUploadRequestUsefulHeadersTestForHost(HttpUploadServerHandler.UploadHeader.HOST.getName().toUpperCase(), host);
        runGetUploadRequestUsefulHeadersTestForHost("X-Forwarded-Host", host);
    }

    @Test
    public void testGetUploadRequestUsefulHeadersAllKeys() {
        HttpHeaders request = new DefaultHttpHeaders();
        String sign = "Sign";
        String metadata = "met";
        String expires = "ex";
        String host = "SomeHost";
        long contentLength = 100L;
        request.add("x-Signature", sign);
        request.add("X-metadata", metadata);
        request.add("X-EXPIRES", expires);
        request.add("X-Forwarded-Host", host);
        request.add(HttpHeaders.Names.CONTENT_LENGTH, String.valueOf(contentLength));
        Map<HttpUploadServerHandler.UploadHeader, String> headers = httpUploadServerHandler.getUploadRequestUsefulHeaders(request);
        Assert.assertEquals(sign, headers.get(HttpUploadServerHandler.UploadHeader.SIGNATURE));
        Assert.assertEquals(metadata, headers.get(HttpUploadServerHandler.UploadHeader.METADATA));
        Assert.assertEquals(expires, headers.get(HttpUploadServerHandler.UploadHeader.EXPIRES));
        Assert.assertEquals(host, headers.get(HttpUploadServerHandler.UploadHeader.HOST));
        Assert.assertEquals(String.valueOf(contentLength), headers.get(HttpUploadServerHandler.UploadHeader.CONTENT_LENGTH));
    }
}
