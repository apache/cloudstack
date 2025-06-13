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

import static com.github.tomakehurst.wiremock.client.WireMock.aResponse;
import static com.github.tomakehurst.wiremock.client.WireMock.containing;
import static com.github.tomakehurst.wiremock.client.WireMock.delete;
import static com.github.tomakehurst.wiremock.client.WireMock.deleteRequestedFor;
import static com.github.tomakehurst.wiremock.client.WireMock.equalTo;
import static com.github.tomakehurst.wiremock.client.WireMock.get;
import static com.github.tomakehurst.wiremock.client.WireMock.getRequestedFor;
import static com.github.tomakehurst.wiremock.client.WireMock.post;
import static com.github.tomakehurst.wiremock.client.WireMock.postRequestedFor;
import static com.github.tomakehurst.wiremock.client.WireMock.put;
import static com.github.tomakehurst.wiremock.client.WireMock.putRequestedFor;
import static com.github.tomakehurst.wiremock.client.WireMock.urlEqualTo;
import static com.github.tomakehurst.wiremock.client.WireMock.urlPathMatching;
import static com.github.tomakehurst.wiremock.client.WireMock.verify;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

import java.io.ByteArrayInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.util.ArrayList;
import java.util.List;

import org.apache.http.HttpEntity;
import org.apache.http.HttpResponse;

import org.apache.cloudstack.api.ApiErrorCode;
import org.apache.cloudstack.api.ServerApiException;
import org.apache.cloudstack.cloudian.client.CloudianUserBucketUsage.CloudianBucketUsage;
import org.junit.Assert;
import org.junit.Before;
import org.junit.Rule;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.junit.MockitoJUnitRunner;

import com.cloud.utils.exception.CloudRuntimeException;
import com.github.tomakehurst.wiremock.client.BasicCredentials;
import com.github.tomakehurst.wiremock.junit.WireMockRule;

@RunWith(MockitoJUnitRunner.class)
public class CloudianClientTest {
    private final int port = 14333;
    private final int timeout = 2;
    private final String adminUsername = "admin";
    private final String adminPassword = "public";
    private CloudianClient client;

    @Rule
    public WireMockRule wireMockRule = new WireMockRule(port);

    @Before
    public void setUp() throws Exception {
        client = new CloudianClient("localhost", port, "http", adminUsername, adminPassword, false, timeout);
    }

    private CloudianUser getTestUser() {
        final CloudianUser user = new CloudianUser();
        user.setActive(true);
        user.setUserId("someUserId");
        user.setGroupId("someGroupId");
        user.setUserType(CloudianUser.USER);
        user.setFullName("John Doe");
        return user;
    }

    private CloudianGroup getTestGroup() {
        final CloudianGroup group = new CloudianGroup();
        group.setActive(true);
        group.setGroupId("someGroupId");
        group.setGroupName("someGroupName");
        return group;
    }

    ////////////////////////////////////////////////////////
    //////////////// General API tests /////////////////////
    ////////////////////////////////////////////////////////

    @Test(expected = CloudRuntimeException.class)
    public void testRequestTimeout() {
        wireMockRule.stubFor(get(urlEqualTo("/group/list"))
                .willReturn(aResponse()
                        .withHeader("content-type", "application/json")
                        .withStatus(200)
                        .withFixedDelay(2 * timeout * 1000)
                        .withBody("")));
        client.listGroups();
    }

    @Test
    public void testBasicAuth() {
        wireMockRule.stubFor(get(urlEqualTo("/group/list"))
                .willReturn(aResponse()
                        .withStatus(200)
                        .withBody("[]")));
        client.listGroups();
        verify(getRequestedFor(urlEqualTo("/group/list"))
                .withBasicAuth(new BasicCredentials(adminUsername, adminPassword)));
    }

    @Test(expected = ServerApiException.class)
    public void testBasicAuthFailure() {
        wireMockRule.stubFor(get(urlPathMatching("/user"))
                .willReturn(aResponse()
                        .withStatus(401)
                        .withBody("")));
        client.listUser("someUserId", "somegGroupId");
    }

    @Test
    public void getNonEmptyContentStreamEmpty() {
        InputStream emptyStream = new ByteArrayInputStream(new byte[]{});
        HttpEntity entity = mock(HttpEntity.class);
        HttpResponse response = mock(HttpResponse.class);
        when(response.getEntity()).thenReturn(entity);
        try {
                when(entity.getContent()).thenReturn(emptyStream);
                Assert.assertNull(client.getNonEmptyContentStream(response));
        } catch (IOException e) {
                Assert.fail("Should not be any exception here");
        }
    }

    @Test
    public void getNonEmptyContentStreamWithContent() {
        InputStream nonEmptyStream = new ByteArrayInputStream(new byte[]{9, 8});
        HttpEntity entity = mock(HttpEntity.class);
        HttpResponse response = mock(HttpResponse.class);
        when(response.getEntity()).thenReturn(entity);
        try {
                when(entity.getContent()).thenReturn(nonEmptyStream);
                InputStream is = client.getNonEmptyContentStream(response);
                Assert.assertNotNull(is);
                Assert.assertEquals(9, is.read());
                Assert.assertEquals(8, is.read());
                Assert.assertEquals(-1, is.read());
        } catch (IOException e) {
                Assert.fail("Should not be any exception here");
        }
    }

    /////////////////////////////////////////////////////
    //////////////// System API tests ///////////////////
    /////////////////////////////////////////////////////

    @Test
    public void getServerVersion() {
        final String expect = "8.1 Compiled: 2023-11-11 16:30";
        wireMockRule.stubFor(get(urlEqualTo("/system/version"))
                .willReturn(aResponse()
                        .withStatus(200)
                        .withBody(expect)));

        String version = client.getServerVersion();
        Assert.assertEquals(expect, version);
    }

    @Test
    public void getUserBucketUsagesBadUsageBlankGroup() {
        ServerApiException thrown = Assert.assertThrows(ServerApiException.class, () -> client.getUserBucketUsages(null, null, null));
        Assert.assertNotNull(thrown);
        Assert.assertEquals(ApiErrorCode.PARAM_ERROR, thrown.getErrorCode());
    }

    @Test
    public void getUserBucketUsagesBadUsageBlankUserWithBucket() {
        ServerApiException thrown = Assert.assertThrows(ServerApiException.class, () -> client.getUserBucketUsages("group", "", "bucket"));
        Assert.assertNotNull(thrown);
        Assert.assertEquals(ApiErrorCode.PARAM_ERROR, thrown.getErrorCode());
    }

    @Test
    public void getUserBucketUsagesEmptyGroup() {
        wireMockRule.stubFor(get(urlEqualTo("/system/bucketusage?groupId=mygroup"))
                .willReturn(aResponse()
                        .withHeader("content-type", "application/json")
                        .withStatus(200)
                        .withBody("[]")));
        List<CloudianUserBucketUsage> bucketUsages = client.getUserBucketUsages("mygroup", null, null);
        Assert.assertEquals(0, bucketUsages.size());
    }

    @Test(expected = CloudRuntimeException.class)
    public void getUserBucketUsagesNoSuchGroup() {
        // no group, no user, no bucket etc are all 400
        wireMockRule.stubFor(get(urlEqualTo("/system/bucketusage?groupId=mygroup"))
                .willReturn(aResponse()
                        .withStatus(400)
                        .withBody("")));
        client.getUserBucketUsages("mygroup", null, null);
        Assert.fail("The request should throw an exception");
    }

    @Test
    public void getUserBucketUsagesUserNoBuckets() {
        wireMockRule.stubFor(get(urlEqualTo("/system/bucketusage?groupId=mygroup&userId=u1"))
                .willReturn(aResponse()
                        .withHeader("content-type", "application/json")
                        .withStatus(200)
                        .withBody("[{\"userId\": \"u1\", \"buckets\": []}]")));
        List<CloudianUserBucketUsage> bucketUsages = client.getUserBucketUsages("mygroup", "u1", null);
        Assert.assertEquals(1, bucketUsages.size());
        CloudianUserBucketUsage u1 = bucketUsages.get(0);
        Assert.assertEquals("u1", u1.getUserId());
        Assert.assertEquals(0, u1.getBuckets().size());
    }

    @Test
    public void getUserBucketUsagesForBucket() {
        wireMockRule.stubFor(get(urlEqualTo("/system/bucketusage?groupId=mygroup&userId=u1&bucket=b1"))
                .willReturn(aResponse()
                        .withHeader("content-type", "application/json")
                        .withStatus(200)
                        .withBody("[{\"userId\": \"u1\", \"buckets\": [{\"bucketName\":\"b1\",\"objectCount\":1,\"byteCount\":5,\"policyName\":\"p1\"}]}]")));
        List<CloudianUserBucketUsage> bucketUsages = client.getUserBucketUsages("mygroup", "u1", "b1");
        Assert.assertEquals(1, bucketUsages.size());
        CloudianUserBucketUsage u1 = bucketUsages.get(0);
        Assert.assertEquals("u1", u1.getUserId());
        Assert.assertEquals(1, u1.getBuckets().size());
        CloudianBucketUsage cbu = u1.getBuckets().get(0);
        Assert.assertEquals("b1", cbu.getBucketName());
        Assert.assertEquals(5L, cbu.getByteCount().longValue());
        Assert.assertEquals(1L, cbu.getObjectCount().longValue());
        Assert.assertEquals("p1", cbu.getPolicyName());
    }

    @Test
    public void getUserBucketUsagesOneUserTwoBuckets() {
        CloudianUserBucketUsage expect_u1 = new CloudianUserBucketUsage();
        expect_u1.setUserId("u1");
        CloudianBucketUsage b1 = new CloudianBucketUsage();
        b1.setBucketName("b1");
        b1.setByteCount(123L);
        b1.setObjectCount(456L);
        b1.setPolicyName("pname");
        CloudianBucketUsage b2 = new CloudianBucketUsage();
        b2.setBucketName("b2");
        b2.setByteCount(789L);
        b2.setObjectCount(0L);
        b2.setPolicyName("pname2");
        List<CloudianBucketUsage> buckets = new ArrayList<CloudianBucketUsage>();
        buckets.add(b1);
        buckets.add(b2);
        expect_u1.setBuckets(buckets);
        int expect_size = buckets.size();

        int bucket_count = 0;
        StringBuilder sb = new StringBuilder();
        sb.append("[{\"userId\": \"u1\", \"buckets\": [");
        for (CloudianBucketUsage b : buckets) {
                sb.append("{\"bucketName\": \"");
                sb.append(b.getBucketName());
                sb.append("\", \"byteCount\": ");
                sb.append(b.getByteCount());
                sb.append(", \"objectCount\": ");
                sb.append(b.getObjectCount());
                sb.append(", \"policyName\": \"");
                sb.append(b.getPolicyName());
                sb.append("\"}");
                if (++bucket_count < expect_size) {
                        sb.append(",");
                }
        }
        sb.append("]}]");
        wireMockRule.stubFor(get(urlEqualTo("/system/bucketusage?groupId=mygroup&userId=u1"))
                .willReturn(aResponse()
                        .withHeader("content-type", "application/json")
                        .withStatus(200)
                        .withBody(sb.toString())));
        List<CloudianUserBucketUsage> bucketUsages = client.getUserBucketUsages("mygroup", "u1", null);
        Assert.assertEquals(1, bucketUsages.size());
        CloudianUserBucketUsage u1 = bucketUsages.get(0);
        Assert.assertEquals("u1", u1.getUserId());
        Assert.assertEquals(expect_size, u1.getBuckets().size());
        for (int i = 0; i < expect_size; i++) {
                CloudianBucketUsage actual = u1.getBuckets().get(i);
                CloudianBucketUsage expected = buckets.get(i);
                Assert.assertEquals(expected.getBucketName(), actual.getBucketName());
                Assert.assertEquals(expected.getByteCount(), actual.getByteCount());
                Assert.assertEquals(expected.getObjectCount(), actual.getObjectCount());
                Assert.assertEquals(expected.getPolicyName(), actual.getPolicyName());
        }
    }

    @Test
    public void getUserBucketUsagesTwoUsers() {
        CloudianUserBucketUsage expect_u1 = new CloudianUserBucketUsage();
        expect_u1.setUserId("u1");
        CloudianBucketUsage b1 = new CloudianBucketUsage();
        b1.setBucketName("b1");
        b1.setByteCount(123L);
        b1.setObjectCount(456L);
        b1.setPolicyName("pname");
        CloudianBucketUsage b2 = new CloudianBucketUsage();
        b2.setBucketName("b2");
        b2.setByteCount(789L);
        b2.setObjectCount(0L);
        b2.setPolicyName("pname2");
        List<CloudianBucketUsage> buckets = new ArrayList<CloudianBucketUsage>();
        buckets.add(b1);
        buckets.add(b2);
        expect_u1.setBuckets(buckets);
        int expect_size = buckets.size();

        int bucket_count = 0;
        StringBuilder sb = new StringBuilder();
        sb.append("[{\"userId\": \"u1\", \"buckets\": [");
        for (CloudianBucketUsage b : buckets) {
                sb.append("{\"bucketName\": \"");
                sb.append(b.getBucketName());
                sb.append("\", \"byteCount\": ");
                sb.append(b.getByteCount());
                sb.append(", \"objectCount\": ");
                sb.append(b.getObjectCount());
                sb.append(", \"policyName\": \"");
                sb.append(b.getPolicyName());
                sb.append("\"}");
                if (++bucket_count < expect_size) {
                        sb.append(",");
                }
        }
        sb.append("]}, {\"userId\": \"u2\", \"buckets\": []}]");
        wireMockRule.stubFor(get(urlEqualTo("/system/bucketusage?groupId=mygroup"))
                .willReturn(aResponse()
                        .withHeader("content-type", "application/json")
                        .withStatus(200)
                        .withBody(sb.toString())));
        List<CloudianUserBucketUsage> bucketUsages = client.getUserBucketUsages("mygroup", null, null);
        Assert.assertEquals(2, bucketUsages.size());
        CloudianUserBucketUsage u1 = bucketUsages.get(0);
        Assert.assertEquals("u1", u1.getUserId());
        Assert.assertEquals(expect_size, u1.getBuckets().size());
        for (int i = 0; i < expect_size; i++) {
                CloudianBucketUsage actual = u1.getBuckets().get(i);
                CloudianBucketUsage expected = buckets.get(i);
                Assert.assertEquals(expected.getBucketName(), actual.getBucketName());
                Assert.assertEquals(expected.getByteCount(), actual.getByteCount());
                Assert.assertEquals(expected.getObjectCount(), actual.getObjectCount());
                Assert.assertEquals(expected.getPolicyName(), actual.getPolicyName());
        }
        // 2nd user has 0 buckets
        CloudianUserBucketUsage u2 = bucketUsages.get(1);
        Assert.assertEquals("u2", u2.getUserId());
        Assert.assertEquals(0, u2.getBuckets().size());
    }

    /////////////////////////////////////////////////////
    //////////////// User API tests /////////////////////
    /////////////////////////////////////////////////////

    @Test
    public void addUserAccount() {
        wireMockRule.stubFor(put(urlEqualTo("/user"))
                .willReturn(aResponse()
                        .withStatus(200)
                        .withBody("")));

        final CloudianUser user = getTestUser();
        boolean result = client.addUser(user);
        Assert.assertTrue(result);
        verify(putRequestedFor(urlEqualTo("/user"))
                .withRequestBody(containing("userId\":\"" + user.getUserId()))
                .withHeader("content-type", equalTo("application/json")));
    }

    @Test
    public void addUserAccountFail() {
        wireMockRule.stubFor(put(urlEqualTo("/user"))
                .willReturn(aResponse()
                        .withStatus(400)
                        .withBody("")));

        final CloudianUser user = getTestUser();
        boolean result = client.addUser(user);
        Assert.assertFalse(result);
    }

    @Test
    public void listUserAccount() {
        final String userId = "someUser";
        final String groupId = "someGroup";
        wireMockRule.stubFor(get(urlPathMatching("/user?.*"))
                .willReturn(aResponse()
                        .withHeader("content-type", "application/json")
                        .withBody("{\"userId\":\"someUser\",\"userType\":\"User\",\"fullName\":\"John Doe (jdoe)\",\"emailAddr\":\"j@doe.com\",\"address1\":null,\"address2\":null,\"city\":null,\"state\":null,\"zip\":null,\"country\":null,\"phone\":null,\"groupId\":\"someGroup\",\"website\":null,\"active\":\"true\",\"canonicalUserId\":\"b3940886468689d375ebf8747b151c37\",\"ldapEnabled\":false}")));

        final CloudianUser user = client.listUser(userId, groupId);
        Assert.assertEquals(user.getActive(), true);
        Assert.assertEquals(user.getUserId(), userId);
        Assert.assertEquals(user.getGroupId(), groupId);
        Assert.assertEquals(user.getUserType(), "User");
    }

    @Test
    public void listUserAccountNotFound() {
        wireMockRule.stubFor(get(urlPathMatching("/user?.*"))
                .willReturn(aResponse()
                        .withHeader("content-type", "application/json")
                        .withStatus(204) // 204 not found
                        .withBody("")));

        final CloudianUser user = client.listUser("abc", "xyz");
        Assert.assertNull(user);
    }

    @Test(expected = ServerApiException.class)
    public void listUserAccountFail() {
        wireMockRule.stubFor(get(urlPathMatching("/user?.*"))
                .willReturn(aResponse()
                        .withHeader("content-type", "application/json")
                        .withBody("")));

        client.listUser("abc", "xyz");
    }

    @Test
    public void listUserAccounts() {
        final String groupId = "someGroup";
        wireMockRule.stubFor(get(urlPathMatching("/user/list?.*"))
                .willReturn(aResponse()
                        .withHeader("content-type", "application/json")
                        .withBody("[{\"userId\":\"someUser\",\"userType\":\"User\",\"fullName\":\"John Doe (jdoe)\",\"emailAddr\":\"j@doe.com\",\"address1\":null,\"address2\":null,\"city\":null,\"state\":null,\"zip\":null,\"country\":null,\"phone\":null,\"groupId\":\"someGroup\",\"website\":null,\"active\":\"true\",\"canonicalUserId\":\"b3940886468689d375ebf8747b151c37\",\"ldapEnabled\":false}]")));

        final List<CloudianUser> users = client.listUsers(groupId);
        Assert.assertEquals(users.size(), 1);
        Assert.assertEquals(users.get(0).getActive(), true);
        Assert.assertEquals(users.get(0).getGroupId(), groupId);
    }

    @Test
    public void listUserAccountsEmptyList() {
        // empty body with 200 is returned if either:
        // 1. the group is unknown (ie. there is no not found case)
        // 2. the group contains no users
        wireMockRule.stubFor(get(urlPathMatching("/user/list"))
                .willReturn(aResponse()
                        .withHeader("content-type", "application/json")
                        .withBody("")));
        Assert.assertEquals(0, client.listUsers("someGroup").size());
    }

    @Test(expected = ServerApiException.class)
    public void listUserAccountsFail() {
        wireMockRule.stubFor(get(urlPathMatching("/user/list?.*"))
                .willReturn(aResponse()
                        .withHeader("content-type", "application/json")
                        .withStatus(204)  // bad protocol response
                        .withBody("")));

        client.listUsers("xyz");
    }

    @Test
    public void updateUserAccount() {
        wireMockRule.stubFor(post(urlEqualTo("/user"))
                .willReturn(aResponse()
                        .withStatus(200)
                        .withBody("")));

        final CloudianUser user = getTestUser();
        boolean result = client.updateUser(user);
        Assert.assertTrue(result);
        verify(postRequestedFor(urlEqualTo("/user"))
                .withRequestBody(containing("userId\":\"" + user.getUserId()))
                .withHeader("content-type", equalTo("application/json")));
    }

    @Test
    public void updateUserAccountFail() {
        wireMockRule.stubFor(post(urlEqualTo("/user"))
                .willReturn(aResponse()
                        .withStatus(400)
                        .withBody("")));

        boolean result = client.updateUser(getTestUser());
        Assert.assertFalse(result);
    }

    @Test
    public void removeUserAccount() {
        wireMockRule.stubFor(delete(urlPathMatching("/user.*"))
                .willReturn(aResponse()
                        .withStatus(200)
                        .withBody("")));
        final CloudianUser user = getTestUser();
        boolean result = client.removeUser(user.getUserId(), user.getGroupId());
        Assert.assertTrue(result);
        verify(deleteRequestedFor(urlPathMatching("/user.*"))
                .withQueryParam("userId", equalTo(user.getUserId())));
    }

    @Test
    public void removeUserAccountFail() {
        wireMockRule.stubFor(delete(urlPathMatching("/user.*"))
                .willReturn(aResponse()
                        .withStatus(400)
                        .withBody("")));
        final CloudianUser user = getTestUser();
        boolean result = client.removeUser(user.getUserId(), user.getGroupId());
        Assert.assertFalse(result);
    }

    @Test
    public void createCredential() {
        final String expected_ak = "28d945de2a2623fc9483";
        final String expected_sk = "j2OrPGHF69hp3YsZHRHOCWdAQDabppsBtD7kttr9";
        final long expected_createDate = 1502285593100L;

        final String json = String.format("{\"accessKey\": \"%s\", \"active\": true, \"createDate\": 1502285593100, \"expireDate\": null, \"secretKey\": \"%s\"}", expected_ak, expected_sk);
        wireMockRule.stubFor(put(urlPathMatching("/user/credentials.*"))
                .willReturn(aResponse()
                        .withStatus(200)
                        .withBody(json)));

        CloudianCredential credential = client.createCredential("u1", "g1");
        Assert.assertEquals(expected_ak, credential.getAccessKey());
        Assert.assertEquals(expected_sk, credential.getSecretKey());
        Assert.assertEquals(true, credential.getActive());
        Assert.assertEquals(expected_createDate, credential.getCreateDate().getTime());
        Assert.assertNull(credential.getExpireDate());
    }

    @Test(expected = ServerApiException.class)
    public void createCredentialNoSuchUser() {
        wireMockRule.stubFor(put(urlPathMatching("/user/credentials.*"))
                .willReturn(aResponse()
                        .withStatus(400)
                        .withBody("")));
        client.createCredential("u1", "g1");
    }

    @Test(expected = ServerApiException.class)
    public void createCredentialMaxCredentials() {
        wireMockRule.stubFor(put(urlPathMatching("/user/credentials.*"))
                .willReturn(aResponse()
                        .withStatus(403)
                        .withBody("")));
        client.createCredential("u1", "g1");
    }

    @Test(expected = ServerApiException.class)
    public void createCredentialBadMissingResponse() {
        wireMockRule.stubFor(put(urlPathMatching("/user/credentials.*"))
                .willReturn(aResponse()
                        .withStatus(200)
                        .withBody("")));  // 200 should return a credential
        client.createCredential("u1", "g1");
    }

    @Test
    public void listCredentials() {
        final String expected_ak = "28d945de2a2623fc9483";
        final String expected_sk = "j2OrPGHF69hp3YsZHRHOCWdAQDabppsBtD7kttr9";

        final String json = String.format("[{\"accessKey\": \"%s\", \"active\": true, \"createDate\": 1502285593100, \"expireDate\": null, \"secretKey\": \"%s\"}]", expected_ak, expected_sk);
        wireMockRule.stubFor(get(urlPathMatching("/user/credentials/list.*"))
                .willReturn(aResponse()
                        .withStatus(200)
                        .withBody(json)));

        List<CloudianCredential> credentials = client.listCredentials("u1", "g1");
        Assert.assertEquals(1, credentials.size());
        Assert.assertEquals(expected_ak, credentials.get(0).getAccessKey());
    }

    @Test
    public void listCredentialsMany() {
        final String expected_ak = "28d945de2a2623fc9483";
        final String expected_sk = "j2OrPGHF69hp3YsZHRHOCWdAQDabppsBtD7kttr9";
        final int expected_size = 3;
        StringBuilder sb = new StringBuilder();
        sb.append("[");
        for (int i = 0; i < expected_size; i++) {
                sb.append(String.format("{\"accessKey\": \"%s-%d\", \"active\": true, \"createDate\": 1502285593100, \"expireDate\": null, \"secretKey\": \"%s-%d\"}", expected_ak, i, expected_sk, i));
                if (i + 1 < expected_size) {
                        sb.append(",");
                }
        }
        sb.append("]");
        String json = sb.toString();
        wireMockRule.stubFor(get(urlPathMatching("/user/credentials/list.*"))
                .willReturn(aResponse()
                        .withStatus(200)
                        .withBody(json)));

        List<CloudianCredential> credentials = client.listCredentials("u1", "g1");
        Assert.assertEquals(expected_size, credentials.size());
        Assert.assertEquals(expected_ak + "-2", credentials.get(2).getAccessKey());
    }

    @Test
    public void listCredentialsEmptyList() {
        wireMockRule.stubFor(get(urlPathMatching("/user/credentials/list.*"))
                .willReturn(aResponse()
                        .withStatus(204)  // 204 is empty list for credentials
                        .withBody("")));

        List<CloudianCredential> credentials = client.listCredentials("u1", "g1");
        Assert.assertEquals(0, credentials.size());
    }

    @Test(expected = ServerApiException.class)
    public void listCredentialsNoSuchUser() {
        wireMockRule.stubFor(get(urlPathMatching("/user/credentials/list.*"))
                .willReturn(aResponse()
                        .withStatus(400)  // No such user case
                        .withBody("")));

        client.listCredentials("u1", "g1");
    }

    @Test(expected = ServerApiException.class)
    public void listCredentialsBad200EmptyBody() {
        wireMockRule.stubFor(get(urlPathMatching("/user/credentials/list.*"))
                .willReturn(aResponse()
                        .withStatus(200)  // Bad protocol. should be 204 if empty
                        .withBody("")));

        client.listCredentials("u1", "g1");
    }

    //////////////////////////////////////////////////////
    //////////////// Group API tests /////////////////////
    //////////////////////////////////////////////////////

    @Test
    public void addGroup() {
        wireMockRule.stubFor(put(urlEqualTo("/group"))
                .willReturn(aResponse()
                        .withStatus(200)
                        .withBody("")));

        final CloudianGroup group = getTestGroup();
        boolean result = client.addGroup(group);
        Assert.assertTrue(result);
        verify(putRequestedFor(urlEqualTo("/group"))
                .withRequestBody(containing("groupId\":\"someGroupId"))
                .withHeader("content-type", equalTo("application/json")));
    }

    @Test
    public void addGroupFail() throws Exception {
        wireMockRule.stubFor(put(urlEqualTo("/group"))
                .willReturn(aResponse()
                        .withStatus(400)
                        .withBody("")));

        final CloudianGroup group = getTestGroup();
        boolean result = client.addGroup(group);
        Assert.assertFalse(result);
    }

    @Test
    public void listGroup() {
        final String groupId = "someGroup";
        wireMockRule.stubFor(get(urlPathMatching("/group.*"))
                .willReturn(aResponse()
                        .withHeader("content-type", "application/json")
                        .withBody("{\"groupId\":\"someGroup\",\"groupName\":\"/someDomain\",\"ldapGroup\":null,\"active\":\"true\",\"ldapEnabled\":false,\"ldapServerURL\":null,\"ldapUserDNTemplate\":null,\"ldapSearch\":null,\"ldapSearchUserBase\":null,\"ldapMatchAttribute\":null}")));

        final CloudianGroup group = client.listGroup(groupId);
        Assert.assertEquals(group.getActive(), true);
        Assert.assertEquals(group.getGroupId(), groupId);
    }

    @Test
    public void listGroupNotFound() {
        wireMockRule.stubFor(get(urlPathMatching("/group.*"))
                .willReturn(aResponse()
                        .withHeader("content-type", "application/json")
                        .withStatus(204) // group not found
                        .withBody("")));
        Assert.assertNull(client.listGroup("someGroup"));
    }

    @Test(expected = ServerApiException.class)
    public void listGroupFail() {
        // Returning 200 with an empty body is not expected behaviour
        wireMockRule.stubFor(get(urlPathMatching("/group.*"))
                .willReturn(aResponse()
                        .withHeader("content-type", "application/json")
                        .withBody("")));

        client.listGroup("xyz");
    }

    @Test
    public void listGroups() {
        final String groupId = "someGroup";
        wireMockRule.stubFor(get(urlEqualTo("/group/list"))
                .willReturn(aResponse()
                        .withHeader("content-type", "application/json")
                        .withBody("[{\"groupId\":\"someGroup\",\"groupName\":\"/someDomain\",\"ldapGroup\":null,\"active\":\"true\",\"ldapEnabled\":false,\"ldapServerURL\":null,\"ldapUserDNTemplate\":null,\"ldapSearch\":null,\"ldapSearchUserBase\":null,\"ldapMatchAttribute\":null}]")));

        final List<CloudianGroup> groups = client.listGroups();
        Assert.assertEquals(groups.size(), 1);
        Assert.assertEquals(groups.get(0).getActive(), true);
        Assert.assertEquals(groups.get(0).getGroupId(), groupId);
    }

    @Test
    public void listGroupsEmptyList() {
        wireMockRule.stubFor(get(urlEqualTo("/group/list"))
                .willReturn(aResponse()
                        .withHeader("content-type", "application/json")
                        .withBody("")));

        final List<CloudianGroup> groups = client.listGroups();
        Assert.assertEquals(0, groups.size());
    }

    @Test(expected = ServerApiException.class)
    public void listGroupsBad204Response() {
        wireMockRule.stubFor(get(urlEqualTo("/group/list"))
                .willReturn(aResponse()
                        .withHeader("content-type", "application/json")
                        .withStatus(204)  // bad response. should never be 204
                        .withBody("")));
        client.listGroups();
    }

    @Test
    public void updateGroup() {
        wireMockRule.stubFor(post(urlEqualTo("/group"))
                .willReturn(aResponse()
                        .withStatus(200)
                        .withBody("")));

        final CloudianGroup group = getTestGroup();
        boolean result = client.updateGroup(group);
        Assert.assertTrue(result);
        verify(postRequestedFor(urlEqualTo("/group"))
                .withRequestBody(containing("groupId\":\"" + group.getGroupId()))
                .withHeader("content-type", equalTo("application/json")));
    }

    @Test
    public void updateGroupFail() {
        wireMockRule.stubFor(post(urlEqualTo("/group"))
                .willReturn(aResponse()
                        .withStatus(400)
                        .withBody("")));

        boolean result = client.updateGroup(getTestGroup());
        Assert.assertFalse(result);
    }

    @Test
    public void removeGroup() {
        wireMockRule.stubFor(delete(urlPathMatching("/group.*"))
                .willReturn(aResponse()
                        .withStatus(200)
                        .withBody("")));
        final CloudianGroup group = getTestGroup();
        boolean result = client.removeGroup(group.getGroupId());
        Assert.assertTrue(result);
        verify(deleteRequestedFor(urlPathMatching("/group.*"))
                .withQueryParam("groupId", equalTo(group.getGroupId())));
    }

    @Test
    public void removeGroupFail() {
        wireMockRule.stubFor(delete(urlPathMatching("/group.*"))
                .willReturn(aResponse()
                        .withStatus(400)
                        .withBody("")));
        final CloudianGroup group = getTestGroup();
        boolean result = client.removeGroup(group.getGroupId());
        Assert.assertFalse(result);
    }
}
