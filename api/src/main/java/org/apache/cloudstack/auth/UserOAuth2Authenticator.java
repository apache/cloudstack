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
package org.apache.cloudstack.auth;

import com.cloud.utils.component.Adapter;
import com.cloud.utils.exception.CloudRuntimeException;

public interface UserOAuth2Authenticator extends Adapter {
    /**
     * Returns the unique name of the provider
     * @return returns provider name
     */
    String getName();

    /**
     * Returns description about the OAuth2 provider plugin
     * @return returns description
     */
    String getDescription();

    /**
     * Verifies if the logged in user is
     * @return returns true if its valid user
     */
    boolean verifyUser(String email, String secretCode);

    /**
     * Verifies the code provided by provider and fetches email
     * @return returns email
     */
    String verifyCodeAndFetchEmail(String secretCode);


    /**
     * Fetches email using the accessToken
     * @return returns email
     */
    String getUserEmailAddress() throws CloudRuntimeException;
}
