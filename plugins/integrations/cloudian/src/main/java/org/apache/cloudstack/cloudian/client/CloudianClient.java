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

package org.apache.cloudstack.cloudian.client;

import java.io.IOException;
import java.io.InputStream;
import java.io.PushbackInputStream;
import java.net.SocketTimeoutException;
import java.security.KeyManagementException;
import java.security.KeyStoreException;
import java.security.NoSuchAlgorithmException;
import java.security.SecureRandom;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

import javax.net.ssl.SSLContext;
import javax.net.ssl.SSLException;
import javax.net.ssl.X509TrustManager;

import org.apache.cloudstack.api.ApiErrorCode;
import org.apache.cloudstack.api.ServerApiException;
import org.apache.cloudstack.utils.security.SSLUtils;
import org.apache.http.HttpEntity;
import org.apache.http.HttpHost;
import org.apache.http.HttpResponse;
import org.apache.http.HttpStatus;
import org.apache.http.auth.AuthScope;
import org.apache.http.auth.Credentials;
import org.apache.http.auth.UsernamePasswordCredentials;
import org.apache.http.client.AuthCache;
import org.apache.http.client.CredentialsProvider;
import org.apache.http.client.HttpClient;
import org.apache.http.client.config.RequestConfig;
import org.apache.http.client.methods.HttpDelete;
import org.apache.http.client.methods.HttpGet;
import org.apache.http.client.methods.HttpPost;
import org.apache.http.client.methods.HttpPut;
import org.apache.http.client.protocol.HttpClientContext;
import org.apache.http.conn.ConnectTimeoutException;
import org.apache.http.conn.ssl.NoopHostnameVerifier;
import org.apache.http.conn.ssl.SSLConnectionSocketFactory;
import org.apache.http.entity.StringEntity;
import org.apache.http.impl.auth.BasicScheme;
import org.apache.http.impl.client.BasicAuthCache;
import org.apache.http.impl.client.BasicCredentialsProvider;
import org.apache.http.impl.client.HttpClientBuilder;
import org.apache.http.util.EntityUtils;
import org.apache.logging.log4j.Logger;
import org.apache.logging.log4j.LogManager;

import com.cloud.utils.nio.TrustAllManager;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.apache.commons.lang3.StringUtils;

public class CloudianClient {
    protected Logger logger = LogManager.getLogger(getClass());

    private final HttpClient httpClient;
    private final HttpClientContext httpContext;
    private final String adminApiUrl;

    public CloudianClient(final String host, final Integer port, final String scheme, final String username, final String password, final boolean validateSSlCertificate, final int timeout) throws KeyStoreException, NoSuchAlgorithmException, KeyManagementException {
        final CredentialsProvider provider = new BasicCredentialsProvider();
        provider.setCredentials(AuthScope.ANY, new UsernamePasswordCredentials(username, password));
        final HttpHost adminHost = new HttpHost(host, port, scheme);
        final AuthCache authCache = new BasicAuthCache();
        authCache.put(adminHost, new BasicScheme());

        this.adminApiUrl = adminHost.toURI();
        this.httpContext = HttpClientContext.create();
        this.httpContext.setCredentialsProvider(provider);
        this.httpContext.setAuthCache(authCache);

        final RequestConfig config = RequestConfig.custom()
                .setConnectTimeout(timeout * 1000)
                .setConnectionRequestTimeout(timeout * 1000)
                .setSocketTimeout(timeout * 1000)
                .build();

        if (!validateSSlCertificate && "https".equalsIgnoreCase(scheme)) {
            final SSLContext sslcontext = SSLUtils.getSSLContext();
            sslcontext.init(null, new X509TrustManager[]{new TrustAllManager()}, new SecureRandom());
            final SSLConnectionSocketFactory factory = new SSLConnectionSocketFactory(sslcontext, NoopHostnameVerifier.INSTANCE);
            this.httpClient = HttpClientBuilder.create()
                    .setDefaultCredentialsProvider(provider)
                    .setDefaultRequestConfig(config)
                    .setSSLSocketFactory(factory)
                    .build();
        } else {
            this.httpClient = HttpClientBuilder.create()
                    .setDefaultCredentialsProvider(provider)
                    .setDefaultRequestConfig(config)
                    .build();
        }
    }

    private void checkAuthFailure(final HttpResponse response) {
        if (response != null && response.getStatusLine().getStatusCode() == HttpStatus.SC_UNAUTHORIZED) {
            final Credentials credentials = httpContext.getCredentialsProvider().getCredentials(AuthScope.ANY);
            // Don't dump the actual password in the log, but its useful to know the length perhaps.
            final String asteriskPassword = "*".repeat(credentials.getPassword().length());
            logger.error("Cloudian admin API authentication failed, please check Cloudian configuration. Admin auth principal=" + credentials.getUserPrincipal() + ", password=" + asteriskPassword + ", API url=" + adminApiUrl);
            throw new ServerApiException(ApiErrorCode.UNAUTHORIZED, "Cloudian backend API call unauthorized, please ask your administrator to fix integration issues.");
        }
    }

    private void checkResponseOK(final HttpResponse response) {
        if (response.getStatusLine().getStatusCode() == HttpStatus.SC_NO_CONTENT) {
            logger.debug("Requested Cloudian resource does not exist");
            return;
        }
        if (response.getStatusLine().getStatusCode() != HttpStatus.SC_OK && response.getStatusLine().getStatusCode() != HttpStatus.SC_NO_CONTENT) {
            throw new ServerApiException(ApiErrorCode.INTERNAL_ERROR, "Failed to find the requested resource and get valid response from Cloudian backend API call, please ask your administrator to diagnose and fix issues.");
        }
    }

    /**
     * Return true if the response does not have an entity.
     * This is not the same thing as an empty body which is different and not detected here.
     * The 200 response for example should always return false even if it has no body bytes.
     * @param response the response to check
     * @return true if status code was 204 or the response does not have an entity. False otherwise.
     */
    private boolean noResponseEntity(final HttpResponse response) {
        return response != null && (response.getStatusLine().getStatusCode() == HttpStatus.SC_NO_CONTENT || response.getEntity() == null);
    }

    /**
     * Throw a specific exception for timeout or a more generic server error.
     * This method does not return to the caller and instead always throws an exception.
     * @param e IOException (including ClientProtocolException) as thrown by httpClient.execute()
     * @throws ServerApiException is always thrown
     */
    private void throwTimeoutOrServerException(final IOException e) {
        if (e instanceof ConnectTimeoutException || e instanceof SocketTimeoutException) {
            throw new ServerApiException(ApiErrorCode.RESOURCE_UNAVAILABLE_ERROR, "Operation timed out, please try again.");
        } else if (e instanceof SSLException) {
            throw new ServerApiException(ApiErrorCode.INTERNAL_ERROR, "SSL Error Connecting to Cloudian Admin Service", e);
        } else {
            throw new ServerApiException(ApiErrorCode.INTERNAL_ERROR, "internal error", e);
        }
    }

    /**
     * Return the body content stream only if the body has bytes.
     *
     * Unfortunately, some of the responses such as listGroups() or listUsers() return
     * an empty body instead of returning and empty list. The only way to detect this is
     * to try read from the body. This method handles this and will return null if the
     * body was empty or a valid stream with the body content otherwise.
     *
     * @param response the response to check for the body contents.
     * @return a valid InputStream or null if the body was empty.
     *
     * @throws IOException some error reading from the body such as timeout etc.
     */
    protected InputStream getNonEmptyContentStream(HttpResponse response) throws IOException {
        PushbackInputStream iStream = new PushbackInputStream(response.getEntity().getContent());
        int firstByte=iStream.read();
        if (firstByte == -1) {
            return null;
        } else {
            iStream.unread(firstByte);
            return iStream;
        }
    }

    private HttpResponse delete(final String path) throws IOException {
        final HttpResponse response = httpClient.execute(new HttpDelete(adminApiUrl + path), httpContext);
        checkAuthFailure(response);
        return response;
    }

    private HttpResponse get(final String path) throws IOException {
        final HttpResponse response = httpClient.execute(new HttpGet(adminApiUrl + path), httpContext);
        checkAuthFailure(response);
        return response;
    }

    private HttpResponse post(final String path, final Object item) throws IOException {
        final ObjectMapper mapper = new ObjectMapper();
        final String json = mapper.writeValueAsString(item);
        final StringEntity entity = new StringEntity(json);
        final HttpPost request = new HttpPost(adminApiUrl + path);
        request.setHeader("content-type", "application/json");
        request.setEntity(entity);
        final HttpResponse response = httpClient.execute(request, httpContext);
        checkAuthFailure(response);
        return response;
    }

    /**
     * Perform a HTTP PUT operation using the path and optional JSON body item.
     * @param path the http path to use
     * @param item optional object to send in the body payload. Set to null if no body.
     * @return the HttpResponse object
     * @throws IOException if the request cannot be executed completely.
     * @throws ServerApiException if the request meets 401 unauthorized.
     */
    private HttpResponse put(final String path, final Object item) throws IOException {
        final HttpPut request = new HttpPut(adminApiUrl + path);
        if (item != null) {
            final ObjectMapper mapper = new ObjectMapper();
            final String json = mapper.writeValueAsString(item);
            final StringEntity entity = new StringEntity(json);
            request.setHeader("content-type", "application/json");
            request.setEntity(entity);
        }
        final HttpResponse response = httpClient.execute(request, httpContext);
        checkAuthFailure(response);
        return response;
    }

    ////////////////////////////////////////////////////////
    //////////////// Public APIs: Misc /////////////////////
    ////////////////////////////////////////////////////////

    /**
     * Get the HyperStore Server Version number.
     *
     * @return version number
     * @throws ServerApiException on non-200 response or timeout
     */
    public String getServerVersion() {
        logger.debug("Getting server version");
        try {
            final HttpResponse response = get("/system/version");
            checkResponseOK(response);
            HttpEntity entity = response.getEntity();
            return EntityUtils.toString(entity, "UTF-8");
        } catch (final IOException e) {
            logger.error("Failed to get HyperStore system version:", e);
            throwTimeoutOrServerException(e);
        }
        return null;
    }

    /**
     * Get bucket usage information for a group, a user or a particular bucket.
     *
     * Note: Bucket Usage Statistics in HyperStore are disabled by default. They
     * can be enabled by the HyperStore Administrator by setting of the configuration
     * 's3.qos.bucketLevel=true'.
     *
     * @param groupId the groupId is required (and must exist)
     * @param userId the userId is optional (null) and if not set all group users are returned.
     * @param bucket the bucket is optional (null). If set, the userId must also be set.
     * @return a list of bucket usages (possibly empty).
     * @throws ServerApiException on non-200 response such as unknown groupId etc or response issue.
     */
    public List<CloudianUserBucketUsage> getUserBucketUsages(final String groupId, final String userId, final String bucket) {
        if (StringUtils.isBlank(groupId) || (StringUtils.isBlank(userId) && !StringUtils.isBlank(bucket))) {
            String msg = String.format("Bad parameters groupId=%s userId=%s bucket=%s", groupId, userId, bucket);
            logger.error(msg);
            throw new ServerApiException(ApiErrorCode.PARAM_ERROR, msg);
        }

        logger.debug("Getting bucket usages for groupId={} userId={} bucket={}", groupId, userId, bucket);
        StringBuilder cmd = new StringBuilder("/system/bucketusage?groupId=");
        cmd.append(groupId);
        if (! StringUtils.isBlank(userId)) {
            cmd.append("&userId=");
            cmd.append(userId);
        }
        if (! StringUtils.isBlank(bucket)) {
            cmd.append("&bucket=");
            cmd.append(bucket);
        }

        try {
            final HttpResponse response = get(cmd.toString());
            checkResponseOK(response);
            if (noResponseEntity(response)) {
                throw new ServerApiException(ApiErrorCode.INTERNAL_ERROR, "API error");
            }
            // If the groupId exists, this request always returns a proper (possibly empty) list
            final ObjectMapper mapper = new ObjectMapper();
            return Arrays.asList(mapper.readValue(response.getEntity().getContent(), CloudianUserBucketUsage[].class));
        } catch (final IOException e) {
            logger.error("Failed to get bucket usage stats due to:", e);
            throwTimeoutOrServerException(e);
            return new ArrayList<>(); // never reached
        }
    }

    ////////////////////////////////////////////////////////
    //////////////// Public APIs: User /////////////////////
    ////////////////////////////////////////////////////////

    /**
     * Create a new HyperStore user.
     * @param user the User object to create.
     * @return true if the user was successfully created, false if it exists or there was other non-200 (except 401) response.
     * @throws ServerApiException if there was any other issue such as 401 unauthorized or network error.
     */
    public boolean addUser(final CloudianUser user) {
        if (user == null) {
            return false;
        }
        logger.debug("Adding Cloudian user: " + user);
        try {
            final HttpResponse response = put("/user", user);
            return response.getStatusLine().getStatusCode() == HttpStatus.SC_OK;
        } catch (final IOException e) {
            logger.error("Failed to add Cloudian user due to:", e);
            throwTimeoutOrServerException(e);
        }
        return false;
    }

    /**
     * Get a HyperStore user.
     * @param userId the userId
     * @param groupId the groupId the user belongs to
     * @return CloudianUser if found, null if not found.
     * @throws ServerApiException if the is any problem.
     */
    public CloudianUser listUser(final String userId, final String groupId) {
        if (StringUtils.isAnyEmpty(userId, groupId)) {
            return null;
        }
        logger.debug("Trying to find Cloudian user with id=" + userId + " and group id=" + groupId);
        try {
            final HttpResponse response = get(String.format("/user?userId=%s&groupId=%s", userId, groupId));
            checkResponseOK(response);
            if (noResponseEntity(response)) {
                return null; // User not found
            }
            final ObjectMapper mapper = new ObjectMapper();
            return mapper.readValue(response.getEntity().getContent(), CloudianUser.class);
        } catch (final IOException e) {
            logger.error("Failed to list Cloudian user due to:", e);
            throwTimeoutOrServerException(e);
            return null; // never reached
        }
    }

    /**
     * Return a list of all active HyperStore users in a group.
     * @param groupId the target group to list
     * @return a possibly empty list of CloudianUser objects.
     * @throws ServerApiException if there is any problem or non-200 response.
     */
    public List<CloudianUser> listUsers(final String groupId) {
        if (StringUtils.isEmpty(groupId)) {
            return new ArrayList<>();
        }
        logger.debug("Trying to list Cloudian users in group id=" + groupId);
        try {
            final HttpResponse response = get(String.format("/user/list?groupId=%s&userType=all&userStatus=active", groupId));
            checkResponseOK(response);
            if (noResponseEntity(response)) {
                throw new ServerApiException(ApiErrorCode.INTERNAL_ERROR, "API error");
            }
            InputStream iStream = getNonEmptyContentStream(response);
            if (iStream == null) {
                return new ArrayList<>(); // empty body => empty list
            }
            final ObjectMapper mapper = new ObjectMapper();
            return Arrays.asList(mapper.readValue(iStream, CloudianUser[].class));
        } catch (final IOException e) {
            logger.error("Failed to list Cloudian users due to:", e);
            throwTimeoutOrServerException(e);
            return new ArrayList<>(); // never reached
        }
    }

    public boolean updateUser(final CloudianUser user) {
        if (user == null) {
            return false;
        }
        logger.debug("Updating Cloudian user: " + user);
        try {
            final HttpResponse response = post("/user", user);
            return response.getStatusLine().getStatusCode() == HttpStatus.SC_OK;
        } catch (final IOException e) {
            logger.error("Failed to update Cloudian user due to:", e);
            throwTimeoutOrServerException(e);
        }
        return false;
    }

    public boolean removeUser(final String userId, final String groupId) {
        if (StringUtils.isAnyEmpty(userId, groupId)) {
            return false;
        }
        logger.debug("Removing Cloudian user with user id=" + userId + " in group id=" + groupId);
        try {
            final HttpResponse response = delete(String.format("/user?userId=%s&groupId=%s", userId, groupId));
            return response.getStatusLine().getStatusCode() == HttpStatus.SC_OK;
        } catch (final IOException e) {
            logger.error("Failed to remove Cloudian user due to:", e);
            throwTimeoutOrServerException(e);
        }
        return false;
    }

    /**
     * Create a new HyperStore Root credential.
     * @param userId the userId
     * @param groupId the groupId
     * @return the new Credential (should never be null)
     * @throws ServerApiException if the request fails or bad parameters given
     */
    public CloudianCredential createCredential(final String userId, final String groupId) {
        if (StringUtils.isAnyBlank(userId, groupId)) {
            throw new ServerApiException(ApiErrorCode.INTERNAL_ERROR, "API error. Missing user or group");
        }
        logger.debug("Creating new credentials for user id={} group id={} ", userId, groupId);
        try {
            String cmd = String.format("/user/credentials?userId=%s&groupId=%s", userId, groupId);
            final HttpResponse response = put(cmd, null);
            if (response.getStatusLine().getStatusCode() == HttpStatus.SC_FORBIDDEN) {
                String msg = String.format("Maximum credentials reached for user id=%s group id=%s. Consult your HyperStore Administrator", userId, groupId);
                logger.error(msg);
                throw new ServerApiException(ApiErrorCode.ACCOUNT_RESOURCE_LIMIT_ERROR, msg);
            }
            checkResponseOK(response);
            final ObjectMapper mapper = new ObjectMapper();
            return mapper.readValue(response.getEntity().getContent(), CloudianCredential.class);
        } catch (final IOException e) {
            logger.error("Failed to create credential due to:", e);
            throwTimeoutOrServerException(e);
            return null; // never reached
        }
    }

    /**
     * Get a list of Root credentials for the given user.
     * @param userId Cloudian userId
     * @param groupId Cloudian groupId
     * @return a potentially empty list of Root CloudianCredentials
     * @throws ServerApiException on non-2xx response or timeout
     */
    public List<CloudianCredential> listCredentials(final String userId, final String groupId) {
        return listCredentials(userId, groupId, true);
    }

    /**
     * Get a list of credentials for the given user.
     * @param userId Cloudian userId
     * @param groupId Cloudian groupId
     * @param rootOnly true only returns root credentials, false returns IAM credentials also.
     * @return a potentially empty list of CloudianCredentials
     * @throws ServerApiException on non-2xx response or timeout
     */
    public List<CloudianCredential> listCredentials(final String userId, final String groupId, final boolean rootOnly) {
        if (StringUtils.isAnyBlank(userId, groupId)) {
            return new ArrayList<>();
        }
        logger.debug("Listing credentials for Cloudian user id={} group id={}", userId, groupId);
        try {
            String cmd = String.format("/user/credentials/list?userId=%s&groupId=%s&isRootAccountOnly=%b", userId, groupId, rootOnly);
            final HttpResponse response = get(cmd);
            checkResponseOK(response);
            if (noResponseEntity(response)) {
                return new ArrayList<>(); // No credentials to be listed case -> 204
            }
            final ObjectMapper mapper = new ObjectMapper();
            return Arrays.asList(mapper.readValue(response.getEntity().getContent(), CloudianCredential[].class));
        } catch (final IOException e) {
            logger.error("Failed to list credentials due to:", e);
            throwTimeoutOrServerException(e);
            return new ArrayList<>();  // never reached
        }
    }

    /////////////////////////////////////////////////////////
    //////////////// Public APIs: Group /////////////////////
    /////////////////////////////////////////////////////////

    public boolean addGroup(final CloudianGroup group) {
        if (group == null) {
            return false;
        }
        logger.debug("Adding Cloudian group: " + group);
        try {
            final HttpResponse response = put("/group", group);
            return response.getStatusLine().getStatusCode() == HttpStatus.SC_OK;
        } catch (final IOException e) {
            logger.error("Failed to add Cloudian group due to:", e);
            throwTimeoutOrServerException(e);
        }
        return false;
    }

    /**
     * Get the HyperStore group
     * @param groupId the group to return
     * @return the group if it exists or null if it does not exist.
     * @throws ServerApiException on error
     */
    public CloudianGroup listGroup(final String groupId) {
        if (StringUtils.isEmpty(groupId)) {
            return null;
        }
        logger.debug("Trying to find Cloudian group with id=" + groupId);
        try {
            final HttpResponse response = get(String.format("/group?groupId=%s", groupId));
            checkResponseOK(response);
            if (noResponseEntity(response)) {
                return null; // Group Not Found
            }
            final ObjectMapper mapper = new ObjectMapper();
            return mapper.readValue(response.getEntity().getContent(), CloudianGroup.class);
        } catch (final IOException e) {
            logger.error("Failed to list Cloudian group due to:", e);
            throwTimeoutOrServerException(e);
            return null; // never reached
        }
    }

    public List<CloudianGroup> listGroups() {
        logger.debug("Trying to list Cloudian groups");
        try {
            final HttpResponse response = get("/group/list");
            checkResponseOK(response);
            if (noResponseEntity(response)) {
                throw new ServerApiException(ApiErrorCode.INTERNAL_ERROR, "API error");
            }
            InputStream iStream = getNonEmptyContentStream(response);
            if (iStream == null) {
                return new ArrayList<>(); // Empty body => empty list
            }
            final ObjectMapper mapper = new ObjectMapper();
            return Arrays.asList(mapper.readValue(iStream, CloudianGroup[].class));
        } catch (final IOException e) {
            logger.error("Failed to list Cloudian groups due to:", e);
            throwTimeoutOrServerException(e);
            return new ArrayList<>(); // never reached
        }
    }

    public boolean updateGroup(final CloudianGroup group) {
        if (group == null) {
            return false;
        }
        logger.debug("Updating Cloudian group: " + group);
        try {
            final HttpResponse response = post("/group", group);
            return response.getStatusLine().getStatusCode() == HttpStatus.SC_OK;
        } catch (final IOException e) {
            logger.error("Failed to update group due to:", e);
            throwTimeoutOrServerException(e);
        }
        return false;
    }

    public boolean removeGroup(final String groupId) {
        if (StringUtils.isEmpty(groupId)) {
            return false;
        }
        logger.debug("Removing Cloudian group id=" + groupId);
        try {
            final HttpResponse response = delete(String.format("/group?groupId=%s", groupId));
            return response.getStatusLine().getStatusCode() == HttpStatus.SC_OK;
        } catch (final IOException e) {
            logger.error("Failed to remove group due to:", e);
            throwTimeoutOrServerException(e);
        }
        return false;
    }
}
