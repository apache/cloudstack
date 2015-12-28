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
package com.cloud.naming;

import org.apache.log4j.Logger;

import com.cloud.network.security.SecurityGroup;
import com.cloud.network.security.SecurityGroupVO;

public class DefaultSecurityGroupNamingPolicy extends AbstractResourceNamingPolicy implements SecurityGroupNamingPolicy {

    private static final String DEFAULT_GROUP_NAME = "default";
    private static final Logger s_logger = Logger.getLogger(DefaultSecurityGroupNamingPolicy.class);

    @Override
    public void finalizeIdentifiers(SecurityGroupVO vo) {
        if (!checkUuidSimple(vo.getUuid(), SecurityGroup.class)) {
            String oldUuid = vo.getUuid();
            vo.setUuid(generateUuid(vo.getId(), vo.getAccountId(), null));
            s_logger.warn("Invalid uuid for resource: '" + oldUuid + "'; changed to " + vo.getUuid());
        }
    }

    @Override
    public void checkCustomUuid(String uuid) {
        super.checkUuid(uuid, SecurityGroup.class);
    }

    @Override
    public String generateUuid(Long resourceId, Long userId, String customUuid) {
        return super.generateUuid(SecurityGroup.class, resourceId, userId, customUuid);
    }

    @Override
    public String getSgDefaultName() {
        return DEFAULT_GROUP_NAME;
    }

}
