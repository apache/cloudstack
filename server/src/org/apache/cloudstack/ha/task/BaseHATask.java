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
import org.apache.cloudstack.ha.HAResource;
import org.apache.cloudstack.ha.provider.HACheckerException;
import org.apache.cloudstack.ha.provider.HAFenceException;
import org.apache.cloudstack.ha.provider.HAProvider;
import org.apache.cloudstack.ha.provider.HARecoveryException;
import org.apache.log4j.Logger;

import java.util.concurrent.Callable;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Future;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.TimeoutException;

public abstract class BaseHATask implements Callable<Boolean> {
    public static final Logger LOG = Logger.getLogger(BaseHATask.class);

    private final HAResource resource;
    private final HAProvider<HAResource> haProvider;
    private final HAConfig haConfig;
    private final ExecutorService executor;
    private Long timeout;

    public BaseHATask(final HAResource resource, final HAProvider<HAResource> haProvider, final HAConfig haConfig, final HAProvider.HAProviderConfig haProviderConfig,
            final ExecutorService executor) {
        this.resource = resource;
        this.haProvider = haProvider;
        this.haConfig = haConfig;
        this.executor = executor;
        this.timeout = (Long)haProvider.getConfigValue(haProviderConfig, resource);
    }

    public HAProvider<HAResource> getHaProvider() {
        return haProvider;
    }

    public HAConfig getHaConfig() {
        return haConfig;
    }

    public HAResource getResource() {
        return resource;
    }

    public String getTaskType() {
        return this.getClass().getSimpleName();
    }

    public boolean performAction() throws HACheckerException, HAFenceException, HARecoveryException {
        return true;
    }

    public abstract void processResult(boolean result, Throwable e);

    @Override
    public Boolean call() {
        final Future<Boolean> future = executor.submit(new Callable<Boolean>() {
            @Override
            public Boolean call() throws HACheckerException, HAFenceException, HARecoveryException {
                return performAction();
            }
        });

        boolean result = false;
        Throwable throwable = null;
        try {
            if (timeout == null) {
                result = future.get();
            } else {
                result = future.get(timeout, TimeUnit.SECONDS);
            }
        } catch (InterruptedException | ExecutionException e) {
            LOG.warn("Exception occurred while running " + getTaskType() + " on a resource: " + e.getMessage(), e.getCause());
            throwable = e.getCause();
        } catch (TimeoutException e) {
            LOG.trace(getTaskType() + " operation timed out for resource id:" + resource.getId());
        }
        processResult(result, throwable);
        return result;
    }

}
