/*
 * Licensed to the Apache Software Foundation (ASF) under one
 * or more contributor license agreements.  See the NOTICE file
 * distributed with this work for additional information
 * regarding copyright ownership.  The ASF licenses this file
 * to you under the Apache License, Version 2.0 (the
 * "License"); you may not use this file except in compliance
 * with the License.  You may obtain a copy of the License at
 *
 *   http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing,
 * software distributed under the License is distributed on an
 * "AS IS" BASIS, WITHOUT WARRANTIES OR CONDITIONS OF ANY
 * KIND, either express or implied.  See the License for the
 * specific language governing permissions and limitations
 * under the License.
 */
package org.apache.cloudstack.ldap;

public class DistinguishedNameParser {

    private static final String KEY_VALUE_SEPARATOR = "=";
    private static final String NAME_SEPARATOR = ",";

    public static String parseLeafName(final String distinguishedName) {
        if (distinguishedName.contains(NAME_SEPARATOR)) {
            final String[] parts = distinguishedName.split(NAME_SEPARATOR);
            return parseValue(parts[0]);
        } else {
            return parseValue(distinguishedName);
        }
    }

    private static String parseValue(final String distinguishedName) {
        if (distinguishedName.contains(KEY_VALUE_SEPARATOR)) {
            final String[] parts = distinguishedName.split(KEY_VALUE_SEPARATOR);
            if (parts.length > 1) {
                return parts[1];
            }
        }
        throw new IllegalArgumentException("Malformed distinguished name: " + distinguishedName);
    }

}
