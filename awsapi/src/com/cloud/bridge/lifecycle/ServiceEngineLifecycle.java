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
package com.cloud.bridge.lifecycle;

import org.apache.axis2.context.ConfigurationContext;
import org.apache.axis2.description.AxisService;
import org.apache.axis2.engine.ServiceLifeCycle;
import org.apache.log4j.Logger;

import com.cloud.bridge.service.controller.s3.ServiceProvider;

/**
 * ServiceEngineLifecycle is used to participate Axis service life cycle management
 * so that we can inject proper initialization and cleanup procedure into the
 * process
 */
public class ServiceEngineLifecycle implements ServiceLifeCycle {
    private static final long serialVersionUID = -249114759030608486L;
    public static final Logger logger = Logger.getLogger(ServiceEngineLifecycle.class);
    private static boolean initialized = false;

    public void startUp(ConfigurationContext config, AxisService service) {
        // initialize service provider during Axis engine startup
        try {
            //UserCredentialsDao.preCheckTableExistence();
            ServiceProvider.getInstance();
            ServiceEngineLifecycle.initialized = true;
        } catch (Exception e) {
            logger.error("Error initializing awsapi: " + e.getMessage());
        }
    }

    public void shutDown(ConfigurationContext config, AxisService service) {
        if (ServiceEngineLifecycle.initialized) {
            ServiceProvider.getInstance().shutdown();
        }
    }
};
