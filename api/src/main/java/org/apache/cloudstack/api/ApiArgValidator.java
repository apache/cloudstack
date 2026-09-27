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

package org.apache.cloudstack.api;

import java.util.Locale;
import java.util.regex.Pattern;

import org.apache.commons.lang3.StringUtils;

import com.cloud.exception.InvalidParameterValueException;
import com.cloud.utils.UuidUtils;

public enum ApiArgValidator {
    /**
     * Validates if the parameter is null or empty with the method {@link StringUtils#isEmpty(CharSequence)}.
     * Validation is currently done in the method ParamProcessWorker#validateNonEmptyString(String, String).
     */
    NotNullOrEmpty,

    /**
     * Validates if the parameter is different from null (parameter != null) and greater than zero (parameter > 0).
     */
    PositiveNumber,

    /**
     * Validates if the parameter is a UUID with the method {@link UuidUtils#isUuid(String)}.
     * Validation is currently done in the method ParamProcessWorker#validateUuidString(String, String).
     */
    UuidString,

    /**
     * Validates if the parameter is a valid RFC Compliance domain name.
     */
    RFCComplianceDomainName,

    /**
     * Validates command option strings to avoid unsafe/code-like content.
     */
    SafeCommandOptions((param, annotation) -> {
        if (BaseCmd.CommandType.STRING.equals(annotation.type())) {
            validateSafeCommandOptions(param, annotation.name());
        }
    });

    private static final Pattern SAFE_COMMAND_OPTIONS_PATTERN = Pattern.compile("^[A-Za-z0-9,._=:/+\\-\\s]*$");

    private static final String[] UNSAFE_TOKENS = {
            "$(", "`", "&&", "||", ";", "|", ">", "<"
    };

    private final ValidationRule rule;

    ApiArgValidator() {
        this(null);
    }

    ApiArgValidator(ValidationRule rule) {
        this.rule = rule;
    }

    public void validate(final Object paramObj, final Parameter annotation) {
        if (rule != null) {
            rule.validate(paramObj, annotation);
        }
    }

    private static void validateSafeCommandOptions(final Object param, final String argName) {
        final String value = String.valueOf(param);
        if (StringUtils.isBlank(value)) {
            return;
        }

        if (!SAFE_COMMAND_OPTIONS_PATTERN.matcher(value).matches()) {
            throwInvalidParameterValueException(argName, "contains unsupported or unsafe characters");
        }

        final String normalized = value.toLowerCase(Locale.ROOT);
        for (String token : UNSAFE_TOKENS) {
            if (normalized.contains(token)) {
                throwInvalidParameterValueException(argName, "contains code-like or unsafe content");
            }
        }
    }

    private static void throwInvalidParameterValueException(final String argName, final String customMsg) {
        throw new InvalidParameterValueException(String.format("Invalid value provided for API arg: %s%s", argName,
                StringUtils.isBlank(customMsg) ? "" : " - " + customMsg));
    }

    @FunctionalInterface
    interface ValidationRule {
        void validate(Object paramObj, Parameter annotation);
    }
}
