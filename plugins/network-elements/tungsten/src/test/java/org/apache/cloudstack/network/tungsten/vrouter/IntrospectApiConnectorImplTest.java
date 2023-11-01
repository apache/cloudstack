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
package org.apache.cloudstack.network.tungsten.vrouter;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNull;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

import org.apache.http.HttpEntity;
import org.apache.http.client.methods.CloseableHttpResponse;
import org.apache.http.client.methods.HttpUriRequest;
import org.apache.http.impl.client.CloseableHttpClient;
import org.apache.http.impl.client.HttpClients;
import org.junit.After;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.MockedStatic;
import org.mockito.Mockito;
import org.mockito.junit.MockitoJUnitRunner;
import org.w3c.dom.Document;
import org.xml.sax.SAXException;

import java.io.IOException;
import java.io.InputStream;

import javax.xml.parsers.DocumentBuilder;
import javax.xml.parsers.DocumentBuilderFactory;
import javax.xml.parsers.ParserConfigurationException;

@RunWith(MockitoJUnitRunner.class)
public class IntrospectApiConnectorImplTest {
    IntrospectApiConnector introspectApiConnector;

    MockedStatic<HttpClients> httpClientsMocked;

    MockedStatic<DocumentBuilderFactory> documentBuilderFactoryMocked;

    @Before
    public void setup() {
        VRouter vRouter = mock(VRouter.class);
        introspectApiConnector = new IntrospectApiConnectorImpl(vRouter);
        httpClientsMocked =  Mockito.mockStatic(HttpClients.class);
        documentBuilderFactoryMocked = Mockito.mockStatic(DocumentBuilderFactory.class);
    }

    @After
    public void tearDown() {
        httpClientsMocked.close();
        documentBuilderFactoryMocked.close();
    }

    @Test
    public void getSnhItfReqTest() throws Exception {
        Document document = mock(Document.class);
        CloseableHttpClient httpClient = mock(CloseableHttpClient.class);
        CloseableHttpResponse closeableHttpResponse = mock(CloseableHttpResponse.class);
        HttpEntity httpEntity = mock(HttpEntity.class);
        InputStream inputStream = mock(InputStream.class);
        DocumentBuilderFactory documentBuilderFactory = mock(DocumentBuilderFactory.class);
        DocumentBuilder documentBuilder = mock(DocumentBuilder.class);

        httpClientsMocked.when(HttpClients::createDefault).thenReturn(httpClient);
        when(httpClient.execute(any(HttpUriRequest.class))).thenReturn(closeableHttpResponse);
        when(closeableHttpResponse.getEntity()).thenReturn(httpEntity);
        when(httpEntity.getContent()).thenReturn(inputStream);
        documentBuilderFactoryMocked.when(DocumentBuilderFactory::newInstance).thenReturn(documentBuilderFactory);
        when(documentBuilderFactory.newDocumentBuilder()).thenReturn(documentBuilder);
        when(documentBuilder.parse(any(InputStream.class))).thenReturn(document);

        assertEquals(document, introspectApiConnector.getSnhItfReq("948f421c-edde-4518-a391-09299cc25dc2"));
    }

    @Test
    public void getSnhItfReqWithIOExceptionTest() throws Exception {
        CloseableHttpClient httpClient = mock(CloseableHttpClient.class);

        httpClientsMocked.when(HttpClients::createDefault).thenReturn(httpClient);
        when(httpClient.execute(any(HttpUriRequest.class))).thenThrow(IOException.class);

        assertNull(introspectApiConnector.getSnhItfReq("948f421c-edde-4518-a391-09299cc25dc2"));
    }

    @Test
    public void getSnhItfReqWithParserConfigurationExceptionTest() throws Exception {
        CloseableHttpClient httpClient = mock(CloseableHttpClient.class);
        CloseableHttpResponse closeableHttpResponse = mock(CloseableHttpResponse.class);
        HttpEntity httpEntity = mock(HttpEntity.class);
        InputStream inputStream = mock(InputStream.class);
        DocumentBuilderFactory documentBuilderFactory = mock(DocumentBuilderFactory.class);

        httpClientsMocked.when(HttpClients::createDefault).thenReturn(httpClient);
        when(httpClient.execute(any(HttpUriRequest.class))).thenReturn(closeableHttpResponse);
        documentBuilderFactoryMocked.when(DocumentBuilderFactory::newInstance).thenReturn(documentBuilderFactory);
        when(documentBuilderFactory.newDocumentBuilder()).thenThrow(ParserConfigurationException.class);

        assertNull(introspectApiConnector.getSnhItfReq("948f421c-edde-4518-a391-09299cc25dc2"));
    }

    @Test
    public void getSnhItfReqWithSAXExceptionTest() throws Exception {
        CloseableHttpClient httpClient = mock(CloseableHttpClient.class);
        CloseableHttpResponse closeableHttpResponse = mock(CloseableHttpResponse.class);
        HttpEntity httpEntity = mock(HttpEntity.class);
        InputStream inputStream = mock(InputStream.class);
        DocumentBuilderFactory documentBuilderFactory = mock(DocumentBuilderFactory.class);
        DocumentBuilder documentBuilder = mock(DocumentBuilder.class);

        httpClientsMocked.when(HttpClients::createDefault).thenReturn(httpClient);
        when(httpClient.execute(any(HttpUriRequest.class))).thenReturn(closeableHttpResponse);
        when(closeableHttpResponse.getEntity()).thenReturn(httpEntity);
        when(httpEntity.getContent()).thenReturn(inputStream);
        documentBuilderFactoryMocked.when(DocumentBuilderFactory::newInstance).thenReturn(documentBuilderFactory);
        when(documentBuilderFactory.newDocumentBuilder()).thenReturn(documentBuilder);
        when(documentBuilder.parse(any(InputStream.class))).thenThrow(SAXException.class);

        assertNull(introspectApiConnector.getSnhItfReq("948f421c-edde-4518-a391-09299cc25dc2"));
    }
}
