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

import java.util.List;
import java.util.Map;

import com.cloud.utils.component.ComponentLocator.ComponentInfo;
import com.cloud.utils.db.GenericDao;

/**
 * ComponentLibrary specifies the implementation classes that a server needs
 * to do its work.  You can specify the implementation class in the "library"
 * attribute of the server element within components.xml.  ComponentLocator
 * first loads the implementations specified here, then, it loads the 
 * implementations from components.xml.  If an interface is specified in both
 * the ComponentLibrary and the components.xml for the same server, the interface 
 * within the components.xml overrides the one within ComponentLibrary.
 *
 */
public interface ComponentLibrary {    
    /**
     * @return all of the daos
     */
    Map<String, ComponentInfo<GenericDao<?,?>>> getDaos();
    
    /**
     * @return all of the Managers
     */
    Map<String, ComponentInfo<Manager>> getManagers();
    
    /**
     * @return all of the adapters
     */
    Map<String, List<ComponentInfo<Adapter>>> getAdapters();
    
    Map<Class<?>, Class<?>> getFactories();
    
    /**
     * @return all the services
     * 
     */
    Map<String, ComponentInfo<PluggableService>> getPluggableServices();
}
