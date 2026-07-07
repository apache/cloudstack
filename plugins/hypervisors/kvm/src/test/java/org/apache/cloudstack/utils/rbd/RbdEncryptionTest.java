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
package org.apache.cloudstack.utils.rbd;

import com.cloud.utils.exception.CloudRuntimeException;
import com.cloud.utils.script.Script;
import org.apache.cloudstack.utils.cryptsetup.CryptSetup;
import org.junit.Assert;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.junit.MockitoJUnitRunner;

import java.nio.charset.StandardCharsets;

/**
 * Unit tests for {@link RbdEncryption}. These assert the {@code rbd}/{@code qemu-img} argv that
 * would be handed to {@link Script}, so the command construction is verified without a live Ceph
 * cluster (the actual execution needs a real cluster and is covered by end-to-end testing).
 */
@RunWith(MockitoJUnitRunner.class)
public class RbdEncryptionTest {

    private final RbdEncryption rbdEncryption = new RbdEncryption();

    @Test
    public void buildFormatScriptWithCephxAuth() {
        Script script = rbdEncryption.buildFormatScript("cloudstack/img", CryptSetup.LuksType.LUKS2,
                "/tmp/pass", "1.2.3.4:6789", "cloudstack", "/tmp/key");
        String cmd = script.toString();
        Assert.assertTrue(cmd, cmd.contains("rbd encryption format cloudstack/img luks2 /tmp/pass"));
        Assert.assertTrue(cmd, cmd.contains("--mon-host 1.2.3.4:6789"));
        Assert.assertTrue(cmd, cmd.contains("--id cloudstack"));
        Assert.assertTrue(cmd, cmd.contains("--keyfile /tmp/key"));
    }

    @Test
    public void buildFormatScriptWithoutCephxAuth() {
        // authUser == null and cephKeyFilePath == null (e.g. auth-less cluster): no --id / --keyfile.
        Script script = rbdEncryption.buildFormatScript("pool/vol", CryptSetup.LuksType.LUKS2,
                "/tmp/pass", "mon:6789", null, null);
        String cmd = script.toString();
        Assert.assertTrue(cmd, cmd.contains("rbd encryption format pool/vol luks2 /tmp/pass --mon-host mon:6789"));
        Assert.assertFalse(cmd, cmd.contains("--id"));
        Assert.assertFalse(cmd, cmd.contains("--keyfile"));
    }

    @Test
    public void buildResizeScriptGrowDoesNotAllowShrink() {
        Script script = rbdEncryption.buildResizeScript("cloudstack/img", 10240L, "/tmp/pass", false,
                "1.2.3.4:6789", "cloudstack", "/tmp/key");
        String cmd = script.toString();
        Assert.assertTrue(cmd, cmd.contains("rbd resize --size 10240 cloudstack/img --encryption-passphrase-file /tmp/pass"));
        Assert.assertTrue(cmd, cmd.contains("--id cloudstack"));
        Assert.assertFalse(cmd, cmd.contains("--allow-shrink"));
    }

    @Test
    public void buildResizeScriptShrinkPassesAllowShrink() {
        Script script = rbdEncryption.buildResizeScript("cloudstack/img", 5120L, "/tmp/pass", true,
                "1.2.3.4:6789", "cloudstack", "/tmp/key");
        Assert.assertTrue(script.toString(), script.toString().contains("--allow-shrink"));
    }

    @Test
    public void buildConvertScriptFromRbdSource() {
        Script q = rbdEncryption.buildConvertScript("srcpool", "srcimg", null, null,
                "cloudstack", "dst", "/tmp/conf", "cloudstack", "/tmp/pass", CryptSetup.LuksType.LUKS2);
        String cmd = q.toString();
        Assert.assertTrue(cmd, cmd.contains("qemu-img convert -n"));
        Assert.assertTrue(cmd, cmd.contains("--image-opts driver=rbd,pool=srcpool,image=srcimg,conf=/tmp/conf,user=cloudstack"));
        Assert.assertTrue(cmd, cmd.contains("--object secret,id=luks0,file=/tmp/pass"));
        Assert.assertTrue(cmd, cmd.contains("--target-image-opts driver=rbd,pool=cloudstack,image=dst,conf=/tmp/conf,user=cloudstack,encrypt.format=luks2,encrypt.key-secret=luks0"));
    }

    @Test
    public void buildConvertScriptFromFileSource() {
        Script q = rbdEncryption.buildConvertScript(null, null, "/tmp/tmpl.qcow2", "QCOW2",
                "cloudstack", "dst", "/tmp/conf", "cloudstack", "/tmp/pass", CryptSetup.LuksType.LUKS2);
        String cmd = q.toString();
        Assert.assertTrue(cmd, cmd.contains("-f qcow2 /tmp/tmpl.qcow2"));
        Assert.assertFalse(cmd, cmd.contains("--image-opts"));
        Assert.assertTrue(cmd, cmd.contains("encrypt.format=luks2,encrypt.key-secret=luks0"));
    }

    @Test
    public void formatRejectsEmptyPassphrase() {
        Assert.assertThrows(CloudRuntimeException.class, () -> rbdEncryption.format(
                "1.2.3.4", 6789, "cloudstack", "secret", "cloudstack", "img",
                new byte[0], CryptSetup.LuksType.LUKS2));
    }

    @Test
    public void resizeRejectsNullPassphrase() {
        Assert.assertThrows(CloudRuntimeException.class, () -> rbdEncryption.resize(
                "1.2.3.4", 6789, "cloudstack", "secret", "cloudstack", "img",
                1L << 30, false, null));
    }

    @Test
    public void importTemplateRejectsEmptyPassphrase() {
        Assert.assertThrows(CloudRuntimeException.class, () -> rbdEncryption.importTemplate(
                "srcpool", "srcimg", null, null, "1.2.3.4", 6789, "cloudstack", "secret",
                "cloudstack", "dst", "".getBytes(StandardCharsets.UTF_8), CryptSetup.LuksType.LUKS2));
    }
}
