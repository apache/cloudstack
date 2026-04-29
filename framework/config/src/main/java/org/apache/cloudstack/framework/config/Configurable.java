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
package org.apache.cloudstack.framework.config;

/**
 * Configurable can be implemented by components to insert their own
 * configuration keys.
 *
 * CloudStack will gather all of these configurations at startup and insert
 * them into the configuration table.
 *
 */
public interface Configurable {

    /**
     * @return The name of the component that provided this configuration
     * variable.  This value is saved in the database so someone can easily
     * identify who provides this variable.
     **/
    String getConfigComponentName();

    /**
     * @return The list of config keys provided by this configuable.
     */
    ConfigKey<?>[] getConfigKeys();
}
