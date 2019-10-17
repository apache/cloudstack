/* Licensed to the Apache Software Foundation (ASF) under one
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
package org.apache.cloudstack.framework.async;

import java.util.concurrent.ExecutionException;
import java.util.concurrent.Future;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.TimeoutException;

public class AsyncCallFuture<T> implements Future<T>, AsyncCompletionCallback<T> {

    Object _completed = new Object();
    boolean _done = false;
    T _resultObject;        // we will store a copy of the result object

    public AsyncCallFuture() {
    }

    @Override
    public boolean cancel(boolean mayInterruptIfRunning) {
        // TODO we don't support cancel yet
        return false;
    }

    @Override
    public T get() throws InterruptedException, ExecutionException {
        synchronized (_completed) {
            if (!_done)
                _completed.wait();
        }

        return _resultObject;
    }

    @Override
    public T get(long timeout, TimeUnit timeUnit) throws InterruptedException, ExecutionException, TimeoutException {

        TimeUnit milliSecondsUnit = TimeUnit.MILLISECONDS;

        synchronized (_completed) {
            if (!_done)
                _completed.wait(milliSecondsUnit.convert(timeout, timeUnit));
        }

        return _resultObject;
    }

    @Override
    public boolean isCancelled() {
        // TODO we don't support cancel yet
        return false;
    }

    @Override
    public boolean isDone() {
        return _done;
    }

    @Override
    public void complete(T resultObject) {
        _resultObject = resultObject;
        synchronized (_completed) {
            _done = true;
            _completed.notifyAll();
        }
    }
}
