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

package com.cloud.utils.net;

import java.net.URI;
import java.net.URL;
import java.util.HashMap;
import java.util.Map;

public class UrlUtil {
    public final static Map<String, String> parseQueryParameters(String query) {
        HashMap<String, String> values = new HashMap<String, String>();
        parseQueryParameters(query, false, values);

        return values;
    }

    public final static Map<String, String> parseQueryParameters(URL url) {
        return parseQueryParameters(url.getQuery());
    }

    public final static Map<String, String> parseQueryParameters(URI url) {
        return parseQueryParameters(url.getQuery());
    }

    public final static void parseQueryParameters(String query, boolean lowercaseKeys, Map<String, String> params) {
        if (query == null) {
            return;
        }

        if (query.startsWith("?")) {
            query = query.substring(1);
        }

        String[] parts = query.split("&");
        for (String part : parts) {
            String[] tokens = part.split("=");

            if (lowercaseKeys) {
                tokens[0] = tokens[0].toLowerCase();
            }

            params.put(tokens[0], tokens[1]);
        }
    }
}
