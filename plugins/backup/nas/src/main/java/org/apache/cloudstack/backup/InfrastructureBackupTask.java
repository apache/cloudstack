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

import org.apache.cloudstack.managed.context.ManagedContextRunnable;
import org.apache.cloudstack.poll.BackgroundPollTask;
import org.apache.logging.log4j.Logger;
import org.apache.logging.log4j.LogManager;

import java.io.BufferedReader;
import java.io.File;
import java.io.FileReader;
import java.io.IOException;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.Arrays;
import java.util.Comparator;
import java.util.Properties;
import java.util.concurrent.TimeUnit;

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

    @Override
    protected void runInContext() {
        if (!Boolean.TRUE.equals(NASBackupProvider.NASInfraBackupEnabled.value())) {
            LOG.debug("Infrastructure backup is disabled (nas.infra.backup.enabled=false)");
            return;
        }

        String nasBackupPath = NASBackupProvider.NASInfraBackupLocation.value();
        if (nasBackupPath == null || nasBackupPath.isEmpty()) {
            LOG.error("Infrastructure backup location not configured (nas.infra.backup.location is empty)");
            return;
        }

        int retentionCount = NASBackupProvider.NASInfraBackupRetention.value();
        boolean includeUsageDb = Boolean.TRUE.equals(NASBackupProvider.NASInfraBackupUsageDb.value());

        String timestamp = LocalDateTime.now().format(DateTimeFormatter.ofPattern("yyyyMMdd-HHmmss"));
        String backupDir = nasBackupPath + "/infra-backup/" + timestamp;

        LOG.info("Starting infrastructure backup to {}", backupDir);

        try {
            File dir = new File(backupDir);
            if (!dir.mkdirs()) {
                LOG.error("Failed to create backup directory: {}", backupDir);
                return;
            }

            // Read database credentials from db.properties
            Properties dbProps = loadDbProperties();
            if (dbProps == null) {
                LOG.error("Failed to load database properties from {}", DB_PROPERTIES_PATH);
                return;
            }

            String dbHost = dbProps.getProperty("db.cloud.host", "localhost");
            String dbUser = dbProps.getProperty("db.cloud.username", "cloud");
            String dbPassword = dbProps.getProperty("db.cloud.password", "");

            // 1. Backup CloudStack database
            backupDatabase("cloud", backupDir, timestamp, dbHost, dbUser, dbPassword);

            // 2. Backup usage database if enabled
            if (includeUsageDb) {
                String usageHost = dbProps.getProperty("db.usage.host", dbHost);
                String usageUser = dbProps.getProperty("db.usage.username", dbUser);
                String usagePassword = dbProps.getProperty("db.usage.password", dbPassword);
                backupDatabase("cloud_usage", backupDir, timestamp, usageHost, usageUser, usagePassword);
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
        }
    }

    private Properties loadDbProperties() {
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

    private void backupDatabase(String dbName, String backupDir, String timestamp,
                                 String dbHost, String dbUser, String dbPassword) {
        String dumpFile = backupDir + "/" + dbName + "-" + timestamp + ".sql.gz";

        // Use --single-transaction for InnoDB hot backup (no table locks, consistent snapshot)
        String[] cmd = {"/bin/bash", "-c",
            String.format("mysqldump --single-transaction --routines --triggers --events " +
                "-h '%s' -u '%s' -p'%s' '%s' | gzip > '%s'",
                dbHost, dbUser, dbPassword, dbName, dumpFile)};

        try {
            ProcessBuilder pb = new ProcessBuilder(cmd);
            pb.redirectErrorStream(true);
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

            File dump = new File(dumpFile);
            LOG.info("Database {} backed up: {} ({} bytes)", dbName, dumpFile, dump.length());

        } catch (IOException | InterruptedException e) {
            LOG.error("Failed to backup database {}: {}", dbName, e.getMessage());
            if (e instanceof InterruptedException) {
                Thread.currentThread().interrupt();
            }
        }
    }

    private void backupDirectory(String sourcePath, String backupDir, String archiveName) {
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

    private void cleanupOldBackups(String nasBackupPath, int retentionCount) {
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
        File[] files = dir.listFiles();
        if (files != null) {
            for (File file : files) {
                if (file.isDirectory()) {
                    deleteDirectory(file);
                } else {
                    if (!file.delete()) {
                        LOG.warn("Failed to delete file: {}", file.getAbsolutePath());
                    }
                }
            }
        }
        if (!dir.delete()) {
            LOG.warn("Failed to delete directory: {}", dir.getAbsolutePath());
        }
    }
}
