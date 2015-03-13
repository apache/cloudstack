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

package com.cloud.utils.rest;

import com.google.gson.FieldNamingPolicy;
import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import com.google.gson.JsonDeserializer;
import com.google.gson.reflect.TypeToken;
import org.apache.cloudstack.utils.security.SSLUtils;
import org.apache.cloudstack.utils.security.SecureSSLSocketFactory;
import org.apache.commons.httpclient.ConnectTimeoutException;
import org.apache.commons.httpclient.HttpClient;
import org.apache.commons.httpclient.HttpException;
import org.apache.commons.httpclient.HttpMethod;
import org.apache.commons.httpclient.HttpMethodBase;
import org.apache.commons.httpclient.HttpStatus;
import org.apache.commons.httpclient.MultiThreadedHttpConnectionManager;
import org.apache.commons.httpclient.NameValuePair;
import org.apache.commons.httpclient.cookie.CookiePolicy;
import org.apache.commons.httpclient.methods.DeleteMethod;
import org.apache.commons.httpclient.methods.GetMethod;
import org.apache.commons.httpclient.methods.PostMethod;
import org.apache.commons.httpclient.methods.PutMethod;
import org.apache.commons.httpclient.methods.StringRequestEntity;
import org.apache.commons.httpclient.params.HttpConnectionParams;
import org.apache.commons.httpclient.protocol.Protocol;
import org.apache.commons.httpclient.protocol.ProtocolSocketFactory;
import org.apache.commons.httpclient.protocol.SecureProtocolSocketFactory;
import org.apache.log4j.Logger;

import javax.net.ssl.SSLContext;
import javax.net.ssl.SSLSocket;
import javax.net.ssl.SSLSocketFactory;
import javax.net.ssl.TrustManager;
import javax.net.ssl.X509TrustManager;
import java.io.IOException;
import java.io.UnsupportedEncodingException;
import java.lang.reflect.Type;
import java.net.InetAddress;
import java.net.InetSocketAddress;
import java.net.MalformedURLException;
import java.net.Socket;
import java.net.URL;
import java.net.UnknownHostException;
import java.security.KeyManagementException;
import java.security.NoSuchAlgorithmException;
import java.security.cert.X509Certificate;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.Map.Entry;

/**
 * This abstraction encapsulates client side code for REST service communication. It encapsulates
 * access in a delegate validation strategy. There may different implementations extending
 * {@link RESTValidationStrategy}, and any of them should mention the needed data to work.
 *
 * This connector allows the use of {@link JsonDeserializer} for specific classes. You can provide
 * in the constructor a list of classes and a list of deserializers for these classes. These should
 * be a correlated so that Nth deserializer is correctly mapped to Nth class.
 */
public class RESTServiceConnector {
    private static final String HTTPS = "https";
    protected static final String GET_METHOD_TYPE = "get";
    protected static final String DELETE_METHOD_TYPE = "delete";
    protected static final String PUT_METHOD_TYPE = "put";
    protected static final String POST_METHOD_TYPE = "post";
    private static final String TEXT_HTML_CONTENT_TYPE = "text/html";
    private static final String JSON_CONTENT_TYPE = "application/json";
    private static final String CONTENT_TYPE = "Content-Type";
    private static final int BODY_RESP_MAX_LEN = 1024;
    private static final int HTTPS_PORT = 443;

    private static final Logger s_logger = Logger.getLogger(RESTServiceConnector.class);

    protected final static String protocol = HTTPS;

    private final static MultiThreadedHttpConnectionManager s_httpClientManager = new MultiThreadedHttpConnectionManager();

    protected RESTValidationStrategy validation;

    private final HttpClient client;

    private final Gson gson;


    /**
     * Getter that may be needed only for test purpose
     *
     * @return
     */
    public Gson getGson() {
        return gson;
    }

    public RESTServiceConnector(final RESTValidationStrategy validationStrategy) {
        this(validationStrategy, null, null);
    }

    public RESTServiceConnector(final RESTValidationStrategy validationStrategy, final List<Class<?>> classList, final List<JsonDeserializer<?>> deserializerList) {
        validation = validationStrategy;
        client = createHttpClient();
        client.getParams().setCookiePolicy(CookiePolicy.BROWSER_COMPATIBILITY);

        try {
            // Cast to ProtocolSocketFactory to avoid the deprecated constructor with the SecureProtocolSocketFactory parameter
            Protocol.registerProtocol(HTTPS, new Protocol(HTTPS, (ProtocolSocketFactory)new TrustingProtocolSocketFactory(), HTTPS_PORT));
        } catch (final IOException e) {
            s_logger.warn("Failed to register the TrustingProtocolSocketFactory, falling back to default SSLSocketFactory", e);
        }

        final GsonBuilder gsonBuilder = new GsonBuilder();
        if(classList != null && deserializerList != null) {
            for(int i = 0; i < classList.size() && i < deserializerList.size(); i++) {
                gsonBuilder.registerTypeAdapter(classList.get(i), deserializerList.get(i));
            }
        }
        gson = gsonBuilder.setFieldNamingPolicy(FieldNamingPolicy.LOWER_CASE_WITH_UNDERSCORES).create();
    }

    public HttpClient createHttpClient() {
        return new HttpClient(s_httpClientManager);
    }

    public HttpMethod createMethod(final String type, final String uri) throws CloudstackRESTException {
        String url;
        try {
            url = new URL(protocol, validation.getHost(), uri).toString();
        } catch (final MalformedURLException e) {
            s_logger.error("Unable to build REST Service URL", e);
            throw new CloudstackRESTException("Unable to build Nicira API URL", e);
        }

        if (POST_METHOD_TYPE.equalsIgnoreCase(type)) {
            return new PostMethod(url);
        } else if (GET_METHOD_TYPE.equalsIgnoreCase(type)) {
            return new GetMethod(url);
        } else if (DELETE_METHOD_TYPE.equalsIgnoreCase(type)) {
            return new DeleteMethod(url);
        } else if (PUT_METHOD_TYPE.equalsIgnoreCase(type)) {
            return new PutMethod(url);
        } else {
            throw new CloudstackRESTException("Requesting unknown method type");
        }
    }

    public void setControllerAddress(final String address) {
        validation.setHost(address);
    }

    public void setAdminCredentials(final String username, final String password) {
        validation.setUser(username);
        validation.setPassword(password);
    }

    public <T> void executeUpdateObject(final T newObject, final String uri, final Map<String, String> parameters) throws CloudstackRESTException {

        final PutMethod pm = (PutMethod)createMethod(PUT_METHOD_TYPE, uri);
        pm.setRequestHeader(CONTENT_TYPE, JSON_CONTENT_TYPE);
        try {
            pm.setRequestEntity(new StringRequestEntity(gson.toJson(newObject), JSON_CONTENT_TYPE, null));
        } catch (final UnsupportedEncodingException e) {
            throw new CloudstackRESTException("Failed to encode json request body", e);
        }

        executeMethod(pm);

        if (pm.getStatusCode() != HttpStatus.SC_OK) {
            final String errorMessage = responseToErrorMessage(pm);
            pm.releaseConnection();
            s_logger.error("Failed to update object : " + errorMessage);
            throw new CloudstackRESTException("Failed to update object : " + errorMessage);
        }
        pm.releaseConnection();
    }

    @SuppressWarnings("unchecked")
    public <T> T executeCreateObject(final T newObject, final Type returnObjectType, final String uri, final Map<String, String> parameters)
            throws CloudstackRESTException {

        final PostMethod pm = (PostMethod)createMethod(POST_METHOD_TYPE, uri);
        pm.setRequestHeader(CONTENT_TYPE, JSON_CONTENT_TYPE);
        try {
            pm.setRequestEntity(new StringRequestEntity(gson.toJson(newObject), JSON_CONTENT_TYPE, null));
        } catch (final UnsupportedEncodingException e) {
            throw new CloudstackRESTException("Failed to encode json request body", e);
        }

        executeMethod(pm);

        if (pm.getStatusCode() != HttpStatus.SC_CREATED) {
            final String errorMessage = responseToErrorMessage(pm);
            pm.releaseConnection();
            s_logger.error("Failed to create object : " + errorMessage);
            throw new CloudstackRESTException("Failed to create object : " + errorMessage);
        }

        T result;
        try {
            result = (T)gson.fromJson(pm.getResponseBodyAsString(), TypeToken.get(newObject.getClass()).getType());
        } catch (final IOException e) {
            throw new CloudstackRESTException("Failed to decode json response body", e);
        } finally {
            pm.releaseConnection();
        }

        return result;
    }

    public void executeDeleteObject(final String uri) throws CloudstackRESTException {
        final DeleteMethod dm = (DeleteMethod)createMethod(DELETE_METHOD_TYPE, uri);
        dm.setRequestHeader(CONTENT_TYPE, JSON_CONTENT_TYPE);

        executeMethod(dm);

        if (dm.getStatusCode() != HttpStatus.SC_NO_CONTENT) {
            final String errorMessage = responseToErrorMessage(dm);
            dm.releaseConnection();
            s_logger.error("Failed to delete object : " + errorMessage);
            throw new CloudstackRESTException("Failed to delete object : " + errorMessage);
        }
        dm.releaseConnection();
    }

    @SuppressWarnings("unchecked")
    public <T> T executeRetrieveObject(final Type returnObjectType, final String uri, final Map<String, String> parameters) throws CloudstackRESTException {
        final GetMethod gm = (GetMethod)createMethod(GET_METHOD_TYPE, uri);
        gm.setRequestHeader(CONTENT_TYPE, JSON_CONTENT_TYPE);
        if (parameters != null && !parameters.isEmpty()) {
            final List<NameValuePair> nameValuePairs = new ArrayList<NameValuePair>(parameters.size());
            for (final Entry<String, String> e : parameters.entrySet()) {
                nameValuePairs.add(new NameValuePair(e.getKey(), e.getValue()));
            }
            gm.setQueryString(nameValuePairs.toArray(new NameValuePair[0]));
        }

        executeMethod(gm);

        if (gm.getStatusCode() != HttpStatus.SC_OK) {
            final String errorMessage = responseToErrorMessage(gm);
            gm.releaseConnection();
            s_logger.error("Failed to retrieve object : " + errorMessage);
            throw new CloudstackRESTException("Failed to retrieve object : " + errorMessage);
        }

        T returnValue;
        try {
            returnValue = (T)gson.fromJson(gm.getResponseBodyAsString(), returnObjectType);
        } catch (final IOException e) {
            s_logger.error("IOException while retrieving response body", e);
            throw new CloudstackRESTException(e);
        } finally {
            gm.releaseConnection();
        }
        return returnValue;
    }

    public void executeMethod(final HttpMethodBase method) throws CloudstackRESTException {
        try {
            validation.executeMethod(method, client, protocol);
        } catch (final HttpException e) {
            s_logger.error("HttpException caught while trying to connect to the REST Service", e);
            method.releaseConnection();
            throw new CloudstackRESTException("API call to REST Service Failed", e);
        } catch (final IOException e) {
            s_logger.error("IOException caught while trying to connect to the REST Service", e);
            method.releaseConnection();
            throw new CloudstackRESTException("API call to Nicira REST Service Failed", e);
        }
    }

    private String responseToErrorMessage(final HttpMethodBase method) {
        assert method.isRequestSent() : "no use getting an error message unless the request is sent";

        if (TEXT_HTML_CONTENT_TYPE.equals(method.getResponseHeader(CONTENT_TYPE).getValue())) {
            // The error message is the response content
            // Safety margin of 1024 characters, anything longer is probably useless
            // and will clutter the logs
            try {
                return method.getResponseBodyAsString(BODY_RESP_MAX_LEN);
            } catch (final IOException e) {
                s_logger.debug("Error while loading response body", e);
            }
        }

        // The default
        return method.getStatusText();
    }

    /* Some controllers use a self-signed certificate. The
     * TrustingProtocolSocketFactory will accept any provided
     * certificate when making an SSL connection to the SDN
     * Manager
     */
    private class TrustingProtocolSocketFactory implements SecureProtocolSocketFactory {

        private SSLSocketFactory ssf;

        public TrustingProtocolSocketFactory() throws IOException {
            // Create a trust manager that does not validate certificate chains
            final TrustManager[] trustAllCerts = new TrustManager[] {new X509TrustManager() {
                @Override
                public X509Certificate[] getAcceptedIssuers() {
                    return null;
                }

                @Override
                public void checkClientTrusted(final X509Certificate[] certs, final String authType) {
                    // Trust always
                }

                @Override
                public void checkServerTrusted(final X509Certificate[] certs, final String authType) {
                    // Trust always
                }
            }};

            try {
                // Install the all-trusting trust manager
                final SSLContext sc = SSLUtils.getSSLContext();
                sc.init(null, trustAllCerts, new java.security.SecureRandom());
                ssf = new SecureSSLSocketFactory(sc);
            } catch (final KeyManagementException e) {
                throw new IOException(e);
            } catch (final NoSuchAlgorithmException e) {
                throw new IOException(e);
            }
        }

        @Override
        public Socket createSocket(final String host, final int port) throws IOException {
            SSLSocket socket = (SSLSocket) ssf.createSocket(host, port);
            socket.setEnabledProtocols(SSLUtils.getSupportedProtocols(socket.getEnabledProtocols()));
            return socket;
        }

        @Override
        public Socket createSocket(final String address, final int port, final InetAddress localAddress, final int localPort) throws IOException, UnknownHostException {
            Socket socket = ssf.createSocket(address, port, localAddress, localPort);
            if (socket instanceof SSLSocket) {
                ((SSLSocket)socket).setEnabledProtocols(SSLUtils.getSupportedProtocols(((SSLSocket)socket).getEnabledProtocols()));
            }
            return socket;
        }

        @Override
        public Socket createSocket(final Socket socket, final String host, final int port, final boolean autoClose) throws IOException, UnknownHostException {
            Socket s = ssf.createSocket(socket, host, port, autoClose);
            if (s instanceof SSLSocket) {
                ((SSLSocket)s).setEnabledProtocols(SSLUtils.getSupportedProtocols(((SSLSocket)s).getEnabledProtocols()));
            }
            return s;
        }

        @Override
        public Socket createSocket(final String host, final int port, final InetAddress localAddress, final int localPort, final HttpConnectionParams params)
                throws IOException, UnknownHostException, ConnectTimeoutException {
            final int timeout = params.getConnectionTimeout();
            if (timeout == 0) {
                Socket socket = createSocket(host, port, localAddress, localPort);
                if (socket instanceof SSLSocket) {
                    ((SSLSocket)socket).setEnabledProtocols(SSLUtils.getSupportedProtocols(((SSLSocket)socket).getEnabledProtocols()));
                }
                return socket;
            } else {
                final Socket s = ssf.createSocket();
                if (s instanceof SSLSocket) {
                    ((SSLSocket)s).setEnabledProtocols(SSLUtils.getSupportedProtocols(((SSLSocket)s).getEnabledProtocols()));
                }
                s.bind(new InetSocketAddress(localAddress, localPort));
                s.connect(new InetSocketAddress(host, port), timeout);
                return s;
            }
        }
    }

}
