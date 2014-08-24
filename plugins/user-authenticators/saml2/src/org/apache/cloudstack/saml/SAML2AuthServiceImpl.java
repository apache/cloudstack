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
package org.apache.cloudstack.saml;

import com.cloud.utils.component.AdapterBase;
import org.apache.cloudstack.api.auth.PluggableAPIAuthenticator;
import org.apache.cloudstack.api.command.SAML2LoginAPIAuthenticatorCmd;
import org.apache.cloudstack.api.command.SAML2LogoutAPIAuthenticatorCmd;
import org.apache.log4j.Logger;
import org.springframework.stereotype.Component;

import javax.ejb.Local;
import java.util.ArrayList;
import java.util.List;

@Component
@Local(value = PluggableAPIAuthenticator.class)
public class SAML2AuthServiceImpl extends AdapterBase implements PluggableAPIAuthenticator {
    private static final Logger s_logger = Logger.getLogger(SAML2AuthServiceImpl.class);

    protected SAML2AuthServiceImpl() {
        super();
    }

    @Override
    public boolean start() {
        return true;
    }

    @Override
    public List<Class<?>> getAuthCommands() {
        List<Class<?>> cmdList = new ArrayList<Class<?>>();
        cmdList.add(SAML2LoginAPIAuthenticatorCmd.class);
        cmdList.add(SAML2LogoutAPIAuthenticatorCmd.class);
        return cmdList;
    }
}
