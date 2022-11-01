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
package org.apache.cloudstack.utils.cryptsetup;

import org.apache.cloudstack.secret.PassphraseVO;
import org.junit.Assert;
import org.junit.Assume;
import org.junit.Before;
import org.junit.Test;

import java.io.IOException;
import java.io.RandomAccessFile;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.attribute.PosixFilePermission;
import java.nio.file.attribute.PosixFilePermissions;
import java.util.Set;

public class CryptSetupTest {
    CryptSetup cryptSetup = new CryptSetup();

    @Before
    public void setup() {
        Assume.assumeTrue(cryptSetup.isSupported());
    }

    @Test
    public void cryptSetupTest() throws IOException, CryptSetupException {
        Set<PosixFilePermission> permissions = PosixFilePermissions.fromString("rw-------");
        Path path = Files.createTempFile("cryptsetup", ".tmp",PosixFilePermissions.asFileAttribute(permissions));

        // create a 1MB file to use as a crypt device
        RandomAccessFile file = new RandomAccessFile(path.toFile(),"rw");
        file.setLength(10<<20);
        file.close();

        String filePath = path.toAbsolutePath().toString();
        PassphraseVO passphrase = new PassphraseVO();

        cryptSetup.luksFormat(passphrase.getPassphrase(), CryptSetup.LuksType.LUKS, filePath);

        Assert.assertTrue(cryptSetup.isLuks(filePath));

        Assert.assertTrue(Files.deleteIfExists(path));
    }

    @Test
    public void cryptSetupNonLuksTest() throws IOException {
        Set<PosixFilePermission> permissions = PosixFilePermissions.fromString("rw-------");
        Path path = Files.createTempFile("cryptsetup", ".tmp",PosixFilePermissions.asFileAttribute(permissions));

        Assert.assertFalse(cryptSetup.isLuks(path.toAbsolutePath().toString()));
        Assert.assertTrue(Files.deleteIfExists(path));
    }
}
