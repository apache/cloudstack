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

package com.cloud.network.nicira;

public class ExecutionCounter {

    private final int executionLimit;
    private final ThreadLocal<Integer> executionCount;

    public ExecutionCounter(final int executionLimit) {
        this.executionLimit = executionLimit;
        executionCount = new ThreadLocal<Integer>() {
            @Override
            protected Integer initialValue() {
                return new Integer(0);
            }
        };
    }

    public ExecutionCounter resetExecutionCounter() {
        executionCount.set(0);
        return this;
    }

    public boolean hasReachedExecutionLimit() {
        return executionCount.get() >= executionLimit;
    }

    public ExecutionCounter incrementExecutionCounter() {
        executionCount.set(executionCount.get() + 1);
        return this;
    }

    public int getValue() {
        return executionCount.get();
    }
}
