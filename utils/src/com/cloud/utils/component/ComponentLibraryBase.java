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
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

import com.cloud.utils.Pair;
import com.cloud.utils.component.ComponentLocator.ComponentInfo;
import com.cloud.utils.db.GenericDao;

public abstract class ComponentLibraryBase implements ComponentLibrary {
    
    protected final Map<String, ComponentInfo<GenericDao<?, ? extends Serializable>>> _daos = new LinkedHashMap<String, ComponentInfo<GenericDao<?, ? extends Serializable>>>();

    protected ComponentInfo<? extends GenericDao<?, ? extends Serializable>> addDao(String name, Class<? extends GenericDao<?, ? extends Serializable>> clazz) {
        return addDao(name, clazz, new ArrayList<Pair<String, Object>>(), true);
    }

    protected ComponentInfo<? extends GenericDao<?, ? extends Serializable>> addDao(String name, Class<? extends GenericDao<?, ? extends Serializable>> clazz, List<Pair<String, Object>> params, boolean singleton) {
        ComponentInfo<GenericDao<?, ? extends Serializable>> componentInfo = new ComponentInfo<GenericDao<?, ? extends Serializable>>(name, clazz, params, singleton);
        for (String key : componentInfo.getKeys()) {
            _daos.put(key, componentInfo);
        }
        return componentInfo;
    }

    protected Map<String, ComponentInfo<Manager>> _managers = new LinkedHashMap<String, ComponentInfo<Manager>>();
    protected Map<String, List<ComponentInfo<Adapter>>> _adapters = new LinkedHashMap<String, List<ComponentInfo<Adapter>>>();
    protected Map<String, ComponentInfo<PluggableService>> _pluggableServices = new LinkedHashMap<String, ComponentInfo<PluggableService>>();

    protected ComponentInfo<Manager> addManager(String name, Class<? extends Manager> clazz, List<Pair<String, Object>> params, boolean singleton) {
        ComponentInfo<Manager> info = new ComponentInfo<Manager>(name, clazz, params, singleton);
        for (String key : info.getKeys()) {
            _managers.put(key, info);
        }
        return info;
    }
    
    protected ComponentInfo<Manager> addManager(String name, Class<? extends Manager> clazz) {
        return addManager(name, clazz, new ArrayList<Pair<String, Object>>(), true);
    }
    
    protected <T> List<ComponentInfo<Adapter>> addAdapterChain(Class<T> interphace, List<Pair<String, Class<? extends T>>> adapters) {
        ArrayList<ComponentInfo<Adapter>> lst = new ArrayList<ComponentInfo<Adapter>>(adapters.size());
        for (Pair<String, Class<? extends T>> adapter : adapters) {
            @SuppressWarnings("unchecked")
            Class<? extends Adapter> clazz = (Class<? extends Adapter>)adapter.second();
            lst.add(new ComponentInfo<Adapter>(adapter.first(), clazz));
        }
        _adapters.put(interphace.getName(), lst);
        return lst;
    }
    
    protected <T> void addAdapter(Class<T> interphace, String name, Class<? extends T> adapterClass) {
    	List<ComponentInfo<Adapter>> lst = _adapters.get(interphace.getName());
    	if (lst == null) {
    		addOneAdapter(interphace, name, adapterClass);
    	} else {
    		@SuppressWarnings("unchecked")
    		Class<? extends Adapter> clazz = (Class<? extends Adapter>)adapterClass;
    		lst.add(new ComponentInfo<Adapter>(name, clazz));
    	}
    }
    
    protected <T> ComponentInfo<Adapter> addOneAdapter(Class<T> interphace, String name, Class<? extends T> adapterClass) {
        List<Pair<String, Class<? extends T>>> adapters = new ArrayList<Pair<String, Class<? extends T>>>();
        adapters.add(new Pair<String, Class<? extends T>>(name, adapterClass));
        return addAdapterChain(interphace, adapters).get(0);
    }
    

    protected <T> ComponentInfo<PluggableService> addService(String name, Class<T> serviceInterphace, Class<? extends PluggableService> clazz, List<Pair<String, Object>> params, boolean singleton) {
        ComponentInfo<PluggableService> info = new ComponentInfo<PluggableService>(name, clazz, params, singleton);
        _pluggableServices.put(serviceInterphace.getName(), info);
        return info;
    }
    
    protected <T> ComponentInfo<PluggableService> addService(String name, Class<T> serviceInterphace, Class<? extends PluggableService> clazz) {
        return addService(name, serviceInterphace, clazz, new ArrayList<Pair<String, Object>>(), true);
    }
 }
