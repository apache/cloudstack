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

package com.cloud.hypervisor.kvm.resource;

import groovy.util.ResourceException;
import groovy.util.ScriptException;
import junit.framework.TestCase;
import org.junit.After;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.junit.MockitoJUnitRunner;

import java.io.File;
import java.io.IOException;
import java.io.PrintWriter;
import java.util.UUID;

@RunWith(MockitoJUnitRunner.class)
public class LibvirtKvmAgentHookTest extends TestCase {

    private final String source = "<xml />";
    private final String dir = "/tmp";
    private final String script = "xml-transform-test.groovy";
    private final String method = "transform";
    private final String methodNull = "transform2";
    private final String testImpl = "package groovy\n" +
            "\n" +
            "class BaseTransform {\n" +
            "    String transform(Object logger, String xml) {\n" +
            "        return xml + xml\n" +
            "    }\n" +
            "    String transform2(Object logger, String xml) {\n" +
            "        return null\n" +
            "    }\n" +
            "}\n" +
            "\n" +
            "new BaseTransform()\n" +
            "\n";

    @Before
    public void setUp() throws Exception {
        super.setUp();
        PrintWriter pw = new PrintWriter(new File(dir, script));
        pw.println(testImpl);
        pw.close();
    }

    @After
    public void tearDown() throws Exception {
        new File(dir, script).delete();
        super.tearDown();
    }

    @Test
    public void testTransform() throws IOException, ResourceException, ScriptException {
        LibvirtKvmAgentHook t = new LibvirtKvmAgentHook(dir, script, method);
        assertEquals(t.isInitialized(), true);
        String result = (String)t.handle(source);
        assertEquals(result, source + source);
    }

    @Test
    public void testWrongMethod() throws IOException, ResourceException, ScriptException {
        LibvirtKvmAgentHook t = new LibvirtKvmAgentHook(dir, script, "methodX");
        assertEquals(t.isInitialized(), true);
        assertEquals(t.handle(source), source);
    }

    @Test
    public void testNullMethod() throws IOException, ResourceException, ScriptException {
        LibvirtKvmAgentHook t = new LibvirtKvmAgentHook(dir, script, methodNull);
        assertEquals(t.isInitialized(), true);
        assertEquals(t.handle(source), null);
    }

    @Test
    public void testWrongScript() throws IOException, ResourceException, ScriptException {
        LibvirtKvmAgentHook t = new LibvirtKvmAgentHook(dir, "wrong-script.groovy", method);
        assertEquals(t.isInitialized(), false);
        assertEquals(t.handle(source), source);
    }

    @Test
    public void testWrongDir() throws IOException, ResourceException, ScriptException {
        LibvirtKvmAgentHook t = new LibvirtKvmAgentHook("/" + UUID.randomUUID().toString() + "-dir", script, method);
        assertEquals(t.isInitialized(), false);
        assertEquals(t.handle(source), source);
    }
}
