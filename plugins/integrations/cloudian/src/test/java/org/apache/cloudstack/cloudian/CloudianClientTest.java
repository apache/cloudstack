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

package org.apache.cloudstack.cloudian;

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

import java.util.List;

import org.apache.cloudstack.api.ServerApiException;
import org.apache.cloudstack.cloudian.client.CloudianClient;
import org.apache.cloudstack.cloudian.client.CloudianGroup;
import org.apache.cloudstack.cloudian.client.CloudianUser;
import org.junit.Assert;
import org.junit.Before;
import org.junit.Rule;
import org.junit.Test;

import com.cloud.utils.exception.CloudRuntimeException;
import com.github.tomakehurst.wiremock.client.BasicCredentials;
import com.github.tomakehurst.wiremock.junit.WireMockRule;

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
    public void listUserAccountFail() {
        wireMockRule.stubFor(get(urlPathMatching("/user?.*"))
                .willReturn(aResponse()
                        .withHeader("content-type", "application/json")
                        .withBody("")));

        final CloudianUser user = client.listUser("abc", "xyz");
        Assert.assertNull(user);
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
    public void testEmptyListUsersResponse() {
        wireMockRule.stubFor(get(urlPathMatching("/user/list"))
                .willReturn(aResponse()
                        .withHeader("content-type", "application/json")
                        .withStatus(204)
                        .withBody("")));
        Assert.assertTrue(client.listUsers("someGroup").size() == 0);

        wireMockRule.stubFor(get(urlPathMatching("/user"))
                .willReturn(aResponse()
                        .withHeader("content-type", "application/json")
                        .withStatus(204)
                        .withBody("")));
        Assert.assertNull(client.listUser("someUserId", "someGroupId"));
    }

    @Test
    public void listUserAccountsFail() {
        wireMockRule.stubFor(get(urlPathMatching("/user/list?.*"))
                .willReturn(aResponse()
                        .withHeader("content-type", "application/json")
                        .withBody("")));

        final List<CloudianUser> users = client.listUsers("xyz");
        Assert.assertEquals(users.size(), 0);
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
    public void listGroupFail() {
        wireMockRule.stubFor(get(urlPathMatching("/group.*"))
                .willReturn(aResponse()
                        .withHeader("content-type", "application/json")
                        .withBody("")));

        final CloudianGroup group = client.listGroup("xyz");
        Assert.assertNull(group);
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
    public void listGroupsFail() {
        wireMockRule.stubFor(get(urlEqualTo("/group/list"))
                .willReturn(aResponse()
                        .withHeader("content-type", "application/json")
                        .withBody("")));

        final List<CloudianGroup> groups = client.listGroups();
        Assert.assertEquals(groups.size(), 0);
    }

    @Test
    public void testEmptyListGroupResponse() {
        wireMockRule.stubFor(get(urlEqualTo("/group/list"))
                .willReturn(aResponse()
                        .withHeader("content-type", "application/json")
                        .withStatus(204)
                        .withBody("")));

        Assert.assertTrue(client.listGroups().size() == 0);


        wireMockRule.stubFor(get(urlPathMatching("/group"))
                .willReturn(aResponse()
                        .withHeader("content-type", "application/json")
                        .withStatus(204)
                        .withBody("")));
        Assert.assertNull(client.listGroup("someGroup"));
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
