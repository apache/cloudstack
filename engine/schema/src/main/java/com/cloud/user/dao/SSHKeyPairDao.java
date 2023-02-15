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
package com.cloud.user.dao;

import java.util.List;

import com.cloud.user.SSHKeyPairVO;
import com.cloud.utils.db.GenericDao;

public interface SSHKeyPairDao extends GenericDao<SSHKeyPairVO, Long> {

    public List<SSHKeyPairVO> listKeyPairs(long accountId, long domainId);

    public List<SSHKeyPairVO> listKeyPairsByName(long accountId, long domainId, String name);

    public List<SSHKeyPairVO> listKeyPairsByFingerprint(long accountId, long domainId, String fingerprint);

    public SSHKeyPairVO findByName(long accountId, long domainId, String name);

    public SSHKeyPairVO findByPublicKey(String publicKey);

    public boolean deleteByName(long accountId, long domainId, String name);

    public SSHKeyPairVO findByPublicKey(long accountId, long domainId, String publicKey);

    public List<SSHKeyPairVO> findByNames(long accountId, long domainId, List<String> names);

}
