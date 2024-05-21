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
import java.util.concurrent.Callable;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.Future;
import java.util.concurrent.TimeoutException;

import com.cloud.utils.exception.CloudRuntimeException;
import org.junit.Assert;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.Mockito;
import org.mockito.Spy;
import org.mockito.junit.MockitoJUnitRunner;
import org.openjdk.nashorn.api.scripting.NashornScriptEngineFactory;

import javax.script.ScriptEngine;

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
    public void addVariablesToScriptTestVariablesMapIsEmptyReturnScript() {
        String script = "a + b";
        jsInterpreterSpy.variables = new LinkedHashMap<>();

        String result = jsInterpreterSpy.addVariablesToScript(script);

        Assert.assertEquals(script, result);
    }

    @Test
    public void addVariablesToScriptTestVariablesMapIsNotEmptyInjectVariableToScript() {
        String script = "a + b";
        String var1 = "{test: \"test\"}";
        jsInterpreterSpy.injectVariable("var1", var1);
        jsInterpreterSpy.injectVariable("var2", var1);

        String expected = String.format(" var1 = %s; var2 = %s; %s", var1, var1, script);

        String result = jsInterpreterSpy.addVariablesToScript(script);

        Assert.assertEquals(expected, result);
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
        Map<String, String> variables = new LinkedHashMap<>();
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

        Mockito.doReturn(scriptEngineMock).when(nashornScriptEngineFactoryMock).getScriptEngine(Mockito.anyString());

        jsInterpreterSpy.setScriptEngineDisablingJavaLanguage(nashornScriptEngineFactoryMock);

        Assert.assertEquals(scriptEngineMock, jsInterpreterSpy.interpreter);
        Mockito.verify(nashornScriptEngineFactoryMock).getScriptEngine("--no-java");
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

        Assert.assertEquals(jsInterpreterSpy.variables.get("a"), "\"b\"");
    }
}
