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
package com.cloud.utils.db;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import org.apache.log4j.Logger;

import com.cloud.utils.exception.CloudRuntimeException;

public class DriverLoader {

    private static final Logger LOGGER = Logger.getLogger(DriverLoader.class.getName());
    private static final List<String> LOADED_DRIVERS;
    private static final Map<String, String> DRIVERS;

    static {
        DRIVERS = new HashMap<String, String>();
        DRIVERS.put("jdbc:mysql", "com.mysql.cj.jdbc.Driver");
        DRIVERS.put("jdbc:postgresql", "org.postgresql.Driver");
        DRIVERS.put("jdbc:h2", "org.h2.Driver");

        LOADED_DRIVERS = new ArrayList<String>();
    }


    public static void loadDriver(String dbDriver) {
        String driverClass = DRIVERS.get(dbDriver);
        if (driverClass == null) {
            LOGGER.error("DB driver type " + dbDriver + " is not supported!");
            throw new CloudRuntimeException("DB driver type " + dbDriver + " is not supported!");
        }

        if (LOADED_DRIVERS.contains(dbDriver)) {
            if (LOGGER.isDebugEnabled()) {
                LOGGER.debug("DB driver " + driverClass + " was already loaded.");
            }
            return;
        }

        try {
            Class.forName(driverClass).newInstance();
            LOADED_DRIVERS.add(dbDriver);
            if (LOGGER.isDebugEnabled()) {
                LOGGER.debug("Successfully loaded DB driver " + driverClass);
            }
        } catch (ClassNotFoundException | InstantiationException | IllegalAccessException e) {
            LOGGER.error("Failed to load DB driver " + driverClass);
            throw new CloudRuntimeException("Failed to load DB driver " + driverClass, e);
        }
    }

}
