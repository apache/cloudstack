//
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
//

package com.cloud.utils.component;

import java.util.HashMap;
import java.util.Map;

import javax.naming.ConfigurationException;

import org.apache.log4j.Logger;

public class ComponentLifecycleBase implements ComponentLifecycle {
    private static final Logger s_logger = Logger.getLogger(ComponentLifecycleBase.class);

    protected String _name;
    protected int _runLevel;
    protected Map<String, Object> _configParams = new HashMap<String, Object>();

    public ComponentLifecycleBase() {
        _name = this.getClass().getSimpleName();
        _runLevel = RUN_LEVEL_COMPONENT;
    }

    @Override
    public String getName() {
        return _name;
    }

    @Override
    public void setName(String name) {
        _name = name;
    }

    @Override
    public void setConfigParams(Map<String, Object> params) {
        _configParams = params;
    }

    @Override
    public Map<String, Object> getConfigParams() {
        return _configParams;
    }

    @Override
    public int getRunLevel() {
        return _runLevel;
    }

    @Override
    public void setRunLevel(int level) {
        _runLevel = level;
    }

    @Override
    public boolean configure(String name, Map<String, Object> params) throws ConfigurationException {
        _name = name;
        _configParams = params;
        return true;
    }

    @Override
    public boolean start() {
        return true;
    }

    @Override
    public boolean stop() {
        return true;
    }
}
