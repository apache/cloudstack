//
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
//

package com.cloud.utils.component;

import net.sf.cglib.core.NamingPolicy;
import net.sf.cglib.core.Predicate;

/**
 * Copied/Modified from Spring source
 *
 */
public class ComponentNamingPolicy implements NamingPolicy {

    public static final ComponentNamingPolicy INSTANCE = new ComponentNamingPolicy();

    @Override
    public String getClassName(String prefix, String source, Object key, Predicate names) {
        if (prefix == null) {
            prefix = "net.sf.cglib.empty.Object";
        } else if (prefix.startsWith("java")) {
            prefix = "_" + prefix;
        }
        String base = prefix + "_" + source.substring(source.lastIndexOf('.') + 1) + getTag() + "_" + Integer.toHexString(key.hashCode());
        String attempt = base;
        int index = 2;
        while (names.evaluate(attempt))
            attempt = base + "_" + index++;
        return attempt;
    }

    /**
     * Returns a string which is incorporated into every generated class name.
     * By default returns "ByCloudStack"
     */
    protected String getTag() {
        return "ByCloudStack";
    }

    @Override
    public int hashCode() {
        return getTag().hashCode();
    }

    @Override
    public boolean equals(Object o) {
        return (o instanceof ComponentNamingPolicy) && ((ComponentNamingPolicy)o).getTag().equals(getTag());
    }
}
