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

import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.equalTo;

import org.junit.Test;

public class ExecutionCounterTest {

    @Test
    public void testIncrementCounter() throws Exception {
        final ExecutionCounter executionCounter = new ExecutionCounter(-1);

        executionCounter.incrementExecutionCounter().incrementExecutionCounter();

        assertThat(executionCounter.getValue(), equalTo(2));
    }

    @Test
    public void testHasNotYetReachedTheExecutuionLimit() throws Exception {
        final ExecutionCounter executionCounter = new ExecutionCounter(2);

        executionCounter.incrementExecutionCounter();

        assertThat(executionCounter.hasReachedExecutionLimit(), equalTo(false));
    }

    @Test
    public void testHasAlreadyReachedTheExecutuionLimit() throws Exception {
        final ExecutionCounter executionCounter = new ExecutionCounter(2);

        executionCounter.incrementExecutionCounter().incrementExecutionCounter();

        assertThat(executionCounter.hasReachedExecutionLimit(), equalTo(true));
    }
}
