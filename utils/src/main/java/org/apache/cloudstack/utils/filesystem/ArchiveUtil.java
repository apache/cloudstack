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

package org.apache.cloudstack.utils.filesystem;

import java.io.File;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.stream.Stream;
import java.util.zip.ZipEntry;
import java.util.zip.ZipOutputStream;

import com.cloud.utils.script.Script;

public class ArchiveUtil {

    public enum ArchiveFormat {
        TGZ("tar", "tar"),
        ZIP("zip", "unzip");

        private final String packToolName;
        private final String extractToolName;

        ArchiveFormat(String packToolName, String extractToolName) {
            this.packToolName = packToolName;
            this.extractToolName = extractToolName;
        }

        public String getPackToolName() {
            return packToolName;
        }

        public String getExtractToolName() {
            return extractToolName;
        }
    }

    private static String[] getPackCommandParams(ArchiveFormat format, Path sourcePath, Path archivePath) {
        String toolPath = Script.getExecutableAbsolutePath(format.getPackToolName());
        if (format == ArchiveFormat.ZIP) {
            return new String[]{
                    toolPath,
                    "-r",
                    archivePath.toAbsolutePath().toString(),
                    sourcePath.toAbsolutePath().toString()
            };
        }
        return new String[]{
                toolPath,
                "-czpf", archivePath.toAbsolutePath().toString(),
                "-C", sourcePath.toAbsolutePath().toString(),
                "."
        };
    }

    private static String[] getExtractCommandParams(ArchiveFormat format, Path archive, Path destinationPath) {
        String toolPath = Script.getExecutableAbsolutePath(format.getExtractToolName());
        if (format == ArchiveFormat.ZIP) {
            return new String[]{
                    toolPath,
                    archive.toAbsolutePath().toString(),
                    "-d", destinationPath.toAbsolutePath().toString()
            };
        }
        return new String[]{
                toolPath,
                "-xpf", archive.toAbsolutePath().toString(),
                "-C", destinationPath.toAbsolutePath().toString()
        };
    }

    protected static boolean packDirectoryPathUsingJavaZip(Path sourcePath, Path archivePath) {
        final AtomicBoolean failed = new AtomicBoolean(false);
        if (!Files.exists(sourcePath)) {
            return false;
        }

        try (ZipOutputStream zs = new ZipOutputStream(Files.newOutputStream(archivePath))) {
            if (Files.isDirectory(sourcePath)) {
                try (Stream<Path> stream = Files.walk(sourcePath)) {
                    stream.filter(path -> !Files.isDirectory(path))
                            .forEach(path -> {
                                if (failed.get()) {
                                    return;
                                }
                                try {
                                    Path relativePath = sourcePath.relativize(path);
                                    String entryName = relativePath.toString().replace(File.separatorChar, '/');
                                    ZipEntry zipEntry = new ZipEntry(entryName);
                                    zs.putNextEntry(zipEntry);
                                    Files.copy(path, zs);
                                    zs.closeEntry();
                                } catch (IOException e) {
                                    failed.set(true);
                                }
                            });
                }
            } else if (Files.isRegularFile(sourcePath)) {
                try {
                    String entryName = sourcePath.getFileName().toString().replace(File.separatorChar, '/');
                    ZipEntry zipEntry = new ZipEntry(entryName);
                    zs.putNextEntry(zipEntry);
                    Files.copy(sourcePath, zs);
                    zs.closeEntry();
                } catch (IOException e) {
                    return false;
                }
            } else {
                return false;
            }
        } catch (IOException e) {
            return false;
        }
        return !failed.get();
    }

    public static boolean packPath(ArchiveFormat format, Path sourcePath, Path archivePath, int timeoutSeconds) {
        if (format == ArchiveFormat.ZIP) {
            return packDirectoryPathUsingJavaZip(sourcePath, archivePath);
        }
        int result = Script.executeCommandForExitValue(
                timeoutSeconds * 1000L,
                getPackCommandParams(format, sourcePath, archivePath)
        );
        return result == 0;
    }

    public static boolean extractToPath(ArchiveFormat format, Path archivePath, Path destinationPath, int timeoutSeconds) {
        int result = Script.executeCommandForExitValue(
                timeoutSeconds * 1000L,
                getExtractCommandParams(format, archivePath, destinationPath)
        );
        return result == 0;
    }
}
