//
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
//
package org.apache.cloudstack.oauth2;

import com.cloud.user.User;
import com.cloud.user.UserVO;
import com.cloud.user.dao.UserDao;
import com.cloud.utils.component.AdapterBase;
import org.apache.log4j.Logger;

import javax.inject.Inject;
import java.util.List;

public class OAuth2AuthManagerImpl extends AdapterBase implements OAuth2AuthManager {
    private static final Logger s_logger = Logger.getLogger(OAuth2AuthManagerImpl.class);
    @Inject
    private UserDao _userDao;

    @Override
    public List<Class<?>> getAuthCommands() {
        return null;
    }

    @Override
    public boolean start() {
        if (isSAMLPluginEnabled()) {
            s_logger.info("OAUTH auth plugin loaded");
            return setup();
        } else {
            s_logger.info("OAUTH auth plugin not enabled so not loading");
            return super.start();
        }
    }

    private boolean setup() {
        return true;
    }

    private boolean isSAMLPluginEnabled() {
        return OAuth2IsPluginEnabled.value();
    }

    @Override
    public boolean stop() {
        return false;
    }

    @Override
    public List<Class<?>> getCommands() {
        return null;
    }

    @Override
    public boolean authorizeUser(Long userId, String OauthProviderId, boolean enable) {
        UserVO user = _userDao.getUser(userId);
        if (user != null) {
            if (enable) {
                user.setExternalEntity(OauthProviderId);
                user.setSource(User.Source.OAUTH2);
            } else {
                return false;
            }
            _userDao.update(user.getId(), user);
            return true;
        }
        return false;
    }
}
