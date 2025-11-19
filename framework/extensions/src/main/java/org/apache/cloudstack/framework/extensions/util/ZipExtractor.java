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

package org.apache.cloudstack.framework.extensions.util;

import java.io.IOException;
import java.io.OutputStream;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.zip.ZipEntry;
import java.util.zip.ZipInputStream;

public class ZipExtractor {

    /**
     * Extracts a GitHub ZIP file contents directly into destDir, skipping top-level folder.
     *
     * @param zipFilePath Path to the ZIP file
     * @param destDir     Destination directory
     */
    public static void extractZipContents(String zipFilePath, String destDir) throws IOException {
        Path destPath = Paths.get(destDir);
        if (!Files.exists(destPath)) {
            Files.createDirectories(destPath);
        }

        try (ZipInputStream zis = new ZipInputStream(Files.newInputStream(Paths.get(zipFilePath)))) {
            ZipEntry entry;

            while ((entry = zis.getNextEntry()) != null) {
                String entryName = entry.getName();

                // Skip the top-level folder (everything before first '/')
                int firstSlash = entryName.indexOf('/');
                if (firstSlash >= 0) {
                    entryName = entryName.substring(firstSlash + 1);
                }

                if (entryName.isEmpty()) {
                    zis.closeEntry();
                    continue; // skip the top-level folder itself
                }

                Path newPath = safeResolve(destPath, entryName);

                if (entry.isDirectory()) {
                    Files.createDirectories(newPath);
                } else {
                    if (newPath.getParent() != null) {
                        Files.createDirectories(newPath.getParent());
                    }
                    try (OutputStream os = Files.newOutputStream(newPath)) {
                        zis.transferTo(os);
                    }
                }

                zis.closeEntry();
            }
        }
    }

    /**
     * Protects from ZIP Slip vulnerability.
     */
    private static Path safeResolve(Path destDir, String entryName) throws IOException {
        Path resolved = destDir.resolve(entryName).normalize();
        if (!resolved.startsWith(destDir)) {
            throw new IOException("ZIP entry outside target dir: " + entryName);
        }
        return resolved;
    }
}
