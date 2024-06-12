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

package org.apache.cloudstack.utils.process;

import java.util.concurrent.ExecutorService;

import org.junit.Assert;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.InjectMocks;
import org.mockito.Mockito;
import org.mockito.junit.MockitoJUnitRunner;

@RunWith(MockitoJUnitRunner.class)
public class ProcessRunnerTest {

    @InjectMocks
    ProcessRunner processRunner = new ProcessRunner(Mockito.mock(ExecutorService.class));

    private int countSubstringOccurrences(String mainString, String subString) {
        int count = 0;
        int index = 0;
        while ((index = mainString.indexOf(subString, index)) != -1) {
            count++;
            index += subString.length();
        }
        return count;
    }

    @Test
    public void testRemoveCommandSensitiveInfoForLoggingIpmi() {
        String password = "R@ndomP@ss";
        String command = String.format("/usr/bin/ipmitool -H host -p 1234 -U Admin " +
                "-P %s chassis power status", password);
        String log = processRunner.removeCommandSensitiveInfoForLogging(command);
        Assert.assertFalse(log.contains(password));

        String command1 = String.format("%s -D %s", command, password);
        log = processRunner.removeCommandSensitiveInfoForLogging(command1);
        Assert.assertTrue(log.contains(password));
        Assert.assertEquals(1, countSubstringOccurrences(log, password));

        String command2 = command.replace("ipmitool", "impit00l");
        log = processRunner.removeCommandSensitiveInfoForLogging(command2);
        Assert.assertTrue(log.contains(password));
        Assert.assertEquals(1, countSubstringOccurrences(log, password));
    }
}
