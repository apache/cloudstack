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

import org.apache.http.client.methods.CloseableHttpResponse;
import org.apache.http.client.methods.HttpGet;
import org.apache.http.client.methods.HttpUriRequest;
import org.apache.http.impl.client.CloseableHttpClient;
import org.apache.http.impl.client.HttpClients;
import org.apache.log4j.Logger;
import org.w3c.dom.Document;
import org.xml.sax.SAXException;

import java.io.IOException;

import javax.xml.parsers.DocumentBuilder;
import javax.xml.parsers.DocumentBuilderFactory;
import javax.xml.parsers.ParserConfigurationException;

public class IntrospectApiConnectorImpl implements IntrospectApiConnector {
    private static final Logger s_logger = Logger.getLogger(IntrospectApiConnectorImpl.class);
    private String url;

    public IntrospectApiConnectorImpl(VRouter vRouter) {
        url = "http://" + vRouter.getHost() + ":" + vRouter.getPort() + "/";
    }

    public Document getSnhItfReq(String uuid) {
        final StringBuffer url = new StringBuffer();
        url.append(this.url).append("Snh_ItfReq?uuid=").append(uuid);
        HttpUriRequest request = new HttpGet(url.toString());
        try (CloseableHttpClient httpClient = HttpClients.createDefault();
            CloseableHttpResponse httpResponse = httpClient.execute(request)) {
            return getResponse(httpResponse);
        } catch (IOException ex) {
            s_logger.error("Failed to connect host : " + ex.getMessage());
            return null;
        } catch (ParserConfigurationException ex) {
            s_logger.error("Failed to parse xml configuration : " + ex.getMessage());
            return null;
        } catch (SAXException ex) {
            s_logger.error("Failed to get xml data : " + ex.getMessage());
            return null;
        }
    }

    private Document getResponse(final CloseableHttpResponse httpResponse)
        throws IOException, ParserConfigurationException, SAXException {
        DocumentBuilderFactory documentBuilderFactory = DocumentBuilderFactory.newInstance();
        DocumentBuilder documentBuilder = documentBuilderFactory.newDocumentBuilder();
        return documentBuilder.parse(httpResponse.getEntity().getContent());
    }
}
