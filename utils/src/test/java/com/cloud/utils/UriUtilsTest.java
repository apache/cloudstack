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
        Assert.assertTrue(UriUtils.isUrlForCompressedFile("http://abc.com/xyz.xz"));
        Assert.assertFalse(UriUtils.isUrlForCompressedFile("http://abc.com/xyz.qcow2"));
    }

    @Test
    public void validateUrl() {
        Pair<String, Integer> url1 = UriUtils.validateUrl("https://cloudstack.apache.org/");
        Assert.assertEquals(url1.first(), "cloudstack.apache.org");

        Pair<String, Integer> url2 = UriUtils.validateUrl("https://www.apache.org");
        Assert.assertEquals(url2.first(), "www.apache.org");
    }

    @Test
    public void testGetClvmUriInfoBasic() {
        String host = "10.11.12.13";

        String url1 = String.format("clvm://%s/vg0/lv0", host);
        String url2 = String.format("clvm://%s/vg0", host);
        String url3 = String.format("clvm://%s", host);

        UriUtils.UriInfo info1 = UriUtils.getUriInfo(url1);
        Assert.assertEquals("clvm", info1.getScheme());
        Assert.assertEquals(host, info1.getStorageHost());
        Assert.assertEquals("/vg0/lv0", info1.getStoragePath());
        Assert.assertNull(info1.getUserInfo());
        Assert.assertEquals(-1, info1.getPort());
        Assert.assertEquals(url1, info1.toString());

        UriUtils.UriInfo info2 = UriUtils.getUriInfo(url2);
        Assert.assertEquals("clvm", info2.getScheme());
        Assert.assertEquals(host, info2.getStorageHost());
        Assert.assertEquals("/vg0", info2.getStoragePath());

        UriUtils.UriInfo info3 = UriUtils.getUriInfo(url3);
        Assert.assertEquals("clvm", info3.getScheme());
        Assert.assertEquals(host, info3.getStorageHost());
        Assert.assertEquals("/", info3.getStoragePath());
    }

    @Test
    public void testGetClvmNgUriInfoBasic() {
        String host = "10.11.12.13";

        String url1 = String.format("clvm_ng://%s/vg0/lv0", host);
        String url2 = String.format("clvm_ng://%s/vg0", host);
        String url3 = String.format("clvm_ng://%s", host);

        UriUtils.UriInfo info1 = UriUtils.getUriInfo(url1);
        Assert.assertEquals("clvm_ng", info1.getScheme());
        Assert.assertEquals(host, info1.getStorageHost());
        Assert.assertEquals("/vg0/lv0", info1.getStoragePath());
        Assert.assertNull(info1.getUserInfo());
        Assert.assertEquals(-1, info1.getPort());
        Assert.assertEquals(url1, info1.toString());

        UriUtils.UriInfo info2 = UriUtils.getUriInfo(url2);
        Assert.assertEquals("clvm_ng", info2.getScheme());
        Assert.assertEquals(host, info2.getStorageHost());
        Assert.assertEquals("/vg0", info2.getStoragePath());

        UriUtils.UriInfo info3 = UriUtils.getUriInfo(url3);
        Assert.assertEquals("clvm_ng", info3.getScheme());
        Assert.assertEquals(host, info3.getStorageHost());
        Assert.assertEquals("/", info3.getStoragePath());
    }

    @Test
    public void testGetClvmUriInfoNoHost() {
        String url1 = "clvm:///vg0/lv0";
        String url2 = "clvm_ng:///vg0/lv0";

        UriUtils.UriInfo info1 = UriUtils.getUriInfo(url1);
        Assert.assertEquals("clvm", info1.getScheme());
        Assert.assertEquals("localhost", info1.getStorageHost());
        Assert.assertEquals("/vg0/lv0", info1.getStoragePath());

        UriUtils.UriInfo info2 = UriUtils.getUriInfo(url2);
        Assert.assertEquals("clvm_ng", info2.getScheme());
        Assert.assertEquals("localhost", info2.getStorageHost());
        Assert.assertEquals("/vg0/lv0", info2.getStoragePath());
    }

    @Test
    public void testGetClvmUriInfoMultipleSlashes() {
        String url1 = "clvm://host1//vg0//lv0";
        String url2 = "clvm_ng://host2///vg1///lv1";

        UriUtils.UriInfo info1 = UriUtils.getUriInfo(url1);
        Assert.assertEquals("clvm", info1.getScheme());
        Assert.assertEquals("host1", info1.getStorageHost());
        Assert.assertEquals("/vg0//lv0", info1.getStoragePath());

        UriUtils.UriInfo info2 = UriUtils.getUriInfo(url2);
        Assert.assertEquals("clvm_ng", info2.getScheme());
        Assert.assertEquals("host2", info2.getStorageHost());
        Assert.assertEquals("/vg1///lv1", info2.getStoragePath());
    }

    @Test
    public void testGetClvmUriInfoComplexPaths() {
        String host = "storage-node1";
        String url1 = String.format("clvm://%s/vg-name-with-dashes/lv_name_with_underscores", host);
        String url2 = String.format("clvm_ng://%s/vg.name.with.dots/lv-123-456", host);

        UriUtils.UriInfo info1 = UriUtils.getUriInfo(url1);
        Assert.assertEquals("clvm", info1.getScheme());
        Assert.assertEquals(host, info1.getStorageHost());
        Assert.assertEquals("/vg-name-with-dashes/lv_name_with_underscores", info1.getStoragePath());

        UriUtils.UriInfo info2 = UriUtils.getUriInfo(url2);
        Assert.assertEquals("clvm_ng", info2.getScheme());
        Assert.assertEquals(host, info2.getStorageHost());
        Assert.assertEquals("/vg.name.with.dots/lv-123-456", info2.getStoragePath());
    }

    @Test
    public void testGetClvmUriInfoHostnames() {
        String[] hosts = {
            "localhost",
            "node1",
            "storage-node-1",
            "storage.example.com",
            "10.0.0.1",
            "192.168.1.100"
        };

        for (String host : hosts) {
            String clvmUrl = String.format("clvm://%s/vg0/lv0", host);
            String clvmNgUrl = String.format("clvm_ng://%s/vg0/lv0", host);

            UriUtils.UriInfo clvmInfo = UriUtils.getUriInfo(clvmUrl);
            Assert.assertEquals("clvm", clvmInfo.getScheme());
            Assert.assertEquals(host, clvmInfo.getStorageHost());
            Assert.assertEquals("/vg0/lv0", clvmInfo.getStoragePath());

            UriUtils.UriInfo clvmNgInfo = UriUtils.getUriInfo(clvmNgUrl);
            Assert.assertEquals("clvm_ng", clvmNgInfo.getScheme());
            Assert.assertEquals(host, clvmNgInfo.getStorageHost());
            Assert.assertEquals("/vg0/lv0", clvmNgInfo.getStoragePath());
        }
    }

    @Test
    public void testGetClvmUriInfoToString() {
        String url1 = "clvm://host1/vg0/lv0";
        String url2 = "clvm_ng://host2/vg1/lv1";
        String url3 = "clvm://localhost/vg0";

        Assert.assertEquals(url1, UriUtils.getUriInfo(url1).toString());
        Assert.assertEquals(url2, UriUtils.getUriInfo(url2).toString());
        Assert.assertEquals(url3, UriUtils.getUriInfo(url3).toString());
    }

    @Test
    public void testGetClvmUriInfoCaseInsensitive() {
        String url1 = "CLVM://host1/vg0/lv0";
        String url2 = "ClVm://host2/vg1/lv1";
        String url3 = "CLVM_NG://host3/vg2/lv2";
        String url4 = "clvm_NG://host4/vg3/lv3";

        UriUtils.UriInfo info1 = UriUtils.getUriInfo(url1);
        Assert.assertEquals("clvm", info1.getScheme());
        Assert.assertEquals("host1", info1.getStorageHost());

        UriUtils.UriInfo info2 = UriUtils.getUriInfo(url2);
        Assert.assertEquals("clvm", info2.getScheme());
        Assert.assertEquals("host2", info2.getStorageHost());

        UriUtils.UriInfo info3 = UriUtils.getUriInfo(url3);
        Assert.assertEquals("clvm_ng", info3.getScheme());
        Assert.assertEquals("host3", info3.getStorageHost());

        UriUtils.UriInfo info4 = UriUtils.getUriInfo(url4);
        Assert.assertEquals("clvm_ng", info4.getScheme());
        Assert.assertEquals("host4", info4.getStorageHost());
    }

    @Test
    public void testGetClvmUriInfoIntegration() {
        String host = "clvm-host";

        String clvmUrl = String.format("clvm://%s/vg0/lv0", host);
        String clvmNgUrl = String.format("clvm_ng://%s/vg1/lv1", host);

        testGetUriInfoInternal(clvmUrl, host);
        testGetUriInfoInternal(clvmNgUrl, host);
    }
}
