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

import org.junit.Assert;
import org.junit.Test;

import java.util.Arrays;
import java.util.Collections;
import java.util.List;

public class UriUtilsTest {
    @Test
    public void encodeURIComponent() {
        Assert.assertEquals("http://localhost",
                UriUtils.encodeURIComponent("http://localhost"));
        Assert.assertEquals("http://localhost/",
                UriUtils.encodeURIComponent("http://localhost/"));
        Assert.assertEquals("http://localhost/foo/bar",
                UriUtils.encodeURIComponent("http://localhost/foo/bar"));
    }

    @Test
    public void getUpdateUri() {
        // no password param, no request for encryption
        Assert.assertEquals("http://localhost/foo/bar?param=true", UriUtils
                .getUpdateUri("http://localhost/foo/bar?param=true", false));
        // there is password param but still no request for encryption, should
        // be unchanged
        Assert.assertEquals("http://localhost/foo/bar?password=1234", UriUtils
                .getUpdateUri("http://localhost/foo/bar?password=1234", false));
        // if there is password param and encryption is requested then it may or
        // may not be changed depending on how the EncrytionUtils is setup, but
        // at least it needs to start with the same url
        Assert.assertTrue(UriUtils.getUpdateUri(
                "http://localhost/foo/bar?password=1234", true).startsWith(
                "http://localhost/foo/bar"));

        //just to see if it is still ok with multiple parameters
        Assert.assertEquals("http://localhost/foo/bar?param1=true&param2=12345", UriUtils
                .getUpdateUri("http://localhost/foo/bar?param1=true&param2=12345", false));

        //XXX: Interesting cases not covered:
        // * port is ignored and left out from the return value
    }

    @Test
    public void expandVlanEmpty() {
        List<Integer> vlans = UriUtils.expandVlanUri("");
        Assert.assertTrue(vlans.size() == 0);
    }

    @Test
    public void expandVlanSingleValue() {
        List<Integer> vlans = UriUtils.expandVlanUri("10");
        Assert.assertTrue(vlans.size() == 1);
        Assert.assertEquals(vlans, Collections.singletonList(10));
    }

    @Test
    public void expandVlanValidRange() {
        List<Integer> vlans = UriUtils.expandVlanUri("10-12,14,17,40-43");
        Assert.assertEquals(vlans, Arrays.asList(10,11,12,14,17,40,41,42,43));
    }

    @Test
    public void expandVlanInvalidRange() {
        List<Integer> vlans = UriUtils.expandVlanUri("10-,12-14,-4,5-2");
        Assert.assertEquals(vlans, Arrays.asList(10,12,13,14));
    }

    @Test
    public void testVlanUriOverlap() {
        Assert.assertTrue(UriUtils.checkVlanUriOverlap("10-30,45,50,12,31", "10"));
        Assert.assertTrue(UriUtils.checkVlanUriOverlap("10-30,45,50,12,31", "32,33-44,30-31"));
        Assert.assertTrue(UriUtils.checkVlanUriOverlap("10-30", "25-35"));
    }

    @Test
    public void testVlanUriNoOverlap() {
        Assert.assertFalse(UriUtils.checkVlanUriOverlap("10-30,45,50,12,31", null));
        Assert.assertFalse(UriUtils.checkVlanUriOverlap("10-30,45,50,12,31", ""));
        Assert.assertFalse(UriUtils.checkVlanUriOverlap("10-30,45,50,12,31", "32"));
        Assert.assertFalse(UriUtils.checkVlanUriOverlap("10,22,111", "12"));
        Assert.assertFalse(UriUtils.checkVlanUriOverlap("100-200", "30-40,50,201-250"));
    }

    private void testGetUriInfoInternal(String url, String host) {
        UriUtils.UriInfo uriInfo = UriUtils.getUriInfo(url);

        Assert.assertEquals(host, uriInfo.getStorageHost());
        Assert.assertEquals(url, uriInfo.toString());
    }

    @Test
    public void testGetRbdUriInfo() {
        String url0 = "rbd://user:password@host1,host2,host3:3300/pool/volume2";
        String url1 = "rbd://user:password@host1,host2,host3:3300/pool";
        String url2 = "rbd://user:password@host1,host2,host3/pool";
        String url3 = "rbd://host1,host2,host3:3300/pool";
        String url4 = "rbd://host1,host2,host3/pool";
        String url5 = "rbd://user:password@host1,host2,host3";
        String url6 = "rbd://host1,host2,host3:3300";
        String url7 = "rbd://host1,host2,host3";
        String url8 = "rbd://user@host1,host2,host3";

        String host = "host1,host2,host3";

        testGetUriInfoInternal(url0, host);
        testGetUriInfoInternal(url1, host);
        testGetUriInfoInternal(url2, host);
        testGetUriInfoInternal(url3, host);
        testGetUriInfoInternal(url4, host);
        testGetUriInfoInternal(url5, host);
        testGetUriInfoInternal(url6, host);
        testGetUriInfoInternal(url7, host);
        testGetUriInfoInternal(url8, host);
    }

    @Test
    public void testGetUriInfo() {
        String url0 = "nfs://user:password@host:3300/pool/volume2";
        String url1 = "cifs://user:password@host:3300/pool";
        String url2 = "file://user:password@host/pool";
        String url3 = "sharedMountPoint://host:3300/pool";
        String url4 = "clvm://host/pool";
        String url5 = "PreSetup://user@host";
        String url6 = "DatastoreCluster://host:3300";
        String url7 = "iscsi://host";
        String url8 = "iso://user@host:3300/pool/volume2";
        String url9 = "vmfs://user@host:3300/pool";
        String url10 = "ocfs2://user@host/pool";
        String url11 = "gluster://host:3300/pool";
        String url12 = "rbd://user:password@host:3300/pool/volume2";

        String host = "host";

        testGetUriInfoInternal(url0, host);
        testGetUriInfoInternal(url1, host);
        testGetUriInfoInternal(url2, host);
        testGetUriInfoInternal(url3, host);
        testGetUriInfoInternal(url4, host);
        testGetUriInfoInternal(url5, host);
        testGetUriInfoInternal(url6, host);
        testGetUriInfoInternal(url7, host);
        testGetUriInfoInternal(url8, host);
        testGetUriInfoInternal(url9, host);
        testGetUriInfoInternal(url10, host);
        testGetUriInfoInternal(url11, host);
        testGetUriInfoInternal(url12, host);
    }
}
