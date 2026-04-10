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
package org.apache.cloudstack.gui.theme.json.config.validator.attributes;

import java.util.List;

/**
 * Specific validator for the "error" object within the GUI theme JSON configuration.
 *
 * <p>
 * This component is defined as a bean in the Spring XML configuration and is automatically injected into
 * the {@link org.apache.cloudstack.gui.theme.json.config.validator.JsonConfigValidator} attribute list.
 * </p>
 */
public class ErrorAttribute extends AttributeBase {

    @Override
    protected String getAttributeName() {
        return "error";
    }

    @Override
    protected List<String> getAllowedProperties() {
        return List.of("403", "404", "500");
    }
}
