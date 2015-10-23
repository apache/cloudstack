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
import java.util.ArrayList;
import java.util.List;
import java.util.ListIterator;
import java.util.StringTokenizer;

import javax.net.ssl.HttpsURLConnection;

import org.apache.commons.httpclient.Credentials;
import org.apache.commons.httpclient.HttpClient;
import org.apache.commons.httpclient.HttpException;
import org.apache.commons.httpclient.HttpStatus;
import org.apache.commons.httpclient.MultiThreadedHttpConnectionManager;
import org.apache.commons.httpclient.UsernamePasswordCredentials;
import org.apache.commons.httpclient.auth.AuthScope;
import org.apache.commons.httpclient.methods.GetMethod;
import org.apache.commons.httpclient.methods.HeadMethod;
import org.apache.commons.httpclient.util.URIUtil;
import org.apache.http.NameValuePair;
import org.apache.http.client.utils.URIBuilder;
import org.apache.http.client.utils.URLEncodedUtils;

import org.apache.http.message.BasicNameValuePair;
import org.apache.log4j.Logger;

import com.cloud.utils.crypt.DBEncryptionUtil;
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
        if (schemeTail > 0)
            pathStart = url.indexOf('/', schemeTail + 3);
        else
            pathStart = url.indexOf('/');

        if (pathStart > 0) {
            String[] tokens = url.substring(pathStart + 1).split("/");
            StringBuilder sb = new StringBuilder(url.substring(0, pathStart));
            for (String token : tokens) {
                sb.append("/").append(URLEncoder.encode(token));
            }

            return sb.toString();
        }

        // no need to do URL component encoding
        return url;
    }

    public static String getCifsUriParametersProblems(URI uri) {
        if (!UriUtils.hostAndPathPresent(uri)) {
            String errMsg = "cifs URI missing host and/or path. Make sure it's of the format cifs://hostname/path";
            s_logger.warn(errMsg);
            return errMsg;
        }
        return null;
    }

    public static boolean hostAndPathPresent(URI uri) {
        return !(uri.getHost() == null || uri.getHost().trim().isEmpty() || uri.getPath() == null || uri.getPath().trim().isEmpty());
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
            } else if (name.equals("password")) {
                foundPswd = true;
                s_logger.debug("foundPswd is" + foundPswd);
            }
        }
        return (foundUser && foundPswd);
    }

    public static String getUpdateUri(String url, boolean encrypt) {
        String updatedPath = null;
        try {
            String query = URIUtil.getQuery(url);
            URIBuilder builder = new URIBuilder(url);
            builder.removeQuery();

            StringBuilder updatedQuery = new StringBuilder();
            List<NameValuePair> queryParams = getUserDetails(query);
            ListIterator<NameValuePair> iterator = queryParams.listIterator();
            while (iterator.hasNext()) {
                NameValuePair param = iterator.next();
                String value = null;
                if ("password".equalsIgnoreCase(param.getName()) &&
                        param.getValue() != null) {
                    value = encrypt ? DBEncryptionUtil.encrypt(param.getValue()) : DBEncryptionUtil.decrypt(param.getValue());
                } else {
                    value = param.getValue();
                }

                if (updatedQuery.length() == 0) {
                    updatedQuery.append(param.getName()).append('=')
                            .append(value);
                } else {
                    updatedQuery.append('&').append(param.getName())
                            .append('=').append(value);
                }
            }

            String schemeAndHost = "";
            URI newUri = builder.build();
            if (newUri.getScheme() != null) {
                schemeAndHost = newUri.getScheme() + "://" + newUri.getHost();
            }

            updatedPath = schemeAndHost + newUri.getPath() + "?" + updatedQuery;
        } catch (URISyntaxException e) {
            throw new CloudRuntimeException("Couldn't generate an updated uri. " + e.getMessage());
        }

        return updatedPath;
    }

    private static List<NameValuePair> getUserDetails(String query) {
        List<NameValuePair> details = new ArrayList<NameValuePair>();
        if (query != null && !query.isEmpty()) {
            StringTokenizer allParams = new StringTokenizer(query, "&");
            while (allParams.hasMoreTokens()) {
                String param = allParams.nextToken();
                details.add(new BasicNameValuePair(param.substring(0, param.indexOf("=")),
                        param.substring(param.indexOf("=") + 1)));
            }
        }

        return details;
    }

    // Get the size of a file from URL response header.
    public static Long getRemoteSize(String url) {
        Long remoteSize = (long)0;
        HttpURLConnection httpConn = null;
        HttpsURLConnection httpsConn = null;
        try {
            URI uri = new URI(url);
            if (uri.getScheme().equalsIgnoreCase("http")) {
                httpConn = (HttpURLConnection)uri.toURL().openConnection();
                if (httpConn != null) {
                    httpConn.setConnectTimeout(2000);
                    httpConn.setReadTimeout(5000);
                    String contentLength = httpConn.getHeaderField("content-length");
                    if (contentLength != null) {
                        remoteSize = Long.parseLong(contentLength);
                    }
                    httpConn.disconnect();
                }
            } else if (uri.getScheme().equalsIgnoreCase("https")) {
                httpsConn = (HttpsURLConnection)uri.toURL().openConnection();
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

    public static Pair<String, Integer> validateUrl(String url) throws IllegalArgumentException {
        return validateUrl(null, url);
    }

    public static Pair<String, Integer> validateUrl(String format, String url) throws IllegalArgumentException {
        try {
            URI uri = new URI(url);
            if ((uri.getScheme() == null) ||
                    (!uri.getScheme().equalsIgnoreCase("http") && !uri.getScheme().equalsIgnoreCase("https") && !uri.getScheme().equalsIgnoreCase("file"))) {
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
            } catch (UnknownHostException uhe) {
                throw new IllegalArgumentException("Unable to resolve " + host);
            }

            // verify format
            if (format != null) {
                String uripath = uri.getPath();
                checkFormat(format, uripath);
            }
            return new Pair<String, Integer>(host, port);
        } catch (URISyntaxException use) {
            throw new IllegalArgumentException("Invalid URL: " + url);
        }
    }

    // use http HEAD method to validate url
    public static void checkUrlExistence(String url) {
        if (url.toLowerCase().startsWith("http") || url.toLowerCase().startsWith("https")) {
            HttpClient httpClient = new HttpClient(new MultiThreadedHttpConnectionManager());
            HeadMethod httphead = new HeadMethod(url);
            try {
                if (httpClient.executeMethod(httphead) != HttpStatus.SC_OK) {
                    throw new IllegalArgumentException("Invalid URL: " + url);
                }
            } catch (HttpException hte) {
                throw new IllegalArgumentException("Cannot reach URL: " + url);
            } catch (IOException ioe) {
                throw new IllegalArgumentException("Cannot reach URL: " + url);
            }
        }
    }

    // verify if a URI path is compliance with the file format given
    private static void checkFormat(String format, String uripath) {
        if ((!uripath.toLowerCase().endsWith("vhd")) && (!uripath.toLowerCase().endsWith("vhd.zip")) && (!uripath.toLowerCase().endsWith("vhd.bz2")) &&
                (!uripath.toLowerCase().endsWith("vhdx")) && (!uripath.toLowerCase().endsWith("vhdx.gz")) &&
                (!uripath.toLowerCase().endsWith("vhdx.bz2")) && (!uripath.toLowerCase().endsWith("vhdx.zip")) &&
                (!uripath.toLowerCase().endsWith("vhd.gz")) && (!uripath.toLowerCase().endsWith("qcow2")) && (!uripath.toLowerCase().endsWith("qcow2.zip")) &&
                (!uripath.toLowerCase().endsWith("qcow2.bz2")) && (!uripath.toLowerCase().endsWith("qcow2.gz")) && (!uripath.toLowerCase().endsWith("ova")) &&
                (!uripath.toLowerCase().endsWith("ova.zip")) && (!uripath.toLowerCase().endsWith("ova.bz2")) && (!uripath.toLowerCase().endsWith("ova.gz")) &&
                (!uripath.toLowerCase().endsWith("tar")) && (!uripath.toLowerCase().endsWith("tar.zip")) && (!uripath.toLowerCase().endsWith("tar.bz2")) &&
                (!uripath.toLowerCase().endsWith("tar.gz")) && (!uripath.toLowerCase().endsWith("vmdk")) && (!uripath.toLowerCase().endsWith("vmdk.gz")) &&
                (!uripath.toLowerCase().endsWith("vmdk.zip")) && (!uripath.toLowerCase().endsWith("vmdk.bz2")) && (!uripath.toLowerCase().endsWith("img")) &&
                (!uripath.toLowerCase().endsWith("img.gz")) && (!uripath.toLowerCase().endsWith("img.zip")) && (!uripath.toLowerCase().endsWith("img.bz2")) &&
                (!uripath.toLowerCase().endsWith("raw")) && (!uripath.toLowerCase().endsWith("raw.gz")) && (!uripath.toLowerCase().endsWith("raw.bz2")) &&
                (!uripath.toLowerCase().endsWith("raw.zip")) && (!uripath.toLowerCase().endsWith("iso")) && (!uripath.toLowerCase().endsWith("iso.zip"))
                && (!uripath.toLowerCase().endsWith("iso.bz2")) && (!uripath.toLowerCase().endsWith("iso.gz"))) {
            throw new IllegalArgumentException("Please specify a valid " + format.toLowerCase());
        }

        if ((format.equalsIgnoreCase("vhd")
                && (!uripath.toLowerCase().endsWith("vhd")
                && !uripath.toLowerCase().endsWith("vhd.zip")
                && !uripath.toLowerCase().endsWith("vhd.bz2")
                && !uripath.toLowerCase().endsWith("vhd.gz")))
                || (format.equalsIgnoreCase("vhdx")
                && (!uripath.toLowerCase().endsWith("vhdx")
                        && !uripath.toLowerCase().endsWith("vhdx.zip")
                        && !uripath.toLowerCase().endsWith("vhdx.bz2")
                        && !uripath.toLowerCase().endsWith("vhdx.gz")))
                || (format.equalsIgnoreCase("qcow2")
                && (!uripath.toLowerCase().endsWith("qcow2")
                        && !uripath.toLowerCase().endsWith("qcow2.zip")
                        && !uripath.toLowerCase().endsWith("qcow2.bz2")
                        && !uripath.toLowerCase().endsWith("qcow2.gz")))
                || (format.equalsIgnoreCase("ova")
                && (!uripath.toLowerCase().endsWith("ova")
                        && !uripath.toLowerCase().endsWith("ova.zip")
                        && !uripath.toLowerCase().endsWith("ova.bz2")
                        && !uripath.toLowerCase().endsWith("ova.gz")))
                || (format.equalsIgnoreCase("tar")
                && (!uripath.toLowerCase().endsWith("tar")
                        && !uripath.toLowerCase().endsWith("tar.zip")
                        && !uripath.toLowerCase().endsWith("tar.bz2")
                        && !uripath.toLowerCase().endsWith("tar.gz")))
                || (format.equalsIgnoreCase("raw")
                && (!uripath.toLowerCase().endsWith("img")
                        && !uripath.toLowerCase().endsWith("img.zip")
                        && !uripath.toLowerCase().endsWith("img.bz2")
                        && !uripath.toLowerCase().endsWith("img.gz")
                        && !uripath.toLowerCase().endsWith("raw")
                        && !uripath.toLowerCase().endsWith("raw.bz2")
                        && !uripath.toLowerCase().endsWith("raw.zip")
                        && !uripath.toLowerCase().endsWith("raw.gz")))
                || (format.equalsIgnoreCase("vmdk")
                && (!uripath.toLowerCase().endsWith("vmdk")
                        && !uripath.toLowerCase().endsWith("vmdk.zip")
                        && !uripath.toLowerCase().endsWith("vmdk.bz2")
                        && !uripath.toLowerCase().endsWith("vmdk.gz")))
                || (format.equalsIgnoreCase("iso")
                && (!uripath.toLowerCase().endsWith("iso")
                        && !uripath.toLowerCase().endsWith("iso.zip")
                        && !uripath.toLowerCase().endsWith("iso.bz2")
                        && !uripath.toLowerCase().endsWith("iso.gz")))) {
            throw new IllegalArgumentException("Please specify a valid URL. URL:" + uripath + " is an invalid for the format " + format.toLowerCase());
        }

    }

    public static InputStream getInputStreamFromUrl(String url, String user, String password) {

        try {
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
        } catch (Exception ex) {
            s_logger.error("Failed to read from URL: " + url);
            return null;
        }
    }
}
