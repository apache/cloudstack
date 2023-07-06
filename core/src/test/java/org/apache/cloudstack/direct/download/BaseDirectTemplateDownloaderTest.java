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
package org.apache.cloudstack.direct.download;

import org.apache.http.HttpEntity;
import org.apache.http.HttpStatus;
import org.apache.http.HttpVersion;
import org.apache.http.StatusLine;
import org.apache.http.client.methods.CloseableHttpResponse;
import org.apache.http.client.methods.HttpGet;
import org.apache.http.client.methods.HttpHead;
import org.apache.http.impl.client.CloseableHttpClient;
import org.apache.http.message.BasicStatusLine;
import org.junit.Before;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.Mockito;
import org.mockito.MockitoAnnotations;

import java.io.ByteArrayInputStream;
import java.io.IOException;
import java.nio.charset.StandardCharsets;

public class BaseDirectTemplateDownloaderTest {
    protected static final String httpUrl = "http://server/path";
    protected static final String httpsUrl = "https://server:443/path";
    protected static final String httpMetalinkUrl = "http://dl.openvm.eu/cloudstack/macchinina/x86_64/macchinina-kvm.qcow2.bz2";
    protected static final String httpMetalinkContent = "<metalink xmlns=\"urn:ietf:params:xml:ns:metalink\">\n" +
            "<file name=\"macchinina-kvm.qcow2.bz2\">\n" +
            "<url location=\"pr\">" + httpMetalinkUrl + "</url>\n" +
            "</file>\n" +
            "</metalink>";

    @Mock
    private CloseableHttpClient httpsClient;

    @Mock
    private CloseableHttpResponse response;
    @Mock
    private HttpEntity httpEntity;

    @InjectMocks
    protected HttpsDirectTemplateDownloader httpsDownloader = new HttpsDirectTemplateDownloader(httpUrl);

    @Before
    public void init() throws IOException {
        MockitoAnnotations.initMocks(this);
        Mockito.when(httpsClient.execute(Mockito.any(HttpGet.class))).thenReturn(response);
        Mockito.when(httpsClient.execute(Mockito.any(HttpHead.class))).thenReturn(response);
        StatusLine statusLine = new BasicStatusLine(HttpVersion.HTTP_1_1, HttpStatus.SC_OK, "OK");
        Mockito.when(response.getStatusLine()).thenReturn(statusLine);
        Mockito.when(response.getEntity()).thenReturn(httpEntity);
        ByteArrayInputStream inputStream = new ByteArrayInputStream(httpMetalinkContent.getBytes(StandardCharsets.UTF_8));
        Mockito.when(httpEntity.getContent()).thenReturn(inputStream);
    }
}