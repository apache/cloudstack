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

import java.io.BufferedReader;
import java.io.IOException;

import org.apache.commons.lang.SystemUtils;
import org.apache.log4j.Logger;
import org.junit.Assert;
import org.junit.Assume;
import org.junit.Ignore;
import org.junit.Test;
import org.mockito.Matchers;
import org.mockito.Mockito;

import com.cloud.utils.script.OutputInterpreter;
import com.cloud.utils.script.Script;

public class ScriptTest {
    @Test
    public void testEcho() {
        Assume.assumeTrue(SystemUtils.IS_OS_LINUX);
        Script script = new Script("/bin/echo");
        script.add("bar");
        OutputInterpreter.AllLinesParser resultParser = new OutputInterpreter.AllLinesParser();
        String result = script.execute(resultParser);
        // With allLinesParser, result is not comming from the return value
        Assert.assertNull(result);
        Assert.assertEquals("bar\n", resultParser.getLines());
    }

    @Test
    public void testLogger() {
        Assume.assumeTrue(SystemUtils.IS_OS_LINUX);
        Logger mock = Mockito.mock(Logger.class);
        Mockito.doNothing().when(mock).debug(Matchers.any());
        Script script = new Script("/bin/echo", mock);
        script.execute();
    }

    @Test
    public void testToString() {
        Assume.assumeTrue(SystemUtils.IS_OS_LINUX);
        Script script = new Script("/bin/echo");
        script.add("foo");
        Assert.assertEquals("/bin/echo foo ", script.toString());
    }

    @Test
    public void testSet() {
        Assume.assumeTrue(SystemUtils.IS_OS_LINUX);
        Script script = new Script("/bin/echo");
        script.add("foo");
        script.add("bar", "baz");
        script.set("blah", "blah");
        Assert.assertEquals("/bin/echo foo bar baz blah blah ",
                script.toString());
    }

    @Test
    @Ignore
    public void testExecute() {
        Assume.assumeTrue(SystemUtils.IS_OS_LINUX);
        Logger mock = Mockito.mock(Logger.class);
        Mockito.doNothing().when(mock).debug(Matchers.any());
        for (int i = 0; i < 100000; i++) {
            Script script = new Script("/bin/false", mock);
            script.execute();
        }
    }

    @Test
    public void testRunSimpleBashScript() {
        Assume.assumeTrue(SystemUtils.IS_OS_LINUX);
        Assert.assertEquals("hello world!",
                Script.runSimpleBashScript("echo 'hello world!'"));
    }

    @Test
    public void executeWithOutputInterpreter() {
        Assume.assumeTrue(SystemUtils.IS_OS_LINUX);
        Script script = new Script("/bin/bash");
        script.add("-c");
        script.add("echo 'hello world!'");
        String value = script.execute(new OutputInterpreter() {

            @Override
            public String interpret(BufferedReader reader) throws IOException {
                throw new IllegalArgumentException();
            }
        });
        // it is a stack trace in this case as string
        Assert.assertNotNull(value);
    }

    @Test
    public void runSimpleBashScriptNotExisting() {
        Assume.assumeTrue(SystemUtils.IS_OS_LINUX);
        String output = Script.runSimpleBashScript("/not/existing/scripts/"
                + System.currentTimeMillis());
        Assert.assertNull(output);
    }

    @Test
    public void testRunSimpleBashScriptWithTimeout() {
        Assume.assumeTrue(SystemUtils.IS_OS_LINUX);
        Assert.assertEquals("hello world!",
                Script.runSimpleBashScript("echo 'hello world!'", 1000));
    }

    @Test
    public void testFindScript() {
        Assume.assumeTrue(SystemUtils.IS_OS_LINUX);
        String script = Script.findScript("/bin", "pwd");
        Assert.assertNotNull("/bin/pwd shoud be there on linux", script);
    }
}
