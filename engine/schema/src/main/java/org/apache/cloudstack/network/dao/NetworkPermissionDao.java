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
package org.apache.cloudstack.network.dao;

import java.util.List;

import com.cloud.utils.db.GenericDao;
import org.apache.cloudstack.network.NetworkPermissionVO;

public interface NetworkPermissionDao extends GenericDao<NetworkPermissionVO, Long> {
    /**
     * remove the ability to Network vms from the given network for the given
     * account names which are valid in the given domain
     *
     * @param networkId
     *            id of the network to modify Network permissions
     * @param accountIds
     *            list of account ids
     */
    void removePermissions(long networkId, List<Long> accountIds);

    /**
     * remove all Network permissions associated with a network
     *
     * @param networkId
     */
    void removeAllPermissions(long networkId);

    /**
     * Find a Network permission by networkId, accountName, and domainId
     *
     * @param networkId
     *            the id of the network to search for
     * @param accountId
     *            the id of the account for which permission is being searched
     * @return Network permission if found, null otherwise
     */
    NetworkPermissionVO findByNetworkAndAccount(long networkId, long accountId);

    /**
     * List all Network permissions for the given network
     *
     * @param networkId
     *            id of the network for which Network permissions will be
     *            queried
     * @return list of Network permissions
     */
    List<NetworkPermissionVO> findByNetwork(long networkId);

    List<Long> listPermittedNetworkIdsByAccounts(List<Long> permittedAccounts);
}
