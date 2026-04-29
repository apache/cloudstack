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

package org.apache.cloudstack.acl;

import com.cloud.exception.InvalidParameterValueException;

import java.util.regex.Pattern;

import org.apache.commons.lang3.StringUtils;

public final class Rule {
    private final String rule;
    private final static Pattern ALLOWED_PATTERN = Pattern.compile("^[a-zA-Z0-9*]+$");

    public Rule(final String rule) {
        validate(rule);
        this.rule = rule;
    }

    public boolean matches(final String commandName) {
        return StringUtils.isNotEmpty(commandName)
                && commandName.toLowerCase().matches(rule.toLowerCase().replace("*", "\\w*"));
    }

    public String getRuleString() {
        return rule;
    }

    @Override
    public String toString() {
        return rule;
    }

    private static boolean validate(final String rule) {
        if (StringUtils.isEmpty(rule) || !ALLOWED_PATTERN.matcher(rule).matches()) {
            throw new InvalidParameterValueException("Only API names and wildcards are allowed, invalid rule provided: " + rule);
        }
        return true;
    }
}
