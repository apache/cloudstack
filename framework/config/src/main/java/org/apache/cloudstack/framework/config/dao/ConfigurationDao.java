// Licensed to the Apache Software Foundation (ASF) under one
// or more contributor license agreements.  See the NOTICE file
// distributed with this work for additional information
// regarding copyright ownership.  The ASF licenses this file
// to you under the Apache License, Version 2.0 (the
// "License"); you may not use this file except in compliance
// with the License.  You may obtain a copy of the License at
//
//   http://www.apache.org/licenses/LICENSE-2.0
//
// Unless required by applicable law or agreed to in writing,
// software distributed under the License is distributed on an
// "AS IS" BASIS, WITHOUT WARRANTIES OR CONDITIONS OF ANY
// KIND, either express or implied.  See the License for the
// specific language governing permissions and limitations
// under the License.
package org.apache.cloudstack.framework.config.dao;

import java.util.Map;

import org.apache.cloudstack.framework.config.impl.ConfigurationVO;

import com.cloud.utils.db.GenericDao;

public interface ConfigurationDao extends GenericDao<ConfigurationVO, String> {

    /**
     *
     *    1. params passed in.
     *    2. configuration for the instance.
     *    3. configuration for the DEFAULT instance.
     *
     * @param params parameters from the components.xml which will override the database values.
     * @return a consolidated look at the configuration parameters.
     */
    public Map<String, String> getConfiguration(String instance, Map<String, ? extends Object> params);

    public Map<String, String> getConfiguration(Map<String, ? extends Object> params);

    public Map<String, String> getConfiguration();

    /**
     * Updates a configuration value
     * @param value the new value
     * @return true if success, false if failure
     */
    public boolean update(String name, String value);

    /**
     * Gets the value for the specified configuration name
     * @return value
     */
    public String getValue(String name);

    public String getValueAndInitIfNotExist(String name, String category, String initValue);

    public String getValueAndInitIfNotExist(String name, String category, String initValue, String desc);

    /**
     * returns whether or not this is a premium configuration
     * @return true if premium configuration, false otherwise
     */
    boolean isPremium();

    ConfigurationVO findByName(String name);

    boolean update(String name, String category, String value);

    void invalidateCache();
}
