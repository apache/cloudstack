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
package com.cloud.usage;

import java.io.File;
import java.io.IOException;
import java.nio.charset.Charset;

import org.apache.commons.io.FileUtils;
import org.apache.commons.lang3.math.NumberUtils;
import org.junit.Assert;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.InOrder;
import org.mockito.Mockito;
import org.powermock.api.mockito.PowerMockito;
import org.powermock.core.classloader.annotations.PrepareForTest;
import org.powermock.modules.junit4.PowerMockRunner;

import com.cloud.utils.exception.CloudRuntimeException;

@RunWith(PowerMockRunner.class)
@PrepareForTest({FileUtils.class, UsageManagerImpl.class, NumberUtils.class})
public class UsageManagerImplTest {

    private static final String CLOUDSTACK_USAGE_SERVER_SERVICE_PID_FILE = "/var/run/cloudstack-usage.service.pid";
    private UsageManagerImpl usageManagerImpl = new UsageManagerImpl();

    @Test
    public void retrieveUsageManagerServicePidTestCompleteExecutionFlow() throws Exception {
        prepareAndVerifyTestRetrieveUsageManagerServicePidTest(true, true, 1, 1234, "1234" + System.getProperty("line.separator"));
    }

    @Test(expected=CloudRuntimeException.class)
    public void retrieveUsageManagerServicePidTestFileDoesNotExist() throws Exception {
        prepareAndVerifyTestRetrieveUsageManagerServicePidTest(false, true, 0, 1234, "1234" + System.getProperty("line.separator"));
    }

    @Test(expected = CloudRuntimeException.class)
    public void retrieveUsageManagerServicePidTestCannotReadFile() throws Exception {
        prepareAndVerifyTestRetrieveUsageManagerServicePidTest(true, false, 1, 1234, "1234" + System.getProperty("line.separator"));
    }

    @Test(expected = CloudRuntimeException.class)
    public void retrieveUsageManagerServicePidTestPidEqualsToZero() throws Exception {
        prepareAndVerifyTestRetrieveUsageManagerServicePidTest(true, true, 1, 0, "abc");
    }

    private void prepareAndVerifyTestRetrieveUsageManagerServicePidTest(boolean fileExists, boolean canReadFile, int timesCanReadIsExecuted, int expectedPid,
            String readedStringFromFile)
            throws Exception, IOException {
        File usageServicePid = Mockito.mock(File.class);
        PowerMockito.whenNew(File.class).withArguments(CLOUDSTACK_USAGE_SERVER_SERVICE_PID_FILE).thenReturn(usageServicePid);

        Mockito.when(usageServicePid.exists()).thenReturn(fileExists);
        Mockito.when(usageServicePid.canRead()).thenReturn(canReadFile);

        PowerMockito.mockStatic(FileUtils.class);
        Mockito.when(FileUtils.readFileToString(usageServicePid, Charset.defaultCharset())).thenReturn(readedStringFromFile);

        int result = usageManagerImpl.retrieveUsageManagerServicePid();

        InOrder inOrder = Mockito.inOrder(usageServicePid);
        inOrder.verify(usageServicePid).exists();
        inOrder.verify(usageServicePid, Mockito.times(timesCanReadIsExecuted)).canRead();

        Assert.assertEquals(expectedPid, result);
    }

}
