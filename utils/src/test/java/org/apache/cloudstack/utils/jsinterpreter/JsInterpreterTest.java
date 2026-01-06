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

package org.apache.cloudstack.utils.jsinterpreter;

import java.io.IOException;
import java.util.LinkedHashMap;
import java.util.Map;
import java.util.UUID;
import java.util.concurrent.Callable;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.Future;
import java.util.concurrent.TimeoutException;

import javax.script.ScriptEngine;

import org.junit.Assert;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.Mockito;
import org.mockito.Spy;
import org.mockito.junit.MockitoJUnitRunner;
import org.openjdk.nashorn.api.scripting.ClassFilter;
import org.openjdk.nashorn.api.scripting.NashornScriptEngineFactory;

import com.cloud.utils.exception.CloudRuntimeException;

@RunWith(MockitoJUnitRunner.class)
public class JsInterpreterTest {
    @InjectMocks
    @Spy
    JsInterpreter jsInterpreterSpy = new JsInterpreter();

    @Mock
    ExecutorService executorMock;

    @Mock
    Future<Object> futureObjectMock;

    @Test
    public void closeTestShutdownExecutor() throws IOException {
        ExecutorService executor = Executors.newSingleThreadExecutor();
        jsInterpreterSpy.executor = executor;

        jsInterpreterSpy.close();
        Assert.assertTrue(executor.isShutdown());
    }

    @Test
    public void executeScriptTestReturnResultOfScriptExecution() {
        String script = "5";
        Object expected = new Object();
        Mockito.doReturn(expected).when(jsInterpreterSpy).executeScript(Mockito.anyString());

        Object result = jsInterpreterSpy.executeScript(script);

        Assert.assertEquals(expected, result);
    }

    @Test(expected = CloudRuntimeException.class)
    public void executeScriptInThreadTestThreadThrowInterruptedException() throws InterruptedException, ExecutionException, TimeoutException {
        Mockito.doReturn(futureObjectMock).when(executorMock).submit(Mockito.<Callable<Object>>any());
        Mockito.doThrow(InterruptedException.class).when(futureObjectMock).get(Mockito.anyLong(), Mockito.any());

        jsInterpreterSpy.executeScriptInThread("a");
        Mockito.verify(futureObjectMock).cancel(true);
    }

    @Test(expected = CloudRuntimeException.class)
    public void executeScriptInThreadTestThreadThrowExecutionException() throws InterruptedException, ExecutionException, TimeoutException {
        Mockito.doReturn(futureObjectMock).when(executorMock).submit(Mockito.<Callable<Object>>any());
        Mockito.doThrow(ExecutionException.class).when(futureObjectMock).get(Mockito.anyLong(), Mockito.any());

        jsInterpreterSpy.executeScriptInThread("b");
        Mockito.verify(futureObjectMock).cancel(true);
    }

    @Test(expected = CloudRuntimeException.class)
    public void executeScriptInThreadTestThreadThrowTimeoutException() throws InterruptedException, ExecutionException, TimeoutException {
        Mockito.doReturn(futureObjectMock).when(executorMock).submit(Mockito.<Callable<Object>>any());
        Mockito.doThrow(TimeoutException.class).when(futureObjectMock).get(Mockito.anyLong(), Mockito.any());

        jsInterpreterSpy.executeScriptInThread("c");
        Mockito.verify(futureObjectMock).cancel(true);
    }

    @Test
    public void executeScriptInThreadTestReturnResultOfScriptExecution() throws InterruptedException, ExecutionException, TimeoutException {
        Object expected = new Object();

        Mockito.doReturn(futureObjectMock).when(executorMock).submit(Mockito.<Callable<Object>>any());
        Mockito.doReturn(expected).when(futureObjectMock).get(Mockito.anyLong(), Mockito.any());

        Object result = jsInterpreterSpy.executeScriptInThread("");

        Assert.assertEquals(expected, result);
        Mockito.verify(futureObjectMock).cancel(true);
    }

    @Test
    public void injectVariableTestAddVariableToMap() {
        Map<String, String> variables = new LinkedHashMap<>();
        variables.put("a", "b");
        variables.put("b", null);

        jsInterpreterSpy.variables = new LinkedHashMap<>();

        jsInterpreterSpy.injectVariable("a", "b");
        jsInterpreterSpy.injectVariable("b", null);

        variables.forEach((key, value) -> {
            Assert.assertEquals(value, jsInterpreterSpy.variables.get(key));
        });
    }

    @Test
    public void discardCurrentVariablesTestInstantiateNewMap() {
        Map<String, Object> variables = new LinkedHashMap<>();
        variables.put("a", "b");
        variables.put("b", null);

        jsInterpreterSpy.variables = variables;

        jsInterpreterSpy.discardCurrentVariables();

        Assert.assertEquals(0, jsInterpreterSpy.variables.size());
    }

    @Test
    public void setScriptEngineDisablingJavaLanguageTest() {
        NashornScriptEngineFactory nashornScriptEngineFactoryMock = Mockito.spy(NashornScriptEngineFactory.class);
        ScriptEngine scriptEngineMock = Mockito.mock(ScriptEngine.class);

        Mockito.doReturn(scriptEngineMock).when(nashornScriptEngineFactoryMock).getScriptEngine(Mockito.any(),
                Mockito.any(ClassLoader.class), Mockito.any(ClassFilter.class));

        jsInterpreterSpy.setScriptEngineDisablingJavaLanguage(nashornScriptEngineFactoryMock);

        Assert.assertEquals(scriptEngineMock, jsInterpreterSpy.interpreter);
        Mockito.verify(nashornScriptEngineFactoryMock).getScriptEngine(Mockito.any(),
                Mockito.any(ClassLoader.class), Mockito.any(ClassFilter.class));
    }

    @Test
    public void injectStringVariableTestNullValueDoNothing() {
        jsInterpreterSpy.variables = new LinkedHashMap<>();

        jsInterpreterSpy.injectStringVariable("a", null);

        Assert.assertTrue(jsInterpreterSpy.variables.isEmpty());
    }

    @Test
    public void injectStringVariableTestNotNullValueSurroundWithDoubleQuotes() {
        jsInterpreterSpy.variables = new LinkedHashMap<>();

        jsInterpreterSpy.injectStringVariable("a", "b");

        Assert.assertEquals(jsInterpreterSpy.variables.get("a"), "b");
    }

    @Test
    public void executeScriptTestValidScriptShouldPassWithMixedVariables() {
        try (JsInterpreter jsInterpreter = new JsInterpreter(1000)) {
            jsInterpreter.injectVariable("x", 10);
            jsInterpreter.injectVariable("y", "hello");
            jsInterpreter.injectVariable("z", true);
            String validScript = "var result = x + (z ? 1 : 0); y + '-' + result;";
            Object result = jsInterpreter.executeScript(validScript);
            Assert.assertEquals("hello-11", result);
        } catch (IOException exception) {
            Assert.fail("IOException not expected here");
        }
    }

    private void runMaliciousScriptFileTest(String script, String filename) {
        try (JsInterpreter jsInterpreter = new JsInterpreter(1000)) {
            jsInterpreter.executeScript(script);
        } catch (CloudRuntimeException ex) {
            Assert.assertTrue(ex.getMessage().contains("Unable to execute script"));
            java.io.File f = new java.io.File(filename);
            Assert.assertFalse(f.exists());
        } catch (IOException exception) {
            Assert.fail("IOException not expected here");
        }
    }

    @Test
    public void executeScriptTestMaliciousScriptShouldThrowException1() {
        String filename = "/tmp/hack1-" + UUID.randomUUID();
        String maliciousScript = "Java.type('java.lang.Runtime').getRuntime().exec('touch " + filename + "')";
        runMaliciousScriptFileTest(maliciousScript, filename);
    }

    @Test
    public void executeScriptTestMaliciousScriptShouldThrowException2() {
        String filename = "/tmp/hack2-" + UUID.randomUUID();
        String maliciousScript = "var e=this.engine.getFactory().getScriptEngine('-Dnashorn.args=--no-java=False'); e.eval(\"java.lang.Runtime.getRuntime().exec(['/bin/bash','-c','touch " + filename + "']);\");";
        runMaliciousScriptFileTest(maliciousScript, filename);
    }
}
