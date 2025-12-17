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

package org.apache.cloudstack.kms.dao;

import com.cloud.utils.db.GenericDao;
import org.apache.cloudstack.kms.KMSWrappedKeyVO;

import java.util.List;

/**
 * Data Access Object for KMS Wrapped Keys.
 * This DAO is purpose-agnostic and can be used for any key purpose
 * (volumes, TLS certs, config secrets, etc.)
 */
public interface KMSWrappedKeyDao extends GenericDao<KMSWrappedKeyVO, Long> {

    /**
     * Find a wrapped key by UUID
     *
     * @param uuid the key UUID
     * @return the wrapped key, or null if not found
     */
    KMSWrappedKeyVO findByUuid(String uuid);

    /**
     * List all wrapped keys using a specific KMS key
     * (useful for key rotation)
     *
     * @param kmsKeyId the KMS key ID (FK to kms_keys)
     * @return list of wrapped keys
     */
    List<KMSWrappedKeyVO> listByKmsKeyId(Long kmsKeyId);

    /**
     * List all wrapped keys in a zone
     *
     * @param zoneId the zone ID
     * @return list of wrapped keys
     */
    List<KMSWrappedKeyVO> listByZone(Long zoneId);

    /**
     * Count wrapped keys using a specific KMS key
     *
     * @param kmsKeyId the KMS key ID (FK to kms_keys)
     * @return count of keys
     */
    long countByKmsKeyId(Long kmsKeyId);

    /**
     * List all wrapped keys using a specific KEK version
     *
     * @param kekVersionId the KEK version ID (FK to kms_kek_versions)
     * @return list of wrapped keys
     */
    List<KMSWrappedKeyVO> listByKekVersionId(Long kekVersionId);
}

