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
import java.util.concurrent.Callable;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.Future;
import java.util.concurrent.TimeoutException;

import org.junit.Assert;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.Mockito;
import org.mockito.Spy;
import org.powermock.api.mockito.PowerMockito;
import org.powermock.core.classloader.annotations.PowerMockIgnore;
import org.powermock.core.classloader.annotations.PrepareForTest;
import org.powermock.modules.junit4.PowerMockRunner;

import com.cloud.utils.exception.CloudRuntimeException;
import com.eclipsesource.v8.V8;
import com.eclipsesource.v8.V8Object;

@RunWith(PowerMockRunner.class)
@PowerMockIgnore({"javax.xml.*", "org.apache.xerces.*", "org.xml.*", "org.w3c.*"})
@PrepareForTest(JsInterpreter.class)
public class JsInterpreterTest {
    private long timeout = 2000;

    @Mock
    V8 v8Mock;

    @InjectMocks
    @Spy
    JsInterpreter jsInterpreterSpy = new JsInterpreter(timeout);

    @Mock
    V8Object v8ObjectMock;

    @Mock
    ExecutorService executorMock;

    @Mock
    Future<V8> futureV8Mock;

    @Mock
    Future<Object> futureObjectMock;

    @Test
    public void injectNullVariableTestInjectNullVariableResultTrue() {
        Mockito.doNothing().when(jsInterpreterSpy).injectVariableInThread(Mockito.anyString(), Mockito.any());

        String name = "test";
        Object value = null;

        boolean result = jsInterpreterSpy.injectNullVariable(name, value);

        Mockito.verify(jsInterpreterSpy).injectVariableInThread(name, null);
        Assert.assertTrue(result);
    }

    @Test
    public void injectNullVariableTestVariableWithValueDoNotInjectAndReturnFalse() {
        String name = "test";
        Object value = new Object();

        boolean result = jsInterpreterSpy.injectNullVariable(name, value);

        Mockito.verify(jsInterpreterSpy, Mockito.never()).injectVariableInThread(name, null);
        Assert.assertFalse(result);
    }

    @Test
    public void injectVariableTestInjectStringVariable() {
        Mockito.doNothing().when(jsInterpreterSpy).injectVariable(Mockito.anyString(), Mockito.any());
        jsInterpreterSpy.injectVariable("test", "test");

        Mockito.verify(jsInterpreterSpy).injectVariable(Mockito.anyString(), Mockito.any());
    }

    @Test
    public void injectVariableTestInjectNullStringVariable() {
        Mockito.doReturn(true).when(jsInterpreterSpy).injectNullVariable(Mockito.anyString(), Mockito.any());

        String name = "name_test";
        String value = "value_test";
        jsInterpreterSpy.injectVariable(name, value);

        Mockito.verify(jsInterpreterSpy, Mockito.never()).injectVariableInThread(Mockito.anyString(), Mockito.any());
    }

    @Test
    public void injectVariableTestInjectNotJsonStringVariable() {
        Mockito.doReturn(false).when(jsInterpreterSpy).injectNullVariable(Mockito.anyString(), Mockito.any());
        Mockito.doNothing().when(jsInterpreterSpy).injectVariableInThread(Mockito.anyString(), Mockito.any());

        String name = "name_test";
        String value = "value_test";
        jsInterpreterSpy.injectVariable(name, value, false);

        Mockito.verify(jsInterpreterSpy).injectVariableInThread(name, value);
        Assert.assertEquals("", jsInterpreterSpy.valuesToConvertToJsonBeforeExecutingTheCode);
    }

    @Test
    public void injectVariableTestInjectJsonStringVariable() {
        Mockito.doReturn(false).when(jsInterpreterSpy).injectNullVariable(Mockito.anyString(), Mockito.any());
        Mockito.doNothing().when(jsInterpreterSpy).injectVariableInThread(Mockito.anyString(), Mockito.any());

        String name = "name_test";
        String value = "value_test";
        jsInterpreterSpy.injectVariable(name, value, true);

        Mockito.verify(jsInterpreterSpy).injectVariableInThread(name, value);
        Assert.assertTrue(jsInterpreterSpy.valuesToConvertToJsonBeforeExecutingTheCode.contains(String.format("%s = JSON.parse(%s);", name, name)));
    }

    @Test(expected = CloudRuntimeException.class)
    public void jsInterpreterTestThreadThrowInterruptedException() throws InterruptedException, ExecutionException, TimeoutException, IOException {
        PowerMockito.mockStatic(Executors.class);
        PowerMockito.when(Executors.newSingleThreadExecutor()).thenReturn(executorMock);

        Mockito.doReturn(futureV8Mock).when(executorMock).submit(Mockito.<Callable<V8>>any());
        Mockito.doThrow(InterruptedException.class).when(futureV8Mock).get(Mockito.anyLong(), Mockito.any());

        JsInterpreter js = new JsInterpreter(timeout);
        js.close();

        Mockito.verify(futureV8Mock).cancel(true);
    }

    @Test(expected = CloudRuntimeException.class)
    public void jsInterpreterTestThreadThrowExecutionException() throws InterruptedException, ExecutionException, TimeoutException, IOException {
        PowerMockito.mockStatic(Executors.class);
        PowerMockito.when(Executors.newSingleThreadExecutor()).thenReturn(executorMock);

        Mockito.doReturn(futureV8Mock).when(executorMock).submit(Mockito.<Callable<V8>>any());
        Mockito.doThrow(ExecutionException.class).when(futureV8Mock).get(Mockito.anyLong(), Mockito.any());

        JsInterpreter js = new JsInterpreter(timeout);
        js.close();

        Mockito.verify(futureV8Mock).cancel(true);
    }

    @Test(expected = CloudRuntimeException.class)
    public void jsInterpreterTestThreadThrowTimeoutException() throws InterruptedException, ExecutionException, TimeoutException, IOException {
        PowerMockito.mockStatic(Executors.class);
        PowerMockito.when(Executors.newSingleThreadExecutor()).thenReturn(executorMock);

        Mockito.doReturn(futureV8Mock).when(executorMock).submit(Mockito.<Callable<V8>>any());
        Mockito.doThrow(TimeoutException.class).when(futureV8Mock).get(Mockito.anyLong(), Mockito.any());

        JsInterpreter js = new JsInterpreter(timeout);
        js.close();

        Mockito.verify(futureV8Mock).cancel(true);
    }

    @Test
    public void jsInterpreterTestInstantiateV8() throws InterruptedException, ExecutionException, TimeoutException, IOException {
        PowerMockito.mockStatic(Executors.class);
        PowerMockito.when(Executors.newSingleThreadExecutor()).thenReturn(executorMock);

        Mockito.doReturn(futureV8Mock).when(executorMock).submit(Mockito.<Callable<V8>>any());
        Mockito.doReturn(v8Mock).when(futureV8Mock).get(Mockito.anyLong(), Mockito.any());

        JsInterpreter js = new JsInterpreter(timeout);

        Assert.assertEquals(v8Mock, js.interpreter);
        Mockito.verify(futureV8Mock).cancel(true);

        js.close();
    }

    @Test
    public void executeScriptTestReturnResultOfScriptExecution() {
        Object expected = new Object();
        Mockito.doReturn(expected).when(jsInterpreterSpy).executeScript(Mockito.anyString());

        Object result = jsInterpreterSpy.executeScript("");

        Assert.assertEquals(expected, result);
        Assert.assertEquals("", jsInterpreterSpy.valuesToConvertToJsonBeforeExecutingTheCode);
    }

    @Test(expected = CloudRuntimeException.class)
    public void executeScriptInThreadTestThreadThrowInterruptedException() throws InterruptedException, ExecutionException, TimeoutException {
        Mockito.doReturn(futureObjectMock).when(executorMock).submit(Mockito.<Callable<Object>>any());
        Mockito.doThrow(InterruptedException.class).when(futureObjectMock).get(Mockito.anyLong(), Mockito.any());
        Mockito.doNothing().when(v8Mock).terminateExecution();

        jsInterpreterSpy.executeScriptInThread("a");
        Mockito.verify(v8Mock).terminateExecution();
        Mockito.verify(futureObjectMock).cancel(true);
    }

    @Test(expected = CloudRuntimeException.class)
    public void executeScriptInThreadTestThreadThrowExecutionException() throws InterruptedException, ExecutionException, TimeoutException {
        Mockito.doReturn(futureObjectMock).when(executorMock).submit(Mockito.<Callable<Object>>any());
        Mockito.doThrow(ExecutionException.class).when(futureObjectMock).get(Mockito.anyLong(), Mockito.any());
        Mockito.doNothing().when(v8Mock).terminateExecution();

        jsInterpreterSpy.executeScriptInThread("b");
        Mockito.verify(v8Mock).terminateExecution();
        Mockito.verify(futureObjectMock).cancel(true);
    }

    @Test(expected = CloudRuntimeException.class)
    public void executeScriptInThreadTestThreadThrowTimeoutException() throws InterruptedException, ExecutionException, TimeoutException {
        Mockito.doReturn(futureObjectMock).when(executorMock).submit(Mockito.<Callable<Object>>any());
        Mockito.doThrow(TimeoutException.class).when(futureObjectMock).get(Mockito.anyLong(), Mockito.any());
        Mockito.doNothing().when(v8Mock).terminateExecution();

        jsInterpreterSpy.executeScriptInThread("c");
        Mockito.verify(v8Mock).terminateExecution();
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

    @Test(expected = CloudRuntimeException.class)
    public void closeTestThreadThrowInterruptedException() throws InterruptedException, ExecutionException, TimeoutException, IOException {
        Mockito.doReturn(futureV8Mock).when(executorMock).submit(Mockito.<Callable<V8>>any());
        Mockito.doThrow(InterruptedException.class).when(futureV8Mock).get(Mockito.anyLong(), Mockito.any());

        jsInterpreterSpy.close();

        Mockito.verify(futureV8Mock).cancel(true);
    }

    @Test(expected = CloudRuntimeException.class)
    public void closeTestThreadThrowExecutionException() throws InterruptedException, ExecutionException, TimeoutException, IOException {
        Mockito.doReturn(futureV8Mock).when(executorMock).submit(Mockito.<Callable<V8>>any());
        Mockito.doThrow(ExecutionException.class).when(futureV8Mock).get(Mockito.anyLong(), Mockito.any());

        jsInterpreterSpy.close();

        Mockito.verify(futureV8Mock).cancel(true);
    }

    @Test(expected = CloudRuntimeException.class)
    public void closeTestThreadThrowTimeoutException() throws InterruptedException, ExecutionException, TimeoutException, IOException {
        Mockito.doReturn(futureV8Mock).when(executorMock).submit(Mockito.<Callable<V8>>any());
        Mockito.doThrow(TimeoutException.class).when(futureV8Mock).get(Mockito.anyLong(), Mockito.any());

        jsInterpreterSpy.close();

        Mockito.verify(futureV8Mock).cancel(true);
    }

    @Test
    public void closeTestReleaseV8() throws InterruptedException, ExecutionException, TimeoutException, IOException {
        Mockito.doReturn(futureV8Mock).when(executorMock).submit(Mockito.<Callable<V8>>any());
        Mockito.doReturn(v8Mock).when(futureV8Mock).get(Mockito.anyLong(), Mockito.any());

        jsInterpreterSpy.close();

        Mockito.verify(futureV8Mock).cancel(true);
    }

    @Test(expected = CloudRuntimeException.class)
    public void injectVariableInThreadTestThreadThrowInterruptedException() throws InterruptedException, ExecutionException, TimeoutException, IOException {
        Mockito.doReturn(futureV8Mock).when(executorMock).submit(Mockito.<Callable<V8>>any());
        Mockito.doThrow(InterruptedException.class).when(futureV8Mock).get(Mockito.anyLong(), Mockito.any());

        jsInterpreterSpy.injectVariableInThread("a", "b");

        Mockito.verify(futureV8Mock).cancel(true);
    }

    @Test(expected = CloudRuntimeException.class)
    public void injectVariableInThreadTestThreadThrowExecutionException() throws InterruptedException, ExecutionException, TimeoutException, IOException {
        Mockito.doReturn(futureV8Mock).when(executorMock).submit(Mockito.<Callable<V8>>any());
        Mockito.doThrow(ExecutionException.class).when(futureV8Mock).get(Mockito.anyLong(), Mockito.any());

        jsInterpreterSpy.injectVariableInThread("a", "b");

        Mockito.verify(futureV8Mock).cancel(true);
    }

    @Test(expected = CloudRuntimeException.class)
    public void injectVariableInThreadTestThreadThrowTimeoutException() throws InterruptedException, ExecutionException, TimeoutException, IOException {
        Mockito.doReturn(futureV8Mock).when(executorMock).submit(Mockito.<Callable<V8>>any());
        Mockito.doThrow(TimeoutException.class).when(futureV8Mock).get(Mockito.anyLong(), Mockito.any());

        jsInterpreterSpy.injectVariableInThread("a", "b");

        Mockito.verify(futureV8Mock).cancel(true);
    }

    @Test
    public void injectVariableInThreadTestReleaseV8() throws InterruptedException, ExecutionException, TimeoutException, IOException {
        Mockito.doReturn(futureV8Mock).when(executorMock).submit(Mockito.<Callable<V8>>any());
        Mockito.doReturn(v8Mock).when(futureV8Mock).get(Mockito.anyLong(), Mockito.any());

        jsInterpreterSpy.injectVariableInThread("a", "b");

        Mockito.verify(futureV8Mock).cancel(true);
    }
}
