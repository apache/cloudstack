package com.cloudian.cloudstack;

import java.util.List;

import org.junit.After;
import org.junit.Before;
import org.junit.Test;

import com.cloudian.client.GroupInfo;
import com.cloudian.client.UserInfo;

public class CloudianClientTest {

    private CloudianClient client;

    @Before
    public void setUp() throws Exception {
        client = new CloudianClient("https://admin.hs.yadav.xyz:19443", "admin", "public", false);
    }

    @After
    public void tearDown() throws Exception {
    }

    @Test
    public void addUserAccount() throws Exception {
    }

    @Test
    public void listUserAccount() throws Exception {
        List<UserInfo> users = client.listUsers("0");
    }

    @Test
    public void updateUserAccount() throws Exception {
    }

    @Test
    public void removeUserAccount() throws Exception {
        for (UserInfo user : client.listUsers("2ddabedc-4733-4cdf-80b1-abbd9d027005")) {
            boolean result = client.removeUser(user.getUserId(), user.getGroupId());
        }
    }

    @Test
    public void addGroup() throws Exception {
    }

    @Test
    public void listGroup() throws Exception {
        List<GroupInfo> groups = client.listGroups();
    }

    @Test
    public void updateGroup() throws Exception {
    }

    @Test
    public void removeGroup() throws Exception {
        boolean result = client.removeGroup("2ddabedc-4733-4cdf-80b1-abbd9d027005");
    }

}