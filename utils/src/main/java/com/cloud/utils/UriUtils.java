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
import java.util.Collections;
import java.util.Comparator;
import java.util.HashMap;
import java.util.List;
import java.util.ListIterator;
import java.util.Map;
import java.util.Set;
import java.util.StringTokenizer;
import java.util.function.Predicate;

import javax.xml.parsers.DocumentBuilder;
import javax.xml.parsers.DocumentBuilderFactory;

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
import org.w3c.dom.Document;
import org.w3c.dom.Element;
import org.w3c.dom.NamedNodeMap;
import org.w3c.dom.Node;
import org.w3c.dom.NodeList;

import com.google.common.base.Strings;
import com.google.common.collect.ImmutableMap;
import com.google.common.collect.ImmutableSet;

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
    public static long getRemoteSize(String url) {
        long remoteSize = 0L;
        final String[] methods = new String[]{"HEAD", "GET"};
        IllegalArgumentException exception = null;
        // Attempting first a HEAD request to avoid downloading the whole file. If
        // it fails (for example with S3 presigned URL), fallback on a standard GET
        // request.
        for (String method : methods) {
            HttpURLConnection httpConn = null;
            try {
                URI uri = new URI(url);
                httpConn = (HttpURLConnection)uri.toURL().openConnection();
                httpConn.setRequestMethod(method);
                httpConn.setConnectTimeout(2000);
                httpConn.setReadTimeout(5000);
                String contentLength = httpConn.getHeaderField("Content-Length");
                if (contentLength != null) {
                    remoteSize = Long.parseLong(contentLength);
                } else if (method.equals("GET") && httpConn.getResponseCode() < 300) {
                    // Calculate the content size based on the input stream content
                    byte[] buf = new byte[1024];
                    int length;
                    while ((length = httpConn.getInputStream().read(buf, 0, buf.length)) != -1) {
                        remoteSize += length;
                    }
                }
                return remoteSize;
            } catch (URISyntaxException e) {
                throw new IllegalArgumentException("Invalid URL " + url);
            } catch (IOException e) {
                exception = new IllegalArgumentException("Unable to establish connection with URL " + url);
            } finally {
                if (httpConn != null) {
                    httpConn.disconnect();
                }
            }
        }
        if (exception != null) {
            throw exception;
        }
        return 0L;
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

    /**
     * Add element to priority list examining node attributes: priority (for urls) and type (for checksums)
     */
    protected static void addPriorityListElementExaminingNode(String tagName, Node node, List<Pair<String, Integer>> priorityList) {
        Integer priority = Integer.MAX_VALUE;
        String first = node.getTextContent();
        if (node.hasAttributes()) {
            NamedNodeMap attributes = node.getAttributes();
            for (int k=0; k<attributes.getLength(); k++) {
                Node attr = attributes.item(k);
                if (tagName.equals("url") && attr.getNodeName().equals("priority")) {
                    String prio = attr.getNodeValue().replace("\"", "");
                    priority = Integer.parseInt(prio);
                    break;
                } else if (tagName.equals("hash") && attr.getNodeName().equals("type")) {
                    first = "{" + attr.getNodeValue() + "}" + first;
                    break;
                }
            }
        }
        priorityList.add(new Pair<>(first, priority));
    }

    /**
     * Return the list of first elements on the list of pairs
     */
    protected static List<String> getListOfFirstElements(List<Pair<String, Integer>> priorityList) {
        List<String> values = new ArrayList<>();
        for (Pair<String, Integer> pair : priorityList) {
            values.add(pair.first());
        }
        return values;
    }

    /**
     * Return HttpClient with connection timeout
     */
    private static HttpClient getHttpClient() {
        MultiThreadedHttpConnectionManager s_httpClientManager = new MultiThreadedHttpConnectionManager();
        s_httpClientManager.getParams().setConnectionTimeout(5000);
        return new HttpClient(s_httpClientManager);
    }

    public static List<String> getMetalinkChecksums(String url) {
        HttpClient httpClient = getHttpClient();
        GetMethod getMethod = new GetMethod(url);
        try {
            if (httpClient.executeMethod(getMethod) == HttpStatus.SC_OK) {
                InputStream is = getMethod.getResponseBodyAsStream();
                Map<String, List<String>> checksums = getMultipleValuesFromXML(is, new String[] {"hash"});
                if (checksums.containsKey("hash")) {
                    List<String> listChksum = new ArrayList<>();
                    for (String chk : checksums.get("hash")) {
                        listChksum.add(chk.replaceAll("\n", "").replaceAll(" ", "").trim());
                    }
                    return listChksum;
                }
            }
        } catch (IOException e) {
            e.printStackTrace();
        } finally {
            getMethod.releaseConnection();
        }
        return null;
    }
    /**
     * Retrieve values from XML documents ordered by ascending priority for each tag name
     */
    protected static Map<String, List<String>> getMultipleValuesFromXML(InputStream is, String[] tagNames) {
        Map<String, List<String>> returnValues = new HashMap<String, List<String>>();
        try {
            DocumentBuilderFactory factory = DocumentBuilderFactory.newInstance();
            DocumentBuilder docBuilder = factory.newDocumentBuilder();
            Document doc = docBuilder.parse(is);
            Element rootElement = doc.getDocumentElement();
            for (int i = 0; i < tagNames.length; i++) {
                NodeList targetNodes = rootElement.getElementsByTagName(tagNames[i]);
                if (targetNodes.getLength() <= 0) {
                    s_logger.error("no " + tagNames[i] + " tag in XML response...");
                } else {
                    List<Pair<String, Integer>> priorityList = new ArrayList<>();
                    for (int j = 0; j < targetNodes.getLength(); j++) {
                        Node node = targetNodes.item(j);
                        addPriorityListElementExaminingNode(tagNames[i], node, priorityList);
                    }
                    priorityList.sort(Comparator.comparing(x -> x.second()));
                    returnValues.put(tagNames[i], getListOfFirstElements(priorityList));
                }
            }
        } catch (Exception ex) {
            s_logger.error(ex);
        }
        return returnValues;
    }

    /**
     * Check if there is at least one existent URL defined on metalink
     * @param url metalink url
     * @return true if at least one existent URL defined on metalink, false if not
     */
    protected static boolean checkUrlExistenceMetalink(String url) {
        HttpClient httpClient = getHttpClient();
        GetMethod getMethod = new GetMethod(url);
        try {
            if (httpClient.executeMethod(getMethod) == HttpStatus.SC_OK) {
                InputStream is = getMethod.getResponseBodyAsStream();
                Map<String, List<String>> metalinkUrls = getMultipleValuesFromXML(is, new String[] {"url"});
                if (metalinkUrls.containsKey("url")) {
                    List<String> urls = metalinkUrls.get("url");
                    boolean validUrl = false;
                    for (String u : urls) {
                        if (url.endsWith("torrent")) {
                            continue;
                        }
                        try {
                            UriUtils.checkUrlExistence(u);
                            validUrl = true;
                            break;
                        }
                        catch (IllegalArgumentException e) {
                            s_logger.warn(e.getMessage());
                        }
                    }
                    return validUrl;
                }
            }
        } catch (IOException e) {
            s_logger.warn(e.getMessage());
        } finally {
            getMethod.releaseConnection();
        }
        return false;
    }

    /**
     * Get list of urls on metalink ordered by ascending priority (for those which priority tag is not defined, highest priority value is assumed)
     */
    public static List<String> getMetalinkUrls(String metalinkUrl) {
        HttpClient httpClient = getHttpClient();
        GetMethod getMethod = new GetMethod(metalinkUrl);
        List<String> urls = new ArrayList<>();
        int status;
        try {
            status = httpClient.executeMethod(getMethod);
        } catch (IOException e) {
            s_logger.error("Error retrieving urls form metalink: " + metalinkUrl);
            getMethod.releaseConnection();
            return null;
        }
        try {
            InputStream is = getMethod.getResponseBodyAsStream();
            if (status == HttpStatus.SC_OK) {
                Map<String, List<String>> metalinkUrlsMap = getMultipleValuesFromXML(is, new String[] {"url"});
                if (metalinkUrlsMap.containsKey("url")) {
                    List<String> metalinkUrls = metalinkUrlsMap.get("url");
                    urls.addAll(metalinkUrls);
                }
            }
        } catch (IOException e) {
            s_logger.warn(e.getMessage());
        } finally {
            getMethod.releaseConnection();
        }
        return urls;
    }

    // use http HEAD method to validate url
    public static void checkUrlExistence(String url) {
        if (url.toLowerCase().startsWith("http") || url.toLowerCase().startsWith("https")) {
            HttpClient httpClient = getHttpClient();
            HeadMethod httphead = new HeadMethod(url);
            try {
                if (httpClient.executeMethod(httphead) != HttpStatus.SC_OK) {
                    throw new IllegalArgumentException("Invalid URL: " + url);
                }
                if (url.endsWith("metalink") && !checkUrlExistenceMetalink(url)) {
                    throw new IllegalArgumentException("Invalid URLs defined on metalink: " + url);
                }
            } catch (HttpException hte) {
                throw new IllegalArgumentException("Cannot reach URL: " + url + " due to: " + hte.getMessage());
            } catch (IOException ioe) {
                throw new IllegalArgumentException("Cannot reach URL: " + url + " due to: " + ioe.getMessage());
            } finally {
                httphead.releaseConnection();
            }
        }
    }

    public static final Set<String> COMMPRESSION_FORMATS = ImmutableSet.of("zip", "bz2", "gz");

    public static final Set<String> buildExtensionSet(boolean metalink, String... baseExtensions) {
        final ImmutableSet.Builder<String> builder = ImmutableSet.builder();

        for (String baseExtension : baseExtensions) {
            builder.add("." + baseExtension);
            for (String format : COMMPRESSION_FORMATS) {
                builder.add("." + baseExtension + "." + format);
            }
        }

        if (metalink) {
            builder.add(".metalink");
        }

        return builder.build();
    }

    private final static Map<String, Set<String>> SUPPORTED_EXTENSIONS_BY_FORMAT =
            ImmutableMap.<String, Set<String>>builder()
                        .put("vhd", buildExtensionSet(false, "vhd"))
                        .put("vhdx", buildExtensionSet(false, "vhdx"))
                        .put("qcow2", buildExtensionSet(true, "qcow2"))
                        .put("ova", buildExtensionSet(true, "ova"))
                        .put("tar", buildExtensionSet(false, "tar"))
                        .put("raw", buildExtensionSet(false, "img", "raw"))
                        .put("vmdk", buildExtensionSet(false, "vmdk"))
                        .put("iso", buildExtensionSet(true, "iso"))
            .build();

    public final static Set<String> getSupportedExtensions(String format) {
        return SUPPORTED_EXTENSIONS_BY_FORMAT.get(format);
    }

    // verify if a URI path is compliance with the file format given
    private static void checkFormat(String format, String uripath) {
        final String lowerCaseUri = uripath.toLowerCase();

        final boolean unknownExtensionForFormat = SUPPORTED_EXTENSIONS_BY_FORMAT.get(format.toLowerCase())
                                                                                .stream()
                                                                                .noneMatch(lowerCaseUri::endsWith);

        if (unknownExtensionForFormat) {
            final Predicate<Set<String>> uriMatchesAnyExtension =
                    supportedExtensions -> supportedExtensions.stream()
                                                              .anyMatch(lowerCaseUri::endsWith);

            boolean unknownExtension = SUPPORTED_EXTENSIONS_BY_FORMAT.values()
                                                                     .stream()
                                                                     .noneMatch(uriMatchesAnyExtension);

            if (unknownExtension) {
                throw new IllegalArgumentException("Please specify a valid " + format.toLowerCase());
            }

            throw new IllegalArgumentException("Please specify a valid URL. "
                                                       + "URL:" + uripath + " is an invalid for the format " + format.toLowerCase());
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

    /**
     * Expands a given vlan URI to a list of vlan IDs
     * @param vlanAuthority the URI part without the vlan:// scheme
     * @return returns list of vlan integer ids
     */
    public static List<Integer> expandVlanUri(final String vlanAuthority) {
        final List<Integer> expandedVlans = new ArrayList<>();
        if (Strings.isNullOrEmpty(vlanAuthority)) {
            return expandedVlans;
        }
        for (final String vlanPart: vlanAuthority.split(",")) {
            if (Strings.isNullOrEmpty(vlanPart)) {
                continue;
            }
            final String[] range = vlanPart.split("-");
            if (range.length == 2) {
                Integer start = NumbersUtil.parseInt(range[0], -1);
                Integer end = NumbersUtil.parseInt(range[1], -1);
                if (start <= end && end > -1 && start > -1) {
                    while (start <= end) {
                        expandedVlans.add(start++);
                    }
                }
            } else {
                final Integer value = NumbersUtil.parseInt(range[0], -1);
                if (value > -1) {
                    expandedVlans.add(value);
                }
            }
        }
        return expandedVlans;
    }

    /**
     * Checks if given vlan URI authorities overlap
     * @param vlanRange1
     * @param vlanRange2
     * @return true if they overlap
     */
    public static boolean checkVlanUriOverlap(final String vlanRange1, final String vlanRange2) {
        final List<Integer> vlans1 = expandVlanUri(vlanRange1);
        final List<Integer> vlans2 = expandVlanUri(vlanRange2);
        if (vlans1 == null || vlans2 == null) {
            return true;
        }
        return !Collections.disjoint(vlans1, vlans2);
    }
}
