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
package org.apache.cloudstack.spring.module.model.impl;

import java.io.IOException;
import java.io.InputStream;
import java.util.Arrays;
import java.util.Collection;
import java.util.List;
import java.util.Map;
import java.util.Properties;
import java.util.TreeMap;

import org.apache.cloudstack.spring.module.model.ModuleDefinition;
import org.apache.cloudstack.spring.module.util.ModuleLocationUtils;
import org.apache.commons.io.IOUtils;
import org.springframework.core.io.Resource;
import org.springframework.core.io.support.ResourcePatternResolver;
import org.springframework.util.StringUtils;

public class DefaultModuleDefinition implements ModuleDefinition {

    public static final String NAME = "name";
    public static final String PARENT = "parent";
        
    String name;
    String baseDir;
    String parent;
    Resource moduleProperties;
    ResourcePatternResolver resolver;
    boolean valid;
    
    List<Resource> configLocations;
    List<Resource> contextLocations;
    List<Resource> inheritableContextLocations;
    List<Resource> overrideContextLocations;
    Map<String, ModuleDefinition> children = new TreeMap<String, ModuleDefinition>();
    
    public DefaultModuleDefinition(String baseDir, Resource moduleProperties, ResourcePatternResolver resolver) {
        this.baseDir = baseDir;
        this.resolver = resolver;
        this.moduleProperties = moduleProperties;
    }
    
    public void init() throws IOException {
        
        if ( ! moduleProperties.exists() ) {
            return;
        }
        
        resolveNameAndParent();
        
        contextLocations = Arrays.asList(resolver.getResources(ModuleLocationUtils.getContextLocation(baseDir, name)));
        configLocations = Arrays.asList(resolver.getResources(ModuleLocationUtils.getDefaultsLocation(baseDir, name)));
        inheritableContextLocations = Arrays.asList(resolver.getResources(ModuleLocationUtils.getInheritableContextLocation(baseDir, name)));
        overrideContextLocations = Arrays.asList(resolver.getResources(ModuleLocationUtils.getOverrideContextLocation(baseDir, name)));

        valid = true;
    }
    
    protected void resolveNameAndParent() throws IOException {
        InputStream is = null;
        
        try {
            is = moduleProperties.getInputStream();
            Properties props = new Properties();
            props.load(is);
            
            name = props.getProperty(NAME);
            parent = props.getProperty(PARENT);
            
            if ( ! StringUtils.hasText(name) ) {
                throw new IOException("Missing name property in [" + location() + "]");
            }
            
            if ( ! StringUtils.hasText(parent) ) {
                parent = null;
            }
            
            checkNameMatchesSelf();
        } finally {
            IOUtils.closeQuietly(is);
        }
    }
    
    protected void checkNameMatchesSelf() throws IOException {
        String expectedLocation = ModuleLocationUtils.getModuleLocation(baseDir, name);
        Resource self = resolver.getResource(expectedLocation);
        
        if ( ! self.exists() ) {
            throw new IOException("Resource [" + location() + "] is expected to exist at [" +
                    expectedLocation + "] please ensure the name property is correct");
        }
        
        String moduleUrl = moduleProperties.getURL().toExternalForm();
        String selfUrl = self.getURL().toExternalForm();
            
        if ( ! moduleUrl.equals(selfUrl) ) {
            throw new IOException("Resource [" + location() + "] and [" +
                    self.getURL() + "] do not appear to be the same resource, " + 
                    "please ensure the name property is correct");
        }
    }
    
    private String location() throws IOException {
        return moduleProperties.getURL().toString();
    }
    
    public void addChild(ModuleDefinition def) {
        children.put(def.getName(), def);
    }
    
    public Collection<ModuleDefinition> getChildren() {
        return children.values();
    }

    public String getName() {
        return name;
    }

    public String getParentName() {
        return parent;
    }

    public List<Resource> getConfigLocations() {
        return configLocations;
    }

    public List<Resource> getContextLocations() {
        return contextLocations;
    }

    public List<Resource> getInheritableContextLocations() {
        return inheritableContextLocations;
    }

    @Override
    public List<Resource> getOverrideContextLocations() {
        return overrideContextLocations;
    }

    public boolean isValid() {
        return valid;
    }

    public ClassLoader getClassLoader() {
        return resolver.getClassLoader();
    }


}
