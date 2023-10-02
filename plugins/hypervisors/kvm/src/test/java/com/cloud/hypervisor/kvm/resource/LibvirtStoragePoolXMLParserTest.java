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
import org.junit.Assert;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.junit.MockitoJUnitRunner;

@RunWith(MockitoJUnitRunner.class)
public class LibvirtStoragePoolXMLParserTest extends TestCase {

    @Test
    public void testParseNfsStoragePoolXML() {
        String poolXML = "<pool type='netfs'>\n" +
                "  <name>feff06b5-84b2-3258-b5f9-1953217295de</name>\n" +
                "  <uuid>feff06b5-84b2-3258-b5f9-1953217295de</uuid>\n" +
                "  <capacity unit='bytes'>111111111</capacity>\n" +
                "  <allocation unit='bytes'>2222222</allocation>\n" +
                "  <available unit='bytes'>3333333</available>\n" +
                "  <source>\n" +
                "    <host name='10.11.12.13'/>\n" +
                "    <dir path='/mnt/primary1'/>\n" +
                "    <format type='auto'/>\n" +
                "  </source>\n" +
                "  <target>\n" +
                "    <path>/mnt/feff06b5-84b2-3258-b5f9-1953217295de</path>\n" +
                "    <permissions>\n" +
                "      <mode>0755</mode>\n" +
                "      <owner>0</owner>\n" +
                "      <group>0</group>\n" +
                "    </permissions>\n" +
                "  </target>\n" +
                "</pool>";

        LibvirtStoragePoolXMLParser parser = new LibvirtStoragePoolXMLParser();
        LibvirtStoragePoolDef pool = parser.parseStoragePoolXML(poolXML);

        Assert.assertEquals("10.11.12.13", pool.getSourceHost());
    }

    @Test
    public void testParseRbdStoragePoolXMLWithMultipleHosts() {
        String poolXML = "<pool type='rbd'>\n" +
                "  <name>feff06b5-84b2-3258-b5f9-1953217295de</name>\n" +
                "  <uuid>feff06b5-84b2-3258-b5f9-1953217295de</uuid>\n" +
                "  <source>\n" +
                "    <name>rbdpool</name>\n" +
                "    <host name='10.11.12.13' port='6789'/>\n" +
                "    <host name='10.11.12.14' port='6789'/>\n" +
                "    <host name='10.11.12.15' port='6789'/>\n" +
                "    <format type='auto'/>\n" +
                "    <auth username='admin' type='ceph'>\n" +
                "      <secret uuid='262f743a-3726-11ed-aaee-93e90b39d5c4'/>\n" +
                "    </auth>\n" +
                "  </source>\n" +
                "</pool>";

        LibvirtStoragePoolXMLParser parser = new LibvirtStoragePoolXMLParser();
        LibvirtStoragePoolDef pool = parser.parseStoragePoolXML(poolXML);

        Assert.assertEquals(LibvirtStoragePoolDef.PoolType.RBD, pool.getPoolType());
        Assert.assertEquals(LibvirtStoragePoolDef.AuthenticationType.CEPH, pool.getAuthType());
        Assert.assertEquals("10.11.12.13,10.11.12.14,10.11.12.15", pool.getSourceHost());
        Assert.assertEquals(6789, pool.getSourcePort());
    }

    @Test
    public void testParseRbdStoragePoolXMLWithMultipleHostsIpv6() {
        String poolXML = "<pool type='rbd'>\n" +
                "  <name>feff06b5-84b2-3258-b5f9-1953217295de</name>\n" +
                "  <uuid>feff06b5-84b2-3258-b5f9-1953217295de</uuid>\n" +
                "  <source>\n" +
                "    <name>rbdpool</name>\n" +
                "    <host name='[fc00:aa:bb:cc::1]' port='6789'/>\n" +
                "    <host name='[fc00:aa:bb:cc::2]' port='6789'/>\n" +
                "    <host name='[fc00:aa:bb:cc::3]' port='6789'/>\n" +
                "    <format type='auto'/>\n" +
                "    <auth username='admin' type='ceph'>\n" +
                "      <secret uuid='262f743a-3726-11ed-aaee-93e90b39d5c4'/>\n" +
                "    </auth>\n" +
                "  </source>\n" +
                "</pool>";

        LibvirtStoragePoolXMLParser parser = new LibvirtStoragePoolXMLParser();
        LibvirtStoragePoolDef pool = parser.parseStoragePoolXML(poolXML);

        Assert.assertEquals(LibvirtStoragePoolDef.PoolType.RBD, pool.getPoolType());
        Assert.assertEquals(LibvirtStoragePoolDef.AuthenticationType.CEPH, pool.getAuthType());
        Assert.assertEquals("[fc00:aa:bb:cc::1],[fc00:aa:bb:cc::2],[fc00:aa:bb:cc::3]", pool.getSourceHost());
        Assert.assertEquals(6789, pool.getSourcePort());
    }
}
