/*
 * Licensed to the Apache Software Foundation (ASF) under one
 * or more contributor license agreements.  See the NOTICE file
 * distributed with this work for additional information
 * regarding copyright ownership.  The ASF licenses this file
 * to you under the Apache License, Version 2.0 (the
 * "License"); you may not use this file except in compliance
 * with the License.  You may obtain a copy of the License at
 *
 *   http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing,
 * software distributed under the License is distributed on an
 * "AS IS" BASIS, WITHOUT WARRANTIES OR CONDITIONS OF ANY
 * KIND, either express or implied.  See the License for the
 * specific language governing permissions and limitations
 * under the License.
 */
package org.apache.cloudstack.framework.codestyle;

import java.util.concurrent.ExecutionException;

import org.apache.cloudstack.framework.async.AsyncCallFuture;
import org.apache.cloudstack.framework.async.AsyncCallbackDispatcher;
import org.apache.cloudstack.framework.async.AsyncCallbackDriver;
import org.apache.cloudstack.framework.async.AsyncCompletionCallback;
import org.apache.cloudstack.framework.async.AsyncRpcContext;
import org.junit.Assert;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.springframework.test.context.ContextConfiguration;
import org.springframework.test.context.junit4.SpringJUnit4ClassRunner;

@RunWith(SpringJUnit4ClassRunner.class)
@ContextConfiguration(locations = "classpath:/SampleManagementServerAppContext.xml")
public class AsyncSampleEventDrivenStyleCaller {
    private AsyncSampleCallee _ds;
    AsyncCallbackDriver _callbackDriver;

    @Before
    public void setup() {
        _ds = new AsyncSampleCallee();
    }

    @SuppressWarnings("unchecked")
    @Test
    public void MethodThatWillCallAsyncMethod() {
        String vol = new String("Hello");
        AsyncCallbackDispatcher<AsyncSampleEventDrivenStyleCaller, Object> caller = AsyncCallbackDispatcher.create(this);
        AsyncCallFuture<String> future = _ds.createVolume(vol);
        try {
            String result = future.get();
            Assert.assertEquals(result, vol);
        } catch (InterruptedException e) {
            // TODO Auto-generated catch block
            e.printStackTrace();
        } catch (ExecutionException e) {
            // TODO Auto-generated catch block
            e.printStackTrace();
        }
    }

    private class TestContext<T> extends AsyncRpcContext<T> {
        private boolean finished;
        private String result;

        /**
         * @param callback
         */
        public TestContext(AsyncCompletionCallback<T> callback) {
            super(callback);
            this.finished = false;
        }

        public void setResult(String result) {
            this.result = result;
            synchronized (this) {
                this.finished = true;
                this.notify();
            }
        }

        public String getResult() {
            synchronized (this) {
                if (!this.finished) {
                    try {
                        this.wait();

                    } catch (InterruptedException e) {
                        // TODO Auto-generated catch block
                        e.printStackTrace();
                    }
                }
                return this.result;
            }
        }

    }

    @Test
    public void installCallback() {
        TestContext<String> context = new TestContext<String>(null);
        AsyncCallbackDispatcher<AsyncSampleEventDrivenStyleCaller, Object> caller = AsyncCallbackDispatcher.create(this);
        caller.setCallback(caller.getTarget().HandleVolumeCreateAsyncCallback(null, null)).setContext(context);
        String test = "test";
        _ds.createVolumeAsync(test, caller);
        Assert.assertEquals(test, context.getResult());
    }

    protected Void HandleVolumeCreateAsyncCallback(AsyncCallbackDispatcher<AsyncSampleEventDrivenStyleCaller, String> callback, TestContext<String> context) {
        String resultVol = callback.getResult();
        context.setResult(resultVol);
        return null;
    }

    public static void main(String[] args) {
        AsyncSampleEventDrivenStyleCaller caller = new AsyncSampleEventDrivenStyleCaller();
        caller.MethodThatWillCallAsyncMethod();
    }
}
