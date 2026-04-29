// Licensed to the Apache Software Foundation (ASF) under one
// or more contributor license agreements.  See the NOTICE file
// distributed with this work for additional information
// regarding copyright ownership.  The ASF licenses this file
// to you under the Apache License, Version 2.0 (the
// "License"); you may not use this file except in compliance
// the License.  You may obtain a copy of the License at
//
// http://www.apache.org/licenses/LICENSE-2.0
//
// Unless required by applicable law or agreed to in writing,
// software distributed under the License is distributed on an
// "AS IS" BASIS, WITHOUT WARRANTIES OR CONDITIONS OF ANY
// KIND, either express or implied.  See the License for the
// specific language governing permissions and limitations
// under the License.
package org.apache.cloudstack.utils.cryptsetup;

import java.io.Closeable;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.attribute.PosixFilePermission;
import java.nio.file.attribute.PosixFilePermissions;
import java.util.Set;

public class KeyFile implements Closeable {
    private Path filePath = null;

    /**
     * KeyFile represents a temporary file for storing data
     * to pass to commands, as an alternative to putting sensitive
     * data on the command line.
     * @param key byte array of content for the KeyFile
     * @throws IOException as the IOException for creating KeyFile
     */
    public KeyFile(byte[] key) throws IOException {
        if (key != null && key.length > 0) {
            Set<PosixFilePermission> permissions = PosixFilePermissions.fromString("rw-------");
            filePath = Files.createTempFile("keyfile", ".tmp", PosixFilePermissions.asFileAttribute(permissions));
            Files.write(filePath, key);
        }
    }

    public Path getPath() {
        return filePath;
    }

    public boolean isSet() {
        return filePath != null;
    }

    /**
     * Converts the keyfile to the absolute path String where it is located
     * @return absolute path as String
     */
    @Override
    public String toString() {
        if (filePath != null) {
            return filePath.toAbsolutePath().toString();
        }
        return "";
    }

    /**
     * Deletes the underlying key file
     * @throws IOException as the IOException for deleting the underlying key file
     */
    @Override
    public void close() throws IOException {
        if (isSet()) {
            Files.delete(filePath);
            filePath = null;
        }
    }
}
