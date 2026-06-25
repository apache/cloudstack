// Licensed to the Apache Software Foundation (ASF) under one
// or more contributor license agreements.  See the NOTICE file
// distributed with this work for additional information
// regarding copyright ownership.  The ASF licenses this file
// to you under the Apache License, Version 2.0 (the
// "License"); you may not use this file except in compliance
// the License.  You may obtain a copy of the License at
//
// http://www.apache.org/licenses/LICENSE-2.0
//
// Unless required by applicable law or agreed to in writing,
// software distributed under the License is distributed on an
// "AS IS" BASIS, WITHOUT WARRANTIES OR CONDITIONS OF ANY
// KIND, either express or implied.  See the License for the
// specific language governing permissions and limitations
// under the License.
package org.apache.cloudstack.utils.rbd;

import com.cloud.utils.exception.CloudRuntimeException;
import com.cloud.utils.script.Script;
import org.apache.cloudstack.utils.cryptsetup.CryptSetup;
import org.apache.cloudstack.utils.cryptsetup.KeyFile;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

import java.io.IOException;
import java.nio.charset.StandardCharsets;

/**
 * Thin wrapper around the {@code rbd} CLI to apply native librbd LUKS encryption to an
 * RBD image via {@code rbd encryption format}. This is only used at volume create time;
 * runtime decryption is handled by libvirt/qemu through {@code <encryption engine='librbd'>}.
 *
 * The CLI dependency is intentionally isolated in this class so it can later be replaced
 * by a native librbd (JNA) binding without touching callers. rados-java (0.x) does not
 * expose the rbd_encryption_format API, hence the CLI for now.
 */
public class RbdEncryption {
    protected static Logger LOGGER = LogManager.getLogger(RbdEncryption.class);

    protected String commandPath = "rbd";

    public RbdEncryption() {}

    public RbdEncryption(String commandPath) {
        this.commandPath = commandPath;
    }

    /**
     * Apply a LUKS header to an existing RBD image so librbd can transparently encrypt it.
     * <p>
     * cephx authentication is supplied via {@code --id} plus a temporary keyfile so the secret
     * never appears on the command line. The LUKS passphrase is supplied via a temporary file
     * ({@link KeyFile}). Both temp files are deleted when this method returns.
     *
     * @param monHost     ceph monitor host
     * @param monPort     ceph monitor port (0 to omit)
     * @param authUser    cephx user (e.g. "cloudstack"); null to skip --id
     * @param authSecret  cephx secret/key (as used for ceph "key" config); null to skip --keyfile
     * @param cephPool    ceph pool name
     * @param image       rbd image name
     * @param passphrase  LUKS passphrase
     * @param luksType    LUKS1/LUKS2 (librbd engine supports both; LUKS2 recommended)
     */
    public void format(String monHost, int monPort, String authUser, String authSecret,
                       String cephPool, String image, byte[] passphrase, CryptSetup.LuksType luksType) {
        final String imageSpec = cephPool + "/" + image;
        try (KeyFile passFile = new KeyFile(passphrase);
             KeyFile cephKeyFile = new KeyFile(authSecret == null ? null : authSecret.getBytes(StandardCharsets.UTF_8))) {
            final Script script = new Script(commandPath);
            script.add("encryption");
            script.add("format");
            script.add(imageSpec);
            script.add(luksType.toString());
            script.add(passFile.toString());
            script.add("--mon-host");
            script.add(monPort > 0 ? monHost + ":" + monPort : monHost);
            if (authUser != null) {
                script.add("--id");
                script.add(authUser);
            }
            if (cephKeyFile.isSet()) {
                script.add("--keyfile");
                script.add(cephKeyFile.toString());
            }

            final String result = script.execute();
            if (result != null) {
                throw new CloudRuntimeException(String.format("Failed to apply librbd %s encryption to %s: %s", luksType, imageSpec, result));
            }
            LOGGER.debug("Applied {} encryption to RBD image {}", luksType, imageSpec);
        } catch (IOException ex) {
            throw new CloudRuntimeException(String.format("Failed to apply librbd %s encryption to %s", luksType, imageSpec), ex);
        }
    }

    /**
     * Resize an encrypted RBD image. librbd needs the passphrase so it can resize the encrypted
     * payload (not just the raw image) and keep the LUKS header consistent. {@code newSizeBytes} is
     * the usable (decrypted) size requested; rbd {@code --size} is expressed in MiB.
     *
     * @param allowShrink pass --allow-shrink when shrinking is permitted
     */
    public void resize(String monHost, int monPort, String authUser, String authSecret,
                       String cephPool, String image, long newSizeBytes, boolean allowShrink, byte[] passphrase) {
        final String imageSpec = cephPool + "/" + image;
        final long sizeMiB = newSizeBytes / (1024L * 1024L);
        try (KeyFile passFile = new KeyFile(passphrase);
             KeyFile cephKeyFile = new KeyFile(authSecret == null ? null : authSecret.getBytes(StandardCharsets.UTF_8))) {
            final Script script = new Script(commandPath);
            script.add("resize");
            script.add("--size");
            script.add(String.valueOf(sizeMiB));
            script.add(imageSpec);
            script.add("--encryption-passphrase-file");
            script.add(passFile.toString());
            if (allowShrink) {
                script.add("--allow-shrink");
            }
            script.add("--mon-host");
            script.add(monPort > 0 ? monHost + ":" + monPort : monHost);
            if (authUser != null) {
                script.add("--id");
                script.add(authUser);
            }
            if (cephKeyFile.isSet()) {
                script.add("--keyfile");
                script.add(cephKeyFile.toString());
            }

            final String result = script.execute();
            if (result != null) {
                throw new CloudRuntimeException(String.format("Failed to resize encrypted RBD image %s to %d MiB: %s", imageSpec, sizeMiB, result));
            }
            LOGGER.debug("Resized encrypted RBD image {} to {} MiB", imageSpec, sizeMiB);
        } catch (IOException ex) {
            throw new CloudRuntimeException(String.format("Failed to resize encrypted RBD image %s", imageSpec), ex);
        }
    }

    /**
     * Best-effort probe that the local rbd CLI supports the encryption subcommand.
     */
    public boolean isSupported() {
        final Script script = new Script(commandPath);
        script.add("help");
        script.add("encryption");
        script.add("format");
        return script.execute() == null;
    }
}
