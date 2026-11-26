// Licensed to the Apache Software Foundation (ASF) under one or more
// contributor license agreements. See the NOTICE file distributed with this
// work for additional information regarding copyright ownership. The ASF
// licenses this file to you under the Apache License, Version 2.0 (the
// "License"); you may not use this file except in compliance with the
// License. You may obtain a copy of the License at
//
//   http://www.apache.org/licenses/LICENSE-2.0
//
// Unless required by applicable law or agreed to in writing, software
// distributed under the License is distributed on an "AS IS" BASIS, WITHOUT
// WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied. See the
// License for the specific language governing permissions and limitations
// under the License.
package org.apache.cloudstack.ai.engine;

import java.util.Map;

/**
 * One HTTP exchange with an engine replica.
 *
 * Separated from {@link EngineClient} so failover and caller-context
 * handling can be tested without a socket or a certificate.
 */
public interface EngineTransport {

    /**
     * @throws EngineException with status 0 when the replica cannot be
     *         reached, which is the only case the client fails over on.
     */
    EngineResponse send(String method, String url, Map<String, String> headers, String body);
}
