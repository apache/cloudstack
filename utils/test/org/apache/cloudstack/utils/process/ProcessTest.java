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

import com.google.common.base.Strings;
import org.joda.time.Duration;
import org.junit.Assert;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.runners.MockitoJUnitRunner;

import java.util.Arrays;

@RunWith(MockitoJUnitRunner.class)
public class ProcessTest {

    @Test
    public void testProcessRunner() {
        ProcessResult result = ProcessRunner.executeCommands(Arrays.asList("ls", "/tmp"), Duration.ZERO);
        Assert.assertEquals(result.getReturnCode(), 0);
        Assert.assertTrue(Strings.isNullOrEmpty(result.getStdError()));
        Assert.assertTrue(result.getStdOutput().length() > 0);
    }

    @Test
    public void testProcessRunnerWithTimeout() {
        ProcessResult result = ProcessRunner.executeCommands(Arrays.asList("sleep", "5"), Duration.standardSeconds(1));
        Assert.assertNotEquals(result.getReturnCode(), 0);
        Assert.assertTrue(result.getStdError().length() > 0);
        Assert.assertEquals(result.getStdError(), "Operation timed out, aborted");
    }

    @Test
    public void testProcessRunnerWithTimeoutAndException() {
        ProcessResult result = ProcessRunner.executeCommands(Arrays.asList("ls", "/some/dir/that/should/not/exist"), Duration.standardSeconds(2));
        Assert.assertNotEquals(result.getReturnCode(), 0);
        Assert.assertTrue(result.getStdError().length() > 0);
        Assert.assertNotEquals(result.getStdError(), "Operation timed out, aborted");
    }
}
