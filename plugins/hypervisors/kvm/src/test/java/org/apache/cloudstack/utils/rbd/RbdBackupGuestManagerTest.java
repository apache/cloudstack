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
package org.apache.cloudstack.utils.rbd;

import java.io.File;
import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.util.ArrayList;
import java.util.List;

import org.junit.Assert;
import org.junit.Rule;
import org.junit.Test;
import org.junit.rules.TemporaryFolder;
import org.mockito.MockedStatic;
import org.mockito.Mockito;

import com.cloud.utils.script.Script;

public class RbdBackupGuestManagerTest {

    @Rule
    public TemporaryFolder folder = new TemporaryFolder();

    private File write(File directory, String name, String content) throws IOException {
        File file = new File(directory, name);
        Files.write(file.toPath(), content.getBytes(StandardCharsets.UTF_8));
        return file;
    }

    private List<List<String>> releaseAll(File directory, String domState) {
        List<List<String>> calls = new ArrayList<>();
        try (MockedStatic<Script> script = Mockito.mockStatic(Script.class)) {
            script.when(() -> Script.getExecutableAbsolutePath(Mockito.anyString())).thenAnswer(i -> i.getArgument(0));
            script.when(() -> Script.executeCommand(Mockito.any(String[].class))).thenReturn(domState);
            script.when(() -> Script.executeCommandForExitValue(Mockito.any(String[].class))).thenAnswer(invocation -> {
                List<String> call = new ArrayList<>();
                for (Object part : invocation.getArguments()) {
                    if (part instanceof String[]) {
                        call.addAll(java.util.Arrays.asList((String[]) part));
                    } else {
                        call.add(String.valueOf(part));
                    }
                }
                calls.add(call);
                return 0;
            });
            new RbdBackupGuestManager(directory).releaseAllGuests();
        }
        return calls;
    }

    @Test
    public void startingTheAgentReleasesEveryGuestAStateFileHolds() throws IOException {
        File dir = folder.newFolder();
        File paused = write(dir, "vm-a.guest", "suspended\n");
        File frozen = write(dir, "vm-b.guest", "frozen\n");

        List<List<String>> calls = releaseAll(dir, "paused\n");

        Assert.assertTrue(calls.stream().anyMatch(c -> c.contains("resume") && c.contains("vm-a")));
        Assert.assertTrue(calls.stream().anyMatch(c -> c.contains("qemu-agent-command") && c.contains("vm-b")));
        Assert.assertFalse(paused.exists());
        Assert.assertFalse(frozen.exists());
    }

    @Test
    public void startingTheAgentLeavesOtherFilesAndRunningGuestsAlone() throws IOException {
        File dir = folder.newFolder();
        File other = write(dir, "something-else.txt", "suspended\n");
        File stale = write(dir, "vm-c.guest", "suspended\n");

        List<List<String>> calls = releaseAll(dir, "running\n");

        Assert.assertTrue("no guest is resumed: " + calls, calls.isEmpty());
        Assert.assertTrue(other.exists());
        Assert.assertFalse("the stale state file is removed", stale.exists());
    }

    @Test
    public void aMissingDirectoryIsNoProblem() {
        Assert.assertTrue(releaseAll(new File(folder.getRoot(), "missing"), "paused\n").isEmpty());
    }
}
