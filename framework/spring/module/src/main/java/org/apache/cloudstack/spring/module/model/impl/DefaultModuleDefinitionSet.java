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
import java.net.URL;
import java.util.ArrayList;
import java.util.EmptyStackException;
import java.util.HashMap;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.Stack;

import org.apache.cloudstack.spring.module.context.ResourceApplicationContext;
import org.apache.cloudstack.spring.module.model.ModuleDefinition;
import org.apache.cloudstack.spring.module.model.ModuleDefinitionSet;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.BeansException;
import org.springframework.context.ApplicationContext;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.core.io.Resource;
import org.springframework.core.io.UrlResource;

public class DefaultModuleDefinitionSet implements ModuleDefinitionSet {

    private static final Logger log = LoggerFactory.getLogger(DefaultModuleDefinitionSet.class);
    
    public static final String DEFAULT_CONFIG_RESOURCES = "DefaultConfigResources";
    public static final String DEFAULT_CONFIG_XML = "defaults-context.xml";
    
    String root;
    Map<String, ModuleDefinition> modules;
    Map<String, ApplicationContext> contexts = new HashMap<String, ApplicationContext>();
    ApplicationContext rootContext = null;

    public DefaultModuleDefinitionSet(Map<String, ModuleDefinition> modules, String root) {
        super();
        this.root = root;
        this.modules = modules;
    }

    public void load() throws IOException {
        if ( ! loadRootContext() )
            return;
        
        printHierarchy();
        loadContexts();
        startContexts();
    }
    
    protected boolean loadRootContext() {
        ModuleDefinition def = modules.get(root);
        
        if ( def == null )
            return false;
        
        ApplicationContext defaultsContext = getDefaultsContext();
        
        rootContext = loadContext(def, defaultsContext);
        
        return true;
    }
    
    protected void startContexts() {
        withModule(new WithModule() {
            public void with(ModuleDefinition def, Stack<ModuleDefinition> parents) {
                try {
                    ApplicationContext context = getApplicationContext(def.getName());
                    try {
                        Runnable runnable = context.getBean("moduleStartup", Runnable.class);
                        log.info("Starting module [{}]", def.getName());
                        runnable.run();
                    } catch ( BeansException e ) {
                       // Ignore 
                    }
                } catch ( EmptyStackException e ) {
                    // The root context is already loaded, so ignore the exception
                }
            }
        });
    }
    
    protected void loadContexts() {
        withModule(new WithModule() {
            public void with(ModuleDefinition def, Stack<ModuleDefinition> parents) {
                try {
                    ApplicationContext parent = getApplicationContext(parents.peek().getName());
                    loadContext(def, parent);
                } catch ( EmptyStackException e ) {
                    // The root context is already loaded, so ignore the exception
                }
            }
        });
    }
    protected ApplicationContext loadContext(ModuleDefinition def, ApplicationContext parent) {
        ResourceApplicationContext context = new ResourceApplicationContext();
        context.setApplicationName("/" + def.getName());
        
        Resource[] resources = getConfigResources(def.getName());
        context.setConfigResources(resources);
        context.setParent(parent);
        context.setClassLoader(def.getClassLoader());

        long start = System.currentTimeMillis();
        if ( log.isInfoEnabled() ) {
            for ( Resource resource : resources ) {
                log.info("Loading module context [{}] from {}", def.getName(), resource);
            }
        }
        context.refresh();
        log.info("Loaded module context [{}] in {} ms", def.getName(), (System.currentTimeMillis() - start));
        
        contexts.put(def.getName(), context);
        
        return context;
    }
    
    protected boolean shouldLoad(ModuleDefinition def) {
        return true;
    }
    
    protected ApplicationContext getDefaultsContext() {
        URL config = DefaultModuleDefinitionSet.class.getResource(DEFAULT_CONFIG_XML);
        
        ResourceApplicationContext context = new ResourceApplicationContext(new UrlResource(config));
        context.setApplicationName("/defaults");
        context.refresh();
        
        @SuppressWarnings("unchecked")
        final List<Resource> resources = (List<Resource>) context.getBean(DEFAULT_CONFIG_RESOURCES);
        
        withModule(new WithModule() {
            public void with(ModuleDefinition def, Stack<ModuleDefinition> parents) {
                for ( Resource defaults : def.getConfigLocations() ) {
                    resources.add(defaults);
                }
            }
        });
        
        return context;
    }
    
    protected void printHierarchy() {
        withModule(new WithModule() {
            public void with(ModuleDefinition def, Stack<ModuleDefinition> parents) {
                log.info(String.format("Module Hierarchy:%" + ((parents.size() * 2) + 1) + "s%s", "", def.getName()));
            }
        });
    }
    
    protected void withModule(WithModule with) {
        ModuleDefinition rootDef = modules.get(root);
        withModule(rootDef, new Stack<ModuleDefinition>(), with);
    }
    
    protected void withModule(ModuleDefinition def, Stack<ModuleDefinition> parents, WithModule with) {
        if ( def == null )
            return;
        
        if ( ! shouldLoad(def) ) {
            return;
        }
        
        with.with(def, parents);
        
        parents.push(def);
        
        for ( ModuleDefinition child : def.getChildren() ) {
            withModule(child, parents, with);
        }
        
        parents.pop();
    }
    
    private static interface WithModule {
        public void with(ModuleDefinition def, Stack<ModuleDefinition> parents);
    }
    
    @Configuration
    public static class ConfigContext {
        
        List<Resource> resources;
        
        public ConfigContext(List<Resource> resources) {
            super();
            this.resources = resources;
        }

        @Bean(name = DEFAULT_CONFIG_RESOURCES)
        public List<Resource> defaultConfigResources() {
            return new ArrayList<Resource>();
        }
    }

    public ApplicationContext getApplicationContext(String name) {
        return contexts.get(name);
    }

    public Resource[] getConfigResources(String name) {
        Set<Resource> resources = new LinkedHashSet<Resource>();
        
        ModuleDefinition original = null;
        ModuleDefinition def = original = modules.get(name);
        
        if ( def == null )
            return new Resource[] {};
        
        resources.addAll(def.getContextLocations());
        
        while ( def != null ) {
            resources.addAll(def.getInheritableContextLocations());
            def = modules.get(def.getParentName());
        }
        
        resources.addAll(original.getOverrideContextLocations());
        
        return resources.toArray(new Resource[resources.size()]);
    }

    public ModuleDefinition getModuleDefinition(String name) {
        return modules.get(name);
    }
}
