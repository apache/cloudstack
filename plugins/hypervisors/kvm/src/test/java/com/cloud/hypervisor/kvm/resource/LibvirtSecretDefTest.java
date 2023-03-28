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
import com.cloud.hypervisor.kvm.resource.LibvirtSecretDef.Usage;

public class LibvirtSecretDefTest extends TestCase {

    public void testVolumeSecretDef() {
        String uuid = "db66f42b-a79e-4666-9910-9dfc8a024427";
        String name = "myEncryptedQCOW2";
        Usage use = Usage.VOLUME;

        LibvirtSecretDef def = new LibvirtSecretDef(use, uuid);
        def.setVolumeVolume(name);

        String expectedXml = "<secret ephemeral='no' private='no'>\n<uuid>" + uuid + "</uuid>\n" +
                             "<usage type='" + use.toString() + "'>\n<volume>" + name + "</volume>\n</usage>\n</secret>\n";

        assertEquals(expectedXml, def.toString());
    }

    public void testCephSecretDef() {
        String uuid = "a9febe83-ac5c-467a-bf19-eb75325ec23c";
        String name = "admin";
        Usage use = Usage.CEPH;

        LibvirtSecretDef def = new LibvirtSecretDef(use, uuid);
        def.setCephName(name);

        String expectedXml = "<secret ephemeral='no' private='no'>\n<uuid>" + uuid + "</uuid>\n" +
                             "<usage type='" + use.toString() + "'>\n<name>" + name + "</name>\n</usage>\n</secret>\n";

        assertEquals(expectedXml, def.toString());
    }

}
