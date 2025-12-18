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
package com.cloud.network.lb;

import static java.lang.String.format;

import java.util.Map;
import java.util.concurrent.Executors;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.TimeUnit;

import javax.inject.Inject;
import javax.naming.ConfigurationException;

import org.springframework.stereotype.Component;

import org.apache.cloudstack.framework.config.dao.ConfigurationDao;
import org.apache.cloudstack.managed.context.ManagedContextRunnable;

import com.cloud.configuration.Config;
import com.cloud.exception.ResourceUnavailableException;
import com.cloud.network.rules.LoadBalancerContainer.Scheme;
import com.cloud.utils.NumbersUtil;
import com.cloud.utils.component.Manager;
import com.cloud.utils.component.ManagerBase;
import com.cloud.utils.concurrency.NamedThreadFactory;

@Component
public class LBHealthCheckManagerImpl extends ManagerBase implements LBHealthCheckManager, Manager {

    @Inject
    ConfigurationDao _configDao;
    @Inject
    LoadBalancingRulesService _lbService;

    private String name;
    private Map<String, String> _configs;
    ScheduledExecutorService _executor;

    private long _interval;

    @Override
    public boolean configure(String name, Map<String, Object> params) throws ConfigurationException {
        _configs = _configDao.getConfiguration("management-server", params);
        if (logger.isInfoEnabled()) {
            logger.info(format("Configuring LBHealthCheck Manager %1$s", name));
        }
        this.name = name;
        _interval = NumbersUtil.parseLong(_configs.get(Config.LBHealthCheck.key()), 600);
        _executor = Executors.newScheduledThreadPool(1, new NamedThreadFactory("LBHealthCheck"));
        return true;
    }

    @Override
    public boolean start() {
        logger.debug("LB HealthCheckmanager is getting Started");
        _executor.scheduleAtFixedRate(new UpdateLBHealthCheck(), 10, _interval, TimeUnit.SECONDS);
        return true;
    }

    @Override
    public boolean stop() {
        logger.debug("HealthCheckmanager is getting Stopped");
        _executor.shutdown();
        return true;
    }

    @Override
    public String getName() {
        return this.name;
    }

    protected class UpdateLBHealthCheck extends ManagedContextRunnable {
        @Override
        protected void runInContext() {
            try {
                updateLBHealthCheck(Scheme.Public);
                updateLBHealthCheck(Scheme.Internal);
            } catch (Exception e) {
                logger.error("Exception in LB HealthCheck Update Checker", e);
            }
        }
    }

    @Override
    public void updateLBHealthCheck(Scheme scheme) {
        try {
            _lbService.updateLBHealthChecks(scheme);
        } catch (ResourceUnavailableException e) {
            logger.debug("Error while updating the LB HealtCheck ", e);
        }
        logger.debug("LB HealthCheck Manager is running and getting the updates from LB providers and updating service status");
    }

}
