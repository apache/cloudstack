// Licensed to the Apache Software Foundation (ASF) under one
// or more contributor license agreements.  See the NOTICE file
// distributed with this work for additional information
// regarding copyright ownership.  The ASF licenses this file
// to you under the Apache License, Version 2.0 (the
// "License"); you may not use this file except in compliance
// the License.  You may obtain a copy of the License at
//
// http://www.apache.org/licenses/LICENSE-2.0
//
// Unless required by applicable law or agreed to in writing,
// software distributed under the License is distributed on an
// "AS IS" BASIS, WITHOUT WARRANTIES OR CONDITIONS OF ANY
// KIND, either express or implied.  See the License for the
// specific language governing permissions and limitations
// under the License.
package com.cloud.utils;

import java.io.File;
import java.io.IOException;
import java.io.InputStream;
import java.net.HttpURLConnection;
import java.net.Inet6Address;
import java.net.InetAddress;
import java.net.URI;
import java.net.URISyntaxException;
import java.net.URLEncoder;
import java.net.UnknownHostException;
import java.util.List;

import javax.net.ssl.HttpsURLConnection;

import org.apache.commons.httpclient.Credentials;
import org.apache.commons.httpclient.HttpClient;
import org.apache.commons.httpclient.HttpStatus;
import org.apache.commons.httpclient.MultiThreadedHttpConnectionManager;
import org.apache.commons.httpclient.UsernamePasswordCredentials;
import org.apache.commons.httpclient.auth.AuthScope;
import org.apache.commons.httpclient.methods.GetMethod;
import org.apache.http.NameValuePair;
import org.apache.http.client.utils.URLEncodedUtils;
import org.apache.log4j.Logger;

import com.cloud.utils.exception.CloudRuntimeException;

public class UriUtils {

    public static final Logger s_logger = Logger.getLogger(UriUtils.class.getName());

    public static String formNfsUri(String host, String path) {
        try {
            URI uri = new URI("nfs", host, path, null);
            return uri.toString();
        } catch (URISyntaxException e) {
            throw new CloudRuntimeException("Unable to form nfs URI: " + host + " - " + path);
        }
    }

    public static String formIscsiUri(String host, String iqn, Integer lun) {
        try {
            String path = iqn;
            if (lun != null) {
                path += "/" + lun.toString();
            }
            URI uri = new URI("iscsi", host, path, null);
            return uri.toString();
        } catch (URISyntaxException e) {
            throw new CloudRuntimeException("Unable to form iscsi URI: " + host + " - " + iqn + " - " + lun);
        }
    }

    public static String formFileUri(String path) {
        File file = new File(path);

        return file.toURI().toString();
    }

    // a simple URI component helper (Note: it does not deal with URI paramemeter area)
    public static String encodeURIComponent(String url) {
        int schemeTail = url.indexOf("://");

        int pathStart = 0;
        if(schemeTail > 0)
            pathStart = url.indexOf('/', schemeTail + 3);
        else
            pathStart = url.indexOf('/');

        if(pathStart > 0) {
            String[] tokens = url.substring(pathStart + 1).split("/");
            if(tokens != null) {
                StringBuffer sb = new StringBuffer();
                sb.append(url.substring(0, pathStart));
                for(String token : tokens) {
                    sb.append("/").append(URLEncoder.encode(token));
                }

                return sb.toString();
            }
        }

        // no need to do URL component encoding
        return url;
    }

    public static String getCifsUriParametersProblems(URI uri) {
        if (!UriUtils.hostAndPathPresent(uri)) {
            String errMsg = "cifs URI missing host and/or path.  "
                    + " Make sure it's of the format "
                    + "cifs://hostname/path?user=<username>&password=<password>";
            s_logger.warn(errMsg);
            return errMsg;
        }
        if (!UriUtils.cifsCredentialsPresent(uri))
        {
            String errMsg = "cifs URI missing user and password details. "
                    + "Add them as query parameters, e.g. "
                    + "cifs://example.com/some_share?user=foo&password=bar";
            s_logger.warn(errMsg);
            return errMsg;
        }
        return null;
    }

    public static boolean hostAndPathPresent(URI uri) {
        return !(uri.getHost() == null || uri.getHost().trim().isEmpty()
                || uri.getPath() == null || uri.getPath().trim().isEmpty());
    }

    public static boolean cifsCredentialsPresent(URI uri) {
        List<NameValuePair> args = URLEncodedUtils.parse(uri, "UTF-8");
        boolean foundUser = false;
        boolean foundPswd = false;
        for (NameValuePair nvp : args) {
            String name = nvp.getName();
            if (name.equals("user")) {
                foundUser = true;
                s_logger.debug("foundUser is" + foundUser);
            }
            else if (name.equals("password")) {
                foundPswd = true;
                s_logger.debug("foundPswd is" + foundPswd);
            }
        }
        return (foundUser && foundPswd);
    }
    // Get the size of a file from URL response header.
    public static Long getRemoteSize(String url) {
        Long remoteSize = (long) 0;
        HttpURLConnection httpConn = null;
        HttpsURLConnection httpsConn = null;
        try {
            URI uri = new URI(url);
            if (uri.getScheme().equalsIgnoreCase("http")) {
                httpConn = (HttpURLConnection) uri.toURL().openConnection();
                if (httpConn != null) {
                    String contentLength = httpConn.getHeaderField("content-length");
                    if (contentLength != null) {
                        remoteSize = Long.parseLong(contentLength);
                    }
                    httpConn.disconnect();
                }
            } else if (uri.getScheme().equalsIgnoreCase("https")) {
                httpsConn = (HttpsURLConnection) uri.toURL().openConnection();
                if (httpsConn != null) {
                    String contentLength = httpsConn.getHeaderField("content-length");
                    if (contentLength != null) {
                        remoteSize = Long.parseLong(contentLength);
                    }
                    httpsConn.disconnect();
                }
            }
        } catch (URISyntaxException e) {
            throw new IllegalArgumentException("Invalid URL " + url);
        } catch (IOException e) {
            throw new IllegalArgumentException("Unable to establish connection with URL " + url);
        }
        return remoteSize;
    }

    public static  Pair<String, Integer> validateUrl(String url) throws IllegalArgumentException {
        try {
            URI uri = new URI(url);
            if ((uri.getScheme() == null) || (!uri.getScheme().equalsIgnoreCase("http")
                    && !uri.getScheme().equalsIgnoreCase("https") && !uri.getScheme().equalsIgnoreCase("file"))) {
                    throw new IllegalArgumentException("Unsupported scheme for url: " + url);
            }
            int port = uri.getPort();
            if (!(port == 80 || port == 8080 || port == 443 || port == -1)) {
                throw new IllegalArgumentException("Only ports 80, 8080 and 443 are allowed");
            }

            if (port == -1 && uri.getScheme().equalsIgnoreCase("https")) {
                port = 443;
            } else if (port == -1 && uri.getScheme().equalsIgnoreCase("http")) {
                port = 80;
            }

            String host = uri.getHost();
            try {
                InetAddress hostAddr = InetAddress.getByName(host);
                if (hostAddr.isAnyLocalAddress() || hostAddr.isLinkLocalAddress() || hostAddr.isLoopbackAddress() || hostAddr.isMulticastAddress()) {
                    throw new IllegalArgumentException("Illegal host specified in url");
                }
                if (hostAddr instanceof Inet6Address) {
                    throw new IllegalArgumentException("IPV6 addresses not supported (" + hostAddr.getHostAddress() + ")");
                }
                return new Pair<String, Integer>(host, port);
            } catch (UnknownHostException uhe) {
                throw new IllegalArgumentException("Unable to resolve " + host);
            }
        } catch (URISyntaxException use) {
            throw new IllegalArgumentException("Invalid URL: " + url);
        }
    }

    public static InputStream getInputStreamFromUrl(String url, String user, String password) {

        try{
          Pair<String, Integer> hostAndPort = validateUrl(url);
          HttpClient httpclient = new HttpClient(new MultiThreadedHttpConnectionManager());
          if ((user != null) && (password != null)) {
              httpclient.getParams().setAuthenticationPreemptive(true);
              Credentials defaultcreds = new UsernamePasswordCredentials(user, password);
              httpclient.getState().setCredentials(new AuthScope(hostAndPort.first(), hostAndPort.second(), AuthScope.ANY_REALM), defaultcreds);
              s_logger.info("Added username=" + user + ", password=" + password + "for host " + hostAndPort.first() + ":" + hostAndPort.second());
          }
          // Execute the method.
          GetMethod method = new GetMethod(url);
          int statusCode = httpclient.executeMethod(method);

          if (statusCode != HttpStatus.SC_OK) {
            s_logger.error("Failed to read from URL: " + url);
            return null;
          }

          return method.getResponseBodyAsStream();
        }
        catch (Exception ex){
            s_logger.error("Failed to read from URL: " + url);
            return null;
        }
    }
}
