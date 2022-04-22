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
package com.cloud.utils;

import com.cloud.utils.ssh.SshHelper;
import org.junit.Assert;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.Mockito;
import org.powermock.api.mockito.PowerMockito;
import org.powermock.core.classloader.annotations.PowerMockIgnore;
import org.powermock.core.classloader.annotations.PrepareForTest;
import org.powermock.modules.junit4.PowerMockRunner;

import java.io.File;

import static org.mockito.ArgumentMatchers.nullable;

@PrepareForTest(value = {SshHelper.class})
@RunWith(PowerMockRunner.class)
@PowerMockIgnore({"com.sun.org.apache.xerces.*", "javax.xml.parsers.*", "javax.xml.*", "org.w3c.dom.*", "org.xml.*"})
public class FileUtilTest {

    @Test
    public void successfulScpTest() throws Exception {
        PowerMockito.mockStatic(SshHelper.class);
        String basePath = "/tmp";
        String[] files = new String[] { "file1.txt" };
        int sshPort = 3922;
        String controlIp = "10.0.0.10";
        String destPath = "/home/cloud/";
        File pemFile = new File("/root/.ssh/id_rsa");
        PowerMockito.doNothing().when(SshHelper.class, "scpTo", Mockito.anyString(), Mockito.anyInt(), Mockito.anyString(), Mockito.any(File.class), nullable(String.class), Mockito.anyString(), Mockito.any(String[].class), Mockito.anyString());
        FileUtil.scpPatchFiles(controlIp, destPath, sshPort, pemFile, files, basePath);
    }

    @Test
    public void FailingScpFilesTest() throws Exception {
        PowerMockito.mockStatic(SshHelper.class);
        String basePath = "/tmp";
        String[] files = new String[] { "file1.txt" };
        int sshPort = 3922;
        String controlIp = "10.0.0.10";
        String destPath = "/home/cloud/";
        File pemFile = new File("/root/.ssh/id_rsa");
        PowerMockito.doThrow(new Exception()).when(SshHelper.class, "scpTo", Mockito.anyString(), Mockito.anyInt(), Mockito.anyString(), Mockito.any(File.class), Mockito.anyString(), Mockito.anyString(), Mockito.any(String[].class), Mockito.anyString());
        try {
            FileUtil.scpPatchFiles(controlIp, destPath, sshPort, pemFile, files, basePath);
        } catch (Exception e) {
            Assert.assertEquals("Failed to scp files to system VM", e.getMessage());
        }

    }

}
