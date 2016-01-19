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
package com.cloud.hypervisor.kvm.resource.wrapper;

import java.io.File;
import java.util.UUID;

import com.cloud.utils.script.Script;

import junit.framework.TestCase;

public class LibvirtUtilitiesHelperTest extends TestCase {

    public void testGenerateUUID() {
        LibvirtUtilitiesHelper helper = new LibvirtUtilitiesHelper();
        UUID uuid = UUID.fromString(helper.generateUUIDName());
        assertEquals(4, uuid.version());
    }

    public void testSSHKeyPaths() {
        LibvirtUtilitiesHelper helper = new LibvirtUtilitiesHelper();
        /* These paths are hardcoded in LibvirtComputingResource and we should
         * verify that they do not change.
         * Hardcoded paths are not what we want in the longer run
         */
        assertEquals("/root/.ssh", helper.retrieveSshKeysPath());
        assertEquals("/root/.ssh" + File.separator + "id_rsa.pub.cloud", helper.retrieveSshPubKeyPath());
        assertEquals("/root/.ssh" + File.separator + "id_rsa.cloud", helper.retrieveSshPrvKeyPath());
    }

    public void testBashScriptPath() {
        LibvirtUtilitiesHelper helper = new LibvirtUtilitiesHelper();
        assertEquals("/bin/bash", helper.retrieveBashScriptPath());
    }

    public void testBuildScript() {
        LibvirtUtilitiesHelper helper = new LibvirtUtilitiesHelper();
        String path = "/path/to/my/script";
        Script script = helper.buildScript(path);
        assertEquals(path + " ", script.toString());
        assertEquals(LibvirtUtilitiesHelper.TIMEOUT, script.getTimeout());
    }
}
