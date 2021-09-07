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
package org.apache.cloudstack.utils.redfish;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.io.UnsupportedEncodingException;
import java.net.URISyntaxException;
import java.nio.charset.StandardCharsets;
import java.security.KeyManagementException;
import java.security.NoSuchAlgorithmException;
import java.util.Base64;
import java.util.concurrent.TimeUnit;

import javax.net.ssl.HostnameVerifier;
import javax.net.ssl.HttpsURLConnection;
import javax.net.ssl.SSLContext;
import javax.net.ssl.SSLSession;
import javax.net.ssl.TrustManager;

import com.cloud.utils.nio.TrustAllManager;
import org.apache.commons.httpclient.HttpStatus;
import org.apache.commons.lang.StringUtils;
import org.apache.http.HttpResponse;
import org.apache.http.client.HttpClient;
import org.apache.http.client.methods.CloseableHttpResponse;
import org.apache.http.client.methods.HttpGet;
import org.apache.http.client.methods.HttpPost;
import org.apache.http.client.methods.HttpRequestBase;
import org.apache.http.client.utils.URIBuilder;
import org.apache.http.conn.ssl.SSLConnectionSocketFactory;
import org.apache.http.entity.StringEntity;
import org.apache.http.impl.client.CloseableHttpClient;
import org.apache.http.impl.client.HttpClientBuilder;
import org.apache.http.protocol.HTTP;
import org.apache.log4j.Logger;

import com.cloud.utils.net.NetUtils;
import com.google.common.net.InternetDomainName;
import com.google.gson.JsonArray;
import com.google.gson.JsonObject;
import com.google.gson.JsonParser;

/**
 * Provides support to a set of REST requests that can be sent to a Redfish Server. </br>
 * RedfishClient allows to gather the server Power State, and execute Reset
 * actions such as 'On', 'ForceOff', 'GracefulShutdown', 'GracefulRestart' etc.
 */
public class RedfishClient {

    private static final Logger LOGGER = Logger.getLogger(RedfishClient.class);

    private String username;
    private String password;
    private boolean useHttps;
    private boolean ignoreSsl;
    private int redfishRequestMaxRetries;

    private final static String SYSTEMS_URL_PATH = "redfish/v1/Systems/";
    private final static String COMPUTER_SYSTEM_RESET_URL_PATH = "/Actions/ComputerSystem.Reset";
    private final static String REDFISH_RESET_TYPE = "ResetType";
    private final static String POWER_STATE = "PowerState";
    private final static String APPLICATION_JSON = "application/json";
    private final static String ACCEPT = "accept";
    private final static String ODATA_ID = "@odata.id";
    private final static String MEMBERS = "Members";
    private final static String EXPECTED_HTTP_STATUS = "2XX";
    private final static int WAIT_FOR_REQUEST_RETRY = 2;


    /**
     * Redfish Command type: </br>
     * <b>ComputerSystemReset:</b> execute Redfish reset commands ({@link RedfishResetCmd}). </br>
     * <b>GetSystemId:</b> get the system ID. </br>
     * <b>GetPowerState:</b> used for get the system power state. </br>
     */
    public enum
    RedfishCmdType {
        ComputerSystemReset, GetSystemId, GetPowerState
    }

    /**
     * Redfish System Power State: </br>
     * <b>Off:</b> The state is powered Off. </br>
     * <b>On:</b> The state is powered On. </br>
     * <b>PoweringOff:</b> A temporary state between On and Off. </br>
     * <b>PoweringOn:</b> A temporary state between Off and On.
     */
    public enum RedfishPowerState {
        On, Off, PoweringOn, PoweringOff
    }

    /**
     * <ul>
     * <li><b>ForceOff:</b> Turn the unit off immediately (non-graceful shutdown).
     * <li><b>ForceOn:</b> Turn the unit on immediately.
     * <li><b>ForceRestart:</b> Perform an immediate (non-graceful) shutdown,
     * followed by a restart.
     * <li><b>GracefulRestart:</b> Perform a graceful shutdown followed by a restart
     * of the system.
     * <li><b>GracefulShutdown:</b> Perform a graceful shutdown and power off.
     * <li><b>Nmi:</b> Generate a Diagnostic Interrupt (usually an NMI on x86
     * systems) to cease normal operations, perform diagnostic actions and typically
     * halt the system.
     * <li><b>On:</b> Turn the unit on.
     * <li><b>PowerCycle:</b> Perform a power cycle of the unit.
     * <li><b>PushPowerButton:</b> Simulate the pressing of the physical power
     * button on this unit.
     * </ul>
     */
    public enum RedfishResetCmd {
        ForceOff, ForceOn, ForceRestart, GracefulRestart, GracefulShutdown, Nmi, On, PowerCycle, PushPowerButton
    }

    public RedfishClient(String username, String password, boolean useHttps, boolean ignoreSsl, int redfishRequestRetries) {
        this.username = username;
        this.password = password;
        this.useHttps = useHttps;
        this.ignoreSsl = ignoreSsl;
        this.redfishRequestMaxRetries = redfishRequestRetries;
    }

    protected String buildRequestUrl(String hostAddress, RedfishCmdType cmd, String resourceId) {
        String urlHostAddress = validateAddressAndPrepareForUrl(hostAddress);
        String requestPath = getRequestPathForCommand(cmd, resourceId);

        if (useHttps) {
            return String.format("https://%s/%s", urlHostAddress, requestPath);
        } else {
            return String.format("http://%s/%s", urlHostAddress, requestPath);
        }
    }

    /**
     * Executes a GET request for the given URL address.
     */
    protected HttpResponse executeGetRequest(String url) {
        URIBuilder builder = null;
        HttpGet httpReq = null;
        try {
            builder = new URIBuilder(url);
            httpReq = new HttpGet(builder.build());
        } catch (URISyntaxException e) {
            throw new RedfishException(String.format("Failed to create URI for GET request [URL: %s] due to exception.", url), e);
        }

        prepareHttpRequestBasicAuth(httpReq);
        return executeHttpRequest(url, httpReq);
    }

    /**
     * Executes a POST request for the given URL address and Json object.
     */
    private HttpResponse executePostRequest(String url, JsonObject jsonToSend) {
        HttpPost httpReq = null;
        try {
            URIBuilder builder = new URIBuilder(url);
            httpReq = new HttpPost(builder.build());
            httpReq.addHeader(HTTP.CONTENT_TYPE, APPLICATION_JSON);
            httpReq.setEntity(new StringEntity(jsonToSend.toString()));
        } catch (URISyntaxException | UnsupportedEncodingException e) {
            throw new RedfishException(String.format("Failed to create URI for POST request [URL: %s] due to exception.", url), e);
        }

        prepareHttpRequestBasicAuth(httpReq);
        return executeHttpRequest(url, httpReq);
    }

    /**
     * Prepare http request to accept JSON and basic authentication
     */
    private void prepareHttpRequestBasicAuth(HttpRequestBase httpReq) {
        httpReq.addHeader(ACCEPT, APPLICATION_JSON);
        String encoding = basicAuth(username, password);
        httpReq.addHeader("Authorization", encoding);
    }

    /**
     * Encodes 'username:password' into 64-base encoded String
     */
    private static String basicAuth(String username, String password) {
        return "Basic " + Base64.getEncoder().encodeToString((username + ":" + password).getBytes());
    }

    /**
     * Executes Http request according to URL and HttpRequestBase (e.g. HttpGet, HttpPost)
     */
    private HttpResponse executeHttpRequest(String url, HttpRequestBase httpReq) {
        HttpClient client = null;
        if (ignoreSsl) {
            try {
                client = ignoreSSLCertValidator();
            } catch (NoSuchAlgorithmException | KeyManagementException e) {
                throw new RedfishException(String.format("Failed to handle SSL Cert validator on POST request [URL: %s] due to exception.", url), e);
            }
        } else {
            client = HttpClientBuilder.create().build();
        }

        try {
            return client.execute(httpReq);
        } catch (IOException e) {
            if (redfishRequestMaxRetries == 0) {
                throw new RedfishException(String.format("Failed to execute HTTP %s request [URL: %s] due to exception %s.", httpReq.getMethod(), url, e), e);
            }
            return retryHttpRequest(url, httpReq, client);
        }
    }

    protected HttpResponse retryHttpRequest(String url, HttpRequestBase httpReq, HttpClient client) {
        LOGGER.warn(String.format("Failed to execute HTTP %s request [URL: %s]. Executing the request again.", httpReq.getMethod(), url));
        HttpResponse response = null;
        for (int attempt = 1; attempt < redfishRequestMaxRetries + 1; attempt++) {
            try {
                TimeUnit.SECONDS.sleep(WAIT_FOR_REQUEST_RETRY);
                LOGGER.debug(String.format("HTTP %s request retry attempt %d/%d [URL: %s].", httpReq.getMethod(), attempt, redfishRequestMaxRetries, url));
                response = client.execute(httpReq);
                break;
            } catch (IOException | InterruptedException e) {
                if (attempt == redfishRequestMaxRetries) {
                    throw new RedfishException(String.format("Failed to execute HTTP %s request retry attempt %d/%d [URL: %s] due to exception %s", httpReq.getMethod(), attempt, redfishRequestMaxRetries,url, e));
                } else {
                    LOGGER.warn(
                            String.format("Failed to execute HTTP %s request retry attempt %d/%d [URL: %s] due to exception %s", httpReq.getMethod(), attempt, redfishRequestMaxRetries,
                                    url, e));
                }
            }
        }

        if (response == null) {
            throw new RedfishException(String.format("Failed to execute HTTP %s request [URL: %s].", httpReq.getMethod(), url));
        }

        LOGGER.debug(String.format("Successfully executed HTTP %s request [URL: %s].", httpReq.getMethod(), url));
        return response;
    }

    /**
     *  Returns the proper URL path for the given Redfish command ({@link RedfishCmdType}).
     */
    private String getRequestPathForCommand(RedfishCmdType cmd, String resourceId) {
        switch (cmd) {
        case GetSystemId:
            return SYSTEMS_URL_PATH;
        case GetPowerState:
            if (StringUtils.isBlank(resourceId)) {
                throw new RedfishException(String.format("Command '%s' requires a valid resource ID '%s'.", cmd, resourceId));
            }
            return String.format("%s%s", SYSTEMS_URL_PATH, resourceId);
        case ComputerSystemReset:
            if (StringUtils.isBlank(resourceId)) {
                throw new RedfishException(String.format("Command '%s' requires a valid resource ID '%s'.", cmd, resourceId));
            }
            return String.format("%s%s%s", SYSTEMS_URL_PATH, resourceId, COMPUTER_SYSTEM_RESET_URL_PATH);
        default:
            throw new RedfishException(String.format("Redfish client does not support command '%s'.", cmd));
        }
    }

    /**
     * Validates the host address. It needs to be either a valid host domain name, or a valid IP address (IPv6 or IPv4).
     */
    protected String validateAddressAndPrepareForUrl(String hostAddress) {
        if (NetUtils.isValidIp6(hostAddress)) {
            return String.format("[%s]", hostAddress);
        } else if (NetUtils.isValidIp4(hostAddress)) {
            return hostAddress;
        } else if (InternetDomainName.isValid(hostAddress)) {
            return hostAddress;
        } else {
            throw new RedfishException(String.format("Redfish host address '%s' is not a valid IPv4/IPv6 address nor a valid domain name.", hostAddress));
        }
    }

    /**
     * Sends a POST request for a ComputerSystem with the Reset command ({@link RedfishResetCmd}, e.g. RedfishResetCmd.GracefulShutdown). </br> </br>
     * <b>URL example:</b> https://[host address]/redfish/v1/Systems/[System ID]/Actions/ComputerSystem.Reset
     */
    public void executeComputerSystemReset(String hostAddress, RedfishResetCmd resetCommand) {
        String systemId = getSystemId(hostAddress);
        String url = buildRequestUrl(hostAddress, RedfishCmdType.ComputerSystemReset, systemId);
        JsonObject resetType = new JsonObject();
        resetType.addProperty(REDFISH_RESET_TYPE, resetCommand.toString());

        CloseableHttpResponse response = (CloseableHttpResponse)executePostRequest(url, resetType);

        int statusCode = response.getStatusLine().getStatusCode();
        if (statusCode < HttpStatus.SC_OK || statusCode >= HttpStatus.SC_MULTIPLE_CHOICES) {
            throw new RedfishException(String.format("Failed to get System power state for host '%s' with request '%s: %s'. The expected HTTP status code is '%s' but it got '%s'.",
                    HttpGet.METHOD_NAME, url, hostAddress, EXPECTED_HTTP_STATUS, statusCode));
        }
        LOGGER.debug(String.format("Sending ComputerSystem.Reset Command '%s' to host '%s' with request '%s %s'", resetCommand, hostAddress, HttpPost.METHOD_NAME, url));
    }

    /**
     *  Returns the System ID. Used when sending Computer System requests (e.g. ComputerSystem.Reset request).
     */
    public String getSystemId(String hostAddress) {
        String url = buildRequestUrl(hostAddress, RedfishCmdType.GetSystemId, null);
        CloseableHttpResponse response = (CloseableHttpResponse)executeGetRequest(url);

        int statusCode = response.getStatusLine().getStatusCode();
        if (statusCode != HttpStatus.SC_OK) {
            throw new RedfishException(String.format("Failed to get System ID for host '%s' with request '%s: %s'. HTTP status code expected '%s' but it got '%s'.", hostAddress,
                    HttpGet.METHOD_NAME, url, HttpStatus.SC_OK, statusCode));
        }

        String systemId = processGetSystemIdResponse(response);

        LOGGER.debug(String.format("Retrieved System ID '%s' with request '%s: %s'", systemId, HttpGet.METHOD_NAME, url));

        return systemId;
    }

    /**
     * Processes the response of request GET System ID as a JSON object.
     */
    protected String processGetSystemIdResponse(CloseableHttpResponse response) {
        InputStream in;
        String jsonString;
        try {
            in = response.getEntity().getContent();
            BufferedReader streamReader = new BufferedReader(new InputStreamReader(in, StandardCharsets.UTF_8));
            jsonString = streamReader.readLine();
        } catch (UnsupportedOperationException | IOException e) {
            throw new RedfishException("Failed to process system Response", e);
        }

        // retrieving the system ID (e.g. 'System.Embedded.1') via JsonParser:
        // (...) Members":[{"@odata.id":"/redfish/v1/Systems/System.Embedded.1"}] (...)
        JsonArray jArray = new JsonParser().parse(jsonString).getAsJsonObject().get(MEMBERS).getAsJsonArray();
        JsonObject jsonnObject = jArray.get(0).getAsJsonObject();
        String jsonObjectAsString = jsonnObject.get(ODATA_ID).getAsString();
        String[] arrayOfStrings = StringUtils.split(jsonObjectAsString, '/');

        return arrayOfStrings[arrayOfStrings.length - 1];
    }

    /**
     * Returns the Redfish system Power State ({@link RedfishPowerState}).
     */
    public RedfishPowerState getSystemPowerState(String hostAddress) {
        String systemId = getSystemId(hostAddress);

        String url = buildRequestUrl(hostAddress, RedfishCmdType.GetPowerState, systemId);
        CloseableHttpResponse response = (CloseableHttpResponse)executeGetRequest(url);

        int statusCode = response.getStatusLine().getStatusCode();
        if (statusCode != HttpStatus.SC_OK) {
            throw new RedfishException(String.format("Failed to get System power state for host '%s' with request '%s: %s'. The expected HTTP status code is '%s' but it got '%s'.",
                    HttpGet.METHOD_NAME, url, hostAddress, HttpStatus.SC_OK, statusCode));
        }

        RedfishPowerState powerState = processGetSystemRequestResponse(response);
        LOGGER.debug(String.format("Retrieved System power state '%s' with request '%s: %s'", powerState, HttpGet.METHOD_NAME, url));
        return powerState;
    }

    /**
     * Process 'ComputerSystem' GET request response and return as a RedfishPowerState
     */
    protected RedfishPowerState processGetSystemRequestResponse(CloseableHttpResponse response) {
        InputStream in;
        String jsonString = null;
        try {
            in = response.getEntity().getContent();
            BufferedReader streamReader = new BufferedReader(new InputStreamReader(in, StandardCharsets.UTF_8));

            jsonString = streamReader.readLine();
            String powerState = new JsonParser().parse(jsonString).getAsJsonObject().get(POWER_STATE).getAsString();
            return RedfishPowerState.valueOf(powerState);
        } catch (UnsupportedOperationException | IOException e) {
            throw new RedfishException("Failed to process system response due to exception", e);
        }
    }

    /**
     * Ignores SSL certififcation validator.
     */
    private CloseableHttpClient ignoreSSLCertValidator() throws NoSuchAlgorithmException, KeyManagementException {
        TrustManager[] trustAllCerts = new TrustManager[]{new TrustAllManager()};

        SSLContext sslContext = SSLContext.getInstance("SSL");
        sslContext.init(null, trustAllCerts, new java.security.SecureRandom());
        HttpsURLConnection.setDefaultSSLSocketFactory(sslContext.getSocketFactory());

        HostnameVerifier allHostsValid = new HostnameVerifier() {
            public boolean verify(String hostname, SSLSession session) {
                return true;
            }
        };

        HttpsURLConnection.setDefaultHostnameVerifier(allHostsValid);
        SSLConnectionSocketFactory socketFactory = new SSLConnectionSocketFactory(sslContext, allHostsValid);
        return HttpClientBuilder.create().setSSLSocketFactory(socketFactory).build();
    }
}
