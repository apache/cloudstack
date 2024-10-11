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
package org.apache.cloudstack.ldap.dao;

import java.util.List;

import org.apache.cloudstack.ldap.LdapConfigurationVO;

import com.cloud.utils.Pair;
import com.cloud.utils.db.GenericDao;

/**
 * TODO the domain value null now searches for that specifically and there is no way to search for all domains
 */
public interface LdapConfigurationDao extends GenericDao<LdapConfigurationVO, Long> {
    /**
     * @deprecated there might well be more then one ldap implementation on a host and or a double binding of several domains
     * @param hostname
     * @return
     */
    @Deprecated
    LdapConfigurationVO findByHostname(String hostname);

    LdapConfigurationVO find(String hostname, int port, Long domainId);

    LdapConfigurationVO find(String hostname, int port, Long domainId, boolean listAll);

    Pair<List<LdapConfigurationVO>, Integer> searchConfigurations(String hostname, int port, Long domainId);

    Pair<List<LdapConfigurationVO>, Integer> searchConfigurations(String hostname, int port, Long domainId, boolean listAll);
}
