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

import com.cloud.utils.db.GlobalLock;

import org.apache.cloudstack.managed.context.ManagedContextRunnable;
import org.apache.cloudstack.poll.BackgroundPollTask;
import org.apache.logging.log4j.Logger;
import org.apache.logging.log4j.LogManager;

import java.io.BufferedReader;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.FileReader;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.io.Writer;
import java.nio.charset.StandardCharsets;
import java.nio.file.FileVisitResult;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.SimpleFileVisitor;
import java.nio.file.attribute.BasicFileAttributes;
import java.nio.file.attribute.PosixFilePermission;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.Arrays;
import java.util.Comparator;
import java.util.EnumSet;
import java.util.Properties;
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
 * Database credentials are read from /etc/cloudstack/management/db.properties.
 * Backups are stored under {nasBackupPath}/infra-backup/{timestamp}/ with
 * automatic retention management.
 */
public class InfrastructureBackupTask extends ManagedContextRunnable implements BackgroundPollTask {

    private static final Logger LOG = LogManager.getLogger(InfrastructureBackupTask.class);

    private static final String DB_PROPERTIES_PATH = "/etc/cloudstack/management/db.properties";
    private static final String MANAGEMENT_CONFIG_PATH = "/etc/cloudstack/management";
    private static final String AGENT_CONFIG_PATH = "/etc/cloudstack/agent";
    private static final String SSL_CERT_PATH = "/etc/cloudstack/management/cert";

    /** 24 hours in milliseconds */
    private static final long DAILY_INTERVAL_MS = 86400L * 1000L;

    private final NASBackupProvider provider;

    public InfrastructureBackupTask(NASBackupProvider provider) {
        this.provider = provider;
    }

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
        String backupDir = nasBackupPath + "/infra-backup/" + timestamp;

        LOG.info("Starting infrastructure backup to {} (database included: {})", backupDir, includeDatabase);

        // Cluster-wide lock: in multi-management-server deployments only one MS should run the
        // infrastructure backup at a time, otherwise they would write/delete concurrently in the
        // same NAS infra-backup path (duplicate backups, retention races, accidental deletes).
        GlobalLock lock = acquireRunLock();
        if (lock == null) {
            LOG.info("Another management server is performing the infrastructure backup; skipping this run");
            return;
        }

        try {
            File dir = new File(backupDir);
            if (!dir.exists() && !dir.mkdirs()) {
                LOG.error("Failed to create backup directory: {}", backupDir);
                return;
            }

            // 1 & 2. Database backup — opt-in via nas.infra.backup.include.database.
            // Production deployments typically run their own mysqldump cron jobs and disable this;
            // it exists for small/edge deployments wanting unified DR on the same NAS as VM backups.
            if (includeDatabase) {
                Properties dbProps = loadDbProperties();
                if (dbProps == null) {
                    LOG.error("Database backup requested but failed to load properties from {} — skipping DB component", DB_PROPERTIES_PATH);
                } else {
                    String dbHost = dbProps.getProperty("db.cloud.host", "localhost");
                    String dbUser = dbProps.getProperty("db.cloud.username", "cloud");
                    String dbPassword = dbProps.getProperty("db.cloud.password", "");

                    backupDatabase("cloud", backupDir, timestamp, dbHost, dbUser, dbPassword);

                    if (includeUsageDb) {
                        String usageHost = dbProps.getProperty("db.usage.host", dbHost);
                        String usageUser = dbProps.getProperty("db.usage.username", dbUser);
                        String usagePassword = dbProps.getProperty("db.usage.password", dbPassword);
                        backupDatabase("cloud_usage", backupDir, timestamp, usageHost, usageUser, usagePassword);
                    }
                }
            } else {
                LOG.debug("Database backup skipped (nas.infra.backup.include.database=false). " +
                        "Manage DB backups externally for production deployments.");
            }

            // 3. Backup management server configs
            backupDirectory(MANAGEMENT_CONFIG_PATH, backupDir, "management-config");

            // 4. Backup agent configs (if present on this host)
            File agentDir = new File(AGENT_CONFIG_PATH);
            if (agentDir.exists()) {
                backupDirectory(AGENT_CONFIG_PATH, backupDir, "agent-config");
            }

            // 5. Backup SSL certificates
            File sslDir = new File(SSL_CERT_PATH);
            if (sslDir.exists()) {
                backupDirectory(SSL_CERT_PATH, backupDir, "ssl-certs");
            }

            // 6. Cleanup old backups based on retention policy
            cleanupOldBackups(nasBackupPath, retentionCount);

            LOG.info("Infrastructure backup completed successfully: {}", backupDir);

        } catch (Exception e) {
            LOG.error("Infrastructure backup failed: {}", e.getMessage(), e);
        } finally {
            releaseRunLock(lock);
        }
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

    protected Properties loadDbProperties() {
        File propsFile = new File(DB_PROPERTIES_PATH);
        if (!propsFile.exists()) {
            LOG.warn("Database properties file not found: {}", DB_PROPERTIES_PATH);
            return null;
        }

        Properties props = new Properties();
        try (BufferedReader reader = new BufferedReader(new FileReader(propsFile))) {
            props.load(reader);
            return props;
        } catch (IOException e) {
            LOG.error("Failed to read database properties: {}", e.getMessage());
            return null;
        }
    }

    protected void backupDatabase(String dbName, String backupDir, String timestamp,
                                 String dbHost, String dbUser, String dbPassword) {
        String dumpFile = backupDir + "/" + dbName + "-" + timestamp + ".sql.gz";

        File defaultsFile = null;
        File rawDump = null;
        try {
            // Pass credentials via a 0600 --defaults-extra-file rather than the command line, so the
            // password never appears in the process list, and invoke mysqldump WITHOUT a shell so that
            // values read from db.properties cannot be interpreted as shell metacharacters (no injection).
            defaultsFile = writeMysqlDefaultsFile(dbHost, dbUser, dbPassword);

            rawDump = new File(backupDir, dbName + "-" + timestamp + ".sql");
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
                return;
            }

            if (process.exitValue() != 0) {
                LOG.error("Database backup failed for {} with exit code {}", dbName, process.exitValue());
                return;
            }

            // Compress in-process (no shell pipeline) into the final .sql.gz.
            try (InputStream in = new FileInputStream(rawDump);
                 OutputStream out = new GZIPOutputStream(new FileOutputStream(dumpFile))) {
                in.transferTo(out);
            }

            File dump = new File(dumpFile);
            LOG.info("Database {} backed up: {} ({} bytes)", dbName, dumpFile, dump.length());

        } catch (IOException | InterruptedException e) {
            LOG.error("Failed to backup database {}: {}", dbName, e.getMessage());
            if (e instanceof InterruptedException) {
                Thread.currentThread().interrupt();
            }
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

    protected void backupDirectory(String sourcePath, String backupDir, String archiveName) {
        File source = new File(sourcePath);
        if (!source.exists() || !source.isDirectory()) {
            LOG.debug("Directory {} does not exist, skipping", sourcePath);
            return;
        }

        String tarFile = backupDir + "/" + archiveName + ".tar.gz";
        String[] cmd = {"/bin/bash", "-c",
            String.format("tar czf '%s' -C '%s' .", tarFile, sourcePath)};

        try {
            ProcessBuilder pb = new ProcessBuilder(cmd);
            pb.redirectErrorStream(true);
            Process process = pb.start();
            boolean completed = process.waitFor(60, TimeUnit.SECONDS);

            if (completed && process.exitValue() == 0) {
                LOG.info("Directory {} backed up to {}", sourcePath, tarFile);
            } else {
                if (!completed) {
                    process.destroyForcibly();
                }
                LOG.warn("Directory backup failed for {} (exit code: {})",
                    sourcePath, completed ? process.exitValue() : "timeout");
            }
        } catch (IOException | InterruptedException e) {
            LOG.error("Failed to backup directory {}: {}", sourcePath, e.getMessage());
            if (e instanceof InterruptedException) {
                Thread.currentThread().interrupt();
            }
        }
    }

    protected void cleanupOldBackups(String nasBackupPath, int retentionCount) {
        // A negative retention (misconfiguration) would make toDelete exceed backups.length below and
        // throw ArrayIndexOutOfBoundsException; clamp it so we never compute a delete count > available.
        if (retentionCount < 0) {
            retentionCount = 0;
        }
        File infraDir = new File(nasBackupPath + "/infra-backup");
        if (!infraDir.exists()) {
            return;
        }

        File[] backups = infraDir.listFiles(File::isDirectory);
        if (backups == null || backups.length <= retentionCount) {
            return;
        }

        // Sort by name (timestamp-based), oldest first
        Arrays.sort(backups, Comparator.comparing(File::getName));

        int toDelete = backups.length - retentionCount;
        for (int i = 0; i < toDelete; i++) {
            LOG.info("Removing old infrastructure backup: {}", backups[i].getName());
            deleteDirectory(backups[i]);
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
