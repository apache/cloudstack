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

public class LibvirtVMDefTest extends TestCase {

    public void testInterfaceEtehrnet() {
        LibvirtVMDef.InterfaceDef ifDef = new LibvirtVMDef.InterfaceDef();
        ifDef.defEthernet("targetDeviceName", "00:11:22:aa:bb:dd", LibvirtVMDef.InterfaceDef.nicModel.VIRTIO);

        String expected = "<interface type='ethernet'>\n" +
                "<target dev='targetDeviceName'/>\n" +
                "<mac address='00:11:22:aa:bb:dd'/>\n" +
                "<model type='virtio'/>\n" +
                "</interface>\n";

        assertEquals(expected, ifDef.toString());
    }

    public void testInterfaceDirectNet() {
        LibvirtVMDef.InterfaceDef ifDef = new LibvirtVMDef.InterfaceDef();
        ifDef.defDirectNet("targetDeviceName", null, "00:11:22:aa:bb:dd", LibvirtVMDef.InterfaceDef.nicModel.VIRTIO, "private");

        String expected = "<interface type='" + LibvirtVMDef.InterfaceDef.guestNetType.DIRECT + "'>\n" +
                "<source dev='targetDeviceName' mode='private'/>\n" +
                "<mac address='00:11:22:aa:bb:dd'/>\n" +
                "<model type='virtio'/>\n" +
                "</interface>\n";

        assertEquals(expected, ifDef.toString());
    }

}
