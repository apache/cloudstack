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

package org.apache.cloudstack.network.opendaylight.api;

import org.apache.cloudstack.utils.security.SSLUtils;
import org.apache.cloudstack.utils.security.SecureSSLSocketFactory;
import org.apache.commons.httpclient.ConnectTimeoutException;
import org.apache.commons.httpclient.HttpClient;
import org.apache.commons.httpclient.HttpException;
import org.apache.commons.httpclient.HttpMethodBase;
import org.apache.commons.httpclient.MultiThreadedHttpConnectionManager;
import org.apache.commons.httpclient.cookie.CookiePolicy;
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
import java.lang.reflect.Constructor;
import java.lang.reflect.InvocationTargetException;
import java.net.InetAddress;
import java.net.InetSocketAddress;
import java.net.MalformedURLException;
import java.net.Socket;
import java.net.URL;
import java.net.UnknownHostException;
import java.security.KeyManagementException;
import java.security.NoSuchAlgorithmException;
import java.security.cert.X509Certificate;

public class NeutronRestApi {

    private static final Logger s_logger = Logger.getLogger(NeutronRestApi.class);
    private static final MultiThreadedHttpConnectionManager s_httpClientManager = new MultiThreadedHttpConnectionManager();

    private static final String PROTOCOL = "https";
    private static final int HTTPS_PORT = 443;

    private final HttpClient client;

    private Class<? extends HttpMethodBase> httpClazz;

    protected NeutronRestApi(final Class<? extends HttpMethodBase> httpClazz) {
        this(httpClazz, PROTOCOL, HTTPS_PORT);
    }

    protected NeutronRestApi(final Class<? extends HttpMethodBase> httpClazz, final String protocol, final int port) {
        client = createHttpClient();
        client.getParams().setCookiePolicy(CookiePolicy.BROWSER_COMPATIBILITY);
        this.httpClazz = httpClazz;

        try {
            // Cast to ProtocolSocketFactory to avoid the deprecated constructor
            // with the SecureProtocolSocketFactory parameter
            Protocol.registerProtocol(protocol, new Protocol(protocol, (ProtocolSocketFactory) new TrustingProtocolSocketFactory(), HTTPS_PORT));
        } catch (IOException e) {
            s_logger.warn("Failed to register the TrustingProtocolSocketFactory, falling back to default SSLSocketFactory", e);
        }
    }

    public Class<? extends HttpMethodBase> getHttpClazz() {
        return httpClazz;
    }

    public HttpMethodBase createMethod(final URL neutronUrl, final String uri) throws NeutronRestApiException {
        String url;
        try {
            String formattedUrl = neutronUrl.toString() + uri;
            url = new URL(formattedUrl).toString();

            Constructor<? extends HttpMethodBase> httpMethodConstructor = httpClazz.getConstructor(String.class);
            HttpMethodBase httpMethod = httpMethodConstructor.newInstance(url);

            return httpMethod;
        } catch (MalformedURLException e) {
            String error = "Unable to build Neutron API URL";
            s_logger.error(error, e);
            throw new NeutronRestApiException(error, e);
        } catch (NoSuchMethodException e) {
            String error = "Unable to build Neutron API URL due to reflection error";
            s_logger.error(error, e);
            throw new NeutronRestApiException(error, e);
        } catch (SecurityException e) {
            String error = "Unable to build Neutron API URL due to security violation";
            s_logger.error(error, e);
            throw new NeutronRestApiException(error, e);
        } catch (InstantiationException e) {
            String error = "Unable to build Neutron API due to instantiation error";
            s_logger.error(error, e);
            throw new NeutronRestApiException(error, e);
        } catch (IllegalAccessException e) {
            String error = "Unable to build Neutron API URL due to absence of access modifier";
            s_logger.error(error, e);
            throw new NeutronRestApiException(error, e);
        } catch (IllegalArgumentException e) {
            String error = "Unable to build Neutron API URL due to wrong argument in constructor";
            s_logger.error(error, e);
            throw new NeutronRestApiException(error, e);
        } catch (InvocationTargetException e) {
            String error = "Unable to build Neutron API URL due to target error";
            s_logger.error(error, e);
            throw new NeutronRestApiException(error, e);
        }
    }

    public void executeMethod(final HttpMethodBase method) throws NeutronRestApiException {
        try {
            client.executeMethod(method);
        } catch (HttpException e) {
            s_logger.error("HttpException caught while trying to connect to the Neutron Controller", e);
            method.releaseConnection();
            throw new NeutronRestApiException("API call to Neutron Controller Failed", e);
        } catch (IOException e) {
            s_logger.error("IOException caught while trying to connect to the Neutron Controller", e);
            method.releaseConnection();
            throw new NeutronRestApiException("API call to Neutron Controller Failed", e);
        }
    }

    /*
     * This factory method is protected so we can extend this in the unit tests.
     */
    protected HttpClient createHttpClient() {
        return new HttpClient(s_httpClientManager);
    }

    /*
     * It uses a self-signed certificate. The TrustingProtocolSocketFactory will
     * accept any provided certificate when making an SSL connection to the SDN
     * Manager
     */
    private class TrustingProtocolSocketFactory implements SecureProtocolSocketFactory {

        private SSLSocketFactory ssf;

        public TrustingProtocolSocketFactory() throws IOException {
            // Create a trust manager that does not validate certificate chains
            TrustManager[] trustAllCerts = new TrustManager[] { new X509TrustManager() {
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
            } };

            try {
                // Install the all-trusting trust manager
                SSLContext sc = SSLUtils.getSSLContext();
                sc.init(null, trustAllCerts, new java.security.SecureRandom());
                ssf = new SecureSSLSocketFactory(sc);
            } catch (KeyManagementException e) {
                throw new IOException(e);
            } catch (NoSuchAlgorithmException e) {
                throw new IOException(e);
            }
        }

        @Override
        public Socket createSocket(final String host, final int port) throws IOException {
            Socket s = ssf.createSocket(host, port);
            if (s instanceof SSLSocket) {
                ((SSLSocket)s).setEnabledProtocols(SSLUtils.getSupportedProtocols(((SSLSocket)s).getEnabledProtocols()));
            }
            return s;
        }

        @Override
        public Socket createSocket(final String address, final int port, final InetAddress localAddress, final int localPort) throws IOException, UnknownHostException {
            Socket s = ssf.createSocket(address, port, localAddress, localPort);
            if (s instanceof SSLSocket) {
                ((SSLSocket)s).setEnabledProtocols(SSLUtils.getSupportedProtocols(((SSLSocket)s).getEnabledProtocols()));
            }
            return s;
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
        public Socket createSocket(final String host, final int port, final InetAddress localAddress, final int localPort, final HttpConnectionParams params) throws IOException,
                UnknownHostException, ConnectTimeoutException {
            int timeout = params.getConnectionTimeout();
            if (timeout == 0) {
                Socket s = createSocket(host, port, localAddress, localPort);
                if (s instanceof SSLSocket) {
                    ((SSLSocket)s).setEnabledProtocols(SSLUtils.getSupportedProtocols(((SSLSocket)s).getEnabledProtocols()));
                }
                return s;
            } else {
                Socket s = ssf.createSocket();
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
