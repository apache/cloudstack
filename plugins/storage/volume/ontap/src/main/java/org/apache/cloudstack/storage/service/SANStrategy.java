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

import org.apache.cloudstack.storage.feign.model.Lun;
import org.apache.cloudstack.storage.feign.model.OntapStorage;

public abstract class SANStrategy extends StorageStrategy {
    public SANStrategy(OntapStorage ontapStorage) {
        super(ontapStorage);
    }

    public abstract Lun createLUN(String svmName, String volumeName, String lunName, long sizeBytes, String osType);
    public abstract String createIgroup(String svmName, String igroupName, String[] initiators);
    public abstract String mapLUNToIgroup(String lunName, String igroupName);
    public abstract String enableISCSI(String svmUuid);
}