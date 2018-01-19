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

package org.apache.cloudstack.ha.task;

import org.apache.cloudstack.ha.HAConfig;
import org.apache.cloudstack.ha.HAManager;
import org.apache.cloudstack.ha.HAResource;
import org.apache.cloudstack.ha.HAResourceCounter;
import org.apache.cloudstack.ha.provider.HACheckerException;
import org.apache.cloudstack.ha.provider.HAFenceException;
import org.apache.cloudstack.ha.provider.HAProvider;

import javax.inject.Inject;
import java.util.concurrent.ExecutorService;

public class FenceTask extends BaseHATask {

    @Inject
    private HAManager haManager;

    public FenceTask(final HAResource resource, final HAProvider<HAResource> haProvider, final HAConfig haConfig,
                     final HAProvider.HAProviderConfig haProviderConfig, final ExecutorService executor) {
        super(resource, haProvider, haConfig, haProviderConfig, executor);
    }

    public boolean performAction() throws HACheckerException, HAFenceException {
        return getHaProvider().fence(getResource());
    }

    public void processResult(boolean result, Throwable e) {
        final HAConfig haConfig = getHaConfig();
        final HAResourceCounter counter = haManager.getHACounter(haConfig.getResourceId(), haConfig.getResourceType());
        if (result) {
            counter.resetRecoveryCounter();
            haManager.transitionHAState(HAConfig.Event.Fenced, haConfig);
            getHaProvider().fenceSubResources(getResource());
            getHaProvider().enableMaintenance(getResource());
        }
        getHaProvider().sendAlert(getResource(), HAConfig.HAState.Fencing);
    }
}
