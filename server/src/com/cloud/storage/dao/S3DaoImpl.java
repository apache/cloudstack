/*
 * Licensed to the Apache Software Foundation (ASF) under one
 * or more contributor license agreements.  See the NOTICE file
 * distributed with this work for additional information
 * regarding copyright ownership.  The ASF licenses this file
 * to you under the Apache License, Version 2.0 (the
 * "License"); you may not use this file except in compliance
 * with the License.  You may obtain a copy of the License at
 *
 *    http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing,
 * software distributed under the License is distributed on an
 * "AS IS" BASIS, WITHOUT WARRANTIES OR CONDITIONS OF ANY
 * KIND, either express or implied.  See the License for the
 * specific language governing permissions and limitations
 * under the License.
 */
package com.cloud.storage.dao;

import com.cloud.agent.api.to.S3TO;
import com.cloud.storage.S3VO;
import com.cloud.utils.db.GenericDaoBase;

import javax.ejb.Local;

@Local(S3Dao.class)
public class S3DaoImpl extends GenericDaoBase<S3VO, Long> implements S3Dao {

    @Override
    public S3TO getS3TO(final Long id) {

        if (id != null) {

            final S3VO s3VO = findById(id);
            if (s3VO != null) {
                return s3VO.toS3TO();
            }

        }

        // NOTE: Excluded listAll / shuffle operation implemented in SwiftDaoImpl ...

        return null;

    }
}
