//  Licensed to the Apache Software Foundation (ASF) under one or more
//  contributor license agreements.  See the NOTICE file distributed with
//  this work for additional information regarding copyright ownership.
//  The ASF licenses this file to You under the Apache License, Version 2.0
//  (the "License"); you may not use this file except in compliance with
//  the License.  You may obtain a copy of the License at
//
//    http://www.apache.org/licenses/LICENSE-2.0
//
//  Unless required by applicable law or agreed to in writing, software
//  distributed under the License is distributed on an "AS IS" BASIS,
//  WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
//  See the License for the specific language governing permissions and
//  limitations under the License.
package org.apache.cloudstack;

import com.cloud.server.auth.DefaultUserAuthenticator;
import com.cloud.server.auth.UserAuthenticator;
import com.cloud.utils.Pair;
import org.apache.log4j.Logger;

import javax.ejb.Local;
import java.util.Map;

@Local(value = {UserAuthenticator.class})
public class SAML2UserAuthenticator extends DefaultUserAuthenticator {
    public static final Logger s_logger = Logger.getLogger(SAML2UserAuthenticator.class);

    @Override
    public Pair<Boolean, ActionOnFailedAuthentication> authenticate(String username, String password, Long domainId, Map<String, Object[]> requestParameters) {
        if (s_logger.isDebugEnabled()) {
            s_logger.debug("Trying SAML2 auth for user: " + username);
        }

        // TODO: implement core logic, HTTP GET redirections etc.

        return new Pair<Boolean, ActionOnFailedAuthentication>(true, null);
    }

    @Override
    public String encode(final String password) {
        // TODO: Complete method
        StringBuilder sb = new StringBuilder(32);
        return sb.toString();
    }
}
