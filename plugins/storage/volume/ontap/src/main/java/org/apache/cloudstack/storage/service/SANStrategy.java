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

import org.apache.cloudstack.storage.feign.model.Igroup;
import org.apache.cloudstack.storage.feign.model.Initiator;
import org.apache.cloudstack.storage.feign.model.OntapStorage;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

public abstract class SANStrategy extends StorageStrategy {
    private static final Logger s_logger = LogManager.getLogger(SANStrategy.class);
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
     * @param igroup the igroup
     * @return true if the initiator is found in the igroup, false otherwise
     */
    public boolean validateInitiatorInAccessGroup(String hostInitiator, String svmName, Igroup igroup) {
        s_logger.info("validateInitiatorInAccessGroup: Validating initiator [{}] is in igroup [{}] on SVM [{}]", hostInitiator, igroup, svmName);

        if (hostInitiator == null || hostInitiator.isEmpty()) {
            s_logger.warn("validateInitiatorInAccessGroup: host initiator is null or empty");
            return false;
        }
        if (igroup.getInitiators() != null) {
            for (Initiator initiator : igroup.getInitiators()) {
                if (initiator.getName().equalsIgnoreCase(hostInitiator)) {
                    s_logger.info("validateInitiatorInAccessGroup: Initiator [{}] validated successfully in igroup [{}]", hostInitiator, igroup);
                    return true;
                }
            }
        }
        s_logger.warn("validateInitiatorInAccessGroup: Initiator [{}] NOT found in igroup [{}]", hostInitiator, igroup);
        return false;
    }
}
