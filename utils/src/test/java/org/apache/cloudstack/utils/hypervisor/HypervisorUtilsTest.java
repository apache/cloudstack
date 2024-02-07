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

package org.apache.cloudstack.utils.hypervisor;

import com.cloud.utils.exception.CloudRuntimeException;
import org.junit.Assert;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.junit.MockitoJUnitRunner;

import java.io.File;
import java.io.FileWriter;
import java.io.IOException;
import java.util.Arrays;

@RunWith(MockitoJUnitRunner.class)
public class HypervisorUtilsTest {

    final long _minFileSize = 10485760L;

    @Test
    public void checkVolumeFileForActivitySmallFileTest() throws IOException {
        System.out.print("Testing don't block on newly created clones - ");
        String filePath = "./testsmallfileinactive";
        int timeoutSeconds = 5;
        long thresholdMilliseconds = 2000;
        File file = new File(filePath);

        long startTime = setupcheckVolumeFileForActivityFile(file, 0);
        HypervisorUtils.checkVolumeFileForActivity(filePath, timeoutSeconds, thresholdMilliseconds, _minFileSize);
        long endTime = System.currentTimeMillis();

        Assert.assertEquals(startTime, endTime, 1000L);
        System.out.println("pass");

        file.delete();
    }

    @Test
    public void checkVolumeFileForActivityTest() throws IOException {
        System.out.print("Testing block on modified files - ");
        String filePath = "./testfileinactive";
        int timeoutSeconds = 8;
        long thresholdMilliseconds = 1000;
        File file = new File(filePath);

        long startTime = setupcheckVolumeFileForActivityFile(file, _minFileSize);
        try {
            HypervisorUtils.checkVolumeFileForActivity(filePath, timeoutSeconds, thresholdMilliseconds, _minFileSize);
        } catch (CloudRuntimeException ex) {
            System.out.println("fail");
            return;
        }
        long duration = System.currentTimeMillis() - startTime;

        Assert.assertFalse("Didn't block long enough, expected at least " + thresholdMilliseconds + " and got " + duration, duration < thresholdMilliseconds);
        System.out.println("pass");

        file.delete();
    }

    @Test(expected=CloudRuntimeException.class)
    public void checkVolumeFileForActivityTimeoutTest() throws IOException {
        System.out.print("Testing timeout of blocking on modified files - ");
        String filePath = "./testfileinactive";
        int timeoutSeconds = 3;
        long thresholdMilliseconds = 5000;
        File file = new File(filePath);
        setupcheckVolumeFileForActivityFile(file, _minFileSize);

        try {
            HypervisorUtils.checkVolumeFileForActivity(filePath, timeoutSeconds, thresholdMilliseconds, _minFileSize);
        } catch (CloudRuntimeException ex) {
            System.out.println("pass");
            throw ex;
        } finally {
            file.delete();
        }
        System.out.println("Fail");
    }

    private long setupcheckVolumeFileForActivityFile(File file, long minSize) throws IOException {
        if (file.exists()) {
            file.delete();
        }
        file.createNewFile();
        char[] chars = new char[1048576];
        Arrays.fill(chars, 'X');
        long written = 0;
        FileWriter writer = new FileWriter(file);
        while (written < minSize) {
            writer.write(chars);
            written += chars.length;
        }
        long creationTime = System.currentTimeMillis();
        writer.close();
        return creationTime;
    }
}
