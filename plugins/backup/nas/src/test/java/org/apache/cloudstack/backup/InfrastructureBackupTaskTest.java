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

import com.cloud.utils.crypt.EncryptionSecretKeyChecker;
import com.cloud.utils.db.DbProperties;
import com.cloud.utils.db.GlobalLock;

import java.io.File;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.attribute.PosixFilePermissions;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashSet;
import java.util.List;
import java.util.Properties;
import java.util.Set;
import java.util.concurrent.atomic.AtomicInteger;

import org.junit.After;
import org.junit.Assert;
import org.junit.Assume;
import org.junit.Before;
import org.junit.Test;
import org.mockito.MockedStatic;
import org.mockito.Mockito;

public class InfrastructureBackupTaskTest {

    private Path tmpRoot;

    @Before
    public void setUp() throws IOException {
        tmpRoot = Files.createTempDirectory("cs-infra-backup-test-");
    }

    @After
    public void tearDown() {
        if (tmpRoot != null) {
            deleteRecursively(tmpRoot.toFile());
        }
    }

    /** Captures backupDatabase/backupDirectory invocations instead of shelling out. */
    private static class RecordingTask extends InfrastructureBackupTask {
        boolean enabled = true;
        String location;
        int retention = 7;
        boolean databaseIncluded = false;
        boolean usageDbIncluded = true;
        Properties dbProps;
        boolean useRealDbPropertiesLoader = false;
        final List<String> databaseBackupPasswords = new ArrayList<>();
        final List<String> databaseBackupNames = new ArrayList<>();
        final List<String> directoryBackupNames = new ArrayList<>();
        final AtomicInteger retentionCalls = new AtomicInteger(0);

        final List<String> directoryBackupDirs = new ArrayList<>();
        final Set<String> failingDatabases = new HashSet<>();
        final Set<String> failingArchives = new HashSet<>();

        @Override
        protected String getManagementServerLabel() { return "ms-a"; }

        @Override
        protected boolean isEnabled() { return enabled; }
        @Override
        protected String getBackupLocation() { return location; }
        @Override
        protected int getRetentionCount() { return retention; }
        @Override
        protected boolean isDatabaseIncluded() { return databaseIncluded; }
        @Override
        protected boolean isUsageDbIncluded() { return usageDbIncluded; }

        @Override
        protected Properties loadDbProperties() {
            return useRealDbPropertiesLoader ? super.loadDbProperties() : dbProps;
        }

        @Override
        protected boolean backupDatabase(String dbName, String backupDir, String timestamp,
                                      String dbHost, String dbUser, String dbPassword) {
            databaseBackupNames.add(dbName);
            databaseBackupPasswords.add(dbPassword);
            return !failingDatabases.contains(dbName);
        }

        @Override
        protected boolean backupDirectory(String sourcePath, String backupDir, String archiveName) {
            directoryBackupNames.add(archiveName);
            directoryBackupDirs.add(backupDir);
            return !failingArchives.contains(archiveName);
        }

        @Override
        protected void cleanupOldBackups(String nasBackupPath, int retentionCount) {
            retentionCalls.incrementAndGet();
        }

        @Override
        protected GlobalLock acquireRunLock() {
            // in-memory interned lock only; never touches the DB-backed lock manager in unit tests
            return GlobalLock.getInternLock("infra-backup-test");
        }

        @Override
        protected void releaseRunLock(GlobalLock lock) {
            // no-op: the DB lock was never actually acquired in tests
        }
    }

    private static Properties stubDbProps() {
        Properties props = new Properties();
        props.setProperty("db.cloud.host", "localhost");
        props.setProperty("db.cloud.username", "cloud");
        props.setProperty("db.cloud.password", "secret");
        return props;
    }

    @Test
    public void doesNothingWhenMasterSwitchDisabled() {
        RecordingTask task = new RecordingTask();
        task.enabled = false;
        task.location = tmpRoot.toString();

        task.runInContext();

        Assert.assertTrue("no DB backups when feature disabled", task.databaseBackupNames.isEmpty());
        Assert.assertTrue("no dir backups when feature disabled", task.directoryBackupNames.isEmpty());
        Assert.assertEquals("no retention when feature disabled", 0, task.retentionCalls.get());
    }

    @Test
    public void doesNothingWhenLocationEmpty() {
        RecordingTask task = new RecordingTask();
        task.enabled = true;
        task.location = "";

        task.runInContext();

        Assert.assertTrue(task.databaseBackupNames.isEmpty());
        Assert.assertTrue(task.directoryBackupNames.isEmpty());
        Assert.assertEquals(0, task.retentionCalls.get());
    }

    @Test
    public void skipsDatabaseByDefault() {
        RecordingTask task = new RecordingTask();
        task.enabled = true;
        task.location = tmpRoot.toString();
        task.databaseIncluded = false;  // default
        task.dbProps = stubDbProps();

        task.runInContext();

        Assert.assertTrue("DB component is opt-in: no DB backups by default",
                task.databaseBackupNames.isEmpty());
        // Configs+certs still attempted (they're skipped silently if dirs absent)
        Assert.assertEquals("retention still runs", 1, task.retentionCalls.get());
    }

    @Test
    public void backsUpCloudDbWhenIncludeDatabaseTrue() {
        RecordingTask task = new RecordingTask();
        task.enabled = true;
        task.location = tmpRoot.toString();
        task.databaseIncluded = true;
        task.usageDbIncluded = false;  // suppress cloud_usage
        task.dbProps = stubDbProps();

        task.runInContext();

        Assert.assertEquals("only cloud DB backed up", 1, task.databaseBackupNames.size());
        Assert.assertEquals("cloud", task.databaseBackupNames.get(0));
    }

    @Test
    public void backsUpBothDbsWhenUsageEnabled() {
        RecordingTask task = new RecordingTask();
        task.enabled = true;
        task.location = tmpRoot.toString();
        task.databaseIncluded = true;
        task.usageDbIncluded = true;
        task.dbProps = stubDbProps();

        task.runInContext();

        Assert.assertEquals("both DBs backed up", 2, task.databaseBackupNames.size());
        Assert.assertEquals("cloud", task.databaseBackupNames.get(0));
        Assert.assertEquals("cloud_usage", task.databaseBackupNames.get(1));
    }

    @Test
    public void usageDbFlagIgnoredWhenDatabaseExcluded() {
        RecordingTask task = new RecordingTask();
        task.enabled = true;
        task.location = tmpRoot.toString();
        task.databaseIncluded = false;  // master DB gate off
        task.usageDbIncluded = true;    // should be ignored
        task.dbProps = stubDbProps();

        task.runInContext();

        Assert.assertTrue("usage DB requires include.database=true",
                task.databaseBackupNames.isEmpty());
    }

    @Test
    public void unreadablePropertiesFailTheRunWhenDatabaseRequested() {
        RecordingTask task = new RecordingTask();
        task.enabled = true;
        task.location = tmpRoot.toString();
        task.databaseIncluded = true;
        task.dbProps = null;  // simulate missing/unreadable db.properties

        task.runInContext();

        Assert.assertTrue("no DB backups attempted when props can't be loaded",
                task.databaseBackupNames.isEmpty());
        Assert.assertEquals("a run without the requested DB dump must not trigger retention",
                0, task.retentionCalls.get());
        Assert.assertEquals(0, backupSetsOf("ms-a").length);
    }

    @Test
    public void retentionRunsOnEverySuccessfulPass() {
        RecordingTask task = new RecordingTask();
        task.enabled = true;
        task.location = tmpRoot.toString();
        task.retention = 3;
        task.databaseIncluded = false;

        task.runInContext();

        Assert.assertEquals(1, task.retentionCalls.get());
    }

    @Test
    public void backupsAreWrittenUnderThisManagementServersDirectory() {
        RecordingTask task = new RecordingTask();
        task.location = tmpRoot.toString();
        task.runInContext();
        Assert.assertFalse(task.directoryBackupDirs.isEmpty());
        for (String dir : task.directoryBackupDirs) {
            Assert.assertTrue(dir, dir.startsWith(tmpRoot + "/infra-backup/ms-a/"));
        }
    }

    @Test
    public void retentionIsCountedPerManagementServer() throws IOException {
        File msA = new File(infraBackupDir(), "ms-a");
        File msB = new File(infraBackupDir(), "ms-b");
        for (String name : new String[] {"20240101-000000", "20240102-000000", "20240103-000000"}) {
            markComplete(new File(msA, name));
        }
        markComplete(new File(msB, "20240101-000000"));

        realTask.cleanupOldBackups(msA.toString(), 2);

        Assert.assertArrayEquals(new String[] {"20240102-000000", "20240103-000000"},
                Arrays.stream(msA.listFiles(File::isDirectory)).map(File::getName).sorted().toArray(String[]::new));
        Assert.assertEquals(1, msB.listFiles(File::isDirectory).length);
    }

    @Test
    public void dailyIntervalIs24Hours() {
        InfrastructureBackupTask task = new RecordingTask();
        Assert.assertEquals(Long.valueOf(86_400_000L), task.getDelay());
    }

    // ---- retention / delete logic (exercises the REAL cleanupOldBackups + deleteDirectory,
    //      which RecordingTask above stubs out) ----

    private final InfrastructureBackupTask realTask = new InfrastructureBackupTask();

    private File infraBackupDir() {
        return new File(tmpRoot.toFile(), "infra-backup");
    }

    private File makeBackup(String name) throws IOException {
        File dir = makeIncompleteBackup(name);
        Assert.assertTrue(new File(dir, InfrastructureBackupTask.COMPLETION_MARKER).createNewFile());
        return dir;
    }

    /** A set without the completion marker: a failed run left behind by a crash, or one written before the marker existed. */
    private File makeIncompleteBackup(String name) throws IOException {
        File dir = new File(infraBackupDir(), name);
        Assert.assertTrue(dir.mkdirs());
        Assert.assertTrue(new File(dir, "dump.sql.gz").createNewFile());
        return dir;
    }

    private static void markComplete(File dir) throws IOException {
        Assert.assertTrue(dir.mkdirs());
        Assert.assertTrue(new File(dir, InfrastructureBackupTask.COMPLETION_MARKER).createNewFile());
    }

    private File[] backupSetsOf(String ms) {
        File[] sets = new File(infraBackupDir(), ms).listFiles(File::isDirectory);
        return sets == null ? new File[0] : sets;
    }

    private static String mode(File f) throws IOException {
        return PosixFilePermissions.toString(Files.getPosixFilePermissions(f.toPath()));
    }

    private void assumePosix() {
        Assume.assumeTrue("needs a POSIX file system",
                tmpRoot.getFileSystem().supportedFileAttributeViews().contains("posix"));
    }

    // ---- review: a failed run must never count, and never trigger retention ----

    @Test
    public void successfulRunMarksTheSetComplete() {
        RecordingTask task = new RecordingTask();
        task.location = tmpRoot.toString();

        task.runInContext();

        File[] sets = backupSetsOf("ms-a");
        Assert.assertEquals(1, sets.length);
        Assert.assertTrue(InfrastructureBackupTask.isCompleteBackup(sets[0]));
        Assert.assertEquals(1, task.retentionCalls.get());
    }

    @Test
    public void failedDatabaseDumpSkipsRetentionAndRemovesThePartialSet() {
        RecordingTask task = new RecordingTask();
        task.location = tmpRoot.toString();
        task.databaseIncluded = true;
        task.dbProps = stubDbProps();
        task.failingDatabases.add("cloud_usage");

        task.runInContext();

        Assert.assertEquals(0, task.retentionCalls.get());
        Assert.assertEquals("the partial set is removed", 0, backupSetsOf("ms-a").length);
    }

    @Test
    public void failedArchiveSkipsRetentionAndRemovesThePartialSet() {
        RecordingTask task = new RecordingTask();
        task.location = tmpRoot.toString();
        task.failingArchives.add("management-config");

        task.runInContext();

        Assert.assertEquals(0, task.retentionCalls.get());
        Assert.assertEquals(0, backupSetsOf("ms-a").length);
    }

    @Test
    public void aWeekOfFailedRunsKeepsEveryGoodBackup() throws IOException {
        File msA = new File(infraBackupDir(), "ms-a");
        for (String name : new String[] {"20240101-000000", "20240102-000000", "20240103-000000"}) {
            markComplete(new File(msA, name));
        }
        // real retention this time, so the test sees what a failed run would delete
        RecordingTask task = new RecordingTask() {
            @Override
            protected void cleanupOldBackups(String root, int retentionCount) {
                retentionCalls.incrementAndGet();
                realTask.cleanupOldBackups(root, retentionCount);
            }
        };
        task.location = tmpRoot.toString();
        task.retention = 3;
        task.failingArchives.add("management-config");

        for (int day = 0; day < 7; day++) {
            task.runInContext();
        }

        Assert.assertEquals(0, task.retentionCalls.get());
        Assert.assertEquals(3, backupSetsOf("ms-a").length);
    }

    @Test
    public void cleanupCountsOnlyCompleteSets() throws IOException {
        makeBackup("20240101-000000");
        makeBackup("20240102-000000");
        makeBackup("20240103-000000");
        makeIncompleteBackup("20240104-000000");
        makeIncompleteBackup("20240105-000000");
        makeIncompleteBackup("20240106-000000");

        realTask.cleanupOldBackups(infraBackupDir().toString(), 3);

        Assert.assertEquals("incomplete sets do not push complete ones out", 6, remainingBackups().length);
    }

    @Test
    public void cleanupRemovesIncompleteSetsOlderThanTheKeptWindow() throws IOException {
        makeIncompleteBackup("20240101-000000");
        makeBackup("20240102-000000");
        makeIncompleteBackup("20240103-000000");
        makeBackup("20240104-000000");
        makeBackup("20240105-000000");
        makeIncompleteBackup("20240106-000000");

        realTask.cleanupOldBackups(infraBackupDir().toString(), 2);

        Assert.assertArrayEquals(new String[] {"20240104-000000", "20240105-000000", "20240106-000000"}, remainingBackups());
    }

    @Test
    public void unmarkedSetsFromBeforeTheUpgradeStayUntilAFullWindowIsComplete() throws IOException {
        for (int day = 1; day <= 7; day++) {
            makeIncompleteBackup("2024010" + day + "-000000");
        }
        makeBackup("20240108-000000");

        realTask.cleanupOldBackups(infraBackupDir().toString(), 7);

        Assert.assertEquals(8, remainingBackups().length);
    }

    // ---- review: backups hold db.properties and the key, so they are owner-only ----

    @Test
    public void backupDirectoriesAndMarkerAreOwnerOnly() throws IOException {
        assumePosix();
        RecordingTask task = new RecordingTask();
        task.location = tmpRoot.toString();

        task.runInContext();

        File set = backupSetsOf("ms-a")[0];
        Assert.assertEquals("rwx------", mode(infraBackupDir()));
        Assert.assertEquals("rwx------", mode(new File(infraBackupDir(), "ms-a")));
        Assert.assertEquals("rwx------", mode(set));
        Assert.assertEquals("rw-------", mode(new File(set, InfrastructureBackupTask.COMPLETION_MARKER)));
    }

    @Test
    public void existingBackupDirectoriesAreTightened() throws IOException {
        assumePosix();
        File msA = new File(infraBackupDir(), "ms-a");
        Assert.assertTrue(msA.mkdirs());
        Files.setPosixFilePermissions(infraBackupDir().toPath(), PosixFilePermissions.fromString("rwxr-xr-x"));
        Files.setPosixFilePermissions(msA.toPath(), PosixFilePermissions.fromString("rwxr-xr-x"));
        RecordingTask task = new RecordingTask();
        task.location = tmpRoot.toString();

        task.runInContext();

        Assert.assertEquals("rwx------", mode(infraBackupDir()));
        Assert.assertEquals("rwx------", mode(msA));
    }

    @Test
    public void configArchiveIsOwnerOnly() throws IOException {
        assumePosix();
        File source = new File(tmpRoot.toFile(), "management");
        Assert.assertTrue(source.mkdirs());
        Files.write(new File(source, "db.properties").toPath(), "db.cloud.password=secret\n".getBytes());
        File set = new File(tmpRoot.toFile(), "set");
        Assert.assertTrue(set.mkdirs());

        Assert.assertTrue(realTask.backupDirectory(source.getPath(), set.getPath(), "management-config"));

        File archive = new File(set, "management-config.tar.gz");
        Assert.assertTrue(archive.length() > 0);
        Assert.assertEquals("rw-------", mode(archive));
    }

    @Test
    public void ownerOnlyFileIsCreatedWithoutAWiderWindow() throws IOException {
        assumePosix();
        File file = new File(tmpRoot.toFile(), "cloud-20240101-000000.sql.gz");
        Assert.assertTrue(file.createNewFile());
        Files.setPosixFilePermissions(file.toPath(), PosixFilePermissions.fromString("rw-r--r--"));

        realTask.createOwnerOnlyFile(file.toPath());

        Assert.assertEquals("rw-------", mode(file));
    }

    private String[] remainingBackups() {
        File[] dirs = infraBackupDir().listFiles(File::isDirectory);
        if (dirs == null) {
            return new String[0];
        }
        return Arrays.stream(dirs).map(File::getName).sorted().toArray(String[]::new);
    }

    @Test
    public void cleanupClampsNegativeRetentionInsteadOfThrowing() throws IOException {
        makeBackup("20240101-000000");
        makeBackup("20240102-000000");
        makeBackup("20240103-000000");

        // A negative retention (misconfiguration) previously made toDelete (= count - retention)
        // larger than the array length and threw ArrayIndexOutOfBoundsException. It must now clamp
        // to 0 and simply remove everything, without throwing.
        realTask.cleanupOldBackups(infraBackupDir().toString(), -5);

        Assert.assertEquals(0, remainingBackups().length);
    }

    @Test
    public void cleanupKeepsNewestAndDeletesOldest() throws IOException {
        makeBackup("20240101-000000");
        makeBackup("20240102-000000");
        makeBackup("20240103-000000");
        makeBackup("20240104-000000");
        makeBackup("20240105-000000");

        realTask.cleanupOldBackups(infraBackupDir().toString(), 2);

        Assert.assertArrayEquals(new String[] {"20240104-000000", "20240105-000000"}, remainingBackups());
    }

    @Test
    public void deleteDoesNotFollowSymlinkOutOfTheBackupTree() throws IOException {
        // A file living OUTSIDE the backup tree, reachable only via a symlink inside an old backup.
        File outside = new File(tmpRoot.toFile(), "outside");
        Assert.assertTrue(outside.mkdirs());
        File precious = new File(outside, "precious.txt");
        Assert.assertTrue(precious.createNewFile());

        File oldest = makeBackup("20240101-000000");
        makeBackup("20240102-000000");
        makeBackup("20240103-000000");

        Path link = new File(oldest, "escape").toPath();
        try {
            Files.createSymbolicLink(link, outside.toPath());
        } catch (IOException | UnsupportedOperationException e) {
            Assume.assumeNoException("Filesystem does not support symbolic links", e);
        }

        // Retain only the newest; the two oldest (including the one holding the symlink) are deleted.
        realTask.cleanupOldBackups(infraBackupDir().toString(), 1);

        Assert.assertArrayEquals(new String[] {"20240103-000000"}, remainingBackups());
        // The symlink target and its contents MUST survive — the delete must not follow the link out.
        Assert.assertTrue("symlink target directory must survive", outside.exists());
        Assert.assertTrue("file behind the symlink must survive", precious.exists());
    }

    @Test
    public void encryptedDbPasswordIsDecryptedBeforeMysqldump() {
        // db.properties with db.cloud.encryption.type=file: the password is stored as ENC(...).
        EncryptionSecretKeyChecker.initEncryptor("unit-test-key");
        try {
            Properties onDisk = stubDbProps();
            onDisk.setProperty("db.cloud.password", "ENC(" + EncryptionSecretKeyChecker.getEncryptor().encrypt("s3cret") + ")");
            Properties loadedByServer = new Properties();
            loadedByServer.putAll(onDisk);
            EncryptionSecretKeyChecker.decryptAnyProperties(loadedByServer);

            RecordingTask task = new RecordingTask();
            task.location = tmpRoot.toString();
            task.databaseIncluded = true;
            task.usageDbIncluded = true;
            task.useRealDbPropertiesLoader = true;
            try (MockedStatic<DbProperties> dbProperties = Mockito.mockStatic(DbProperties.class)) {
                dbProperties.when(DbProperties::getDbProperties).thenReturn(loadedByServer);
                task.runInContext();
            }

            Assert.assertEquals(Arrays.asList("cloud", "cloud_usage"), task.databaseBackupNames);
            Assert.assertEquals("mysqldump must get the decrypted password, not the ENC(...) value",
                    Arrays.asList("s3cret", "s3cret"), task.databaseBackupPasswords);
        } finally {
            EncryptionSecretKeyChecker.resetEncryptor();
        }
    }

    @Test
    public void missingDbPropertiesSkipsOnlyTheDatabase() {
        RecordingTask task = new RecordingTask();
        task.location = tmpRoot.toString();
        task.databaseIncluded = true;
        task.useRealDbPropertiesLoader = true;
        try (MockedStatic<DbProperties> dbProperties = Mockito.mockStatic(DbProperties.class)) {
            dbProperties.when(DbProperties::getDbProperties).thenThrow(new IllegalStateException("Failed to load db.properties"));
            task.runInContext();
        }

        Assert.assertTrue(task.databaseBackupNames.isEmpty());
        Assert.assertFalse("configs are still attempted so the log shows every problem", task.directoryBackupNames.isEmpty());
        Assert.assertEquals("retention is skipped", 0, task.retentionCalls.get());
    }

    @Test
    public void unwritableLocationStopsTheRunWithAnActionableMessage() {
        Assume.assumeFalse("root can write anywhere", "root".equals(System.getProperty("user.name")));
        File readOnly = tmpRoot.toFile();
        Assert.assertTrue(readOnly.setWritable(false));
        try {
            RecordingTask task = new RecordingTask();
            task.location = readOnly.getAbsolutePath();

            task.runInContext();

            Assert.assertTrue("nothing is archived when the directory cannot be created", task.directoryBackupNames.isEmpty());
            Assert.assertEquals(0, task.retentionCalls.get());
        } finally {
            readOnly.setWritable(true);
        }

        String message = new InfrastructureBackupTask().describeDirectoryCreationFailure("/mnt/nas-backup/infra-backup/ms-a/20260909-130348", "/mnt/nas-backup");
        String user = System.getProperty("user.name");
        Assert.assertTrue(message, message.contains("runs as user '" + user + "'"));
        Assert.assertTrue(message, message.contains("write access to /mnt/nas-backup"));
        Assert.assertTrue(message, message.contains("chown " + user + " /mnt/nas-backup"));
    }

    private void deleteRecursively(File f) {
        File[] kids = f.listFiles();
        if (kids != null) {
            for (File k : kids) deleteRecursively(k);
        }
        f.delete();
    }
}
