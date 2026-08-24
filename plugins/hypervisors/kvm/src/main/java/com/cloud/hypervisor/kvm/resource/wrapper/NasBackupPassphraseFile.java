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

import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.OutputStreamWriter;
import java.io.Writer;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.attribute.PosixFilePermission;
import java.util.EnumSet;

/**
 * Temporary 0600 key file that hands the NAS backup LUKS passphrase to nasbackup.sh ({@code -e}) and to
 * qemu-img ({@code --object secret,file=...}) without ever putting it on a command line or in a log.
 * Shared by the take and restore wrappers; the caller deletes it as soon as the command has finished.
 */
final class NasBackupPassphraseFile {

    private static final Logger LOGGER = LogManager.getLogger(NasBackupPassphraseFile.class);

    private NasBackupPassphraseFile() {
    }

    /** Writes {@code passphrase} to a fresh owner-only temp file, or returns {@code null} when there is no passphrase. */
    static File write(String passphrase) throws IOException {
        if (passphrase == null || passphrase.isEmpty()) {
            return null;
        }
        File passphraseFile = null;
        try {
            passphraseFile = File.createTempFile("cs-backup-enc-", ".key");
            passphraseFile.deleteOnExit();
            Files.setPosixFilePermissions(passphraseFile.toPath(),
                    EnumSet.of(PosixFilePermission.OWNER_READ, PosixFilePermission.OWNER_WRITE));
            try (Writer writer = new OutputStreamWriter(new FileOutputStream(passphraseFile), StandardCharsets.UTF_8)) {
                writer.write(passphrase);
            }
            return passphraseFile;
        } catch (IOException e) {
            delete(passphraseFile);
            throw e;
        }
    }

    /** Best-effort removal; safe to call with {@code null}. */
    static void delete(File passphraseFile) {
        if (passphraseFile != null && passphraseFile.exists() && !passphraseFile.delete()) {
            LOGGER.warn("Could not delete temporary backup passphrase file {}", passphraseFile.getAbsolutePath());
        }
    }
}
