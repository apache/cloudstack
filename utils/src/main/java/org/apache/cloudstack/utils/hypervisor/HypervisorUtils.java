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
import org.apache.logging.log4j.Logger;
import org.apache.logging.log4j.LogManager;

import java.io.File;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.attribute.BasicFileAttributes;
import java.util.concurrent.TimeUnit;

public class HypervisorUtils {
    protected static Logger LOGGER = LogManager.getLogger(HypervisorUtils.class);

    public static void checkVolumeFileForActivity(final String filePath, int timeoutSeconds, long inactiveThresholdMilliseconds, long minimumFileSize) throws IOException {
        File file = new File(filePath);
        if (!file.exists()) {
            throw new CloudRuntimeException("File " + file.getAbsolutePath() + " not found");
        }
        if (file.length() < minimumFileSize) {
            LOGGER.debug("VM disk file too small, fresh clone? skipping modify check");
            return;
        }
        int waitedSeconds = 0;
        int intervalSeconds = 1;
        while (true) {
            BasicFileAttributes attrs = Files.readAttributes(file.toPath(), BasicFileAttributes.class);
            long modifyIdle = System.currentTimeMillis() - attrs.lastModifiedTime().toMillis();
            long accessIdle = System.currentTimeMillis() - attrs.lastAccessTime().toMillis();
            if (modifyIdle > inactiveThresholdMilliseconds && accessIdle > inactiveThresholdMilliseconds) {
                LOGGER.debug("File " + filePath + " has not been accessed or modified for at least " + inactiveThresholdMilliseconds + " ms");
                return;
            } else {
                LOGGER.debug("File was modified " + modifyIdle + "ms ago, accessed " + accessIdle + "ms ago, waiting for inactivity threshold of "
                        + inactiveThresholdMilliseconds + "ms or timeout of " + timeoutSeconds + "s (waited " + waitedSeconds + "s)");
            }
            try {
                TimeUnit.SECONDS.sleep(intervalSeconds);
            } catch (InterruptedException ex) {
                throw new CloudRuntimeException("Interrupted while waiting for activity on " + filePath + " to subside", ex);
            }
            waitedSeconds += intervalSeconds;
            if (waitedSeconds >= timeoutSeconds) {
                throw new CloudRuntimeException("Reached timeout while waiting for activity on " + filePath + " to subside");
            }
        }
    }

}
