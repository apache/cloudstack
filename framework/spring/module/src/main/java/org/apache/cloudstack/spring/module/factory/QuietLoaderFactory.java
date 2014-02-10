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
package org.apache.cloudstack.spring.module.factory;

import java.util.ArrayList;
import java.util.List;

import org.springframework.beans.factory.FactoryBean;
import org.springframework.core.io.Resource;

public class QuietLoaderFactory implements FactoryBean<Resource[]> {

    Resource[] resources;

    @Override
    public Resource[] getObject() throws Exception {
        List<Resource> existing = new ArrayList<Resource>();

        for (Resource resource : resources) {
            if (resource.exists()) {
                existing.add(resource);
            }
        }

        return existing.toArray(new Resource[existing.size()]);
    }

    @Override
    public Class<?> getObjectType() {
        return Resource[].class;
    }

    @Override
    public boolean isSingleton() {
        return false;
    }

    public Resource[] getResources() {
        return resources;
    }

    public void setResources(Resource[] resources) {
        this.resources = resources;
    }

}
