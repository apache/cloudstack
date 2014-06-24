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
package org.apache.cloudstack.framework.security.keys;

import org.apache.cloudstack.framework.config.ConfigKey;

/**
 *
 * Started this file to manage keys.  Will be needed by other services.
 *
 */
public interface KeysManager {
    final ConfigKey<String> EncryptionKey = new ConfigKey<String>("Hidden", String.class, "security.encryption.key", null, "base64 encoded key data", false);
    final ConfigKey<String> EncryptionIV = new ConfigKey<String>("Hidden", String.class, "security.encryption.iv", null, "base64 encoded IV data", false);
    final ConfigKey<String> HashKey = new ConfigKey<String>("Hidden", String.class, "security.hash.key", null, "for generic key-ed hash", false);

    String getEncryptionKey();

    String getEncryptionIV();

    void resetEncryptionKeyIV();

    String getHashKey();
}
