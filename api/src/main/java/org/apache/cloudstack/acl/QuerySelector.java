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

import java.util.List;

import org.apache.cloudstack.acl.SecurityChecker.AccessType;

import com.cloud.user.Account;
import com.cloud.utils.component.Adapter;

/**
 * QueryChecker returns granted access at domain, account or resource level.
 */
public interface QuerySelector extends Adapter {

    /**
    * List granted domains for the caller, given a specific action.
    *
    * @param caller account to check against.
    * @param action action
    * @return list of domain Ids granted to the caller account.
    */
    List<Long> getAuthorizedDomains(Account caller, String action, AccessType accessType);

    /**
    * List granted accounts for the caller, given a specific action.
    *
    * @param caller account to check against.
    * @param action action.
    * @return list of domain Ids granted to the caller account.
    */
    List<Long> getAuthorizedAccounts(Account caller, String action, AccessType accessType);


    /**
    * List granted resources for the caller, given a specific action.
    *
    * @param caller account to check against.
    * @param action action.
    * @return list of domain Ids granted to the caller account.
    */
    List<Long> getAuthorizedResources(Account caller, String action, AccessType accessType);

    /**
     * Check if this account is associated with a policy with scope of ALL
     * @param caller account to check
     * @param action action.
     * @return true if this account is attached with a policy for the given action of ALL scope.
     */
    boolean isGrantedAll(Account caller, String action, AccessType accessType);

    /**
     * List of ACL group the given account belongs to
     * @param accountId account id.
     * @return ACL group names
     */
    List<String> listAclGroupsByAccount(long accountId);

}
