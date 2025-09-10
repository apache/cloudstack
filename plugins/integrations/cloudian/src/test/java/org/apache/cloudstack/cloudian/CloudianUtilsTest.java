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

import java.io.UnsupportedEncodingException;
import java.net.MalformedURLException;
import java.net.URL;
import java.net.URLDecoder;
import java.util.HashMap;
import org.apache.cloudstack.cloudian.client.CloudianUtils;
import org.junit.Assert;
import org.junit.Test;

public class CloudianUtilsTest {

    @Test
    public void testGenerateSSOUrl() {
        final String cmcUrlPath = "https://cmc.cloudian.com:8443/Cloudian/";
        final String user = "abc-def-ghi";
        final String group = "uvw-xyz";
        final String ssoKey = "randomkey";

        // test expectations
        final String expPath = "/Cloudian/ssosecurelogin.htm";
        HashMap<String, String> expected = new HashMap();
        expected.put("user", user);
        expected.put("group", group);
        expected.put("timestamp", null); // null value will not be checked by this test
        expected.put("signature", null); // null value will not be checked by this test
        expected.put("redirect", "/");

        // Generated URL will be something like this
        // https://cmc.cloudian.com:8443/Cloudian/ssosecurelogin.htm?user=abc-def-ghi&group=uvw-xyz&timestamp=1725937474949&signature=Wu1hjafeyE82mGwd1MIwrp5hPt4%3D&redirect=/
        String output = CloudianUtils.generateSSOUrl(cmcUrlPath, user, group, ssoKey);
        Assert.assertNotNull(output);

        // Check main parts of the output URL
        URL url = null;
        try {
            url = new URL(output);
        } catch (MalformedURLException e) {
            Assert.fail("failed to parse URL: " + output);
        }
        String path = url.getPath();
        Assert.assertEquals(expPath, path);

        // No easy way to check Query parameters in Java still?
        // Just do a rudementary check as we are in charge of the URL
        String query = url.getQuery();
        String[] nameValues = query.split("&");
        int matchedCount = 0;
        for(String nameValue : nameValues) {
            String[] nameValuePair = nameValue.split("=", 2);
            Assert.assertEquals(nameValue, 2, nameValuePair.length);
            String name = null;
            String value = null;
            try {
                name = URLDecoder.decode(nameValuePair[0], "UTF-8");
                value = URLDecoder.decode(nameValuePair[1], "UTF-8");
            } catch (UnsupportedEncodingException e) {
                Assert.fail("not expecting UTF-8 to fail");
            }
            Assert.assertTrue(expected.containsKey(name));
            matchedCount++;
            String expValue = expected.get(name);
            if (expValue != null) {
                Assert.assertEquals("Parameter " + name, expValue, value);
            }
        }
        Assert.assertEquals("Should be 5 query parameters", 5, matchedCount);
    }
}
