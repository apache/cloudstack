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

import java.util.function.Consumer;

public class ValidatedConfigKey<T> extends ConfigKey<T> {
    private final Consumer<T> validator;

    public ValidatedConfigKey(String category, Class<T> type, String name, String defaultValue, String description, boolean dynamic, Scope scope, String parent, Consumer<T> validator) {
        super(category, type, name, defaultValue, description, dynamic, scope, parent);
        this.validator = validator;
    }

    public Consumer<T> getValidator() {
        return validator;
    }

    public void validateValue(String value) {
        if (validator != null) {
            validator.accept((T) value);
        }
    }
}
