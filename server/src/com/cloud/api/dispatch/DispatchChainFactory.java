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
    protected AccountManager _accountMgr;

    @Inject
    protected ParamGenericValidationWorker paramGenericValidationWorker;

    @Inject
    protected ParamUnpackWorker paramUnpackWorker;

    @Inject
    protected ParamProcessWorker paramProcessWorker;

    @Inject
    protected SpecificCmdValidationWorker specificCmdValidationWorker;

    @Inject
    protected CommandCreationWorker commandCreationWorker;

    protected DispatchChain standardDispatchChain;

    protected DispatchChain asyncCreationDispatchChain;

    @PostConstruct
    public void setup() {
        standardDispatchChain = new DispatchChain().
                add(paramUnpackWorker).
                add(paramProcessWorker).
                add(paramGenericValidationWorker).
                add(specificCmdValidationWorker);

        asyncCreationDispatchChain = new DispatchChain().
                add(paramUnpackWorker).
                add(paramProcessWorker).
                add(paramGenericValidationWorker).
                add(specificCmdValidationWorker).
                add(commandCreationWorker);
    }

    public DispatchChain getStandardDispatchChain() {
        return standardDispatchChain;
    }

    public DispatchChain getAsyncCreationDispatchChain() {
        return asyncCreationDispatchChain;
    }
}
