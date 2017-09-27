package com.cloudian.client;

import static com.github.tomakehurst.wiremock.client.WireMock.containing;
import static com.github.tomakehurst.wiremock.client.WireMock.deleteRequestedFor;
import static com.github.tomakehurst.wiremock.client.WireMock.equalTo;
import static com.github.tomakehurst.wiremock.client.WireMock.getRequestedFor;
import static com.github.tomakehurst.wiremock.client.WireMock.postRequestedFor;
import static com.github.tomakehurst.wiremock.client.WireMock.putRequestedFor;
import static com.github.tomakehurst.wiremock.client.WireMock.urlEqualTo;
import static com.github.tomakehurst.wiremock.client.WireMock.urlPathMatching;

import java.util.List;

import org.junit.After;
import org.junit.Assert;
import org.junit.Before;
import org.junit.Rule;
import org.junit.Test;

import com.cloud.utils.exception.CloudRuntimeException;
import com.github.tomakehurst.wiremock.client.BasicCredentials;
import com.github.tomakehurst.wiremock.client.WireMock;
import com.github.tomakehurst.wiremock.junit.WireMockRule;

public class CloudianClientTest {
    @Rule
    public WireMockRule wireMockRule = new WireMockRule(14333);

    private final int timeout = 2;
    private final String adminUsername = "admin";
    private final String adminPassword = "public";
    private CloudianClient client;

    @Before
    public void setUp() throws Exception {
        client = new CloudianClient("http://localhost:14333", adminUsername, adminPassword, false, timeout);
    }

    @After
    public void tearDown() throws Exception {
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
        wireMockRule.stubFor(WireMock.get(urlEqualTo("/group/list"))
                .willReturn(WireMock.aResponse()
                        .withHeader("Content-Type", "application/json")
                        .withStatus(200)
                        .withFixedDelay(2 * timeout * 1000)
                        .withBody("")));
        client.listGroups();
    }

    @Test(expected = CloudRuntimeException.class)
    public void testBasicAuth() {
        wireMockRule.stubFor(WireMock.get(urlEqualTo("/group/list"))
                .willReturn(WireMock.aResponse()
                        .withStatus(200)
                        .withBody("")));
        client.listGroups();
        WireMock.verify(getRequestedFor(urlEqualTo("/group/list"))
                .withBasicAuth(new BasicCredentials(adminUsername, adminPassword)));
    }

    /////////////////////////////////////////////////////
    //////////////// User API tests /////////////////////
    /////////////////////////////////////////////////////

    @Test
    public void addUserAccount() {
        wireMockRule.stubFor(WireMock.put(urlEqualTo("/user"))
                .willReturn(WireMock.aResponse()
                        .withStatus(200)
                        .withBody("")));

        final CloudianUser user = getTestUser();
        boolean result = client.addUser(user);
        Assert.assertTrue(result);
        WireMock.verify(putRequestedFor(urlEqualTo("/user"))
                .withRequestBody(containing("userId\":\"" + user.getUserId()))
                .withHeader("Content-Type", equalTo("application/json")));
    }

    @Test
    public void addUserAccountFail() {
        wireMockRule.stubFor(WireMock.put(urlEqualTo("/user"))
                .willReturn(WireMock.aResponse()
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
        wireMockRule.stubFor(WireMock.get(urlPathMatching("/user?.*"))
                .willReturn(WireMock.aResponse()
                        .withHeader("Content-Type", "application/json")
                        .withBody("{\"userId\":\"someUser\",\"userType\":\"User\",\"fullName\":\"John Doe (jdoe)\",\"emailAddr\":\"j@doe.com\",\"address1\":null,\"address2\":null,\"city\":null,\"state\":null,\"zip\":null,\"country\":null,\"phone\":null,\"groupId\":\"someGroup\",\"website\":null,\"active\":\"true\",\"canonicalUserId\":\"b3940886468689d375ebf8747b151c37\",\"ldapEnabled\":false}")));

        final CloudianUser user = client.listUser(userId, groupId);
        Assert.assertEquals(user.getActive(), true);
        Assert.assertEquals(user.getUserId(), userId);
        Assert.assertEquals(user.getGroupId(), groupId);
        Assert.assertEquals(user.getUserType(), "User");
    }

    @Test
    public void listUserAccountFail() {
        wireMockRule.stubFor(WireMock.get(urlPathMatching("/user?.*"))
                .willReturn(WireMock.aResponse()
                        .withHeader("Content-Type", "application/json")
                        .withBody("")));

        final CloudianUser user = client.listUser("abc", "xyz");
        Assert.assertNull(user);
    }

    @Test
    public void listUserAccounts() {
        final String groupId = "someGroup";
        wireMockRule.stubFor(WireMock.get(urlPathMatching("/user/list?.*"))
                .willReturn(WireMock.aResponse()
                        .withHeader("Content-Type", "application/json")
                        .withBody("[{\"userId\":\"someUser\",\"userType\":\"User\",\"fullName\":\"John Doe (jdoe)\",\"emailAddr\":\"j@doe.com\",\"address1\":null,\"address2\":null,\"city\":null,\"state\":null,\"zip\":null,\"country\":null,\"phone\":null,\"groupId\":\"someGroup\",\"website\":null,\"active\":\"true\",\"canonicalUserId\":\"b3940886468689d375ebf8747b151c37\",\"ldapEnabled\":false}]")));

        final List<CloudianUser> users = client.listUsers(groupId);
        Assert.assertEquals(users.size(), 1);
        Assert.assertEquals(users.get(0).getActive(), true);
        Assert.assertEquals(users.get(0).getGroupId(), groupId);
    }

    @Test
    public void listUserAccountsFail() {
        wireMockRule.stubFor(WireMock.get(urlPathMatching("/user/list?.*"))
                .willReturn(WireMock.aResponse()
                        .withHeader("Content-Type", "application/json")
                        .withBody("")));

        final List<CloudianUser> users = client.listUsers("xyz");
        Assert.assertEquals(users.size(), 0);
    }

    @Test
    public void updateUserAccount() {
        wireMockRule.stubFor(WireMock.post(urlEqualTo("/user"))
                .willReturn(WireMock.aResponse()
                        .withStatus(200)
                        .withBody("")));

        final CloudianUser user = getTestUser();
        boolean result = client.updateUser(user);
        Assert.assertTrue(result);
        WireMock.verify(postRequestedFor(urlEqualTo("/user"))
                .withRequestBody(containing("userId\":\"" + user.getUserId()))
                .withHeader("Content-Type", equalTo("application/json")));
    }

    @Test
    public void updateUserAccountFail() {
        wireMockRule.stubFor(WireMock.post(urlEqualTo("/user"))
                .willReturn(WireMock.aResponse()
                        .withStatus(400)
                        .withBody("")));

        boolean result = client.updateUser(getTestUser());
        Assert.assertFalse(result);
    }

    @Test
    public void removeUserAccount() {
        wireMockRule.stubFor(WireMock.delete(urlPathMatching("/user.*"))
                .willReturn(WireMock.aResponse()
                        .withStatus(200)
                        .withBody("")));
        final CloudianUser user = getTestUser();
        boolean result = client.removeUser(user.getUserId(), user.getGroupId());
        Assert.assertTrue(result);
        WireMock.verify(deleteRequestedFor(urlPathMatching("/user.*"))
                .withQueryParam("userId", equalTo(user.getUserId())));
    }

    @Test
    public void removeUserAccountFail() {
        wireMockRule.stubFor(WireMock.delete(urlPathMatching("/user.*"))
                .willReturn(WireMock.aResponse()
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
        wireMockRule.stubFor(WireMock.put(urlEqualTo("/group"))
                .willReturn(WireMock.aResponse()
                        .withStatus(200)
                        .withBody("")));

        final CloudianGroup group = getTestGroup();
        boolean result = client.addGroup(group);
        Assert.assertTrue(result);
        WireMock.verify(putRequestedFor(urlEqualTo("/group"))
                .withRequestBody(containing("groupId\":\"someGroupId"))
                .withHeader("Content-Type", equalTo("application/json")));
    }

    @Test
    public void addGroupFail() throws Exception {
        wireMockRule.stubFor(WireMock.put(urlEqualTo("/group"))
                .willReturn(WireMock.aResponse()
                        .withStatus(400)
                        .withBody("")));

        final CloudianGroup group = getTestGroup();
        boolean result = client.addGroup(group);
        Assert.assertFalse(result);
    }

    @Test
    public void listGroup() {
        final String groupId = "someGroup";
        wireMockRule.stubFor(WireMock.get(urlPathMatching("/group.*"))
                .willReturn(WireMock.aResponse()
                        .withHeader("Content-Type", "application/json")
                        .withBody("{\"groupId\":\"someGroup\",\"groupName\":\"/someDomain\",\"ldapGroup\":null,\"active\":\"true\",\"ldapEnabled\":false,\"ldapServerURL\":null,\"ldapUserDNTemplate\":null,\"ldapSearch\":null,\"ldapSearchUserBase\":null,\"ldapMatchAttribute\":null}")));

        final CloudianGroup group = client.listGroup(groupId);
        Assert.assertEquals(group.getActive(), true);
        Assert.assertEquals(group.getGroupId(), groupId);
    }

    @Test
    public void listGroupFail() {
        wireMockRule.stubFor(WireMock.get(urlPathMatching("/group.*"))
                .willReturn(WireMock.aResponse()
                        .withHeader("Content-Type", "application/json")
                        .withBody("")));

        final CloudianGroup group = client.listGroup("xyz");
        Assert.assertNull(group);
    }

    @Test
    public void listGroups() {
        final String groupId = "someGroup";
        wireMockRule.stubFor(WireMock.get(urlEqualTo("/group/list"))
                .willReturn(WireMock.aResponse()
                        .withHeader("Content-Type", "application/json")
                        .withBody("[{\"groupId\":\"someGroup\",\"groupName\":\"/someDomain\",\"ldapGroup\":null,\"active\":\"true\",\"ldapEnabled\":false,\"ldapServerURL\":null,\"ldapUserDNTemplate\":null,\"ldapSearch\":null,\"ldapSearchUserBase\":null,\"ldapMatchAttribute\":null}]")));

        final List<CloudianGroup> groups = client.listGroups();
        Assert.assertEquals(groups.size(), 1);
        Assert.assertEquals(groups.get(0).getActive(), true);
        Assert.assertEquals(groups.get(0).getGroupId(), groupId);
    }

    @Test
    public void listGroupsFail() {
        wireMockRule.stubFor(WireMock.get(urlEqualTo("/group/list"))
                .willReturn(WireMock.aResponse()
                        .withHeader("Content-Type", "application/json")
                        .withBody("")));

        final List<CloudianGroup> groups = client.listGroups();
        Assert.assertEquals(groups.size(), 0);
    }

    @Test
    public void updateGroup() {
        wireMockRule.stubFor(WireMock.post(urlEqualTo("/group"))
                .willReturn(WireMock.aResponse()
                        .withStatus(200)
                        .withBody("")));

        final CloudianGroup group = getTestGroup();
        boolean result = client.updateGroup(group);
        Assert.assertTrue(result);
        WireMock.verify(postRequestedFor(urlEqualTo("/group"))
                .withRequestBody(containing("groupId\":\"" + group.getGroupId()))
                .withHeader("Content-Type", equalTo("application/json")));
    }

    @Test
    public void updateGroupFail() {
        wireMockRule.stubFor(WireMock.post(urlEqualTo("/group"))
                .willReturn(WireMock.aResponse()
                        .withStatus(400)
                        .withBody("")));

        boolean result = client.updateGroup(getTestGroup());
        Assert.assertFalse(result);
    }

    @Test
    public void removeGroup() {
        wireMockRule.stubFor(WireMock.delete(urlPathMatching("/group.*"))
                .willReturn(WireMock.aResponse()
                        .withStatus(200)
                        .withBody("")));
        final CloudianGroup group = getTestGroup();
        boolean result = client.removeGroup(group.getGroupId());
        Assert.assertTrue(result);
        WireMock.verify(deleteRequestedFor(urlPathMatching("/group.*"))
                .withQueryParam("groupId", equalTo(group.getGroupId())));
    }

    @Test
    public void removeGroupFail() {
        wireMockRule.stubFor(WireMock.delete(urlPathMatching("/group.*"))
                .willReturn(WireMock.aResponse()
                        .withStatus(400)
                        .withBody("")));
        final CloudianGroup group = getTestGroup();
        boolean result = client.removeGroup(group.getGroupId());
        Assert.assertFalse(result);
    }

}