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
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.attribute.FileAttribute;
import java.nio.file.attribute.PosixFilePermissions;

/**
 * Thin wrapper around the {@code rbd} CLI to apply native librbd LUKS encryption to an
 * RBD image via {@code rbd encryption format}. This is only used at volume create time;
 * runtime decryption is handled by libvirt/qemu through {@code <encryption engine='librbd'>}.
 *
 * The CLI dependency is intentionally isolated in this class so it can later be replaced
 * by a native librbd (JNA) binding without touching callers. rados-java (0.x) does not
 * expose the rbd_encryption_format API, hence the CLI for now.
 *
 * The command builders ({@code build*Script}) are separated from execution so the generated
 * argv can be unit-tested without a live Ceph cluster (see {@code RbdEncryptionTest}).
 */
public class RbdEncryption {
    protected Logger logger = LogManager.getLogger(getClass());

    protected String commandPath = "rbd";
    protected String qemuImgPath = "qemu-img";

    public RbdEncryption() {}

    public RbdEncryption(String commandPath) {
        this.commandPath = commandPath;
    }

    private static String monSpec(String monHost, int monPort) {
        return monPort > 0 ? monHost + ":" + monPort : monHost;
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
        if (passphrase == null || passphrase.length == 0) {
            throw new CloudRuntimeException("Cannot LUKS-format RBD image " + imageSpec + ": empty passphrase");
        }
        try (KeyFile passFile = new KeyFile(passphrase);
             KeyFile cephKeyFile = new KeyFile(authSecret == null ? null : authSecret.getBytes(StandardCharsets.UTF_8))) {
            final Script script = buildFormatScript(imageSpec, luksType, passFile.toString(),
                    monSpec(monHost, monPort), authUser, cephKeyFile.isSet() ? cephKeyFile.toString() : null);
            final String result = script.execute();
            if (result != null) {
                throw new CloudRuntimeException(String.format("Failed to apply librbd %s encryption to %s: %s", luksType, imageSpec, result));
            }
            logger.debug("Applied {} encryption to RBD image {}", luksType, imageSpec);
        } catch (IOException ex) {
            throw new CloudRuntimeException(String.format("Failed to apply librbd %s encryption to %s", luksType, imageSpec), ex);
        }
    }

    protected Script buildFormatScript(String imageSpec, CryptSetup.LuksType luksType, String passFilePath,
                                       String monSpec, String authUser, String cephKeyFilePath) {
        final Script script = new Script(commandPath);
        script.add("encryption");
        script.add("format");
        script.add(imageSpec);
        script.add(luksType.toString());
        script.add(passFilePath);
        script.add("--mon-host");
        script.add(monSpec);
        if (authUser != null) {
            script.add("--id");
            script.add(authUser);
        }
        if (cephKeyFilePath != null) {
            script.add("--keyfile");
            script.add(cephKeyFilePath);
        }
        return script;
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
        if (passphrase == null || passphrase.length == 0) {
            throw new CloudRuntimeException("Cannot resize encrypted RBD image " + imageSpec + ": empty passphrase");
        }
        // rbd --size is in MiB; round up so a non-MiB-aligned request never shrinks the volume below what was asked for.
        final long sizeMiB = (newSizeBytes + (1024L * 1024L) - 1) / (1024L * 1024L);
        try (KeyFile passFile = new KeyFile(passphrase);
             KeyFile cephKeyFile = new KeyFile(authSecret == null ? null : authSecret.getBytes(StandardCharsets.UTF_8))) {
            final Script script = buildResizeScript(imageSpec, sizeMiB, passFile.toString(), allowShrink,
                    monSpec(monHost, monPort), authUser, cephKeyFile.isSet() ? cephKeyFile.toString() : null);
            final String result = script.execute();
            if (result != null) {
                throw new CloudRuntimeException(String.format("Failed to resize encrypted RBD image %s to %d MiB: %s", imageSpec, sizeMiB, result));
            }
            logger.debug("Resized encrypted RBD image {} to {} MiB", imageSpec, sizeMiB);
        } catch (IOException ex) {
            throw new CloudRuntimeException(String.format("Failed to resize encrypted RBD image %s", imageSpec), ex);
        }
    }

    protected Script buildResizeScript(String imageSpec, long sizeMiB, String passFilePath, boolean allowShrink,
                                       String monSpec, String authUser, String cephKeyFilePath) {
        final Script script = new Script(commandPath);
        script.add("resize");
        script.add("--size");
        script.add(String.valueOf(sizeMiB));
        script.add(imageSpec);
        script.add("--encryption-passphrase-file");
        script.add(passFilePath);
        if (allowShrink) {
            script.add("--allow-shrink");
        }
        script.add("--mon-host");
        script.add(monSpec);
        if (authUser != null) {
            script.add("--id");
            script.add(authUser);
        }
        if (cephKeyFilePath != null) {
            script.add("--keyfile");
            script.add(cephKeyFilePath);
        }
        return script;
    }

    /**
     * Import a template into an already-created, already-LUKS-formatted RBD image by writing it
     * THROUGH the librbd encryption layer with {@code qemu-img convert -n} (so the data lands
     * encrypted). This is how encrypted root disks are populated: we never clone-then-format a
     * plaintext template (that leaves the inherited OS data unreadable) — instead we format an
     * empty image and convert the template into it.
     *
     * Exactly one source must be given: an RBD image ({@code srcRbdPool}+{@code srcRbdImage}) or a
     * local file ({@code srcFilePath}[+{@code srcFileFormat}]). cephx auth is provided to qemu-img
     * via a temporary ceph.conf + keyring (deleted on return).
     */
    public void importTemplate(String srcRbdPool, String srcRbdImage,
                               String srcFilePath, String srcFileFormat,
                               String monHost, int monPort, String authUser, String authSecret,
                               String cephPool, String destImage, byte[] passphrase, CryptSetup.LuksType luksType) {
        final String imageSpec = cephPool + "/" + destImage;
        if (passphrase == null || passphrase.length == 0) {
            throw new CloudRuntimeException("Cannot import template into encrypted RBD image " + imageSpec + ": empty passphrase");
        }
        Path conf = null;
        Path keyring = null;
        try (KeyFile passFile = new KeyFile(passphrase)) {
            // These temp files hold the cephx secret; create them 0600 up front (matching KeyFile) rather than relying on the umask.
            final FileAttribute<?> ownerOnly = PosixFilePermissions.asFileAttribute(PosixFilePermissions.fromString("rw-------"));
            keyring = Files.createTempFile("cs-ceph-", ".keyring", ownerOnly);
            Files.writeString(keyring, "[client." + authUser + "]\n\tkey = " + authSecret + "\n");
            conf = Files.createTempFile("cs-ceph-", ".conf", ownerOnly);
            Files.writeString(conf, "[global]\nmon_host = " + monSpec(monHost, monPort) + "\nkeyring = " + keyring + "\n");

            final Script q = buildConvertScript(srcRbdPool, srcRbdImage, srcFilePath, srcFileFormat,
                    cephPool, destImage, conf.toString(), authUser, passFile.toString(), luksType);
            final String result = q.execute();
            if (result != null) {
                throw new CloudRuntimeException(String.format("Failed to import template into encrypted RBD image %s: %s", imageSpec, result));
            }
            logger.debug("Imported template into encrypted RBD image {}", imageSpec);
        } catch (IOException ex) {
            throw new CloudRuntimeException(String.format("Failed to import template into encrypted RBD image %s", imageSpec), ex);
        } finally {
            deleteQuietly(conf);
            deleteQuietly(keyring);
        }
    }

    protected Script buildConvertScript(String srcRbdPool, String srcRbdImage, String srcFilePath, String srcFileFormat,
                                        String cephPool, String destImage, String confPath, String authUser,
                                        String passFilePath, CryptSetup.LuksType luksType) {
        final Script q = new Script(qemuImgPath);
        q.add("convert");
        q.add("-n"); // target already exists (pre-created + luks-formatted)
        if (srcRbdImage != null) {
            q.add("--image-opts");
            q.add("driver=rbd,pool=" + srcRbdPool + ",image=" + srcRbdImage + ",conf=" + confPath + ",user=" + authUser);
        } else {
            if (srcFileFormat != null) {
                q.add("-f");
                q.add(srcFileFormat.toLowerCase());
            }
            q.add(srcFilePath);
        }
        q.add("--object");
        q.add("secret,id=luks0,file=" + passFilePath);
        q.add("--target-image-opts");
        q.add("driver=rbd,pool=" + cephPool + ",image=" + destImage + ",conf=" + confPath + ",user=" + authUser
                + ",encrypt.format=" + luksType + ",encrypt.key-secret=luks0");
        return q;
    }

    private static void deleteQuietly(Path p) {
        if (p == null) {
            return;
        }
        try {
            Files.deleteIfExists(p);
        } catch (IOException ignored) {
            // best-effort cleanup of the temporary ceph auth files
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
