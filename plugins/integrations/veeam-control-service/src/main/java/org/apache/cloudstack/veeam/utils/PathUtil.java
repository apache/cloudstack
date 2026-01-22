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

package org.apache.cloudstack.veeam.utils;

import com.cloud.utils.Pair;

public class PathUtil {

    public static Pair<String, String> extractIdAndSubPath(final String path, final String baseRoute) {

        // baseRoute = "/api/datacenters"
        if (!path.startsWith(baseRoute)) {
            return null;
        }

        // Remove base route
        String rest = path.substring(baseRoute.length());

        // Expect "" or "/{id}" or "/{id}/{sub}"
        if (rest.isEmpty()) {
            return null; // /api/datacenters (no id)
        }

        if (!rest.startsWith("/")) {
            return null;
        }

        rest = rest.substring(1); // remove leading '/'

        final String[] parts = rest.split("/", -1);

        if (parts.length == 1) {
            // /api/datacenters/{id}
            if (parts[0].isEmpty()) return null;
            return new Pair<>(parts[0], null);
        }

        if (parts.length == 2) {
            // /api/datacenters/{id}/{subPath}
            if (parts[0].isEmpty() || parts[1].isEmpty()) return null;
            return new Pair<>(parts[0], parts[1]);
        }

        // deeper paths not handled here
        return null;
    }
}
