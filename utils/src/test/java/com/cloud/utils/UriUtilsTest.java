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

import java.util.Arrays;
import java.util.Collections;
import java.util.List;

import org.junit.Assert;
import org.junit.Test;

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
        testGetUriInfoInternal(url, host, url);
    }

    private void testGetUriInfoInternal(String url, String host, String newUrl) {
        UriUtils.UriInfo uriInfo = UriUtils.getUriInfo(url);

        Assert.assertEquals(host, uriInfo.getStorageHost());
        Assert.assertEquals(newUrl, uriInfo.toString());
    }

    @Test
    public void testGetRbdUriInfo() {
        String host = "10.11.12.13";

        String url0 = String.format("rbd://user:password@%s:3300/pool/volume2", host);
        String url1 = String.format("rbd://user:password@%s:3300/pool", host);
        String url2 = String.format("rbd://user:password@%s/pool", host);
        String url3 = String.format("rbd://%s:3300/pool", host);
        String url4 = String.format("rbd://%s/pool", host);
        String url5 = String.format("rbd://user:password@%s", host);
        String url6 = String.format("rbd://%s:3300", host);
        String url7 = String.format("rbd://%s", host);
        String url8 = String.format("rbd://user@%s", host);
        String url9 = String.format("rbd://cloudstack:AQD+hJxklW1RGRAAA56oHGN6d+WPDLss2b05Cw==@%s:3300/cloudstack", host);
        String url10 = String.format("rbd://cloudstack:AQDlhZxkgdmiKRAA8uHt/O9jqoBp2Iwdk2MjjQ==@%s:3300/cloudstack", host);
        String url11 = String.format("rbd://cloudstack:AQD-hJxklW1RGRAAA56oHGN6d-WPDLss2b05Cw==@%s:3300/cloudstack", host);
        String url12 = String.format("rbd://cloudstack:AQDlhZxkgdmiKRAA8uHt_O9jqoBp2Iwdk2MjjQ==@%s:3300/cloudstack", host);

        testGetUriInfoInternal(url0, host);
        testGetUriInfoInternal(url1, host);
        testGetUriInfoInternal(url2, host);
        testGetUriInfoInternal(url3, host);
        testGetUriInfoInternal(url4, host);
        testGetUriInfoInternal(url5, host);
        testGetUriInfoInternal(url6, host);
        testGetUriInfoInternal(url7, host);
        testGetUriInfoInternal(url8, host);
        testGetUriInfoInternal(url9, host, url11);
        testGetUriInfoInternal(url10, host, url12);
        testGetUriInfoInternal(url11, host);
        testGetUriInfoInternal(url12, host);
    }

    @Test
    public void testGetRbdUriInfoSingleIpv6() {
        String host = "[fc00:aa:bb:cc::1]";

        String url0 = String.format("rbd://user:password@%s:3300/pool/volume2", host);
        String url1 = String.format("rbd://user:password@%s:3300/pool", host);
        String url2 = String.format("rbd://user:password@%s/pool", host);
        String url3 = String.format("rbd://%s:3300/pool", host);
        String url4 = String.format("rbd://%s/pool", host);
        String url5 = String.format("rbd://user:password@%s", host);
        String url6 = String.format("rbd://%s:3300", host);
        String url7 = String.format("rbd://%s", host);
        String url8 = String.format("rbd://user@%s", host);

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
    public void testGetRbdUriInfoMultipleIpv6() {
        String host1 = "[fc00:aa:bb:cc::1]";
        String host2 = "[fc00:aa:bb:cc::2]";
        String host3 = "[fc00:aa:bb:cc::3]";

        String url0 = String.format("rbd://user:password@%s,%s,%s:3300/pool/volume2", host1, host2, host3);
        String url1 = String.format("rbd://user:password@%s,%s,%s:3300/pool", host1, host2, host3);
        String url2 = String.format("rbd://user:password@%s,%s,%s/pool", host1, host2, host3);
        String url3 = String.format("rbd://%s,%s,%s:3300/pool", host1, host2, host3);
        String url4 = String.format("rbd://%s,%s,%s/pool", host1, host2, host3);
        String url5 = String.format("rbd://user:password@%s,%s,%s", host1, host2, host3);
        String url6 = String.format("rbd://%s,%s,%s:3300", host1, host2, host3);
        String url7 = String.format("rbd://%s,%s,%s", host1, host2, host3);
        String url8 = String.format("rbd://user@%s,%s,%s", host1, host2, host3);

        String host = String.format("%s,%s,%s", host1, host2, host3);

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
        String host = "10.11.12.13";

        String url0 = String.format("nfs://user:password@%s:3300/pool/volume2", host);
        String url1 = String.format("cifs://user:password@%s:3300/pool", host);
        String url2 = String.format("file://user:password@%s/pool", host);
        String url3 = String.format("sharedMountPoint://%s:3300/pool", host);
        String url4 = String.format("clvm://%s/pool", host);
        String url5 = String.format("PreSetup://user@%s", host);
        String url6 = String.format("DatastoreCluster://%s:3300", host);
        String url7 = String.format("iscsi://%s", host);
        String url8 = String.format("iso://user@%s:3300/pool/volume2", host);
        String url9 = String.format("vmfs://user@%s:3300/pool", host);
        String url10 = String.format("ocfs2://user@%s/pool", host);
        String url11 = String.format("gluster://%s:3300/pool", host);
        String url12 = String.format("rbd://user:password@%s:3300/pool/volume2", host);

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

    @Test
    public void testGetUriInfoIpv6() {
        String host = "[fc00:aa:bb:cc::1]";

        String url0 = String.format("nfs://user:password@%s:3300/pool/volume2", host);
        String url1 = String.format("cifs://user:password@%s:3300/pool", host);
        String url2 = String.format("file://user:password@%s/pool", host);
        String url3 = String.format("sharedMountPoint://%s:3300/pool", host);
        String url4 = String.format("clvm://%s/pool", host);
        String url5 = String.format("PreSetup://user@%s", host);
        String url6 = String.format("DatastoreCluster://%s:3300", host);
        String url7 = String.format("iscsi://%s", host);
        String url8 = String.format("iso://user@%s:3300/pool/volume2", host);
        String url9 = String.format("vmfs://user@%s:3300/pool", host);
        String url10 = String.format("ocfs2://user@%s/pool", host);
        String url11 = String.format("gluster://%s:3300/pool", host);
        String url12 = String.format("rbd://user:password@%s:3300/pool/volume2", host);

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

    @Test
    public void testIsUrlForCompressedFile() {
        Assert.assertTrue(UriUtils.isUrlForCompressedFile("https://abc.com/xyz.bz2"));
        Assert.assertTrue(UriUtils.isUrlForCompressedFile("http://abc.com/xyz.zip"));
        Assert.assertTrue(UriUtils.isUrlForCompressedFile("https://abc.com/xyz.gz"));
        Assert.assertFalse(UriUtils.isUrlForCompressedFile("http://abc.com/xyz.qcow2"));
    }

    @Test
    public void validateUrl() {
        Pair<String, Integer> url1 = UriUtils.validateUrl("https://www.cloudstack.org");
        Assert.assertEquals(url1.first(), "www.cloudstack.org");

        Pair<String, Integer> url2 = UriUtils.validateUrl("https://www.apache.org");
        Assert.assertEquals(url2.first(), "www.apache.org");
    }
}
