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
package com.cloud.api.dispatch;

import org.junit.Test;

import static org.junit.Assert.assertEquals;

public class DispatchChainFactoryTest {

    protected static final String STANDARD_CHAIN_ERROR = "Expecting worker of class %s at index %s of StandardChain";
    protected static final String ASYNC_CHAIN_ERROR = "Expecting worker of class %s at index %s of StandardChain";

    @Test
    public void testAllChainCreation() {
        // Prepare
        final DispatchChainFactory dispatchChainFactory = new DispatchChainFactory();
        dispatchChainFactory.paramGenericValidationWorker = new ParamGenericValidationWorker();
        dispatchChainFactory.specificCmdValidationWorker = new SpecificCmdValidationWorker();
        dispatchChainFactory.paramProcessWorker = new ParamProcessWorker();
        dispatchChainFactory.commandCreationWorker = new CommandCreationWorker();
        dispatchChainFactory.paramUnpackWorker = new ParamUnpackWorker();

        final Class<?>[] standardClasses = {ParamUnpackWorker.class, ParamProcessWorker.class,
                ParamGenericValidationWorker.class, SpecificCmdValidationWorker.class};
        final Class<?>[] asyncClasses = {ParamUnpackWorker.class, ParamProcessWorker.class,
                ParamGenericValidationWorker.class, SpecificCmdValidationWorker.class, CommandCreationWorker.class};

        // Execute
        dispatchChainFactory.setup();
        final DispatchChain standardChain = dispatchChainFactory.getStandardDispatchChain();
        final DispatchChain asyncChain = dispatchChainFactory.getAsyncCreationDispatchChain();
        for (int i = 0; i < standardClasses.length; i++) {
            assertEquals(String.format(STANDARD_CHAIN_ERROR, standardClasses[i], i),
                    standardClasses[i], standardChain.workers.get(i).getClass());
        }
        for (int i = 0; i < asyncClasses.length; i++) {
            assertEquals(String.format(ASYNC_CHAIN_ERROR, asyncClasses[i], i),
                    asyncClasses[i], asyncChain.workers.get(i).getClass());
        }
    }
}
