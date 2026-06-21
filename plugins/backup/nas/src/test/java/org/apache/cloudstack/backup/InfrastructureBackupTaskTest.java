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

import java.io.File;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.Properties;
import java.util.concurrent.atomic.AtomicInteger;

import org.junit.After;
import org.junit.Assert;
import org.junit.Assume;
import org.junit.Before;
import org.junit.Test;

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
        final List<String> databaseBackupNames = new ArrayList<>();
        final List<String> directoryBackupNames = new ArrayList<>();
        final AtomicInteger retentionCalls = new AtomicInteger(0);

        RecordingTask() {
            super(null);
        }

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
            return dbProps;
        }

        @Override
        protected void backupDatabase(String dbName, String backupDir, String timestamp,
                                      String dbHost, String dbUser, String dbPassword) {
            databaseBackupNames.add(dbName);
        }

        @Override
        protected void backupDirectory(String sourcePath, String backupDir, String archiveName) {
            directoryBackupNames.add(archiveName);
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
    public void skipsDbBackupWhenPropertiesUnreadable() {
        RecordingTask task = new RecordingTask();
        task.enabled = true;
        task.location = tmpRoot.toString();
        task.databaseIncluded = true;
        task.dbProps = null;  // simulate missing/unreadable db.properties

        task.runInContext();

        Assert.assertTrue("no DB backups attempted when props can't be loaded",
                task.databaseBackupNames.isEmpty());
        Assert.assertEquals("retention still runs (configs+certs path unaffected)",
                1, task.retentionCalls.get());
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
    public void dailyIntervalIs24Hours() {
        InfrastructureBackupTask task = new RecordingTask();
        Assert.assertEquals(Long.valueOf(86_400_000L), task.getDelay());
    }

    // ---- retention / delete logic (exercises the REAL cleanupOldBackups + deleteDirectory,
    //      which RecordingTask above stubs out) ----

    private final InfrastructureBackupTask realTask = new InfrastructureBackupTask(null);

    private File infraBackupDir() {
        return new File(tmpRoot.toFile(), "infra-backup");
    }

    private File makeBackup(String name) throws IOException {
        File dir = new File(infraBackupDir(), name);
        Assert.assertTrue(dir.mkdirs());
        Assert.assertTrue(new File(dir, "dump.sql.gz").createNewFile());
        return dir;
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
        realTask.cleanupOldBackups(tmpRoot.toString(), -5);

        Assert.assertEquals(0, remainingBackups().length);
    }

    @Test
    public void cleanupKeepsNewestAndDeletesOldest() throws IOException {
        makeBackup("20240101-000000");
        makeBackup("20240102-000000");
        makeBackup("20240103-000000");
        makeBackup("20240104-000000");
        makeBackup("20240105-000000");

        realTask.cleanupOldBackups(tmpRoot.toString(), 2);

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
        realTask.cleanupOldBackups(tmpRoot.toString(), 1);

        Assert.assertArrayEquals(new String[] {"20240103-000000"}, remainingBackups());
        // The symlink target and its contents MUST survive — the delete must not follow the link out.
        Assert.assertTrue("symlink target directory must survive", outside.exists());
        Assert.assertTrue("file behind the symlink must survive", precious.exists());
    }

    private void deleteRecursively(File f) {
        File[] kids = f.listFiles();
        if (kids != null) {
            for (File k : kids) deleteRecursively(k);
        }
        f.delete();
    }
}
