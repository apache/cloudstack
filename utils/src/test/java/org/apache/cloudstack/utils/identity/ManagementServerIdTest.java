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

    @Rule
    public TemporaryFolder folder = new TemporaryFolder();

    private Path idFile() {
        return folder.getRoot().toPath().resolve("identity/management-server.id");
    }

    private LongSupplier unexpectedLookup() {
        return () -> {
            throw new AssertionError("Network interfaces must not be consulted");
        };
    }

    @Test
    public void preservesInitialIdWhenNetworkInterfacesChange() throws IOException {
        Path path = idFile();
        assertEquals(130014950130305L, ManagementServerId.load(path, null, () -> 130014950130305L));
        assertEquals(130014950130305L, ManagementServerId.load(path, null, () -> 42L));
        assertEquals(130014950130305L, ManagementServerId.load(path, null, unexpectedLookup()));
    }

    @Test
    public void configuredIdTakesPrecedenceOverSavedId() throws IOException {
        Path path = idFile();
        ManagementServerId.load(path, null, () -> 42L);
        assertEquals(123L, ManagementServerId.load(path, "123", unexpectedLookup()));
        assertEquals(42L, ManagementServerId.load(path, null, unexpectedLookup()));
    }

    @Test
    public void configuredIdDoesNotRequireWritableStorage() throws IOException {
        Path path = folder.newFile("not-a-directory").toPath().resolve("id");
        assertEquals(123L, ManagementServerId.load(path, "123", unexpectedLookup()));
    }

    @Test
    public void rejectsInvalidConfiguredIds() {
        for (String value : new String[] {"", "0", "-1", "invalid", "9223372036854775808"}) {
            assertThrows(IllegalArgumentException.class,
                    () -> ManagementServerId.load(idFile(), value, unexpectedLookup()));
        }
        assertFalse(Files.exists(idFile()));
    }

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

    @Test
    public void rejectsInvalidInitialIdsWithoutSavingThem() {
        for (long value : new long[] {0L, -1L}) {
            assertThrows(IllegalArgumentException.class,
                    () -> ManagementServerId.load(idFile(), null, () -> value));
        }
        assertFalse(Files.exists(idFile()));
    }

    @Test
    public void failsWhenIdentityCannotBeSaved() throws IOException {
        Path path = folder.newFile("not-a-directory").toPath().resolve("id");
        assertThrows(IOException.class, () -> ManagementServerId.load(path, null, unexpectedLookup()));
    }

    @Test
    public void failedInitialWriteDoesNotPublishAnInvalidIdentity() throws IOException {
        Path path = idFile();
        AtomicReference<FileChannel> underlyingWriter = new AtomicReference<>();
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
            assertEquals(1L, files.count()); // Only the interprocess lock remains.
        }
        assertEquals(43L, ManagementServerId.load(path, null, () -> 43L));
    }

    @Test
    public void readsWhitespaceAndFullLongIds() throws IOException {
        Path path = folder.newFile("id").toPath();
        Files.writeString(path, "  " + Long.MAX_VALUE + "\n", StandardCharsets.UTF_8);
        assertEquals(Long.MAX_VALUE, ManagementServerId.load(path, null, unexpectedLookup()));
    }

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

    public static class IdWriter {
        public static void main(String[] args) throws IOException {
            System.out.println(ManagementServerId.load(Paths.get(args[0]), null, () -> Long.parseLong(args[1])));
        }
    }

    private void restoreProperty(String name, String value) {
        if (value == null) {
            System.clearProperty(name);
        } else {
            System.setProperty(name, value);
        }
    }
}
