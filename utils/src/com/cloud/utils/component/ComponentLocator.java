/**
 *  Copyright (C) 2010 Cloud.com, Inc.  All rights reserved.
 * 
 * This software is licensed under the GNU General Public License v3 or later.
 * 
 * It is free software: you can redistribute it and/or modify
 * it under the terms of the GNU General Public License as published by
 * the Free Software Foundation, either version 3 of the License, or any later version.
 * This program is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 * GNU General Public License for more details.
 * 
 * You should have received a copy of the GNU General Public License
 * along with this program.  If not, see <http://www.gnu.org/licenses/>.
 * 
 */
package com.cloud.utils.component;

import java.io.File;
import java.io.IOException;
import java.io.Serializable;
import java.lang.reflect.Constructor;
import java.lang.reflect.Field;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Enumeration;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Iterator;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;

import javax.ejb.Local;
import javax.management.InstanceAlreadyExistsException;
import javax.management.MBeanRegistrationException;
import javax.management.MalformedObjectNameException;
import javax.management.NotCompliantMBeanException;
import javax.naming.ConfigurationException;
import javax.xml.parsers.ParserConfigurationException;
import javax.xml.parsers.SAXParser;
import javax.xml.parsers.SAXParserFactory;

import net.sf.cglib.proxy.Callback;
import net.sf.cglib.proxy.CallbackFilter;
import net.sf.cglib.proxy.Enhancer;
import net.sf.cglib.proxy.Factory;
import net.sf.cglib.proxy.NoOp;

import org.apache.log4j.Logger;
import org.apache.log4j.PropertyConfigurator;
import org.apache.log4j.xml.DOMConfigurator;
import org.xml.sax.Attributes;
import org.xml.sax.SAXException;
import org.xml.sax.helpers.DefaultHandler;

import com.cloud.utils.Pair;
import com.cloud.utils.PropertiesUtil;
import com.cloud.utils.db.DatabaseCallback;
import com.cloud.utils.db.DatabaseCallbackFilter;
import com.cloud.utils.db.GenericDao;
import com.cloud.utils.exception.CloudRuntimeException;
import com.cloud.utils.mgmt.JmxUtil;
import com.cloud.utils.mgmt.ManagementBean;

/**
 * ComponentLocator manages all of the adapters within a system. It operates on
 * top of an components.xml and uses reflection to instantiate all of the
 * adapters. It also supports rereading of all of the adapters.
 * 
 **/
@SuppressWarnings("unchecked")
public class ComponentLocator extends Thread implements ComponentLocatorMBean {
    protected static final Logger                      s_logger     = Logger.getLogger(ComponentLocator.class);
    
    protected static HashMap<String, Object> s_singletons = new HashMap<String, Object>(111);
    private static boolean                             s_doOnce = false;
    protected static final Callback[] s_callbacks = new Callback[] { NoOp.INSTANCE, new DatabaseCallback() };
    protected static final CallbackFilter s_callbackFilter = new DatabaseCallbackFilter();
    protected static final HashMap<Class<?>, InjectInfo> s_factories = new HashMap<Class<?>, InjectInfo>();
    protected static HashMap<String, ComponentLocator> s_locatorMap = new HashMap<String, ComponentLocator>();
    protected static HashMap<String, Object>           _componentMap = new HashMap<String, Object>();
    protected static HashMap<String, String>           _implementationClassMap = new HashMap<String, String>();

    protected HashMap<String, Adapters<? extends Adapter>> _adapterMap;
    protected HashMap<String, ComponentInfo<Manager>>           _managerMap;
    protected LinkedHashMap<String, ComponentInfo<GenericDao<?, ?>>>  _daoMap;
    protected ComponentLocator                         _parentLocator;
    protected String                                   _serverName;

    public ComponentLocator(String server) {
        _parentLocator = null;
        _serverName = server;
        Runtime.getRuntime().addShutdownHook(this);
    }

    public String getLocatorName() {
        return _serverName;
    }

    @Override
	public synchronized void run() {
        Iterator<Adapters<? extends Adapter>> itAdapters = _adapterMap.values().iterator();
        while (itAdapters.hasNext()) {
            Adapters<? extends Adapter> adapters = itAdapters.next();
            itAdapters.remove();
            Enumeration<? extends Adapter> it = adapters.enumeration();
            while (it.hasMoreElements()) {
                Adapter adapter = it.nextElement();
                adapter.stop();
            }
        }

        Iterator<ComponentInfo<Manager>> itManagers = _managerMap.values().iterator();
        while (itManagers.hasNext()) {
            ComponentInfo<Manager> manager = itManagers.next();
            itManagers.remove();
            manager.instance.stop();
        }
    }

    protected void parse(String filename, String log4jFile) {
        try {
            SAXParserFactory spfactory = SAXParserFactory.newInstance();
            SAXParser saxParser = spfactory.newSAXParser();
            File file = PropertiesUtil.findConfigFile(filename);
            if (file == null) {
                s_logger.warn("Unable to find the config file automatically.  Now checking properties files.");
                _parentLocator = null;
                _managerMap = new HashMap<String, ComponentInfo<Manager>>();
                _adapterMap = new HashMap<String, Adapters<? extends Adapter>>();
                _daoMap = new LinkedHashMap<String, ComponentInfo<GenericDao<?, ?>>>();
                _parentLocator = null;
                return;
            }
            s_logger.info("Config file found at " + file.getAbsolutePath() + ".  Configuring " + _serverName);
            XmlHandler handler = new XmlHandler(_serverName);
            saxParser.parse(file, handler);

            if (handler.parent != null) {
                _parentLocator = getLocatorInternal(handler.parent, false, filename, log4jFile);
            }

            _daoMap = new LinkedHashMap<String, ComponentInfo<GenericDao<?, ? extends Serializable>>>();
            _managerMap = new LinkedHashMap<String, ComponentInfo<Manager>>();
            _adapterMap = new HashMap<String, Adapters<? extends Adapter>>();
            if (handler.library != null) {
                Class<?> clazz = Class.forName(handler.library);
                ComponentLibrary library = (ComponentLibrary)clazz.newInstance();
                _managerMap.putAll(library.getManagers());
                _daoMap.putAll(library.getDaos());
                createAdaptersMap(library.getAdapters());
            }

            _managerMap.putAll(handler.managers);
            _daoMap.putAll(handler.daos);

            startDaos();    // daos should not be using managers and adapters.
            createAdaptersMap(handler.adapters);
            instantiateManagers();
            instantiateAdapters(handler.adapters);
            configureManagers();
            startManagers();
            startAdapters();

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
    }

    /**
     * Daos should not refer to any other components so it is safe to start them
     * here.
     */
    protected void startDaos()  {
        Set<Map.Entry<String, ComponentInfo<GenericDao<?, ? extends Serializable>>>> entries = _daoMap.entrySet();

        for (Map.Entry<String, ComponentInfo<GenericDao<?, ?>>> entry : entries) {
            ComponentInfo<GenericDao<?, ?>> info = entry.getValue();
            s_logger.info("Starting DAO: " + info.name);
            try {
                info.instance = (GenericDao<?, ?>)createInstance(info.clazz, true, info.singleton);
                inject(info.clazz, info.instance);
                if (!info.instance.configure(info.name, info.params)) {
                    s_logger.error("Unable to configure DAO: " + info.name);
                    System.exit(1);
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
        Object entity = null;
        synchronized(s_factories) {
            if (singleton) {
                entity = s_singletons.get(clazz.toString());
                if (entity != null) {
                    s_logger.debug("Found singleton instantiation for " + clazz.toString());
                    return entity;
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
        
            entity = factory.newInstance(argTypes, args, s_callbacks);
        } else {
            entity = factory.newInstance(s_callbacks);
        }
        
        if (inject) {
            inject(clazz, entity);
        }
        
        if (singleton) {
            synchronized(s_factories) {
                s_singletons.put(clazz.toString(), entity);
            }
        }
        
        return entity;
    }
    

    protected ComponentInfo<GenericDao<?, ?>> getDao(String name) {
        ComponentInfo<GenericDao<?, ?>> info = _daoMap.get(name);
        if (info != null) {
            return info;
        }

        if (_parentLocator != null) {
            info = _parentLocator.getDao(name);
        }

        if (info == null) {
            return null;
        }

        _daoMap.put(name, info);
        return info;
    }

    private static synchronized Object getComponent(Class<?> clazz) {
        String name = clazz.getName();
        try {
            Object component = _componentMap.get(name);
            if (component == null) {
                Class<?> impl = Class.forName(name);
                component = createInstance(impl, true, true);
                _componentMap.put(name, component);
            }
            return component;
        } catch (ClassNotFoundException e) {
            s_logger.error("Unable to load " + name + " due to ", e);
            System.exit(1);
        }
        return null;
    }

    public static synchronized Object getComponent(String componentName) {
        ComponentLocator locator = s_locators.get(componentName);
        if (locator == null) {
            ComponentLocator.getLocator(componentName);
        }
        String implementationClass = _implementationClassMap.get(componentName);
        if (implementationClass != null) {
            try {
                Class<?> clazz = Class.forName(implementationClass);
                return getComponent(clazz);
            } catch (Exception ex) {
                s_logger.error("Failed to get component " + componentName + ", caused by exception " + ex, ex);
            }
        } else {
            s_logger.warn("Unable to find component with name: " + componentName);
        }
        return null;
    }

    public <T extends GenericDao<?, ?>> T getDao(Class<T> clazz) {
        ComponentInfo<GenericDao<?, ?>> info = getDao(clazz.getName());
        return info != null ? (T)info.instance : null;
    }

    protected void instantiateManagers() {
        Set<Map.Entry<String, ComponentInfo<Manager>>> entries = _managerMap.entrySet();
        for (Map.Entry<String, ComponentInfo<Manager>> entry : entries) {
            ComponentInfo<Manager> info = entry.getValue();
            if (info.instance == null) {
                s_logger.info("Instantiating Manager: " + info.name);
                info.instance = (Manager)createInstance(info.clazz, false, info.singleton);
            }
        }
    }

    protected void configureManagers() {
        Set<Map.Entry<String, ComponentInfo<Manager>>> entries = _managerMap.entrySet();
        for (Map.Entry<String, ComponentInfo<Manager>> entry : entries) {
            ComponentInfo<Manager> info = entry.getValue();
            s_logger.info("Injecting Manager: " + info.name);
            inject(info.clazz, info.instance);
        }
        for (Map.Entry<String, ComponentInfo<Manager>> entry : entries) {
            ComponentInfo<Manager> info = entry.getValue();
            s_logger.info("Configuring Manager: " + info.name);
            try {
                info.instance.configure(info.name, info.params);
            } catch (ConfigurationException e) {
                s_logger.error("Unable to configure manager: " + info.name, e);
                System.exit(1);
            }
        }
    }
    
    protected static void inject(Class<?> clazz, Object entity) {
        ComponentLocator locator = ComponentLocator.getCurrentLocator();
        
        do {
            Field[] fields = clazz.getDeclaredFields();
            for (Field field : fields) {
                Inject inject = field.getAnnotation(Inject.class);
                if (inject == null) {
                    continue;
                }
                Class<?> fc = field.getType();
                Object instance = null;
                if (Manager.class.isAssignableFrom(fc)) {
                    instance = locator.getManager(fc);
                } else if (GenericDao.class.isAssignableFrom(fc)) {
                    instance = locator.getDao((Class<? extends GenericDao<?, ?>>)fc);
                } else if (Adapters.class.isAssignableFrom(fc)) {
                    instance = locator.getAdapters(inject.adapter());
                } else {
                    instance = locator.getManager(fc);
                }
        
                if (instance == null) {
                    throw new CloudRuntimeException("Unable to inject " + fc.getSimpleName() + " in " + clazz.getSimpleName());
                }
                
                try {
                    field.setAccessible(true);
                    field.set(entity, instance);
                } catch (IllegalArgumentException e) {
                    throw new CloudRuntimeException("hmmm....is it really illegal?", e);
                } catch (IllegalAccessException e) {
                    throw new CloudRuntimeException("what! what ! what!", e);
                }
            }
            clazz = clazz.getSuperclass();
        } while (clazz != Object.class && clazz != null);
    }

    protected void startManagers() {
        Set<Map.Entry<String, ComponentInfo<Manager>>> entries = _managerMap.entrySet();
        for (Map.Entry<String, ComponentInfo<Manager>> entry : entries) {
            ComponentInfo<Manager> info = entry.getValue();
            s_logger.info("Starting Manager: " + info.name);
            if (!info.instance.start()) {
                throw new CloudRuntimeException("Incorrect Configuration: " + info.name);
            }
            if (info.instance instanceof ManagementBean) {
                registerMBean((ManagementBean) info.instance);
            }
            s_logger.info("Started Manager: " + info.name);
        }
    }

    protected void registerMBean(ManagementBean mbean) {
        try {
            JmxUtil.registerMBean(mbean);
        } catch (MalformedObjectNameException e) {
            s_logger.warn("Unable to register MBean: " + mbean.getName(), e);
        } catch (InstanceAlreadyExistsException e) {
            s_logger.warn("Unable to register MBean: " + mbean.getName(), e);
        } catch (MBeanRegistrationException e) {
            s_logger.warn("Unable to register MBean: " + mbean.getName(), e);
        } catch (NotCompliantMBeanException e) {
            s_logger.warn("Unable to register MBean: " + mbean.getName(), e);
        }
        s_logger.info("Registered MBean: " + mbean.getName());
    }

    protected ComponentInfo<Manager> getManager(String name) {
        ComponentInfo<Manager> mgr = _managerMap.get(name);
        if (mgr != null) {
            return mgr;
        }

        if (_parentLocator != null) {
            mgr = _parentLocator.getManager(name);
        }

        if (mgr == null) {
            return null;
        }

        _managerMap.put(name, mgr);
        return mgr;
    }

    public <T> T getManager(Class<T> clazz) {
        ComponentInfo<Manager> info = getManager(clazz.getName());
        if (info == null) {
            return null;
        }
        if (info.instance == null) {
            info.instance = (Manager)createInstance(info.clazz, false, info.singleton);
        }
        return (T)info.instance;
    }

    protected void instantiateAdapters(Map<String, List<ComponentInfo<Adapter>>> map) {
        Set<Map.Entry<String, List<ComponentInfo<Adapter>>>> entries = map.entrySet();
        for (Map.Entry<String, List<ComponentInfo<Adapter>>> entry : entries) {
            Adapters<Adapter> adapters = (Adapters<Adapter>)_adapterMap.get(entry.getKey());
            List<Adapter> lst = new ArrayList<Adapter>();
            for (ComponentInfo<Adapter> info : entry.getValue()) {
                s_logger.info("Instantiating Adapter: " + info.name);
                info.instance = (Adapter)createInstance(info.clazz, true, info.singleton);
                try {
                    if (!info.instance.configure(info.name, info.params)) {
                        s_logger.error("Unable to configure adapter: " + info.name);
                        System.exit(1);
                    }
                } catch (ConfigurationException e) {
                    s_logger.error("Unable to configure adapter: " + info.name, e);
                    System.exit(1);
                } catch (Exception e) {
                    s_logger.error("Unable to configure adapter: " + info.name, e);
                    System.exit(1);
                }
                lst.add(info.instance);
                s_logger.info("Instantiated Adapter: " + info.name);
            }
            adapters.set(lst);
        }
    }

    protected void createAdaptersMap(Map<String, List<ComponentInfo<Adapter>>> map) {
        Set<Map.Entry<String, List<ComponentInfo<Adapter>>>> entries = map.entrySet();
        for (Map.Entry<String, List<ComponentInfo<Adapter>>> entry : entries) {
            List<? extends Adapter> lst = new ArrayList<Adapter>(entry.getValue().size());
            _adapterMap.put(entry.getKey(), new Adapters(entry.getKey(), lst));
        }
    }

    protected void startAdapters() {
        for (Map.Entry<String, Adapters<? extends Adapter>> entry : _adapterMap.entrySet()) {
            for (Adapter adapter : entry.getValue().get()) {
                s_logger.info("Starting Adapter: " + adapter.getName());
                if (!adapter.start()) {
                    throw new CloudRuntimeException("Unable to start adapter: " + adapter.getName());
                }
                if (adapter instanceof ManagementBean) {
                    registerMBean((ManagementBean)adapter);
                }
                s_logger.info("Started Adapter: " + adapter.getName());
            }
        }
    }

    @Override
    public String getParentName() {
        return _parentLocator != null ? _parentLocator.getName() : "None";
    }
    
    public static <T> T inject(Class<T> clazz) {
        return (T)createInstance(clazz, true, false);
    }
    
    public static <T> T inject(Class<T> clazz, Object... args) {
        return (T)createInstance(clazz, true, false, args);
    }
    
    @Override
    public Map<String, List<String>> getAdapters() {
        HashMap<String, List<String>> result = new HashMap<String, List<String>>();
        for (Map.Entry<String, Adapters<? extends Adapter>> entry : _adapterMap.entrySet()) {
            Adapters<? extends Adapter> adapters = entry.getValue();
            Enumeration<? extends Adapter> en = adapters.enumeration();
            List<String> lst = new ArrayList<String>();
            while (en.hasMoreElements()) {
                Adapter adapter = en.nextElement();
                lst.add(adapter.getName() + "-" + adapter.getClass().getName());
            }
            result.put(entry.getKey(), lst);
        }
        return result;
    }

    public Map<String, List<String>> getAllAccessibleAdapters() {
        Map<String, List<String>> parentResults = _parentLocator != null ? _parentLocator.getAllAccessibleAdapters()
                : new HashMap<String, List<String>>();
        Map<String, List<String>> results = getAdapters();
        parentResults.putAll(results);
        return parentResults;
    }

    @Override
    public Collection<String> getManagers() {
        Collection<String> names = _parentLocator != null ? _parentLocator.getManagers() : new HashSet<String>();
        for (Map.Entry<String, ComponentInfo<Manager>> entry : _managerMap.entrySet()) {
            names.add(entry.getValue().name);
        }
        return names;
    }

    @Override
    public Collection<String> getDaos() {
        Collection<String> names = _parentLocator != null ? _parentLocator.getDaos() : new HashSet<String>();
        for (Map.Entry<String, ComponentInfo<GenericDao<?, ?>>> entry : _daoMap.entrySet()) {
            names.add(entry.getValue().name);
        }
        return names;
    }

    public <T extends Adapter> Adapters<T> getAdapters(Class<T> clazz) {
        return (Adapters<T>)getAdapters(clazz.getName());
    }

    public Adapters<? extends Adapter> getAdapters(String key) {
        Adapters<? extends Adapter> adapters = _adapterMap.get(key);
        if (adapters != null) {
            return adapters;
        }
        if (_parentLocator != null) {
            adapters = _parentLocator.getAdapters(key);
            if (adapters != null) {
                return adapters;
            }
        }
        return new Adapters(key, new ArrayList<Adapter>());
    }

    static HashMap<String, ComponentLocator> s_locators = new HashMap<String, ComponentLocator>();
    protected static ComponentLocator getLocatorInternal(String server, boolean setInThreadLocal, String configFileName, String log4jFile) {
        // init log4j based on the passed in configuration
        if (s_doOnce == false) {
            File file = PropertiesUtil.findConfigFile(log4jFile + ".xml");
            if (file != null) {
                DOMConfigurator.configureAndWatch(file.getAbsolutePath());
            } else {
                file = PropertiesUtil.findConfigFile(log4jFile + ".properties");
                if (file != null) {
                    PropertyConfigurator.configureAndWatch(file.getAbsolutePath());
                }
            }
            s_doOnce = true;
        }

        ComponentLocator locator;
        synchronized (s_locators) {
            locator = s_locators.get(server);
            if (locator == null) {
                locator = new ComponentLocator(server);
                s_locators.put(server, locator);
                if (setInThreadLocal) {
                    s_tl.set(locator);
                }
                locator.parse(configFileName, log4jFile);
            } else {
                if (setInThreadLocal) {
                    s_tl.set(locator);
                }
            }
        }

        return locator;
    }

    protected static final ThreadLocal<ComponentLocator> s_tl = new ThreadLocal<ComponentLocator>();

    public static ComponentLocator getLocator(String server, String configFileName, String log4jFile) {
        return getLocatorInternal(server, true, configFileName, log4jFile);
    }

    public static ComponentLocator getLocator(String server) {
    	String configfile = "components-premium.xml";
    	if (PropertiesUtil.findConfigFile(configfile) == null){
    		configfile = "components.xml";
    	}
        return getLocatorInternal(server, true, configfile, "log4j-cloud");
    }

    public static ComponentLocator getCurrentLocator() {
        return s_tl.get();
    }

    public static class ComponentInfo<T> {
        Class<?>                clazz;
        HashMap<String, Object> params = new HashMap<String, Object>();
        String                  name;
        List<String>            keys = new ArrayList<String>();
        T                       instance;
        boolean                 singleton = true;
        
        protected ComponentInfo() {
        }
        
        public List<String> getKeys() {
            return keys;
        }
        
        public String getName() {
            return name;
        }
        
        public ComponentInfo(String name, Class<? extends T> clazz) {
            this(name, clazz, new ArrayList<Pair<String, Object>>(0));
        }
        
        public ComponentInfo(String name, Class<? extends T> clazz, List<Pair<String, Object>> params) {
            this(name, clazz, params, true);
        }
        
        public ComponentInfo(String name, Class<? extends T> clazz, List<Pair<String, Object>> params, boolean singleton) {
            this.name = name;
            this.clazz = clazz;
            this.singleton = singleton;
            for (Pair<String, Object> param : params) {
                this.params.put(param.first(), param.second());
            }
            fillInfo();
        }
        
        protected void fillInfo() {
            String clazzName = clazz.getName();
            
            Local local = clazz.getAnnotation(Local.class);
            if (local == null) {
                throw new CloudRuntimeException("Unable to find Local annotation for class " + clazzName);
            }

            // Verify that all interfaces specified in the Local annotation is implemented by the class.
            Class<?>[] classes = local.value();
            for (int i = 0; i < classes.length; i++) {
                if (!classes[i].isInterface()) {
                    throw new CloudRuntimeException(classes[i].getName() + " is not an interface");
                }
                if (classes[i].isAssignableFrom(clazz)) {
                    keys.add(classes[i].getName());
                    s_logger.info("Found component: " + classes[i].getName() + " in " + clazzName + " - " + name);
                } else {
                    throw new CloudRuntimeException(classes[i].getName() + " is not implemented by " + clazzName);
                }
            }
        }
    }

    /**
     * XmlHandler is used by AdapterManager to handle the SAX parser callbacks.
     * It builds a hash map of lists of adapters and a hash map of managers.
     **/
    protected class XmlHandler extends DefaultHandler {
        public HashMap<String, List<ComponentInfo<Adapter>>>    adapters;
        public HashMap<String, ComponentInfo<Manager>>          managers;
        public LinkedHashMap<String, ComponentInfo<GenericDao<?, ?>>> daos;
        public String                                  parent;
        public String                                  library;

        List<ComponentInfo<Adapter>>                            lst;
        String                                         paramName;
        StringBuilder                                  value;
        String                                         serverName;
        boolean                                        parse;
        ComponentInfo<?>                                        currentInfo;

        public XmlHandler(String serverName) {
            this.serverName = serverName;
            parse = false;
            adapters = new HashMap<String, List<ComponentInfo<Adapter>>>();
            managers = new HashMap<String, ComponentInfo<Manager>>();
            daos = new LinkedHashMap<String, ComponentInfo<GenericDao<?, ?>>>();
            value = null;
            parent = null;
        }

        protected void fillInfo(Attributes atts, Class<?> interphace, ComponentInfo<?> info) {
            String clazzName = getAttribute(atts, "class");
            if (clazzName == null) {
                throw new CloudRuntimeException("Missing class attribute for " + interphace.getName());
            }
            info.name = getAttribute(atts, "name");
            if (info.name == null) {
                throw new CloudRuntimeException("Missing name attribute for " + interphace.getName());
            }
            info.name = info.name + "-" + clazzName;
            s_logger.debug("Looking for class " + clazzName);
            try {
                info.clazz = Class.forName(clazzName);
            } catch (ClassNotFoundException e) {
                throw new CloudRuntimeException("Unable to find class: " + clazzName);
            } catch (Throwable e) {
                throw new CloudRuntimeException("Caught throwable: ", e);
            }

            if (!interphace.isAssignableFrom(info.clazz)) {
                throw new CloudRuntimeException("Class " + info.clazz.toString() + " does not implment " + interphace);
            }
            
            info.fillInfo();
        }
        
        @Override
        public void startElement(String namespaceURI, String localName, String qName, Attributes atts)
        throws SAXException {
            if (!parse) {
                if (qName.equals(_serverName)) {
                    parse = true;
                    parent = getAttribute(atts, "extends");
                    String implementationClass = getAttribute(atts, "class");

                    if (implementationClass != null) {
                        _implementationClassMap.put(_serverName, implementationClass);
                    }
                    
                    library = getAttribute(atts, "library");
                }
            } else if (qName.equals("adapters")) {
                lst = new ArrayList<ComponentInfo<Adapter>>();
                String key = getAttribute(atts, "key");
                if (key == null) {
                    throw new CloudRuntimeException("Missing key attribute for adapters");
                }
                adapters.put(key, lst);
            } else if (qName.equals("adapter")) {
                ComponentInfo<Adapter> info = new ComponentInfo<Adapter>();
                fillInfo(atts, Adapter.class, info);
                lst.add(info);
                currentInfo = info;
            } else if (qName.equals("manager")) {
                ComponentInfo<Manager> info = new ComponentInfo<Manager>();
                fillInfo(atts, Manager.class, info);
                s_logger.info("Adding Manager: " + info.name);
                for (String key : info.keys) {
                    s_logger.info("Linking " + key + " to " + info.name);
                    managers.put(key, info);
                }
                currentInfo = info;
            } else if (qName.equals("param")) {
                paramName = getAttribute(atts, "name");
                value = new StringBuilder();
            } else if (qName.equals("dao")) {
                ComponentInfo<GenericDao<?, ?>> info = new ComponentInfo<GenericDao<?, ?>>();
                fillInfo(atts, GenericDao.class, info);
                for (String key : info.keys) {
                    daos.put(key, info);
                }
                currentInfo = info;
            } else {
                // ignore
            }
        }

        protected String getAttribute(Attributes atts, String name) {
            for (int att = 0; att < atts.getLength(); att++) {
                String attName = atts.getQName(att);
                if (attName.equals(name)) {
                    return atts.getValue(att);
                }
            }
            return null;
        }

        @Override
        public void endElement(String namespaceURI, String localName, String qName) throws SAXException {
            if (!parse) {
                return;
            }

            if (qName.equals(_serverName)) {
                parse = false;
            } else if (qName.equals("adapters")) {
            } else if (qName.equals("adapter")) {
            } else if (qName.equals("manager")) {
            } else if (qName.equals("dao")) {
            } else if (qName.equals("param")) {
                currentInfo.params.put(paramName, value.toString());
                paramName = null;
                value = null;
            } else {
                // ignore
            }
        }

        @Override
        public void characters(char[] ch, int start, int length) throws SAXException {
            if (parse && value != null) {
                value.append(ch, start, length);
            }
        }
    }
    
    protected static class InjectInfo {
        public Factory factory;
        public Enhancer enhancer;
        
        public InjectInfo(Enhancer enhancer, Factory factory) {
            this.factory = factory;
            this.enhancer = enhancer;
        }
    }
}
