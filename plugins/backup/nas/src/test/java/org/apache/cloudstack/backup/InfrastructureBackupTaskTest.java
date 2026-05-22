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

import java.io.File;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.List;
import java.util.Properties;
import java.util.concurrent.atomic.AtomicInteger;

import org.junit.After;
import org.junit.Assert;
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

    private void deleteRecursively(File f) {
        File[] kids = f.listFiles();
        if (kids != null) {
            for (File k : kids) deleteRecursively(k);
        }
        f.delete();
    }
}
