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
import java.nio.file.StandardOpenOption;
import java.util.function.LongSupplier;

import com.cloud.utils.exception.CloudRuntimeException;
import com.cloud.utils.net.MacAddress;

final class ManagementServerId {

    private ManagementServerId() {
    }

    static long get() {
        Path path = Paths.get(System.getProperty("management.server.id.file",
                Paths.get(System.getProperty("user.home"), ".cloudstack", "management-server.id").toString()));
        try {
            return load(path, System.getProperty("management.server.id"), () -> MacAddress.getMacAddress().toLong());
        } catch (IOException | IllegalArgumentException e) {
            throw new CloudRuntimeException("Unable to load the management server ID from " + path, e);
        }
    }

    static long load(Path path, String configuredId, LongSupplier initialId) throws IOException {
        if (configuredId != null) {
            return parse(configuredId);
        }

        Files.createDirectories(path.toAbsolutePath().getParent());
        try (FileChannel lockChannel = FileChannel.open(path.resolveSibling(path.getFileName() + ".lock"),
                StandardOpenOption.CREATE, StandardOpenOption.WRITE);
                FileLock lock = lockChannel.lock()) {
            if (Files.exists(path)) {
                if (Files.size(path) > 64) {
                    throw new IllegalArgumentException("Invalid management server ID file");
                }
                return parse(new String(Files.readAllBytes(path), StandardCharsets.UTF_8));
            }

            long id = parse(Long.toString(initialId.getAsLong()));
            try (FileChannel channel = FileChannel.open(path, StandardOpenOption.CREATE_NEW, StandardOpenOption.WRITE)) {
                ByteBuffer buffer = StandardCharsets.UTF_8.encode(id + "\n");
                while (buffer.hasRemaining()) {
                    channel.write(buffer);
                }
                channel.force(true);
            }
            return id;
        }
    }

    private static long parse(String value) {
        long id = Long.parseLong(value.trim());
        if (id <= 0) {
            throw new IllegalArgumentException("Management server ID must be a positive integer");
        }
        return id;
    }
}
