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
package org.apache.cloudstack.ldap;

import org.apache.cloudstack.api.LdapAccountManager;
import org.apache.cloudstack.ldap.dao.LdapTrustMapDao;
import org.apache.log4j.Logger;
import org.springframework.stereotype.Component;

import javax.inject.Inject;

@Component
public class LdapAccountManagerImpl implements LdapAccountManager {

    @Inject
    private LdapTrustMapDao ldapTrustMapDao;

    public static final Logger LOGGER = Logger.getLogger(LdapAccountManagerImpl.class);

    @Override
    public boolean isAccountLinkedToLdap(long domainId, long accountId) {
        return ldapTrustMapDao.findByAccount(domainId, accountId) != null;
    }

    @Override
    public void removeAccountLinkToLdap(long domainId, long accountId) {
        LdapTrustMapVO map = ldapTrustMapDao.findByAccount(domainId, accountId);
        if (map != null) {
            String msg = String.format("Removing link between LDAP: %s - type: %s and account: %s on domain: %s",
                    map.getName(), map.getType().name(), accountId, domainId);
            LOGGER.debug(msg);
            ldapTrustMapDao.remove(map.getId());
        }
    }
}
