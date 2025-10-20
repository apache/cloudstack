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

import java.util.concurrent.ExecutorService;

import javax.inject.Inject;

import org.apache.cloudstack.ha.HAConfig;
import org.apache.cloudstack.ha.HAManager;
import org.apache.cloudstack.ha.HAResource;
import org.apache.cloudstack.ha.HAResourceCounter;
import org.apache.cloudstack.ha.provider.HACheckerException;
import org.apache.cloudstack.ha.provider.HAProvider;
import org.apache.cloudstack.ha.provider.HAProvider.HAProviderConfig;
import org.joda.time.DateTime;

public class ActivityCheckTask extends BaseHATask {


    @Inject
    private HAManager haManager;

    private long disconnectTime;
    private long maxActivityChecks;
    private double activityCheckFailureRatio;

    public ActivityCheckTask(final HAResource resource, final HAProvider<HAResource> haProvider, final HAConfig haConfig, final HAProvider.HAProviderConfig haProviderConfig,
            final ExecutorService executor, final long disconnectTime) {
        super(resource, haProvider, haConfig, haProviderConfig, executor);
        this.disconnectTime = disconnectTime;
        this.maxActivityChecks = (Long)haProvider.getConfigValue(HAProviderConfig.MaxActivityChecks, resource);
        this.activityCheckFailureRatio = (Double)haProvider.getConfigValue(HAProviderConfig.ActivityCheckFailureRatio, resource);
    }

    public boolean performAction() throws HACheckerException {
        return getHaProvider().hasActivity(getResource(), new DateTime(disconnectTime));
    }

    public synchronized void processResult(boolean result, Throwable t) {
        final HAConfig haConfig = getHaConfig();
        final HAResourceCounter counter = haManager.getHACounter(haConfig.getResourceId(), haConfig.getResourceType());

        if (t != null && t instanceof HACheckerException) {
            haManager.transitionHAState(HAConfig.Event.Ineligible, getHaConfig());
            counter.resetActivityCounter();
            return;
        }

        counter.incrActivityCounter(!result);

        if (counter.getActivityCheckCounter() < maxActivityChecks) {
            haManager.transitionHAState(HAConfig.Event.TooFewActivityCheckSamples, haConfig);
            return;
        }

        if (counter.hasActivityThresholdExceeded(activityCheckFailureRatio)) {
            haManager.transitionHAState(HAConfig.Event.ActivityCheckFailureOverThresholdRatio, haConfig);
        } else {
            if (haManager.transitionHAState(HAConfig.Event.ActivityCheckFailureUnderThresholdRatio, haConfig)) {
                counter.markResourceDegraded();
            }
        }
        counter.resetActivityCounter();
    }
}
