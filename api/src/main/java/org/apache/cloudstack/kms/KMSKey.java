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

package org.apache.cloudstack.kms;

import org.apache.cloudstack.acl.ControlledEntity;
import org.apache.cloudstack.api.Identity;
import org.apache.cloudstack.api.InternalIdentity;
import org.apache.cloudstack.framework.kms.KeyPurpose;

import java.util.Date;

/**
 * KMS Key (Key Encryption Key) metadata.
 * Represents a KEK that can be used to wrap/unwrap Data Encryption Keys (DEKs).
 * KEKs are account-scoped and used for envelope encryption.
 */
public interface KMSKey extends Identity, InternalIdentity, ControlledEntity {

    String getName();

    String getDescription();

    /**
     * Provider-specific KEK label/ID (internal identifier used by the KMS provider)
     */
    String getKekLabel();

    KeyPurpose getPurpose();

    Long getZoneId();

    String getAlgorithm();

    Integer getKeyBits();

    boolean isEnabled();

    Date getCreated();

    Date getRemoved();

    Long getHsmProfileId();
}
