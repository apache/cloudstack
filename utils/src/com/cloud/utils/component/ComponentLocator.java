// Licensed to the Apache Software Foundation (ASF) under one
// or more contributor license agreements.  See the NOTICE file
// distributed with this work for additional information
// regarding copyright ownership.  The ASF licenses this file
// to you under the Apache License, Version 2.0 (the
// "License"); you may not use this file except in compliance
// the License.  You may obtain a copy of the License at
//
// http://www.apache.org/licenses/LICENSE-2.0
//
// Unless required by applicable law or agreed to in writing,
// software distributed under the License is distributed on an
// "AS IS" BASIS, WITHOUT WARRANTIES OR CONDITIONS OF ANY
// KIND, either express or implied.  See the License for the
// specific language governing permissions and limitations
// under the License.
package com.cloud.utils.component;

import java.io.Serializable;
import java.lang.reflect.Constructor;
import java.lang.reflect.Field;
import java.lang.reflect.Method;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Enumeration;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Iterator;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Map.Entry;
import java.util.Properties;
import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;

import org.springframework.stereotype.Component;

import com.cloud.utils.db.GenericDao;
import com.cloud.utils.exception.CloudRuntimeException;
import com.cloud.utils.mgmt.JmxUtil;
import com.cloud.utils.mgmt.ManagementBean;

/**
 * ComponentLocator ties together several different concepts.  First, it 
 * deals with how a system should be put together.  It manages different 
 * types of components:
 *   - Manager: Singleton implementation of a certain process.
 *   - Adapter: Different singleton implementations for the same functions.
 *   - SystemIntegrityChecker: Singletons that are called at the load time.
 *   - Dao: Data Access Objects.
 *   
 * These components can be declared in several ways:
 *   - ComponentLibrary - A Java class that declares the above components.  The
 *     advantage of declaring components here is they change automatically
 *     with any refactoring.
 *   - components specification - An xml file that overrides the 
 *     ComponentLibrary.  The advantage of declaring components here is 
 *     they can change by hand on every deployment.
 * 
 * The two are NOT mutually exclusive.  ComponentLocator basically locates 
 * the components specification, which specifies the ComponentLibrary within.  
 * Components found in the ComponentLibrary are overridden by components 
 * found in components specification.
 * 
 * Components specification can also be nested.  One components specification
 * can point to another components specification and, therefore, "inherits"
 * those components but still override one or more components.  ComponentLocator 
 * reads the child components specification first and follow the chain up.  
 * the child's components overrides the ones in the parent. 
 * 
 * ComponentLocator looks for the components specification as follows:
 *   1. By following the path specified by "cloud-stack-components-specification"
 *      within the environment.properties file.
 *   2. Look for components.xml in the class path.
 *   
 * ComponentLocator also ties in component injection.  Components can specify 
 * an @Inject annotation to components ComponentLocator knows.  When 
 * instantiating components, ComponentLocator attempts to inject these 
 * components.
 * 
 **/
@SuppressWarnings("unchecked")
public class ComponentLocator implements ComponentLocatorMBean {
    protected static final Logger                      s_logger     = Logger.getLogger(ComponentLocator.class);

    protected static final ThreadLocal<ComponentLocator> s_tl = new ThreadLocal<ComponentLocator>();
    protected static final ConcurrentHashMap<Class<?>, Singleton> s_singletons = new ConcurrentHashMap<Class<?>, Singleton>(111);
    protected static final HashMap<String, ComponentLocator> s_locators = new HashMap<String, ComponentLocator>();
    protected static final HashMap<Class<?>, InjectInfo> s_factories = new HashMap<Class<?>, InjectInfo>();
    protected static Boolean s_once = false;
    protected static Boolean _hasCheckerRun = false;
    protected static Callback[] s_callbacks = new Callback[] { NoOp.INSTANCE, new DatabaseCallback()};
    protected static CallbackFilter s_callbackFilter = new DatabaseCallbackFilter();
    protected static final List<AnnotationInterceptor<?>> s_interceptors = new ArrayList<AnnotationInterceptor<?>>();
    protected static CleanupThread s_janitor = null;

    protected HashMap<String, Adapters<? extends Adapter>>              _adapterMap;
    protected HashMap<String, ComponentInfo<Manager>>                   _managerMap;
    protected LinkedHashMap<String, ComponentInfo<SystemIntegrityChecker>>     _checkerMap;
    protected LinkedHashMap<String, ComponentInfo<GenericDao<?, ? extends Serializable>>>    _daoMap;
    protected String                                                    _serverName;
    protected Object                                                    _component;
    protected HashMap<Class<?>, Class<?>>                               _factories;
    protected HashMap<String, ComponentInfo<PluggableService>>          _pluginsMap;

    static {
        if (s_janitor == null) {
            s_janitor = new CleanupThread();
            Runtime.getRuntime().addShutdownHook(new CleanupThread());
        }
    }

    public ComponentLocator(String server) {
        _serverName = server;
        if (s_janitor == null) {
            s_janitor = new CleanupThread();
            Runtime.getRuntime().addShutdownHook(new CleanupThread());
        }
    }

    public String getLocatorName() {
        return _serverName;
    }

    @Override
    public String getName() {
        return getLocatorName();
    }

    protected Pair<XmlHandler, HashMap<String, List<ComponentInfo<Adapter>>>> parse2(String filename) {
        try {
            SAXParserFactory spfactory = SAXParserFactory.newInstance();
            SAXParser saxParser = spfactory.newSAXParser();
            _daoMap = new LinkedHashMap<String, ComponentInfo<GenericDao<?, ? extends Serializable>>>();
            _managerMap = new LinkedHashMap<String, ComponentInfo<Manager>>();
            _checkerMap = new LinkedHashMap<String, ComponentInfo<SystemIntegrityChecker>>();
            _adapterMap = new HashMap<String, Adapters<? extends Adapter>>();
            _factories = new HashMap<Class<?>, Class<?>>();
            _pluginsMap = new LinkedHashMap<String, ComponentInfo<PluggableService>>();
            File file = PropertiesUtil.findConfigFile(filename);
            if (file == null) {
                s_logger.info("Unable to find " + filename);
                return null;
            }
            s_logger.info("Config file found at " + file.getAbsolutePath() + ".  Configuring " + _serverName);
            XmlHandler handler = new XmlHandler(_serverName);
            saxParser.parse(file, handler);

            HashMap<String, List<ComponentInfo<Adapter>>> adapters = new HashMap<String, List<ComponentInfo<Adapter>>>();
            if (handler.parent != null) {
                String[] tokens = handler.parent.split(":");
                String parentFile = filename;
                String parentName = handler.parent;
                if (tokens.length > 1) {
                    parentFile = tokens[0];
                    parentName = tokens[1];
                }
                ComponentLocator parentLocator = new ComponentLocator(parentName);
                adapters.putAll(parentLocator.parse2(parentFile).second());
                _daoMap.putAll(parentLocator._daoMap);
                _managerMap.putAll(parentLocator._managerMap);
                _factories.putAll(parentLocator._factories);
                _pluginsMap.putAll(parentLocator._pluginsMap);
            }

            ComponentLibrary library = null;
            if (handler.library != null) {
                Class<?> clazz = Class.forName(handler.library);
                library = (ComponentLibrary)clazz.newInstance();
                _daoMap.putAll(library.getDaos());
                _managerMap.putAll(library.getManagers());
                _factories.putAll(library.getFactories());
                _pluginsMap.putAll(library.getPluggableServices());
                
                 for (Entry<String, List<ComponentInfo<Adapter>>> e : library.getAdapters().entrySet()) {
                	 if (adapters.containsKey(e.getKey())) {
                 		s_logger.debug("Merge needed for " + e.getKey());
                 		adapters.get(e.getKey()).addAll(e.getValue());
                 	}
                 	else {
                 		adapters.put(e.getKey(), e.getValue());
                 	}
                 }
                // putAll overwrites existing keys, so merge instead
                for (Entry<String, List<ComponentInfo<Adapter>>> e : handler.adapters.entrySet()) {
                	if (adapters.containsKey(e.getKey())) {
                		s_logger.debug("Merge needed for " + e.getKey());
                		adapters.get(e.getKey()).addAll(e.getValue());
                	}
                	else {
                		adapters.put(e.getKey(), e.getValue());
                	}
                }
            }

            _daoMap.putAll(handler.daos);
            _managerMap.putAll(handler.managers);
            _checkerMap.putAll(handler.checkers);
            _pluginsMap.putAll(handler.pluggableServices);
            
            // putAll overwrites existing keys, so merge instead
            for (Entry<String, List<ComponentInfo<Adapter>>> e : handler.adapters.entrySet()) {
            	if (adapters.containsKey(e.getKey())) {
            		s_logger.debug("Merge needed for " + e.getKey());
            		adapters.get(e.getKey()).addAll(e.getValue());
            	}
            	else {
            		adapters.put(e.getKey(), e.getValue());
            	}
            }
            
            
            return new Pair<XmlHandler, HashMap<String, List<ComponentInfo<Adapter>>>>(handler, adapters);
        } catch (ParserConfigurationException e) {
            s_logger.error("Unable to load " + _serverName + " due to errors while parsing " + filename, e);
            System.exit(1);
        } catch (SAXException e) {
            s_logger.error("Unable to load " + _serverName + " due to errors while parsing " + filename, e);
            System.exit(1);
        } catch (IOException e) {
            s_logger.error("Unable to load " + _serverName + " due to errors while reading from " + filename, e);
            System.exit(1);
        } catch (CloudRuntimeException e) {
            s_logger.error("Unable to load configuration for " + _serverName + " from " + filename, e);
            System.exit(1);
        } catch (Exception e) {
            s_logger.error("Unable to load configuration for " + _serverName + " from " + filename, e);
            System.exit(1);
        }
        return null;
    }

    protected void parse(String filename) {
        Pair<XmlHandler, HashMap<String, List<ComponentInfo<Adapter>>>> result = parse2(filename);
        if (result == null) {
            s_logger.info("Skipping configuration using " + filename);
            return;
        }

        instantiatePluggableServices();

        XmlHandler handler = result.first();
        HashMap<String, List<ComponentInfo<Adapter>>> adapters = result.second();
        try {
            runCheckers();
            startDaos();    // daos should not be using managers and adapters.
            instantiateAdapters(adapters);
            instantiateManagers();
            if (handler.componentClass != null) {
                _component = createInstance(handler.componentClass, true, true);
            }
            configureManagers();
            configureAdapters();
            startManagers();
            startAdapters();
            //TODO do we need to follow the instantiate -> inject -> configure -> start -> stop flow of singletons like managers/adapters?
            //TODO do we need to expose pluggableServices to MBean (provide getNames?)
        } catch (CloudRuntimeException e) {
            s_logger.error("Unable to load configuration for " + _serverName + " from " + filename, e);
            System.exit(1);
        } catch (Exception e) {
            s_logger.error("Unable to load configuration for " + _serverName + " from " + filename, e);
            System.exit(1);
        }
    }

    protected void runCheckers() {
        Set<Map.Entry<String, ComponentInfo<SystemIntegrityChecker>>> entries = _checkerMap.entrySet();
        for (Map.Entry<String, ComponentInfo<SystemIntegrityChecker>> entry : entries) {
            ComponentInfo<SystemIntegrityChecker> info = entry.getValue();
            try {
                info.instance = (SystemIntegrityChecker)createInstance(info.clazz, false, info.singleton);
                info.instance.check();
            } catch (Exception e) {
                s_logger.error("Problems with running checker:" + info.name, e);
                System.exit(1);
            }
        }
    }
    /**
     * Daos should not refer to any other components so it is safe to start them
     * here.
     */
    protected void startDaos()  {
        Set<Map.Entry<String, ComponentInfo<GenericDao<?, ? extends Serializable>>>> entries = _daoMap.entrySet();

        for (Map.Entry<String, ComponentInfo<GenericDao<?, ?>>> entry : entries) {
            ComponentInfo<GenericDao<?, ?>> info = entry.getValue();
            try {
                info.instance = (GenericDao<?, ?>)createInstance(info.clazz, true, info.singleton);
                if (info.singleton) {
                    s_logger.info("Starting singleton DAO: " + info.name);
                    Singleton singleton = s_singletons.get(info.clazz);
                    if (singleton.state == Singleton.State.Instantiated) {
                        inject(info.clazz, info.instance);
                        singleton.state = Singleton.State.Injected;
                    }
                    if (singleton.state == Singleton.State.Injected) {
                        if (!info.instance.configure(info.name, info.params)) {
                            s_logger.error("Unable to configure DAO: " + info.name);
                            System.exit(1);
                        }
                        singleton.state = Singleton.State.Started;
                    }
                } else {
                    s_logger.info("Starting DAO: " + info.name);
                    inject(info.clazz, info.instance);
                    if (!info.instance.configure(info.name, info.params)) {
                        s_logger.error("Unable to configure DAO: " + info.name);
                        System.exit(1);
                    }
                }
            } catch (ConfigurationException e) {
                s_logger.error("Unable to configure DAO: " + info.name, e);
                System.exit(1);
            } catch (Exception e) {
                s_logger.error("Problems while configuring DAO: " + info.name, e);
                System.exit(1);
            }
            if (info.instance instanceof ManagementBean) {
                registerMBean((ManagementBean) info.instance);
            }
        }
    }

    private static Object createInstance(Class<?> clazz, boolean inject, boolean singleton, Object... args) {
        Factory factory = null;
        Singleton entity = null;
        synchronized(s_factories) {
            if (singleton) {
                entity = s_singletons.get(clazz);
                if (entity != null) {
                    s_logger.debug("Found singleton instantiation for " + clazz.toString());
                    return entity.singleton;
                }
            }
            InjectInfo info = s_factories.get(clazz);
            if (info == null) {
                Enhancer enhancer = new Enhancer();
                enhancer.setSuperclass(clazz);
                enhancer.setCallbackFilter(s_callbackFilter);
                enhancer.setCallbacks(s_callbacks);
                factory = (Factory)enhancer.create();
                info = new InjectInfo(enhancer, factory);
                s_factories.put(clazz, info);
            } else {
                factory = info.factory;
            }
        }


        Class<?>[] argTypes = null;
        if (args != null && args.length > 0) {
            Constructor<?>[] constructors = clazz.getConstructors();
            for (Constructor<?> constructor : constructors) {
                Class<?>[] paramTypes = constructor.getParameterTypes();
                if (paramTypes.length == args.length) {
                    boolean found = true;
                    for (int i = 0; i < paramTypes.length; i++) {
                        if (!paramTypes[i].isAssignableFrom(args[i].getClass()) && !paramTypes[i].isPrimitive()) {
                            found = false;
                            break;
                        }
                    }
                    if (found) {
                        argTypes = paramTypes;
                        break;
                    }
                }
            }

            if (argTypes == null) {
                throw new CloudRuntimeException("Unable to find constructor to match parameters given: " + clazz.getName());
            }

            entity = new Singleton(factory.newInstance(argTypes, args, s_callbacks));
        } else {
            entity = new Singleton(factory.newInstance(s_callbacks));
        }

        if (inject) {
            inject(clazz, entity.singleton);
            entity.state = Singleton.State.Injected;
        }

        if (singleton) {
            synchronized(s_factories) {
                s_singletons.put(clazz, entity);
            }
        }

        return entity.singleton;
    }

@Component
public class ComponentLocator {

    public static ComponentLocator getCurrentLocator() {
    	return ComponentContext.getCompanent(ComponentLocator.class);
    }
    
    public static ComponentLocator getLocator(String server) {
    	return ComponentContext.getCompanent(ComponentLocator.class);
    }
    
    public static ComponentLocator getLocator(String server, String configFileName, String log4jFilename) {
        return ComponentContext.getCompanent(ComponentLocator.class);
    }
    
    public static Object getComponent(String componentName) {
    	return ComponentContext.getCompanent(componentName);
    }
    
    public <T extends GenericDao<?, ? extends Serializable>> T getDao(Class<T> clazz) {
        return ComponentContext.getCompanent(clazz);
    }
    
    public <T> T getManager(Class<T> clazz) {
        return ComponentContext.getCompanent(clazz);
    }
    
    public <T> T getPluggableService(Class<T> clazz) {
        return ComponentContext.getCompanent(clazz);
    }
    
    public static <T> T inject(Class<T> clazz) {
    	return ComponentContext.inject(clazz);
    }
    
}
