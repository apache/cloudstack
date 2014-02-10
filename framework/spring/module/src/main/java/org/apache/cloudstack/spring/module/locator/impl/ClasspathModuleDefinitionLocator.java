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
package org.apache.cloudstack.spring.module.locator.impl;

import java.io.IOException;
import java.util.Collection;
import java.util.HashMap;
import java.util.Map;

import org.springframework.core.io.Resource;
import org.springframework.core.io.support.PathMatchingResourcePatternResolver;
import org.springframework.core.io.support.ResourcePatternResolver;

import org.apache.cloudstack.spring.module.locator.ModuleDefinitionLocator;
import org.apache.cloudstack.spring.module.model.ModuleDefinition;
import org.apache.cloudstack.spring.module.model.impl.DefaultModuleDefinition;
import org.apache.cloudstack.spring.module.util.ModuleLocationUtils;

public class ClasspathModuleDefinitionLocator implements ModuleDefinitionLocator {

    protected ResourcePatternResolver getResolver() {
        return new PathMatchingResourcePatternResolver();
    }

    @Override
    public Collection<ModuleDefinition> locateModules(String context) throws IOException {
        ResourcePatternResolver resolver = getResolver();

        Map<String, ModuleDefinition> allModules = discoverModules(context, resolver);

        return allModules.values();
    }

    protected Map<String, ModuleDefinition> discoverModules(String baseDir, ResourcePatternResolver resolver) throws IOException {
        Map<String, ModuleDefinition> result = new HashMap<String, ModuleDefinition>();

        for (Resource r : resolver.getResources(ModuleLocationUtils.getModulesLocation(baseDir))) {
            DefaultModuleDefinition def = new DefaultModuleDefinition(baseDir, r, resolver);
            def.init();

            if (def.isValid())
                result.put(def.getName(), def);
        }

        return result;
    }

}
