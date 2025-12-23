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

package org.apache.cloudstack.kms.provider.database.dao;

import com.cloud.utils.db.GenericDao;
import org.apache.cloudstack.framework.kms.KeyPurpose;
import org.apache.cloudstack.kms.provider.database.KMSDatabaseKekObjectVO;

import java.util.List;

/**
 * DAO for KMSDatabaseKekObject entities
 * Provides PKCS#11-like object storage operations for KEKs
 */
public interface KMSDatabaseKekObjectDao extends GenericDao<KMSDatabaseKekObjectVO, Long> {

    /**
     * Find a KEK object by label (PKCS#11 CKA_LABEL)
     */
    KMSDatabaseKekObjectVO findByLabel(String label);

    /**
     * Find a KEK object by object ID (PKCS#11 CKA_ID)
     */
    KMSDatabaseKekObjectVO findByObjectId(byte[] objectId);

    /**
     * List all KEK objects by purpose
     */
    List<KMSDatabaseKekObjectVO> listByPurpose(KeyPurpose purpose);

    /**
     * List all KEK objects by key type (PKCS#11 CKA_KEY_TYPE)
     */
    List<KMSDatabaseKekObjectVO> listByKeyType(String keyType);

    /**
     * List all KEK objects by object class (PKCS#11 CKA_CLASS)
     */
    List<KMSDatabaseKekObjectVO> listByObjectClass(String objectClass);

    /**
     * Check if a KEK object exists with the given label
     */
    boolean existsByLabel(String label);
}
