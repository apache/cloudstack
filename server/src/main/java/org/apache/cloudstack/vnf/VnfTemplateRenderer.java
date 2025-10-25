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
package org.apache.cloudstack.vnf;

import java.util.Map;

public class VnfTemplateRenderer {
    public RenderedRequest render(Dictionary dict, String key, Map<String,Object> inputs, Map<String,String> injectedHeaders) {
        // TODO: SnakeYAML load -> map; replace ${...}; build method/path/body/headers; return RenderedRequest
        return new RenderedRequest("POST", "/api/v2/firewall/rule", Map.of(), injectedHeaders);
    }

    public static class Dictionary { public Map<String,Object> root; }
    public static class RenderedRequest {
        public final String method, path; public final Object body; public final Map<String,String> headers;
        public RenderedRequest(String m, String p, Object b, Map<String,String> h){ method=m; path=p; body=b; headers=h; }
    }
}
