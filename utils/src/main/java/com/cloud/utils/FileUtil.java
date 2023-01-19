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
import java.util.Arrays;
import java.util.List;
import java.util.stream.Collectors;

import com.cloud.utils.exception.CloudRuntimeException;
import com.cloud.utils.ssh.SshHelper;
import org.apache.commons.io.FileUtils;
import org.apache.logging.log4j.Logger;
import org.apache.logging.log4j.LogManager;

public class FileUtil {
    protected static Logger LOGGER = LogManager.getLogger(FileUtil.class);

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
}
