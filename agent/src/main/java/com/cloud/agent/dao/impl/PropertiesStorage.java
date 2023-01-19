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
package com.cloud.agent.dao.impl;

import java.io.File;
import java.io.FileNotFoundException;
import java.io.FileOutputStream;
import java.io.IOException;
import java.util.Map;
import java.util.Properties;

import org.apache.commons.io.IOUtils;
import org.apache.logging.log4j.Logger;
import org.apache.logging.log4j.LogManager;

import com.cloud.agent.dao.StorageComponent;
import com.cloud.utils.PropertiesUtil;

/**
 * Uses Properties to implement storage.
 *
 * @config {@table || Param Name | Description | Values | Default || || path |
 *         path to the properties _file | String | db/db.properties || * }
 **/
public class PropertiesStorage implements StorageComponent {
    protected Logger logger = LogManager.getLogger(getClass());
    Properties _properties = new Properties();
    File _file;
    String _name;

    @Override
    public synchronized String get(String key) {
        return _properties.getProperty(key);
    }

    @Override
    public synchronized void persist(String key, String value) {
        if (!loadFromFile(_file)) {
            logger.error("Failed to load changes and then write to them");
        }
        _properties.setProperty(key, value);
        FileOutputStream output = null;
        try {
            output = new FileOutputStream(_file);
            _properties.store(output, _name);
            output.flush();
            output.close();
        } catch (IOException e) {
            logger.error("Uh-oh: ", e);
        } finally {
            IOUtils.closeQuietly(output);
        }
    }

    private synchronized boolean loadFromFile(final File file) {
        try {
            PropertiesUtil.loadFromFile(_properties, file);
            _file = file;
        } catch (FileNotFoundException e) {
            logger.error("How did we get here? ", e);
            return false;
        } catch (IOException e) {
            logger.error("IOException: ", e);
            return false;
        }
        return true;
    }

    @Override
    public synchronized boolean configure(String name, Map<String, Object> params) {
        _name = name;
        String path = (String)params.get("path");
        if (path == null) {
            path = "agent.properties";
        }

        File file = PropertiesUtil.findConfigFile(path);
        if (file == null) {
            file = new File(path);
            try {
                if (!file.createNewFile()) {
                    logger.error("Unable to create _file: " + file.getAbsolutePath());
                    return false;
                }
            } catch (IOException e) {
                logger.error("Unable to create _file: " + file.getAbsolutePath(), e);
                return false;
            }
        }
        return loadFromFile(file);
    }

    @Override
    public synchronized String getName() {
        return _name;
    }

    @Override
    public boolean start() {
        return true;
    }

    @Override
    public boolean stop() {
        return true;
    }

    @Override
    public void setName(String name) {
        // TODO Auto-generated method stub

    }

    @Override
    public void setConfigParams(Map<String, Object> params) {
        // TODO Auto-generated method stub

    }

    @Override
    public Map<String, Object> getConfigParams() {
        // TODO Auto-generated method stub
        return null;
    }

    @Override
    public int getRunLevel() {
        // TODO Auto-generated method stub
        return 0;
    }

    @Override
    public void setRunLevel(int level) {
        // TODO Auto-generated method stub
    }

}
