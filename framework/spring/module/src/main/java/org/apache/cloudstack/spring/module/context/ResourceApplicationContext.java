/*
 * Licensed to the Apache Software Foundation (ASF) under one
 * or more contributor license agreements.  See the NOTICE file
 * distributed with this work for additional information
 * regarding copyright ownership.  The ASF licenses this file
 * to you under the Apache License, Version 2.0 (the
 * "License"); you may not use this file except in compliance
 * with the License.  You may obtain a copy of the License at
 *
 *   http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing,
 * software distributed under the License is distributed on an
 * "AS IS" BASIS, WITHOUT WARRANTIES OR CONDITIONS OF ANY
 * KIND, either express or implied.  See the License for the
 * specific language governing permissions and limitations
 * under the License.
 */
package org.apache.cloudstack.spring.module.context;

import java.util.Arrays;

import org.springframework.context.support.AbstractXmlApplicationContext;
import org.springframework.core.io.Resource;

public class ResourceApplicationContext extends AbstractXmlApplicationContext {

    Resource[] configResources;
    String applicationName = "";

    public ResourceApplicationContext() {
    }

    public ResourceApplicationContext(Resource... configResources) {
        super();
        this.configResources = configResources;
    }

    @Override
    protected Resource[] getConfigResources() {
        return configResources;
    }

    public void setConfigResources(Resource[] configResources) {
        this.configResources = configResources;
    }

    @Override
    public String getApplicationName() {
        return applicationName;
    }

    public void setApplicationName(String applicationName) {
        this.applicationName = applicationName;
    }

    @Override
    public String toString() {
        return "ResourceApplicationContext [applicationName=" + applicationName + ", configResources=" + Arrays.toString(configResources) + "]";
    }

}
