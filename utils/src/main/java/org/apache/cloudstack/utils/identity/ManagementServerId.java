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

import java.io.IOException;
import java.nio.ByteBuffer;
import java.nio.channels.FileChannel;
import java.nio.channels.FileLock;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.nio.file.StandardCopyOption;
import java.nio.file.StandardOpenOption;
import java.util.function.LongSupplier;

import com.cloud.utils.exception.CloudRuntimeException;
import com.cloud.utils.net.MacAddress;

// Keeps the management server ID the same across restarts
final class ManagementServerId {

    private ManagementServerId() {
    }

    // Find the ID file and load the ID
    static long get() {
        Path path = Paths.get(System.getProperty("management.server.id.file",
                Paths.get(System.getProperty("user.home"), ".cloudstack", "management-server.id").toString()));
        try {
            // Use the ID from management.server.id if it is set
            // Otherwise, read the saved ID from the file at path
            // If the file does not exist, create an ID from the MAC address and save it
            return load(path, System.getProperty("management.server.id"), () -> MacAddress.getMacAddress().toLong());
        } catch (IOException | IllegalArgumentException e) {
            // Stop with a clear error instead of silently choosing a different identity
            throw new CloudRuntimeException("Unable to load the management server ID from " + path, e);
        }
    }

    static long load(Path path, String configuredId, LongSupplier initialId) throws IOException {
        // An operator-provided ID takes priority without overwriting the saved file
        if (configuredId != null) {
            return parse(configuredId);
        }

        // Ensure the directory exists, then lock the neighboring .lock file so
        // two processes cannot initialize the same identity file at the same time
        Files.createDirectories(path.toAbsolutePath().getParent());
        try (FileChannel lockChannel = FileChannel.open(path.resolveSibling(path.getFileName() + ".lock"),
                StandardOpenOption.CREATE, StandardOpenOption.WRITE);
                FileLock lock = lockChannel.lock()) {
            // Reuse the saved ID without checking network interfaces again
            if (Files.exists(path)) {
                // An ID is a short number; reject an unexpectedly large file
                if (Files.size(path) > 64) {
                    throw new IllegalArgumentException("Invalid management server ID file");
                }
                return parse(new String(Files.readAllBytes(path), StandardCharsets.UTF_8));
            }

            // First startup: obtain the initial MAC-derived ID and validate it
            long id = parse(Long.toString(initialId.getAsLong()));
            // Write in the same directory so the completed file can be renamed atomically
            Path temporary = Files.createTempFile(path.toAbsolutePath().getParent(), "management-server-id-", ".tmp");
            try {
                try (FileChannel channel = FileChannel.open(temporary, StandardOpenOption.WRITE)) {
                    ByteBuffer buffer = StandardCharsets.UTF_8.encode(id + "\n");
                    while (buffer.hasRemaining()) {
                        channel.write(buffer);
                    }
                    // Flush the complete ID to storage before publishing it
                    channel.force(true);
                }
                // Make the completed file appear under its final name in one operation
                Files.move(temporary, path, StandardCopyOption.ATOMIC_MOVE);
            } finally {
                // Clean up after a failed save; a successful move already removed this name
                Files.deleteIfExists(temporary);
            }
            return id;
        }
    }

    // Ignore surrounding whitespace and require a positive whole number that fits in a Java long
    private static long parse(String value) {
        long id = Long.parseLong(value.trim());
        if (id <= 0) {
            throw new IllegalArgumentException("Management server ID must be a positive integer");
        }
        return id;
    }
}
