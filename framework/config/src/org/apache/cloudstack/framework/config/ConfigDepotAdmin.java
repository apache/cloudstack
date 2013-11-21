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

import java.util.List;

/**
 * Administrative interface to ConfigDepot
 *
 */
public interface ConfigDepotAdmin {
    /**
     * Create configurations if there are new config parameters.
     * Update configurations if the parameter settings have been changed.
     * All configurations that have been updated/created will have the same timestamp in the updated field.
     * All previous configurations that should be obsolete will have a null updated field.
     * @see Configuration
     */
    void populateConfigurations();

    void populateConfiguration(Configurable configurable);

    List<String> getComponentsInDepot();
}
