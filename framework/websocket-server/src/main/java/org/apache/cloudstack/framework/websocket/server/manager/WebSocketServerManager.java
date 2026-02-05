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

package org.apache.cloudstack.framework.websocket.server.manager;

import org.apache.cloudstack.framework.websocket.server.common.WebSocketHandler;

public interface WebSocketServerManager {
    boolean isServerEnabled();

    int getServerPort();

    String getWebSocketBasePath();

    boolean isServerSslEnabled();

    /**
     * Register a route:
     * - If pathSpec ends with "/", it's treated as a PREFIX (e.g., "/logger/..." matches).
     * - If pathSpec looks like a regex (e.g., starts with '^' or contains ".*", "[", "(", "|"), it's REGEX.
     * - Otherwise it's an EXACT path (e.g., "/echo").
     */
    void registerRoute(String pathSpec, WebSocketHandler handler, long idleTimeoutSeconds);

    /**
     * Unregister the same key you used to register (exact path, normalized prefix with '/', or regex string).
     */
    void unregisterRoute(String pathSpec);
}
