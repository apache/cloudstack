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

package org.apache.cloudstack.storage.service;

import org.apache.cloudstack.storage.feign.model.OntapStorage;

public abstract class SANStrategy extends StorageStrategy {
    public SANStrategy(OntapStorage ontapStorage) {
        super(ontapStorage);
    }

    /**
     * Ensures the LUN is mapped to the specified access group (igroup).
     * If a mapping already exists, returns the existing LUN number.
     * If not, creates a new mapping and returns the assigned LUN number.
     *
     * @param svmName the SVM name
     * @param lunName the LUN name
     * @param accessGroupName the igroup name
     * @return the logical unit number as a String
     */
    public abstract String ensureLunMapped(String svmName, String lunName, String accessGroupName);

    /**
     * Validates that the host initiator is present in the access group (igroup).
     *
     * @param hostInitiator the host initiator IQN
     * @param svmName the SVM name
     * @param accessGroupName the igroup name
     * @return true if the initiator is found in the igroup, false otherwise
     */
    public abstract boolean validateInitiatorInAccessGroup(String hostInitiator, String svmName, String accessGroupName);
}
