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
import org.apache.cloudstack.framework.kms.KeyPurpose;
import org.apache.cloudstack.kms.KMSKey;
import org.apache.cloudstack.kms.KMSKeyVO;

import java.util.List;

/**
 * DAO for KMSKey entities
 */
public interface KMSKeyDao extends GenericDao<KMSKeyVO, Long> {

    /**
     * Find a KMS key by UUID
     */
    KMSKeyVO findByUuid(String uuid);

    /**
     * Find a KMS key by KEK label and provider
     */
    KMSKeyVO findByKekLabel(String kekLabel, String providerName);

    /**
     * List KMS keys owned by an account
     */
    List<KMSKeyVO> listByAccount(Long accountId, KeyPurpose purpose, KMSKey.State state);

    /**
     * List KMS keys in a domain (optionally including subdomains)
     */
    List<KMSKeyVO> listByDomain(Long domainId, KeyPurpose purpose, KMSKey.State state, boolean includeSubdomains);

    /**
     * List KMS keys in a zone
     */
    List<KMSKeyVO> listByZone(Long zoneId, KeyPurpose purpose, KMSKey.State state);

    /**
     * List KMS keys accessible to an account (owns or in parent domain)
     */
    List<KMSKeyVO> listAccessibleKeys(Long accountId, Long domainId, Long zoneId, KeyPurpose purpose, KMSKey.State state);

    /**
     * Count how many wrapped keys reference this KEK
     */
    long countWrappedKeysByKmsKey(Long kmsKeyId);

    /**
     * Count KEKs by label (to check for duplicates)
     */
    long countByKekLabel(String kekLabel, String providerName);
}

