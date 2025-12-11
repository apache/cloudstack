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
package com.cloud.utils.script;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.concurrent.TimeUnit;
import org.apache.commons.lang3.StringUtils;
import org.junit.Assert;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.junit.MockitoJUnitRunner;
import com.cloud.utils.Pair;

@RunWith(MockitoJUnitRunner.class)
public class ScriptTest {

    @Test
    public void testExecutePipedCommandsSingle() {
        String keyword = "Hello World!";
        List<String[]> commands = new ArrayList<>();
        commands.add(new String[]{"echo", keyword});
        Pair<Integer, String> result = Script.executePipedCommands(commands, 0);
        Assert.assertNotNull("Result should not be null", result);
        Assert.assertEquals(0, result.first().intValue()); // Expecting 0 for success
        String output = result.second().trim();
        Assert.assertTrue(StringUtils.isNotEmpty(output));
        Assert.assertEquals(keyword, output);
    }

    @Test
    public void testExecutePipedCommandsMultiple() {
        String keyword = "Hello";
        List<String[]> commands = Arrays.asList(
                new String[]{"echo", String.format("%s\n World", keyword)},
                new String[]{"grep", keyword}
        );
        Pair<Integer, String> result = Script.executePipedCommands(commands, 0);
        Assert.assertNotNull("Result should not be null", result);
        Assert.assertEquals(0, result.first().intValue()); // Expecting 0 for success
        String output = result.second().trim();
        Assert.assertTrue(StringUtils.isNotEmpty(output));
        Assert.assertEquals(keyword, output);
    }

    @Test
    public void testExecutePipedCommandsTimeout() {
        List<String[]> commands = new ArrayList<>();
        commands.add(new String[]{"sh", "-c", "sleep 10"});  // Simulate a long-running command
        Pair<Integer, String> result = Script.executePipedCommands(commands, TimeUnit.SECONDS.toMillis(1));  // Set a timeout of 1 second
        Assert.assertNotNull("Result should not be null", result);
        Assert.assertEquals(-1, result.first().intValue());  // Expecting -1 for timeout
        Assert.assertEquals(Script.ERR_TIMEOUT, result.second()); // Ensure the correct timeout error message
    }

    @Test
    public void testGetExecutableAbsolutePath() {
        if (System.getProperty("os.name").startsWith("Windows")) {
            return;  // Skip on Windows, as 'ls' command isn't available on Windows by default
        }
        String result = Script.getExecutableAbsolutePath("ls");
        Assert.assertTrue(List.of("/usr/bin/ls", "/bin/ls").contains(result)); // Check that the ls command exists in the expected locations
    }
}
