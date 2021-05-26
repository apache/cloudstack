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
package org.apache.cloudstack.pki;

import java.util.Map;

import javax.inject.Inject;
import javax.naming.ConfigurationException;

import org.apache.commons.lang.BooleanUtils;

import org.apache.cloudstack.framework.config.ConfigKey;
import org.apache.cloudstack.framework.config.Configurable;
import org.apache.cloudstack.framework.config.dao.ConfigurationDao;

import com.cloud.domain.Domain;
import com.cloud.exception.RemoteAccessVpnException;
import com.cloud.utils.component.ManagerBase;
import com.cloud.utils.net.Ip;

/**
 * PKI Manager base class. This will work as a factory to construct Vault or Default
 * implementation and pass through the API call to corresponding implementation.
 *
 * @author Khosrow Moossavi
 * @since 4.12.0.0
 */
public class PkiManagerImpl extends ManagerBase implements PkiManager, Configurable {
    @Inject
    private ConfigurationDao configDao;

    private PkiEngine pkiEngine;

    /* (non-Javadoc)
     * @see com.cloud.utils.component.ComponentLifecycleBase#configure(java.lang.String, java.util.Map)
     */
    @Override
    public boolean configure(String name, Map<String, Object> params) throws ConfigurationException {
        Map<String, String> configs = configDao.getConfiguration(params);

        if (BooleanUtils.toBoolean(configs.get(PkiConfig.VaultEnabled.key()))) {
            pkiEngine = new PkiEngineVault(configs);
        } else {
            pkiEngine = new PkiEngineDefault(configs);
        }

        return true;
    }

    /* (non-Javadoc)
     * @see org.apache.cloudstack.framework.config.Configurable#getConfigComponentName()
     */
    @Override
    public String getConfigComponentName() {
        return PkiManager.class.getSimpleName();
    }

    /* (non-Javadoc)
     * @see org.apache.cloudstack.framework.config.Configurable#getConfigKeys()
     */
    @Override
    public ConfigKey<?>[] getConfigKeys() {
        return PkiConfig.asConfigKeys();
    }

    /* (non-Javadoc)
     * @see org.apache.cloudstack.pki.PkiManager#issueCertificate(com.cloud.domain.Domain, com.cloud.utils.net.Ip)
     */
    @Override
    public PkiDetail issueCertificate(Domain domain, Ip publicIp) throws RemoteAccessVpnException {
        try {
            return pkiEngine.issueCertificate(domain, publicIp);
        } catch (Exception e) {
            throw new RemoteAccessVpnException(e.getMessage());
        }
    }

    /* (non-Javadoc)
     * @see org.apache.cloudstack.pki.PkiManager#getCertificate(com.cloud.domain.Domain)
     */
    @Override
    public PkiDetail getCertificate(Domain domain) throws RemoteAccessVpnException {
        try {
            return pkiEngine.getCertificate(domain);
        } catch (Exception e) {
            throw new RemoteAccessVpnException(e.getMessage());
        }
    }
}
