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

package org.apache.cloudstack.utils.identity;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertThrows;
import static org.junit.Assert.assertTrue;

import java.io.IOException;
import java.nio.ByteBuffer;
import java.nio.channels.FileChannel;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicReference;
import java.util.function.LongSupplier;
import java.util.stream.Stream;

import org.junit.Rule;
import org.junit.Test;
import org.junit.rules.TemporaryFolder;
import org.mockito.MockedStatic;
import org.mockito.Mockito;

import com.cloud.utils.exception.CloudRuntimeException;

public class ManagementServerIdTest {

    // Give each test its own temporary folder
    @Rule
    public TemporaryFolder folder = new TemporaryFolder();

    // Choose where this test will save its ID
    private Path idFile() {
        return folder.getRoot().toPath().resolve("identity/management-server.id");
    }

    // Fail the test if the code asks for a new ID when it should not
    private LongSupplier unexpectedLookup() {
        return () -> {
            throw new AssertionError("Network interfaces must not be consulted");
        };
    }

    // Keep the saved ID even when the network card would give a different ID
    @Test
    public void preservesInitialIdWhenNetworkInterfacesChange() throws IOException {
        Path path = idFile();
        assertEquals(130014950130305L, ManagementServerId.load(path, null, () -> 130014950130305L));
        assertEquals(130014950130305L, ManagementServerId.load(path, null, () -> 42L));
        assertEquals(130014950130305L, ManagementServerId.load(path, null, unexpectedLookup()));
    }

    // Use the chosen ID first and keep the saved ID for later
    @Test
    public void configuredIdTakesPrecedenceOverSavedId() throws IOException {
        Path path = idFile();
        ManagementServerId.load(path, null, () -> 42L);
        assertEquals(123L, ManagementServerId.load(path, "123", unexpectedLookup()));
        assertEquals(42L, ManagementServerId.load(path, null, unexpectedLookup()));
    }

    // A chosen ID still works when we cannot save a file
    @Test
    public void configuredIdDoesNotRequireWritableStorage() throws IOException {
        Path path = folder.newFile("not-a-directory").toPath().resolve("id");
        assertEquals(123L, ManagementServerId.load(path, "123", unexpectedLookup()));
    }

    // Reject bad IDs from the settings and do not save them
    @Test
    public void rejectsInvalidConfiguredIds() {
        for (String value : new String[] {"", "0", "-1", "invalid", "9223372036854775808"}) {
            assertThrows(IllegalArgumentException.class,
                    () -> ManagementServerId.load(idFile(), value, unexpectedLookup()));
        }
        assertFalse(Files.exists(idFile()));
    }

    // Reject a bad saved ID without changing its file
    @Test
    public void rejectsInvalidSavedIdsWithoutReplacingThem() throws IOException {
        Path path = folder.newFile("id").toPath();
        for (String value : new String[] {"", "0", "-1", "invalid", "9223372036854775808", "1".repeat(65)}) {
            Files.write(path, value.getBytes(StandardCharsets.UTF_8));
            assertThrows(IllegalArgumentException.class,
                    () -> ManagementServerId.load(path, null, unexpectedLookup()));
            assertEquals(value, Files.readString(path));
        }
    }

    // Do not save a new ID if it is zero or negative
    @Test
    public void rejectsInvalidInitialIdsWithoutSavingThem() {
        for (long value : new long[] {0L, -1L}) {
            assertThrows(IllegalArgumentException.class,
                    () -> ManagementServerId.load(idFile(), null, () -> value));
        }
        assertFalse(Files.exists(idFile()));
    }

    // Report an error when the ID file cannot be saved
    @Test
    public void failsWhenIdentityCannotBeSaved() throws IOException {
        Path path = folder.newFile("not-a-directory").toPath().resolve("id");
        assertThrows(IOException.class, () -> ManagementServerId.load(path, null, unexpectedLookup()));
    }

    // A failed write leaves no broken ID file and a later try can succeed
    @Test
    public void failedInitialWriteDoesNotPublishAnInvalidIdentity() throws IOException {
        Path path = idFile();
        AtomicReference<FileChannel> underlyingWriter = new AtomicReference<>();
        // Make the file writer fail on purpose
        FileChannel failedWriter = Mockito.mock(FileChannel.class);
        Mockito.when(failedWriter.write(Mockito.any(ByteBuffer.class))).thenThrow(new IOException("Interrupted write"));
        Mockito.doAnswer(invocation -> {
            underlyingWriter.get().close();
            return null;
        }).when(failedWriter).close();

        try (MockedStatic<FileChannel> channels = Mockito.mockStatic(FileChannel.class, invocation -> {
            FileChannel channel = (FileChannel) invocation.callRealMethod();
            Path openedPath = invocation.getArgument(0);
            if (openedPath.getFileName().toString().endsWith(".lock")) {
                return channel;
            }
            underlyingWriter.compareAndSet(null, channel);
            return failedWriter;
        })) {
            assertThrows(IOException.class, () -> ManagementServerId.load(path, null, () -> 42L));
        }

        assertFalse(Files.exists(path));
        try (Stream<Path> files = Files.list(path.getParent())) {
            assertEquals(1L, files.count()); // Only the interprocess lock remains
        }
        assertEquals(43L, ManagementServerId.load(path, null, () -> 43L));
    }

    // Allow spaces around a valid ID and accept the biggest allowed number
    @Test
    public void readsWhitespaceAndFullLongIds() throws IOException {
        Path path = folder.newFile("id").toPath();
        Files.writeString(path, "  " + Long.MAX_VALUE + "\n", StandardCharsets.UTF_8);
        assertEquals(Long.MAX_VALUE, ManagementServerId.load(path, null, unexpectedLookup()));
    }

    // Use the chosen file and ID settings and report a bad saved file
    @Test
    public void usesJvmSettingsAndReportsInvalidStorage() throws IOException {
        String previousFile = System.getProperty("management.server.id.file");
        String previousId = System.getProperty("management.server.id");
        Path path = folder.newFile("id").toPath();
        try {
            System.setProperty("management.server.id.file", path.toString());
            System.clearProperty("management.server.id");
            Files.writeString(path, "42\n", StandardCharsets.UTF_8);
            assertEquals(42L, ManagementServerId.get());

            System.setProperty("management.server.id", "123");
            assertEquals(123L, ManagementServerId.get());

            System.clearProperty("management.server.id");
            Files.writeString(path, "invalid", StandardCharsets.UTF_8);
            assertThrows(CloudRuntimeException.class, ManagementServerId::get);
        } finally {
            restoreProperty("management.server.id.file", previousFile);
            restoreProperty("management.server.id", previousId);
        }
    }

    // Three programs using the same file must all get the same ID
    @Test
    public void concurrentProcessesUseTheSameId() throws Exception {
        Path path = idFile();
        List<Process> processes = new ArrayList<>();
        try {
            for (int i = 1; i <= 3; i++) {
                processes.add(new ProcessBuilder(Paths.get(System.getProperty("java.home"), "bin", "java").toString(),
                        "-cp", System.getProperty("java.class.path"), IdWriter.class.getName(), path.toString(), Integer.toString(i))
                        .redirectErrorStream(true).start());
            }
            for (Process process : processes) {
                assertTrue("ID writer timed out", process.waitFor(30, TimeUnit.SECONDS));
                String output = new String(process.getInputStream().readAllBytes(), StandardCharsets.UTF_8).trim();
                assertEquals(output, 0, process.exitValue());
                assertEquals(Files.readString(path).trim(), output);
            }
        } finally {
            for (Process process : processes) {
                process.destroyForcibly();
            }
        }
    }

    // A small helper program that loads an ID and prints it
    public static class IdWriter {
        public static void main(String[] args) throws IOException {
            System.out.println(ManagementServerId.load(Paths.get(args[0]), null, () -> Long.parseLong(args[1])));
        }
    }

    // Put the settings back so one test does not affect another
    private void restoreProperty(String name, String value) {
        if (value == null) {
            System.clearProperty(name);
        } else {
            System.setProperty(name, value);
        }
    }
}
