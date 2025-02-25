//
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
//

package com.cloud.utils;

import java.io.File;
import java.io.IOException;
import java.net.URI;
import java.net.URISyntaxException;
import java.net.URL;
import java.nio.charset.StandardCharsets;
import java.nio.file.FileSystem;
import java.nio.file.FileSystems;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.Arrays;
import java.util.Collections;
import java.util.List;
import java.util.Objects;
import java.util.stream.Collectors;
import java.util.stream.Stream;

import org.apache.commons.io.FileUtils;
import org.apache.commons.io.IOUtils;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

import com.cloud.utils.exception.CloudRuntimeException;
import com.cloud.utils.ssh.SshHelper;

public class FileUtil {
    protected static Logger LOGGER = LogManager.getLogger(FileUtil.class);

    private static boolean deleteFileOrDirectory(File fileOrDirectory) {
        if (fileOrDirectory.isDirectory()) {
            File[] files = fileOrDirectory.listFiles();
            if (files != null) {
                for (File file : files) {
                    if (!deleteFileOrDirectory(file)) {
                        LOGGER.trace(String.format("Failed to delete file: %s", file.getAbsoluteFile()));
                        return false;
                    }
                }
            }
        }
        return fileOrDirectory.delete();
    }

    public static void copyfile(File source, File destination) throws IOException {
        FileUtils.copyFile(source, destination);
    }

    public static void scpPatchFiles(String controlIp, String destPath, int sshPort, File pemFile, String[] files, String basePath) {
        String finalErrMsg = "";
        List<String> srcFiles = Arrays.asList(files);
        srcFiles = srcFiles.stream()
                .map(file -> basePath + file) // Using Lambda notation to update the entries
                .collect(Collectors.toList());
        String[] newSrcFiles = srcFiles.toArray(new String[0]);
        for (int retries = 3; retries > 0; retries--) {
            try {
                SshHelper.scpTo(controlIp, sshPort, "root", pemFile, null,
                        destPath, newSrcFiles, "0755");
                return;
            } catch (Exception e) {
                finalErrMsg = String.format("Failed to scp files to system VM due to, %s",
                        e.getCause() != null ? e.getCause().getLocalizedMessage() : e.getLocalizedMessage());
                LOGGER.error(finalErrMsg);
            }
        }
        throw new CloudRuntimeException(finalErrMsg);
    }

    public static List<String> getFilesPathsUnderResourceDirectory(String resourceDirectory) {
        LOGGER.info(String.format("Searching for files under resource directory [%s].", resourceDirectory));

        URL resourceDirectoryUrl = Thread.currentThread().getContextClassLoader().getResource(resourceDirectory);
        if (resourceDirectoryUrl == null) {
            throw new CloudRuntimeException(String.format("Resource directory [%s] does not exist or is empty.", resourceDirectory));
        }

        URI resourceDirectoryUri;
        try {
            resourceDirectoryUri = resourceDirectoryUrl.toURI();
        } catch (URISyntaxException e) {
            throw new CloudRuntimeException(String.format("Unable to get URI from URL [%s].", resourceDirectoryUrl), e);
        }

        try (FileSystem fs = FileSystems.newFileSystem(resourceDirectoryUri, Collections.emptyMap());
                Stream<Path> paths = Files.walk(fs.getPath(resourceDirectory))) {
            return paths.filter(Files::isRegularFile).map(Path::toString).collect(Collectors.toList());
        } catch (IOException e) {
            throw new CloudRuntimeException(String.format("Error while trying to list files under resource directory [%s].", resourceDirectoryUri), e);
        }
    }

    public static void deletePath(String path) {
        if (StringUtils.isBlank(path)) {
            return;
        }
        File fileOrDirectory = new File(path);
        if (!fileOrDirectory.exists()) {
            return;
        }
        boolean result = deleteFileOrDirectory(fileOrDirectory);
        if (result) {
            LOGGER.debug(String.format("Deleted path: %s", path));
        } else  {
            LOGGER.error(String.format("Failed to delete path: %s", path));
        }
    }

    public static void deleteFiles(String directory, String prefix, String suffix) {
        Path dirPath = Paths.get(directory);
        try (Stream<Path> files = Files.list(dirPath)) {
            files.filter(file -> file.getFileName().toString().startsWith(prefix) &&
                            file.getFileName().toString().endsWith(suffix))
                    .forEach(file -> {
                        try {
                            Files.delete(file);
                            LOGGER.debug(String.format("Deleted file: %s", file));
                        } catch (IOException e) {
                            LOGGER.error(String.format("Failed to delete file: %s", file), e);
                        }
                    });
        } catch (IOException e) {
            LOGGER.error(String.format("Error accessing directory: %s", directory), e);
        }
    }

    public static boolean writeToFile(String fileName, String content) {
        Path filePath = Paths.get(fileName);
        try {
            Files.write(filePath, content.getBytes(StandardCharsets.UTF_8));
            LOGGER.debug(String.format("Successfully wrote to the file: %s", fileName));
            return true;
        } catch (IOException e) {
            LOGGER.error(String.format("Error writing to the file: %s", fileName), e);
        }
        return false;
    }

    public static String readResourceFile(String resource) throws IOException {
        return IOUtils.toString(Objects.requireNonNull(Thread.currentThread().getContextClassLoader().getResourceAsStream(resource)), com.cloud.utils.StringUtils.getPreferredCharset());
    }
}
