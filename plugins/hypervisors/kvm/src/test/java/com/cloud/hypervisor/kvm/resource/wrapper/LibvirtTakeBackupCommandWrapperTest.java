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
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import org.apache.cloudstack.backup.TakeBackupCommand;
import org.junit.Assert;
import org.junit.Test;

public class LibvirtTakeBackupCommandWrapperTest {

    private final LibvirtTakeBackupCommandWrapper wrapper = new LibvirtTakeBackupCommandWrapper();

    @Test
    public void testNullDetailsAddsNoFlags() throws Exception {
        List<String> args = new ArrayList<>();
        Assert.assertNull(wrapper.appendEnhancementFlags(args, null));
        Assert.assertTrue(args.isEmpty());
    }

    @Test
    public void testCompressionBandwidthAndIntegrityFlags() throws Exception {
        Map<String, String> details = new HashMap<>();
        details.put(TakeBackupCommand.DETAIL_COMPRESSION, "true");
        details.put(TakeBackupCommand.DETAIL_BANDWIDTH_LIMIT, "50");
        details.put(TakeBackupCommand.DETAIL_INTEGRITY_CHECK, "true");

        List<String> args = new ArrayList<>();
        Assert.assertNull(wrapper.appendEnhancementFlags(args, details));
        Assert.assertTrue(args.contains("-c"));
        Assert.assertEquals("50", args.get(args.indexOf("-b") + 1));
        Assert.assertTrue(args.contains("--verify"));
    }

    @Test
    public void testBandwidthZeroIsSkipped() throws Exception {
        Map<String, String> details = new HashMap<>();
        details.put(TakeBackupCommand.DETAIL_BANDWIDTH_LIMIT, "0");

        List<String> args = new ArrayList<>();
        wrapper.appendEnhancementFlags(args, details);
        Assert.assertFalse(args.contains("-b"));
    }

    @Test
    public void testEncryptionWritesPassphraseFileAndFlag() throws Exception {
        Map<String, String> details = new HashMap<>();
        details.put(TakeBackupCommand.DETAIL_ENCRYPTION, "true");
        details.put(TakeBackupCommand.DETAIL_ENCRYPTION_PASSPHRASE, "s3cret");

        List<String> args = new ArrayList<>();
        File passphraseFile = wrapper.appendEnhancementFlags(args, details);
        try {
            Assert.assertNotNull(passphraseFile);
            Assert.assertTrue(passphraseFile.exists());
            Assert.assertEquals(passphraseFile.getAbsolutePath(), args.get(args.indexOf("-e") + 1));
            Assert.assertEquals("s3cret",
                    new String(Files.readAllBytes(passphraseFile.toPath()), StandardCharsets.UTF_8));
        } finally {
            if (passphraseFile != null) {
                passphraseFile.delete();
            }
        }
    }

    @Test
    public void testEncryptionWithoutPassphraseThrows() {
        Map<String, String> details = new HashMap<>();
        details.put(TakeBackupCommand.DETAIL_ENCRYPTION, "true");

        try {
            wrapper.appendEnhancementFlags(new ArrayList<>(), details);
            Assert.fail("Expected BackupConfigException when encryption is enabled without a passphrase");
        } catch (LibvirtTakeBackupCommandWrapper.BackupConfigException e) {
            Assert.assertTrue(e.getMessage().toLowerCase().contains("passphrase"));
        }
    }
}
