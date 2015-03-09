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

import java.util.Map;

import javax.naming.ConfigurationException;

public interface ComponentLifecycle extends Named {
    public static final int RUN_LEVEL_SYSTEM_BOOTSTRAP = 0;        // for system level bootstrap components
    public static final int RUN_LEVEL_SYSTEM = 1;                // for system level service components (i.e., DAOs)
    public static final int RUN_LEVEL_FRAMEWORK_BOOTSTRAP = 2;    // for framework startup checkers (i.e., DB migration check)
    public static final int RUN_LEVEL_FRAMEWORK = 3;            // for framework bootstrap components(i.e., clustering management components)
    public static final int RUN_LEVEL_COMPONENT_BOOTSTRAP = 4;    // general manager components
    public static final int RUN_LEVEL_COMPONENT = 5;            // regular adapters, plugin components
    public static final int RUN_LEVEL_APPLICATION_MAINLOOP = 6;
    public static final int MAX_RUN_LEVELS = 7;

    @Override
    String getName();

    void setName(String name);

    void setConfigParams(Map<String, Object> params);

    Map<String, Object> getConfigParams();

    int getRunLevel();

    void setRunLevel(int level);

    public boolean configure(String name, Map<String, Object> params) throws ConfigurationException;

    /**
     * Start any background tasks.
     *
     * @return true if the tasks were started, false otherwise.
     */
    public boolean start();

    /**
     * Stop any background tasks.
     *
     * @return true background tasks were stopped, false otherwise.
     */
    public boolean stop();
}
