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
import org.apache.cloudstack.utils.qemu.QemuImageOptions;
import org.apache.cloudstack.utils.qemu.QemuImg;
import org.apache.cloudstack.utils.qemu.QemuImgFile;
import org.apache.cloudstack.utils.qemu.QemuObject;
import org.junit.Assert;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Mockito;
import org.mockito.junit.MockitoJUnitRunner;

import java.nio.charset.StandardCharsets;
import java.util.List;
import java.util.stream.Collectors;

/**
 * Unit tests for {@link RbdEncryption}. These assert the {@code rbd} argv that would be handed to
 * {@link Script} and the image options handed to {@link QemuImg}, so the command construction is
 * verified without a live Ceph cluster (the actual execution needs a real cluster and is covered
 * by end-to-end testing).
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
    public void importTemplateFromRbdSourceConvertsThroughQemuImg() throws Exception {
        RbdEncryption spy = Mockito.spy(new RbdEncryption());
        QemuImg qemuImg = Mockito.mock(QemuImg.class);
        Mockito.doReturn(qemuImg).when(spy).createQemuImg();

        spy.importTemplate("srcpool", "srcimg", null, null, "1.2.3.4", 6789, "cloudstack", "secret",
                "cloudstack", "dst", "passphrase".getBytes(StandardCharsets.UTF_8), CryptSetup.LuksType.LUKS2);

        ArgumentCaptor<QemuImageOptions> srcOpts = ArgumentCaptor.forClass(QemuImageOptions.class);
        ArgumentCaptor<QemuImageOptions> destOpts = ArgumentCaptor.forClass(QemuImageOptions.class);
        ArgumentCaptor<List<QemuObject>> objects = ArgumentCaptor.forClass(List.class);
        Mockito.verify(qemuImg).convertIntoExistingTarget(Mockito.any(QemuImgFile.class), Mockito.isNull(),
                objects.capture(), srcOpts.capture(), destOpts.capture(), Mockito.eq(false));

        String src = String.join(" ", srcOpts.getValue().toCommandFlag());
        Assert.assertTrue(src, src.startsWith("--image-opts "));
        Assert.assertTrue(src, src.contains("driver=rbd"));
        Assert.assertTrue(src, src.contains("pool=srcpool"));
        Assert.assertTrue(src, src.contains("image=srcimg"));
        Assert.assertTrue(src, src.contains("user=cloudstack"));
        Assert.assertTrue(src, src.contains("conf="));

        String dest = String.join(" ", destOpts.getValue().toCommandFlag(QemuImg.TARGET_IMAGE_OPTS_FLAG));
        Assert.assertTrue(dest, dest.startsWith(QemuImg.TARGET_IMAGE_OPTS_FLAG + " "));
        Assert.assertTrue(dest, dest.contains("driver=rbd"));
        Assert.assertTrue(dest, dest.contains("pool=cloudstack"));
        Assert.assertTrue(dest, dest.contains("image=dst"));
        Assert.assertTrue(dest, dest.contains("encrypt.format=luks2"));
        Assert.assertTrue(dest, dest.contains("encrypt.key-secret=luks0"));

        String secretObjects = objects.getValue().stream()
                .map(o -> String.join(" ", o.toCommandFlag())).collect(Collectors.joining(" "));
        Assert.assertTrue(secretObjects, secretObjects.contains("--object secret,"));
        Assert.assertTrue(secretObjects, secretObjects.contains("id=luks0"));
        Assert.assertTrue(secretObjects, secretObjects.contains("file="));
    }

    @Test
    public void importTemplateFromFileSourceForcesSourceFormat() throws Exception {
        RbdEncryption spy = Mockito.spy(new RbdEncryption());
        QemuImg qemuImg = Mockito.mock(QemuImg.class);
        Mockito.doReturn(qemuImg).when(spy).createQemuImg();

        spy.importTemplate(null, null, "/tmp/tmpl.qcow2", "QCOW2", "1.2.3.4", 6789, "cloudstack", "secret",
                "cloudstack", "dst", "passphrase".getBytes(StandardCharsets.UTF_8), CryptSetup.LuksType.LUKS2);

        ArgumentCaptor<QemuImgFile> srcFile = ArgumentCaptor.forClass(QemuImgFile.class);
        ArgumentCaptor<QemuImageOptions> srcOpts = ArgumentCaptor.forClass(QemuImageOptions.class);
        Mockito.verify(qemuImg).convertIntoExistingTarget(srcFile.capture(), Mockito.isNull(),
                Mockito.anyList(), srcOpts.capture(), Mockito.any(QemuImageOptions.class), Mockito.eq(true));

        Assert.assertEquals(QemuImg.PhysicalDiskFormat.QCOW2, srcFile.getValue().getFormat());
        String src = String.join(" ", srcOpts.getValue().toCommandFlag());
        Assert.assertTrue(src, src.contains("file.filename=/tmp/tmpl.qcow2"));
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
