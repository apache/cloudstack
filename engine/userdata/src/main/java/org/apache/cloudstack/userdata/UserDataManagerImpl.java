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
package org.apache.cloudstack.userdata;

import java.util.HashMap;
import java.util.List;
import java.util.Map;

import org.apache.cloudstack.framework.config.ConfigKey;
import org.apache.commons.codec.binary.Base64;
import org.apache.commons.lang3.StringUtils;

import com.cloud.utils.component.ManagerBase;
import com.cloud.utils.exception.CloudRuntimeException;

public class UserDataManagerImpl extends ManagerBase implements UserDataManager {
    private List<UserDataProvider> userDataProviders;
    private static Map<String, UserDataProvider> userDataProvidersMap = new HashMap<>();

    public void setUserDataProviders(final List<UserDataProvider> userDataProviders) {
        this.userDataProviders = userDataProviders;
    }

    private void initializeUserdataProvidersMap() {
        if (userDataProviders != null) {
            for (final UserDataProvider provider : userDataProviders) {
                userDataProvidersMap.put(provider.getName().toLowerCase(), provider);
            }
        }
    }

    @Override
    public boolean start() {
        initializeUserdataProvidersMap();
        return true;
    }

    @Override
    public String getConfigComponentName() {
        return UserDataManagerImpl.class.getSimpleName();
    }

    @Override
    public ConfigKey<?>[] getConfigKeys() {
        return new ConfigKey[] {};
    }

    protected UserDataProvider getUserdataProvider(String name) {
        if (StringUtils.isEmpty(name)) {
            // Use cloud-init as the default userdata provider
            name = "cloud-init";
        }
        if (!userDataProvidersMap.containsKey(name)) {
            throw new CloudRuntimeException("Failed to find userdata provider by the name: " + name);
        }
        return userDataProvidersMap.get(name);
    }

    @Override
    public String concatenateUserData(String userdata1, String userdata2, String userdataProvider) {
        UserDataProvider provider = getUserdataProvider(userdataProvider);
        String appendUserData = provider.appendUserData(userdata1, userdata2);
        return Base64.encodeBase64String(appendUserData.getBytes());
    }
}
