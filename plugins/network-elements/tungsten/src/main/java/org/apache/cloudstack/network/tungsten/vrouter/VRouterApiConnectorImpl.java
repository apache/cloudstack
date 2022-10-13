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

import com.google.gson.Gson;
import com.google.gson.JsonObject;
import com.google.gson.JsonParser;
import org.apache.http.client.methods.CloseableHttpResponse;
import org.apache.http.client.methods.HttpDelete;
import org.apache.http.client.methods.HttpPost;
import org.apache.http.client.methods.HttpPut;
import org.apache.http.entity.StringEntity;
import org.apache.http.impl.client.CloseableHttpClient;
import org.apache.http.impl.client.HttpClients;
import org.apache.http.util.EntityUtils;
import org.apache.log4j.Logger;

import java.io.IOException;
import java.util.List;

public class VRouterApiConnectorImpl implements VRouterApiConnector {
    private static final Logger s_logger = Logger.getLogger(VRouterApiConnector.class);
    private String _url;

    public VRouterApiConnectorImpl(String host, String port) {
        _url = "http://" + host + ":" + port + "/";
    }

    @Override
    public boolean addPort(final Port port) throws IOException {
        final StringBuffer url = new StringBuffer();
        url.append(_url).append("port");
        Gson gson = new Gson();
        final String jsonData = gson.toJson(port);
        HttpPost httpPost = new HttpPost(url.toString());
        httpPost.setEntity(new StringEntity(jsonData));
        try (CloseableHttpClient httpClient = HttpClients.createDefault();
            CloseableHttpResponse httpResponse = httpClient.execute(httpPost)) {
            return getResponse(httpResponse);
        } catch (IOException ex) {
            s_logger.error("Failed to add vrouter port : " + ex.getMessage());
            return false;
        }
    }

    @Override
    public boolean deletePort(final String portId) {
        final StringBuffer url = new StringBuffer();
        url.append(_url).append("port/").append(portId);
        HttpDelete httpDelete = new HttpDelete(url.toString());
        try (CloseableHttpClient httpClient = HttpClients.createDefault();
            CloseableHttpResponse httpResponse = httpClient.execute(httpDelete)) {
            return getResponse(httpResponse);
        } catch (IOException ex) {
            s_logger.error("Failed to delete vrouter port : " + ex.getMessage());
            return false;
        }
    }

    @Override
    public boolean enablePort(final String portId) {
        final StringBuffer url = new StringBuffer();
        url.append(_url).append("enable-port/").append(portId);
        HttpPut httpPut = new HttpPut(url.toString());
        try (CloseableHttpClient httpClient = HttpClients.createDefault();
            CloseableHttpResponse httpResponse = httpClient.execute(httpPut)) {
            return getResponse(httpResponse);
        } catch (IOException ex) {
            s_logger.error("Failed to enable vrouter port : " + ex.getMessage());
            return false;
        }
    }

    @Override
    public boolean disablePort(final String portId) {
        final StringBuffer url = new StringBuffer();
        url.append(_url).append("disable-port/").append(portId);
        HttpPut httpPut = new HttpPut(url.toString());
        try (CloseableHttpClient httpClient = HttpClients.createDefault();
            CloseableHttpResponse httpResponse = httpClient.execute(httpPut)) {
            return getResponse(httpResponse);
        } catch (IOException ex) {
            s_logger.error("Failed to disable vrouter port : " + ex.getMessage());
            return false;
        }
    }


    @Override
    public boolean addGateway(List<Gateway> gatewayList) throws IOException {
        final StringBuffer url = new StringBuffer();
        url.append(_url).append("gateway");
        HttpPost httpPost = new HttpPost(url.toString());
        Gson gson = new Gson();
        final String jsonData = gson.toJson(gatewayList);
        httpPost.setEntity(new StringEntity(jsonData));
        try (CloseableHttpClient httpClient = HttpClients.createDefault();
            CloseableHttpResponse httpResponse = httpClient.execute(httpPost)) {
            return getResponse(httpResponse);
        } catch (IOException ex) {
            s_logger.error("Failed to add route : " + ex.getMessage());
            return false;
        }
    }

    @Override
    public boolean deleteGateway(List<Gateway> gatewayList) throws IOException {
        final StringBuffer url = new StringBuffer();
        url.append(_url).append("gateway");
        CustomHttpDelete customHttpDelete = new CustomHttpDelete(url.toString());
        Gson gson = new Gson();
        final String jsonData = gson.toJson(gatewayList);
        customHttpDelete.setEntity(new StringEntity(jsonData));
        try (CloseableHttpClient httpClient = HttpClients.createDefault();
            CloseableHttpResponse httpResponse = httpClient.execute(customHttpDelete)) {
            return getResponse(httpResponse);
        } catch (IOException ex) {
            s_logger.error("Failed to remove route : " + ex.getMessage());
            return false;
        }
    }

    private boolean getResponse(final CloseableHttpResponse httpResponse) throws IOException {
        JsonParser parser = new JsonParser();
        String result = EntityUtils.toString(httpResponse.getEntity());
        JsonObject jsonObject = parser.parse(result).getAsJsonObject();
        if (jsonObject.entrySet().size() == 0) {
            return true;
        } else {
            String error = jsonObject.get("error").getAsString();
            s_logger.error(error);
            return false;
        }
    }
}
