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

package org.apache.cloudstack.backup;

public class BackupPolicyTO implements BackupPolicy {

    private String uuid;
    private long id;
    private String name;
    private String description;
    private String externalId;
    private long zoneId;

    public BackupPolicyTO() {
    }

    public BackupPolicyTO(final String externalId, final String name, final String description) {
        this.name = name;
        this.description = description;
        this.externalId = externalId;
    }

    @Override
    public String getExternalId() {
        return externalId;
    }

    public String getName() {
        return name;
    }

    @Override
    public String getDescription() {
        return description;
    }

    @Override
    public String getUuid() {
        return uuid;
    }

    @Override
    public long getId() {
        return id;
    }

    @Override
    public boolean isImported() {
        return false;
    }

    @Override
    public long getZoneId() {
        return zoneId;
    }
}
