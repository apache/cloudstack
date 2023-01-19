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
import java.net.URL;
import java.util.ArrayList;
import java.util.EmptyStackException;
import java.util.HashMap;
import java.util.HashSet;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Map;
import java.util.Properties;
import java.util.Set;
import java.util.Stack;

import org.apache.commons.io.IOUtils;
import org.apache.logging.log4j.Logger;
import org.apache.logging.log4j.LogManager;
import org.springframework.beans.BeansException;
import org.springframework.context.ApplicationContext;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.core.io.Resource;
import org.springframework.core.io.UrlResource;
import org.springframework.util.StringUtils;

import org.apache.cloudstack.spring.module.context.ResourceApplicationContext;
import org.apache.cloudstack.spring.module.model.ModuleDefinition;
import org.apache.cloudstack.spring.module.model.ModuleDefinitionSet;

public class DefaultModuleDefinitionSet implements ModuleDefinitionSet {

    protected Logger logger = LogManager.getLogger(getClass());

    public static final String DEFAULT_CONFIG_RESOURCES = "DefaultConfigResources";
    public static final String DEFAULT_CONFIG_PROPERTIES = "DefaultConfigProperties";
    public static final String MODULES_EXCLUDE = "modules.exclude";
    public static final String MODULES_INCLUDE_PREFIX = "modules.include.";
    public static final String MODULE_PROPERITES = "ModuleProperties";
    public static final String DEFAULT_CONFIG_XML = "defaults-context.xml";

    String root;
    Map<String, ModuleDefinition> modules;
    Map<String, ApplicationContext> contexts = new HashMap<String, ApplicationContext>();
    ApplicationContext rootContext = null;
    Set<String> excludes = new HashSet<String>();
    Properties configProperties = null;

    public DefaultModuleDefinitionSet(Map<String, ModuleDefinition> modules, String root) {
        super();
        this.root = root;
        this.modules = modules;
    }

    public void load() throws IOException {
        if (!loadRootContext())
            return;

        printHierarchy();
        loadContexts();
        startContexts();
    }

    protected boolean loadRootContext() {
        ModuleDefinition def = modules.get(root);

        if (def == null)
            return false;

        ApplicationContext defaultsContext = getDefaultsContext();

        rootContext = loadContext(def, defaultsContext);

        return true;
    }

    protected void startContexts() {
        withModule(new WithModule() {
            @Override
            public void with(ModuleDefinition def, Stack<ModuleDefinition> parents) {
                try {
                    ApplicationContext context = getApplicationContext(def.getName());
                    try {
                        Runnable runnable = context.getBean("moduleStartup", Runnable.class);
                        logger.info("Starting module [" + def.getName() + "]");
                        runnable.run();
                    } catch (BeansException e) {
                        // Ignore
                    }
                } catch (EmptyStackException e) {
                    // The root context is already loaded, so ignore the exception
                }
            }
        });
    }

    protected void loadContexts() {
        withModule(new WithModule() {
            @Override
            public void with(ModuleDefinition def, Stack<ModuleDefinition> parents) {
                try {
                    ApplicationContext parent = getApplicationContext(parents.peek().getName());
                    loadContext(def, parent);
                } catch (EmptyStackException e) {
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
        if (logger.isInfoEnabled()) {
            for (Resource resource : resources) {
                logger.info("Loading module context [" + def.getName() + "] from " + resource);
            }
        }
        context.refresh();
        logger.info("Loaded module context [" + def.getName() + "] in " + (System.currentTimeMillis() - start) + " ms");

        contexts.put(def.getName(), context);

        return context;
    }

    protected boolean shouldLoad(ModuleDefinition def) {
        return !excludes.contains(def.getName());
    }

    protected ApplicationContext getDefaultsContext() {
        URL config = DefaultModuleDefinitionSet.class.getResource(DEFAULT_CONFIG_XML);

        ResourceApplicationContext context = new ResourceApplicationContext(new UrlResource(config));
        context.setApplicationName("/defaults");
        context.refresh();

        @SuppressWarnings("unchecked")
        final List<Resource> resources = (List<Resource>)context.getBean(DEFAULT_CONFIG_RESOURCES);

        withModule(new WithModule() {
            @Override
            public void with(ModuleDefinition def, Stack<ModuleDefinition> parents) {
                for (Resource defaults : def.getConfigLocations()) {
                    resources.add(defaults);
                }
            }
        });

        configProperties = (Properties)context.getBean(DEFAULT_CONFIG_PROPERTIES);
        for (Resource resource : resources) {
            load(resource, configProperties);
        }

        for (Resource resource : (Resource[])context.getBean(MODULE_PROPERITES)) {
            load(resource, configProperties);
        }

        parseExcludes();

        return context;
    }

    protected void parseExcludes() {
        for (String exclude : configProperties.getProperty(MODULES_EXCLUDE, "").trim().split("\\s*,\\s*")) {
            if (StringUtils.hasText(exclude)) {
                excludes.add(exclude);
            }
        }

        for (String key : configProperties.stringPropertyNames()) {
            if (key.startsWith(MODULES_INCLUDE_PREFIX)) {
                String module = key.substring(MODULES_INCLUDE_PREFIX.length());
                boolean include = configProperties.getProperty(key).equalsIgnoreCase("true");
                if (!include) {
                    excludes.add(module);
                }
            }
        }
    }

    protected void load(Resource resource, Properties props) {
        InputStream is = null;
        try {
            if (resource.exists()) {
                is = resource.getInputStream();
                props.load(is);
            }
        } catch (IOException e) {
            throw new IllegalStateException("Failed to load resource [" + resource + "]", e);
        } finally {
            IOUtils.closeQuietly(is);
        }
    }

    protected void printHierarchy() {
        withModule(new WithModule() {
            @Override
            public void with(ModuleDefinition def, Stack<ModuleDefinition> parents) {
                logger.info(String.format("Module Hierarchy:%" + ((parents.size() * 2) + 1) + "s%s", "", def.getName()));
            }
        });
    }

    protected void withModule(WithModule with) {
        ModuleDefinition rootDef = modules.get(root);
        withModule(rootDef, new Stack<ModuleDefinition>(), with);
    }

    protected void withModule(ModuleDefinition def, Stack<ModuleDefinition> parents, WithModule with) {
        if (def == null)
            return;

        if (!shouldLoad(def)) {
            logger.info("Excluding context [" + def.getName() + "] based on configuration");
            return;
        }

        with.with(def, parents);

        parents.push(def);

        for (ModuleDefinition child : def.getChildren()) {
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

    @Override
    public ApplicationContext getApplicationContext(String name) {
        return contexts.get(name);
    }

    @Override
    public Map<String, ApplicationContext> getContextMap() {
        return contexts;
    }

    @Override
    public Resource[] getConfigResources(String name) {
        Set<Resource> resources = new LinkedHashSet<Resource>();

        ModuleDefinition original = null;
        ModuleDefinition def = original = modules.get(name);

        if (def == null)
            return new Resource[] {};

        resources.addAll(def.getContextLocations());

        while (def != null) {
            resources.addAll(def.getInheritableContextLocations());
            def = modules.get(def.getParentName());
        }

        resources.addAll(original.getOverrideContextLocations());

        return resources.toArray(new Resource[resources.size()]);
    }

    @Override
    public ModuleDefinition getModuleDefinition(String name) {
        return modules.get(name);
    }
}
