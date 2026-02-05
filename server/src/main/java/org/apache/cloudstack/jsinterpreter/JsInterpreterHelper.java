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

package org.apache.cloudstack.jsinterpreter;

import com.cloud.exception.InvalidParameterValueException;
import org.apache.cloudstack.framework.config.ConfigKey;
import org.apache.cloudstack.framework.config.Configurable;
import org.apache.cloudstack.utils.jsinterpreter.JsInterpreter;

public class JsInterpreterHelper implements Configurable {

    public static final ConfigKey<Boolean> JS_INTERPRETATION_ENABLED = new ConfigKey<>(ConfigKey.CATEGORY_SYSTEM, Boolean.class, "js.interpretation.enabled",
            "false", "Enable/disable all JavaScript interpretation related functionalities.",
            true, ConfigKey.Scope.Global);

    public void ensureInterpreterEnabledIfParameterProvided(String paramName, boolean paramProvided) {
        if (paramProvided && !JS_INTERPRETATION_ENABLED.value()) {
            throw new InvalidParameterValueException(String.format(
                    "'%s' cannot be set because JavaScript interpretation is disabled in setting '%s'.", paramName, JS_INTERPRETATION_ENABLED.key()));
        }
    }

    @Override
    public String getConfigComponentName() {
        return JsInterpreter.class.getSimpleName();
    }

    @Override
    public ConfigKey<?>[] getConfigKeys() {
        return new ConfigKey<?>[] { JS_INTERPRETATION_ENABLED };
    }

}
