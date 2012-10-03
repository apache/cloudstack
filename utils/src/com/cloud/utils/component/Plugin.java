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

import java.util.List;

import com.cloud.utils.Pair;


/**
 * CloudStack uses Adapters to implement different capabilities.
 * There are different Adapters such as NetworkGuru, NetworkElement, 
 * HypervisorGuru, DeploymentPlanner, etc.  However, Adapters only
 * defines what CloudStack needs from the implementation.  What about
 * what the Adapter itself needs, such as configurations and administrative
 * operations, and what if one implementation can 
 * implement two different Adapters?
 *
 * Plugin is a CloudStack container for Adapters.  It rolls the following 
 * capabilities into the one package for CloudStack to load at runtime.
 *   - REST API commands supported by the Plugin.
 *   - Components needed by the Plugin.
 *   - Adapters implemented by the Plugin.
 *   - Database operations
 *
 */
public interface Plugin extends PluggableService {

    /**
     * Retrieves the component libraries needed by this Plugin.  
     * ComponentLocator put these components and add them to the startup 
     * and shutdown processes of CloudStack.  This is only needed if the 
     * Plugin uses ComponentLocator to inject what it needs.  If the
     * Plugin uses other mechanisms, then it can return null here.
     *  
     * @return a component library that contains the components this Plugin
     * contains and needs.
     */
    ComponentLibrary getComponentLibrary();

    /**
     * Retrieves the list of Adapters and the interface they implement.  It
     * can be an empty list if the Plugin does not implement any.
     * 
     * @return list of pairs where the first is the interface and the second 
     *         is the adapter.
     */
    List<Pair<Class<?>, Class<? extends Adapter>>> getAdapterImplementations();
}
