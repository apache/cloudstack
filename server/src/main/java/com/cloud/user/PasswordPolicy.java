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
package com.cloud.user;

import org.apache.cloudstack.framework.config.ConfigKey;

public interface PasswordPolicy {

    ConfigKey<Integer> PasswordPolicyMinimumSpecialCharacters = new ConfigKey<>(
            "Advanced",
            Integer.class,
            "password.policy.minimum.special.characters",
            "0",
            "Minimum number of special characters that the user's password must have. Any character that is neither a letter nor numeric is considered special. " +
                    "The value 0 means the user's password does not require any special characters.",
            true,
            ConfigKey.Scope.Domain);

    ConfigKey<Integer> PasswordPolicyMinimumLength = new ConfigKey<>(
            "Advanced",
            Integer.class,
            "password.policy.minimum.length",
            "0",
            "Minimum length that the user's password must have. The value 0 means the user's password can have any length.",
            true,
            ConfigKey.Scope.Domain);

    ConfigKey<Integer> PasswordPolicyMinimumUppercaseLetters = new ConfigKey<>(
            "Advanced",
            Integer.class,
            "password.policy.minimum.uppercase.letters",
            "0",
            "Minimum number of uppercase letters [A-Z] that the user's password must have. The value 0 means the user's password does not require any uppercase letters.",
            true,
            ConfigKey.Scope.Domain);

    ConfigKey<Integer> PasswordPolicyMinimumLowercaseLetters = new ConfigKey<>(
            "Advanced",
            Integer.class,
            "password.policy.minimum.lowercase.letters",
            "0",
            "Minimum number of lowercase letters [a-z] that the user's password must have. The value 0 means the user's password does not require any lowercase letters.",
            true,
            ConfigKey.Scope.Domain);

    ConfigKey<Integer> PasswordPolicyMinimumDigits = new ConfigKey<>(
            "Advanced",
            Integer.class,
            "password.policy.minimum.digits",
            "0",
            "Minimum number of numeric characters [0-9] that the user's password must have. The value 0 means the user's password does not require any numeric characters.",
            true,
            ConfigKey.Scope.Domain);

    ConfigKey<Boolean> PasswordPolicyAllowPasswordToContainUsername = new ConfigKey<>(
            "Advanced",
            Boolean.class,
            "password.policy.allowPasswordToContainUsername",
            "true",
            "Indicates if the user's password may contain their username. Set 'true' (default) if it is allowed, otherwise set 'false'.",
            true,
            ConfigKey.Scope.Domain);

    ConfigKey<String> PasswordPolicyRegex = new ConfigKey<>(
            "Advanced",
            String.class,
            "password.policy.regex",
            ".+",
            "A regular expression that the user's password must match. The default expression '.+' will match with any password.",
            true,
            ConfigKey.Scope.Domain);

    /**
     * Checks if a given user's password complies with the configured password policies.
     * If it does not comply, a {@link com.cloud.exception.InvalidParameterValueException} will be thrown.
     * */
    void verifyIfPasswordCompliesWithPasswordPolicies(String password, String username, Long domainID);
}
