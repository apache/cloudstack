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
package org.apache.cloudstack.config;

import java.util.Date;

/**
 * Configuration represents one global configuration parameter for CloudStack.
 * Its scope should indicate whether this parameter can be set at different
 * organization levels in CloudStack.
 *
 */
public interface Configuration {

    /**
     * @return Category of the parameter.
     */
    String getCategory();

    /**
     * @return Server instance that uses this parameter.
     */
    String getInstance();

    /**
     * @return Component that introduced this parameter.
     */
    String getComponent();

    /**
     * @return Name of the parameter.
     */
    String getName();

    /**
     * @return Value set by the administrator.  Defaults to the defaultValue.
     */
    String getValue();

    /**
     * @return Description of the value and the range of the value.
     */
    String getDescription();

    /**
     * @return Default value for this parameter.  Null indicates this parameter is optional.
     */
    String getDefaultValue();

    /**
     * @return Scope for the parameter.  Null indicates that this parameter is
     * always global.  A non-null value indicates that this parameter can be
     * set at a certain organization level.
     */
    String getScope();

    /**
     * @return can the configuration parameter be changed without restarting the server.
     */
    boolean isDynamic();

    /**
     * @return The date this VO was updated by the components.  Note that this is not
     * a date for when an administrator updates the value.  This is when the system
     * updated this value.  By searching on this field gives you all the config
     * parameters that have changed in an upgrade.  Null value indicates that this
     * parameter is no longer used and can be deleted.
     */
    Date getUpdated();
}
