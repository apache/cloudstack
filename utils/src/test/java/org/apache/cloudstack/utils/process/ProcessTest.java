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

package org.apache.cloudstack.utils.process;

import com.cloud.utils.concurrency.NamedThreadFactory;
import com.google.common.base.Strings;
import org.joda.time.Duration;
import org.junit.Assert;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.runners.MockitoJUnitRunner;

import java.util.Arrays;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

@RunWith(MockitoJUnitRunner.class)
public class ProcessTest {

    private static final ExecutorService executor = Executors.newFixedThreadPool(10, new NamedThreadFactory("IpmiToolDriverTest"));
    private static final ProcessRunner RUNNER = new ProcessRunner(executor);

    @Test
    public void testProcessRunner() {
        ProcessResult result = RUNNER.executeCommands(Arrays.asList("sleep", "0"));
        Assert.assertEquals(result.getReturnCode(), 0);
        Assert.assertTrue(Strings.isNullOrEmpty(result.getStdError()));
    }

    @Test
    public void testProcessRunnerWithTimeout() {
        ProcessResult result = RUNNER.executeCommands(Arrays.asList("sleep", "5"), Duration.standardSeconds(1));
        Assert.assertNotEquals(result.getReturnCode(), 0);
        Assert.assertTrue(result.getStdError().length() > 0);
        Assert.assertEquals(result.getStdError(), "Operation timed out, aborted.");
    }

    @Test
    public void testProcessRunnerWithTimeoutAndException() {
        ProcessResult result = RUNNER.executeCommands(Arrays.asList("ls", "/some/dir/that/should/not/exist"), Duration.standardSeconds(2));
        Assert.assertNotEquals(result.getReturnCode(), 0);
        Assert.assertTrue(result.getStdError().length() > 0);
        Assert.assertNotEquals(result.getStdError(), "Operation timed out, aborted.");
    }

    @Test(expected = IllegalArgumentException.class)
    public void testProcessRunnerWithMoreThanMaxAllowedTimeout() {
        RUNNER.executeCommands(Arrays.asList("ls", "/some/dir/that/should/not/exist"), ProcessRunner.DEFAULT_MAX_TIMEOUT.plus(1000));
        Assert.fail("Illegal argument exception was expected");
    }
}
