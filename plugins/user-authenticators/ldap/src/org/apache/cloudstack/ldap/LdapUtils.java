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
package org.apache.cloudstack.ldap;

import javax.naming.NamingException;
import javax.naming.directory.Attribute;
import javax.naming.directory.Attributes;

public final class LdapUtils {
    public static String escapeLDAPSearchFilter(final String filter) {
        final StringBuilder sb = new StringBuilder();
        for (final char character : filter.toCharArray()) {
            switch (character) {
                case '\\':
                    sb.append("\\5c");
                    break;
                case '*':
                    sb.append("\\2a");
                    break;
                case '(':
                    sb.append("\\28");
                    break;
                case ')':
                    sb.append("\\29");
                    break;
                case '\u0000':
                    sb.append("\\00");
                    break;
                default:
                    sb.append(character);
            }
        }
        return sb.toString();
    }

    public static String getAttributeValue(final Attributes attributes, final String attributeName) throws NamingException {
        final Attribute attribute = attributes.get(attributeName);
        if (attribute != null) {
            final Object value = attribute.get();
            return String.valueOf(value);
        }
        return null;
    }

    private LdapUtils() {
    }
}