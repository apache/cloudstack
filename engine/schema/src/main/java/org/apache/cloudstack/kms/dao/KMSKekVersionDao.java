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
import org.apache.cloudstack.kms.KMSKekVersionVO;

import java.util.List;

/**
 * DAO for KMSKekVersion entities
 */
public interface KMSKekVersionDao extends GenericDao<KMSKekVersionVO, Long> {

    /**
     * Find a KEK version by UUID
     */
    KMSKekVersionVO findByUuid(String uuid);

    /**
     * Get the active version for a KMS key
     */
    KMSKekVersionVO getActiveVersion(Long kmsKeyId);

    /**
     * Get all versions that can be used for decryption (Active and Previous)
     */
    List<KMSKekVersionVO> getVersionsForDecryption(Long kmsKeyId);

    /**
     * List all versions for a KMS key
     */
    List<KMSKekVersionVO> listByKmsKeyId(Long kmsKeyId);

    /**
     * Find a specific version by KMS key ID and version number
     */
    KMSKekVersionVO findByKmsKeyIdAndVersion(Long kmsKeyId, Integer versionNumber);

    /**
     * Find a KEK version by KEK label
     */
    KMSKekVersionVO findByKekLabel(String kekLabel);
}

