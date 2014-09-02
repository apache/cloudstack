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

import com.cloud.utils.component.ComponentContext;
import com.cloud.utils.component.ManagerBase;
import org.apache.cloudstack.api.APICommand;
import org.apache.log4j.Logger;

import javax.ejb.Local;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

@Local(value = APIAuthenticationManager.class)
@SuppressWarnings("unchecked")
public class APIAuthenticationManagerImpl extends ManagerBase implements APIAuthenticationManager {
    public static final Logger s_logger = Logger.getLogger(APIAuthenticationManagerImpl.class.getName());

    private static Map<String, Class<?>> s_authenticators = null;
    private static List<Class<?>> s_commandList = null;

    public APIAuthenticationManagerImpl() {
    }

    @Override
    public boolean start() {
        s_authenticators = new HashMap<String, Class<?>>();
        for (Class<?> authenticator: getCommands()) {
            APICommand command = authenticator.getAnnotation(APICommand.class);
            if (command != null && !command.name().isEmpty()
                    && APIAuthenticator.class.isAssignableFrom(authenticator)) {
                s_authenticators.put(command.name(), authenticator);
            }
        }
        return true;
    }

    @Override
    public List<Class<?>> getCommands() {
        if (s_commandList == null) {
            s_commandList = new ArrayList<Class<?>>();
            s_commandList.add(DefaultLoginAPIAuthenticatorCmd.class);
            s_commandList.add(DefaultLogoutAPIAuthenticatorCmd.class);
            s_commandList.add(SAML2LoginAPIAuthenticatorCmd.class);
            s_commandList.add(SAML2LogoutAPIAuthenticatorCmd.class);
        }
        return s_commandList;
    }

    @Override
    public APIAuthenticator getAPIAuthenticator(String name) {
        APIAuthenticator apiAuthenticator = null;
        if (s_authenticators != null && s_authenticators.containsKey(name)) {
            try {
                apiAuthenticator = (APIAuthenticator) s_authenticators.get(name).newInstance();
                apiAuthenticator = ComponentContext.inject(apiAuthenticator);
            } catch (InstantiationException | IllegalAccessException e) {
                if (s_logger.isDebugEnabled()) {
                    s_logger.debug("APIAuthenticationManagerImpl::getAPIAuthenticator failed: " + e.getMessage());
                }
            }
        }
        return apiAuthenticator;
    }
}
