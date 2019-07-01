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
package org.apache.cloudstack.diagnostics;

import java.io.File;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.nio.file.attribute.BasicFileAttributeView;
import java.nio.file.attribute.BasicFileAttributes;
import java.nio.file.attribute.FileTime;
import java.nio.file.attribute.PosixFileAttributes;
import java.nio.file.attribute.PosixFilePermission;
import java.util.Set;

import org.apache.commons.lang3.StringUtils;
import org.apache.log4j.Logger;

import com.cloud.utils.script.Script2;

public class DiagnosticsHelper {
    private static final Logger LOGGER = Logger.getLogger(DiagnosticsHelper.class);

    public static void setDirFilePermissions(Path path) throws java.io.IOException {
        Set<PosixFilePermission> perms = Files.readAttributes(path, PosixFileAttributes.class).permissions();
        perms.add(PosixFilePermission.OWNER_WRITE);
        perms.add(PosixFilePermission.OWNER_READ);
        perms.add(PosixFilePermission.OWNER_EXECUTE);
        perms.add(PosixFilePermission.GROUP_WRITE);
        perms.add(PosixFilePermission.GROUP_READ);
        perms.add(PosixFilePermission.GROUP_EXECUTE);
        perms.add(PosixFilePermission.OTHERS_WRITE);
        perms.add(PosixFilePermission.OTHERS_READ);
        perms.add(PosixFilePermission.OTHERS_EXECUTE);
        Files.setPosixFilePermissions(path, perms);
    }

    public static void umountSecondaryStorage(String mountPoint) {
        if (StringUtils.isNotBlank(mountPoint)) {
            Script2 umountCmd = new Script2("/bin/bash", LOGGER);
            umountCmd.add("-c");
            String cmdLine = String.format("umount %s", mountPoint);
            umountCmd.add(cmdLine);
            umountCmd.execute();
        }
    }

    public static Long getFileCreationTime(File file) throws IOException {
        Path p = Paths.get(file.getAbsolutePath());
        BasicFileAttributes view = Files.getFileAttributeView(p, BasicFileAttributeView.class).readAttributes();
        FileTime fileTime = view.creationTime();
        return fileTime.toMillis();
    }

    public static Long getTimeDifference(File f) {
        Long fileCreationTime = null;
        try {
            fileCreationTime = getFileCreationTime(f);
        } catch (IOException e) {
            LOGGER.error("File not found: " + e);
        }
        return (fileCreationTime != null) ? (System.currentTimeMillis() - fileCreationTime) / 1000 : 1L;
    }
}
