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
package com.cloud.storage.dao;

import java.util.List;

import com.cloud.storage.LaunchPermissionVO;
import com.cloud.storage.VMTemplateVO;
import com.cloud.utils.db.GenericDao;

public interface LaunchPermissionDao extends GenericDao<LaunchPermissionVO, Long> {
    /**
     * remove the ability to launch vms from the given template for the given
     * account names which are valid in the given domain
     *
     * @param templateId
     *            id of the template to modify launch permissions
     * @param accountIds
     *            list of account ids
     */
    void removePermissions(long templateId, List<Long> accountIds);

    /**
     * remove all launch permissions associated with a template
     *
     * @param templateId
     */
    void removeAllPermissions(long templateId);

    /**
     * Find a launch permission by templateId, accountName, and domainId
     *
     * @param templateId
     *            the id of the template to search for
     * @param accountId
     *            the id of the account for which permission is being searched
     * @return launch permission if found, null otherwise
     */
    LaunchPermissionVO findByTemplateAndAccount(long templateId, long accountId);

    /**
     * List all launch permissions for the given template
     *
     * @param templateId
     *            id of the template for which launch permissions will be
     *            queried
     * @return list of launch permissions
     */
    List<LaunchPermissionVO> findByTemplate(long templateId);

    /**
     * List all templates for which permission to launch instances has been
     * granted to the given account
     *
     * @param accountId
     * @return
     */
    List<VMTemplateVO> listPermittedTemplates(long accountId);
}
