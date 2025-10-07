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

package org.apache.cloudstack.framework.extensions.manager;

import java.io.File;
import java.io.IOException;
import java.io.UncheckedIOException;
import java.net.URLEncoder;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.NoSuchFileException;
import java.nio.file.Path;
import java.nio.file.StandardCopyOption;
import java.security.InvalidKeyException;
import java.security.NoSuchAlgorithmException;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.concurrent.Executors;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.TimeUnit;
import java.util.stream.Stream;

import javax.inject.Inject;
import javax.naming.ConfigurationException;

import org.apache.cloudstack.extension.Extension;
import org.apache.cloudstack.framework.config.ConfigKey;
import org.apache.cloudstack.framework.extensions.command.DownloadAndSyncExtensionFilesCommand;
import org.apache.cloudstack.managed.context.ManagedContextRunnable;
import org.apache.cloudstack.management.ManagementServerHost;
import org.apache.cloudstack.utils.security.DigestHelper;
import org.apache.cloudstack.utils.security.HMACSignUtil;
import org.apache.cloudstack.utils.server.ServerPropertiesUtil;
import org.apache.commons.codec.DecoderException;
import org.apache.commons.collections.CollectionUtils;

import com.cloud.agent.api.Answer;
import com.cloud.agent.api.Command;
import com.cloud.cluster.ClusterManager;
import com.cloud.serializer.GsonHelper;
import com.cloud.utils.FileUtil;
import com.cloud.utils.HttpUtils;
import com.cloud.utils.Pair;
import com.cloud.utils.StringUtils;
import com.cloud.utils.component.ManagerBase;
import com.cloud.utils.concurrency.NamedThreadFactory;
import com.cloud.utils.db.GlobalLock;
import com.cloud.utils.script.Script;

public class ExtensionsShareManagerImpl extends ManagerBase implements ExtensionsShareManager {

    protected static final String EXTENSIONS_SHARE_SUBDIR = "extensions";
    protected static final int DEFAULT_SHARE_LINK_VALIDITY_SECONDS = 3600; // 1 hour

    ConfigKey<Integer> ShareLinkValidityInterval = new ConfigKey<>("Advanced", Integer.class,
            "extension.share.link.validity.interval", String.valueOf(DEFAULT_SHARE_LINK_VALIDITY_SECONDS),
            String.format("Interval (in seconds) for which the extension archive share link is valid. " +
                    "Default is %s seconds", DEFAULT_SHARE_LINK_VALIDITY_SECONDS),
            false, ConfigKey.Scope.Global);

    @Inject
    ExtensionsFilesystemManager extensionsFilesystemManager;

    @Inject
    ClusterManager clusterManager;

    private ScheduledExecutorService extensionShareCleanupExecutor;
    private int shareLinkValidityInterval;

    protected Path getExtensionsSharePath() {
        String shareBaseDir = ServerPropertiesUtil.getShareBaseDirectory();
        shareBaseDir += File.separator + EXTENSIONS_SHARE_SUBDIR;
        return Path.of(shareBaseDir);
    }

    protected String getManagementServerBaseUrl(ManagementServerHost managementHost) {
        boolean secure = Boolean.parseBoolean(ServerPropertiesUtil.getProperty("https.enable",
                "false"));
        final String scheme = secure ? "https" : "http";
        final String host = managementHost.getServiceIP();
        int port = secure
                ? Integer.parseInt(ServerPropertiesUtil.getProperty("https.port", "8443"))
                : Integer.parseInt(ServerPropertiesUtil.getProperty("http.port", "8080"));

        return String.format("%s://%s:%d", scheme, host, port);
    }

    protected Pair<Boolean, String> getResultFromAnswersString(String answersStr, Extension extension,
                   ManagementServerHost msHost, String op) {
        Answer[] answers = null;
        try {
            answers = GsonHelper.getGson().fromJson(answersStr, Answer[].class);
        } catch (Exception e) {
            logger.error("Failed to parse answer JSON during {} for {} on {}: {}",
                    op, extension, msHost, e.getMessage(), e);
            return new Pair<>(false, e.getMessage());
        }
        Answer answer = answers != null && answers.length > 0 ? answers[0] : null;
        boolean result = false;
        String details = "Unknown error";
        if (answer != null) {
            result = answer.getResult();
            details = answer.getDetails();
        }
        if (!result) {
            logger.error("Failed to {} for {} on {} due to {}", op, extension, msHost, details);
            return new Pair<>(false, details);
        }
        return new Pair<>(true, details);
    }

    /**
      * Creates a .tgz archive for the specified extension.
      * If the files list is empty, the entire extension directory is archived.
      * If the files list is not empty, only the specified relative files are archived; throws if any file is missing.
      *
      * @return ArchiveInfo containing the archive path, size, SHA-256 checksum, and sync type.
      */
    protected ArchiveInfo createArchive(Extension extension, List<String> files) throws IOException {
        final boolean isPartial = CollectionUtils.isNotEmpty(files);
        final DownloadAndSyncExtensionFilesCommand.SyncType syncType =
                isPartial ? DownloadAndSyncExtensionFilesCommand.SyncType.Partial
                        : DownloadAndSyncExtensionFilesCommand.SyncType.Complete;
        final Path extensionRootPath = extensionsFilesystemManager.getExtensionRootPath(extension);
        final List<Path> toPack;
        if (isPartial) {
            toPack = new ArrayList<>(files.size());
            for (String rel : files) {
                Path p = extensionRootPath.resolve(rel).normalize();
                if (!p.startsWith(extensionRootPath)) {
                    throw new SecurityException("Path escapes version dir: " + rel);
                }
                if (!Files.exists(p)) {
                    throw new NoSuchFileException("File not found: " + p.toAbsolutePath().toString());
                }
                toPack.add(p);
            }
        } else {
            toPack = List.of(extensionRootPath);
        }
        StringBuilder archiveName = new StringBuilder(Extension.getDirectoryName(extension.getName()))
                .append("-").append(System.currentTimeMillis()).append(".tgz");
        if (isPartial) {
            archiveName.insert(0, "partial-");
        }
        Path archivePath = getExtensionsSharePath().resolve(archiveName.toString());

        if (!packAsTgz(extension, extensionRootPath, toPack, archivePath)) {
            throw new IOException("Failed to create archive " + archivePath);
        }

        logger.info("Created archive {} from {} ({} files)", archivePath, extensionRootPath, toPack.size());

        long size = Files.size(archivePath);
        String checksum = DigestHelper.calculateChecksum(archivePath.toFile());

        return new ArchiveInfo(archivePath, size, checksum, syncType);
    }

    /**
      * Generates a signed share URL for the given extension archive.
      * The resulting URL format is: {baseUrl}/share/extensions/{archiveName}?exp={expiry}&sig={signature}
      *
      * @param managementServer the management server host generating the URL
      * @param archivePath the path to the archive file
      * @return the signed share URL for the archive
      * @throws DecoderException if signature decoding fails
      * @throws NoSuchAlgorithmException if the HMAC algorithm is not available
      * @throws InvalidKeyException if the secret key is invalid
      */
    protected String generateSignedArchiveUrl(ManagementServerHost managementServer, Path archivePath)
            throws DecoderException, NoSuchAlgorithmException, InvalidKeyException {
        final String baseUrl = getManagementServerBaseUrl(managementServer);
        final long expiresAtEpochSec = System.currentTimeMillis() / 1000L + shareLinkValidityInterval;
        final String secretKey = ServerPropertiesUtil.getShareSecret();
        String archiveName = archivePath.getFileName().toString();
        String uriPath = String.format("/%s/%s/%s", ServerPropertiesUtil.SHARE_DIR, EXTENSIONS_SHARE_SUBDIR,
                archiveName);
        String sig = "";
        if (StringUtils.isNotBlank(secretKey)) {
            String payload = uriPath + "|" + expiresAtEpochSec;
            sig = HMACSignUtil.generateSignature(payload, secretKey);
        }
        StringBuilder sb = new StringBuilder();
        sb.append(baseUrl).append(uriPath).append("?exp=").append(expiresAtEpochSec);
        if (StringUtils.isNotBlank(sig)) {
            sb.append("&sig=").append(URLEncoder.encode(sig, StandardCharsets.UTF_8));
        }
        return sb.toString();
    }

    /**
     * Build the DownloadAndSyncExtensionFilesCommand to send to a target MS.
     */
    protected DownloadAndSyncExtensionFilesCommand buildCommand(long msId, Extension ext, ArchiveInfo archive,
                String signedUrl) {
        DownloadAndSyncExtensionFilesCommand cmd =
                new DownloadAndSyncExtensionFilesCommand(msId, ext, signedUrl, archive.getSize(), archive.getChecksum());
        cmd.setSyncType(archive.getSyncType());
        return cmd;
    }

    /**
     * Packs the specified files or directories into a .tgz archive.
     * If a single directory is provided, archives the entire directory.
     * If multiple files are provided, copies them to a temporary directory preserving structure before archiving.
     *
     * @param extensionRootPath the root path of the extension
     * @param toPack list of files or directories to include in the archive
     * @param archivePath the destination path for the .tgz archive
     * @return true if the archive was created successfully, false otherwise
     * @throws IOException if an I/O error occurs during packing
     */
    protected boolean packAsTgz(Extension extension, Path extensionRootPath, List<Path> toPack, Path archivePath)
            throws IOException {
        Files.createDirectories(archivePath.getParent());
        FileUtil.deletePath(archivePath.toAbsolutePath().toString());

        Path sourceDir;
        if (toPack.size() == 1 && Files.isDirectory(toPack.get(0))) {
            sourceDir = toPack.get(0);
        } else {
            sourceDir = Files.createTempDirectory("pack-tmp-");
            for (Path p : toPack) {
                Path rel = extensionRootPath.relativize(p);
                Path dest = sourceDir.resolve(rel);
                Files.createDirectories(dest.getParent());
                Files.copy(p, dest, StandardCopyOption.COPY_ATTRIBUTES);
            }
        }

        boolean result = extensionsFilesystemManager.packExtensionFilesAsTgz(extension, sourceDir, archivePath);

        if (!sourceDir.equals(extensionRootPath)) {
            FileUtil.deleteRecursively(sourceDir);
        }

        return result;
    }

    protected static boolean extractTgz(Path archive, Path destRoot) throws IOException {
        int result = Script.executeCommandForExitValue(
                60 * 1000,
                Script.getExecutableAbsolutePath("tar"),
                "-xpf", archive.toAbsolutePath().toString(),
                "-C", destRoot.toAbsolutePath().toString()
        );
        return result == 0;
    }

    protected long downloadTo(String url, Path dest) throws IOException {
        boolean result = HttpUtils.downloadFileWithProgress(url, dest.toString(), logger);
        if (!result) {
            throw new IOException("Download failed");
        }
        if (!Files.exists(dest)) {
            throw new IOException("Download failed: file not found");
        }
        return Files.size(dest);
    }

    /**
     * Atomically replaces the target directory with the source directory.
     * If the target exists, it is first moved to a backup location.
     * Attempts an atomic move; falls back to a regular move if necessary.
     * Cleans up the backup directory after a successful move.
     *
     * @param from the source directory to move
     * @param to the target directory to replace
     * @throws IOException if an I/O error occurs during the operation
     */
    protected static void atomicReplaceDir(Path from, Path to) throws IOException {
        Files.createDirectories(from);
        Files.createDirectories(to.getParent());
        Path backup = to.getParent().resolve(to.getFileName().toString() + ".bak-" + System.currentTimeMillis());
        if (Files.exists(to)) {
            try {
                Files.move(to, backup, StandardCopyOption.ATOMIC_MOVE);
            } catch (IOException e) {
                Files.move(to, backup);
            }
        }
        try {
            Files.move(from, to, StandardCopyOption.ATOMIC_MOVE);
            FileUtil.deleteRecursively(backup);
        } catch (IOException e) {
            if (Files.exists(backup)) {
                try {
                    Files.move(backup, to, StandardCopyOption.ATOMIC_MOVE);
                } catch (IOException ignore) {}
            }
            throw e;
        }
    }

    /**
     * Overlays files from the source directory into the target directory.
     * For each file in `fromRoot`, copies it to the corresponding location in `targetRoot`,
     * replacing existing files atomically when possible.
     * Directory structure is preserved.
     *
     * @param fromRoot the source root directory containing files to overlay
     * @param targetRoot the target root directory to overlay files into
     * @throws IOException if an I/O error occurs during overlay
     */
    protected static void overlayInto(Path fromRoot, Path targetRoot) throws IOException {
        try (Stream<Path> stream = Files.walk(fromRoot)) {
            stream.forEach(src -> {
                try {
                    if (Files.isDirectory(src)) return;
                    Path rel = fromRoot.relativize(src);
                    Path dst = targetRoot.resolve(rel);
                    Files.createDirectories(dst.getParent());
                    Path tmp = dst.getParent().resolve(dst.getFileName() + ".tmp-" + System.nanoTime());
                    Files.copy(src, tmp, StandardCopyOption.REPLACE_EXISTING);
                    try {
                        Files.move(tmp, dst, StandardCopyOption.ATOMIC_MOVE, StandardCopyOption.REPLACE_EXISTING);
                    } catch (IOException e) {
                        Files.move(tmp, dst, StandardCopyOption.REPLACE_EXISTING);
                    }
                } catch (IOException ex) {
                    throw new UncheckedIOException(ex);
                }
            });
        }
    }

    /**
     * Applies the extension synchronization by extracting the archive and updating the extension directory.
     * For COMPLETE sync type, replaces the entire extension directory atomically.
     * For PARTIAL sync type, overlays the extracted files into the existing extension directory.
     *
     * @param extension the extension to synchronize
     * @param syncType the type of synchronization (COMPLETE or PARTIAL)
     * @param tmpArchive the path to the temporary archive file
     * @param extensionRootPath the root path of the extension directory
     * @throws IOException if an I/O error occurs during extraction or application
     */
    protected void applyExtensionSync(Extension extension, DownloadAndSyncExtensionFilesCommand.SyncType syncType,
                      Path tmpArchive, Path extensionRootPath) throws IOException {
        logger.debug("Applying extension sync for {} with sync type {} from archive {}", extension, syncType,
                tmpArchive);
        Path stagingDir = extensionsFilesystemManager.getExtensionsStagingPath();
        Path applyRoot = Files.createTempDirectory(stagingDir,
                Extension.getDirectoryName(extension.getName()) + "-");
        if (!extractTgz(tmpArchive, applyRoot)) {
            throw new IOException("Failed to extract archive " + tmpArchive);
        }
        if (DownloadAndSyncExtensionFilesCommand.SyncType.Complete.equals(syncType)) {
            atomicReplaceDir(applyRoot, extensionRootPath);
            return;
        }
        overlayInto(applyRoot, extensionRootPath);
        FileUtil.deleteRecursively(applyRoot);
    }

    protected void  cleanupExtensionsShareFiles(long cutoff) throws IOException {
        Path sharePath = getExtensionsSharePath();
        if (!Files.exists(sharePath) || !Files.isDirectory(sharePath)) {
            return;
        }
        try (Stream<Path> paths = Files.list(sharePath)) {
            paths.filter(p -> p.getFileName().toString().endsWith(".tgz"))
                    .filter(p -> {
                        try {
                            return Files.getLastModifiedTime(p).toMillis() < cutoff;
                        } catch (IOException e) {
                            return false;
                        }
                    })
                    .forEach(p -> {
                        try {
                            Files.delete(p);
                            logger.debug("Deleted expired extension archive {}", p);
                        } catch (IOException e) {
                            logger.warn("Failed to delete expired extension archive {}", p, e);
                        }
                    });
        }
    }

    @Override
    public boolean configure(String name, Map<String, Object> params) throws ConfigurationException {
        try {
            extensionShareCleanupExecutor = Executors.newScheduledThreadPool(1,
                    new NamedThreadFactory("Extension-Share-Cleanup"));
        } catch (final Exception e) {
            throw new ConfigurationException("Unable to to configure ExtensionsManagerImpl");
        }
        return true;
    }

    @Override
    public boolean start() {
        int initialDelay = 120;
        shareLinkValidityInterval = ShareLinkValidityInterval.value();
        logger.debug("Scheduling cleanup task for extension share archive with initial delay={}s and interval={}s",
                initialDelay, shareLinkValidityInterval);
        extensionShareCleanupExecutor.scheduleWithFixedDelay(new ShareCleanupWorker(),
                initialDelay, shareLinkValidityInterval, TimeUnit.SECONDS);
        return true;
    }

    @Override
    public boolean stop() {
        if (extensionShareCleanupExecutor != null && !extensionShareCleanupExecutor.isShutdown()) {
            extensionShareCleanupExecutor.shutdownNow();
        }
        return true;
    }

    @Override
    public Pair<Boolean, String> syncExtension(Extension extension, ManagementServerHost sourceManagementServer,
                     List<ManagementServerHost> targetManagementServers, List<String> files) {
        ArchiveInfo archiveInfo = null;
        try {
            try {
                archiveInfo = createArchive(extension, files);
            } catch (IOException e) {
                String msg = "Failed to create archive";
                logger.error("{} for {}", extension, msg, e);
                return new Pair<>(false, msg);
            }
            String signedUrl;
            try {
                signedUrl = generateSignedArchiveUrl(sourceManagementServer, archiveInfo.getPath());
            } catch (DecoderException | NoSuchAlgorithmException | InvalidKeyException e) {
                String msg = "Failed to generate signed URL";
                logger.error("{} for {} using {}", msg, extension, sourceManagementServer, e);
                return new Pair<>(false, msg);
            }
            for (ManagementServerHost targetMs : targetManagementServers) {
                String targetUrl = signedUrl + "&tgt=" + URLEncoder.encode(targetMs.getUuid(), StandardCharsets.UTF_8);
                final String msPeer = Long.toString(targetMs.getMsid());
                final Command[] cmds = new Command[1];
                cmds[0] = buildCommand(sourceManagementServer.getMsid(), extension, archiveInfo, targetUrl);
                String answersStr = clusterManager.execute(msPeer, 0L, GsonHelper.getGson().toJson(cmds), true);
                Pair<Boolean, String> result = getResultFromAnswersString(answersStr, extension, targetMs,
                        "sync");
                if (!result.first()) {
                    String msg = "Sync failed";
                    logger.error("{} for {} on {} due to: {}", msg, extension, targetMs, result.second());
                    return new Pair<>(false, String.format("%s on management server: %s", msg, targetMs.getName()));
                }
            }
        } finally {
            if (archiveInfo != null) {
                FileUtil.deletePath(archiveInfo.getPath().toAbsolutePath().toString());
            }
        }
        return new Pair<>(true, "");
    }

    @Override
    public Pair<Boolean, String> downloadAndApplyExtensionSync(Extension extension,
                   DownloadAndSyncExtensionFilesCommand cmd) {
        final Path extensionRootPath = extensionsFilesystemManager.getExtensionRootPath(extension);
        Path tmpArchive = null;
        try {
            tmpArchive = Files.createTempFile("dl-", ".tgz");
            long contentLength = downloadTo(cmd.getDownloadUrl(), tmpArchive);

            if (cmd.getSize() > 0 && contentLength != -1 && cmd.getSize() != contentLength) {
                return new Pair<>(false, String.format("Size mismatch: expected %d got %d", cmd.getSize(),
                        contentLength));
            }

            String got = DigestHelper.calculateChecksum(tmpArchive.toFile());
            if (!got.equalsIgnoreCase(cmd.getChecksum())) {
                return new Pair<>(false, String.format("Checksum mismatch for archive. expected=%s got=%s",
                        cmd.getChecksum(), got));
            }

            applyExtensionSync(extension, cmd.getSyncType(), tmpArchive, extensionRootPath);
        } catch (IOException e) {
            String msg = String.format("Failed to download/apply sync for %s from %s: %s", extension,
                    cmd.getDownloadUrl(), e.getMessage());
            logger.error(msg, e);
            return new Pair<>(false, msg);
        } finally {
            if (tmpArchive != null) {
                FileUtil.deletePath(tmpArchive.toAbsolutePath().toString());
            }
        }
        return new Pair<>(true, "");
    }

    protected static class ArchiveInfo {
        private final Path path;
        private final long size;
        private final String checksum;
        private final DownloadAndSyncExtensionFilesCommand.SyncType syncType;

        public ArchiveInfo(Path path, long size, String checksum,
                   DownloadAndSyncExtensionFilesCommand.SyncType syncType) {
            this.path = path;
            this.size = size;
            this.checksum = checksum;
            this.syncType = syncType;
        }

        public Path getPath() { return path; }
        public long getSize() { return size; }
        public String getChecksum() { return checksum; }
        public DownloadAndSyncExtensionFilesCommand.SyncType getSyncType() { return syncType; }
    }

    protected class ShareCleanupWorker extends ManagedContextRunnable {
        protected void reallyRun() {
            try {
                long expiryMillis = shareLinkValidityInterval * 1100L;
                long cutoff = System.currentTimeMillis() - expiryMillis;
                cleanupExtensionsShareFiles(cutoff);
            } catch (Exception e) {
                logger.warn("Extensions share cleanup failed", e);
            }
        }

        @Override
        protected void runInContext() {
            GlobalLock gcLock = GlobalLock.getInternLock("ExtensionShareCleanup");
            try {
                if (gcLock.lock(3)) {
                    try {
                        reallyRun();
                    } finally {
                        gcLock.unlock();
                    }
                }
            } finally {
                gcLock.releaseRef();
            }
        }
    }
}
