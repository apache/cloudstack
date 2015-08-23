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

package com.cloud.utils.db;


public interface UUIDManager {

    /**
     * Generates a new uuid or uses the customId
     * @param entityType the type of entity
     * @param customId optional custom uuid of the object.
     * @return newly created uuid.
     */
    public <T> String generateUuid(Class<T> entityType, String customId);

    /**
     * Checks the uuid for correct format, uniqueness and permissions.
     * @param uuid uuid to check
     * @param entityType the type of entity
     * .
     */
    <T> void checkUuid(String uuid, Class<T> entityType);

    /**
     * Checks the uuid for correct format, uniqueness, without checking permissions
     * @param uuid uuid to check
     * @param entityType the type of entity
     * .
     */
    <T> void checkUuidSimple(String uuid, Class<T> entityType);
}
