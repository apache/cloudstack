package com.cloudian.cloudstack;

import org.junit.After;
import org.junit.Before;
import org.junit.Test;

import com.cloudian.client.CloudianClient;

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
    }

    @Test
    public void updateUserAccount() throws Exception {
    }

    @Test
    public void removeUserAccount() throws Exception {

    }

    @Test
    public void addGroup() throws Exception {
    }

    @Test
    public void listGroup() throws Exception {
    }

    @Test
    public void updateGroup() throws Exception {
    }

    @Test
    public void removeGroup() throws Exception {
    }

}