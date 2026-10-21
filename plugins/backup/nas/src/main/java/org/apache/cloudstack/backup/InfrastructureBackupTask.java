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
package org.apache.cloudstack.backup;

import com.cloud.utils.db.DbProperties;
import com.cloud.utils.db.GlobalLock;

import org.apache.cloudstack.managed.context.ManagedContextRunnable;
import org.apache.cloudstack.poll.BackgroundPollTask;
import org.apache.cloudstack.utils.identity.ManagementServerNode;
import org.apache.logging.log4j.Logger;
import org.apache.logging.log4j.LogManager;

import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.io.Writer;
import java.net.InetAddress;
import java.net.UnknownHostException;
import java.nio.charset.StandardCharsets;
import java.nio.file.FileVisitResult;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.nio.file.SimpleFileVisitor;
import java.nio.file.attribute.BasicFileAttributes;
import java.nio.file.attribute.FileAttribute;
import java.nio.file.attribute.PosixFilePermission;
import java.nio.file.attribute.PosixFilePermissions;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.Arrays;
import java.util.Comparator;
import java.util.EnumSet;
import java.util.List;
import java.util.Properties;
import java.util.Set;
import java.util.stream.Collectors;
import java.util.concurrent.TimeUnit;
import java.util.zip.GZIPOutputStream;

/**
 * Scheduled task that backs up CloudStack infrastructure to NAS storage:
 * <ul>
 *   <li>MySQL databases (cloud, cloud_usage if enabled)</li>
 *   <li>Management server configuration files</li>
 *   <li>Agent configuration files</li>
 *   <li>SSL certificates and keystores</li>
 * </ul>
 *
 * Database credentials come from the management server's db.properties (via {@link DbProperties},
 * which decrypts ENC(...) values when db.properties encryption is in use).
 * Backups are stored under {nasBackupPath}/infra-backup/{ms}/{timestamp}/ with
 * automatic retention management. A backup set only counts once every requested component succeeded,
 * which is recorded by a {@value #COMPLETION_MARKER} file; a failed run is removed and never triggers
 * retention, so failures cannot push good backups out. Everything is created owner-only (directories
 * 0700, files 0600) because the archives hold db.properties and the encryption key, and the NAS is
 * usually mounted on the hosts as well.
 */
public class InfrastructureBackupTask extends ManagedContextRunnable implements BackgroundPollTask {

    private static final Logger LOG = LogManager.getLogger(InfrastructureBackupTask.class);

    private static final String MANAGEMENT_CONFIG_PATH = "/etc/cloudstack/management";
    private static final String AGENT_CONFIG_PATH = "/etc/cloudstack/agent";
    private static final String SSL_CERT_PATH = "/etc/cloudstack/management/cert";

    /** Written last into a backup set, only when every requested component succeeded. */
    protected static final String COMPLETION_MARKER = "BACKUP_COMPLETE";

    private static final Set<PosixFilePermission> OWNER_ONLY_DIR = PosixFilePermissions.fromString("rwx------");
    private static final Set<PosixFilePermission> OWNER_ONLY_FILE = PosixFilePermissions.fromString("rw-------");

    /** 24 hours in milliseconds */
    private static final long DAILY_INTERVAL_MS = 86400L * 1000L;

    @Override
    public Long getDelay() {
        return DAILY_INTERVAL_MS;
    }

    /** Indirection so tests can override without standing up the ConfigDepot. */
    protected boolean isEnabled() {
        return Boolean.TRUE.equals(NASBackupProvider.NASInfraBackupEnabled.value());
    }

    protected String getBackupLocation() {
        return NASBackupProvider.NASInfraBackupLocation.value();
    }

    protected int getRetentionCount() {
        return NASBackupProvider.NASInfraBackupRetention.value();
    }

    protected boolean isDatabaseIncluded() {
        return Boolean.TRUE.equals(NASBackupProvider.NASInfraBackupIncludeDatabase.value());
    }

    protected boolean isUsageDbIncluded() {
        return Boolean.TRUE.equals(NASBackupProvider.NASInfraBackupUsageDb.value());
    }

    @Override
    protected void runInContext() {
        if (!isEnabled()) {
            LOG.debug("Infrastructure backup is disabled (nas.infra.backup.enabled=false)");
            return;
        }

        String nasBackupPath = getBackupLocation();
        if (nasBackupPath == null || nasBackupPath.isEmpty()) {
            LOG.error("Infrastructure backup location not configured (nas.infra.backup.location is empty)");
            return;
        }

        int retentionCount = getRetentionCount();
        boolean includeDatabase = isDatabaseIncluded();
        boolean includeUsageDb = isUsageDbIncluded();

        String timestamp = LocalDateTime.now().format(DateTimeFormatter.ofPattern("yyyyMMdd-HHmmss"));
        String infraBackupRoot = nasBackupPath + "/infra-backup/" + getManagementServerLabel();
        String backupDir = infraBackupRoot + "/" + timestamp;

        LOG.info("Starting infrastructure backup to {} (database included: {})", backupDir, includeDatabase);

        // Cluster-wide lock: in multi-management-server deployments only one MS should run the
        // infrastructure backup at a time, otherwise they would write/delete concurrently in the
        // same NAS infra-backup path (duplicate backups, retention races, accidental deletes).
        GlobalLock lock = acquireRunLock();
        if (lock == null) {
            LOG.info("Another management server is performing the infrastructure backup; skipping this run");
            return;
        }

        File dir = new File(backupDir);
        boolean complete = false;
        try {
            try {
                createBackupDirectories(nasBackupPath, infraBackupRoot, backupDir);
            } catch (IOException e) {
                LOG.error("{} ({})", describeDirectoryCreationFailure(backupDir, nasBackupPath), e.toString());
                return;
            }

            // Run every step even after a failure so one run reports all problems.
            boolean ok = backupDatabases(backupDir, timestamp, includeDatabase, includeUsageDb);
            ok &= backupDirectory(MANAGEMENT_CONFIG_PATH, backupDir, "management-config");
            ok &= backupDirectoryIfPresent(AGENT_CONFIG_PATH, backupDir, "agent-config");
            ok &= backupDirectoryIfPresent(SSL_CERT_PATH, backupDir, "ssl-certs");
            complete = ok && writeCompletionMarker(backupDir, timestamp);

            if (complete) {
                // Retention only after a complete set exists, and it only counts complete sets.
                cleanupOldBackups(infraBackupRoot, retentionCount);
                LOG.info("Infrastructure backup completed successfully: {}", backupDir);
            }
        } catch (Exception e) {
            LOG.error("Infrastructure backup failed: {}", e.getMessage(), e);
        } finally {
            if (!complete && dir.exists()) {
                LOG.error("Infrastructure backup {} is incomplete; removing it and skipping retention so existing backups are kept", backupDir);
                deleteDirectory(dir);
            }
            releaseRunLock(lock);
        }
    }

    /**
     * Creates {@code infra-backup/<ms>/<timestamp>} under the configured location, each level owner-only
     * (0700). The permissions are applied at creation, so there is no moment where the directory is open
     * to other users. Existing {@code infra-backup} and per-server directories, for example from an older
     * version, are tightened to 0700 as well, which also closes access to backups written before.
     */
    protected void createBackupDirectories(String location, String infraBackupRoot, String backupDir) throws IOException {
        Path locationPath = Paths.get(location);
        if (!Files.isDirectory(locationPath)) {
            Files.createDirectories(locationPath);
        }
        createOwnerOnlyDirectory(Paths.get(infraBackupRoot).getParent());
        createOwnerOnlyDirectory(Paths.get(infraBackupRoot));
        createOwnerOnlyDirectory(Paths.get(backupDir));
    }

    /** Creates {@code dir} with mode 0700, or restricts it to 0700 when it already exists. */
    protected void createOwnerOnlyDirectory(Path dir) throws IOException {
        if (Files.isDirectory(dir)) {
            restrictToOwner(dir, OWNER_ONLY_DIR);
            return;
        }
        try {
            Files.createDirectory(dir, ownerOnly(OWNER_ONLY_DIR));
        } catch (UnsupportedOperationException e) {
            Files.createDirectory(dir);
            warnNonPosix(dir);
        }
    }

    /** Creates an empty file with mode 0600 (replacing any existing one), so later writes never widen access. */
    protected Path createOwnerOnlyFile(Path file) throws IOException {
        Files.deleteIfExists(file);
        try {
            return Files.createFile(file, ownerOnly(OWNER_ONLY_FILE));
        } catch (UnsupportedOperationException e) {
            Files.createFile(file);
            warnNonPosix(file);
            return file;
        }
    }

    private static FileAttribute<Set<PosixFilePermission>> ownerOnly(Set<PosixFilePermission> perms) {
        return PosixFilePermissions.asFileAttribute(perms);
    }

    private void restrictToOwner(Path path, Set<PosixFilePermission> perms) {
        try {
            Files.setPosixFilePermissions(path, perms);
        } catch (UnsupportedOperationException e) {
            warnNonPosix(path);
        } catch (IOException e) {
            LOG.warn("Could not restrict {} to its owner: {}. Check that only the management server user can read the infrastructure backups.",
                    path, e.getMessage());
        }
    }

    private void warnNonPosix(Path path) {
        LOG.warn("The file system of {} does not support POSIX permissions, so the infrastructure backup cannot be made owner-only there. "
                + "The backups include db.properties and the encryption key; restrict access to nas.infra.backup.location on the NAS itself.", path);
    }

    /** Marks {@code backupDir} as a complete set; only marked sets count towards retention. */
    protected boolean writeCompletionMarker(String backupDir, String timestamp) {
        try {
            Path marker = createOwnerOnlyFile(Paths.get(backupDir, COMPLETION_MARKER));
            Files.write(marker, (timestamp + "\n").getBytes(StandardCharsets.UTF_8));
            return true;
        } catch (IOException e) {
            LOG.error("Failed to write the completion marker in {}: {}", backupDir, e.getMessage());
            return false;
        }
    }

    protected static boolean isCompleteBackup(File backup) {
        return new File(backup, COMPLETION_MARKER).isFile();
    }

    /**
     * Error text for a backup directory that could not be created. The usual cause is that the location
     * (typically a NAS mount) is owned by root while the management server runs as an unprivileged user,
     * so the message names that user and the fix.
     */
    protected String describeDirectoryCreationFailure(String backupDir, String location) {
        String user = System.getProperty("user.name", "cloud");
        return String.format("Failed to create infrastructure backup directory %s. The management server runs as user '%s', "
                + "which needs write access to %s (nas.infra.backup.location); for example, run 'chown %s %s' on the management "
                + "server after mounting the NAS there. Also check that the NAS is mounted at that path.",
                backupDir, user, location, user, location);
    }

    /**
     * Name of the sub-directory that keeps this management server's backups apart from those of the
     * other servers in the cluster. Management configs and certificates are per server, so they must
     * not overwrite each other, and the retention count applies per server. Uses the host name and
     * falls back to the management server id.
     */
    protected String getManagementServerLabel() {
        String label = null;
        try {
            label = InetAddress.getLocalHost().getHostName();
        } catch (UnknownHostException e) {
            LOG.debug("Could not determine the local host name for the infrastructure backup directory: {}", e.getMessage());
        }
        if (label == null || label.isBlank()) {
            label = "ms-" + ManagementServerNode.getManagementServerId();
        }
        return label.replaceAll("[^A-Za-z0-9._-]", "_");
    }

    /**
     * Dumps the cloud database, and the usage database when requested, into {@code backupDir}.
     * The database component is opt-in ({@code nas.infra.backup.include.database}): production
     * deployments typically run their own mysqldump jobs and leave it off; it exists for small and
     * edge deployments that want unified disaster recovery on the same NAS as their VM backups.
     *
     * @return false when a requested dump could not be made
     */
    protected boolean backupDatabases(String backupDir, String timestamp, boolean includeDatabase, boolean includeUsageDb) {
        if (!includeDatabase) {
            LOG.debug("Database backup skipped (nas.infra.backup.include.database=false). " +
                    "Manage DB backups externally for production deployments.");
            return true;
        }
        Properties dbProps = loadDbProperties();
        if (dbProps == null) {
            LOG.error("Database backup requested but the management server's db.properties could not be loaded");
            return false;
        }
        String dbHost = dbProps.getProperty("db.cloud.host", "localhost");
        String dbUser = dbProps.getProperty("db.cloud.username", "cloud");
        String dbPassword = dbProps.getProperty("db.cloud.password", "");

        boolean ok = backupDatabase("cloud", backupDir, timestamp, dbHost, dbUser, dbPassword);

        if (includeUsageDb) {
            String usageHost = dbProps.getProperty("db.usage.host", dbHost);
            String usageUser = dbProps.getProperty("db.usage.username", dbUser);
            String usagePassword = dbProps.getProperty("db.usage.password", dbPassword);
            ok &= backupDatabase("cloud_usage", backupDir, timestamp, usageHost, usageUser, usagePassword);
        }
        return ok;
    }

    /** Archives {@code sourcePath} like {@link #backupDirectory} but silently skips it when it does not exist on this server. */
    protected boolean backupDirectoryIfPresent(String sourcePath, String backupDir, String archiveName) {
        if (new File(sourcePath).exists()) {
            return backupDirectory(sourcePath, backupDir, archiveName);
        }
        return true;
    }

    /**
     * Acquire the cluster-wide run lock so only one management server performs the infrastructure
     * backup at a time. Returns null if the lock can't be taken (another MS holds it). Overridable
     * so unit tests can exercise runInContext without standing up the DB-backed lock manager.
     */
    protected GlobalLock acquireRunLock() {
        GlobalLock lock = GlobalLock.getInternLock("infra-backup");
        return (lock != null && lock.lock(5)) ? lock : null;
    }

    protected void releaseRunLock(GlobalLock lock) {
        if (lock != null) {
            lock.unlock();
        }
    }

    /**
     * The management server's database properties, already decrypted: when db.properties holds ENC(...)
     * values, reading the file directly would hand mysqldump the encrypted string and fail with
     * "Access denied". {@link DbProperties} applies the same decryption the server uses for its own pool.
     */
    protected Properties loadDbProperties() {
        try {
            Properties props = DbProperties.getDbProperties();
            if (props == null || props.isEmpty()) {
                LOG.warn("The management server's db.properties is empty or could not be found");
                return null;
            }
            return props;
        } catch (RuntimeException e) {
            LOG.error("Failed to load database properties: {}", e.getMessage());
            return null;
        }
    }

    /** Dumps {@code dbName} into an owner-only {@code .sql.gz}; returns false when the dump failed. */
    protected boolean backupDatabase(String dbName, String backupDir, String timestamp,
                                 String dbHost, String dbUser, String dbPassword) {
        String dumpFile = backupDir + "/" + dbName + "-" + timestamp + ".sql.gz";

        File defaultsFile = null;
        File rawDump = null;
        try {
            // Pass credentials via a 0600 --defaults-extra-file rather than the command line, so the
            // password never appears in the process list, and invoke mysqldump WITHOUT a shell so that
            // values read from db.properties cannot be interpreted as shell metacharacters (no injection).
            defaultsFile = writeMysqlDefaultsFile(dbHost, dbUser, dbPassword);

            rawDump = createOwnerOnlyFile(Paths.get(backupDir, dbName + "-" + timestamp + ".sql")).toFile();
            // --single-transaction = InnoDB hot backup (no table locks, consistent snapshot)
            ProcessBuilder pb = new ProcessBuilder(
                    "mysqldump",
                    "--defaults-extra-file=" + defaultsFile.getAbsolutePath(),
                    "--single-transaction", "--routines", "--triggers", "--events",
                    dbName);
            pb.redirectOutput(ProcessBuilder.Redirect.to(rawDump));
            pb.redirectError(ProcessBuilder.Redirect.INHERIT);
            Process process = pb.start();
            boolean completed = process.waitFor(300, TimeUnit.SECONDS);

            if (!completed) {
                process.destroyForcibly();
                LOG.error("Database backup timed out for {}", dbName);
                return false;
            }

            if (process.exitValue() != 0) {
                LOG.error("Database backup failed for {} with exit code {}", dbName, process.exitValue());
                return false;
            }

            // Compress in-process (no shell pipeline) into the final .sql.gz, created owner-only first.
            createOwnerOnlyFile(Paths.get(dumpFile));
            try (InputStream in = new FileInputStream(rawDump);
                 OutputStream out = new GZIPOutputStream(new FileOutputStream(dumpFile))) {
                in.transferTo(out);
            }

            File dump = new File(dumpFile);
            LOG.info("Database {} backed up: {} ({} bytes)", dbName, dumpFile, dump.length());
            return true;

        } catch (IOException | InterruptedException e) {
            LOG.error("Failed to backup database {}: {}", dbName, e.getMessage());
            if (e instanceof InterruptedException) {
                Thread.currentThread().interrupt();
            }
            return false;
        } finally {
            if (rawDump != null && rawDump.exists() && !rawDump.delete()) {
                LOG.warn("Failed to delete temporary dump file: {}", rawDump.getAbsolutePath());
            }
            if (defaultsFile != null && defaultsFile.exists() && !defaultsFile.delete()) {
                LOG.warn("Failed to delete temporary mysql defaults file: {}", defaultsFile.getAbsolutePath());
            }
        }
    }

    /**
     * Writes a temporary MySQL option file (restricted to mode 0600 where supported) holding the
     * connection credentials, so mysqldump can read them via {@code --defaults-extra-file}. Keeping
     * the password out of the argument list prevents it leaking through the process list.
     */
    private File writeMysqlDefaultsFile(String host, String user, String password) throws IOException {
        Path tmp = Files.createTempFile("cs-infra-mysqldump-", ".cnf");
        try {
            Files.setPosixFilePermissions(tmp,
                    EnumSet.of(PosixFilePermission.OWNER_READ, PosixFilePermission.OWNER_WRITE));
        } catch (UnsupportedOperationException ignored) {
            // Non-POSIX filesystem; createTempFile already restricts access to the owner.
        }
        String content = "[client]\n"
                + "host=" + (host == null ? "" : host) + "\n"
                + "user=" + (user == null ? "" : user) + "\n"
                + "password=" + (password == null ? "" : password) + "\n";
        try (Writer w = Files.newBufferedWriter(tmp, StandardCharsets.UTF_8)) {
            w.write(content);
        }
        return tmp.toFile();
    }

    /**
     * Archives {@code sourcePath} into an owner-only {@code <archiveName>.tar.gz}. Returns false when tar
     * failed; a missing source directory is skipped and is not a failure.
     */
    protected boolean backupDirectory(String sourcePath, String backupDir, String archiveName) {
        File source = new File(sourcePath);
        if (!source.exists() || !source.isDirectory()) {
            LOG.debug("Directory {} does not exist, skipping", sourcePath);
            return true;
        }

        String tarFile = backupDir + "/" + archiveName + ".tar.gz";
        String[] cmd = {"/bin/bash", "-c",
            String.format("tar czf '%s' -C '%s' .", tarFile, sourcePath)};

        try {
            // tar truncates the existing file and keeps its 0600 mode
            createOwnerOnlyFile(Paths.get(tarFile));
            ProcessBuilder pb = new ProcessBuilder(cmd);
            pb.redirectErrorStream(true);
            Process process = pb.start();
            boolean completed = process.waitFor(60, TimeUnit.SECONDS);

            if (completed && process.exitValue() == 0) {
                LOG.info("Directory {} backed up to {}", sourcePath, tarFile);
                return true;
            }
            if (!completed) {
                process.destroyForcibly();
            }
            LOG.error("Directory backup failed for {} (exit code: {})",
                sourcePath, completed ? process.exitValue() : "timeout");
            return false;
        } catch (IOException | InterruptedException e) {
            LOG.error("Failed to backup directory {}: {}", sourcePath, e.getMessage());
            if (e instanceof InterruptedException) {
                Thread.currentThread().interrupt();
            }
            return false;
        }
    }

    /**
     * Keeps the newest {@code retentionCount} complete backups under {@code infraBackupRoot} (this server's
     * directory) and deletes older complete ones. Sets without the completion marker never count, so failed
     * or partial runs cannot push good backups out. Unmarked sets (left by a crash, or written before the
     * marker existed) are removed only once a full window of complete sets exists, and only when they are
     * older than the oldest complete set that is kept.
     */
    protected void cleanupOldBackups(String infraBackupRoot, int retentionCount) {
        // A negative retention (misconfiguration) would make toDelete exceed backups.length below and
        // throw ArrayIndexOutOfBoundsException; clamp it so we never compute a delete count > available.
        if (retentionCount < 0) {
            retentionCount = 0;
        }
        File infraDir = new File(infraBackupRoot);
        if (!infraDir.exists()) {
            return;
        }

        File[] backups = infraDir.listFiles(File::isDirectory);
        if (backups == null) {
            return;
        }

        // Sort by name (timestamp-based), oldest first
        Arrays.sort(backups, Comparator.comparing(File::getName));
        List<File> complete = Arrays.stream(backups).filter(InfrastructureBackupTask::isCompleteBackup).collect(Collectors.toList());
        if (complete.isEmpty() || complete.size() < retentionCount) {
            return;
        }

        int toDelete = complete.size() - retentionCount;
        for (int i = 0; i < toDelete; i++) {
            LOG.info("Removing old infrastructure backup: {}", complete.get(i).getName());
            deleteDirectory(complete.get(i));
        }

        String oldestKept = complete.get(Math.min(toDelete, complete.size() - 1)).getName();
        for (File backup : backups) {
            if (!complete.contains(backup) && backup.getName().compareTo(oldestKept) < 0) {
                LOG.info("Removing incomplete infrastructure backup: {}", backup.getName());
                deleteDirectory(backup);
            }
        }
    }

    private void deleteDirectory(File dir) {
        try {
            // walkFileTree does NOT follow symbolic links by default: a symlink placed inside the
            // backup tree (NAS shares are often writable by multiple systems) is removed as a link,
            // its target is never descended into or deleted — so a delete can't escape the tree.
            Files.walkFileTree(dir.toPath(), new SimpleFileVisitor<Path>() {
                @Override
                public FileVisitResult visitFile(Path file, BasicFileAttributes attrs) throws IOException {
                    Files.deleteIfExists(file);
                    return FileVisitResult.CONTINUE;
                }

                @Override
                public FileVisitResult postVisitDirectory(Path visited, IOException exc) throws IOException {
                    Files.deleteIfExists(visited);
                    return FileVisitResult.CONTINUE;
                }
            });
        } catch (IOException e) {
            LOG.warn("Failed to delete directory {}: {}", dir.getAbsolutePath(), e.getMessage());
        }
    }
}
