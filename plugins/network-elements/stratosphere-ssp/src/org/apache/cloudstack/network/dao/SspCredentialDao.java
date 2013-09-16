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
package org.apache.cloudstack.network.dao;

import com.cloud.utils.db.GenericDao;

public interface SspCredentialDao extends GenericDao<SspCredentialVO, Long> {
    /**
     * Find an ssp credential for a specific cloudstack zone.
     *
     * For now, credential is a pair of username and password.
     * We might want to fetch different pairs for each cloudstack users
     * in future work.
     *
     * @param zoneId zone Id
     * @return credential object
     */
    public SspCredentialVO findByZone(long zoneId);
}
