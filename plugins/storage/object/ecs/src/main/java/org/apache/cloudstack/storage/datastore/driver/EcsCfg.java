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
package org.apache.cloudstack.storage.datastore.driver;

/** Immutable ECS connection configuration shared by the driver and lifecycle. */
public final class EcsCfg {
    public final String mgmtUrl;
    public final String saUser;
    public final String saPass;
    public final String ns;
    public final boolean insecure;

    public EcsCfg(final String mgmtUrl, final String saUser, final String saPass,
                  final String ns, final boolean insecure) {
        this.mgmtUrl  = mgmtUrl;
        this.saUser   = saUser;
        this.saPass   = saPass;
        this.ns       = ns;
        this.insecure = insecure;
    }
}
