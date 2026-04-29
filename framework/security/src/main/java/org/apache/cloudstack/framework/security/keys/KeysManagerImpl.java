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
package org.apache.cloudstack.framework.security.keys;

import java.security.NoSuchAlgorithmException;
import java.security.SecureRandom;

import javax.inject.Inject;
import javax.net.ssl.KeyManager;

import org.apache.commons.codec.binary.Base64;
import org.apache.logging.log4j.Logger;
import org.apache.logging.log4j.LogManager;

import org.apache.cloudstack.framework.config.ConfigDepot;
import org.apache.cloudstack.framework.config.ConfigKey;
import org.apache.cloudstack.framework.config.Configurable;
import org.apache.cloudstack.framework.config.dao.ConfigurationDao;
import org.apache.cloudstack.framework.config.impl.ConfigurationVO;

import com.cloud.utils.db.DB;
import com.cloud.utils.db.SearchBuilder;
import com.cloud.utils.db.SearchCriteria;

/**
 * To be perfectly honest, I'm not sure why we need this class.  This used
 * to be in ManagementServerImpl.  I moved the functionality because it seems
 * many features will need this.  However, the right thing will be for setup
 * and upgrade to take care of key generation.  Here, the methods appear to
 * mainly be used for dynamic generation.  I added this class because after
 * talking to Kelven, we think there will be other functionalities we need
 * to centralize to this class.  We'll see how that works out.
 *
 * There's multiple problems here that we need to fix.
 *   - Multiple servers can be generating keys.  This is not atomic.
 *   - The functionality of generating the keys should be moved over to setup/upgrade.
 *
 */
public class KeysManagerImpl implements KeysManager, Configurable {
    protected Logger logger = LogManager.getLogger(getClass());

    @Inject
    ConfigurationDao _configDao;
    @Inject
    ConfigDepot _configDepot;

    @Override
    public String getHashKey() {
        String value = HashKey.value();
        if (value == null) {
            _configDao.getValueAndInitIfNotExist(HashKey.key(), HashKey.category(), getBase64EncodedRandomKey(128), HashKey.description());
        }

        return HashKey.value();
    }

    @Override
    public String getEncryptionKey() {
        String value = EncryptionKey.value();
        if (value == null) {
            _configDao.getValueAndInitIfNotExist(EncryptionKey.key(), EncryptionKey.category(), getBase64EncodedRandomKey(128),
                    EncryptionKey.description());
        }
        return EncryptionKey.value();
    }

    @Override
    public String getEncryptionIV() {
        String value = EncryptionIV.value();
        if (value == null) {
            _configDao.getValueAndInitIfNotExist(EncryptionIV.key(), EncryptionIV.category(), getBase64EncodedRandomKey(128),
                    EncryptionIV.description());
        }
        return EncryptionIV.value();
    }

    private String getBase64EncodedRandomKey(int nBits) {
        SecureRandom random;
        try {
            random = SecureRandom.getInstance("SHA1PRNG");
            byte[] keyBytes = new byte[nBits / 8];
            random.nextBytes(keyBytes);
            return Base64.encodeBase64URLSafeString(keyBytes);
        } catch (NoSuchAlgorithmException e) {
            logger.error("Unhandled exception: ", e);
        }
        return null;
    }

    @Override
    @DB
    public void resetEncryptionKeyIV() {

        SearchBuilder<ConfigurationVO> sb = _configDao.createSearchBuilder();
        sb.and("name1", sb.entity().getName(), SearchCriteria.Op.EQ);
        sb.or("name2", sb.entity().getName(), SearchCriteria.Op.EQ);
        sb.done();

        SearchCriteria<ConfigurationVO> sc = sb.create();
        sc.setParameters("name1", EncryptionKey.key());
        sc.setParameters("name2", EncryptionIV.key());

        _configDao.expunge(sc);
    }

    @Override
    public String getConfigComponentName() {
        return KeyManager.class.getSimpleName();
    }

    @Override
    public ConfigKey<?>[] getConfigKeys() {
        return new ConfigKey<?>[] {EncryptionKey, EncryptionIV, HashKey};
    }

}
