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
import org.apache.cloudstack.ha.provider.HAProvider;
import org.apache.cloudstack.ha.provider.HAProvider.HAProviderConfig;
import org.apache.log4j.Logger;

import javax.inject.Inject;

import org.joda.time.DateTime;
import java.util.concurrent.ExecutorService;

public class ActivityCheckTask extends BaseHATask {

    public static final Logger LOG = Logger.getLogger(ActivityCheckTask.class);

    @Inject
    private HAManager haManager;

    private final long disconnectTime;

    public ActivityCheckTask(final HAResource resource, final HAProvider<HAResource> haProvider, final HAConfig haConfig, final HAProvider.HAProviderConfig haProviderConfig,
            final ExecutorService executor, final long disconnectTime) {
        super(resource, haProvider, haConfig, haProviderConfig, executor);
        this.disconnectTime = disconnectTime;
    }

    public boolean performAction() throws HACheckerException {
        return getHaProvider().hasActivity(getResource(), new DateTime(disconnectTime));
    }

    public void processResult(boolean result, Throwable t) {
        final HAConfig haConfig = getHaConfig();
        final HAProvider<HAResource> haProvider = getHaProvider();
        final HAResource resource = getResource();
        final HAResourceCounter counter = haManager.getHACounter(haConfig.getResourceId(), haConfig.getResourceType());

        if (t != null && t instanceof HACheckerException) {
            haManager.transitionHAState(HAConfig.Event.Ineligible, getHaConfig());
            counter.resetActivityCounter();
            return;
        }

        counter.incrActivityCounter(!result);

        long maxActivityChecks = (Long)haProvider.getConfigValue(HAProviderConfig.MaxActivityChecks, resource);
        if (counter.getActivityCheckCounter() < maxActivityChecks) {
            haManager.transitionHAState(HAConfig.Event.TooFewActivityCheckSamples, haConfig);
            return;
        }

        double activityCheckFailureRatio = (Double)haProvider.getConfigValue(HAProviderConfig.ActivityCheckFailureRatio, resource);
        if (counter.hasActivityThresholdExceeded(activityCheckFailureRatio)) {
            haManager.transitionHAState(HAConfig.Event.ActivityCheckFailureOverThresholdRatio, haConfig);
        } else {
            haManager.transitionHAState(HAConfig.Event.ActivityCheckFailureUnderThresholdRatio, haConfig);
            counter.markResourceDegraded();
        }
        counter.resetActivityCounter();
    }
}
