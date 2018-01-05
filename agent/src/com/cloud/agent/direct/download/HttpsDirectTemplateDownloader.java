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

import com.cloud.utils.exception.CloudRuntimeException;
import com.cloud.utils.script.Script;
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
import java.io.IOException;
import java.security.KeyManagementException;
import java.security.KeyStore;
import java.security.KeyStoreException;
import java.security.NoSuchAlgorithmException;
import java.security.cert.CertificateException;

public class HttpsDirectTemplateDownloader extends HttpDirectTemplateDownloader {

    private CloseableHttpClient httpsClient;
    private HttpUriRequest req;

    public HttpsDirectTemplateDownloader(String url, Long templateId, String destPoolPath, String checksum) {
        super(url, templateId, destPoolPath, checksum, null);
        SSLContext sslcontext = null;
        try {
            sslcontext = getSSLContext();
        } catch (KeyStoreException | NoSuchAlgorithmException | CertificateException | IOException | KeyManagementException e) {
            throw new CloudRuntimeException("Failure getting SSL context for HTTPS downloader: " + e.getMessage());
        }
        SSLConnectionSocketFactory factory = new SSLConnectionSocketFactory(sslcontext, SSLConnectionSocketFactory.ALLOW_ALL_HOSTNAME_VERIFIER);
        httpsClient = HttpClients.custom().setSSLSocketFactory(factory).build();
        req = createUriRequest(url);
    }

    protected HttpUriRequest createUriRequest(String downloadUrl) {
        return new HttpGet(downloadUrl);
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
    public boolean downloadTemplate() {
        try {
            httpsClient.execute(req);
        } catch (IOException e) {
            throw new CloudRuntimeException("Error on HTTPS request: " + e.getMessage());
        }
        return performDownload();
    }
}