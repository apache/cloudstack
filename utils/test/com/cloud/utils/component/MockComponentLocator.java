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

import java.io.Serializable;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

import net.sf.cglib.proxy.Callback;
import net.sf.cglib.proxy.NoOp;

import com.cloud.utils.Pair;
import com.cloud.utils.Ternary;
import com.cloud.utils.component.ComponentLocator.ComponentInfo;
import com.cloud.utils.db.DatabaseCallback;
import com.cloud.utils.db.DatabaseCallbackFilter;
import com.cloud.utils.db.GenericDao;

/**
 * MockComponentLocator allows you to define exactly the components you need
 * to test your component.  This gives you a lot of flexibility in terms of
 * defining mock components.
 */
public class MockComponentLocator extends ComponentLocator {
    MockComponentLibrary _library = new MockComponentLibrary();
    
    public MockComponentLocator(String server) {
        super(server);
    }
    
    public ComponentInfo<? extends GenericDao<?, ? extends Serializable>> addDao(String name, Class<? extends GenericDao<?, ? extends Serializable>> dao) {
        return _library.addDao(name, dao);
    }
    
    public ComponentInfo<Manager> addManager(String name, Class<? extends Manager> manager) {
        return _library.addManager(name, manager);
    }
    
    public <T> ComponentInfo<Adapter> addOneAdapter(Class<T> interphace, String name, Class<? extends T> adapterClass) {
        return _library.addOneAdapter(interphace, name, adapterClass);
    }
    
    public <T> List<ComponentInfo<Adapter>> addAdapterChain(Class<T> interphace, List<Pair<String, Class<? extends T>>> adapters) {
        return _library.addAdapterChain(interphace, adapters);
    }
    
    @Override
    protected Pair<XmlHandler, HashMap<String, List<ComponentInfo<Adapter>>>> parse2(String filename) {
        Pair<XmlHandler, HashMap<String, List<ComponentInfo<Adapter>>>> result = new Pair<XmlHandler, HashMap<String, List<ComponentInfo<Adapter>>>>(new XmlHandler("fake"), new HashMap<String, List<ComponentInfo<Adapter>>>());
        _daoMap = new LinkedHashMap<String, ComponentInfo<GenericDao<?, ? extends Serializable>>>();
        _managerMap = new LinkedHashMap<String, ComponentInfo<Manager>>();
        _checkerMap = new HashMap<String, ComponentInfo<SystemIntegrityChecker>>();
        _adapterMap = new HashMap<String, Adapters<? extends Adapter>>();
        _factories = new HashMap<Class<?>, Class<?>>();
        _daoMap.putAll(_library.getDaos());
        _managerMap.putAll(_library.getManagers());
        result.second().putAll(_library.getAdapters());
        _factories.putAll(_library.getFactories());
        return result;
    } 
    
    public void makeActive(InterceptorLibrary interceptors) {
        s_singletons.clear();
        s_locators.clear();
        s_factories.clear();
        s_callbacks = new Callback[] { NoOp.INSTANCE, new DatabaseCallback()};
        s_callbackFilter = new DatabaseCallbackFilter();
        s_interceptors.clear();
        if (interceptors != null) {
        	resetInterceptors(interceptors);
        }
        s_tl.set(this);
        parse("fake file");
    }
    
    protected class MockComponentLibrary extends ComponentLibraryBase implements ComponentLibrary { 
                
        @Override
        public Map<String, List<ComponentInfo<Adapter>>> getAdapters() {
            return _adapters;
        }
    
        @Override
        public Map<Class<?>, Class<?>> getFactories() {
            return new HashMap<Class<?>, Class<?>>();
        }
        
        @Override
        public Map<String, ComponentInfo<GenericDao<?, ?>>> getDaos() {
            return _daos;
        }

        @Override
        public Map<String, ComponentInfo<Manager>> getManagers() {
            return _managers;
        }
    }
}
