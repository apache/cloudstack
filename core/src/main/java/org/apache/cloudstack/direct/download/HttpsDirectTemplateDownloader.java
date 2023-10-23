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

import java.io.ByteArrayInputStream;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.net.URL;
import java.nio.charset.StandardCharsets;
import java.security.KeyManagementException;
import java.security.KeyStore;
import java.security.KeyStoreException;
import java.security.NoSuchAlgorithmException;
import java.security.cert.CertificateException;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;

import javax.net.ssl.HttpsURLConnection;
import javax.net.ssl.SSLContext;
import javax.net.ssl.TrustManager;

import org.apache.cloudstack.utils.security.SSLUtils;
import org.apache.commons.collections.MapUtils;
import org.apache.commons.httpclient.HttpStatus;
import org.apache.commons.io.IOUtils;
import org.apache.http.Header;
import org.apache.http.HttpEntity;
import org.apache.http.client.config.RequestConfig;
import org.apache.http.client.methods.CloseableHttpResponse;
import org.apache.http.client.methods.HttpGet;
import org.apache.http.client.methods.HttpHead;
import org.apache.http.client.methods.HttpUriRequest;
import org.apache.http.conn.ssl.SSLConnectionSocketFactory;
import org.apache.http.impl.client.CloseableHttpClient;
import org.apache.http.impl.client.HttpClients;
import org.apache.http.util.EntityUtils;

import com.cloud.utils.Pair;
import com.cloud.utils.UriUtils;
import com.cloud.utils.exception.CloudRuntimeException;
import com.cloud.utils.script.Script;
import com.cloud.utils.storage.QCOW2Utils;

public class HttpsDirectTemplateDownloader extends DirectTemplateDownloaderImpl {

    protected CloseableHttpClient httpsClient;
    private HttpUriRequest req;

    protected HttpsDirectTemplateDownloader(String url, Integer connectTimeout, Integer connectionRequestTimeout, Integer socketTimeout) {
        this(url, null, null, null, null, connectTimeout, socketTimeout, connectionRequestTimeout, null);
    }

    public HttpsDirectTemplateDownloader(String url, Long templateId, String destPoolPath, String checksum, Map<String, String> headers,
                                         Integer connectTimeout, Integer soTimeout, Integer connectionRequestTimeout, String temporaryDownloadPath) {
        super(url, destPoolPath, templateId, checksum, temporaryDownloadPath);
        SSLContext sslcontext = getSSLContext();
        SSLConnectionSocketFactory factory = new SSLConnectionSocketFactory(sslcontext, SSLConnectionSocketFactory.ALLOW_ALL_HOSTNAME_VERIFIER);
        RequestConfig config = RequestConfig.custom()
                .setConnectTimeout(connectTimeout == null ? 5000 : connectTimeout)
                .setConnectionRequestTimeout(connectionRequestTimeout == null ? 5000 : connectionRequestTimeout)
                .setSocketTimeout(soTimeout == null ? 5000 : soTimeout).build();
        httpsClient = HttpClients.custom().setSSLSocketFactory(factory).setDefaultRequestConfig(config).build();
        createUriRequest(url, headers);
        String downloadDir = getDirectDownloadTempPath(templateId);
        File tempFile = createTemporaryDirectoryAndFile(downloadDir);
        setDownloadedFilePath(tempFile.getAbsolutePath());
    }

    protected void createUriRequest(String downloadUrl, Map<String, String> headers) {
        req = new HttpGet(downloadUrl);
        if (MapUtils.isNotEmpty(headers)) {
            for (String headerKey: headers.keySet()) {
                req.setHeader(headerKey, headers.get(headerKey));
            }
        }
    }

    private SSLContext getSSLContext() {
        try {
            KeyStore customKeystore  = KeyStore.getInstance("jks");
            try (FileInputStream instream = new FileInputStream(new File("/etc/cloudstack/agent/cloud.jks"))) {
                String privatePasswordFormat = "sed -n '/keystore.passphrase/p' '%s' 2>/dev/null  | sed 's/keystore.passphrase=//g' 2>/dev/null";
                String privatePasswordCmd = String.format(privatePasswordFormat, "/etc/cloudstack/agent/agent.properties");
                String privatePassword = Script.runSimpleBashScript(privatePasswordCmd);
                customKeystore.load(instream, privatePassword.toCharArray());
            }
            KeyStore defaultKeystore = KeyStore.getInstance(KeyStore.getDefaultType());
            String relativeCacertsPath = "/lib/security/cacerts".replace("/", File.separator);
            String filename = System.getProperty("java.home") + relativeCacertsPath;
            try (FileInputStream is = new FileInputStream(filename)) {
                String password = "changeit";
                defaultKeystore.load(is, password.toCharArray());
            }
            TrustManager[] tm = HttpsMultiTrustManager.getTrustManagersFromKeyStores(customKeystore, defaultKeystore);
            SSLContext sslContext = SSLUtils.getSSLContext();
            sslContext.init(null, tm, null);
            return sslContext;
        } catch (KeyStoreException | NoSuchAlgorithmException | CertificateException | IOException | KeyManagementException e) {
            s_logger.error(String.format("Failure getting SSL context for HTTPS downloader, using default SSL context: %s", e.getMessage()), e);
            try {
                return SSLContext.getDefault();
            } catch (NoSuchAlgorithmException ex) {
                throw new CloudRuntimeException(String.format("Cannot return the default SSL context due to: %s", ex.getMessage()), e);
            }
        }

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

    @Override
    public boolean checkUrl(String url) {
        HttpHead httpHead = new HttpHead(url);
        try {
            CloseableHttpResponse response = httpsClient.execute(httpHead);
            if (response.getStatusLine().getStatusCode() != HttpStatus.SC_OK) {
                s_logger.error(String.format("Invalid URL: %s", url));
                return false;
            }
            return true;
        } catch (IOException e) {
            s_logger.error(String.format("Cannot reach URL: %s due to: %s", url, e.getMessage()), e);
            return false;
        } finally {
            httpHead.releaseConnection();
        }
    }

    @Override
    public Long getRemoteFileSize(String url, String format) {
        if ("qcow2".equalsIgnoreCase(format)) {
            try {
                URL urlObj = new URL(url);
                HttpsURLConnection urlConnection = (HttpsURLConnection)urlObj.openConnection();
                SSLContext context = getSSLContext();
                urlConnection.setSSLSocketFactory(context.getSocketFactory());
                urlConnection.connect();
                return QCOW2Utils.getVirtualSize(urlObj.openStream(), UriUtils.isUrlForCompressedFile(url));
            } catch (IOException e) {
                throw new CloudRuntimeException(String.format("Cannot obtain qcow2 virtual size due to: %s", e.getMessage()), e);
            }
        } else {
            HttpHead httpHead = new HttpHead(url);
            CloseableHttpResponse response = null;
            try {
                response = httpsClient.execute(httpHead);
                Header[] headers = response.getHeaders("Content-Length");
                for (Header header : headers) {
                    return Long.parseLong(header.getValue());
                }
            } catch (IOException e) {
                throw new RuntimeException(e);
            }
            return null;
        }
    }

    @Override
    public List<String> getMetalinkUrls(String metalinkUrl) {
        HttpGet getMethod = new HttpGet(metalinkUrl);
        List<String> urls = new ArrayList<>();
        CloseableHttpResponse response;
        try {
            response = httpsClient.execute(getMethod);
            if (response.getStatusLine().getStatusCode() != HttpStatus.SC_OK) {
                String msg = String.format("Cannot access metalink content on URL %s", metalinkUrl);
                s_logger.error(msg);
                throw new IOException(msg);
            }
        } catch (IOException e) {
            s_logger.error(String.format("Error retrieving urls form metalink URL %s: %s", metalinkUrl, e.getMessage()), e);
            getMethod.releaseConnection();
            return null;
        }

        try {
            String responseStr = EntityUtils.toString(response.getEntity());
            ByteArrayInputStream inputStream = new ByteArrayInputStream(responseStr.getBytes(StandardCharsets.UTF_8));
            addMetalinkUrlsToListFromInputStream(inputStream, urls);
        } catch (IOException e) {
            s_logger.warn(e.getMessage(), e);
        } finally {
            getMethod.releaseConnection();
        }
        return urls;
    }

    @Override
    public List<String> getMetalinkChecksums(String metalinkUrl) {
        HttpGet getMethod = new HttpGet(metalinkUrl);
        try {
            CloseableHttpResponse response = httpsClient.execute(getMethod);
            if (response.getStatusLine().getStatusCode() == HttpStatus.SC_OK) {
                InputStream is = response.getEntity().getContent();
                return generateChecksumListFromInputStream(is);
            }
        } catch (IOException e) {
            s_logger.error(String.format("Error obtaining metalink checksums on URL %s: %s", metalinkUrl, e.getMessage()), e);
        } finally {
            getMethod.releaseConnection();
        }
        return null;
    }
}
