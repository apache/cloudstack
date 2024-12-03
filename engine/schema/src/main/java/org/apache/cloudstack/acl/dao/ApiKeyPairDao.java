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
package org.apache.cloudstack.acl.dao;

import com.cloud.utils.Pair;
import com.cloud.utils.db.GenericDao;
import org.apache.cloudstack.acl.ApiKeyPairVO;
import org.apache.cloudstack.api.command.admin.user.ListUserKeysCmd;

import java.util.List;

public interface ApiKeyPairDao extends GenericDao<ApiKeyPairVO, Long> {
    ApiKeyPairVO findBySecretKey(String secretKey);

    ApiKeyPairVO findByApiKey(String apiKey);

    ApiKeyPairVO findByUuid(String uuid);

    Pair<List<ApiKeyPairVO>, Integer> listApiKeysByUserOrApiKeyId(Long userId, Long apiKeyId);

    ApiKeyPairVO getLastApiKeyCreatedByUser(Long userId);

    Pair<List<ApiKeyPairVO>, Integer> listByUserIdsPaginated(List<Long> userIds, ListUserKeysCmd cmd);

}
