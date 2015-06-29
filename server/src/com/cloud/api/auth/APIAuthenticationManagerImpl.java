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
package com.cloud.api.auth;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

import javax.ejb.Local;

import org.apache.log4j.Logger;

import org.apache.cloudstack.api.APICommand;
import org.apache.cloudstack.api.auth.APIAuthenticationManager;
import org.apache.cloudstack.api.auth.APIAuthenticator;
import org.apache.cloudstack.api.auth.PluggableAPIAuthenticator;

import com.cloud.utils.component.ComponentContext;
import com.cloud.utils.component.ManagerBase;

@Local(value = APIAuthenticationManager.class)
@SuppressWarnings("unchecked")
public class APIAuthenticationManagerImpl extends ManagerBase implements APIAuthenticationManager {
    public static final Logger s_logger = Logger.getLogger(APIAuthenticationManagerImpl.class.getName());

    private List<PluggableAPIAuthenticator> _apiAuthenticators;

    private static Map<String, Class<?>> s_authenticators = null;

    public APIAuthenticationManagerImpl() {
    }

    public List<PluggableAPIAuthenticator> getApiAuthenticators() {
        return _apiAuthenticators;
    }

    public void setApiAuthenticators(List<PluggableAPIAuthenticator> authenticators) {
        _apiAuthenticators = authenticators;
    }

    @Override
    public boolean start() {
        initAuthenticator();
        for (Class<?> authenticator: getCommands()) {
            APICommand command = authenticator.getAnnotation(APICommand.class);
            if (command != null && !command.name().isEmpty()
                    && APIAuthenticator.class.isAssignableFrom(authenticator)) {
                addAuthenticator(authenticator, command);
            }
        }
        return true;
    }

    private static synchronized void addAuthenticator(Class<?> authenticator, APICommand command) {
        s_authenticators.put(command.name().toLowerCase(), authenticator);
    }

    private static synchronized void initAuthenticator() {
        s_authenticators = new ConcurrentHashMap<String, Class<?>>();
    }

    @Override
    public List<Class<?>> getCommands() {
        List<Class<?>> cmdList = new ArrayList<Class<?>>();
        cmdList.add(DefaultLoginAPIAuthenticatorCmd.class);
        cmdList.add(DefaultLogoutAPIAuthenticatorCmd.class);
        for (PluggableAPIAuthenticator apiAuthenticator: _apiAuthenticators) {
            List<Class<?>> commands = apiAuthenticator.getAuthCommands();
            if (commands != null) {
                cmdList.addAll(commands);
            } else {
                s_logger.warn("API Authenticator returned null api commands:" + apiAuthenticator.getName());
            }
        }
        return cmdList;
    }

    @Override
    public APIAuthenticator getAPIAuthenticator(String name) {
        name = name.toLowerCase();
        APIAuthenticator apiAuthenticator = null;
        if (s_authenticators != null && s_authenticators.containsKey(name)) {
            try {
                apiAuthenticator = (APIAuthenticator) s_authenticators.get(name).newInstance();
                apiAuthenticator = ComponentContext.inject(apiAuthenticator);
                apiAuthenticator.setAuthenticators(_apiAuthenticators);
            } catch (InstantiationException | IllegalAccessException e) {
                if (s_logger.isDebugEnabled()) {
                    s_logger.debug("APIAuthenticationManagerImpl::getAPIAuthenticator failed: " + e.getMessage());
                }
            }
        }
        return apiAuthenticator;
    }
}
