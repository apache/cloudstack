/*
 * Licensed to the Apache Software Foundation (ASF) under one
 * or more contributor license agreements.  See the NOTICE file
 * distributed with this work for additional information
 * regarding copyright ownership.  The ASF licenses this file
 * to you under the Apache License, Version 2.0 (the
 * "License"); you may not use this file except in compliance
 * with the License.  You may obtain a copy of the License at
 *
 *   http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing,
 * software distributed under the License is distributed on an
 * "AS IS" BASIS, WITHOUT WARRANTIES OR CONDITIONS OF ANY
 * KIND, either express or implied.  See the License for the
 * specific language governing permissions and limitations
 * under the License.
 */

package com.cloud.hypervisor.kvm.resource;

import junit.framework.TestCase;
import com.cloud.hypervisor.kvm.resource.LibvirtStoragePoolDef.PoolType;
import com.cloud.hypervisor.kvm.resource.LibvirtStoragePoolDef.AuthenticationType;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.junit.MockitoJUnitRunner;

@RunWith(MockitoJUnitRunner.class)
public class LibvirtStoragePoolDefTest extends TestCase {

    @Test
    public void testSetGetStoragePool() {
        PoolType type = PoolType.NETFS;
        String name = "myNFSPool";
        String uuid = "d7846cb0-f610-4a5b-8d38-ee6e8d63f37b";
        String host = "127.0.0.1";
        String dir  = "/export/primary";
        String targetPath = "/mnt/" + uuid;
        int port = 1234;

        LibvirtStoragePoolDef pool = new LibvirtStoragePoolDef(type, name, uuid, host, port, dir, targetPath);

        assertEquals(type, pool.getPoolType());
        assertEquals(name, pool.getPoolName());
        assertEquals(host, pool.getSourceHost());
        assertEquals(port, pool.getSourcePort());
        assertEquals(dir, pool.getSourceDir());
        assertEquals(targetPath, pool.getTargetPath());
    }

    @Test
    public void testNfsStoragePool() {
        PoolType type = PoolType.NETFS;
        String name = "myNFSPool";
        String uuid = "89a605bc-d470-4637-b3df-27388be452f5";
        String host = "127.0.0.1";
        String dir  = "/export/primary";
        String targetPath = "/mnt/" + uuid;

        LibvirtStoragePoolDef pool = new LibvirtStoragePoolDef(type, name, uuid, host, dir, targetPath);

        String expectedXml = "<pool type='" + type.toString() + "'>\n<name>" + name + "</name>\n<uuid>" + uuid + "</uuid>\n" +
                             "<source>\n<host name='" + host + "'/>\n<dir path='" + dir + "'/>\n</source>\n<target>\n" +
                             "<path>" + targetPath + "</path>\n</target>\n</pool>\n";

        assertEquals(expectedXml, pool.toString());
    }

    @Test
    public void testRbdStoragePool() {
        PoolType type = PoolType.RBD;
        String name = "myRBDPool";
        String uuid = "921ef8b2-955a-4c18-a697-66bb9adf6131";
        String host = "127.0.0.1";
        String dir  = "cloudstackrbdpool";
        String authUsername = "admin";
        String secretUuid = "08c2fa02-50d0-4a78-8903-b742d3f34934";
        AuthenticationType auth = AuthenticationType.CEPH;
        int port = 6789;

        LibvirtStoragePoolDef pool = new LibvirtStoragePoolDef(type, name, uuid, host, port, dir, authUsername, auth, secretUuid);

        String expectedXml = "<pool type='" + type.toString() + "'>\n<name>" + name + "</name>\n<uuid>" + uuid + "</uuid>\n" +
                             "<source>\n<host name='" + host + "' port='" + port + "'/>\n<name>" + dir + "</name>\n" +
                             "<auth username='" + authUsername + "' type='" + auth.toString() + "'>\n<secret uuid='" + secretUuid + "'/>\n" +
                             "</auth>\n</source>\n</pool>\n";

        assertEquals(expectedXml, pool.toString());
    }

    @Test
    public void testRbdStoragePoolWithoutPort() {
        PoolType type = PoolType.RBD;
        String name = "myRBDPool";
        String uuid = "30a5fb6f-7277-44ce-9065-67e2bfdb0ebb";
        String host = "::1";
        String dir  = "rbd";
        String authUsername = "admin";
        String secretUuid = "d0d616dd-3446-409e-84d7-44465e325b35";
        AuthenticationType auth = AuthenticationType.CEPH;
        int port = 0;

        LibvirtStoragePoolDef pool = new LibvirtStoragePoolDef(type, name, uuid, host, port, dir, authUsername, auth, secretUuid);

        String expectedXml = "<pool type='" + type.toString() + "'>\n<name>" + name + "</name>\n<uuid>" + uuid + "</uuid>\n" +
                             "<source>\n<host name='" + host + "'/>\n<name>" + dir + "</name>\n" +
                             "<auth username='" + authUsername + "' type='" + auth.toString() + "'>\n<secret uuid='" + secretUuid + "'/>\n" +
                             "</auth>\n</source>\n</pool>\n";

        assertEquals(expectedXml, pool.toString());
    }

    @Test
    public void testRbdStoragePoolWithMultipleHostsIpv6() {
        PoolType type = PoolType.RBD;
        String name = "myRBDPool";
        String uuid = "1583a25a-b192-436c-93e6-0ef60b198a32";
        String host = "[fc00:1234::1],[fc00:1234::2],[fc00:1234::3]";
        int port = 3300;
        String authUsername = "admin";
        AuthenticationType auth = AuthenticationType.CEPH;
        String dir  = "rbd";
        String secretUuid = "28909c4f-314e-4db7-a6b3-5eccd9dcf973";

        LibvirtStoragePoolDef pool = new LibvirtStoragePoolDef(type, name, uuid, host, port, dir, authUsername, auth, secretUuid);

        String expected = "<pool type='rbd'>\n" +
                "<name>myRBDPool</name>\n" +
                "<uuid>1583a25a-b192-436c-93e6-0ef60b198a32</uuid>\n" +
                "<source>\n" +
                "<host name='[fc00:1234::1]' port='3300'/>\n" +
                "<host name='[fc00:1234::2]' port='3300'/>\n" +
                "<host name='[fc00:1234::3]' port='3300'/>\n" +
                "<name>rbd</name>\n" +
                "<auth username='admin' type='ceph'>\n" +
                "<secret uuid='28909c4f-314e-4db7-a6b3-5eccd9dcf973'/>\n" +
                "</auth>\n" +
                "</source>\n" +
                "</pool>\n";

        assertEquals(expected, pool.toString());
    }
}
