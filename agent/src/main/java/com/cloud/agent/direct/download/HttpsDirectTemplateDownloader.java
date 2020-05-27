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

package com.cloud.agent.direct.download;

import com.cloud.utils.Pair;
import com.cloud.utils.exception.CloudRuntimeException;
import com.cloud.utils.script.Script;
import org.apache.commons.io.IOUtils;
import org.apache.http.HttpEntity;
import org.apache.http.client.methods.CloseableHttpResponse;
import org.apache.commons.collections.MapUtils;
import org.apache.http.client.config.RequestConfig;
import org.apache.http.client.methods.HttpGet;
import org.apache.http.client.methods.HttpUriRequest;
import org.apache.http.conn.ssl.SSLConnectionSocketFactory;
import org.apache.http.conn.ssl.TrustSelfSignedStrategy;
import org.apache.http.impl.client.CloseableHttpClient;
import org.apache.http.impl.client.HttpClients;
import org.apache.http.ssl.SSLContexts;

import javax.net.ssl.SSLContext;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.security.KeyManagementException;
import java.security.KeyStore;
import java.security.KeyStoreException;
import java.security.NoSuchAlgorithmException;
import java.security.cert.CertificateException;
import java.util.Map;

public class HttpsDirectTemplateDownloader extends HttpDirectTemplateDownloader {

    private CloseableHttpClient httpsClient;
    private HttpUriRequest req;

    public HttpsDirectTemplateDownloader(String url, Long templateId, String destPoolPath, String checksum, Map<String, String> headers,
                                         Integer connectTimeout, Integer soTimeout, Integer connectionRequestTimeout, String temporaryDownloadPath) {
        super(url, templateId, destPoolPath, checksum, headers, connectTimeout, soTimeout, temporaryDownloadPath);
        SSLContext sslcontext = null;
        try {
            sslcontext = getSSLContext();
        } catch (KeyStoreException | NoSuchAlgorithmException | CertificateException | IOException | KeyManagementException e) {
            throw new CloudRuntimeException("Failure getting SSL context for HTTPS downloader: " + e.getMessage());
        }
        SSLConnectionSocketFactory factory = new SSLConnectionSocketFactory(sslcontext, SSLConnectionSocketFactory.ALLOW_ALL_HOSTNAME_VERIFIER);
        RequestConfig config = RequestConfig.custom()
                .setConnectTimeout(connectTimeout == null ? 5000 : connectTimeout)
                .setConnectionRequestTimeout(connectionRequestTimeout == null ? 5000 : connectionRequestTimeout)
                .setSocketTimeout(soTimeout == null ? 5000 : soTimeout).build();
        httpsClient = HttpClients.custom().setSSLSocketFactory(factory).setDefaultRequestConfig(config).build();
        createUriRequest(url, headers);
    }

    protected void createUriRequest(String downloadUrl, Map<String, String> headers) {
        req = new HttpGet(downloadUrl);
        if (MapUtils.isNotEmpty(headers)) {
            for (String headerKey: headers.keySet()) {
                req.setHeader(headerKey, headers.get(headerKey));
            }
        }
    }

    private SSLContext getSSLContext() throws KeyStoreException, NoSuchAlgorithmException, CertificateException, IOException, KeyManagementException {
        KeyStore trustStore  = KeyStore.getInstance("jks");
        FileInputStream instream = new FileInputStream(new File("/etc/cloudstack/agent/cloud.jks"));
        try {
            String privatePasswordFormat = "sed -n '/keystore.passphrase/p' '%s' 2>/dev/null  | sed 's/keystore.passphrase=//g' 2>/dev/null";
            String privatePasswordCmd = String.format(privatePasswordFormat, "/etc/cloudstack/agent/agent.properties");
            String privatePassword = Script.runSimpleBashScript(privatePasswordCmd);
            trustStore.load(instream, privatePassword.toCharArray());
        } finally {
            instream.close();
        }
        return SSLContexts.custom()
                .loadTrustMaterial(trustStore, new TrustSelfSignedStrategy())
                .build();
    }

    @Override
    public Pair<Boolean, String> downloadTemplate() {
        CloseableHttpResponse response;
        try {
            response = httpsClient.execute(req);
        } catch (IOException e) {
            throw new CloudRuntimeException("Error on HTTPS request: " + e.getMessage());
        }
        return consumeResponse(response);
    }

    /**
     * Consume response and persist it on getDownloadedFilePath() file
     */
    protected Pair<Boolean, String> consumeResponse(CloseableHttpResponse response) {
        s_logger.info("Downloading template " + getTemplateId() + " from " + getUrl() + " to: " + getDownloadedFilePath());
        if (response.getStatusLine().getStatusCode() != 200) {
            throw new CloudRuntimeException("Error on HTTPS response");
        }
        try {
            HttpEntity entity = response.getEntity();
            InputStream in = entity.getContent();
            OutputStream out = new FileOutputStream(getDownloadedFilePath());
            IOUtils.copy(in, out);
        } catch (Exception e) {
            s_logger.error("Error parsing response for template " + getTemplateId() + " due to: " + e.getMessage());
            return new Pair<>(false, null);
        }
        return new Pair<>(true, getDownloadedFilePath());
    }

}
