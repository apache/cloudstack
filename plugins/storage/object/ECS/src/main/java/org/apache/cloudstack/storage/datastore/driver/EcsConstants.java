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

public final class EcsConstants {
    private EcsConstants() {
    }

    // Object store details keys
    public static final String MGMT_URL = "mgmt_url";
    public static final String SA_USER = "sa_user";
    public static final String SA_PASS = "sa_password";
    public static final String NAMESPACE = "namespace";
    public static final String INSECURE = "insecure";
    public static final String S3_HOST = "s3_host";
    public static final String USER_PREFIX = "user_prefix";
    public static final String DEFAULT_USER_PREFIX = "cs-";

    // Per-account keys
    public static final String AD_KEY_ACCESS = "ecs.accesskey";
    public static final String AD_KEY_SECRET = "ecs.secretkey";
}
