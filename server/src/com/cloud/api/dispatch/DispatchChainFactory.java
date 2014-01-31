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

import javax.annotation.PostConstruct;
import javax.inject.Inject;

import com.cloud.user.AccountManager;

public class DispatchChainFactory {

    @Inject
    protected AccountManager _accountMgr = null;

    @Inject
    protected ParamGenericValidationWorker paramGenericValidationWorker = null;

    @Inject
    protected ParamProcessWorker paramProcessWorker = null;

    @Inject
    protected ParamSemanticValidationWorker paramSemanticValidationWorker = null;

    @Inject
    protected CommandCreationWorker commandCreationWorker = null;

    protected DispatchChain standardDispatchChain = null;

    protected DispatchChain asyncCreationDispatchChain = null;

    @PostConstruct
    public void setup() {
        standardDispatchChain = new DispatchChain().
                add(paramGenericValidationWorker).
                add(paramProcessWorker).
                add(paramSemanticValidationWorker);

        asyncCreationDispatchChain = new DispatchChain().
                add(paramGenericValidationWorker).
                add(paramProcessWorker).
                add(paramSemanticValidationWorker).
                add(commandCreationWorker);

    }

    public DispatchChain getStandardDispatchChain() {
        return standardDispatchChain;
    }

    public DispatchChain getAsyncCreationDispatchChain() {
        return asyncCreationDispatchChain;
    }
}
