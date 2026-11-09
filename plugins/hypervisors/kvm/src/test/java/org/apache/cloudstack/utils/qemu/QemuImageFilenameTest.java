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

package org.apache.cloudstack.utils.qemu;

import org.junit.Assert;
import org.junit.Test;

public class QemuImageFilenameTest {

    @Test
    public void plainPathIsReturnedAsIs() {
        Assert.assertEquals("/p/vda.qcow2", QemuImageFilename.plain("/p/vda.qcow2"));
        Assert.assertNull(QemuImageFilename.plain(null));
        Assert.assertEquals("", QemuImageFilename.plain(""));
    }

    @Test
    public void encryptedImagePseudoFilenameIsUnwrapped() {
        // As qemu 7.2 reports a LUKS qcow2 opened by libvirt.
        String json = "json:{\"encrypt.key-secret\": \"libvirt-2-format-encryption-secret0\", \"driver\": \"qcow2\","
                + " \"file\": {\"driver\": \"file\", \"filename\": \"/mnt/pool/c2a9aa03.qcow2\"}}";
        Assert.assertEquals("/mnt/pool/c2a9aa03.qcow2", QemuImageFilename.plain(json));
    }

    @Test
    public void nestedProtocolLayersAreWalkedToTheInnermostFilename() {
        String json = "json:{\"driver\": \"qcow2\", \"file\": {\"driver\": \"raw\", \"file\": {\"driver\": \"file\", \"filename\": \"/p/inner\"}}}";
        Assert.assertEquals("/p/inner", QemuImageFilename.plain(json));
    }

    @Test
    public void unparseableOrFilenameLessJsonIsReturnedAsIs() {
        Assert.assertEquals("json:{not json", QemuImageFilename.plain("json:{not json"));
        Assert.assertEquals("json:{\"driver\": \"nbd\", \"server\": {\"type\": \"unix\"}}", QemuImageFilename.plain("json:{\"driver\": \"nbd\", \"server\": {\"type\": \"unix\"}}"));
    }
}
