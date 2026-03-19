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
package org.apache.cloudstack.acl;

import com.cloud.exception.PermissionDeniedException;
import com.cloud.exception.RequestLimitException;
import com.cloud.user.Account;
import com.cloud.user.User;
import com.cloud.utils.component.Adapter;

import java.util.ArrayList;
import java.util.List;

import org.apache.log4j.Logger;

/**
 * APICheckers is designed to verify the ownership of resources and to control the access to APIs.
 */
public interface APIChecker extends Adapter {
    Logger s_logger = Logger.getLogger(APIChecker.class.getName());
    // Interface for checking access for a role using apiname
    // If true, apiChecker has checked the operation
    // If false, apiChecker is unable to handle the operation or not implemented
    // On exception, checkAccess failed don't allow
    boolean checkAccess(User user, String apiCommandName) throws PermissionDeniedException;
    boolean checkAccess(Account account, String apiCommandName) throws PermissionDeniedException;
    /**
     * Verifies if the account has permission for the given list of APIs and returns only the allowed ones.
     *
     * @param role of the user to be verified
     * @param user to be verified
     * @param apiNames the list of apis to be verified
     * @return the list of allowed apis for the given user
     */
    List<String> getApisAllowedToUser(Role role, User user, List<String> apiNames) throws PermissionDeniedException;

    default List<String> getApisAllowedToAccount(Account account, List<String> apiNames) {
        List<String> allowedApis = new ArrayList<>();
        for (String apiName : apiNames) {
            try {
                checkAccess(account, apiName);
                allowedApis.add(apiName);
            } catch (RequestLimitException e) {
                // Non-ACL failure (e.g. rate limiting) should not be treated as simple "not allowed".
                // Propagate as unchecked so callers are aware of the failure.
                throw new RuntimeException("Failed to check access for API [" + apiName + "] due to request limits", e);
            } catch (PermissionDeniedException e) {
                s_logger.debug("Account [" + account + "] is not allowed to access API [" + apiName + "]", e);
            }
        }
        return allowedApis;
    }

    boolean isEnabled();
}
