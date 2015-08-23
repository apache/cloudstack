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
package org.apache.cloudstack.utils.imagestore;

import java.net.MalformedURLException;
import java.net.URL;
import java.util.UUID;

import org.junit.Assert;
import org.junit.Test;

public class ImageStoreUtilTest {

    @Test
    public void testgeneratePostUploadUrl() throws MalformedURLException {
        String ssvmdomain = "*.realhostip.com";
        String ipAddress = "10.147.28.14";
        String uuid = UUID.randomUUID().toString();

        //ssvm domain is not set
        String url = ImageStoreUtil.generatePostUploadUrl(null, ipAddress, uuid);
        assertPostUploadUrl(url, ipAddress, uuid);

        //ssvm domain is set to empty value
        url = ImageStoreUtil.generatePostUploadUrl("", ipAddress, uuid);
        assertPostUploadUrl(url, ipAddress, uuid);

        //ssvm domain is set to a valid value
        url = ImageStoreUtil.generatePostUploadUrl(ssvmdomain, ipAddress, uuid);
        assertPostUploadUrl(url, ipAddress.replace(".", "-") + ssvmdomain.substring(1), uuid);
    }

    private void assertPostUploadUrl(String urlStr, String domain, String uuid) throws MalformedURLException {
        URL url = new URL(urlStr);
        Assert.assertNotNull(url);
        Assert.assertEquals(url.getHost(), domain);
        Assert.assertEquals(url.getPath(), "/upload/" + uuid);
    }

}
